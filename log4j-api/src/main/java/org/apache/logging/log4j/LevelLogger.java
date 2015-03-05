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

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;

/**
 * This is the central interface in the log4j package. Most logging operations, except configuration, are done through
 * this interface.
 *
 * <p>
 * The canonical way to obtain a Logger for a class is through {@link LogManager#getLogger()}. Typically, each class
 * gets its own Logger named after its fully qualified class name (the default Logger name when obtained through the
 * {@link LogManager#getLogger()} method). Thus, the simplest way to use this would be like so:
 * </p>
 * 
 * <pre>
 * public class MyClass {
 *     private static final Logger LOGGER = LogManager.getLogger();
 *     // ...
 * }
 * </pre>
 * <p>
 * For ease of filtering, searching, sorting, etc., it is generally a good idea to create Loggers for each class rather
 * than sharing Loggers. Instead, {@link Marker Markers} should be used for shared, filterable identification.
 * </p>
 * <p>
 * For service provider implementations, it is recommended to extend the
 * {@link org.apache.logging.log4j.spi.AbstractLogger} class rather than implementing this interface directly.
 * </p>
 */
public interface LevelLogger {

    /**
     * Logs an exception or error that has been caught. Normally, one may wish to provide additional information with an
     * exception while logging it; in these cases, one would not use this method. In other cases where simply logging
     * the fact that an exception was swallowed somewhere (e.g., at the top of the stack trace in a {@code main()}
     * method), this method is ideal for it.
     *
     * @param t
     *        The Throwable.
     */
    void catching(Throwable t);

    /**
     * Logs entry to a method. Used when the method in question has no parameters or when the parameters should not be
     * logged.
     */
    void entry();

    /**
     * Logs entry to a method along with its parameters. For example,
     * 
     * <pre>
     * public void doSomething(String foo, int bar) {
     *     LOGGER.entry(foo, bar);
     *     // do something
     * }
     * </pre>
     * <p>
     * The use of methods such as this are more effective when combined with aspect-oriented programming or other
     * bytecode manipulation tools. It can be rather tedious (and messy) to use this type of method manually.
     * </p>
     *
     * @param params
     *        The parameters to the method. TODO Use of varargs results in array creation which can be a substantial
     *        portion of no-op case. LogMF/LogSF provides several overrides to avoid vararg except in edge cases. (RG)
     *        LogMF and LogSF implement these in LogXF which calls logger.callAppenders. callAppenders is part of the
     *        implementation and cannot be used by the API. Adding more methods here and in AbstractLogger is
     *        sufficient.
     */
    void entry(Object... params);

    /**
     * Logs exit from a method. Used for methods that do not return anything.
     */
    void exit();

    /**
     * Logs exiting from a method with the result. This may be coded as:
     * 
     * <pre>
     * return LOGGER.exit(myResult);
     * </pre>
     *
     * @param <R>
     *        The type of the parameter and object being returned.
     * @param result
     *        The result being returned from the method call.
     * @return the result.
     */
    <R> R exit(R result);

    /**
     * Gets the Level associated with the Logger.
     *
     * @return the Level associate with the Logger.
     */
    Level getLevel();

    /**
     * Gets the message factory used to convert message Objects and Strings into actual log Messages.
     *
     * @return the message factory.
     */
    MessageFactory getMessageFactory();

    /**
     * Gets the logger name.
     *
     * @return the logger name.
     */
    String getName();

    /**
     * Checks whether this Logger is enabled for the {@link Level#DEBUG DEBUG} Level.
     *
     * @return boolean - {@code true} if this Logger is enabled for level DEBUG, {@code false} otherwise.
     */
    boolean isDebugEnabled();

    /**
     * Checks whether this Logger is enabled for the {@link Level#DEBUG DEBUG} Level.
     *
     * @param marker
     *        The marker data specific to this log statement.
     * @return boolean - {@code true} if this Logger is enabled for level DEBUG, {@code false} otherwise.
     */
    boolean isDebugEnabled(Marker marker);

    /**
     * Checks whether this Logger is enabled for the the given Level.
     * <p>
     * Note that passing in {@link Level#OFF OFF} always returns {@code true}.
     * </p>
     *
     * @return boolean - {@code true} if this Logger is enabled for level, {@code false} otherwise.
     */
    boolean isEnabled(Level level);

    /**
     * Checks whether this logger is enabled at the specified level and an optional Marker.
     *
     * @param marker
     *        The marker data specific to this log statement.
     * @return boolean - {@code true} if this Logger is enabled for level {@link Level#WARN WARN}, {@code false}
     *         otherwise.
     */
    boolean isEnabled(Marker marker);

    /**
     * Checks whether this Logger is enabled for the {@link Level#ERROR ERROR} Level.
     *
     * @return boolean - {@code true} if this Logger is enabled for level {@link Level#ERROR ERROR}, {@code false}
     *         otherwise.
     */
    boolean isErrorEnabled();

    /**
     * Checks whether this Logger is enabled for the {@link Level#ERROR ERROR} Level.
     *
     * @param marker
     *        The marker data specific to this log statement.
     * @return boolean - {@code true} if this Logger is enabled for level {@link Level#ERROR ERROR}, {@code false}
     *         otherwise.
     */
    boolean isErrorEnabled(Marker marker);

    /**
     * Checks whether this Logger is enabled for the {@link Level#FATAL FATAL} Level.
     *
     * @return boolean - {@code true} if this Logger is enabled for level {@link Level#FATAL FATAL}, {@code false}
     *         otherwise.
     */
    boolean isFatalEnabled();

    /**
     * Checks whether this Logger is enabled for the {@link Level#FATAL FATAL} Level.
     *
     * @param marker
     *        The marker data specific to this log statement.
     * @return boolean - {@code true} if this Logger is enabled for level {@link Level#FATAL FATAL}, {@code false}
     *         otherwise.
     */
    boolean isFatalEnabled(Marker marker);

    /**
     * Checks whether this Logger is enabled for the {@link Level#INFO INFO} Level.
     *
     * @return boolean - {@code true} if this Logger is enabled for level INFO, {@code false} otherwise.
     */
    boolean isInfoEnabled();

    /**
     * Checks whether this Logger is enabled for the {@link Level#INFO INFO} Level.
     *
     * @param marker
     *        The marker data specific to this log statement.
     * @return boolean - {@code true} if this Logger is enabled for level INFO, {@code false} otherwise.
     */
    boolean isInfoEnabled(Marker marker);

    /**
     * Checks whether this Logger is enabled for the {@link Level#TRACE TRACE} level.
     *
     * @return boolean - {@code true} if this Logger is enabled for level TRACE, {@code false} otherwise.
     */
    boolean isTraceEnabled();

    /**
     * Checks whether this Logger is enabled for the {@link Level#TRACE TRACE} level.
     *
     * @param marker
     *        The marker data specific to this log statement.
     * @return boolean - {@code true} if this Logger is enabled for level TRACE, {@code false} otherwise.
     */
    boolean isTraceEnabled(Marker marker);

    /**
     * Checks whether this Logger is enabled for the {@link Level#WARN WARN} Level.
     *
     * @return boolean - {@code true} if this Logger is enabled for level {@link Level#WARN WARN}, {@code false}
     *         otherwise.
     */
    boolean isWarnEnabled();

    /**
     * Checks whether this Logger is enabled for the {@link Level#WARN WARN} Level.
     *
     * @param marker
     *        The marker data specific to this log statement.
     * @return boolean - {@code true} if this Logger is enabled for level {@link Level#WARN WARN}, {@code false}
     *         otherwise.
     */
    boolean isWarnEnabled(Marker marker);

    /**
     * Logs a message with the specific Marker at the given level.
     *
     * 
     * @param marker
     *        the marker data specific to this log statement
     * @param msg
     *        the message string to be logged
     */
    void log(Marker marker, Message msg);

    /**
     * Logs a message with the specific Marker at the given level.
     *
     * @param marker
     *        the marker data specific to this log statement
     * @param msg
     *        the message string to be logged
     * @param t
     *        A Throwable or null.
     */
    void log(Marker marker, Message msg, Throwable t);

    /**
     * Logs a message object with the given level.
     *
     * @param marker
     *        the marker data specific to this log statement
     * @param message
     *        the message object to log.
     */
    void log(Marker marker, Object message);

    /**
     * Logs a message at the given level including the stack trace of the {@link Throwable} <code>t</code> passed as
     * parameter.
     *
     * @param marker
     *        the marker data specific to this log statement
     * @param message
     *        the message to log.
     * @param t
     *        the exception to log, including its stack trace.
     */
    void log(Marker marker, Object message, Throwable t);

    /**
     * Logs a message object with the given level.
     *
     * 
     * @param marker
     *        the marker data specific to this log statement
     * @param message
     *        the message object to log.
     */
    void log(Marker marker, String message);

    /**
     * Logs a message with parameters at the given level.
     *
     * @param marker
     *        the marker data specific to this log statement
     * @param message
     *        the message to log; the format depends on the message factory.
     * @param params
     *        parameters to the message.
     * @see #getMessageFactory()
     */
    void log(Marker marker, String message, Object... params);

    /**
     * Logs a message at the given level including the stack trace of the {@link Throwable} <code>t</code> passed as
     * parameter.
     *
     * @param marker
     *        the marker data specific to this log statement
     * @param message
     *        the message to log.
     * @param t
     *        the exception to log, including its stack trace.
     */
    void log(Marker marker, String message, Throwable t);

    /**
     * Logs a message with the specific Marker at the given level.
     *
     * @param msg
     *        the message string to be logged
     */
    void log(Message msg);

    /**
     * Logs a message with the specific Marker at the given level.
     *
     * @param msg
     *        the message string to be logged
     * @param t
     *        A Throwable or null.
     */
    void log(Message msg, Throwable t);

    /**
     * Logs a message object with the given level.
     *
     * @param message
     *        the message object to log.
     */
    void log(Object message);

    /**
     * Logs a message at the given level including the stack trace of the {@link Throwable} <code>t</code> passed as
     * parameter.
     *
     * @param message
     *        the message to log.
     * @param t
     *        the exception to log, including its stack trace.
     */
    void log(Object message, Throwable t);

    /**
     * Logs a message object with the given level.
     *
     * @param message
     *        the message string to log.
     */
    void log(String message);

    /**
     * Logs a message with parameters at the given level.
     *
     * 
     * @param message
     *        the message to log; the format depends on the message factory.
     * @param params
     *        parameters to the message.
     * @see #getMessageFactory()
     */
    void log(String message, Object... params);

    /**
     * Logs a message at the given level including the stack trace of the {@link Throwable} <code>t</code> passed as
     * parameter.
     *
     * 
     * @param message
     *        the message to log.
     * @param t
     *        the exception to log, including its stack trace.
     */
    void log(String message, Throwable t);

    /**
     * Logs a formatted message using the specified format string and arguments.
     *
     * 
     * @param marker
     *        the marker data specific to this log statement.
     * @param format
     *        The format String.
     * @param params
     *        Arguments specified by the format.
     */
    void printf(Marker marker, String format, Object... params);

    /**
     * Logs a formatted message using the specified format string and arguments.
     *
     * 
     * @param format
     *        The format String.
     * @param params
     *        Arguments specified by the format.
     */
    void printf(String format, Object... params);

    /**
     * Logs an exception or error to be thrown. This may be coded as:
     * 
     * <pre>
     * throw logger.throwing(myException);
     * </pre>
     *
     * @param <T>
     *        the Throwable type.
     * @param t
     *        The Throwable.
     * @return the Throwable.
     */
    <T extends Throwable> T throwing(T t);
}
