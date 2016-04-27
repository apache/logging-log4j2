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

import java.io.Serializable;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.spi.StandardLevel;
import org.apache.logging.log4j.util.Strings;

/**
 * Levels used for identifying the severity of an event. Levels are organized from most specific to least:
 * <ul>
 * <li>{@link #OFF} (most specific, no logging)</li>
 * <li>{@link #FATAL} (most specific, little data)</li>
 * <li>{@link #ERROR}</li>
 * <li>{@link #WARN}</li>
 * <li>{@link #INFO}</li>
 * <li>{@link #DEBUG}</li>
 * <li>{@link #TRACE} (least specific, a lot of data)</li>
 * <li>{@link #ALL} (least specific, all data)</li>
 * </ul>
 *
 * Typically, configuring a level in a filter or on a logger will cause logging events of that level and those that are
 * more specific to pass through the filter. A special level, {@link #ALL}, is guaranteed to capture all levels when
 * used in logging configurations.
 */
public final class Level implements Comparable<Level>, Serializable {

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

    /**
     * @since 2.1
     */
    public static final String CATEGORY = "Level";

    private static final ConcurrentMap<String, Level> LEVELS = new ConcurrentHashMap<>(); // SUPPRESS CHECKSTYLE

    private static final long serialVersionUID = 1581082L;

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

    private Level(final String name, final int intLevel) {
        if (Strings.isEmpty(name)) {
            throw new IllegalArgumentException("Illegal null or empty Level name.");
        }
        if (intLevel < 0) {
            throw new IllegalArgumentException("Illegal Level int less than zero.");
        }
        this.name = name;
        this.intLevel = intLevel;
        this.standardLevel = StandardLevel.getStandardLevel(intLevel);
        if (LEVELS.putIfAbsent(name, this) != null) {
            throw new IllegalStateException("Level " + name + " has already been defined.");
        }
    }

    /**
     * Gets the integral value of this Level.
     *
     * @return the value of this Level.
     */
    public int intLevel() {
        return this.intLevel;
    }

    /**
     * Gets the standard Level values as an enum.
     *
     * @return an enum of the standard Levels.
     */
    public StandardLevel getStandardLevel() {
        return standardLevel;
    }

    /**
     * Compares this level against the levels passed as arguments and returns true if this level is in between the given
     * levels.
     *
     * @param minLevel The minimum level to test.
     * @param maxLevel The maximum level to test.
     * @return True true if this level is in between the given levels
     * @since 2.4
     */
    public boolean isInRange(final Level minLevel, final Level maxLevel) {
        return this.intLevel >= minLevel.intLevel && this.intLevel <= maxLevel.intLevel;
    }

    /**
     * Compares this level against the level passed as an argument and returns true if this level is the same or is less
     * specific.
     * <p>
     * Concretely, {@link #TRACE} is less specific than {@link #DEBUG}, which is less specific than {@link #INFO},
     * etc., until {@link #FATAL}, and finally {@link #OFF}, which is the most specific standard level.
     * The least specific level is {@link #ALL}.
     * </p>
     *
     * @param level The level to test.
     * @return True if this level Level is less specific or the same as the given Level.
     */
    public boolean isLessSpecificThan(final Level level) {
        return this.intLevel >= level.intLevel;
    }

    /**
     * Compares this level against the level passed as an argument and returns true if this level is the same or is more
     * specific.
     * <p>
     * Concretely, {@link #FATAL} is more specific than {@link #ERROR}, which is more specific than {@link #WARN},
     * etc., until {@link #TRACE}, and finally {@link #ALL}, which is the least specific standard level.
     * The most specific level is {@link #OFF}.
     * </p>
     *
     * @param level The level to test.
     * @return True if this level Level is more specific or the same as the given Level.
     */
    public boolean isMoreSpecificThan(final Level level) {
        return this.intLevel <= level.intLevel;
    }

    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    // CHECKSTYLE:OFF
    public Level clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
    // CHECKSTYLE:ON

    @Override
    public int compareTo(final Level other) {
        return intLevel < other.intLevel ? -1 : (intLevel > other.intLevel ? 1 : 0);
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof Level && other == this;
    }

    public Class<Level> getDeclaringClass() {
        return Level.class;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    /**
     * Gets the symbolic name of this Level. Equivalent to calling {@link #toString()}.
     *
     * @return the name of this Level.
     */
    public String name() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    /**
     * Retrieves an existing Level or creates on if it didn't previously exist.
     *
     * @param name The name of the level.
     * @param intValue The integer value for the Level. If the level was previously created this value is ignored.
     * @return The Level.
     * @throws java.lang.IllegalArgumentException if the name is null or intValue is less than zero.
     */
    public static Level forName(final String name, final int intValue) {
        final Level level = LEVELS.get(name);
        if (level != null) {
            return level;
        }
        try {
            return new Level(name, intValue);
        } catch (final IllegalStateException ex) {
            // The level was added by something else so just return that one.
            return LEVELS.get(name);
        }
    }

    /**
     * Return the Level associated with the name or null if the Level cannot be found.
     *
     * @param name The name of the Level.
     * @return The Level or null.
     */
    public static Level getLevel(final String name) {
        return LEVELS.get(name);
    }

    /**
     * Converts the string passed as argument to a level. If the conversion fails, then this method returns
     * {@link #DEBUG}.
     *
     * @param sArg The name of the desired Level.
     * @return The Level associated with the String.
     */
    public static Level toLevel(final String sArg) {
        return toLevel(sArg, Level.DEBUG);
    }

    /**
     * Converts the string passed as argument to a level. If the conversion fails, then this method returns the value of
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
        final Level level = LEVELS.get(name.toUpperCase(Locale.ENGLISH));
        return level == null ? defaultLevel : level;
    }

    /**
     * Return an array of all the Levels that have been registered.
     *
     * @return An array of Levels.
     */
    public static Level[] values() {
        final Collection<Level> values = Level.LEVELS.values();
        return values.toArray(new Level[values.size()]);
    }

    /**
     * Return the Level associated with the name.
     *
     * @param name The name of the Level to return.
     * @return The Level.
     * @throws java.lang.NullPointerException if the Level name is {@code null}.
     * @throws java.lang.IllegalArgumentException if the Level name is not registered.
     */
    public static Level valueOf(final String name) {
        Objects.requireNonNull(name, "No level name given.");
        final String levelName = name.toUpperCase(Locale.ENGLISH);
        final Level level = LEVELS.get(levelName);
        if (level != null) {
            return level;
        }
        throw new IllegalArgumentException("Unknown level constant [" + levelName + "].");
    }

    /**
     * Returns the enum constant of the specified enum type with the specified name. The name must match exactly an
     * identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
     *
     * @param enumType the {@code Class} object of the enum type from which to return a constant
     * @param name the name of the constant to return
     * @param <T> The enum type whose constant is to be returned
     * @return the enum constant of the specified enum type with the specified name
     * @throws java.lang.IllegalArgumentException if the specified enum type has no constant with the specified name, or
     *             the specified class object does not represent an enum type
     * @throws java.lang.NullPointerException if {@code enumType} or {@code name} are {@code null}
     * @see java.lang.Enum#valueOf(Class, String)
     */
    public static <T extends Enum<T>> T valueOf(final Class<T> enumType, final String name) {
        return Enum.valueOf(enumType, name);
    }

    // for deserialization
    protected Object readResolve() {
        return Level.valueOf(this.name);
    }
}
