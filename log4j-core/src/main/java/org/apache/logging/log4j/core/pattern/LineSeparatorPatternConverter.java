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
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.Strings;

/**
 * Formats a line separator.
 */
@Plugin(name = "LineSeparatorPatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"n"})
@PerformanceSensitive("allocation")
public final class LineSeparatorPatternConverter extends LogEventPatternConverter {

    /**
     * Singleton.
     */
    private static final LineSeparatorPatternConverter INSTANCE = new LineSeparatorPatternConverter();

    /**
     * Private constructor.
     */
    private LineSeparatorPatternConverter() {
        super("Line Sep", "lineSep");
    }

    /**
     * Obtains an instance of pattern converter.
     *
     * @param options
     *        options, may be null.
     * @return instance of pattern converter.
     */
    public static LineSeparatorPatternConverter newInstance(final String[] options) {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent ignored, final StringBuilder toAppendTo) {
        toAppendTo.append(Strings.LINE_SEPARATOR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final Object ignored, final StringBuilder output) {
        output.append(Strings.LINE_SEPARATOR);
    }

    @Override
    public boolean isVariable() {
        return false;
    }
}
