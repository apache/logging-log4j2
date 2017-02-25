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

import org.apache.logging.log4j.message.EntryMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.MessageFactory2;
import org.apache.logging.log4j.util.MessageSupplier;
import org.apache.logging.log4j.util.Supplier;

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
 * Note that although {@link MessageSupplier} is provided, using {@link Supplier Supplier<Message>} works just the
 * same. MessageSupplier was deprecated in 2.6 and un-deprecated in 2.8.1. Anonymous class usage of these APIs
 * should prefer using Supplier instead.
 * </p>
 */
public interface Logger {

    /**
     * Logs an exception or error that has been caught to a specific logging level.
     *
     * @param level The logging Level.
     * @param t The Throwable.
     */
    void catching(Level level, Throwable t);

    /**
     * Logs an exception or error that has been caught. Normally, one may wish to provide additional information with an
     * exception while logging it; in these cases, one would not use this method. In other cases where simply logging
     * the fact that an exception was swallowed somewhere (e.g., at the top of the stack trace in a {@code main()}
     * method), this method is ideal for it.
     *
     * @param t The Throwable.
     */
    void catching(Throwable t);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     */
    void debug(Marker marker, Message msg);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    void debug(Marker marker, Message msg, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level with
     * the specified Marker. The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the
     * {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @since 2.4
     */
    void debug(Marker marker, MessageSupplier msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>t</code> passed as parameter. The
     * {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t A Throwable or null.
     * @since 2.4
     */
    void debug(Marker marker, MessageSupplier msgSupplier, Throwable t);

    /**
     * Logs a message CharSequence with the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message CharSequence to log.
     */
    void debug(Marker marker, CharSequence message);

    /**
     * Logs a message CharSequence at the {@link Level#DEBUG DEBUG} level including the stack trace of the
     * {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message CharSequence to log.
     * @param t the exception to log, including its stack trace.
     */
    void debug(Marker marker, CharSequence message, Throwable t);

    /**
     * Logs a message object with the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    void debug(Marker marker, Object message);

    /**
     * Logs a message at the {@link Level#DEBUG DEBUG} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    void debug(Marker marker, Object message, Throwable t);

    /**
     * Logs a message object with the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    void debug(Marker marker, String message);

    /**
     * Logs a message with parameters at the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param params parameters to the message.
     * @see #getMessageFactory()
     */
    void debug(Marker marker, String message, Object... params);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    void debug(Marker marker, String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message at the {@link Level#DEBUG DEBUG} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    void debug(Marker marker, String message, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level with
     * the specified Marker.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @since 2.4
     */
    void debug(Marker marker, Supplier<?> msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param t A Throwable or null.
     * @since 2.4
     */
    void debug(Marker marker, Supplier<?> msgSupplier, Throwable t);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param msg the message string to be logged
     */
    void debug(Message msg);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    void debug(Message msg, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level. The
     * {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @since 2.4
     */
    void debug(MessageSupplier msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level) including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter. The {@code MessageSupplier} may or may
     * not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t the exception to log, including its stack trace.
     * @since 2.4
     */
    void debug(MessageSupplier msgSupplier, Throwable t);

    /**
     * Logs a message CharSequence with the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message object to log.
     */
    void debug(CharSequence message);

    /**
     * Logs a CharSequence at the {@link Level#DEBUG DEBUG} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message CharSequence to log.
     * @param t the exception to log, including its stack trace.
     */
    void debug(CharSequence message, Throwable t);

    /**
     * Logs a message object with the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message object to log.
     */
    void debug(Object message);

    /**
     * Logs a message at the {@link Level#DEBUG DEBUG} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    void debug(Object message, Throwable t);

    /**
     * Logs a message object with the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message string to log.
     */
    void debug(String message);

    /**
     * Logs a message with parameters at the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param params parameters to the message.
     * @see #getMessageFactory()
     */
    void debug(String message, Object... params);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    void debug(String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message at the {@link Level#DEBUG DEBUG} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    void debug(String message, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level.
     *
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @since 2.4
     */
    void debug(Supplier<?> msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level) including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param t the exception to log, including its stack trace.
     * @since 2.4
     */
    void debug(Supplier<?> msgSupplier, Throwable t);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     */
    void debug(Marker marker, String message, Object p0);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     */
    void debug(Marker marker, String message, Object p0, Object p1);

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
    void debug(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
            Object p6);

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
    void debug(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
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
    void debug(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
            Object p7, Object p8);

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
    void debug(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
            Object p7, Object p8, Object p9);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     */
    void debug(String message, Object p0);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     */
    void debug(String message, Object p0, Object p1);

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
    void debug(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7,
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
    void debug(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7,
            Object p8, Object p9);

    /**
     * Logs entry to a method. Used when the method in question has no parameters or when the parameters should not be
     * logged.
     * @deprecated Use {@link #traceEntry()} instead which performs the same function.
     */
    @Deprecated
    void entry();

    /**
     * Logs entry to a method along with its parameters (consider using one of the {@code traceEntry(...)} methods instead.)
     * <p>
     * For example:
     * </p>
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
     * @param params The parameters to the method.
     */
    void entry(Object... params);

    /**
     * Logs a message with the specific Marker at the {@link Level#ERROR ERROR} level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     */
    void error(Marker marker, Message msg);

    /**
     * Logs a message with the specific Marker at the {@link Level#ERROR ERROR} level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    void error(Marker marker, Message msg, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#ERROR ERROR} level with
     * the specified Marker. The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the
     * {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @since 2.4
     */
    void error(Marker marker, MessageSupplier msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#ERROR ERROR} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>t</code> passed as parameter. The
     * {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t A Throwable or null.
     * @since 2.4
     */
    void error(Marker marker, MessageSupplier msgSupplier, Throwable t);

    /**
     * Logs a message CharSequence with the {@link Level#ERROR ERROR} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message CharSequence to log.
     */
    void error(Marker marker, CharSequence message);

    /**
     * Logs a CharSequence at the {@link Level#ERROR ERROR} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message CharSequence to log.
     * @param t the exception to log, including its stack trace.
     */
    void error(Marker marker, CharSequence message, Throwable t);

    /**
     * Logs a message object with the {@link Level#ERROR ERROR} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    void error(Marker marker, Object message);

    /**
     * Logs a message at the {@link Level#ERROR ERROR} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    void error(Marker marker, Object message, Throwable t);

    /**
     * Logs a message object with the {@link Level#ERROR ERROR} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    void error(Marker marker, String message);

    /**
     * Logs a message with parameters at the {@link Level#ERROR ERROR} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message to log; the format depends on the message factory.
     * @param params parameters to the message.
     * @see #getMessageFactory()
     */
    void error(Marker marker, String message, Object... params);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#ERROR
     * ERROR} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    void error(Marker marker, String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message at the {@link Level#ERROR ERROR} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    void error(Marker marker, String message, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#ERROR ERROR} level with
     * the specified Marker.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @since 2.4
     */
    void error(Marker marker, Supplier<?> msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#ERROR ERROR} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param t A Throwable or null.
     * @since 2.4
     */
    void error(Marker marker, Supplier<?> msgSupplier, Throwable t);

    /**
     * Logs a message with the specific Marker at the {@link Level#ERROR ERROR} level.
     *
     * @param msg the message string to be logged
     */
    void error(Message msg);

    /**
     * Logs a message with the specific Marker at the {@link Level#ERROR ERROR} level.
     *
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    void error(Message msg, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#ERROR ERROR} level. The
     * {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @since 2.4
     */
    void error(MessageSupplier msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#ERROR ERROR} level) including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter. The {@code MessageSupplier} may or may
     * not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t the exception to log, including its stack trace.
     * @since 2.4
     */
    void error(MessageSupplier msgSupplier, Throwable t);

    /**
     * Logs a message CharSequence with the {@link Level#ERROR ERROR} level.
     *
     * @param message the message CharSequence to log.
     */
    void error(CharSequence message);

    /**
     * Logs a CharSequence at the {@link Level#ERROR ERROR} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message CharSequence to log.
     * @param t the exception to log, including its stack trace.
     */
    void error(CharSequence message, Throwable t);

    /**
     * Logs a message object with the {@link Level#ERROR ERROR} level.
     *
     * @param message the message object to log.
     */
    void error(Object message);

    /**
     * Logs a message at the {@link Level#ERROR ERROR} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    void error(Object message, Throwable t);

    /**
     * Logs a message object with the {@link Level#ERROR ERROR} level.
     *
     * @param message the message string to log.
     */
    void error(String message);

    /**
     * Logs a message with parameters at the {@link Level#ERROR ERROR} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param params parameters to the message.
     * @see #getMessageFactory()
     */
    void error(String message, Object... params);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#ERROR
     * ERROR} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    void error(String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message at the {@link Level#ERROR ERROR} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    void error(String message, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#ERROR ERROR} level.
     *
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @since 2.4
     */
    void error(Supplier<?> msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#ERROR ERROR} level) including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param t the exception to log, including its stack trace.
     * @since 2.4
     */
    void error(Supplier<?> msgSupplier, Throwable t);

    /**
     * Logs a message with parameters at error level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     */
    void error(Marker marker, String message, Object p0);

    /**
     * Logs a message with parameters at error level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     */
    void error(Marker marker, String message, Object p0, Object p1);

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
    void error(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
            Object p6);

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
    void error(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
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
    void error(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
            Object p7, Object p8);

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
    void error(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
            Object p7, Object p8, Object p9);

    /**
     * Logs a message with parameters at error level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     */
    void error(String message, Object p0);

    /**
     * Logs a message with parameters at error level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     */
    void error(String message, Object p0, Object p1);

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
    void error(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7,
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
    void error(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7,
            Object p8, Object p9);

    /**
     * Logs exit from a method. Used for methods that do not return anything.
     * @deprecated Use {@link #traceExit()} instead which performs the same function.
     */
    @Deprecated
    void exit();

    /**
     * Logs exiting from a method with the result. This may be coded as:
     *
     * <pre>
     * return LOGGER.exit(myResult);
     * </pre>
     *
     * @param <R> The type of the parameter and object being returned.
     * @param result The result being returned from the method call.
     * @return the result.
     * @deprecated Use {@link #traceExit(Object)} instead which performs the same function.
     */
    @Deprecated
    <R> R exit(R result);

    /**
     * Logs a message with the specific Marker at the {@link Level#FATAL FATAL} level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     */
    void fatal(Marker marker, Message msg);

    /**
     * Logs a message with the specific Marker at the {@link Level#FATAL FATAL} level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    void fatal(Marker marker, Message msg, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#FATAL FATAL} level with
     * the specified Marker. The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the
     * {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @since 2.4
     */
    void fatal(Marker marker, MessageSupplier msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#FATAL FATAL} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>t</code> passed as parameter. The
     * {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t A Throwable or null.
     * @since 2.4
     */
    void fatal(Marker marker, MessageSupplier msgSupplier, Throwable t);

    /**
     * Logs a message CharSequence with the {@link Level#FATAL FATAL} level.
     *
     * @param marker The marker data specific to this log statement.
     * @param message the message CharSequence to log.
     */
    void fatal(Marker marker, CharSequence message);

    /**
     * Logs a CharSequence at the {@link Level#FATAL FATAL} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker The marker data specific to this log statement.
     * @param message the message CharSequence to log.
     * @param t the exception to log, including its stack trace.
     */
    void fatal(Marker marker, CharSequence message, Throwable t);

    /**
     * Logs a message object with the {@link Level#FATAL FATAL} level.
     *
     * @param marker The marker data specific to this log statement.
     * @param message the message object to log.
     */
    void fatal(Marker marker, Object message);

    /**
     * Logs a message at the {@link Level#FATAL FATAL} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker The marker data specific to this log statement.
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    void fatal(Marker marker, Object message, Throwable t);

    /**
     * Logs a message object with the {@link Level#FATAL FATAL} level.
     *
     * @param marker The marker data specific to this log statement.
     * @param message the message object to log.
     */
    void fatal(Marker marker, String message);

    /**
     * Logs a message with parameters at the {@link Level#FATAL FATAL} level.
     *
     * @param marker The marker data specific to this log statement.
     * @param message the message to log; the format depends on the message factory.
     * @param params parameters to the message.
     * @see #getMessageFactory()
     */
    void fatal(Marker marker, String message, Object... params);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#FATAL
     * FATAL} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    void fatal(Marker marker, String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message at the {@link Level#FATAL FATAL} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker The marker data specific to this log statement.
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    void fatal(Marker marker, String message, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#FATAL FATAL} level with
     * the specified Marker.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @since 2.4
     */
    void fatal(Marker marker, Supplier<?> msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#FATAL FATAL} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param t A Throwable or null.
     * @since 2.4
     */
    void fatal(Marker marker, Supplier<?> msgSupplier, Throwable t);

    /**
     * Logs a message with the specific Marker at the {@link Level#FATAL FATAL} level.
     *
     * @param msg the message string to be logged
     */
    void fatal(Message msg);

    /**
     * Logs a message with the specific Marker at the {@link Level#FATAL FATAL} level.
     *
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    void fatal(Message msg, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#FATAL FATAL} level. The
     * {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @since 2.4
     */
    void fatal(MessageSupplier msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#FATAL FATAL} level) including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter. The {@code MessageSupplier} may or may
     * not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t the exception to log, including its stack trace.
     * @since 2.4
     */
    void fatal(MessageSupplier msgSupplier, Throwable t);

    /**
     * Logs a message CharSequence with the {@link Level#FATAL FATAL} level.
     *
     * @param message the message CharSequence to log.
     */
    void fatal(CharSequence message);

    /**
     * Logs a CharSequence at the {@link Level#FATAL FATAL} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message CharSequence to log.
     * @param t the exception to log, including its stack trace.
     */
    void fatal(CharSequence message, Throwable t);

    /**
     * Logs a message object with the {@link Level#FATAL FATAL} level.
     *
     * @param message the message object to log.
     */
    void fatal(Object message);

    /**
     * Logs a message at the {@link Level#FATAL FATAL} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    void fatal(Object message, Throwable t);

    /**
     * Logs a message object with the {@link Level#FATAL FATAL} level.
     *
     * @param message the message string to log.
     */
    void fatal(String message);

    /**
     * Logs a message with parameters at the {@link Level#FATAL FATAL} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param params parameters to the message.
     * @see #getMessageFactory()
     */
    void fatal(String message, Object... params);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#FATAL
     * FATAL} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    void fatal(String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message at the {@link Level#FATAL FATAL} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    void fatal(String message, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#FATAL FATAL} level.
     *
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @since 2.4
     */
    void fatal(Supplier<?> msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#FATAL FATAL} level) including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param t the exception to log, including its stack trace.
     * @since 2.4
     */
    void fatal(Supplier<?> msgSupplier, Throwable t);

    /**
     * Logs a message with parameters at fatal level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     */
    void fatal(Marker marker, String message, Object p0);

    /**
     * Logs a message with parameters at fatal level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     */
    void fatal(Marker marker, String message, Object p0, Object p1);

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
    void fatal(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
            Object p6);

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
    void fatal(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
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
    void fatal(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
            Object p7, Object p8);

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
    void fatal(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
            Object p7, Object p8, Object p9);

    /**
     * Logs a message with parameters at fatal level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     */
    void fatal(String message, Object p0);

    /**
     * Logs a message with parameters at fatal level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     */
    void fatal(String message, Object p0, Object p1);

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
    void fatal(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7,
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
    void fatal(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7,
            Object p8, Object p9);

    /**
     * Gets the Level associated with the Logger.
     *
     * @return the Level associate with the Logger.
     */
    Level getLevel();

    /**
     * Gets the message factory used to convert message Objects and Strings/CharSequences into actual log Messages.
     *
     * Since version 2.6, Log4j internally uses message factories that implement the {@link MessageFactory2} interface.
     * From version 2.6.2, the return type of this method was changed from {@link MessageFactory} to
     * {@code <MF extends MessageFactory> MF}. The returned factory will always implement {@link MessageFactory2},
     * but the return type of this method could not be changed to {@link MessageFactory2} without breaking binary
     * compatibility.
     *
     * @return the message factory, as an instance of {@link MessageFactory2}
     */
    <MF extends MessageFactory> MF getMessageFactory();

    /**
     * Gets the logger name.
     *
     * @return the logger name.
     */
    String getName();

    /**
     * Logs a message with the specific Marker at the {@link Level#INFO INFO} level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     */
    void info(Marker marker, Message msg);

    /**
     * Logs a message with the specific Marker at the {@link Level#INFO INFO} level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    void info(Marker marker, Message msg, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#INFO INFO} level with the
     * specified Marker. The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the
     * {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @since 2.4
     */
    void info(Marker marker, MessageSupplier msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#INFO INFO} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>t</code> passed as parameter. The
     * {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t A Throwable or null.
     * @since 2.4
     */
    void info(Marker marker, MessageSupplier msgSupplier, Throwable t);

    /**
     * Logs a message CharSequence with the {@link Level#INFO INFO} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message CharSequence to log.
     */
    void info(Marker marker, CharSequence message);

    /**
     * Logs a CharSequence at the {@link Level#INFO INFO} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message CharSequence to log.
     * @param t the exception to log, including its stack trace.
     */
    void info(Marker marker, CharSequence message, Throwable t);

    /**
     * Logs a message object with the {@link Level#INFO INFO} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    void info(Marker marker, Object message);

    /**
     * Logs a message at the {@link Level#INFO INFO} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    void info(Marker marker, Object message, Throwable t);

    /**
     * Logs a message object with the {@link Level#INFO INFO} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    void info(Marker marker, String message);

    /**
     * Logs a message with parameters at the {@link Level#INFO INFO} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param params parameters to the message.
     * @see #getMessageFactory()
     */
    void info(Marker marker, String message, Object... params);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#INFO
     * INFO} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    void info(Marker marker, String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message at the {@link Level#INFO INFO} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    void info(Marker marker, String message, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#INFO INFO} level with the
     * specified Marker.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @since 2.4
     */
    void info(Marker marker, Supplier<?> msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#INFO INFO} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param t A Throwable or null.
     * @since 2.4
     */
    void info(Marker marker, Supplier<?> msgSupplier, Throwable t);

    /**
     * Logs a message with the specific Marker at the {@link Level#INFO INFO} level.
     *
     * @param msg the message string to be logged
     */
    void info(Message msg);

    /**
     * Logs a message with the specific Marker at the {@link Level#INFO INFO} level.
     *
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    void info(Message msg, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#INFO INFO} level. The
     * {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @since 2.4
     */
    void info(MessageSupplier msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#INFO INFO} level) including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter. The {@code MessageSupplier} may or may
     * not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t the exception to log, including its stack trace.
     * @since 2.4
     */
    void info(MessageSupplier msgSupplier, Throwable t);

    /**
     * Logs a message CharSequence with the {@link Level#INFO INFO} level.
     *
     * @param message the message CharSequence to log.
     */
    void info(CharSequence message);

    /**
     * Logs a CharSequence at the {@link Level#INFO INFO} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message CharSequence to log.
     * @param t the exception to log, including its stack trace.
     */
    void info(CharSequence message, Throwable t);

    /**
     * Logs a message object with the {@link Level#INFO INFO} level.
     *
     * @param message the message object to log.
     */
    void info(Object message);

    /**
     * Logs a message at the {@link Level#INFO INFO} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    void info(Object message, Throwable t);

    /**
     * Logs a message object with the {@link Level#INFO INFO} level.
     *
     * @param message the message string to log.
     */
    void info(String message);

    /**
     * Logs a message with parameters at the {@link Level#INFO INFO} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param params parameters to the message.
     * @see #getMessageFactory()
     */
    void info(String message, Object... params);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#INFO
     * INFO} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    void info(String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message at the {@link Level#INFO INFO} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    void info(String message, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#INFO INFO} level.
     *
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @since 2.4
     */
    void info(Supplier<?> msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#INFO INFO} level) including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param t the exception to log, including its stack trace.
     * @since 2.4
     */
    void info(Supplier<?> msgSupplier, Throwable t);

    /**
     * Logs a message with parameters at info level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     */
    void info(Marker marker, String message, Object p0);

    /**
     * Logs a message with parameters at info level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     */
    void info(Marker marker, String message, Object p0, Object p1);

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
    void info(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
            Object p6);

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
    void info(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
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
    void info(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
            Object p7, Object p8);

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
    void info(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
            Object p7, Object p8, Object p9);

    /**
     * Logs a message with parameters at info level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     */
    void info(String message, Object p0);

    /**
     * Logs a message with parameters at info level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     */
    void info(String message, Object p0, Object p1);

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
    void info(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7,
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
    void info(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7,
            Object p8, Object p9);

    /**
     * Checks whether this Logger is enabled for the {@link Level#DEBUG DEBUG} Level.
     *
     * @return boolean - {@code true} if this Logger is enabled for level DEBUG, {@code false} otherwise.
     */
    boolean isDebugEnabled();

    /**
     * Checks whether this Logger is enabled for the {@link Level#DEBUG DEBUG} Level.
     *
     * @param marker The Marker to check
     * @return boolean - {@code true} if this Logger is enabled for level DEBUG, {@code false} otherwise.
     */
    boolean isDebugEnabled(Marker marker);

    /**
     * Checks whether this Logger is enabled for the given Level.
     * <p>
     * Note that passing in {@link Level#OFF OFF} always returns {@code true}.
     * </p>
     *
     * @param level the Level to check
     * @return boolean - {@code true} if this Logger is enabled for level, {@code false} otherwise.
     */
    boolean isEnabled(Level level);

    /**
     * Checks whether this Logger is enabled for the given Level and Marker.
     *
     * @param level The Level to check
     * @param marker The Marker to check
     * @return boolean - {@code true} if this Logger is enabled for level and marker, {@code false} otherwise.
     */
    boolean isEnabled(Level level, Marker marker);

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
     * @param marker The Marker to check
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
     * @param marker The Marker to check
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
     * @param marker The Marker to check
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
     * @param marker The Marker to check
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
     * @param marker The Marker to check
     * @return boolean - {@code true} if this Logger is enabled for level {@link Level#WARN WARN}, {@code false}
     *         otherwise.
     */
    boolean isWarnEnabled(Marker marker);

    /**
     * Logs a message with the specific Marker at the given level.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     */
    void log(Level level, Marker marker, Message msg);

    /**
     * Logs a message with the specific Marker at the given level.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    void log(Level level, Marker marker, Message msg, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the specified level with the specified
     * Marker. The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the
     * {@code Message}.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @since 2.4
     */
    void log(Level level, Marker marker, MessageSupplier msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the specified level) with the specified Marker and
     * including the stack log of the {@link Throwable} <code>t</code> passed as parameter. The {@code MessageSupplier}
     * may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t A Throwable or null.
     * @since 2.4
     */
    void log(Level level, Marker marker, MessageSupplier msgSupplier, Throwable t);

    /**
     * Logs a message CharSequence with the given level.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param message the message CharSequence to log.
     */
    void log(Level level, Marker marker, CharSequence message);

    /**
     * Logs a CharSequence at the given level including the stack trace of the {@link Throwable} <code>t</code> passed as
     * parameter.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param message the message CharSequence to log.
     * @param t the exception to log, including its stack trace.
     */
    void log(Level level, Marker marker, CharSequence message, Throwable t);

    /**
     * Logs a message object with the given level.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    void log(Level level, Marker marker, Object message);

    /**
     * Logs a message at the given level including the stack trace of the {@link Throwable} <code>t</code> passed as
     * parameter.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    void log(Level level, Marker marker, Object message, Throwable t);

    /**
     * Logs a message object with the given level.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    void log(Level level, Marker marker, String message);

    /**
     * Logs a message with parameters at the given level.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param params parameters to the message.
     * @see #getMessageFactory()
     */
    void log(Level level, Marker marker, String message, Object... params);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the specified level.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    void log(Level level, Marker marker, String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message at the given level including the stack trace of the {@link Throwable} <code>t</code> passed as
     * parameter.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    void log(Level level, Marker marker, String message, Throwable t);

    /**
     * Logs a message (only to be constructed if the logging level is the specified level) with the specified Marker.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @since 2.4
     */
    void log(Level level, Marker marker, Supplier<?> msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the specified level) with the specified Marker and
     * including the stack log of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param t A Throwable or null.
     * @since 2.4
     */
    void log(Level level, Marker marker, Supplier<?> msgSupplier, Throwable t);

    /**
     * Logs a message with the specific Marker at the given level.
     *
     * @param level the logging level
     * @param msg the message string to be logged
     */
    void log(Level level, Message msg);

    /**
     * Logs a message with the specific Marker at the given level.
     *
     * @param level the logging level
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    void log(Level level, Message msg, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the specified level. The
     * {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param level the logging level
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @since 2.4
     */
    void log(Level level, MessageSupplier msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the specified level) including the stack log of
     * the {@link Throwable} <code>t</code> passed as parameter. The {@code MessageSupplier} may or may not use the
     * {@link MessageFactory} to construct the {@code Message}.
     *
     * @param level the logging level
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t the exception to log, including its stack log.
     * @since 2.4
     */
    void log(Level level, MessageSupplier msgSupplier, Throwable t);

    /**
     * Logs a message CharSequence with the given level.
     *
     * @param level the logging level
     * @param message the message CharSequence to log.
     */
    void log(Level level, CharSequence message);

    /**
     * Logs a CharSequence at the given level including the stack trace of the {@link Throwable} <code>t</code> passed as
     * parameter.
     *
     * @param level the logging level
     * @param message the message CharSequence to log.
     * @param t the exception to log, including its stack trace.
     */
    void log(Level level, CharSequence message, Throwable t);

    /**
     * Logs a message object with the given level.
     *
     * @param level the logging level
     * @param message the message object to log.
     */
    void log(Level level, Object message);

    /**
     * Logs a message at the given level including the stack trace of the {@link Throwable} <code>t</code> passed as
     * parameter.
     *
     * @param level the logging level
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    void log(Level level, Object message, Throwable t);

    /**
     * Logs a message object with the given level.
     *
     * @param level the logging level
     * @param message the message string to log.
     */
    void log(Level level, String message);

    /**
     * Logs a message with parameters at the given level.
     *
     * @param level the logging level
     * @param message the message to log; the format depends on the message factory.
     * @param params parameters to the message.
     * @see #getMessageFactory()
     */
    void log(Level level, String message, Object... params);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the specified level.
     *
     * @param level the logging level
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    void log(Level level, String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message at the given level including the stack trace of the {@link Throwable} <code>t</code> passed as
     * parameter.
     *
     * @param level the logging level
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    void log(Level level, String message, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the specified level.
     *
     * @param level the logging level
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @since 2.4
     */
    void log(Level level, Supplier<?> msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the specified level) including the stack log of
     * the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param level the logging level
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param t the exception to log, including its stack log.
     * @since 2.4
     */
    void log(Level level, Supplier<?> msgSupplier, Throwable t);

    /**
     * Logs a message with parameters at the specified level.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     */
    void log(Level level, Marker marker, String message, Object p0);

    /**
     * Logs a message with parameters at the specified level.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     */
    void log(Level level, Marker marker, String message, Object p0, Object p1);

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
    void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5);

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
    void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
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
    void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
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
    void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
            Object p7, Object p8);

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
    void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
            Object p7, Object p8, Object p9);

    /**
     * Logs a message with parameters at the specified level.
     *
     * @param level the logging level
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     */
    void log(Level level, String message, Object p0);

    /**
     * Logs a message with parameters at the specified level.
     *
     * @param level the logging level
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     */
    void log(Level level, String message, Object p0, Object p1);

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
    void log(Level level, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7);

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
    void log(Level level, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7,
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
    void log(Level level, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7,
            Object p8, Object p9);

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
     * Logs an exception or error to be thrown. This may be coded as:
     *
     * <pre>
     * throw logger.throwing(Level.DEBUG, myException);
     * </pre>
     *
     * @param <T> the Throwable type.
     * @param level The logging Level.
     * @param t The Throwable.
     * @return the Throwable.
     */
    <T extends Throwable> T throwing(Level level, T t);

    /**
     * Logs an exception or error to be thrown. This may be coded as:
     *
     * <pre>
     * throw logger.throwing(myException);
     * </pre>
     *
     * @param <T> the Throwable type.
     * @param t The Throwable.
     * @return the Throwable.
     */
    <T extends Throwable> T throwing(T t);

    /**
     * Logs a message with the specific Marker at the {@link Level#TRACE TRACE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     */
    void trace(Marker marker, Message msg);

    /**
     * Logs a message with the specific Marker at the {@link Level#TRACE TRACE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    void trace(Marker marker, Message msg, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#TRACE TRACE} level with
     * the specified Marker. The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the
     * {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @since 2.4
     */
    void trace(Marker marker, MessageSupplier msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#TRACE TRACE} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>t</code> passed as parameter. The
     * {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t A Throwable or null.
     * @since 2.4
     */
    void trace(Marker marker, MessageSupplier msgSupplier, Throwable t);

    /**
     * Logs a message CharSequence with the {@link Level#TRACE TRACE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message CharSequence to log.
     */
    void trace(Marker marker, CharSequence message);

    /**
     * Logs a CharSequence at the {@link Level#TRACE TRACE} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message CharSequence to log.
     * @param t the exception to log, including its stack trace.
     * @see #debug(String)
     */
    void trace(Marker marker, CharSequence message, Throwable t);

    /**
     * Logs a message object with the {@link Level#TRACE TRACE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    void trace(Marker marker, Object message);

    /**
     * Logs a message at the {@link Level#TRACE TRACE} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     * @see #debug(String)
     */
    void trace(Marker marker, Object message, Throwable t);

    /**
     * Logs a message object with the {@link Level#TRACE TRACE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message string to log.
     */
    void trace(Marker marker, String message);

    /**
     * Logs a message with parameters at the {@link Level#TRACE TRACE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param params parameters to the message.
     * @see #getMessageFactory()
     */
    void trace(Marker marker, String message, Object... params);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#TRACE
     * TRACE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    void trace(Marker marker, String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message at the {@link Level#TRACE TRACE} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     * @see #debug(String)
     */
    void trace(Marker marker, String message, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#TRACE TRACE} level with
     * the specified Marker.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @since 2.4
     */
    void trace(Marker marker, Supplier<?> msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#TRACE TRACE} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param t A Throwable or null.
     * @since 2.4
     */
    void trace(Marker marker, Supplier<?> msgSupplier, Throwable t);

    /**
     * Logs a message with the specific Marker at the {@link Level#TRACE TRACE} level.
     *
     * @param msg the message string to be logged
     */
    void trace(Message msg);

    /**
     * Logs a message with the specific Marker at the {@link Level#TRACE TRACE} level.
     *
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    void trace(Message msg, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#TRACE TRACE} level. The
     * {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @since 2.4
     */
    void trace(MessageSupplier msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#TRACE TRACE} level) including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter. The {@code MessageSupplier} may or may
     * not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t the exception to log, including its stack trace.
     * @since 2.4
     */
    void trace(MessageSupplier msgSupplier, Throwable t);

    /**
     * Logs a message CharSequence with the {@link Level#TRACE TRACE} level.
     *
     * @param message the message CharSequence to log.
     */
    void trace(CharSequence message);

    /**
     * Logs a CharSequence at the {@link Level#TRACE TRACE} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message CharSequence to log.
     * @param t the exception to log, including its stack trace.
     * @see #debug(String)
     */
    void trace(CharSequence message, Throwable t);

    /**
     * Logs a message object with the {@link Level#TRACE TRACE} level.
     *
     * @param message the message object to log.
     */
    void trace(Object message);

    /**
     * Logs a message at the {@link Level#TRACE TRACE} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     * @see #debug(String)
     */
    void trace(Object message, Throwable t);

    /**
     * Logs a message object with the {@link Level#TRACE TRACE} level.
     *
     * @param message the message string to log.
     */
    void trace(String message);

    /**
     * Logs a message with parameters at the {@link Level#TRACE TRACE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param params parameters to the message.
     * @see #getMessageFactory()
     */
    void trace(String message, Object... params);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#TRACE
     * TRACE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    void trace(String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message at the {@link Level#TRACE TRACE} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     * @see #debug(String)
     */
    void trace(String message, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#TRACE TRACE} level.
     *
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @since 2.4
     */
    void trace(Supplier<?> msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#TRACE TRACE} level) including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param t the exception to log, including its stack trace.
     * @since 2.4
     */
    void trace(Supplier<?> msgSupplier, Throwable t);

    /**
     * Logs a message with parameters at trace level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     */
    void trace(Marker marker, String message, Object p0);

    /**
     * Logs a message with parameters at trace level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     */
    void trace(Marker marker, String message, Object p0, Object p1);

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
    void trace(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
            Object p6);

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
    void trace(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
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
    void trace(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
            Object p7, Object p8);

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
    void trace(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
            Object p7, Object p8, Object p9);

    /**
     * Logs a message with parameters at trace level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     */
    void trace(String message, Object p0);

    /**
     * Logs a message with parameters at trace level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     */
    void trace(String message, Object p0, Object p1);

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
    void trace(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7,
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
    void trace(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7,
            Object p8, Object p9);

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
     * <pre>
     * public void doSomething(Request foo) {
     *     LOGGER.traceEntry(()->gson.toJson(foo));
     *     // do something
     * }
     * </pre>
     *
     * @param paramSuppliers The Suppliers for the parameters to the method.
     * @return built message
     *
     * @since 2.6
     */
    EntryMessage traceEntry(Supplier<?>... paramSuppliers);

    /**
     * Logs entry to a method along with its parameters. For example,
     *
     * <pre>
     * public void doSomething(String foo, int bar) {
     *     LOGGER.traceEntry("Parameters: {} and {}", ()->gson.toJson(foo), ()-> bar);
     *     // do something
     * }
     * </pre>
     *
     * @param format The format String for the parameters.
     * @param paramSuppliers The Suppliers for the parameters to the method.
     * @return built message
     *
     * @since 2.6
     */
    EntryMessage traceEntry(String format, Supplier<?>... paramSuppliers);

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
     * Logs a message with the specific Marker at the {@link Level#WARN WARN} level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     */
    void warn(Marker marker, Message msg);

    /**
     * Logs a message with the specific Marker at the {@link Level#WARN WARN} level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    void warn(Marker marker, Message msg, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#WARN WARN} level with the
     * specified Marker. The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the
     * {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @since 2.4
     */
    void warn(Marker marker, MessageSupplier msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#WARN WARN} level) with the
     * specified Marker and including the stack warn of the {@link Throwable} <code>t</code> passed as parameter. The
     * {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t A Throwable or null.
     * @since 2.4
     */
    void warn(Marker marker, MessageSupplier msgSupplier, Throwable t);

    /**
     * Logs a message CharSequence with the {@link Level#WARN WARN} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message CharSequence to log.
     */
    void warn(Marker marker, CharSequence message);

    /**
     * Logs a CharSequence at the {@link Level#WARN WARN} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message CharSequence to log.
     * @param t the exception to log, including its stack trace.
     */
    void warn(Marker marker, CharSequence message, Throwable t);

    /**
     * Logs a message object with the {@link Level#WARN WARN} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    void warn(Marker marker, Object message);

    /**
     * Logs a message at the {@link Level#WARN WARN} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    void warn(Marker marker, Object message, Throwable t);

    /**
     * Logs a message object with the {@link Level#WARN WARN} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    void warn(Marker marker, String message);

    /**
     * Logs a message with parameters at the {@link Level#WARN WARN} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message to log; the format depends on the message factory.
     * @param params parameters to the message.
     * @see #getMessageFactory()
     */
    void warn(Marker marker, String message, Object... params);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#WARN
     * WARN} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    void warn(Marker marker, String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message at the {@link Level#WARN WARN} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    void warn(Marker marker, String message, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#WARN WARN} level with the
     * specified Marker.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @since 2.4
     */
    void warn(Marker marker, Supplier<?> msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#WARN WARN} level) with the
     * specified Marker and including the stack warn of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param t A Throwable or null.
     * @since 2.4
     */
    void warn(Marker marker, Supplier<?> msgSupplier, Throwable t);

    /**
     * Logs a message with the specific Marker at the {@link Level#WARN WARN} level.
     *
     * @param msg the message string to be logged
     */
    void warn(Message msg);

    /**
     * Logs a message with the specific Marker at the {@link Level#WARN WARN} level.
     *
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    void warn(Message msg, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#WARN WARN} level. The
     * {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @since 2.4
     */
    void warn(MessageSupplier msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#WARN WARN} level) including the
     * stack warn of the {@link Throwable} <code>t</code> passed as parameter. The {@code MessageSupplier} may or may
     * not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t the exception to log, including its stack warn.
     * @since 2.4
     */
    void warn(MessageSupplier msgSupplier, Throwable t);

    /**
     * Logs a message CharSequence with the {@link Level#WARN WARN} level.
     *
     * @param message the message CharSequence to log.
     */
    void warn(CharSequence message);

    /**
     * Logs a CharSequence at the {@link Level#WARN WARN} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message CharSequence to log.
     * @param t the exception to log, including its stack trace.
     */
    void warn(CharSequence message, Throwable t);

    /**
     * Logs a message object with the {@link Level#WARN WARN} level.
     *
     * @param message the message object to log.
     */
    void warn(Object message);

    /**
     * Logs a message at the {@link Level#WARN WARN} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    void warn(Object message, Throwable t);

    /**
     * Logs a message object with the {@link Level#WARN WARN} level.
     *
     * @param message the message string to log.
     */
    void warn(String message);

    /**
     * Logs a message with parameters at the {@link Level#WARN WARN} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param params parameters to the message.
     * @see #getMessageFactory()
     */
    void warn(String message, Object... params);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#WARN
     * WARN} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    void warn(String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message at the {@link Level#WARN WARN} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    void warn(String message, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#WARN WARN} level.
     *
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @since 2.4
     */
    void warn(Supplier<?> msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#WARN WARN} level) including the
     * stack warn of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param t the exception to log, including its stack warn.
     * @since 2.4
     */
    void warn(Supplier<?> msgSupplier, Throwable t);

    /**
     * Logs a message with parameters at warn level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     */
    void warn(Marker marker, String message, Object p0);

    /**
     * Logs a message with parameters at warn level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     */
    void warn(Marker marker, String message, Object p0, Object p1);

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
    void warn(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
            Object p6);

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
    void warn(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
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
    void warn(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
            Object p7, Object p8);

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
    void warn(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
            Object p7, Object p8, Object p9);

    /**
     * Logs a message with parameters at warn level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     */
    void warn(String message, Object p0);

    /**
     * Logs a message with parameters at warn level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     */
    void warn(String message, Object p0, Object p1);

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
    void warn(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7,
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
    void warn(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7,
            Object p8, Object p9);

}
