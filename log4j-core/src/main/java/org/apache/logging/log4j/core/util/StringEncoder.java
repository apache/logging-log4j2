/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.util;

import java.nio.charset.Charset;

/**
 * Encodes Strings to bytes.
 *
 * @since 2.5
 */
public final class StringEncoder {

    private StringEncoder() {}

    /**
     * Converts a String to a byte[].
     *
     * @param str if null, return null.
     * @param charset if null, use the default charset.
     * @return a byte[]
     */
    public static byte[] toBytes(final String str, final Charset charset) {
        if (str != null) {
            return str.getBytes(charset != null ? charset : Charset.defaultCharset());
        }
        return null;
    }

    /**
     * Prefer standard {@link String#getBytes(Charset)} which performs better in Java 8 and beyond.
     * Encodes the specified char sequence by casting each character to a byte.
     *
     * @param s the char sequence to encode
     * @return the encoded String
     * @see <a href="https://issues.apache.org/jira/browse/LOG4J2-1151">LOG4J2-1151</a>
     * @deprecated No longer necessary given better performance in Java 8
     */
    @Deprecated
    public static byte[] encodeSingleByteChars(final CharSequence s) {
        final int length = s.length();
        final byte[] result = new byte[length];
        encodeString(s, 0, length, result);
        return result;
    }

    // LOG4J2-1151
    /**
     * Prefer standard {@link String#getBytes(Charset)} which performs better in Java 8 and beyond.
     *
     * Implementation note: this is the fast path. If the char array contains only ISO-8859-1 characters, all the work
     * will be done here.
     *
     * @deprecated No longer necessary given better performance in Java 8
     */
    @Deprecated
    public static int encodeIsoChars(
            final CharSequence charArray, int charIndex, final byte[] byteArray, int byteIndex, final int length) {
        int i = 0;
        for (; i < length; i++) {
            final char c = charArray.charAt(charIndex++);
            if (c > 255) {
                break;
            }
            byteArray[(byteIndex++)] = ((byte) c);
        }
        return i;
    }

    // LOG4J2-1151

    /**
     * Prefer standard {@link String#getBytes(Charset)} which performs better in Java 8 and beyond.
     * @deprecated No longer necessary given better performance in Java 8
     */
    @Deprecated
    public static int encodeString(
            final CharSequence charArray, final int charOffset, final int charLength, final byte[] byteArray) {
        int byteOffset = 0;
        int length = Math.min(charLength, byteArray.length);
        int charDoneIndex = charOffset + length;
        int currentCharOffset = charOffset;
        int currentCharLength = charLength;
        while (currentCharOffset < charDoneIndex) {
            final int done = encodeIsoChars(charArray, currentCharOffset, byteArray, byteOffset, length);
            currentCharOffset += done;
            byteOffset += done;
            if (done != length) {
                final char c = charArray.charAt(currentCharOffset++);
                if ((Character.isHighSurrogate(c))
                        && (currentCharOffset < charDoneIndex)
                        && (Character.isLowSurrogate(charArray.charAt(currentCharOffset)))) {
                    if (currentCharLength > byteArray.length) {
                        charDoneIndex++;
                        currentCharLength--;
                    }
                    currentCharOffset++;
                }
                byteArray[(byteOffset++)] = '?';
                length = Math.min(charDoneIndex - currentCharOffset, byteArray.length - byteOffset);
            }
        }
        return byteOffset;
    }
}
