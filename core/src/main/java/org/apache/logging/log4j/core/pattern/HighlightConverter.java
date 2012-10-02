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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.util.EnumMap;
import java.util.List;

/**
 * Highlight pattern converter. Formats the result of a pattern using a color appropriate
 * for the Level in the LogEvent.
 */
@Plugin(name = "highlight", type = "Converter")
@ConverterKeys({"highlight" })
public final class HighlightConverter extends LogEventPatternConverter {

    private static final EnumMap<Level, String> LEVEL_STYLES = new EnumMap<Level, String>(Level.class);

    private static final String[] FATAL = new String[]{"BLINK", "BRIGHT", "RED"};
    private static final String[] ERROR = new String[] {"BRIGHT", "RED"};
    private static final String[] WARN = new String[] {"RED"};
    private static final String[] INFO = new String[] {"BLUE"};
    private static final String[] DEBUG = null;
    private static final String[] TRACE = null;

    private List<PatternFormatter> formatters;

    static {
        LEVEL_STYLES.put(Level.FATAL, AnsiEscape.createSequence(FATAL));
        LEVEL_STYLES.put(Level.ERROR, AnsiEscape.createSequence(ERROR));
        LEVEL_STYLES.put(Level.WARN, AnsiEscape.createSequence(WARN));
        LEVEL_STYLES.put(Level.INFO, AnsiEscape.createSequence(INFO));
        LEVEL_STYLES.put(Level.DEBUG, AnsiEscape.createSequence(DEBUG));
        LEVEL_STYLES.put(Level.TRACE, AnsiEscape.createSequence(TRACE));
    }

    /**
     * Construct the converter.
     * @param formatters The PatternFormatters to generate the text to manipulate.
     */
    private HighlightConverter(List<PatternFormatter> formatters) {
        super("style", "style");
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
    public static HighlightConverter newInstance(Configuration config, final String[] options) {
        if (options.length < 1) {
            LOGGER.error("Incorrect number of options on style. Expected at least 1, received " + options.length);
            return null;
        }
        if (options[0] == null) {
            LOGGER.error("No pattern supplied on style");
            return null;
        }

        PatternParser parser = PatternLayout.createPatternParser(config);
        List<PatternFormatter> formatters = parser.parse(options[0]);
        return new HighlightConverter(formatters);
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
            toAppendTo.append(LEVEL_STYLES.get(event.getLevel())).append(buf.toString()).
                append(AnsiEscape.getDefaultStyle());
        }
    }
}
