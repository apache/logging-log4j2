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
package org.apache.logging.log4j.spi;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.MessageSupplier;
import org.apache.logging.log4j.util.Supplier;

/**
 * Extends the {@code Logger} interface with methods that facilitate implementing or extending {@code Logger}s. Users
 * should not need to use this interface.
 */
public interface ExtendedLogger extends Logger {

    /**
     * Tests if logging is enabled.
     *
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The Message.
     * @param t A Throwable.
     * @return True if logging is enabled, false otherwise.
     */
    boolean isEnabled(Level level, Marker marker, Message message, Throwable t);

    /**
     * Tests if logging is enabled.
     *
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message.
     * @param t A Throwable.
     * @return True if logging is enabled, false otherwise.
     */
    boolean isEnabled(Level level, Marker marker, CharSequence message, Throwable t);

    /**
     * Tests if logging is enabled.
     *
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message.
     * @param t A Throwable.
     * @return True if logging is enabled, false otherwise.
     */
    boolean isEnabled(Level level, Marker marker, Object message, Throwable t);

    /**
     * Tests if logging is enabled.
     *
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message.
     * @return True if logging is enabled, false otherwise.
     * @param t the exception to log, including its stack trace.
     */
    boolean isEnabled(Level level, Marker marker, String message, Throwable t);

    /**
     * Tests if logging is enabled.
     *
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message.
     * @return True if logging is enabled, false otherwise.
     */
    boolean isEnabled(Level level, Marker marker, String message);

    /**
     * Tests if logging is enabled.
     *
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message.
     * @param params The parameters.
     * @return True if logging is enabled, false otherwise.
     */
    boolean isEnabled(Level level, Marker marker, String message, Object... params);

    /**
     * Tests if logging is enabled.
     *
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message.
     * @param p0 the message parameters
     * @return True if logging is enabled, false otherwise.
     */
    boolean isEnabled(Level level, Marker marker, String message, Object p0);

    /**
     * Tests if logging is enabled.
     *
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @return True if logging is enabled, false otherwise.
     */
    boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1);

    /**
     * Tests if logging is enabled.
     *
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @return True if logging is enabled, false otherwise.
     */
    boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1, Object p2);

    /**
     * Tests if logging is enabled.
     *
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @param p3 the message parameters
     * @return True if logging is enabled, false otherwise.
     */
    boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3);

    /**
     * Tests if logging is enabled.
     *
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @param p3 the message parameters
     * @param p4 the message parameters
     * @return True if logging is enabled, false otherwise.
     */
    boolean isEnabled(
            Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4);

    /**
     * Tests if logging is enabled.
     *
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @param p3 the message parameters
     * @param p4 the message parameters
     * @param p5 the message parameters
     * @return True if logging is enabled, false otherwise.
     */
    boolean isEnabled(
            Level level,
            Marker marker,
            String message,
            Object p0,
            Object p1,
            Object p2,
            Object p3,
            Object p4,
            Object p5);

    /**
     * Determines if logging is enabled.
     *
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @param p3 the message parameters
     * @param p4 the message parameters
     * @param p5 the message parameters
     * @param p6 the message parameters
     * @return True if logging is enabled, false otherwise.
     */
    boolean isEnabled(
            Level level,
            Marker marker,
            String message,
            Object p0,
            Object p1,
            Object p2,
            Object p3,
            Object p4,
            Object p5,
            Object p6);

    /**
     * Tests if logging is enabled.
     *
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @param p3 the message parameters
     * @param p4 the message parameters
     * @param p5 the message parameters
     * @param p6 the message parameters
     * @param p7 the message parameters
     * @return True if logging is enabled, false otherwise.
     */
    boolean isEnabled(
            Level level,
            Marker marker,
            String message,
            Object p0,
            Object p1,
            Object p2,
            Object p3,
            Object p4,
            Object p5,
            Object p6,
            Object p7);

    /**
     * Tests if logging is enabled.
     *
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @param p3 the message parameters
     * @param p4 the message parameters
     * @param p5 the message parameters
     * @param p6 the message parameters
     * @param p7 the message parameters
     * @param p8 the message parameters
     * @return True if logging is enabled, false otherwise.
     */
    boolean isEnabled(
            Level level,
            Marker marker,
            String message,
            Object p0,
            Object p1,
            Object p2,
            Object p3,
            Object p4,
            Object p5,
            Object p6,
            Object p7,
            Object p8);

    /**
     * Tests if logging is enabled.
     *
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @param p3 the message parameters
     * @param p4 the message parameters
     * @param p5 the message parameters
     * @param p6 the message parameters
     * @param p7 the message parameters
     * @param p8 the message parameters
     * @param p9 the message parameters
     * @return True if logging is enabled, false otherwise.
     */
    boolean isEnabled(
            Level level,
            Marker marker,
            String message,
            Object p0,
            Object p1,
            Object p2,
            Object p3,
            Object p4,
            Object p5,
            Object p6,
            Object p7,
            Object p8,
            Object p9);

    /**
     * Logs a message if the specified level is active.
     *
     * @param fqcn The fully qualified class name of the logger entry point, used to determine the caller class and
     *            method when location information needs to be logged.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The Message.
     * @param t the exception to log, including its stack trace.
     */
    void logIfEnabled(String fqcn, Level level, Marker marker, Message message, Throwable t);

    /**
     * Logs a CharSequence message if the specified level is active.
     *
     * @param fqcn The fully qualified class name of the logger entry point, used to determine the caller class and
     *            method when location information needs to be logged.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The CharSequence message.
     * @param t the exception to log, including its stack trace.
     */
    void logIfEnabled(String fqcn, Level level, Marker marker, CharSequence message, Throwable t);

    /**
     * Logs a message if the specified level is active.
     *
     * @param fqcn The fully qualified class name of the logger entry point, used to determine the caller class and
     *            method when location information needs to be logged.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message.
     * @param t the exception to log, including its stack trace.
     */
    void logIfEnabled(String fqcn, Level level, Marker marker, Object message, Throwable t);

    /**
     * Logs a message if the specified level is active.
     *
     * @param fqcn The fully qualified class name of the logger entry point, used to determine the caller class and
     *            method when location information needs to be logged.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message.
     * @param t the exception to log, including its stack trace.
     */
    void logIfEnabled(String fqcn, Level level, Marker marker, String message, Throwable t);

    /**
     * Logs a message if the specified level is active.
     *
     * @param fqcn The fully qualified class name of the logger entry point, used to determine the caller class and
     *            method when location information needs to be logged.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message.
     */
    void logIfEnabled(String fqcn, Level level, Marker marker, String message);

    /**
     * Logs a message if the specified level is active.
     *
     * @param fqcn The fully qualified class name of the logger entry point, used to determine the caller class and
     *            method when location information needs to be logged.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message format.
     * @param params The message parameters.
     */
    void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object... params);

    /**
     * Logs a message if the specified level is active.
     *
     * @param fqcn The fully qualified class name of the logger entry point, used to determine the caller class and
     *            method when location information needs to be logged.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message format.
     * @param p0 the message parameters
     */
    void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0);

    /**
     * Logs a message if the specified level is active.
     *
     * @param fqcn The fully qualified class name of the logger entry point, used to determine the caller class and
     *            method when location information needs to be logged.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message format.
     * @param p0 the message parameters
     * @param p1 the message parameters
     */
    void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0, Object p1);

    /**
     * Logs a message if the specified level is active.
     *
     * @param fqcn The fully qualified class name of the logger entry point, used to determine the caller class and
     *            method when location information needs to be logged.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message format.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     */
    void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2);

    /**
     * Logs a message if the specified level is active.
     *
     * @param fqcn The fully qualified class name of the logger entry point, used to determine the caller class and
     *            method when location information needs to be logged.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message format.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @param p3 the message parameters
     */
    void logIfEnabled(
            String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3);

    /**
     * Logs a message if the specified level is active.
     *
     * @param fqcn The fully qualified class name of the logger entry point, used to determine the caller class and
     *            method when location information needs to be logged.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message format.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @param p3 the message parameters
     * @param p4 the message parameters
     */
    void logIfEnabled(
            String fqcn,
            Level level,
            Marker marker,
            String message,
            Object p0,
            Object p1,
            Object p2,
            Object p3,
            Object p4);

    /**
     * Logs a message if the specified level is active.
     *
     * @param fqcn The fully qualified class name of the logger entry point, used to determine the caller class and
     *            method when location information needs to be logged.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message format.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @param p3 the message parameters
     * @param p4 the message parameters
     * @param p5 the message parameters
     */
    void logIfEnabled(
            String fqcn,
            Level level,
            Marker marker,
            String message,
            Object p0,
            Object p1,
            Object p2,
            Object p3,
            Object p4,
            Object p5);

    /**
     * Logs a message if the specified level is active.
     *
     * @param fqcn The fully qualified class name of the logger entry point, used to determine the caller class and
     *            method when location information needs to be logged.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message format.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @param p3 the message parameters
     * @param p4 the message parameters
     * @param p5 the message parameters
     * @param p6 the message parameters
     */
    void logIfEnabled(
            String fqcn,
            Level level,
            Marker marker,
            String message,
            Object p0,
            Object p1,
            Object p2,
            Object p3,
            Object p4,
            Object p5,
            Object p6);

    /**
     * Logs a message if the specified level is active.
     *
     * @param fqcn The fully qualified class name of the logger entry point, used to determine the caller class and
     *            method when location information needs to be logged.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message format.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @param p3 the message parameters
     * @param p4 the message parameters
     * @param p5 the message parameters
     * @param p6 the message parameters
     * @param p7 the message parameters
     */
    void logIfEnabled(
            String fqcn,
            Level level,
            Marker marker,
            String message,
            Object p0,
            Object p1,
            Object p2,
            Object p3,
            Object p4,
            Object p5,
            Object p6,
            Object p7);

    /**
     * Logs a message if the specified level is active.
     *
     * @param fqcn The fully qualified class name of the logger entry point, used to determine the caller class and
     *            method when location information needs to be logged.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message format.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @param p3 the message parameters
     * @param p4 the message parameters
     * @param p5 the message parameters
     * @param p6 the message parameters
     * @param p7 the message parameters
     * @param p8 the message parameters
     */
    void logIfEnabled(
            String fqcn,
            Level level,
            Marker marker,
            String message,
            Object p0,
            Object p1,
            Object p2,
            Object p3,
            Object p4,
            Object p5,
            Object p6,
            Object p7,
            Object p8);

    /**
     * Logs a message if the specified level is active.
     *
     * @param fqcn The fully qualified class name of the logger entry point, used to determine the caller class and
     *            method when location information needs to be logged.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message format.
     * @param p0 the message parameters
     * @param p1 the message parameters
     * @param p2 the message parameters
     * @param p3 the message parameters
     * @param p4 the message parameters
     * @param p5 the message parameters
     * @param p6 the message parameters
     * @param p7 the message parameters
     * @param p8 the message parameters
     * @param p9 the message parameters
     */
    void logIfEnabled(
            String fqcn,
            Level level,
            Marker marker,
            String message,
            Object p0,
            Object p1,
            Object p2,
            Object p3,
            Object p4,
            Object p5,
            Object p6,
            Object p7,
            Object p8,
            Object p9);

    /**
     * Logs a message at the specified level. It is the responsibility of the caller to ensure the specified
     * level is enabled.
     *
     * @param fqcn The fully qualified class name of the logger entry point, used to determine the caller class and
     *            method when location information needs to be logged.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The Message.
     * @param t the exception to log, including its stack trace.
     */
    void logMessage(String fqcn, Level level, Marker marker, Message message, Throwable t);

    /**
     * Logs a message which is only to be constructed if the specified level is active.
     *
     * @param fqcn The fully qualified class name of the logger entry point, used to determine the caller class and
     *            method when location information needs to be logged.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t the exception to log, including its stack trace.
     */
    void logIfEnabled(String fqcn, Level level, Marker marker, MessageSupplier msgSupplier, Throwable t);

    /**
     * Logs a message whose parameters are only to be constructed if the specified level is active.
     *
     * @param fqcn The fully qualified class name of the logger entry point, used to determine the caller class and
     *            method when location information needs to be logged.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message format.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     */
    @SuppressWarnings("deprecation")
    void logIfEnabled(String fqcn, Level level, Marker marker, String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message which is only to be constructed if the specified level is active.
     *
     * @param fqcn The fully qualified class name of the logger entry point, used to determine the caller class and
     *            method when location information needs to be logged.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t the exception to log, including its stack trace.
     */
    @SuppressWarnings("deprecation")
    void logIfEnabled(String fqcn, Level level, Marker marker, Supplier<?> msgSupplier, Throwable t);
}
