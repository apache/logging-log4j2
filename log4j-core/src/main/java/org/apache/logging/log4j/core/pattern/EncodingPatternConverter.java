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
package org.apache.logging.log4j.core.pattern;

import static org.apache.logging.log4j.util.Chars.CR;
import static org.apache.logging.log4j.util.Chars.LF;

import java.util.List;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.util.EnglishEnums;
import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.StringBuilders;

/**
 * Converter that encodes the output from a pattern using a specified format. Supported formats include HTML
 * (default) and JSON.
 */
@Plugin(name = "encode", category = PatternConverter.CATEGORY)
@ConverterKeys({"enc", "encode"})
@PerformanceSensitive("allocation")
public final class EncodingPatternConverter extends LogEventPatternConverter {

    private final List<PatternFormatter> formatters;
    private final EscapeFormat escapeFormat;

    /**
     * Private constructor.
     *
     * @param formatters   the PatternFormatters to generate the text to manipulate.
     * @param escapeFormat the escape format strategy to use for encoding output of formatters
     */
    private EncodingPatternConverter(final List<PatternFormatter> formatters, final EscapeFormat escapeFormat) {
        super("encode", "encode");
        this.formatters = formatters;
        this.escapeFormat = escapeFormat;
    }

    @Override
    public boolean handlesThrowable() {
        return formatters != null
                && formatters.stream()
                        .map(PatternFormatter::getConverter)
                        .anyMatch(LogEventPatternConverter::handlesThrowable);
    }

    /**
     * Creates an EncodingPatternConverter using a pattern string and an optional escape format.
     *
     * @param config  the current Configuration
     * @param options first option is the nested pattern format; second option is the escape format (optional)
     * @return instance of pattern converter.
     */
    public static EncodingPatternConverter newInstance(final Configuration config, final String[] options) {
        if (options.length > 2 || options.length == 0) {
            LOGGER.error("Incorrect number of options on escape. Expected 1 or 2, but received {}", options.length);
            return null;
        }
        if (options[0] == null) {
            LOGGER.error("No pattern supplied on escape");
            return null;
        }
        final EscapeFormat escapeFormat = options.length < 2
                ? EscapeFormat.HTML
                : EnglishEnums.valueOf(EscapeFormat.class, options[1], EscapeFormat.HTML);
        final PatternParser parser = PatternLayout.createPatternParser(config);
        final List<PatternFormatter> formatters = parser.parse(options[0]);
        return new EncodingPatternConverter(formatters, escapeFormat);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        final int start = toAppendTo.length();
        for (int i = 0; i < formatters.size(); i++) {
            formatters.get(i).format(event, toAppendTo);
        }
        escapeFormat.escape(toAppendTo, start);
    }

    private enum EscapeFormat {
        HTML {
            @Override
            void escape(final StringBuilder toAppendTo, final int start) {

                // do this in two passes to keep O(n) time complexity

                final int origLength = toAppendTo.length();
                int firstSpecialChar = origLength;

                for (int i = origLength - 1; i >= start; i--) {
                    final char c = toAppendTo.charAt(i);
                    final String escaped = escapeChar(c);
                    if (escaped != null) {
                        firstSpecialChar = i;
                        for (int j = 0; j < escaped.length() - 1; j++) {
                            toAppendTo.append(' '); // make room for the escape sequence
                        }
                    }
                }

                for (int i = origLength - 1, j = toAppendTo.length(); i >= firstSpecialChar; i--) {
                    final char c = toAppendTo.charAt(i);
                    final String escaped = escapeChar(c);
                    if (escaped == null) {
                        toAppendTo.setCharAt(--j, c);
                    } else {
                        toAppendTo.replace(j - escaped.length(), j, escaped);
                        j -= escaped.length();
                    }
                }
            }

            private String escapeChar(final char c) {
                switch (c) {
                    case CR:
                        return "\\r";
                    case LF:
                        return "\\n";
                    case '&':
                        return "&amp;";
                    case '<':
                        return "&lt;";
                    case '>':
                        return "&gt;";
                    case '"':
                        return "&quot;";
                    case '\'':
                        return "&apos;";
                    case '/':
                        return "&#x2F;";
                    default:
                        return null;
                }
            }
        },

        /**
         * JSON string escaping as defined in RFC 4627.
         *
         * @see <a href="https://www.ietf.org/rfc/rfc4627.txt">RFC 4627</a>
         */
        JSON {
            @Override
            void escape(final StringBuilder toAppendTo, final int start) {
                StringBuilders.escapeJson(toAppendTo, start);
            }
        },

        CRLF {
            @Override
            void escape(final StringBuilder toAppendTo, final int start) {

                // do this in two passes to keep O(n) time complexity

                final int origLength = toAppendTo.length();
                int firstSpecialChar = origLength;

                for (int i = origLength - 1; i >= start; i--) {
                    final char c = toAppendTo.charAt(i);
                    if (c == CR || c == LF) {
                        firstSpecialChar = i;
                        toAppendTo.append(' '); // make room for the escape sequence
                    }
                }

                for (int i = origLength - 1, j = toAppendTo.length(); i >= firstSpecialChar; i--) {
                    final char c = toAppendTo.charAt(i);
                    switch (c) {
                        case CR:
                            toAppendTo.setCharAt(--j, 'r');
                            toAppendTo.setCharAt(--j, '\\');
                            break;
                        case LF:
                            toAppendTo.setCharAt(--j, 'n');
                            toAppendTo.setCharAt(--j, '\\');
                            break;
                        default:
                            toAppendTo.setCharAt(--j, c);
                    }
                }
            }
        },

        /**
         * XML string escaping as defined in XML specification.
         *
         * @see <a href="https://www.w3.org/TR/xml/">XML specification</a>
         */
        XML {
            @Override
            void escape(final StringBuilder toAppendTo, final int start) {
                StringBuilders.escapeXml(toAppendTo, start);
            }
        };

        /**
         * Escapes text using a standardized format from a given starting point to the end of the string.
         *
         * @param toAppendTo string buffer to escape
         * @param start      where to start escaping from
         */
        abstract void escape(final StringBuilder toAppendTo, final int start);
    }
}
