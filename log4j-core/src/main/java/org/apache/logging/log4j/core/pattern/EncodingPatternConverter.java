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
import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * Returns the event's rendered message in a StringBuilder.
 */
@Plugin(name = "encode", category = PatternConverter.CATEGORY)
@ConverterKeys({ "enc", "encode" })
@PerformanceSensitive("allocation")
public final class EncodingPatternConverter extends LogEventPatternConverter {

    private final List<PatternFormatter> formatters;

    /**
     * Private constructor.
     *
     * @param formatters The PatternFormatters to generate the text to manipulate.
     */
    private EncodingPatternConverter(final List<PatternFormatter> formatters) {
        super("encode", "encode");
        this.formatters = formatters;
    }

    /**
     * Obtains an instance of pattern converter.
     *
     * @param config  The Configuration.
     * @param options options, may be null.
     * @return instance of pattern converter.
     */
    public static EncodingPatternConverter newInstance(final Configuration config, final String[] options) {
        if (options.length != 1) {
            LOGGER.error("Incorrect number of options on escape. Expected 1, received " + options.length);
            return null;
        }
        if (options[0] == null) {
            LOGGER.error("No pattern supplied on escape");
            return null;
        }
        final PatternParser parser = PatternLayout.createPatternParser(config);
        final List<PatternFormatter> formatters = parser.parse(options[0]);
        return new EncodingPatternConverter(formatters);
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
}
