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

import java.util.Arrays;
import java.util.function.Supplier;
import org.apache.logging.log4j.message.EntryMessage;
import org.apache.logging.log4j.message.FlowMessageFactory;
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
 *
 * Since 2.4, methods have been added to the {@code Logger} interface to support lambda expressions. The new methods
 * allow client code to lazily log messages without explicitly checking if the requested log level is enabled. For
 * example, previously one would write:
 *
 * <pre>
 * // pre-Java 8 style optimization: explicitly check the log level
 * // to make sure the expensiveOperation() method is only called if necessary
 * if (logger.isTraceEnabled()) {
 *     logger.trace(&quot;Some long-running operation returned {}&quot;, expensiveOperation());
 * }
 * </pre>
 * <p>
 * With Java 8, the same effect can be achieved with a lambda expression:
 *
 * <pre>
 * // Java-8 style optimization: no need to explicitly check the log level:
 * // the lambda expression is not evaluated if the TRACE level is not enabled
 * logger.trace(&quot;Some long-running operation returned {}&quot;, () -&gt; expensiveOperation());
 * </pre>
 *
 * <p>
 * Note that although {@link org.apache.logging.log4j.util.MessageSupplier} is provided, using {@link org.apache.logging.log4j.util.Supplier org.apache.logging.log4j.util.Supplier&lt;Message&gt;} works just the
 * same. org.apache.logging.log4j.util.MessageSupplier was deprecated in 2.6 and un-deprecated in 2.8.1. Anonymous class usage of these APIs
 * should prefer using org.apache.logging.log4j.util.Supplier instead.
 * </p>
 */
public interface Logger extends org.apache.logging.log4j.v3.Logger {

    /**
     * Logs a {@link Throwable} that has been caught to a specific logging level.
     *
     * @param level The logging Level.
     * @param throwable the Throwable.
     */
    void catching(Level level, Throwable throwable);

    /**
     * Logs a {@link Throwable} that has been caught at the {@link Level#ERROR ERROR} level.
     * Normally, one may wish to provide additional information with an exception while logging it;
     * in these cases, one would not use this method.
     * In other cases where simply logging the fact that an exception was swallowed somewhere
     * (e.g., at the top of the stack trace in a {@code main()} method),
     * this method is ideal for it.
     *
     * @param throwable the Throwable.
     */
    void catching(Throwable throwable);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level with
     * the specified Marker. The {@code org.apache.logging.log4j.util.MessageSupplier} may or may not use the {@link MessageFactory} to construct the
     * {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message.
     * @since 2.4
     */
    default void debug(final Marker marker, final org.apache.logging.log4j.util.MessageSupplier messageSupplier) {
        debug(marker, (Supplier<Message>) messageSupplier);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>throwable</code> passed as parameter. The
     * {@code org.apache.logging.log4j.util.MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message.
     * @param throwable A Throwable or null.
     * @since 2.4
     */
    default void debug(
            final Marker marker,
            final org.apache.logging.log4j.util.MessageSupplier messageSupplier,
            final Throwable throwable) {
        debug(marker, (Supplier<Message>) messageSupplier, throwable);
    }

    /**
     * Logs a message object with the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    void debug(Marker marker, Object message);

    /**
     * Logs a message at the {@link Level#DEBUG DEBUG} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void debug(Marker marker, Object message, Throwable throwable);

    /**
     * Logs a message object with the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    void debug(Marker marker, String message);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void debug(
            final Marker marker,
            final String message,
            final org.apache.logging.log4j.util.Supplier<?>... paramSuppliers) {
        if (isDebugEnabled(marker)) {
            debug(marker, message, Arrays.copyOf(paramSuppliers, paramSuppliers.length, Supplier[].class));
        }
    }

    /**
     * Logs a message at the {@link Level#DEBUG DEBUG} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void debug(Marker marker, String message, Throwable throwable);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level with
     * the specified Marker.
     *
     * @param marker the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void debug(final Marker marker, final org.apache.logging.log4j.util.Supplier<?> messageSupplier) {
        debug(marker, (Supplier<?>) messageSupplier);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param throwable A Throwable or null.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void debug(
            final Marker marker,
            final org.apache.logging.log4j.util.Supplier<?> messageSupplier,
            final Throwable throwable) {
        debug(marker, (Supplier<?>) messageSupplier, throwable);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level. The
     * {@code org.apache.logging.log4j.util.MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param messageSupplier A function, which when called, produces the desired log message.
     * @since 2.4
     */
    default void debug(final org.apache.logging.log4j.util.MessageSupplier messageSupplier) {
        debug((Supplier<Message>) messageSupplier);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level) including the
     * stack trace of the {@link Throwable} <code>throwable</code> passed as parameter. The {@code org.apache.logging.log4j.util.MessageSupplier} may or may
     * not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param messageSupplier A function, which when called, produces the desired log message.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     * @since 2.4
     */
    default void debug(final org.apache.logging.log4j.util.MessageSupplier messageSupplier, final Throwable throwable) {
        debug((Supplier<Message>) messageSupplier, throwable);
    }

    /**
     * Logs a message object with the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message object to log.
     */
    void debug(Object message);

    /**
     * Logs a message at the {@link Level#DEBUG DEBUG} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param message the message to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void debug(Object message, Throwable throwable);

    /**
     * Logs a message object with the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message string to log.
     */
    void debug(String message);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void debug(final String message, final org.apache.logging.log4j.util.Supplier<?>... paramSuppliers) {
        if (isDebugEnabled()) {
            debug(message, Arrays.copyOf(paramSuppliers, paramSuppliers.length, Supplier[].class));
        }
    }

    /**
     * Logs a message at the {@link Level#DEBUG DEBUG} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param message the message to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void debug(String message, Throwable throwable);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level.
     *
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void debug(final org.apache.logging.log4j.util.Supplier<?> messageSupplier) {
        debug((Supplier<?>) messageSupplier);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level) including the
     * stack trace of the {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void debug(final org.apache.logging.log4j.util.Supplier<?> messageSupplier, final Throwable throwable) {
        debug((Supplier<?>) messageSupplier, throwable);
    }

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     */
    void debug(Marker marker, String message, Object p0, Object p1, Object p2);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     */
    void debug(Marker marker, String message, Object p0, Object p1, Object p2, Object p3);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     */
    void debug(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     */
    void debug(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     */
    void debug(
            Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     */
    void debug(
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
     * Logs a message with parameters at debug level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     */
    void debug(
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
     * Logs a message with parameters at debug level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @param p9 parameter to the message.
     */
    void debug(
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
     * Logs a message with parameters at debug level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     */
    void debug(String message, Object p0, Object p1, Object p2);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     */
    void debug(String message, Object p0, Object p1, Object p2, Object p3);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     */
    void debug(String message, Object p0, Object p1, Object p2, Object p3, Object p4);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     */
    void debug(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     */
    void debug(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     */
    void debug(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     */
    void debug(
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
     * Logs a message with parameters at debug level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @param p9 parameter to the message.
     */
    void debug(
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
     * Logs a message which is only to be constructed if the logging level is the {@link Level#ERROR ERROR} level with
     * the specified Marker. The {@code org.apache.logging.log4j.util.MessageSupplier} may or may not use the {@link MessageFactory} to construct the
     * {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message.
     * @since 2.4
     */
    default void error(final Marker marker, final org.apache.logging.log4j.util.MessageSupplier messageSupplier) {
        error(marker, (Supplier<Message>) messageSupplier);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#ERROR ERROR} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>throwable</code> passed as parameter. The
     * {@code org.apache.logging.log4j.util.MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message.
     * @param throwable A Throwable or null.
     * @since 2.4
     */
    default void error(
            final Marker marker,
            final org.apache.logging.log4j.util.MessageSupplier messageSupplier,
            final Throwable throwable) {
        error(marker, (Supplier<Message>) messageSupplier, throwable);
    }

    /**
     * Logs a message object with the {@link Level#ERROR ERROR} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    void error(Marker marker, Object message);

    /**
     * Logs a message at the {@link Level#ERROR ERROR} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void error(Marker marker, Object message, Throwable throwable);

    /**
     * Logs a message object with the {@link Level#ERROR ERROR} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    void error(Marker marker, String message);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#ERROR
     * ERROR} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void error(
            final Marker marker,
            final String message,
            final org.apache.logging.log4j.util.Supplier<?>... paramSuppliers) {
        if (isErrorEnabled(marker)) {
            error(marker, message, Arrays.copyOf(paramSuppliers, paramSuppliers.length, Supplier[].class));
        }
    }

    /**
     * Logs a message at the {@link Level#ERROR ERROR} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void error(Marker marker, String message, Throwable throwable);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#ERROR ERROR} level with
     * the specified Marker.
     *
     * @param marker the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void error(final Marker marker, final org.apache.logging.log4j.util.Supplier<?> messageSupplier) {
        error(marker, (Supplier<?>) messageSupplier);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#ERROR ERROR} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param throwable A Throwable or null.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void error(
            final Marker marker,
            final org.apache.logging.log4j.util.Supplier<?> messageSupplier,
            final Throwable throwable) {
        error(marker, (Supplier<?>) messageSupplier, throwable);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#ERROR ERROR} level. The
     * {@code org.apache.logging.log4j.util.MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param messageSupplier A function, which when called, produces the desired log message.
     * @since 2.4
     */
    default void error(final org.apache.logging.log4j.util.MessageSupplier messageSupplier) {
        error((Supplier<Message>) messageSupplier);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#ERROR ERROR} level) including the
     * stack trace of the {@link Throwable} <code>throwable</code> passed as parameter. The {@code org.apache.logging.log4j.util.MessageSupplier} may or may
     * not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param messageSupplier A function, which when called, produces the desired log message.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     * @since 2.4
     */
    default void error(final org.apache.logging.log4j.util.MessageSupplier messageSupplier, final Throwable throwable) {
        error((Supplier<Message>) messageSupplier, throwable);
    }

    /**
     * Logs a message object with the {@link Level#ERROR ERROR} level.
     *
     * @param message the message object to log.
     */
    void error(Object message);

    /**
     * Logs a message at the {@link Level#ERROR ERROR} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void error(Object message, Throwable throwable);

    /**
     * Logs a message object with the {@link Level#ERROR ERROR} level.
     *
     * @param message the message string to log.
     */
    void error(String message);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#ERROR
     * ERROR} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void error(final String message, final org.apache.logging.log4j.util.Supplier<?>... paramSuppliers) {
        if (isErrorEnabled()) {
            error(message, Arrays.copyOf(paramSuppliers, paramSuppliers.length, Supplier[].class));
        }
    }

    /**
     * Logs a message at the {@link Level#ERROR ERROR} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void error(String message, Throwable throwable);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#ERROR ERROR} level.
     *
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void error(final org.apache.logging.log4j.util.Supplier<?> messageSupplier) {
        error((Supplier<?>) messageSupplier);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#ERROR ERROR} level) including the
     * stack trace of the {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void error(final org.apache.logging.log4j.util.Supplier<?> messageSupplier, final Throwable throwable) {
        error((Supplier<?>) messageSupplier, throwable);
    }

    /**
     * Logs a message with parameters at error level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     */
    void error(Marker marker, String message, Object p0, Object p1, Object p2);

    /**
     * Logs a message with parameters at error level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     */
    void error(Marker marker, String message, Object p0, Object p1, Object p2, Object p3);

    /**
     * Logs a message with parameters at error level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     */
    void error(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4);

    /**
     * Logs a message with parameters at error level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     */
    void error(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5);

    /**
     * Logs a message with parameters at error level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     */
    void error(
            Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6);

    /**
     * Logs a message with parameters at error level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     */
    void error(
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
     * Logs a message with parameters at error level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     */
    void error(
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
     * Logs a message with parameters at error level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @param p9 parameter to the message.
     */
    void error(
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
     * Logs a message with parameters at error level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     */
    void error(String message, Object p0, Object p1, Object p2);

    /**
     * Logs a message with parameters at error level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     */
    void error(String message, Object p0, Object p1, Object p2, Object p3);

    /**
     * Logs a message with parameters at error level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     */
    void error(String message, Object p0, Object p1, Object p2, Object p3, Object p4);

    /**
     * Logs a message with parameters at error level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     */
    void error(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5);

    /**
     * Logs a message with parameters at error level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     */
    void error(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6);

    /**
     * Logs a message with parameters at error level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     */
    void error(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7);

    /**
     * Logs a message with parameters at error level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     */
    void error(
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
     * Logs a message with parameters at error level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @param p9 parameter to the message.
     */
    void error(
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
     * Logs a message which is only to be constructed if the logging level is the {@link Level#FATAL FATAL} level with
     * the specified Marker. The {@code org.apache.logging.log4j.util.MessageSupplier} may or may not use the {@link MessageFactory} to construct the
     * {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message.
     * @since 2.4
     */
    default void fatal(final Marker marker, final org.apache.logging.log4j.util.MessageSupplier messageSupplier) {
        fatal(marker, (Supplier<Message>) messageSupplier);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#FATAL FATAL} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>throwable</code> passed as parameter. The
     * {@code org.apache.logging.log4j.util.MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message.
     * @param throwable A Throwable or null.
     * @since 2.4
     */
    default void fatal(
            final Marker marker,
            final org.apache.logging.log4j.util.MessageSupplier messageSupplier,
            final Throwable throwable) {
        fatal(marker, (Supplier<Message>) messageSupplier, throwable);
    }

    /**
     * Logs a message object with the {@link Level#FATAL FATAL} level.
     *
     * @param marker The marker data specific to this log statement.
     * @param message the message object to log.
     */
    void fatal(Marker marker, Object message);

    /**
     * Logs a message at the {@link Level#FATAL FATAL} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param marker The marker data specific to this log statement.
     * @param message the message object to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void fatal(Marker marker, Object message, Throwable throwable);

    /**
     * Logs a message object with the {@link Level#FATAL FATAL} level.
     *
     * @param marker The marker data specific to this log statement.
     * @param message the message object to log.
     */
    void fatal(Marker marker, String message);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#FATAL
     * FATAL} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void fatal(
            final Marker marker,
            final String message,
            final org.apache.logging.log4j.util.Supplier<?>... paramSuppliers) {
        if (isFatalEnabled(marker)) {
            fatal(marker, message, Arrays.copyOf(paramSuppliers, paramSuppliers.length, Supplier[].class));
        }
    }

    /**
     * Logs a message at the {@link Level#FATAL FATAL} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param marker The marker data specific to this log statement.
     * @param message the message object to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void fatal(Marker marker, String message, Throwable throwable);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#FATAL FATAL} level with
     * the specified Marker.
     *
     * @param marker the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void fatal(final Marker marker, final org.apache.logging.log4j.util.Supplier<?> messageSupplier) {
        fatal(marker, (Supplier<?>) messageSupplier);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#FATAL FATAL} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param throwable A Throwable or null.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void fatal(
            final Marker marker,
            final org.apache.logging.log4j.util.Supplier<?> messageSupplier,
            final Throwable throwable) {
        fatal(marker, (Supplier<?>) messageSupplier, throwable);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#FATAL FATAL} level. The
     * {@code org.apache.logging.log4j.util.MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param messageSupplier A function, which when called, produces the desired log message.
     * @since 2.4
     */
    default void fatal(final org.apache.logging.log4j.util.MessageSupplier messageSupplier) {
        fatal((Supplier<Message>) messageSupplier);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#FATAL FATAL} level) including the
     * stack trace of the {@link Throwable} <code>throwable</code> passed as parameter. The {@code org.apache.logging.log4j.util.MessageSupplier} may or may
     * not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param messageSupplier A function, which when called, produces the desired log message.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     * @since 2.4
     */
    default void fatal(final org.apache.logging.log4j.util.MessageSupplier messageSupplier, final Throwable throwable) {
        fatal((Supplier<Message>) messageSupplier, throwable);
    }

    /**
     * Logs a message object with the {@link Level#FATAL FATAL} level.
     *
     * @param message the message object to log.
     */
    void fatal(Object message);

    /**
     * Logs a message at the {@link Level#FATAL FATAL} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void fatal(Object message, Throwable throwable);

    /**
     * Logs a message object with the {@link Level#FATAL FATAL} level.
     *
     * @param message the message string to log.
     */
    void fatal(String message);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#FATAL
     * FATAL} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void fatal(final String message, final org.apache.logging.log4j.util.Supplier<?>... paramSuppliers) {
        if (isFatalEnabled()) {
            fatal(message, Arrays.copyOf(paramSuppliers, paramSuppliers.length, Supplier[].class));
        }
    }

    /**
     * Logs a message at the {@link Level#FATAL FATAL} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void fatal(String message, Throwable throwable);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#FATAL FATAL} level.
     *
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void fatal(final org.apache.logging.log4j.util.Supplier<?> messageSupplier) {
        fatal((Supplier<?>) messageSupplier);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#FATAL FATAL} level) including the
     * stack trace of the {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void fatal(final org.apache.logging.log4j.util.Supplier<?> messageSupplier, final Throwable throwable) {
        fatal((Supplier<?>) messageSupplier, throwable);
    }

    /**
     * Logs a message with parameters at fatal level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     */
    void fatal(Marker marker, String message, Object p0, Object p1, Object p2);

    /**
     * Logs a message with parameters at fatal level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     */
    void fatal(Marker marker, String message, Object p0, Object p1, Object p2, Object p3);

    /**
     * Logs a message with parameters at fatal level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     */
    void fatal(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4);

    /**
     * Logs a message with parameters at fatal level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     */
    void fatal(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5);

    /**
     * Logs a message with parameters at fatal level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     */
    void fatal(
            Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6);

    /**
     * Logs a message with parameters at fatal level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     */
    void fatal(
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
     * Logs a message with parameters at fatal level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     */
    void fatal(
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
     * Logs a message with parameters at fatal level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @param p9 parameter to the message.
     */
    void fatal(
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
     * Logs a message with parameters at fatal level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     */
    void fatal(String message, Object p0, Object p1, Object p2);

    /**
     * Logs a message with parameters at fatal level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     */
    void fatal(String message, Object p0, Object p1, Object p2, Object p3);

    /**
     * Logs a message with parameters at fatal level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     */
    void fatal(String message, Object p0, Object p1, Object p2, Object p3, Object p4);

    /**
     * Logs a message with parameters at fatal level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     */
    void fatal(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5);

    /**
     * Logs a message with parameters at fatal level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     */
    void fatal(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6);

    /**
     * Logs a message with parameters at fatal level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     */
    void fatal(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7);

    /**
     * Logs a message with parameters at fatal level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     */
    void fatal(
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
     * Logs a message with parameters at fatal level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @param p9 parameter to the message.
     */
    void fatal(
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
     * Gets the message factory used to convert message Objects and Strings/CharSequences into actual log Messages.
     * <p>
     * Since version 2.6, Log4j internally uses message factories that implement the {@link MessageFactory} interface.
     * From version 2.6.2, the return type of this method was changed from {@link MessageFactory} to
     * {@code <MF extends MessageFactory> MF}. The returned factory will always implement {@link MessageFactory},
     * but the return type of this method could not be changed to {@link MessageFactory} without breaking binary
     * compatibility.
     * </p>
     *
     * @return the message factory, as an instance of {@link MessageFactory}
     */
    <MF extends MessageFactory> MF getMessageFactory();

    /**
     * Gets the flow message factory used to convert messages into flow messages.
     *
     * @return the flow message factory, as an instance of {@link FlowMessageFactory}.
     * @since 2.20
     */
    FlowMessageFactory getFlowMessageFactory();

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#INFO INFO} level with the
     * specified Marker. The {@code org.apache.logging.log4j.util.MessageSupplier} may or may not use the {@link MessageFactory} to construct the
     * {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message.
     * @since 2.4
     */
    default void info(final Marker marker, final org.apache.logging.log4j.util.MessageSupplier messageSupplier) {
        info(marker, (Supplier<Message>) messageSupplier);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#INFO INFO} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>throwable</code> passed as parameter. The
     * {@code org.apache.logging.log4j.util.MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message.
     * @param throwable A Throwable or null.
     * @since 2.4
     */
    default void info(
            final Marker marker,
            final org.apache.logging.log4j.util.MessageSupplier messageSupplier,
            final Throwable throwable) {
        info(marker, (Supplier<Message>) messageSupplier, throwable);
    }

    /**
     * Logs a message object with the {@link Level#INFO INFO} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    void info(Marker marker, Object message);

    /**
     * Logs a message at the {@link Level#INFO INFO} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void info(Marker marker, Object message, Throwable throwable);

    /**
     * Logs a message object with the {@link Level#INFO INFO} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    void info(Marker marker, String message);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#INFO
     * INFO} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void info(
            final Marker marker,
            final String message,
            final org.apache.logging.log4j.util.Supplier<?>... paramSuppliers) {
        if (isInfoEnabled(marker)) {
            info(marker, message, Arrays.copyOf(paramSuppliers, paramSuppliers.length, Supplier[].class));
        }
    }

    /**
     * Logs a message at the {@link Level#INFO INFO} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void info(Marker marker, String message, Throwable throwable);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#INFO INFO} level with the
     * specified Marker.
     *
     * @param marker the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void info(final Marker marker, final org.apache.logging.log4j.util.Supplier<?> messageSupplier) {
        info(marker, (Supplier<?>) messageSupplier);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#INFO INFO} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param throwable A Throwable or null.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void info(
            final Marker marker,
            final org.apache.logging.log4j.util.Supplier<?> messageSupplier,
            final Throwable throwable) {
        info(marker, (Supplier<?>) messageSupplier, throwable);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#INFO INFO} level. The
     * {@code org.apache.logging.log4j.util.MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param messageSupplier A function, which when called, produces the desired log message.
     * @since 2.4
     */
    default void info(final org.apache.logging.log4j.util.MessageSupplier messageSupplier) {
        info((Supplier<Message>) messageSupplier);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#INFO INFO} level) including the
     * stack trace of the {@link Throwable} <code>throwable</code> passed as parameter. The {@code org.apache.logging.log4j.util.MessageSupplier} may or may
     * not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param messageSupplier A function, which when called, produces the desired log message.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     * @since 2.4
     */
    default void info(final org.apache.logging.log4j.util.MessageSupplier messageSupplier, final Throwable throwable) {
        info((Supplier<Message>) messageSupplier, throwable);
    }

    /**
     * Logs a message object with the {@link Level#INFO INFO} level.
     *
     * @param message the message object to log.
     */
    void info(Object message);

    /**
     * Logs a message at the {@link Level#INFO INFO} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void info(Object message, Throwable throwable);

    /**
     * Logs a message object with the {@link Level#INFO INFO} level.
     *
     * @param message the message string to log.
     */
    void info(String message);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#INFO
     * INFO} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void info(final String message, final org.apache.logging.log4j.util.Supplier<?>... paramSuppliers) {
        if (isInfoEnabled()) {
            info(message, Arrays.copyOf(paramSuppliers, paramSuppliers.length, Supplier[].class));
        }
    }

    /**
     * Logs a message at the {@link Level#INFO INFO} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void info(String message, Throwable throwable);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#INFO INFO} level.
     *
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void info(final org.apache.logging.log4j.util.Supplier<?> messageSupplier) {
        info((Supplier<?>) messageSupplier);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#INFO INFO} level) including the
     * stack trace of the {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void info(final org.apache.logging.log4j.util.Supplier<?> messageSupplier, final Throwable throwable) {
        info((Supplier<?>) messageSupplier, throwable);
    }

    /**
     * Logs a message with parameters at info level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     */
    void info(Marker marker, String message, Object p0, Object p1, Object p2);

    /**
     * Logs a message with parameters at info level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     */
    void info(Marker marker, String message, Object p0, Object p1, Object p2, Object p3);

    /**
     * Logs a message with parameters at info level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     */
    void info(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4);

    /**
     * Logs a message with parameters at info level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     */
    void info(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5);

    /**
     * Logs a message with parameters at info level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     */
    void info(
            Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6);

    /**
     * Logs a message with parameters at info level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     */
    void info(
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
     * Logs a message with parameters at info level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     */
    void info(
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
     * Logs a message with parameters at info level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @param p9 parameter to the message.
     */
    void info(
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
     * Logs a message with parameters at info level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     */
    void info(String message, Object p0, Object p1, Object p2);

    /**
     * Logs a message with parameters at info level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     */
    void info(String message, Object p0, Object p1, Object p2, Object p3);

    /**
     * Logs a message with parameters at info level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     */
    void info(String message, Object p0, Object p1, Object p2, Object p3, Object p4);

    /**
     * Logs a message with parameters at info level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     */
    void info(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5);

    /**
     * Logs a message with parameters at info level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     */
    void info(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6);

    /**
     * Logs a message with parameters at info level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     */
    void info(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7);

    /**
     * Logs a message with parameters at info level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     */
    void info(
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
     * Logs a message with parameters at info level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @param p9 parameter to the message.
     */
    void info(
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
     * Logs a message which is only to be constructed if the logging level is the specified level with the specified
     * Marker. The {@code org.apache.logging.log4j.util.MessageSupplier} may or may not use the {@link MessageFactory} to construct the
     * {@code Message}.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message.
     * @since 2.4
     */
    default void log(
            final Level level,
            final Marker marker,
            final org.apache.logging.log4j.util.MessageSupplier messageSupplier) {
        log(level, marker, (Supplier<Message>) messageSupplier);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the specified level) with the specified Marker and
     * including the stack log of the {@link Throwable} <code>throwable</code> passed as parameter. The {@code org.apache.logging.log4j.util.MessageSupplier}
     * may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message.
     * @param throwable A Throwable or null.
     * @since 2.4
     */
    default void log(
            final Level level,
            final Marker marker,
            final org.apache.logging.log4j.util.MessageSupplier messageSupplier,
            final Throwable throwable) {
        log(level, marker, (Supplier<Message>) messageSupplier, throwable);
    }

    /**
     * Logs a message object with the given level.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    void log(Level level, Marker marker, Object message);

    /**
     * Logs a message at the given level including the stack trace of the {@link Throwable} <code>throwable</code> passed as
     * parameter.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param message the message to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void log(Level level, Marker marker, Object message, Throwable throwable);

    /**
     * Logs a message object with the given level.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    void log(Level level, Marker marker, String message);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the specified level.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void log(
            final Level level,
            final Marker marker,
            final String message,
            final org.apache.logging.log4j.util.Supplier<?>... paramSuppliers) {
        if (isEnabled(level, marker)) {
            log(level, marker, message, Arrays.copyOf(paramSuppliers, paramSuppliers.length, Supplier[].class));
        }
    }

    /**
     * Logs a message at the given level including the stack trace of the {@link Throwable} <code>throwable</code> passed as
     * parameter.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param message the message to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void log(Level level, Marker marker, String message, Throwable throwable);

    /**
     * Logs a message (only to be constructed if the logging level is the specified level) with the specified Marker.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void log(
            final Level level, final Marker marker, final org.apache.logging.log4j.util.Supplier<?> messageSupplier) {
        log(level, marker, (Supplier<?>) messageSupplier);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the specified level) with the specified Marker and
     * including the stack log of the {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param throwable A Throwable or null.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void log(
            final Level level,
            final Marker marker,
            final org.apache.logging.log4j.util.Supplier<?> messageSupplier,
            final Throwable throwable) {
        log(level, marker, (Supplier<?>) messageSupplier, throwable);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the specified level. The
     * {@code org.apache.logging.log4j.util.MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param level the logging level
     * @param messageSupplier A function, which when called, produces the desired log message.
     * @since 2.4
     */
    default void log(final Level level, final org.apache.logging.log4j.util.MessageSupplier messageSupplier) {
        log(level, (Supplier<Message>) messageSupplier);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the specified level) including the stack log of
     * the {@link Throwable} <code>throwable</code> passed as parameter. The {@code org.apache.logging.log4j.util.MessageSupplier} may or may not use the
     * {@link MessageFactory} to construct the {@code Message}.
     *
     * @param level the logging level
     * @param messageSupplier A function, which when called, produces the desired log message.
     * @param throwable the {@code Throwable} to log, including its stack log.
     * @since 2.4
     */
    default void log(
            final Level level,
            final org.apache.logging.log4j.util.MessageSupplier messageSupplier,
            final Throwable throwable) {
        log(level, (Supplier<Message>) messageSupplier, throwable);
    }

    /**
     * Logs a message object with the given level.
     *
     * @param level the logging level
     * @param message the message object to log.
     */
    void log(Level level, Object message);

    /**
     * Logs a message at the given level including the stack trace of the {@link Throwable} <code>throwable</code> passed as
     * parameter.
     *
     * @param level the logging level
     * @param message the message to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void log(Level level, Object message, Throwable throwable);

    /**
     * Logs a message object with the given level.
     *
     * @param level the logging level
     * @param message the message string to log.
     */
    void log(Level level, String message);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the specified level.
     *
     * @param level the logging level
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void log(
            final Level level,
            final String message,
            final org.apache.logging.log4j.util.Supplier<?>... paramSuppliers) {
        if (isEnabled(level)) {
            log(level, message, Arrays.copyOf(paramSuppliers, paramSuppliers.length, Supplier[].class));
        }
    }

    /**
     * Logs a message at the given level including the stack trace of the {@link Throwable} <code>throwable</code> passed as
     * parameter.
     *
     * @param level the logging level
     * @param message the message to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void log(Level level, String message, Throwable throwable);

    /**
     * Logs a message which is only to be constructed if the logging level is the specified level.
     *
     * @param level the logging level
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void log(final Level level, final org.apache.logging.log4j.util.Supplier<?> messageSupplier) {
        log(level, (Supplier<?>) messageSupplier);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the specified level) including the stack log of
     * the {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param level the logging level
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param throwable the {@code Throwable} to log, including its stack log.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void log(
            final Level level,
            final org.apache.logging.log4j.util.Supplier<?> messageSupplier,
            final Throwable throwable) {
        log(level, (Supplier<?>) messageSupplier, throwable);
    }

    /**
     * Logs a message with parameters at the specified level.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     */
    void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2);

    /**
     * Logs a message with parameters at the specified level.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     */
    void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3);

    /**
     * Logs a message with parameters at the specified level.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     */
    void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4);

    /**
     * Logs a message with parameters at the specified level.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     */
    void log(
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
     * Logs a message with parameters at the specified level.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     */
    void log(
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
     * Logs a message with parameters at the specified level.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     */
    void log(
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
     * Logs a message with parameters at the specified level.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     */
    void log(
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
     * Logs a message with parameters at the specified level.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @param p9 parameter to the message.
     */
    void log(
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
     * Logs a message with parameters at the specified level.
     *
     * @param level the logging level
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     */
    void log(Level level, String message, Object p0, Object p1, Object p2);

    /**
     * Logs a message with parameters at the specified level.
     *
     * @param level the logging level
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     */
    void log(Level level, String message, Object p0, Object p1, Object p2, Object p3);

    /**
     * Logs a message with parameters at the specified level.
     *
     * @param level the logging level
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     */
    void log(Level level, String message, Object p0, Object p1, Object p2, Object p3, Object p4);

    /**
     * Logs a message with parameters at the specified level.
     *
     * @param level the logging level
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     */
    void log(Level level, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5);

    /**
     * Logs a message with parameters at the specified level.
     *
     * @param level the logging level
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     */
    void log(Level level, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6);

    /**
     * Logs a message with parameters at the specified level.
     *
     * @param level the logging level
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     */
    void log(
            Level level,
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
     * Logs a message with parameters at the specified level.
     *
     * @param level the logging level
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     */
    void log(
            Level level,
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
     * Logs a message with parameters at the specified level.
     *
     * @param level the logging level
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @param p9 parameter to the message.
     */
    void log(
            Level level,
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
     * Logs a formatted message using the specified format string and arguments.
     *
     * @param level The logging Level.
     * @param marker the marker data specific to this log statement.
     * @param format The format String.
     * @param params Arguments specified by the format.
     */
    void printf(Level level, Marker marker, String format, Object... params);

    /**
     * Logs a formatted message using the specified format string and arguments.
     *
     * @param level The logging Level.
     * @param format The format String.
     * @param params Arguments specified by the format.
     */
    void printf(Level level, String format, Object... params);

    /**
     * Logs a {@link Throwable} to be thrown. This may be coded as:
     *
     * <pre>
     * throw logger.throwing(Level.DEBUG, myException);
     * </pre>
     *
     * @param <T> the Throwable type.
     * @param level The logging Level.
     * @param throwable The Throwable.
     * @return the Throwable.
     */
    <T extends Throwable> T throwing(Level level, T throwable);

    /**
     * Logs a {@link Throwable} to be thrown at the {@link Level#ERROR ERROR} level.
     * This may be coded as:
     *
     * <pre>
     * throw logger.throwing(myException);
     * </pre>
     *
     * @param <T> the Throwable type.
     * @param throwable The Throwable.
     * @return the Throwable.
     */
    <T extends Throwable> T throwing(T throwable);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#TRACE TRACE} level with
     * the specified Marker. The {@code org.apache.logging.log4j.util.MessageSupplier} may or may not use the {@link MessageFactory} to construct the
     * {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message.
     * @since 2.4
     */
    default void trace(final Marker marker, final org.apache.logging.log4j.util.MessageSupplier messageSupplier) {
        trace(marker, (Supplier<Message>) messageSupplier);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#TRACE TRACE} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>throwable</code> passed as parameter. The
     * {@code org.apache.logging.log4j.util.MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message.
     * @param throwable A Throwable or null.
     * @since 2.4
     */
    default void trace(
            final Marker marker,
            final org.apache.logging.log4j.util.MessageSupplier messageSupplier,
            final Throwable throwable) {
        trace(marker, (Supplier<Message>) messageSupplier, throwable);
    }

    /**
     * Logs a message object with the {@link Level#TRACE TRACE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    void trace(Marker marker, Object message);

    /**
     * Logs a message at the {@link Level#TRACE TRACE} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     * @see #debug(String)
     */
    void trace(Marker marker, Object message, Throwable throwable);

    /**
     * Logs a message object with the {@link Level#TRACE TRACE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message string to log.
     */
    void trace(Marker marker, String message);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#TRACE
     * TRACE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void trace(
            final Marker marker,
            final String message,
            final org.apache.logging.log4j.util.Supplier<?>... paramSuppliers) {
        if (isTraceEnabled(marker)) {
            trace(marker, message, Arrays.copyOf(paramSuppliers, paramSuppliers.length, Supplier[].class));
        }
    }

    /**
     * Logs a message at the {@link Level#TRACE TRACE} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     * @see #debug(String)
     */
    void trace(Marker marker, String message, Throwable throwable);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#TRACE TRACE} level with
     * the specified Marker.
     *
     * @param marker the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void trace(final Marker marker, final org.apache.logging.log4j.util.Supplier<?> messageSupplier) {
        trace(marker, (Supplier<?>) messageSupplier);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#TRACE TRACE} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param throwable A Throwable or null.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void trace(
            final Marker marker,
            final org.apache.logging.log4j.util.Supplier<?> messageSupplier,
            final Throwable throwable) {
        trace(marker, (Supplier<?>) messageSupplier, throwable);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#TRACE TRACE} level. The
     * {@code org.apache.logging.log4j.util.MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param messageSupplier A function, which when called, produces the desired log message.
     * @since 2.4
     */
    default void trace(final org.apache.logging.log4j.util.MessageSupplier messageSupplier) {
        trace((Supplier<Message>) messageSupplier);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#TRACE TRACE} level) including the
     * stack trace of the {@link Throwable} <code>throwable</code> passed as parameter. The {@code org.apache.logging.log4j.util.MessageSupplier} may or may
     * not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param messageSupplier A function, which when called, produces the desired log message.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     * @since 2.4
     */
    default void trace(final org.apache.logging.log4j.util.MessageSupplier messageSupplier, final Throwable throwable) {
        trace((Supplier<Message>) messageSupplier, throwable);
    }

    /**
     * Logs a message object with the {@link Level#TRACE TRACE} level.
     *
     * @param message the message object to log.
     */
    void trace(Object message);

    /**
     * Logs a message at the {@link Level#TRACE TRACE} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     * @see #debug(String)
     */
    void trace(Object message, Throwable throwable);

    /**
     * Logs a message object with the {@link Level#TRACE TRACE} level.
     *
     * @param message the message string to log.
     */
    void trace(String message);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#TRACE
     * TRACE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void trace(final String message, final org.apache.logging.log4j.util.Supplier<?>... paramSuppliers) {
        if (isTraceEnabled()) {
            trace(message, Arrays.copyOf(paramSuppliers, paramSuppliers.length, Supplier[].class));
        }
    }

    /**
     * Logs a message at the {@link Level#TRACE TRACE} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     * @see #debug(String)
     */
    void trace(String message, Throwable throwable);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#TRACE TRACE} level.
     *
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void trace(final org.apache.logging.log4j.util.Supplier<?> messageSupplier) {
        trace((Supplier<?>) messageSupplier);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#TRACE TRACE} level) including the
     * stack trace of the {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void trace(final org.apache.logging.log4j.util.Supplier<?> messageSupplier, final Throwable throwable) {
        trace((Supplier<?>) messageSupplier, throwable);
    }

    /**
     * Logs a message with parameters at trace level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     */
    void trace(Marker marker, String message, Object p0, Object p1, Object p2);

    /**
     * Logs a message with parameters at trace level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     */
    void trace(Marker marker, String message, Object p0, Object p1, Object p2, Object p3);

    /**
     * Logs a message with parameters at trace level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     */
    void trace(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4);

    /**
     * Logs a message with parameters at trace level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     */
    void trace(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5);

    /**
     * Logs a message with parameters at trace level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     */
    void trace(
            Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6);

    /**
     * Logs a message with parameters at trace level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     */
    void trace(
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
     * Logs a message with parameters at trace level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     */
    void trace(
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
     * Logs a message with parameters at trace level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @param p9 parameter to the message.
     */
    void trace(
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
     * Logs a message with parameters at trace level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     */
    void trace(String message, Object p0, Object p1, Object p2);

    /**
     * Logs a message with parameters at trace level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     */
    void trace(String message, Object p0, Object p1, Object p2, Object p3);

    /**
     * Logs a message with parameters at trace level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     */
    void trace(String message, Object p0, Object p1, Object p2, Object p3, Object p4);

    /**
     * Logs a message with parameters at trace level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     */
    void trace(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5);

    /**
     * Logs a message with parameters at trace level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     */
    void trace(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6);

    /**
     * Logs a message with parameters at trace level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     */
    void trace(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7);

    /**
     * Logs a message with parameters at trace level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     */
    void trace(
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
     * Logs a message with parameters at trace level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @param p9 parameter to the message.
     */
    void trace(
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
     * Logs entry to a method. Used when the method in question has no parameters or when the parameters should not be
     * logged.
     *
     * @return built message
     * @since 2.6
     */
    EntryMessage traceEntry();

    /**
     * Logs entry to a method along with its parameters. For example,
     *
     * <pre>
     * public void doSomething(String foo, int bar) {
     *     LOGGER.traceEntry("Parameters: {} and {}", foo, bar);
     *     // do something
     * }
     * </pre>
     * or:
     * <pre>
     * public int doSomething(String foo, int bar) {
     *     Message m = LOGGER.traceEntry("doSomething(foo={}, bar={})", foo, bar);
     *     // do something
     *     return traceExit(m, value);
     * }
     * </pre>
     *
     * @param format The format String for the parameters.
     * @param params The parameters to the method.
     * @return The built Message
     *
     * @since 2.6
     */
    EntryMessage traceEntry(String format, Object... params);

    /**
     * Logs entry to a method along with its parameters. For example,
     *
     * <pre>{@code
     * public void doSomething(Request foo) {
     *     LOGGER.traceEntry(()->gson.toJson(foo));
     *     // do something
     * }
     * }</pre>
     *
     * @param paramSuppliers The org.apache.logging.log4j.util.Suppliers for the parameters to the method.
     * @return built message
     *
     * @since 2.6
     */
    @SuppressWarnings("deprecation")
    EntryMessage traceEntry(org.apache.logging.log4j.util.Supplier<?>... paramSuppliers);

    /**
     * Logs entry to a method along with its parameters. For example,
     *
     * <pre>{@code
     * public void doSomething(String foo, int bar) {
     *     LOGGER.traceEntry("Parameters: {} and {}", ()->gson.toJson(foo), ()-> bar);
     *     // do something
     * }
     * }</pre>
     *
     * @param format The format String for the parameters.
     * @param paramSuppliers The org.apache.logging.log4j.util.Suppliers for the parameters to the method.
     * @return built message
     *
     * @since 2.6
     */
    @SuppressWarnings("deprecation")
    EntryMessage traceEntry(String format, org.apache.logging.log4j.util.Supplier<?>... paramSuppliers);

    /**
     * Logs entry to a method using a Message to describe the parameters.
     * <pre>
     * public void doSomething(Request foo) {
     *     LOGGER.traceEntry(new JsonMessage(foo));
     *     // do something
     * }
     * </pre>
     * <p>
     * Avoid passing a {@code ReusableMessage} to this method (therefore, also avoid passing messages created by
     * calling {@code logger.getMessageFactory().newMessage("some message")}): Log4j will replace such messages with
     * an immutable message to prevent situations where the reused message instance is modified by subsequent calls to
     * the logger before the returned {@code EntryMessage} is fully processed.
     * </p>
     *
     * @param message The message. Avoid specifying a ReusableMessage, use immutable messages instead.
     * @return the built message
     *
     * @since 2.6
     * @see org.apache.logging.log4j.message.ReusableMessage
     */
    EntryMessage traceEntry(Message message);

    /**
     * Logs exit from a method. Used for methods that do not return anything.
     *
     * @since 2.6
     */
    void traceExit();

    /**
     * Logs exiting from a method with the result. This may be coded as:
     *
     * <pre>
     * return LOGGER.traceExit(myResult);
     * </pre>
     *
     * @param <R> The type of the parameter and object being returned.
     * @param result The result being returned from the method call.
     * @return the result.
     *
     * @since 2.6
     */
    <R> R traceExit(R result);

    /**
     * Logs exiting from a method with the result. This may be coded as:
     *
     * <pre>
     * return LOGGER.traceExit("Result: {}", myResult);
     * </pre>
     *
     * @param <R> The type of the parameter and object being returned.
     * @param format The format String for the result.
     * @param result The result being returned from the method call.
     * @return the result.
     *
     * @since 2.6
     */
    <R> R traceExit(String format, R result);

    /**
     * Logs exiting from a method with no result. Allows custom formatting of the result. This may be coded as:
     *
     * <pre>
     * public long doSomething(int a, int b) {
     *    EntryMessage m = traceEntry("doSomething(a={}, b={})", a, b);
     *    // ...
     *    return LOGGER.traceExit(m);
     * }
     * </pre>
     * @param message The Message containing the formatted result.
     *
     * @since 2.6
     */
    void traceExit(EntryMessage message);

    /**
     * Logs exiting from a method with the result. Allows custom formatting of the result. This may be coded as:
     *
     * <pre>
     * public long doSomething(int a, int b) {
     *    EntryMessage m = traceEntry("doSomething(a={}, b={})", a, b);
     *    // ...
     *    return LOGGER.traceExit(m, myResult);
     * }
     * </pre>
     * @param message The Message containing the formatted result.
     * @param result The result being returned from the method call.
     *
     * @param <R> The type of the parameter and object being returned.
     * @return the result.
     *
     * @since 2.6
     */
    <R> R traceExit(EntryMessage message, R result);

    /**
     * Logs exiting from a method with the result. Allows custom formatting of the result. This may be coded as:
     *
     * <pre>
     * return LOGGER.traceExit(new JsonMessage(myResult), myResult);
     * </pre>
     * @param message The Message containing the formatted result.
     * @param result The result being returned from the method call.
     *
     * @param <R> The type of the parameter and object being returned.
     * @return the result.
     *
     * @since 2.6
     */
    <R> R traceExit(Message message, R result);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#WARN WARN} level with the
     * specified Marker. The {@code org.apache.logging.log4j.util.MessageSupplier} may or may not use the {@link MessageFactory} to construct the
     * {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message.
     * @since 2.4
     */
    default void warn(final Marker marker, final org.apache.logging.log4j.util.MessageSupplier messageSupplier) {
        warn(marker, (Supplier<Message>) messageSupplier);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#WARN WARN} level) with the
     * specified Marker and including the stack warn of the {@link Throwable} <code>throwable</code> passed as parameter. The
     * {@code org.apache.logging.log4j.util.MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message.
     * @param throwable A Throwable or null.
     * @since 2.4
     */
    default void warn(
            final Marker marker,
            final org.apache.logging.log4j.util.MessageSupplier messageSupplier,
            final Throwable throwable) {
        warn(marker, (Supplier<Message>) messageSupplier, throwable);
    }

    /**
     * Logs a message object with the {@link Level#WARN WARN} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    void warn(Marker marker, Object message);

    /**
     * Logs a message at the {@link Level#WARN WARN} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void warn(Marker marker, Object message, Throwable throwable);

    /**
     * Logs a message object with the {@link Level#WARN WARN} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    void warn(Marker marker, String message);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#WARN
     * WARN} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void warn(
            final Marker marker,
            final String message,
            final org.apache.logging.log4j.util.Supplier<?>... paramSuppliers) {
        if (isWarnEnabled(marker)) {
            warn(marker, message, Arrays.copyOf(paramSuppliers, paramSuppliers.length, Supplier[].class));
        }
    }

    /**
     * Logs a message at the {@link Level#WARN WARN} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void warn(Marker marker, String message, Throwable throwable);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#WARN WARN} level with the
     * specified Marker.
     *
     * @param marker the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void warn(final Marker marker, final org.apache.logging.log4j.util.Supplier<?> messageSupplier) {
        warn(marker, (Supplier<?>) messageSupplier);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#WARN WARN} level) with the
     * specified Marker and including the stack warn of the {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param throwable A Throwable or null.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void warn(
            final Marker marker,
            final org.apache.logging.log4j.util.Supplier<?> messageSupplier,
            final Throwable throwable) {
        warn(marker, (Supplier<?>) messageSupplier, throwable);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#WARN WARN} level. The
     * {@code org.apache.logging.log4j.util.MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param messageSupplier A function, which when called, produces the desired log message.
     * @since 2.4
     */
    default void warn(final org.apache.logging.log4j.util.MessageSupplier messageSupplier) {
        warn((Supplier<Message>) messageSupplier);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#WARN WARN} level) including the
     * stack warn of the {@link Throwable} <code>throwable</code> passed as parameter. The {@code org.apache.logging.log4j.util.MessageSupplier} may or may
     * not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param messageSupplier A function, which when called, produces the desired log message.
     * @param throwable the {@code Throwable} to log, including its stack warn.
     * @since 2.4
     */
    default void warn(final org.apache.logging.log4j.util.MessageSupplier messageSupplier, final Throwable throwable) {
        warn((Supplier<Message>) messageSupplier, throwable);
    }

    /**
     * Logs a message object with the {@link Level#WARN WARN} level.
     *
     * @param message the message object to log.
     */
    void warn(Object message);

    /**
     * Logs a message at the {@link Level#WARN WARN} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void warn(Object message, Throwable throwable);

    /**
     * Logs a message object with the {@link Level#WARN WARN} level.
     *
     * @param message the message string to log.
     */
    void warn(String message);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#WARN
     * WARN} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void warn(final String message, final org.apache.logging.log4j.util.Supplier<?>... paramSuppliers) {
        if (isWarnEnabled()) {
            warn(message, Arrays.copyOf(paramSuppliers, paramSuppliers.length, Supplier[].class));
        }
    }

    /**
     * Logs a message at the {@link Level#WARN WARN} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void warn(String message, Throwable throwable);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#WARN WARN} level.
     *
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void warn(final org.apache.logging.log4j.util.Supplier<?> messageSupplier) {
        warn((Supplier<?>) messageSupplier);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#WARN WARN} level) including the
     * stack warn of the {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param throwable the {@code Throwable} to log, including its stack warn.
     * @since 2.4
     */
    @SuppressWarnings("deprecation")
    default void warn(final org.apache.logging.log4j.util.Supplier<?> messageSupplier, final Throwable throwable) {
        warn((Supplier<?>) messageSupplier, throwable);
    }

    /**
     * Logs a message with parameters at warn level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     */
    void warn(Marker marker, String message, Object p0, Object p1, Object p2);

    /**
     * Logs a message with parameters at warn level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     */
    void warn(Marker marker, String message, Object p0, Object p1, Object p2, Object p3);

    /**
     * Logs a message with parameters at warn level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     */
    void warn(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4);

    /**
     * Logs a message with parameters at warn level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     */
    void warn(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5);

    /**
     * Logs a message with parameters at warn level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     */
    void warn(
            Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6);

    /**
     * Logs a message with parameters at warn level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     */
    void warn(
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
     * Logs a message with parameters at warn level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     */
    void warn(
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
     * Logs a message with parameters at warn level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @param p9 parameter to the message.
     */
    void warn(
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
     * Logs a message with parameters at warn level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     */
    void warn(String message, Object p0, Object p1, Object p2);

    /**
     * Logs a message with parameters at warn level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     */
    void warn(String message, Object p0, Object p1, Object p2, Object p3);

    /**
     * Logs a message with parameters at warn level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     */
    void warn(String message, Object p0, Object p1, Object p2, Object p3, Object p4);

    /**
     * Logs a message with parameters at warn level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     */
    void warn(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5);

    /**
     * Logs a message with parameters at warn level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     */
    void warn(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6);

    /**
     * Logs a message with parameters at warn level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     */
    void warn(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7);

    /**
     * Logs a message with parameters at warn level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     */
    void warn(
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
     * Logs a message with parameters at warn level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @param p9 parameter to the message.
     */
    void warn(
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
     * Logs a Message.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param fqcn The fully qualified class name of the logger entry point, used to determine the caller class and
     *            method when location information needs to be logged.
     * @param location The location of the caller.
     * @param message The message format.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     * @since 2.13.0
     */
    void logMessage(
            Level level, Marker marker, String fqcn, StackTraceElement location, Message message, Throwable throwable);
}
