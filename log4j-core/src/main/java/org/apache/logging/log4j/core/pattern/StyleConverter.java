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
import org.apache.logging.log4j.core.util.Patterns;

/**
 * Style pattern converter. Adds ANSI color styling to the result of the enclosed pattern.
 */
@Plugin(name = "style", category = PatternConverter.CATEGORY)
@ConverterKeys({ "style" })
public final class StyleConverter extends LogEventPatternConverter implements AnsiConverter {

    /**
     * Gets an instance of the class.
     *
     * @param config
     *            The current Configuration.
     * @param options
     *            pattern options, may be null. If first element is "short", only the first line of the throwable will
     *            be formatted.
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
        final String style = AnsiEscape.createSequence(options[1].split(Patterns.COMMA_SEPARATOR));
        final boolean noConsoleNoAnsi = options.length > 2
                && (PatternParser.NO_CONSOLE_NO_ANSI + "=true").equals(options[2]);
        final boolean hideAnsi = noConsoleNoAnsi && System.console() == null;
        return new StyleConverter(formatters, style, hideAnsi);
    }

    private final List<PatternFormatter> patternFormatters;

    private final boolean noAnsi;

    private final String style;

    private final String defaultStyle;

    /**
     * Constructs the converter.
     *
     * @param patternFormatters
     *            The PatternFormatters to generate the text to manipulate.
     * @param style
     *            The style that should encapsulate the pattern.
     * @param noAnsi
     *            If true, do not output ANSI escape codes.
     */
    private StyleConverter(final List<PatternFormatter> patternFormatters, final String style, final boolean noAnsi) {
        super("style", "style");
        this.patternFormatters = patternFormatters;
        this.style = style;
        this.defaultStyle = AnsiEscape.getDefaultStyle();
        this.noAnsi = noAnsi;
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
            if (noAnsi) {
                // faster to test and do this than setting style and defaultStyle to empty strings.
                toAppendTo.append(buf.toString());
            } else {
                toAppendTo.append(style).append(buf.toString()).append(defaultStyle);
            }
        }
    }

    @Override
    public boolean handlesThrowable() {
        for (final PatternFormatter formatter : patternFormatters) {
            if (formatter.handlesThrowable()) {
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
        final StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("[style=");
        sb.append(style);
        sb.append(", defaultStyle=");
        sb.append(defaultStyle);
        sb.append(", patternFormatters=");
        sb.append(patternFormatters);
        sb.append(", noAnsi=");
        sb.append(noAnsi);
        sb.append(']');
        return sb.toString();
    }

}
