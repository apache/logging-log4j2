/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.util;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * String to byte encoding support.
 *
 * @since 2.5
 */
public final class StringEncoder {

    private StringEncoder() {
    }

    /**
     * Converts a String to a byte[].
     *
     * @param str if null, return null.
     * @param charset if null, use the default charset.
     * @return a byte[]
     */
    public static byte[] toBytes(final String str, final Charset charset) {
        if (str != null) {
            if (StandardCharsets.ISO_8859_1.equals(charset)) {
                return encodeSingleByteChars(str);
            }
            final Charset actual = charset != null ? charset : Charset.defaultCharset();
            try { // LOG4J2-935: String.getBytes(String) gives better performance
                return str.getBytes(actual.name());
            } catch (UnsupportedEncodingException e) {
                return str.getBytes(actual);
            }
        }
        return null;
    }

    /**
     * Encode the specified string by casting each character to a byte.
     *
     * @param s the string to encode
     * @return the encoded String
     * @see https://issues.apache.org/jira/browse/LOG4J2-1151
     */
    public static byte[] encodeSingleByteChars(String s) {
        final int length = s.length();
        final byte[] result = new byte[length];
        encodeString(s, 0, length, result);
        return result;
    }

    // LOG4J2-1151
    /*
     * Implementation note: this is the fast path. If the char array contains only ISO-8859-1 characters, all the work
     * will be done here.
     */
    public static int encodeIsoChars(String charArray, int charIndex, byte[] byteArray, int byteIndex, int length) {
        int i = 0;
        for (; i < length; i++) {
            char c = charArray.charAt(charIndex++);
            if (c > 255) {
                break;
            }
            byteArray[(byteIndex++)] = ((byte) c);
        }
        return i;
    }

    // LOG4J2-1151
    public static int encodeString(String charArray, int charOffset, int charLength, byte[] byteArray) {
        int byteOffset = 0;
        int length = Math.min(charLength, byteArray.length);
        int charDoneIndex = charOffset + length;
        while (charOffset < charDoneIndex) {
            int done = encodeIsoChars(charArray, charOffset, byteArray, byteOffset, length);
            charOffset += done;
            byteOffset += done;
            if (done != length) {
                char c = charArray.charAt(charOffset++);
                if ((Character.isHighSurrogate(c)) && (charOffset < charDoneIndex)
                        && (Character.isLowSurrogate(charArray.charAt(charOffset)))) {
                    if (charLength > byteArray.length) {
                        charDoneIndex++;
                        charLength--;
                    }
                    charOffset++;
                }
                byteArray[(byteOffset++)] = '?';
                length = Math.min(charDoneIndex - charOffset, byteArray.length - byteOffset);
            }
        }
        return byteOffset;
    }
}
