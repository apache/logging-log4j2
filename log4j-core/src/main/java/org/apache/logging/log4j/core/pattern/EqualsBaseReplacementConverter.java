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
import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * Equals pattern converter.
 */
@PerformanceSensitive("allocation")
public abstract class EqualsBaseReplacementConverter extends LogEventPatternConverter {
    private final List<PatternFormatter> formatters;
    private final List<PatternFormatter> substitutionFormatters;
    private final String substitution;
    private final String testString;

    /**
     * Construct the converter.
     *
     * @param name          converter name
     * @param style         converter style
     * @param formatters   The PatternFormatters to generate the text to manipulate.
     * @param testString   The test string.
     * @param substitution The substitution string.
     * @param parser        The PatternParser.
     */
    protected EqualsBaseReplacementConverter(
            final String name,
            final String style,
            final List<PatternFormatter> formatters,
            final String testString,
            final String substitution,
            final PatternParser parser) {
        super(name, style);
        this.testString = testString;
        this.substitution = substitution;
        this.formatters = formatters;

        // check if substitution needs to be parsed
        substitutionFormatters = substitution.contains("%") ? parser.parse(substitution) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        final int initialSize = toAppendTo.length();
        for (int i = 0; i < formatters.size(); i++) {
            final PatternFormatter formatter = formatters.get(i);
            formatter.format(event, toAppendTo);
        }
        if (equals(testString, toAppendTo, initialSize, toAppendTo.length() - initialSize)) {
            toAppendTo.setLength(initialSize);
            parseSubstitution(event, toAppendTo);
        }
    }

    /**
     * Returns true if the specified String equals the specified section of the specified StringBuilder.
     *
     * @param str the String to compare
     * @param buff the StringBuilder to compare a section of
     * @param from start index in the StringBuilder
     * @param len length of the section in the StringBuilder
     * @return true if equal, false otherwise
     */
    protected abstract boolean equals(String str, StringBuilder buff, int from, int len);

    /**
     * Adds the parsed substitution text to the specified buffer.
     *
     * @param event the current log event
     * @param substitutionBuffer the StringBuilder to append the parsed substitution text to
     */
    void parseSubstitution(final LogEvent event, final StringBuilder substitutionBuffer) {
        if (substitutionFormatters != null) {
            for (int i = 0; i < substitutionFormatters.size(); i++) {
                final PatternFormatter formatter = substitutionFormatters.get(i);
                formatter.format(event, substitutionBuffer);
            }
        } else {
            substitutionBuffer.append(substitution);
        }
    }
}
