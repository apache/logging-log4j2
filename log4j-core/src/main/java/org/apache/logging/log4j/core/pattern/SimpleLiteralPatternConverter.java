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
import org.apache.logging.log4j.core.util.OptionConverter;
import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * Formats a string literal without substitution.
 *
 * This is an effectively-sealed internal type.
 */
@PerformanceSensitive("allocation")
abstract class SimpleLiteralPatternConverter extends LogEventPatternConverter implements ArrayPatternConverter {

    private SimpleLiteralPatternConverter() {
        super("SimpleLiteral", "literal");
    }

    static LogEventPatternConverter of(final String literal, final boolean convertBackslashes) {
        final String value = convertBackslashes ? OptionConverter.convertSpecialChars(literal) : literal;
        return of(value);
    }

    static LogEventPatternConverter of(final String literal) {
        if (literal == null || literal.isEmpty()) {
            return Noop.INSTANCE;
        }
        if (" ".equals(literal)) {
            return Space.INSTANCE;
        }
        return new StringValue(literal);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void format(final LogEvent ignored, final StringBuilder output) {
        format(output);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void format(final Object ignored, final StringBuilder output) {
        format(output);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void format(final StringBuilder output, final Object... args) {
        format(output);
    }

    abstract void format(final StringBuilder output);

    @Override
    public final boolean isVariable() {
        return false;
    }

    @Override
    public final boolean handlesThrowable() {
        return false;
    }

    private static final class Noop extends SimpleLiteralPatternConverter {
        private static final Noop INSTANCE = new Noop();

        @Override
        void format(final StringBuilder output) {
            // no-op
        }
    }

    private static final class Space extends SimpleLiteralPatternConverter {
        private static final Space INSTANCE = new Space();

        @Override
        void format(final StringBuilder output) {
            output.append(' ');
        }
    }

    private static final class StringValue extends SimpleLiteralPatternConverter {

        private final String literal;

        StringValue(final String literal) {
            this.literal = literal;
        }

        @Override
        void format(final StringBuilder output) {
            output.append(literal);
        }
    }
}
