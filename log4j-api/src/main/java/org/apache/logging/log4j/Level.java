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

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.EnumSet;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Level implements Comparable<Level>, Serializable {

    private static final long serialVersionUID = 3077535362528045615L;
    private static ConcurrentMap<String, Level> levels = new ConcurrentHashMap<String, Level>();
    private static Object constructorLock = new Object();
    private static int ordinalCount = 0;

    public static Level OFF = new Level("OFF", 0){};
    public static Level FATAL = new Level("FATAL", 100);
    public static Level ERROR = new Level("ERROR", 200);
    public static Level WARN = new Level("WARN", 300);
    public static Level INFO = new Level("INFO", 400);
    public static Level DEBUG = new Level("DEBUG", 500);
    public static Level TRACE = new Level("TRACE", 600);
    public static Level ALL = new Level("ALL", Integer.MAX_VALUE);

    public enum StdLevel {

        /**
         * No events will be logged.
         */
        OFF(Level.OFF.intLevel()),

        /**
         * A severe error that will prevent the application from continuing.
         */
        FATAL(Level.FATAL.intLevel()),

        /**
         * An error in the application, possibly recoverable.
         */
        ERROR(Level.ERROR.intLevel()),

        /**
         * An event that might possible lead to an error.
         */
        WARN(Level.WARN.intLevel()),

        /**
         * An event for informational purposes.
         */
        INFO(Level.INFO.intLevel()),

        /**
         * A general debugging event.
         */
        DEBUG(Level.DEBUG.intLevel()),

        /**
         * A fine-grained debug message, typically capturing the flow through the application.
         */
        TRACE(Level.TRACE.intLevel()),

        /**
         * All events should be logged.
         */
        ALL(Level.ALL.intLevel());


        private final int intLevel;

        private static final EnumSet<StdLevel> levelSet = EnumSet.allOf(StdLevel.class);

        private StdLevel(final int val) {
            intLevel = val;
        }

        /**
         * Returns the integer value of the Level.
         * @return the integer value of the Level.
         */
        public int intLevel() {
            return intLevel;
        }

        /**
         * Method to convert custom Levels into a StdLevel for conversion to other systems.
         * @param level The Level.
         * @return The StdLevel.
         */
        public static StdLevel getStdLevel(Level level) {
            StdLevel severityLevel = StdLevel.OFF;
            for (StdLevel lvl : levelSet) {
                if (lvl.intLevel() > level.intLevel()) {
                    break;
                }
                severityLevel = lvl;
            }
            return severityLevel;
        }
    }

    private final String name;
    private final int intLevel;
    private final int ordinal;

    protected Level(String name, int intLevel) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Illegal null Level constant");
        }
        if (intLevel < 0) {
            throw new IllegalArgumentException("Illegal Level int less than zero.");
        }
        this.name = name;
        this.intLevel = intLevel;
        synchronized(constructorLock) {
            if (levels.containsKey(name)) {
                Level level = levels.get(name);
                if (level.intLevel() != intLevel) {
                    throw new IllegalArgumentException("Level " + name + " has already been defined.");
                }
                ordinal = level.ordinal;
            } else {
                ordinal = ordinalCount++;
                levels.put(name, this);
            }
        }
    }

    public int intLevel() {
        return this.intLevel;
    }

    public int ordinal() {
        return this.ordinal;
    }

    /**
     * Compares this level against the level passed as an argument and returns true if this
     * level is the same or more specific.
     *
     * @param level The level to check.
     * @return True if the passed Level is more specific or the same as this Level.
     */
    public boolean isAtLeastAsSpecificAs(final Level level) {
        return this.intLevel <= level.intLevel;
    }

    /**
     * Compares this level against the level passed as an argument and returns true if this
     * level is the same or more specific.
     *
     * @param level The level to check.
     * @return True if the passed Level is more specific or the same as this Level.
     */
    public boolean isAtLeastAsSpecificAs(final int level) {
        return this.intLevel <= level;
    }

    /**
     * Compares the specified Level against this one.
     * @param level The level to check.
     * @return True if the passed Level is more specific or the same as this Level.
     */
    public boolean lessOrEqual(final Level level) {
        return this.intLevel <= level.intLevel;
    }

    /**
     * Compares the specified Level against this one.
     * @param level The level to check.
     * @return True if the passed Level is more specific or the same as this Level.
     */
    public boolean lessOrEqual(final int level) {
        return this.intLevel <= level;
    }

    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public Level clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    @Override
    public int compareTo(Level other) {
        return intLevel < other.intLevel ? -1 : (intLevel > other.intLevel ? 1 : 0);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Level && other == this;
    }

    public Class<Level> getDeclaringClass() {
        return Level.class;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }


    public String name() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    /**
     * Return the Level assoicated with the name or null if the Level cannot be found.
     * @param name The name of the Level.
     * @return The Level or null.
     */
    public static Level getLevel(String name) {
        return levels.get(name);
    }

    /**
     * Converts the string passed as argument to a level. If the
     * conversion fails, then this method returns {@link #DEBUG}.
     *
     * @param sArg The name of the desired Level.
     * @return The Level associated with the String.
     */
    public static Level toLevel(final String sArg) {
        return toLevel(sArg, Level.DEBUG);
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
        Level level = levels.get(name.toUpperCase(Locale.ENGLISH));
        return level == null ? defaultLevel : level;
    }

    public static Level[] values() {
        return Level.levels.values().toArray(new Level[Level.levels.size()]);
    }


    public static Level valueOf(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Unknown level constant [" + name + "].");
        }
        name = name.toUpperCase();
        if (levels.containsKey(name)) {
            return levels.get(name);
        }
        throw new IllegalArgumentException("Unknown level constant [" + name + "].");
    }

    public static <T extends Enum<T>> T valueOf(Class<T> enumType, String name) {
        return Enum.valueOf(enumType, name);
    }

    // for deserialization
    protected final Object readResolve() throws ObjectStreamException {
        return Level.valueOf(this.name);
    }
}

