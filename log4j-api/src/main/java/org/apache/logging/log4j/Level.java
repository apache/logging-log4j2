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

import org.apache.logging.log4j.spi.StandardLevel;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
public final class Level implements Comparable<Level>, Serializable {

    private static final long serialVersionUID = 3077535362528045615L;
    private static final ConcurrentMap<String, Level> levels = new ConcurrentHashMap<String, Level>();

    /**
     * No events will be logged.
     */
    public static final Level OFF;
    
    /**
     * A severe error that will prevent the application from continuing.
     */
    public static final Level FATAL;
    
    /**
     * An error in the application, possibly recoverable.
     */
    public static final Level ERROR;
    
    /**
     * An event that might possible lead to an error.
     */
    public static final Level WARN;
    
    /**
     * An event for informational purposes.
     */
    public static final Level INFO;
    
    /**
     * A general debugging event.
     */
    public static final Level DEBUG;
    
    /**
     * A fine-grained debug message, typically capturing the flow through the application.
     */
    public static final Level TRACE;
    
    /**
     * All events should be logged.
     */
    public static final Level ALL;

    static {
        OFF = new Level("OFF", StandardLevel.OFF.intLevel());
        FATAL = new Level("FATAL", StandardLevel.FATAL.intLevel());
        ERROR = new Level("ERROR", StandardLevel.ERROR.intLevel());
        WARN = new Level("WARN", StandardLevel.WARN.intLevel());
        INFO = new Level("INFO", StandardLevel.INFO.intLevel());
        DEBUG = new Level("DEBUG", StandardLevel.DEBUG.intLevel());
        TRACE = new Level("TRACE", StandardLevel.TRACE.intLevel());
        ALL = new Level("ALL", StandardLevel.ALL.intLevel());
    }

    private final String name;
    private final int intLevel;
    private final StandardLevel standardLevel;

    private Level(String name, int intLevel) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Illegal null Level constant");
        }
        if (intLevel < 0) {
            throw new IllegalArgumentException("Illegal Level int less than zero.");
        }
        this.name = name;
        this.intLevel = intLevel;
        this.standardLevel = StandardLevel.getStandardLevel(intLevel);
        if (levels.putIfAbsent(name, this) != null) {
            throw new IllegalStateException("Level " + name + " has already been defined.");
        }
    }

    public int intLevel() {
        return this.intLevel;
    }

    public StandardLevel getStandardLevel() {
        return standardLevel;
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
     * Retrieves an existing Level or creates on if it didn't previously exist.
     * @param name The name of the level.
     * @param intValue The integer value for the Level. If the level was previously created this value is ignored.
     * @return The Level.
     * @throws java.lang.IllegalArgumentException if the name is null or intValue is less than zero.
     */
    public static Level forName(String name, int intValue) {
        Level level = levels.get(name);
        if (level != null) {
            return level;
        }
        try {
            return new Level(name, intValue);
        } catch (IllegalStateException ex) {
            // The level was added by something else so just return that one.
            return levels.get(name);
        }
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

    /**
     * Return an array of all the Levels that have been registered.
     * @return An array of Levels.
     */
    public static Level[] values() {
        Collection<Level> values = Level.levels.values();
        return values.toArray(new Level[values.size()]);
    }

    /**
     * Return the Level associated with the name.
     * @param name The name of the Level to return.
     * @return The Level.
     * @throws java.lang.IllegalArgumentException if the Level name is not registered.
     */
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
    protected Object readResolve() throws ObjectStreamException {
        return Level.valueOf(this.name);
    }
}

