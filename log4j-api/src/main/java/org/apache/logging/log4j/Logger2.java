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

import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.util.MessageSupplier;
import org.apache.logging.log4j.util.Supplier;

/**
 * Extends the {@code Logger} interface with support for lambda expressions.
 * <p>
 * This logger allows client code to lazily log messages without explicitly checking if the requested log level is
 * enabled. For example, previously one would write:
 * 
 * <pre>
 * // pre-Java 8 style optimization: explicitly check the log level
 * // to make sure the expensiveOperation() method is only called if necessary
 * Logger logger = LogManager.getLogger();
 * if (logger.isTraceEnabled()) {
 *     logger.trace(&quot;Some long-running operation returned {}&quot;, expensiveOperation());
 * }
 * </pre>
 * <p>
 * With Java 8 and the {@code Logger2} interface, one can achieve the same effect by using a lambda expression:
 * 
 * <pre>
 * // Java-8 style optimization: no need to explicitly check the log level:
 * // the lambda expression is not evaluated if the TRACE level is not enabled
 * Logger2 logger = LogManager.getLogger2();
 * logger.trace(&quot;Some long-running operation returned {}&quot;, () -&gt; expensiveOperation());
 * </pre>
 * 
 * @since 2.4
 */
public interface Logger2 extends Logger {

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level.
     *
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     */
    void debug(Supplier<?> msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level) including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param t the exception to log, including its stack trace.
     */
    void debug(Supplier<?> msgSupplier, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level with
     * the specified Marker.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
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
     */
    void debug(Marker marker, Supplier<?> msgSupplier, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level with
     * the specified Marker. The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the
     * {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
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
     */
    void debug(Marker marker, MessageSupplier msgSupplier, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level. The
     * {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     */
    void debug(MessageSupplier msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level) including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter. The {@code MessageSupplier} may or may
     * not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t the exception to log, including its stack trace.
     */
    void debug(MessageSupplier msgSupplier, Throwable t);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     */
    void debug(Marker marker, String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     */
    void debug(String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#ERROR ERROR} level.
     *
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     */
    void error(Supplier<?> msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#ERROR ERROR} level) including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param t the exception to log, including its stack trace.
     */
    void error(Supplier<?> msgSupplier, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#ERROR ERROR} level with
     * the specified Marker.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
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
     */
    void error(Marker marker, Supplier<?> msgSupplier, Throwable t);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#ERROR
     * ERROR} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     */
    void error(Marker marker, String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#ERROR ERROR} level with
     * the specified Marker. 
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     */
    void error(Marker marker, MessageSupplier msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#ERROR ERROR} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t A Throwable or null.
     */
    void error(Marker marker, MessageSupplier msgSupplier, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#ERROR ERROR} level.
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     */
    void error(MessageSupplier msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#ERROR ERROR} level) including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t the exception to log, including its stack trace.
     */
    void error(MessageSupplier msgSupplier, Throwable t);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#ERROR
     * ERROR} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     */
    void error(String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#FATAL FATAL} level.
     *
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     */
    void fatal(Supplier<?> msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#FATAL FATAL} level) including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param t the exception to log, including its stack trace.
     */
    void fatal(Supplier<?> msgSupplier, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#FATAL FATAL} level with
     * the specified Marker.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
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
     */
    void fatal(Marker marker, Supplier<?> msgSupplier, Throwable t);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#FATAL
     * FATAL} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     */
    void fatal(Marker marker, String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#FATAL FATAL} level with
     * the specified Marker. 
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     */
    void fatal(Marker marker, MessageSupplier msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#FATAL FATAL} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t A Throwable or null.
     */
    void fatal(Marker marker, MessageSupplier msgSupplier, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#FATAL FATAL} level.
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     */
    void fatal(MessageSupplier msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#FATAL FATAL} level) including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t the exception to log, including its stack trace.
     */
    void fatal(MessageSupplier msgSupplier, Throwable t);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#FATAL
     * FATAL} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     */
    void fatal(String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#INFO INFO} level.
     *
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     */
    void info(Supplier<?> msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#INFO INFO} level) including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param t the exception to log, including its stack trace.
     */
    void info(Supplier<?> msgSupplier, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#INFO INFO} level with the
     * specified Marker.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
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
     */
    void info(Marker marker, Supplier<?> msgSupplier, Throwable t);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#INFO
     * INFO} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     */
    void info(Marker marker, String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#INFO INFO} level with
     * the specified Marker. 
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     */
    void info(Marker marker, MessageSupplier msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#INFO INFO} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t A Throwable or null.
     */
    void info(Marker marker, MessageSupplier msgSupplier, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#INFO INFO} level.
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     */
    void info(MessageSupplier msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#INFO INFO} level) including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t the exception to log, including its stack trace.
     */
    void info(MessageSupplier msgSupplier, Throwable t);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#INFO
     * INFO} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     */
    void info(String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message which is only to be constructed if the logging level is the specified level.
     *
     * @param level the logging level
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
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
     */
    void log(Level level, Supplier<?> msgSupplier, Throwable t);

    /**
     * Logs a message (only to be constructed if the logging level is the specified level) with the specified Marker.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
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
     */
    void log(Level level, Marker marker, Supplier<?> msgSupplier, Throwable t);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the specified level.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     */
    void log(Level level, Marker marker, String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message which is only to be constructed if the logging level is the specified level with
     * the specified Marker. 
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     */
    void log(Level level, Marker marker, MessageSupplier msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the specified level) with the
     * specified Marker and including the stack log of the {@link Throwable} <code>t</code> passed as parameter.
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t A Throwable or null.
     */
    void log(Level level, Marker marker, MessageSupplier msgSupplier, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the specified level.
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param level the logging level
     * @param msgSupplier A function, which when called, produces the desired log message.
     */
    void log(Level level, MessageSupplier msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the specified level) including the
     * stack log of the {@link Throwable} <code>t</code> passed as parameter.
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param level the logging level
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t the exception to log, including its stack log.
     */
    void log(Level level, MessageSupplier msgSupplier, Throwable t);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the specified level.
     *
     * @param level the logging level
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     */
    void log(Level level, String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#TRACE TRACE} level.
     *
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     */
    void trace(Supplier<?> msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#TRACE TRACE} level) including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param t the exception to log, including its stack trace.
     */
    void trace(Supplier<?> msgSupplier, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#TRACE TRACE} level with
     * the specified Marker.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
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
     */
    void trace(Marker marker, Supplier<?> msgSupplier, Throwable t);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#TRACE
     * TRACE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     */
    void trace(Marker marker, String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#TRACE TRACE} level with
     * the specified Marker. 
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     */
    void trace(Marker marker, MessageSupplier msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#TRACE TRACE} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t A Throwable or null.
     */
    void trace(Marker marker, MessageSupplier msgSupplier, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#TRACE TRACE} level.
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     */
    void trace(MessageSupplier msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#TRACE TRACE} level) including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t the exception to log, including its stack trace.
     */
    void trace(MessageSupplier msgSupplier, Throwable t);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#TRACE
     * TRACE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     */
    void trace(String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#WARN WARN} level.
     *
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     */
    void warn(Supplier<?> msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#WARN WARN} level) including the
     * stack warn of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
     * @param t the exception to log, including its stack warn.
     */
    void warn(Supplier<?> msgSupplier, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#WARN WARN} level with the
     * specified Marker.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message; the format depends on the
     *            message factory.
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
     */
    void warn(Marker marker, Supplier<?> msgSupplier, Throwable t);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#WARN
     * WARN} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     */
    void warn(Marker marker, String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#WARN WARN} level with
     * the specified Marker. 
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     */
    void warn(Marker marker, MessageSupplier msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#WARN WARN} level) with the
     * specified Marker and including the stack warn of the {@link Throwable} <code>t</code> passed as parameter.
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t A Throwable or null.
     */
    void warn(Marker marker, MessageSupplier msgSupplier, Throwable t);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#WARN WARN} level.
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     */
    void warn(MessageSupplier msgSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#WARN WARN} level) including the
     * stack warn of the {@link Throwable} <code>t</code> passed as parameter.
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t the exception to log, including its stack warn.
     */
    void warn(MessageSupplier msgSupplier, Throwable t);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#WARN
     * WARN} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     */
    void warn(String message, Supplier<?>... paramSuppliers);

}
