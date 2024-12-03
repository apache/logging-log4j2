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
package org.apache.log4j;

import org.apache.log4j.helpers.OptionConverter;

/**
 * <em style="color:#A44">Refrain from using this class directly, use
 * the {@link Level} class instead.</em>
 */
public class Priority {

    /**
     * The <code>OFF</code> has the highest possible rank and is
     * intended to turn off logging.
     */
    public static final int OFF_INT = Integer.MAX_VALUE;
    /**
     * The <code>FATAL</code> level designates very severe error
     * events that will presumably lead the application to abort.
     */
    public static final int FATAL_INT = 50000;
    /**
     * The <code>ERROR</code> level designates error events that
     * might still allow the application to continue running.
     */
    public static final int ERROR_INT = 40000;
    /**
     * The <code>WARN</code> level designates potentially harmful situations.
     */
    public static final int WARN_INT = 30000;
    /**
     * The <code>INFO</code> level designates informational messages
     * that highlight the progress of the application at coarse-grained
     * level.
     */
    public static final int INFO_INT = 20000;
    /**
     * The <code>DEBUG</code> Level designates fine-grained
     * informational events that are most useful to debug an
     * application.
     */
    public static final int DEBUG_INT = 10000;
    // public final static int FINE_INT = DEBUG_INT;
    /**
     * The <code>ALL</code> has the lowest possible rank and is intended to
     * turn on all logging.
     */
    public static final int ALL_INT = Integer.MIN_VALUE;

    /**
     * @deprecated Use {@link Level#FATAL} instead.
     */
    @Deprecated
    public static final Priority FATAL = new Priority(FATAL_INT, "FATAL", 0, org.apache.logging.log4j.Level.FATAL);

    /**
     * @deprecated Use {@link Level#ERROR} instead.
     */
    @Deprecated
    public static final Priority ERROR = new Priority(ERROR_INT, "ERROR", 3, org.apache.logging.log4j.Level.ERROR);

    /**
     * @deprecated Use {@link Level#WARN} instead.
     */
    @Deprecated
    public static final Priority WARN = new Priority(WARN_INT, "WARN", 4, org.apache.logging.log4j.Level.WARN);

    /**
     * @deprecated Use {@link Level#INFO} instead.
     */
    @Deprecated
    public static final Priority INFO = new Priority(INFO_INT, "INFO", 6, org.apache.logging.log4j.Level.INFO);

    /**
     * @deprecated Use {@link Level#DEBUG} instead.
     */
    @Deprecated
    public static final Priority DEBUG = new Priority(DEBUG_INT, "DEBUG", 7, org.apache.logging.log4j.Level.DEBUG);

    /*
     * These variables should be private but were not in Log4j 1.2 so are left the same way here.
     */
    transient int level;
    transient String levelStr;
    transient int syslogEquivalent;
    transient org.apache.logging.log4j.Level version2Level;

    /**
     * Default constructor for deserialization.
     */
    protected Priority() {
        this(DEBUG_INT, "DEBUG", 7, org.apache.logging.log4j.Level.DEBUG);
    }

    /**
     * Instantiate a level object.
     * @param level The level value.
     * @param levelStr The level name.
     * @param syslogEquivalent The equivalent syslog value.
     */
    protected Priority(final int level, final String levelStr, final int syslogEquivalent) {
        this(level, levelStr, syslogEquivalent, null);
    }

    Priority(
            final int level,
            final String levelStr,
            final int syslogEquivalent,
            final org.apache.logging.log4j.Level version2Equivalent) {
        this.level = level;
        this.levelStr = levelStr;
        this.syslogEquivalent = syslogEquivalent;
        this.version2Level = version2Equivalent != null ? version2Equivalent : OptionConverter.createLevel(this);
    }

    /**
     * Two priorities are equal if their level fields are equal.
     * @param o The Object to check.
     * @return true if the objects are equal, false otherwise.
     *
     * @since 1.2
     */
    @Override
    public boolean equals(final Object o) {
        if (o instanceof Priority) {
            final Priority r = (Priority) o;
            return this.level == r.level;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.level;
    }

    /**
     * Returns the syslog equivalent of this priority as an integer.
     * @return The equivalent syslog value.
     */
    public final int getSyslogEquivalent() {
        return syslogEquivalent;
    }

    /**
     * Gets the Log4j 2.x level associated with this priority
     *
     * @return a Log4j 2.x level.
     */
    public org.apache.logging.log4j.Level getVersion2Level() {
        return version2Level;
    }

    /**
     * Returns {@code true} if this level has a higher or equal
     * level than the level passed as argument, {@code false}
     * otherwise.
     * <p>
     * You should think twice before overriding the default
     * implementation of <code>isGreaterOrEqual</code> method.
     * </p>
     * @param r The Priority to check.
     * @return true if the current level is greater or equal to the specified Priority.
     */
    public boolean isGreaterOrEqual(final Priority r) {
        return level >= r.level;
    }

    /**
     * Returns all possible priorities as an array of Level objects in
     * descending order.
     * @return An array of all possible Priorities.
     *
     * @deprecated This method will be removed with no replacement.
     */
    @Deprecated
    public static Priority[] getAllPossiblePriorities() {
        return new Priority[] {Priority.FATAL, Priority.ERROR, Level.WARN, Priority.INFO, Priority.DEBUG};
    }

    /**
     * Returns the string representation of this priority.
     * @return The name of the Priority.
     */
    @Override
    public final String toString() {
        return levelStr;
    }

    /**
     * Returns the integer representation of this level.
     * @return The integer value of this level.
     */
    public final int toInt() {
        return level;
    }

    /**
     * @param sArg The name of the Priority.
     * @return The Priority matching the name.
     * @deprecated Please use the {@link Level#toLevel(String)} method instead.
     */
    @Deprecated
    public static Priority toPriority(final String sArg) {
        return Level.toLevel(sArg);
    }

    /**
     * @param val The value of the Priority.
     * @return The Priority matching the value.
     * @deprecated Please use the {@link Level#toLevel(int)} method instead.
     */
    @Deprecated
    public static Priority toPriority(final int val) {
        return toPriority(val, Priority.DEBUG);
    }

    /**
     * @param val The value of the Priority.
     * @param defaultPriority The default Priority to use if the value is invalid.
     * @return The Priority matching the value or the default Priority if no match is found.
     * @deprecated Please use the {@link Level#toLevel(int, Level)} method instead.
     */
    @Deprecated
    public static Priority toPriority(final int val, final Priority defaultPriority) {
        Level result = Level.toLevel(val, null);
        return result == null ? defaultPriority : result;
    }

    /**
     * @param sArg The name of the Priority.
     * @param defaultPriority The default Priority to use if the name is not found.
     * @return The Priority matching the name or the default Priority if no match is found.
     * @deprecated Please use the {@link Level#toLevel(String, Level)} method instead.
     */
    @Deprecated
    public static Priority toPriority(final String sArg, final Priority defaultPriority) {
        Level result = Level.toLevel(sArg, null);
        return result == null ? defaultPriority : result;
    }
}
