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
package org.apache.logging.log4j;

import static org.apache.logging.log4j.util.Strings.toRootUpperCase;

import aQute.bnd.annotation.baseline.BaselineIgnore;
import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.logging.log4j.spi.StandardLevel;
import org.apache.logging.log4j.util.Strings;

/**
 * Levels used for identifying the severity of an event. Levels are organized from most specific to least:
 * <p>
 * <table>
 * <caption>Level names with description</caption>
 * <tr>
 * <th>Name</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>{@link #OFF}</td>
 * <td>No events will be logged.</td>
 * </tr>
 * <tr>
 * <td>{@link #FATAL}</td>
 * <td>A fatal event that will prevent the application from continuing.</td>
 * </tr>
 * <tr>
 * <td>{@link #ERROR}</td>
 * <td>An error in the application, possibly recoverable.</td>
 * </tr>
 * <tr>
 * <td>{@link #WARN}</td>
 * <td>An event that might possible lead to an error.</td>
 * </tr>
 * <tr>
 * <td>{@link #INFO}</td>
 * <td>An event for informational purposes.</td>
 * </tr>
 * <tr>
 * <td>{@link #DEBUG}</td>
 * <td>A general debugging event.</td>
 * </tr>
 * <tr>
 * <td>{@link #TRACE}</td>
 * <td>A fine-grained debug message, typically capturing the flow through the application.</td>
 * </tr>
 * <tr>
 * <td>{@link #ALL}</td>
 * <td>All events should be logged.</td>
 * </tr>
 * </table>
 * </p>
 * <p>
 * Typically, configuring a level in a filter or on a logger will cause logging events of that level and those that are
 * more specific to pass through the filter. A special level, {@link #ALL}, is guaranteed to capture all levels when
 * used in logging configurations.
 * </p>
 */
@BaselineIgnore("2.22.0")
public final class Level implements Comparable<Level>, Serializable {

    private static final Level[] EMPTY_ARRAY = {};

    private static final ConcurrentMap<String, Level> LEVELS = new ConcurrentHashMap<>(); // SUPPRESS CHECKSTYLE

    /**
     * No events will be logged.
     */
    public static final Level OFF = new Level("OFF", StandardLevel.OFF.intLevel());

    /**
     * A fatal event that will prevent the application from continuing.
     */
    public static final Level FATAL = new Level("FATAL", StandardLevel.FATAL.intLevel());

    /**
     * An error in the application, possibly recoverable.
     */
    public static final Level ERROR = new Level("ERROR", StandardLevel.ERROR.intLevel());

    /**
     * An event that might possible lead to an error.
     */
    public static final Level WARN = new Level("WARN", StandardLevel.WARN.intLevel());

    /**
     * An event for informational purposes.
     */
    public static final Level INFO = new Level("INFO", StandardLevel.INFO.intLevel());

    /**
     * A general debugging event.
     */
    public static final Level DEBUG = new Level("DEBUG", StandardLevel.DEBUG.intLevel());

    /**
     * A fine-grained debug message, typically capturing the flow through the application.
     */
    public static final Level TRACE = new Level("TRACE", StandardLevel.TRACE.intLevel());

    /**
     * All events should be logged.
     */
    public static final Level ALL = new Level("ALL", StandardLevel.ALL.intLevel());

    /**
     * Category to be used by custom levels.
     *
     * @since 2.1
     */
    public static final String CATEGORY = "Level";

    private static final long serialVersionUID = 1581082L;

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
        if (LEVELS.putIfAbsent(toRootUpperCase(name.trim()), this) != null) {
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
     * Concretely, {@link #ALL} is less specific than {@link #TRACE}, which is less specific than {@link #DEBUG}, which
     * is less specific than {@link #INFO}, which is less specific than {@link #WARN}, which is less specific than
     * {@link #ERROR}, which is less specific than {@link #FATAL}, and finally {@link #OFF}, which is the most specific
     * standard level.
     * </p>
     *
     * @param level
     *            The level to test.
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
        if (Strings.isEmpty(name)) {
            throw new IllegalArgumentException("Illegal null or empty Level name.");
        }
        final String normalizedName = toRootUpperCase(name.trim());
        final Level level = LEVELS.get(normalizedName);
        if (level != null) {
            return level;
        }
        try {
            // use original capitalization
            return new Level(name, intValue);
        } catch (final IllegalStateException ex) {
            // The level was added by something else so just return that one.
            return LEVELS.get(normalizedName);
        }
    }

    /**
     * Return the Level associated with the name or null if the Level cannot be found.
     *
     * @param name The name of the Level.
     * @return The Level or null.
     * @throws java.lang.IllegalArgumentException if the name is null.
     */
    public static Level getLevel(final String name) {
        if (Strings.isEmpty(name)) {
            throw new IllegalArgumentException("Illegal null or empty Level name.");
        }
        return LEVELS.get(toRootUpperCase(name.trim()));
    }

    /**
     * Converts the string passed as argument to a level. If the conversion fails, then this method returns
     * {@link #DEBUG}.
     *
     * @param level The name of the desired Level.
     * @return The Level associated with the String.
     */
    public static Level toLevel(final String level) {
        return toLevel(level, Level.DEBUG);
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
        final Level level = LEVELS.get(toRootUpperCase(name.trim()));
        return level == null ? defaultLevel : level;
    }

    /**
     * Return an array of all the Levels that have been registered.
     *
     * @return An array of Levels.
     */
    public static Level[] values() {
        return Level.LEVELS.values().toArray(EMPTY_ARRAY);
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
        final String levelName = toRootUpperCase(name.trim());
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
    private Object readResolve() {
        return Level.valueOf(this.name);
    }
}
