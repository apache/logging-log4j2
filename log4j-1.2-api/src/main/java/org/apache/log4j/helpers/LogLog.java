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

import org.apache.logging.log4j.status.StatusLogger;

/**
 * Logs statements from within Log4j.
 *
 * <p>
 * Log4j components cannot make Log4j logging calls. However, it is sometimes useful for the user to learn about what
 * Log4j is doing. You can enable Log4j internal logging by defining the <b>log4j.configDebug</b> variable.
 * </p>
 * <p>
 * All Log4j internal debug calls go to <code>System.out</code> where as internal error messages are sent to
 * <code>System.err</code>. All internal messages are prepended with the string "log4j: ".
 * </p>
 *
 * @since 0.8.2
 */
public class LogLog {

    private static final StatusLogger LOGGER = StatusLogger.getLogger();

    /**
     * Makes Log4j print log4j-internal debug statements to <code>System.out</code>.
     *
     * <p>
     * The value of this string is {@value #DEBUG_KEY}
     * </p>
     * <p>
     * Note that the search for all option names is case sensitive.
     * </p>
     */
    public static final String DEBUG_KEY = "log4j.debug";

    /**
     * Makes Log4j components print log4j-internal debug statements to <code>System.out</code>.
     *
     * <p>
     * The value of this string is {@value #CONFIG_DEBUG_KEY}.
     * </p>
     * <p>
     * Note that the search for all option names is case sensitive.
     * </p>
     *
     * @deprecated Use {@link #DEBUG_KEY} instead.
     */
    @Deprecated
    public static final String CONFIG_DEBUG_KEY = "log4j.configDebug";

    /**
     * Debug enabled Enable or disable.
     */
    protected static boolean debugEnabled = false;

    /**
     * In quietMode not even errors generate any output.
     */
    private static boolean quietMode = false;

    static {
        String key = OptionConverter.getSystemProperty(DEBUG_KEY, null);
        if (key == null) {
            key = OptionConverter.getSystemProperty(CONFIG_DEBUG_KEY, null);
        }
        if (key != null) {
            debugEnabled = OptionConverter.toBoolean(key, true);
        }
    }

    /**
     * Logs Log4j internal debug statements.
     *
     * @param message the message object to log.
     */
    public static void debug(final String message) {
        if (debugEnabled && !quietMode) {
            LOGGER.debug(message);
        }
    }

    /**
     * Logs Log4j internal debug statements.
     *
     * @param message the message object to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    public static void debug(final String message, final Throwable throwable) {
        if (debugEnabled && !quietMode) {
            LOGGER.debug(message, throwable);
        }
    }

    /**
     * Logs Log4j internal error statements.
     *
     * @param message the message object to log.
     */
    public static void error(final String message) {
        if (!quietMode) {
            LOGGER.error(message);
        }
    }

    /**
     * Logs Log4j internal error statements.
     *
     * @param message the message object to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    public static void error(final String message, final Throwable throwable) {
        if (!quietMode) {
            LOGGER.error(message, throwable);
        }
    }

    /**
     * Enables and disables Log4j internal logging.
     *
     * @param enabled Enable or disable.
     */
    public static void setInternalDebugging(final boolean enabled) {
        debugEnabled = enabled;
    }

    /**
     * In quite mode no LogLog generates strictly no output, not even for errors.
     *
     * @param quietMode A true for not
     */
    public static void setQuietMode(final boolean quietMode) {
        LogLog.quietMode = quietMode;
    }

    /**
     * Logs Log4j internal warning statements.
     *
     * @param message the message object to log.
     */
    public static void warn(final String message) {
        if (!quietMode) {
            LOGGER.warn(message);
        }
    }

    /**
     * Logs Log4j internal warnings.
     *
     * @param message the message object to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    public static void warn(final String message, final Throwable throwable) {
        if (!quietMode) {
            LOGGER.warn(message, throwable);
        }
    }
}
