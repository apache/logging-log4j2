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
import java.util.regex.Pattern;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * Replacement pattern converter.
 */
@Plugin(name = "replace", category = PatternConverter.CATEGORY)
@ConverterKeys({"replace"})
public final class RegexReplacementConverter extends LogEventPatternConverter {

    private final Pattern pattern;

    private final String substitution;

    private final List<PatternFormatter> formatters;

    /**
     * Construct the converter.
     * @param formatters The PatternFormatters to generate the text to manipulate.
     * @param pattern The regular expression Pattern.
     * @param substitution The substitution string.
     */
    private RegexReplacementConverter(
            final List<PatternFormatter> formatters, final Pattern pattern, final String substitution) {
        super("replace", "replace");
        this.pattern = pattern;
        this.substitution = substitution;
        this.formatters = formatters;
    }

    /**
     * Gets an instance of the class.
     *
     * @param config The current Configuration.
     * @param options pattern options, may be null.  If first element is "short",
     *                only the first line of the throwable will be formatted.
     * @return instance of class.
     */
    public static RegexReplacementConverter newInstance(final Configuration config, final String[] options) {
        if (options.length != 3) {
            LOGGER.error("Incorrect number of options on replace. Expected 3 received " + options.length);
            return null;
        }
        if (options[0] == null) {
            LOGGER.error("No pattern supplied on replace");
            return null;
        }
        if (options[1] == null) {
            LOGGER.error("No regular expression supplied on replace");
            return null;
        }
        if (options[2] == null) {
            LOGGER.error("No substitution supplied on replace");
            return null;
        }
        final Pattern p = Pattern.compile(options[1]);
        final PatternParser parser = PatternLayout.createPatternParser(config);
        final List<PatternFormatter> formatters = parser.parse(options[0]);
        return new RegexReplacementConverter(formatters, p, options[2]);
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
        toAppendTo.append(pattern.matcher(buf.toString()).replaceAll(substitution));
    }
}
