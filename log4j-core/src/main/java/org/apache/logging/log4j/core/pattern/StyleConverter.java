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

import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.Patterns;
import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * Style pattern converter. Adds ANSI color styling to the result of the enclosed pattern.
 *
 * <p>
 * To disable ANSI output unconditionally, specify an additional option <code>disableAnsi=true</code>, or to
 * disable ANSI output if no console is detected, specify option <code>noConsoleNoAnsi=true</code>.
 * </p>
 */
@Plugin(name = "style", category = PatternConverter.CATEGORY)
@ConverterKeys({"style"})
@PerformanceSensitive("allocation")
public final class StyleConverter extends LogEventPatternConverter implements AnsiConverter {

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
        if (options == null) {
            return null;
        }
        if (options.length < 2) {
            LOGGER.error("Incorrect number of options on style. Expected at least 1, received " + options.length);
            return null;
        }
        if (options[0] == null) {
            LOGGER.error("No pattern supplied for style converter");
            return null;
        }
        if (options[1] == null) {
            LOGGER.error("No style attributes supplied for style converter");
            return null;
        }
        final PatternParser parser = PatternLayout.createPatternParser(config);
        final List<PatternFormatter> formatters = parser.parse(options[0]);
        final String style = AnsiEscape.createSequence(options[1].split(Patterns.COMMA_SPACE_SEPARATOR));
        final boolean disableAnsi = Arrays.toString(options).contains(PatternParser.DISABLE_ANSI + "=true");
        final boolean noConsoleNoAnsi = Arrays.toString(options).contains(PatternParser.NO_CONSOLE_NO_ANSI + "=true");
        final boolean hideAnsi = disableAnsi || (noConsoleNoAnsi && System.console() == null);
        return new StyleConverter(formatters, style, hideAnsi);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        int start = 0;
        int end = 0;
        if (!noAnsi) { // use ANSI: set prefix
            start = toAppendTo.length();
            toAppendTo.append(style);
            end = toAppendTo.length();
        }

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, size = patternFormatters.size(); i < size; i++) {
            patternFormatters.get(i).format(event, toAppendTo);
        }

        // if we use ANSI we need to add the postfix or erase the unnecessary prefix
        if (!noAnsi) {
            if (toAppendTo.length() == end) {
                toAppendTo.setLength(start); // erase prefix
            } else {
                toAppendTo.append(defaultStyle); // add postfix
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
