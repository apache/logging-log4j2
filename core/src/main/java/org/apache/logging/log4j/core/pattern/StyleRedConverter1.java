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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.util.List;

/**
 * Style pattern converter. Adds ANSI color styling to the result of the enclosed pattern.
 */
@Plugin(name = "red", type = "Converter")
@ConverterKeys("red")
public final class StyleRedConverter1 extends LogEventPatternConverter {

    private final List<PatternFormatter> formatters;

    private final String style;

    /**
     * Constructs the converter.
     * 
     * @param formatters
     *            The PatternFormatters to generate the text to manipulate.
     * @param styling
     *            The styling that should encapsulate the pattern.
     */
    private StyleRedConverter1(List<PatternFormatter> formatters, String styling) {
        super("red", "style");
        this.formatters = formatters;
        this.style = styling;
    }

    /**
     * Gets an instance of the class.
     * 
     * @param config
     *            The current Configuration.
     * @param options
     *            pattern options, may be null. If first element is "short", only the first line of the throwable will be formatted.
     * @return instance of class.
     */
    public static StyleRedConverter1 newInstance(Configuration config, final String[] options) {
        if (options[0] == null) {
            LOGGER.error("No pattern supplied on style");
            return null;
        }
        PatternParser parser = PatternLayout.createPatternParser(config);
        List<PatternFormatter> formatters = parser.parse(options[0]);
        String style = AnsiEscape.createSequence("red");
        return new StyleRedConverter1(formatters, style);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        StringBuilder buf = new StringBuilder();
        for (PatternFormatter formatter : formatters) {
            formatter.format(event, buf);
        }
        if (buf.length() > 0) {
            toAppendTo.append(style).append(buf.toString()).append(AnsiEscape.getDefaultStyle());
        }
    }
}
