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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.util.OptionConverter;
import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * Formats a string literal.
 */
@PerformanceSensitive("allocation") // except for replacements
public final class LiteralPatternConverter extends LogEventPatternConverter implements ArrayPatternConverter {

    /**
     * String literal.
     */
    private final String literal;

    private final Configuration config;

    private final boolean substitute;

    /**
     * Create a new instance.
     *
     * @param config The Configuration.
     * @param literal string literal.
     * @param convertBackslashes if {@code true}, backslash characters are treated as escape characters and character
     *            sequences like "\" followed by "t" (backslash+t) are converted to special characters like '\t' (tab).
     */
    public LiteralPatternConverter(final Configuration config, final String literal, final boolean convertBackslashes) {
        super("Literal", "literal");
        this.literal = convertBackslashes ? OptionConverter.convertSpecialChars(literal) : literal; // LOG4J2-829
        this.config = config;
        substitute = config != null && containsSubstitutionSequence(literal);
    }

    static boolean containsSubstitutionSequence(final String literal) {
        return literal != null && literal.contains("${");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        toAppendTo.append(substitute ? config.getStrSubstitutor().replace(event, literal) : literal);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final Object obj, final StringBuilder output) {
        output.append(substitute ? config.getStrSubstitutor().replace(literal) : literal);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final StringBuilder output, final Object... objects) {
        output.append(substitute ? config.getStrSubstitutor().replace(literal) : literal);
    }

    public String getLiteral() {
        return literal;
    }

    @Override
    public boolean isVariable() {
        return false;
    }

    @Override
    public String toString() {
        return "LiteralPatternConverter[literal=" + literal + ", config=" + config + ", substitute=" + substitute + "]";
    }
}
