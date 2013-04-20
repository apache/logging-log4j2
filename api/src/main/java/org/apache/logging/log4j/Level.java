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

import java.util.Locale;

/**
 * Levels used for identifying the severity of an event. Levels are organized from most specific to least:
 * <ul>
 * <li>{@link #OFF} (most specific)</li>
 * <li>{@link #FATAL}</li>
 * <li>{@link #ERROR}</li>
 * <li>{@link #WARN}</li>
 * <li>{@link #INFO}</li>
 * <li>{@link #DEBUG}</li>
 * <li>{@link #TRACE}</li>
 * <li>{@link #ALL} (least specific)</li>
 * </ul>
 *
 * Typically, configuring a level in a filter or on a logger will cause logging events of that level and those
 * that are more specific to pass through the filter.
 * A special level, {@link #ALL}, is guaranteed to capture all levels when used in logging configurations.
 */
public enum Level {

    /**
     * No events will be logged.
     */
    OFF(0),

    /**
     * A severe error that will prevent the application from continuing.
     */
    FATAL(1),

    /**
     * An error in the application, possibly recoverable.
     */
    ERROR(2),

    /**
     * An event that might possible lead to an error.
     */
    WARN(3),

    /**
     * An event for informational purposes.
     */
    INFO(4),

    /**
     * A general debugging event.
     */
    DEBUG(5),

    /**
     * A fine-grained debug message, typically capturing the flow through the application.
     */
    TRACE(6),

    /**
     * All events should be logged.
     */
    ALL(Integer.MAX_VALUE);

    private final int intLevel;

    private Level(final int val) {
        intLevel = val;
    }

    /**
     * Converts the string passed as argument to a level. If the
     * conversion fails, then this method returns {@link #DEBUG}.
     *
     * @param sArg The name of the desired Level.
     * @return The Level associated with the String.
     */
    public static Level toLevel(final String sArg) {
        return toLevel(sArg, DEBUG);
    }

    /**
     * Converts the string passed as argument to a level. If the
     * conversion fails, then this method returns the value of
     * <code>defaultLevel</code>.
     *
     * @param name The name of the desired Level.
     * @param defaultLevel The Level to use if the String is invalid.
     * @return The Level associated with the String.
     */
    public static Level toLevel(final String name, final Level defaultLevel) {
        if (name == null) {
            return defaultLevel;
        }
        final String cleanLevel = name.toUpperCase(Locale.ENGLISH);
        for (final Level level : values()) {
            if (level.name().equals(cleanLevel)) {
                return level;
            }
        }
        return defaultLevel;
    }

    /**
     * Compares this level against the level passed as an argument and returns true if this
     * level is the same or more specific.
     *
     * @param level The level to check.
     * @return True if the passed Level is more specific or the same as this Level.
     */
    public boolean isAtLeastAsSpecificAs(final Level level) {
        return intLevel <= level.intLevel;
    }

    /**
     * Compares this level against the level passed as an argument and returns true if this
     * level is the same or more specific.
     *
     * @param level The level to check.
     * @return True if the passed Level is more specific or the same as this Level.
     */
    public boolean isAtLeastAsSpecificAs(final int level) {
        return intLevel <= level;
    }

    /**
     * Compares the specified Level against this one.
     * @param level The level to check.
     * @return True if the passed Level is more specific or the same as this Level.
     */
    public boolean lessOrEqual(final Level level) {
        return intLevel <= level.intLevel;
    }

    /**
     * Compares the specified Level against this one.
     * @param level The level to check.
     * @return True if the passed Level is more specific or the same as this Level.
     */
    public boolean lessOrEqual(final int level) {
        return intLevel <= level;
    }

    /**
     * Returns the integer value of the Level.
     * @return the integer value of the Level.
     */
    public int intLevel() {
        return intLevel;
    }
}
