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
package org.apache.logging.log4j.jul.internal;

import static org.apache.logging.log4j.Level.ALL;
import static org.apache.logging.log4j.Level.DEBUG;
import static org.apache.logging.log4j.Level.ERROR;
import static org.apache.logging.log4j.Level.FATAL;
import static org.apache.logging.log4j.Level.INFO;
import static org.apache.logging.log4j.Level.OFF;
import static org.apache.logging.log4j.Level.TRACE;
import static org.apache.logging.log4j.Level.WARN;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.Level;

/**
 * Default implementation of LevelConverter strategy.
 * <p>
 * Since 2.4, supports custom JUL levels by mapping them to their closest mapped neighbour.
 * </p>
 *
 * @since 2.1
 */
public final class LevelConverter {

    /**
     * Custom Log4j level corresponding to the {@link java.util.logging.Level#FINEST} logging level.
     */
    static final Level FINEST = Level.forName("FINEST", TRACE.intLevel() + 100);

    /**
     * Custom Log4j level corresponding to the {@link java.util.logging.Level#CONFIG} logging level.
     */
    static final Level CONFIG = Level.forName("CONFIG", INFO.intLevel() + 50);

    /**
     * Custom JUL level corresponding to the {@link Level#FATAL} logging loevel
     */
    private static final java.util.logging.Level JUL_FATAL =
            new CustomJulLevel("FATAL", java.util.logging.Level.SEVERE.intValue() + 100);

    private static final Map<java.util.logging.Level, Level> julToLog4j = new ConcurrentHashMap<>();
    private static final Map<Level, java.util.logging.Level> log4jToJul = new ConcurrentHashMap<>();

    static {
        // Predefined levels sorted from most severe to least severe.
        Map<Level, java.util.logging.Level> log4jtoJul = Map.of(
                OFF,
                java.util.logging.Level.OFF,
                FATAL,
                JUL_FATAL,
                ERROR,
                java.util.logging.Level.SEVERE,
                WARN,
                java.util.logging.Level.WARNING,
                INFO,
                java.util.logging.Level.INFO,
                CONFIG,
                java.util.logging.Level.CONFIG,
                DEBUG,
                java.util.logging.Level.FINE,
                TRACE,
                java.util.logging.Level.FINER,
                FINEST,
                java.util.logging.Level.FINEST,
                ALL,
                java.util.logging.Level.ALL);
        // Map Log4j to JUL
        LevelConverter.log4jToJul.putAll(log4jtoJul);
        // Map JUL to Log4j
        // SEVERE will be mapped to ERROR.
        for (Map.Entry<Level, java.util.logging.Level> entry : log4jtoJul.entrySet()) {
            LevelConverter.julToLog4j.put(entry.getValue(), entry.getKey());
        }
    }

    private LevelConverter() {}

    static int log4jToJulIntLevel(int log4jLevel) {
        if (log4jLevel <= OFF.intLevel()) {
            return java.util.logging.Level.OFF.intValue();
        }
        // From OFF to INFO: normal pace
        if (log4jLevel <= INFO.intLevel()) {
            return java.util.logging.Level.INFO.intValue() + (INFO.intLevel() - log4jLevel);
        }
        // From INFO to CONFIG: double pace
        if (log4jLevel <= LevelConverter.CONFIG.intLevel()) {
            return java.util.logging.Level.CONFIG.intValue() + 2 * (LevelConverter.CONFIG.intLevel() - log4jLevel);
        }
        // From CONFIG to DEBUG: quadruple pace
        if (log4jLevel <= DEBUG.intLevel()) {
            return java.util.logging.Level.FINE.intValue() + 4 * (DEBUG.intLevel() - log4jLevel);
        }
        // Above DEBUG we have:
        // * Integer.MAX_VALUE - 500 Log4j levels
        // * Integer.MAX_VALUE + 401 JUL levels
        // So every Log4j level will have a JUL level
        if (log4jLevel != ALL.intLevel()) {
            return java.util.logging.Level.FINE.intValue() + (DEBUG.intLevel() - log4jLevel);
        }
        return ALL.intLevel();
    }

    static int julToLog4jIntLevel(int julLevel) {
        // No Log4j level is mapped below this value
        if (julLevel <= java.util.logging.Level.FINE.intValue() + (DEBUG.intLevel() - ALL.intLevel())) {
            return ALL.intLevel();
        }
        // From ALL to FINE: normal pace
        if (julLevel <= java.util.logging.Level.FINE.intValue()) {
            return DEBUG.intLevel() + (java.util.logging.Level.FINE.intValue() - julLevel);
        }
        // From FINE to CONFIG: 1/4 of the pace
        if (julLevel <= java.util.logging.Level.CONFIG.intValue()) {
            return CONFIG.intLevel() + (java.util.logging.Level.CONFIG.intValue() - julLevel) / 4;
        }
        // From CONFIG to INFO: 1/2 of the pace
        if (julLevel <= java.util.logging.Level.INFO.intValue()) {
            return INFO.intLevel() + (java.util.logging.Level.INFO.intValue() - julLevel) / 2;
        }
        // From INFO to OFF: normal pace
        if (julLevel <= java.util.logging.Level.INFO.intValue() + (INFO.intLevel() - 1)) {
            return INFO.intLevel() + (java.util.logging.Level.INFO.intValue() - julLevel);
        }
        return OFF.intLevel();
    }

    public static java.util.logging.Level toJulLevel(final Level log4jLevel) {
        return log4jLevel != null
                ? log4jToJul.computeIfAbsent(
                        log4jLevel, l -> new CustomJulLevel(l.name(), log4jToJulIntLevel(l.intLevel())))
                : null;
    }

    public static Level toLog4jLevel(final java.util.logging.Level julLevel) {
        return julLevel != null
                ? julToLog4j.computeIfAbsent(
                        julLevel, l -> Level.forName(l.getName(), julToLog4jIntLevel(l.intValue())))
                : null;
    }

    private static class CustomJulLevel extends java.util.logging.Level {

        private CustomJulLevel(String name, int value) {
            super(name, value);
        }
    }
}
