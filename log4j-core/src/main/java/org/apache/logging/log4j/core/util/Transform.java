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

import org.apache.logging.log4j.util.Strings;

/**
 * Utility class for transforming strings.
 */
public final class Transform {

    private static final String CDATA_START = "<![CDATA[";
    private static final String CDATA_END = "]]>";
    private static final String CDATA_PSEUDO_END = "]]&gt;";
    private static final String CDATA_EMBEDED_END = CDATA_END + CDATA_PSEUDO_END + CDATA_START;
    private static final int CDATA_END_LEN = CDATA_END.length();

    private static final char REPLACEMENT_CHAR = '\uFFFD';

    private Transform() {}

    /**
     * Escapes characters in a string for safe inclusion in HTML or XML text.
     *
     * <p>Replaces the characters {@code <}, {@code >}, {@code &}, {@code "} and {@code '} with their corresponding
     * entity references ({@code &lt;}, {@code &gt;}, {@code &amp;}, {@code &quot;}, and {@code &#39;}). Any code point
     * that is invalid in XML 1.0 is replaced with the Unicode replacement character U+FFFD.</p>
     *
     * @param input The text to be escaped; may be {@code null} or empty.
     * @return The escaped string, or the original {@code input} if no changes were required.
     */
    public static String escapeHtmlTags(final String input) {
        // Check if the string is null or zero length
        // if so, return what was sent in.
        if (Strings.isEmpty(input)) {
            return input;
        }

        // Only create a new string if we find a special character or invalid code point.
        // In the common case, this should avoid unnecessary allocations.
        final int length = input.length();
        for (int i = 0; i < length; ) {
            final int cp = input.codePointAt(i);
            if (!isValidXml10(cp) || isHtmlTagCharacter(cp)) {
                final StringBuilder out = new StringBuilder(length);
                out.append(input, 0, i);
                appendEscapingHtmlTags(input, i, length, out);
                return out.toString();
            }
            i += Character.charCount(cp);
        }
        return input;
    }

    /**
     * Ensures that embedded CDEnd strings (]]&gt;) are handled properly
     * within message, NDC and throwable tag text.
     *
     * @param buf StringBuilder holding the XML data to this point.  The
     *            initial CDStart (&lt;![CDATA[) and final CDEnd (]]&gt;) of the CDATA
     *            section are the responsibility of the calling method.
     * @param str The String that is inserted into an existing CDATA Section within buf.
     */
    public static void appendEscapingCData(final StringBuilder buf, final String str) {
        if (str != null) {
            int end = str.indexOf(CDATA_END);
            if (end < 0) {
                appendSanitizedXml10(str, 0, str.length(), buf);
            } else {
                int start = 0;
                while (end > -1) {
                    appendSanitizedXml10(str, start, end, buf);
                    buf.append(CDATA_EMBEDED_END);
                    start = end + CDATA_END_LEN;
                    if (start < str.length()) {
                        end = str.indexOf(CDATA_END, start);
                    } else {
                        return;
                    }
                }
                appendSanitizedXml10(str, start, str.length(), buf);
            }
        }
    }

    /**
     * This method takes a string which may contain JSON reserved chars and
     * escapes them.
     *
     * @param input The text to be converted.
     * @return The input string with the special characters replaced.
     */
    public static String escapeJsonControlCharacters(final String input) {
        // Check if the string is null, zero length or devoid of special characters
        // if so, return what was sent in.

        // TODO: escaped Unicode chars.

        if (Strings.isEmpty(input)
                || (input.indexOf('"') == -1
                        && input.indexOf('\\') == -1
                        && input.indexOf('/') == -1
                        && input.indexOf('\b') == -1
                        && input.indexOf('\f') == -1
                        && input.indexOf('\n') == -1
                        && input.indexOf('\r') == -1
                        && input.indexOf('\t') == -1)) {
            return input;
        }

        final StringBuilder buf = new StringBuilder(input.length() + 6);

        final int len = input.length();
        for (int i = 0; i < len; i++) {
            final char ch = input.charAt(i);
            final String escBs = "\\";
            switch (ch) {
                case '"':
                    buf.append(escBs);
                    buf.append(ch);
                    break;
                case '\\':
                    buf.append(escBs);
                    buf.append(ch);
                    break;
                case '/':
                    buf.append(escBs);
                    buf.append(ch);
                    break;
                case '\b':
                    buf.append(escBs);
                    buf.append('b');
                    break;
                case '\f':
                    buf.append(escBs);
                    buf.append('f');
                    break;
                case '\n':
                    buf.append(escBs);
                    buf.append('n');
                    break;
                case '\r':
                    buf.append(escBs);
                    buf.append('r');
                    break;
                case '\t':
                    buf.append(escBs);
                    buf.append('t');
                    break;
                default:
                    buf.append(ch);
            }
        }
        return buf.toString();
    }

    private static void appendEscapingHtmlTags(final String input, int i, final int length, final StringBuilder buf) {
        while (i < length) {
            final int ch = input.codePointAt(i);
            switch (ch) {
                case '<':
                    buf.append("&lt;");
                    break;
                case '>':
                    buf.append("&gt;");
                    break;
                case '&':
                    buf.append("&amp;");
                    break;
                case '"':
                    buf.append("&quot;");
                    break;
                case '\'':
                    buf.append("&#39;");
                    break;
                default:
                    buf.appendCodePoint(isValidXml10(ch) ? ch : REPLACEMENT_CHAR);
                    break;
            }
            i += Character.charCount(ch);
        }
    }

    private static boolean isHtmlTagCharacter(final int cp) {
        return cp == '<' || cp == '>' || cp == '&' || cp == '"' || cp == '\'';
    }

    private static void appendSanitizedXml10(
            final String input, final int start, final int end, final StringBuilder out) {
        for (int i = start; i < end; ) {
            final int cp = input.codePointAt(i);
            out.appendCodePoint(isValidXml10(cp) ? cp : REPLACEMENT_CHAR);
            i += Character.charCount(cp);
        }
    }

    /**
     * Checks if a code point is valid in XML 1.0.
     *
     * @param codePoint a code point between {@code 0} and {@link Character#MAX_CODE_POINT}
     * @return {@code true} if it is a valid XML 1.0 code point
     */
    private static boolean isValidXml10(final int codePoint) {
        assert codePoint >= 0 && codePoint <= Character.MAX_CODE_POINT;
        // XML 1.0 valid characters (Fifth Edition):
        //   #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]

        // [#x20–#xD7FF] (placed early as a fast path for the most common case)
        return (codePoint >= ' ' && codePoint < Character.MIN_SURROGATE)
                // #x9
                || codePoint == '\t'
                // #xA
                || codePoint == '\n'
                // #xD
                || codePoint == '\r'
                // [#xE000-#xFFFD]
                || (codePoint > Character.MAX_SURROGATE && codePoint <= 0xFFFD)
                // [#x10000-#x10FFFF]
                || codePoint >= Character.MIN_SUPPLEMENTARY_CODE_POINT;
    }
}
