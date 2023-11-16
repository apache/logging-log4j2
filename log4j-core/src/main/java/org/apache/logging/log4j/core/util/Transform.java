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

    private Transform() {}

    /**
     * This method takes a string which may contain HTML tags (ie,
     * &lt;b&gt;, &lt;table&gt;, etc) and replaces any
     * '&lt;',  '&gt;' , '&amp;' or '&quot;'
     * characters with respective predefined entity references.
     *
     * @param input The text to be converted.
     * @return The input string with the special characters replaced.
     */
    public static String escapeHtmlTags(final String input) {
        // Check if the string is null, zero length or devoid of special characters
        // if so, return what was sent in.

        if (Strings.isEmpty(input)
                || (input.indexOf('"') == -1
                        && input.indexOf('&') == -1
                        && input.indexOf('<') == -1
                        && input.indexOf('>') == -1)) {
            return input;
        }

        // Use a StringBuilder in lieu of String concatenation -- it is
        // much more efficient this way.

        final StringBuilder buf = new StringBuilder(input.length() + 6);

        final int len = input.length();
        for (int i = 0; i < len; i++) {
            final char ch = input.charAt(i);
            if (ch > '>') {
                buf.append(ch);
            } else
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
                    default:
                        buf.append(ch);
                        break;
                }
        }
        return buf.toString();
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
                buf.append(str);
            } else {
                int start = 0;
                while (end > -1) {
                    buf.append(str.substring(start, end));
                    buf.append(CDATA_EMBEDED_END);
                    start = end + CDATA_END_LEN;
                    if (start < str.length()) {
                        end = str.indexOf(CDATA_END, start);
                    } else {
                        return;
                    }
                }
                buf.append(str.substring(start));
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
}
