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

import static org.apache.logging.log4j.util.Strings.toRootLowerCase;

import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.core.util.Patterns;
import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * Returns the event's level in a StringBuilder.
 */
@Plugin(name = "LevelPatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"p", "level"})
@PerformanceSensitive("allocation")
public class LevelPatternConverter extends LogEventPatternConverter {
    private static final String OPTION_LENGTH = "length";
    private static final String OPTION_LOWER = "lowerCase";

    /**
     * Singleton.
     */
    private static final LevelPatternConverter INSTANCE = new SimpleLevelPatternConverter();

    /**
     * Private constructor.
     */
    private LevelPatternConverter() {
        super("Level", "level");
    }

    /**
     * Obtains an instance of pattern converter.
     *
     * @param options
     *            options, may be null. May contain a list of level names and The value that should be displayed for the
     *            Level.
     * @return instance of pattern converter.
     */
    public static LevelPatternConverter newInstance(final String[] options) {
        if (options == null || options.length == 0) {
            return INSTANCE;
        }
        final Map<Level, String> levelMap = new HashMap<>();
        int length = Integer.MAX_VALUE; // More than the longest level name.
        boolean lowerCase = false;
        final String[] definitions = options[0].split(Patterns.COMMA_SEPARATOR);
        for (final String def : definitions) {
            final String[] pair = def.split("=");
            if (pair == null || pair.length != 2) {
                LOGGER.error("Invalid option {}", def);
                continue;
            }
            final String key = pair[0].trim();
            final String value = pair[1].trim();
            if (OPTION_LENGTH.equalsIgnoreCase(key)) {
                length = Integers.parseInt(value);
            } else if (OPTION_LOWER.equalsIgnoreCase(key)) {
                lowerCase = Boolean.parseBoolean(value);
            } else {
                final Level level = Level.toLevel(key, null);
                if (level == null) {
                    LOGGER.error("Invalid Level {}", key);
                } else {
                    levelMap.put(level, value);
                }
            }
        }
        if (levelMap.isEmpty() && length == Integer.MAX_VALUE && !lowerCase) {
            return INSTANCE;
        }
        for (final Level level : Level.values()) {
            if (!levelMap.containsKey(level)) {
                final String left = left(level, length);
                levelMap.put(level, lowerCase ? toRootLowerCase(left) : left);
            }
        }
        return new LevelMapLevelPatternConverter(levelMap);
    }

    /**
     * Returns the leftmost chars of the level name for the given level.
     *
     * @param level
     *            The level
     * @param length
     *            How many chars to return
     * @return The abbreviated level name, or the whole level name if the {@code length} is greater than the level name
     *         length,
     */
    private static String left(final Level level, final int length) {
        final String string = level.toString();
        if (length >= string.length()) {
            return string;
        }
        return string.substring(0, length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder output) {
        throw new UnsupportedOperationException("Overridden by subclasses");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStyleClass(final Object e) {
        if (e instanceof LogEvent) {
            return "level " + toRootLowerCase(((LogEvent) e).getLevel().name());
        }

        return "level";
    }

    private static final class SimpleLevelPatternConverter extends LevelPatternConverter {

        /**
         * {@inheritDoc}
         */
        @Override
        public void format(final LogEvent event, final StringBuilder output) {
            output.append(event.getLevel());
        }
    }

    private static final class LevelMapLevelPatternConverter extends LevelPatternConverter {

        private final Map<Level, String> levelMap;

        private LevelMapLevelPatternConverter(final Map<Level, String> levelMap) {
            this.levelMap = levelMap;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void format(final LogEvent event, final StringBuilder output) {
            output.append(levelMap.get(event.getLevel()));
        }
    }
}
