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

/**
 * Equals pattern converter.
 */
@Plugin(name = "equals", category = PatternConverter.CATEGORY)
@ConverterKeys({ "equals" })
public final class EqualsReplacementConverter extends LogEventPatternConverter {

    /**
     * Gets an instance of the class.
     *
     * @param config
     *            The current Configuration.
     * @param options
     *            pattern options, an array of three elements: pattern, testString, and substitution.
     * @return instance of class.
     */
    public static EqualsReplacementConverter newInstance(final Configuration config, final String[] options) {
        if (options.length != 3) {
            LOGGER.error("Incorrect number of options on equals. Expected 3 received " + options.length);
            return null;
        }
        if (options[0] == null) {
            LOGGER.error("No pattern supplied on equals");
            return null;
        }
        if (options[1] == null) {
            LOGGER.error("No test string supplied on equals");
            return null;
        }
        if (options[2] == null) {
            LOGGER.error("No substitution supplied on equals");
            return null;
        }
        final String p = options[1];
        final PatternParser parser = PatternLayout.createPatternParser(config);
        final List<PatternFormatter> formatters = parser.parse(options[0]);
        return new EqualsReplacementConverter(formatters, p, options[2]);
    }

    private final List<PatternFormatter> formatters;

    private final String substitution;

    private final String testString;

    /**
     * Construct the converter.
     * 
     * @param formatters
     *            The PatternFormatters to generate the text to manipulate.
     * @param testString
     *            The test string.
     * @param substitution
     *            The substitution string.
     */
    private EqualsReplacementConverter(final List<PatternFormatter> formatters, final String testString,
            final String substitution) {
        super("equals", "equals");
        this.testString = testString;
        this.substitution = substitution;
        this.formatters = formatters;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        final StringBuilder buf = new StringBuilder();
        for (final PatternFormatter formatter : formatters) {
            formatter.format(event, buf);
        }
        final String string = buf.toString();
        toAppendTo.append(testString.equals(string) ? substitution : string);
    }
}
