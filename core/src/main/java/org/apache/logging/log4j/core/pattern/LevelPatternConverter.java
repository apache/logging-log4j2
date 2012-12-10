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
import org.apache.logging.log4j.core.config.plugins.Plugin;

import java.util.EnumMap;

/**
 * Returns the event's level in a StringBuffer.
 */
@Plugin(name = "LevelPatternConverter", type = "Converter")
@ConverterKeys({"p", "level" })
public final class LevelPatternConverter extends LogEventPatternConverter {
    /**
     * Singleton.
     */
    private static final LevelPatternConverter INSTANCE = new LevelPatternConverter(null);

    private final EnumMap<Level, String> levelMap;

    /**
     * Private constructor.
     */
    private LevelPatternConverter(final EnumMap<Level, String> map) {
        super("Level", "level");
        this.levelMap = map;
    }

    /**
     * Obtains an instance of pattern converter.
     *
     * @param options options, may be null. May contain a list of level names and
     * The value that should be displayed for the Level.
     * @return instance of pattern converter.
     */
    public static LevelPatternConverter newInstance(final String[] options) {
        if (options == null || options.length == 0) {
            return INSTANCE;
        }
        final EnumMap<Level, String> levelMap = new EnumMap<Level, String>(Level.class);
        final String[] definitions = options[0].split(",");
        for (final String def : definitions) {
            final String[] pair = def.split("=");
            if (pair == null || pair.length != 2) {
                LOGGER.error("Invalid option {}", def);
                continue;
            }
            final Level level = Level.toLevel(pair[0].trim(), null);
            if (level == null) {
                LOGGER.error("Invalid Level {}", pair[0].trim());
            }
            levelMap.put(level, pair[1].trim());
        }
        if (levelMap.size() == 0) {
            return INSTANCE;
        }
        for (final Level level : Level.values()) {
            if (!levelMap.containsKey(level)) {
                levelMap.put(level, level.toString());
            }
        }
        return new LevelPatternConverter(levelMap);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder output) {
        output.append(levelMap == null ? event.getLevel().toString() : levelMap.get(event.getLevel()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStyleClass(final Object e) {
        if (e instanceof LogEvent) {
            final Level level = ((LogEvent) e).getLevel();

            switch (level) {
                case TRACE:
                    return "level trace";

                case DEBUG:
                    return "level debug";

                case INFO:
                    return "level info";

                case WARN:
                    return "level warn";

                case ERROR:
                    return "level error";

                case FATAL:
                    return "level fatal";

                default:
                    return "level " + ((LogEvent) e).getLevel().toString();
            }
        }

        return "level";
    }
}
