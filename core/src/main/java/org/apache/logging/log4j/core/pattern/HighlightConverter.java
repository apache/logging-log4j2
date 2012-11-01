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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * Highlight pattern converter. Formats the result of a pattern using a color appropriate for the Level in the LogEvent.
 * <p>
 * For example:
 * </p>
 * 
 * <pre>
 * %highlight{%d{ ISO8601 } [%t] %-5level: %msg%n%throwable}
 * </pre>
 * <p>
 * You can define custom colors for each Level:
 * </p>
 * 
 * <pre>
 * %highlight{%d{ ISO8601 } [%t] %-5level: %msg%n%throwable}{FATAL=red, ERROR=red, WARN=yellow, INFO=green, DEBUG=cyan, TRACE=black}
 * </pre>
 * <p>
 * You can use whitespace around the comma and equal sign. The names in values MUST come from the {@linkplain AnsiEscape} enum, case is
 * normalized to upper-case internally.
 * </p>
 */
@Plugin(name = "highlight", type = "Converter")
@ConverterKeys({ "highlight" })
public final class HighlightConverter extends LogEventPatternConverter {

    private static final String[] DEFAULT_FATAL = new String[] { "BLINK", "BRIGHT", "RED" };
    private static final String[] DEFAULT_ERROR = new String[] { "BRIGHT", "RED" };
    private static final String[] DEFAULT_WARN = new String[] { "RED" };
    private static final String[] DEFAULT_INFO = new String[] { "BLUE" };
    private static final String[] DEFAULT_DEBUG = null;
    private static final String[] DEFAULT_TRACE = null;
    
    private static final EnumMap<Level, String> LEVEL_STYLES_DEFAULT = new EnumMap<Level, String>(Level.class);

    static {
        LEVEL_STYLES_DEFAULT.put(Level.FATAL, AnsiEscape.createSequence(DEFAULT_FATAL));
        LEVEL_STYLES_DEFAULT.put(Level.ERROR, AnsiEscape.createSequence(DEFAULT_ERROR));
        LEVEL_STYLES_DEFAULT.put(Level.WARN, AnsiEscape.createSequence(DEFAULT_WARN));
        LEVEL_STYLES_DEFAULT.put(Level.INFO, AnsiEscape.createSequence(DEFAULT_INFO));
        LEVEL_STYLES_DEFAULT.put(Level.DEBUG, AnsiEscape.createSequence(DEFAULT_DEBUG));
        LEVEL_STYLES_DEFAULT.put(Level.TRACE, AnsiEscape.createSequence(DEFAULT_TRACE));
    }

    /**
     * Creates a level style map given configuration options in {@code option[1]}.
     * <p/>
     * The format of the option string in {@code option[1]} is:
     * 
     * <pre>
     * Level1=Value, Level2=Value, ...
     * </pre>
     * 
     * For example:
     * 
     * <pre>
     * ERROR=red bold, WARN=yellow bold, INFO=green, ...
     * </pre>
     * 
     * You can use whitespace around the comma and equal sign. The names in values MUST come from the {@linkplain AnsiEscape} enum, case is
     * normalized to upper-case internally.
     * 
     * @param options
     *            The second slot can optionally contain the style map.
     * @return a new map
     */
    private static EnumMap<Level, String> createLevelStyleMap(final String[] options) {
        Map<String, String> styles = options.length < 2 ? null : AnsiEscape.createMap(options[1]);
        EnumMap<Level, String> levelStyles = new EnumMap<Level, String>(LEVEL_STYLES_DEFAULT);
        for (Map.Entry<String, String> entry : styles.entrySet()) {
            final Level key = Level.valueOf(entry.getKey());
            if (key == null) {
                LOGGER.error("Unkown level name: " + entry.getKey());
            } else {
                levelStyles.put(key, entry.getValue());
            }
        }
        return levelStyles;
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
        return new HighlightConverter(formatters, createLevelStyleMap(options));
    }

    private final List<PatternFormatter> formatters;

    private final EnumMap<Level, String> levelStyles;

    /**
     * Construct the converter.
     * 
     * @param formatters
     *            The PatternFormatters to generate the text to manipulate.
     */
    private HighlightConverter(List<PatternFormatter> formatters, EnumMap<Level, String> levelStyles) {
        super("style", "style");
        this.formatters = formatters;
        this.levelStyles = levelStyles;
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
            toAppendTo.append(levelStyles.get(event.getLevel())).append(buf.toString()).append(AnsiEscape.getDefaultStyle());
        }
    }
}
