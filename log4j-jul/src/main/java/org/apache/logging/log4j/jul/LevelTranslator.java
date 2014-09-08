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

import java.util.IdentityHashMap;
import java.util.Map;

import org.apache.logging.log4j.Level;

/**
 * Utility class to convert between JDK Levels and Log4j 2 Levels.
 *
 * @since 2.1
 */
public final class LevelTranslator {

    private static final int JDK_SEVERE = java.util.logging.Level.SEVERE.intValue();    // ERROR
    private static final int JDK_WARNING = java.util.logging.Level.WARNING.intValue();  // WARN
    private static final int JDK_INFO = java.util.logging.Level.INFO.intValue();        // INFO
    private static final int JDK_CONFIG = java.util.logging.Level.CONFIG.intValue();    // INFO
    private static final int JDK_FINE = java.util.logging.Level.FINE.intValue();        // DEBUG
    private static final int JDK_FINER = java.util.logging.Level.FINER.intValue();      // DEBUG
    private static final int JDK_FINEST = java.util.logging.Level.FINEST.intValue();    // TRACE

    // standard level mappings
    private static final Map<java.util.logging.Level, Level> JDK_TO_LOG4J =
        new IdentityHashMap<java.util.logging.Level, Level>(10);
    private static final Map<Level, java.util.logging.Level> LOG4J_TO_JDK =
        new IdentityHashMap<Level, java.util.logging.Level>(10);

    static {
        JDK_TO_LOG4J.put(java.util.logging.Level.OFF, Level.OFF);
        JDK_TO_LOG4J.put(java.util.logging.Level.FINEST, Level.TRACE);
        JDK_TO_LOG4J.put(java.util.logging.Level.FINER, Level.DEBUG);
        JDK_TO_LOG4J.put(java.util.logging.Level.FINE, Level.DEBUG);
        JDK_TO_LOG4J.put(java.util.logging.Level.CONFIG, Level.INFO);
        JDK_TO_LOG4J.put(java.util.logging.Level.INFO, Level.INFO);
        JDK_TO_LOG4J.put(java.util.logging.Level.WARNING, Level.WARN);
        JDK_TO_LOG4J.put(java.util.logging.Level.SEVERE, Level.ERROR);
        JDK_TO_LOG4J.put(java.util.logging.Level.ALL, Level.ALL);
        LOG4J_TO_JDK.put(Level.OFF, java.util.logging.Level.OFF);
        LOG4J_TO_JDK.put(Level.TRACE, java.util.logging.Level.FINEST);
        LOG4J_TO_JDK.put(Level.DEBUG, java.util.logging.Level.FINE);
        LOG4J_TO_JDK.put(Level.INFO, java.util.logging.Level.INFO);
        LOG4J_TO_JDK.put(Level.WARN, java.util.logging.Level.WARNING);
        LOG4J_TO_JDK.put(Level.ERROR, java.util.logging.Level.SEVERE);
        LOG4J_TO_JDK.put(Level.ALL, java.util.logging.Level.ALL);
    }

    /**
     * Converts a JDK logging Level to a Log4j logging Level.
     *
     * @param level JDK Level to convert.
     * @return converted Level.
     */
    public static Level toLevel(final java.util.logging.Level level) {
        final Level standardLevel = JDK_TO_LOG4J.get(level);
        if (standardLevel != null) {
            return standardLevel;
        }
        final int value = level.intValue();
        if (value == Integer.MAX_VALUE) {
            return Level.OFF;
        }
        if (value == Integer.MIN_VALUE) {
            return Level.ALL;
        }
        if (value <= JDK_FINEST) { // up to 300
            return Level.TRACE;
        }
        if (value <= JDK_FINER) { // 301 to 400
            return Level.DEBUG;
        }
        if (value <= JDK_FINE) { // 401 to 500
            return Level.DEBUG;
        }
        if (value <= JDK_CONFIG) { // 501 to 700
            return Level.INFO;
        }
        if (value <= JDK_INFO) { // 701 to 800
            return Level.INFO;
        }
        if (value <= JDK_WARNING) { // 801 to 900
            return Level.WARN;
        }
        if (value <= JDK_SEVERE) { // 901 to 1000
            return Level.ERROR;
        }
        // 1001+
        return Level.FATAL;
    }

    /**
     * Converts a Log4j logging Level to a JDK logging Level.
     *
     * @param level Log4j Level to convert.
     * @return converted Level.
     */
    public static java.util.logging.Level toJavaLevel(final Level level) {
        final java.util.logging.Level standardLevel = LOG4J_TO_JDK.get(level);
        if (standardLevel != null) {
            return standardLevel;
        }
        return java.util.logging.Level.parse(level.name());
    }

    private LevelTranslator() {
    }
}
