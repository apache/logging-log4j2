/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.logging.log4j.jdk;

import org.apache.logging.log4j.Level;

/**
 * Utility class to convert between JDK Levels and Log4j 2 Levels.
 *
 * @since 2.1
 */
public final class Levels {

    private static final int JDK_OFF = java.util.logging.Level.OFF.intValue();          // OFF
    private static final int JDK_SEVERE = java.util.logging.Level.SEVERE.intValue();    // ERROR
    private static final int JDK_WARNING = java.util.logging.Level.WARNING.intValue();  // WARN
    private static final int JDK_INFO = java.util.logging.Level.INFO.intValue();        // INFO
    private static final int JDK_CONFIG = java.util.logging.Level.CONFIG.intValue();    // INFO
    private static final int JDK_FINE = java.util.logging.Level.FINE.intValue();        // DEBUG
    private static final int JDK_FINER = java.util.logging.Level.FINER.intValue();      // DEBUG
    private static final int JDK_FINEST = java.util.logging.Level.FINEST.intValue();    // TRACE
    private static final int JDK_ALL = java.util.logging.Level.ALL.intValue();          // ALL

    /**
     * Converts a JDK logging Level to a Log4j logging Level.
     *
     * @param level JDK Level to convert.
     * @return converted Level.
     */
    public static Level toLevel(final java.util.logging.Level level) {
        final int value = level.intValue();
        if (value == JDK_OFF) { // Integer.MAX_VALUE
            return Level.OFF;
        }
        if (value == JDK_ALL) { // Integer.MIN_VALUE
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
        if (level == Level.OFF) {
            return java.util.logging.Level.OFF;
        }
        if (level == Level.TRACE) {
            return java.util.logging.Level.FINEST;
        }
        if (level == Level.DEBUG) {
            return java.util.logging.Level.FINE;
        }
        if (level == Level.INFO) {
            return java.util.logging.Level.INFO;
        }
        if (level == Level.WARN) {
            return java.util.logging.Level.WARNING;
        }
        if (level == Level.ERROR || level == Level.FATAL) {
            return java.util.logging.Level.SEVERE;
        }
        if (level == Level.ALL) {
            return java.util.logging.Level.ALL;
        }
        return java.util.logging.Level.parse(level.name());
    }

    private Levels() {
    }
}
