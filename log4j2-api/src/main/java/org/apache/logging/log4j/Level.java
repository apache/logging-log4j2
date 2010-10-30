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
package org.apache.logging.log4j;

/**
 * Levels used for identifying the severity of an event. Levels are organized from most specific to least:<br>
 * OFF<br>    (most specific)
 * FATAL<br>
 * ERROR<br>
 * WARN<br>
 * INFO<br>
 * DEBUG<br>
 * TRACE<br>
 * ALL<br>     (least specific)
 *
 * Typically, configuring a level in a filter or on a logger will cause logging events of that level and those
 * that are more specific to pass through the filter.
 * A special level, ALL, is guaranteed to capture all levels when used in logging configurations.
 * @doubt see LOG4J-41
 */
public enum Level {
    OFF(0), FATAL(1), ERROR(2), WARN(3), INFO(4), DEBUG(5), TRACE(6), ALL(Integer.MAX_VALUE);

    private final int intLevel;

    private Level(int val) {
        intLevel = val;
    }

    /**
     * Convert the string passed as argument to a level. If the
     * conversion fails, then this method returns {@link #DEBUG}.
     *
     * @return the Level associate with the String.
     */
    public static Level toLevel(String sArg) {
        return toLevel(sArg, DEBUG);
    }

    /**
     * Convert the string passed as argument to a level. If the
     * conversion fails, then this method returns the value of
     * <code>defaultLevel</code>.
     */
    public static Level toLevel(String sArg, Level defaultLevel) {
        if (sArg == null) {
            return defaultLevel;
        }

        Level level = valueOf(sArg);
        return (level == null) ? defaultLevel : level;
    }

    /**
     * Compares this level against the level passed as an argument and returns true if this
     * level is the same or more specific.
     *
     * @param level The level to check.
     * @return True if the passed Level is more specific or the same as this Level.
     */
    public boolean isAtLeastAsSpecificAs(Level level) {
        return (intLevel <= level.intLevel);
    }

    /**
     * Compares this level against the level passed as an argument and returns true if this
     * level is the same or more specific.
     *
     * @param level The level to check.
     * @return True if the passed Level is more specific or the same as this Level.
     */
    public boolean isAtLeastAsSpecificAs(int level) {
        return (intLevel <= level);
    }

    /**
     * Compares the specified Level against this one.
     * @param level The level to check.
     * @return True if the passed Level is more specific or the same as this Level.
     */
    public boolean lessOrEqual(Level level) {
        return (intLevel <= level.intLevel);
    }

    /**
     * Compares the specified Level against this one.
     * @param level The level to check.
     * @return True if the passed Level is more specific or the same as this Level.
     */
    public boolean lessOrEqual(int level) {
        return (intLevel <= level);
    }

    public int intLevel() {
        return intLevel;
    }
}
