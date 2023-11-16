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
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * VariablesNotEmpty pattern converter.
 */
@Plugin(name = "notEmpty", category = PatternConverter.CATEGORY)
@ConverterKeys({
    "notEmpty",
    "varsNotEmpty",
    "variablesNotEmpty",
})
@PerformanceSensitive("allocation")
public final class VariablesNotEmptyReplacementConverter extends LogEventPatternConverter {

    private final List<PatternFormatter> formatters;

    /**
     * Constructs the converter.
     *
     * @param formatters
     *            The PatternFormatters to generate the text to manipulate.
     */
    private VariablesNotEmptyReplacementConverter(final List<PatternFormatter> formatters) {
        super("notEmpty", "notEmpty");
        this.formatters = formatters;
    }

    /**
     * Gets an instance of the class.
     *
     * @param config
     *            The current Configuration.
     * @param options
     *            pattern options, may be null.
     * @return instance of class.
     */
    public static VariablesNotEmptyReplacementConverter newInstance(
            final Configuration config, final String[] options) {
        if (options.length != 1) {
            LOGGER.error("Incorrect number of options on varsNotEmpty. Expected 1 received " + options.length);
            return null;
        }
        if (options[0] == null) {
            LOGGER.error("No pattern supplied on varsNotEmpty");
            return null;
        }
        final PatternParser parser = PatternLayout.createPatternParser(config);
        final List<PatternFormatter> formatters = parser.parse(options[0]);
        return new VariablesNotEmptyReplacementConverter(formatters);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        final int start = toAppendTo.length();
        boolean allVarsEmpty = true;
        boolean hasVars = false;
        for (int i = 0; i < formatters.size(); i++) {
            final PatternFormatter formatter = formatters.get(i);
            final int formatterStart = toAppendTo.length();
            formatter.format(event, toAppendTo);
            final LogEventPatternConverter converter = formatter.getConverter();
            if (converter.isVariable()) {
                hasVars = true;
                allVarsEmpty = allVarsEmpty
                        && sequenceRegionMatches(toAppendTo, formatterStart, converter.emptyVariableOutput());
            }
        }
        if (!hasVars || allVarsEmpty) {
            toAppendTo.setLength(start); // remove formatter results
        }
    }

    /**
     * @param sequence1 the 1st sequence
     * @param sequence1Offset the start index of the 1st sequence region
     * @param sequence2 the 2nd sequence
     * @return {@code true}, if the pointed region of the 1st sequence matches to the 2nd sequence; {@code false}, otherwise
     */
    private static boolean sequenceRegionMatches(
            final CharSequence sequence1, final int sequence1Offset, final CharSequence sequence2) {
        final boolean lengthMatches = (sequence1.length() - sequence1Offset) == sequence2.length();
        if (!lengthMatches) {
            return false;
        }
        for (int i2 = 0; i2 < sequence2.length(); i2++) {
            final char c2 = sequence2.charAt(i2);
            final int i1 = i2 + sequence1Offset;
            final char c1 = sequence1.charAt(i1);
            if (c2 != c1) {
                return false;
            }
        }
        return true;
    }
}
