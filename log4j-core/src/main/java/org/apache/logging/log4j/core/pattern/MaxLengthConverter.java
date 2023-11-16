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

import java.util.List;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * Max length pattern converter. Limit contained text to a maximum length.
 * On invalid length the default value 100 is used (and an error message is logged).
 * If max length is greater than 20, an abbreviated text will get ellipsis ("...") appended.
 * Example usage (for email subject):
 * {@code "%maxLen{[AppName, ${hostName}, ${web:contextPath}] %p: %c{1} - %m%notEmpty{ =>%ex{short}}}{160}"}
 *
 * @author Thies Wellpott
 */
@Plugin(name = "maxLength", category = PatternConverter.CATEGORY)
@ConverterKeys({"maxLength", "maxLen"})
@PerformanceSensitive("allocation")
public final class MaxLengthConverter extends LogEventPatternConverter {

    /**
     * Gets an instance of the class.
     *
     * @param config  The current Configuration.
     * @param options pattern options, an array of two elements: pattern, max length (defaults to 100 on invalid value).
     * @return instance of class.
     */
    public static MaxLengthConverter newInstance(final Configuration config, final String[] options) {
        if (options.length != 2) {
            LOGGER.error(
                    "Incorrect number of options on maxLength: expected 2 received {}: {}", options.length, options);
            return null;
        }
        if (options[0] == null) {
            LOGGER.error("No pattern supplied on maxLength");
            return null;
        }
        if (options[1] == null) {
            LOGGER.error("No length supplied on maxLength");
            return null;
        }
        final PatternParser parser = PatternLayout.createPatternParser(config);
        final List<PatternFormatter> formatters = parser.parse(options[0]);
        return new MaxLengthConverter(formatters, AbstractAppender.parseInt(options[1], 100));
    }

    private final List<PatternFormatter> formatters;
    private final int maxLength;

    /**
     * Construct the converter.
     *
     * @param formatters The PatternFormatters to generate the text to manipulate.
     * @param maxLength  The max. length of the resulting string. Ellipsis ("...") is appended on shorted string, if greater than 20.
     */
    private MaxLengthConverter(final List<PatternFormatter> formatters, final int maxLength) {
        super("MaxLength", "maxLength");
        this.maxLength = maxLength;
        this.formatters = formatters;
        LOGGER.trace("new MaxLengthConverter with {}", maxLength);
    }

    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        final int initialLength = toAppendTo.length();
        for (int i = 0; i < formatters.size(); i++) {
            final PatternFormatter formatter = formatters.get(i);
            formatter.format(event, toAppendTo);
            if (toAppendTo.length() > initialLength + maxLength) { // stop early
                break;
            }
        }
        if (toAppendTo.length() > initialLength + maxLength) {
            toAppendTo.setLength(initialLength + maxLength);
            if (maxLength > 20) { // only append ellipses if length is not very short
                toAppendTo.append("...");
            }
        }
    }
}
