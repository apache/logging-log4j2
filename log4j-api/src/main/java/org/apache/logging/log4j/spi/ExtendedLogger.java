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
package org.apache.logging.log4j.spi;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;

/**
 * Extends the {@code Logger} interface with methods that facilitate implementing or extending {@code Logger}s. Users
 * should not need to use this interface.
 */
public interface ExtendedLogger extends Logger {

    /**
     * Determines if logging is enabled.
     * 
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The Message.
     * @param t A Throwable.
     * @return True if logging is enabled, false otherwise.
     */
    boolean isEnabled(Level level, Marker marker, Message message, Throwable t);

    /**
     * Determines if logging is enabled.
     * 
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message.
     * @param t A Throwable.
     * @return True if logging is enabled, false otherwise.
     */
    boolean isEnabled(Level level, Marker marker, Object message, Throwable t);

    /**
     * Determines if logging is enabled.
     * 
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message.
     * @return True if logging is enabled, false otherwise.
     * @param t the exception to log, including its stack trace.
     */
    boolean isEnabled(Level level, Marker marker, String message, Throwable t);

    /**
     * Determine if logging is enabled.
     * 
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message.
     * @return True if logging is enabled, false otherwise.
     */
    boolean isEnabled(Level level, Marker marker, String message);

    /**
     * Determines if logging is enabled.
     * 
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message.
     * @param params The parameters.
     * @return True if logging is enabled, false otherwise.
     */
    boolean isEnabled(Level level, Marker marker, String message, Object... params);

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
     * Always logs a message at the specified level. It is the responsibility of the caller to ensure the specified
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
}
