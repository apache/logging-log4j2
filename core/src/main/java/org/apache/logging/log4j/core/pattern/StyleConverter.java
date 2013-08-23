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
 * Style pattern converter. Adds ANSI color styling to the result of the enclosed pattern.
 */
@Plugin(name = "style", category = "Converter")
@ConverterKeys({"style" })
public final class StyleConverter extends LogEventPatternConverter {

    private final List<PatternFormatter> patternFormatters;

    private final String style;

    /**
     * Constructs the converter.
     * @param patternFormatters The PatternFormatters to generate the text to manipulate.
     * @param style The style that should encapsulate the pattern.
     */
    private StyleConverter(final List<PatternFormatter> patternFormatters, final String style) {
        super("style", "style");
        this.patternFormatters = patternFormatters;
        this.style = style;
    }

    /**
     * Gets an instance of the class.
     *
     * @param config The current Configuration.
     * @param options pattern options, may be null.  If first element is "short",
     *                only the first line of the throwable will be formatted.
     * @return instance of class.
     */
    public static StyleConverter newInstance(final Configuration config, final String[] options) {
        if (options.length < 1) {
            LOGGER.error("Incorrect number of options on style. Expected at least 1, received " + options.length);
            return null;
        }
        if (options[0] == null) {
            LOGGER.error("No pattern supplied on style");
            return null;
        }
        if (options[1] == null) {
            LOGGER.error("No style attributes provided");
            return null;
        }

        final PatternParser parser = PatternLayout.createPatternParser(config);
        final List<PatternFormatter> formatters = parser.parse(options[0]);
        final String style = AnsiEscape.createSequence(options[1].split("\\s*,\\s*"));
        return new StyleConverter(formatters, style);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        final StringBuilder buf = new StringBuilder();
        for (final PatternFormatter formatter : patternFormatters) {
            formatter.format(event, buf);
        }

        if (buf.length() > 0) {
            toAppendTo.append(style).append(buf.toString()).append(AnsiEscape.getDefaultStyle());
        }
    }
    
    @Override
    public boolean handlesThrowable() {
        for (final PatternFormatter formatter : patternFormatters) {
            if (formatter .handlesThrowable()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a String suitable for debugging.
     * 
     * @return a String suitable for debugging.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("[style=");
        sb.append(style);
        sb.append(", patternFormatters=");
        sb.append(patternFormatters);
        sb.append("]");
        return sb.toString();
    }

}
