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
package org.apache.logging.log4j.core.pattern;

import java.util.List;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.util.Chars;
import org.apache.logging.log4j.util.EnglishEnums;
import org.apache.logging.log4j.util.PerformanceSensitive;

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
    private EncodingPatternConverter(final List<PatternFormatter> formatters,
                                     final EscapeFormat escapeFormat) {
        super("encode", "encode");
        this.formatters = formatters;
        this.escapeFormat = escapeFormat;
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
            LOGGER.error("Incorrect number of options on escape. Expected 1 or 2, but received {}",
                options.length);
            return null;
        }
        if (options[0] == null) {
            LOGGER.error("No pattern supplied on escape");
            return null;
        }
        final EscapeFormat escapeFormat = options.length < 2 ? EscapeFormat.HTML
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
                for (int i = toAppendTo.length() - 1; i >= start; i--) { // backwards: length may change
                    final char c = toAppendTo.charAt(i);
                    switch (c) {
                        case '\r':
                            toAppendTo.setCharAt(i, '\\');
                            toAppendTo.insert(i + 1, 'r');
                            break;
                        case '\n':
                            toAppendTo.setCharAt(i, '\\');
                            toAppendTo.insert(i + 1, 'n');
                            break;
                        case '&':
                            toAppendTo.setCharAt(i, '&');
                            toAppendTo.insert(i + 1, "amp;");
                            break;
                        case '<':
                            toAppendTo.setCharAt(i, '&');
                            toAppendTo.insert(i + 1, "lt;");
                            break;
                        case '>':
                            toAppendTo.setCharAt(i, '&');
                            toAppendTo.insert(i + 1, "gt;");
                            break;
                        case '"':
                            toAppendTo.setCharAt(i, '&');
                            toAppendTo.insert(i + 1, "quot;");
                            break;
                        case '\'':
                            toAppendTo.setCharAt(i, '&');
                            toAppendTo.insert(i + 1, "apos;");
                            break;
                        case '/':
                            toAppendTo.setCharAt(i, '&');
                            toAppendTo.insert(i + 1, "#x2F;");
                            break;
                    }
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
                for (int i = toAppendTo.length() - 1; i >= start; i--) { // backwards: length may change
                    final char c = toAppendTo.charAt(i);
                    if (Character.isISOControl(c)) {
                        // all iso control characters are in U+00xx
                        toAppendTo.setCharAt(i, '\\');
                        toAppendTo.insert(i + 1, "u0000");
                        toAppendTo.setCharAt(i + 4, Chars.getUpperCaseHex((c & 0xF0) >> 4));
                        toAppendTo.setCharAt(i + 5, Chars.getUpperCaseHex(c & 0xF));
                    } else if (c == '"' || c == '\\') {
                        // only " and \ need to be escaped; other escapes are optional
                        toAppendTo.insert(i, '\\');
                    }
                }
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
