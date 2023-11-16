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
package org.apache.log4j.helpers;

import static org.apache.logging.log4j.util.Strings.toRootUpperCase;

import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Level;

/**
 * An extension of the Level class that provides support for java.util.logging Levels.
 */
public class UtilLoggingLevel extends Level {

    /**
     * Serialization version id.
     */
    private static final long serialVersionUID = 909301162611820211L;

    /**
     * Numerical value for SEVERE.
     */
    public static final int SEVERE_INT = 22000;
    /**
     * Numerical value for WARNING.
     */
    public static final int WARNING_INT = 21000;

    // INFO level defined in parent as 20000..no need to redefine here

    /**
     * Numerical value for CONFIG.
     */
    public static final int CONFIG_INT = 14000;

    /**
     * Numerical value for FINE.
     */
    public static final int FINE_INT = 13000;

    /**
     * Numerical value for FINER.
     */
    public static final int FINER_INT = 12000;

    /**
     * Numerical value for FINEST.
     */
    public static final int FINEST_INT = 11000;

    /**
     * Numerical value for UNKNOWN.
     */
    public static final int UNKNOWN_INT = 10000;

    /**
     * SEVERE.
     */
    public static final UtilLoggingLevel SEVERE = new UtilLoggingLevel(SEVERE_INT, "SEVERE", 0);

    /**
     * WARNING.
     */
    public static final UtilLoggingLevel WARNING = new UtilLoggingLevel(WARNING_INT, "WARNING", 4);

    /**
     * INFO.
     */
    // note: we've aligned the int values of the java.util.logging INFO level with log4j's level
    public static final UtilLoggingLevel INFO = new UtilLoggingLevel(INFO_INT, "INFO", 5);

    /**
     * CONFIG.
     */
    public static final UtilLoggingLevel CONFIG = new UtilLoggingLevel(CONFIG_INT, "CONFIG", 6);

    /**
     * FINE.
     */
    public static final UtilLoggingLevel FINE = new UtilLoggingLevel(FINE_INT, "FINE", 7);

    /**
     * FINER.
     */
    public static final UtilLoggingLevel FINER = new UtilLoggingLevel(FINER_INT, "FINER", 8);

    /**
     * FINEST.
     */
    public static final UtilLoggingLevel FINEST = new UtilLoggingLevel(FINEST_INT, "FINEST", 9);

    /**
     * Create new instance.
     *
     * @param level numeric value for level.
     * @param levelStr symbolic name for level.
     * @param syslogEquivalent Equivalent syslog severity.
     */
    protected UtilLoggingLevel(final int level, final String levelStr, final int syslogEquivalent) {
        super(level, levelStr, syslogEquivalent);
    }

    /**
     * Convert an integer passed as argument to a level. If the conversion fails, then this method returns the specified
     * default.
     *
     * @param val numeric value.
     * @param defaultLevel level to be returned if no level matches numeric value.
     * @return matching level or default level.
     */
    public static UtilLoggingLevel toLevel(final int val, final UtilLoggingLevel defaultLevel) {
        switch (val) {
            case SEVERE_INT:
                return SEVERE;

            case WARNING_INT:
                return WARNING;

            case INFO_INT:
                return INFO;

            case CONFIG_INT:
                return CONFIG;

            case FINE_INT:
                return FINE;

            case FINER_INT:
                return FINER;

            case FINEST_INT:
                return FINEST;

            default:
                return defaultLevel;
        }
    }

    /**
     * Gets level matching numeric value.
     *
     * @param val numeric value.
     * @return matching level or UtilLoggerLevel.FINEST if no match.
     */
    public static Level toLevel(final int val) {
        return toLevel(val, FINEST);
    }

    /**
     * Gets list of supported levels.
     *
     * @return list of supported levels.
     */
    public static List getAllPossibleLevels() {
        final ArrayList<UtilLoggingLevel> list = new ArrayList<>();
        list.add(FINE);
        list.add(FINER);
        list.add(FINEST);
        list.add(INFO);
        list.add(CONFIG);
        list.add(WARNING);
        list.add(SEVERE);
        return list;
    }

    /**
     * Get level with specified symbolic name.
     *
     * @param s symbolic name.
     * @return matching level or Level.DEBUG if no match.
     */
    public static Level toLevel(final String s) {
        return toLevel(s, Level.DEBUG);
    }

    /**
     * Get level with specified symbolic name.
     *
     * @param sArg symbolic name.
     * @param defaultLevel level to return if no match.
     * @return matching level or defaultLevel if no match.
     */
    public static Level toLevel(final String sArg, final Level defaultLevel) {
        if (sArg == null) {
            return defaultLevel;
        }

        final String s = toRootUpperCase(sArg);

        if (s.equals("SEVERE")) {
            return SEVERE;
        }

        // if(s.equals("FINE")) return Level.FINE;
        if (s.equals("WARNING")) {
            return WARNING;
        }

        if (s.equals("INFO")) {
            return INFO;
        }

        if (s.equals("CONFIG")) {
            return CONFIG;
        }

        if (s.equals("FINE")) {
            return FINE;
        }

        if (s.equals("FINER")) {
            return FINER;
        }

        if (s.equals("FINEST")) {
            return FINEST;
        }
        return defaultLevel;
    }
}
