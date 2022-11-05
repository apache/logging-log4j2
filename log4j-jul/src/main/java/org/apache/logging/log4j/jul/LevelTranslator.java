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

package org.apache.logging.log4j.jul;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * Utility class to convert between JDK Levels and Log4j 2 Levels.
 *
 * @since 2.1
 */
public final class LevelTranslator {

    /**
     * Custom Log4j level corresponding to the {@link java.util.logging.Level#FINEST} logging level. This maps to a
     * level more specific than {@link org.apache.logging.log4j.Level#TRACE}.
     */
    public static final Level FINEST = Level.forName("FINEST", Level.TRACE.intLevel() + 100);

    /**
     * Custom Log4j level corresponding to the {@link java.util.logging.Level#CONFIG} logging level. This maps to a
     * level in between {@link org.apache.logging.log4j.Level#INFO} and {@link org.apache.logging.log4j.Level#DEBUG}.
     */
    public static final Level CONFIG = Level.forName("CONFIG", Level.INFO.intLevel() + 50);

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final LevelConverter LEVEL_CONVERTER;

    static {
        final String levelConverterClassName =
            PropertiesUtil.getProperties().getStringProperty(Constants.LEVEL_CONVERTER_PROPERTY);
        if (levelConverterClassName != null) {
            LevelConverter levelConverter;
            try {
                levelConverter = LoaderUtil.newCheckedInstanceOf(levelConverterClassName, LevelConverter.class);
            } catch (final Exception e) {
                LOGGER.error("Could not create custom LevelConverter [{}].", levelConverterClassName, e);
                levelConverter = new DefaultLevelConverter();
            }
            LEVEL_CONVERTER = levelConverter;
        } else {
            LEVEL_CONVERTER = new DefaultLevelConverter();
        }
    }

    /**
     * Converts a JDK logging Level to a Log4j logging Level.
     *
     * @param level JDK Level to convert, may be null per the JUL specification.
     * @return converted Level or null
     */
    public static Level toLevel(final java.util.logging.Level level) {
        return LEVEL_CONVERTER.toLevel(level);
    }

    /**
     * Converts a Log4j logging Level to a JDK logging Level.
     *
     * @param level Log4j Level to convert.
     * @return converted Level.
     */
    public static java.util.logging.Level toJavaLevel(final Level level) {
        return LEVEL_CONVERTER.toJavaLevel(level);
    }

    private LevelTranslator() {
    }
}
