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
package org.apache.logging.log4j.v3;

import java.util.function.Supplier;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogBuilder;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;

public interface Logger {
    /**
     * Logs a message object with the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker  the marker data specific to this log statement
     * @param throwable the error to log.
     */
    void debug(Marker marker, Throwable throwable);

    /**
     * Logs a message CharSequence with the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message CharSequence to log.
     */
    void debug(Marker marker, CharSequence message);

    /**
     * Logs a message CharSequence at the {@link Level#DEBUG DEBUG} level including the stack trace of the
     * {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param marker    the marker data specific to this log statement
     * @param message   the message CharSequence to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void debug(Marker marker, CharSequence message, Throwable throwable);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     */
    void debug(Marker marker, String message, Object p0);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     * @param p1      parameter to the message.
     */
    void debug(Marker marker, String message, Object p0, Object p1);

    /**
     * Logs a message with parameters at the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param params  parameters to the message.
     */
    void debug(Marker marker, String message, Object... params);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     */
    void debug(Marker marker, String message, Supplier<?> p0);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     * @param p1      parameter to the message.
     */
    void debug(Marker marker, String message, Supplier<?> p0, Supplier<?> p1);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param marker         the marker data specific to this log statement
     * @param message        the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    void debug(Marker marker, String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message string to be logged
     */
    void debug(Marker marker, Message message);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker    the marker data specific to this log statement
     * @param message   the message string to be logged
     * @param throwable A Throwable or null.
     */
    void debug(Marker marker, Message message, Throwable throwable);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level with
     * the specified Marker.
     *
     * @param marker          the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *                        message factory.
     * @since 2.4
     */
    void debug(Marker marker, Supplier<?> messageSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param marker          the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *                        message factory.
     * @param throwable       A Throwable or null.
     * @since 2.4
     */
    void debug(Marker marker, Supplier<?> messageSupplier, Throwable throwable);

    /**
     * Logs a message CharSequence with the {@link Level#DEBUG DEBUG} level.
     */
    void debug(Throwable throwable);

    /**
     * Logs a message CharSequence with the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message object to log.
     */
    void debug(CharSequence message);

    /**
     * Logs a CharSequence at the {@link Level#DEBUG DEBUG} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param message   the message CharSequence to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void debug(CharSequence message, Throwable throwable);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     */
    void debug(String message, Object p0);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     * @param p1      parameter to the message.
     */
    void debug(String message, Object p0, Object p1);

    /**
     * Logs a message with parameters at the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param params  parameters to the message.
     */
    void debug(String message, Object... params);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param message        the message to log; the format depends on the message factory.
     * @since 2.4
     */
    void debug(String message, Supplier<?> p0);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param message        the message to log; the format depends on the message factory.
     * @since 2.4
     */
    void debug(String message, Supplier<?> p0, Supplier<?> p1);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param message        the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    void debug(String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message string to be logged
     */
    void debug(Message message);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param message   the message string to be logged
     * @param throwable A Throwable or null.
     */
    void debug(Message message, Throwable throwable);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level.
     *
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *                        message factory.
     * @since 2.4
     */
    void debug(Supplier<?> messageSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level) including the
     * stack trace of the {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *                        message factory.
     * @param throwable       the {@code Throwable} to log, including its stack trace.
     * @since 2.4
     */
    void debug(Supplier<?> messageSupplier, Throwable throwable);

    /**
     * Logs a message object with the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker  the marker data specific to this log statement
     * @param throwable the error to log.
     */
    void error(Marker marker, Throwable throwable);

    /**
     * Logs a message CharSequence with the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message CharSequence to log.
     */
    void error(Marker marker, CharSequence message);

    /**
     * Logs a message CharSequence at the {@link Level#DEBUG DEBUG} level including the stack trace of the
     * {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param marker    the marker data specific to this log statement
     * @param message   the message CharSequence to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void error(Marker marker, CharSequence message, Throwable throwable);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     */
    void error(Marker marker, String message, Object p0);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     * @param p1      parameter to the message.
     */
    void error(Marker marker, String message, Object p0, Object p1);

    /**
     * Logs a message with parameters at the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param params  parameters to the message.
     */
    void error(Marker marker, String message, Object... params);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     */
    void error(Marker marker, String message, Supplier<?> p0);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     * @param p1      parameter to the message.
     */
    void error(Marker marker, String message, Supplier<?> p0, Supplier<?> p1);
    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param marker         the marker data specific to this log statement
     * @param message        the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    void error(Marker marker, String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message string to be logged
     */
    void error(Marker marker, Message message);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker    the marker data specific to this log statement
     * @param message   the message string to be logged
     * @param throwable A Throwable or null.
     */
    void error(Marker marker, Message message, Throwable throwable);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level with
     * the specified Marker.
     *
     * @param marker          the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *                        message factory.
     * @since 2.4
     */
    void error(Marker marker, Supplier<?> messageSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param marker          the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *                        message factory.
     * @param throwable       A Throwable or null.
     * @since 2.4
     */
    void error(Marker marker, Supplier<?> messageSupplier, Throwable throwable);

    /**
     * Logs a message CharSequence with the {@link Level#DEBUG DEBUG} level.
     */
    void error(Throwable throwable);

    /**
     * Logs a message CharSequence with the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message object to log.
     */
    void error(CharSequence message);

    /**
     * Logs a CharSequence at the {@link Level#DEBUG DEBUG} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param message   the message CharSequence to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void error(CharSequence message, Throwable throwable);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     */
    void error(String message, Object p0);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     * @param p1      parameter to the message.
     */
    void error(String message, Object p0, Object p1);

    /**
     * Logs a message with parameters at the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param params  parameters to the message.
     */
    void error(String message, Object... params);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param message        the message to log; the format depends on the message factory.
     * @since 2.4
     */
    void error(String message, Supplier<?> p0);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param message        the message to log; the format depends on the message factory.
     * @since 2.4
     */
    void error(String message, Supplier<?> p0, Supplier<?> p1);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param message        the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    void error(String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message string to be logged
     */
    void error(Message message);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param message   the message string to be logged
     * @param throwable A Throwable or null.
     */
    void error(Message message, Throwable throwable);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level.
     *
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *                        message factory.
     * @since 2.4
     */
    void error(Supplier<?> messageSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level) including the
     * stack trace of the {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *                        message factory.
     * @param throwable       the {@code Throwable} to log, including its stack trace.
     * @since 2.4
     */
    void error(Supplier<?> messageSupplier, Throwable throwable);
    /**
     * Logs a message object with the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker  the marker data specific to this log statement
     * @param throwable the error to log.
     */
    void fatal(Marker marker, Throwable throwable);

    /**
     * Logs a message CharSequence with the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message CharSequence to log.
     */
    void fatal(Marker marker, CharSequence message);

    /**
     * Logs a message CharSequence at the {@link Level#DEBUG DEBUG} level including the stack trace of the
     * {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param marker    the marker data specific to this log statement
     * @param message   the message CharSequence to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void fatal(Marker marker, CharSequence message, Throwable throwable);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     */
    void fatal(Marker marker, String message, Object p0);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     * @param p1      parameter to the message.
     */
    void fatal(Marker marker, String message, Object p0, Object p1);

    /**
     * Logs a message with parameters at the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param params  parameters to the message.
     */
    void fatal(Marker marker, String message, Object... params);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     */
    void fatal(Marker marker, String message, Supplier<?> p0);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     * @param p1      parameter to the message.
     */
    void fatal(Marker marker, String message, Supplier<?> p0, Supplier<?> p1);
    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param marker         the marker data specific to this log statement
     * @param message        the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    void fatal(Marker marker, String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message string to be logged
     */
    void fatal(Marker marker, Message message);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker    the marker data specific to this log statement
     * @param message   the message string to be logged
     * @param throwable A Throwable or null.
     */
    void fatal(Marker marker, Message message, Throwable throwable);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level with
     * the specified Marker.
     *
     * @param marker          the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *                        message factory.
     * @since 2.4
     */
    void fatal(Marker marker, Supplier<?> messageSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param marker          the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *                        message factory.
     * @param throwable       A Throwable or null.
     * @since 2.4
     */
    void fatal(Marker marker, Supplier<?> messageSupplier, Throwable throwable);

    /**
     * Logs a message CharSequence with the {@link Level#DEBUG DEBUG} level.
     */
    void fatal(Throwable throwable);

    /**
     * Logs a message CharSequence with the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message object to log.
     */
    void fatal(CharSequence message);

    /**
     * Logs a CharSequence at the {@link Level#DEBUG DEBUG} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param message   the message CharSequence to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void fatal(CharSequence message, Throwable throwable);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     */
    void fatal(String message, Object p0);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     * @param p1      parameter to the message.
     */
    void fatal(String message, Object p0, Object p1);

    /**
     * Logs a message with parameters at the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param params  parameters to the message.
     */
    void fatal(String message, Object... params);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param message        the message to log; the format depends on the message factory.
     * @since 2.4
     */
    void fatal(String message, Supplier<?> p0);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param message        the message to log; the format depends on the message factory.
     * @since 2.4
     */
    void fatal(String message, Supplier<?> p0, Supplier<?> p1);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param message        the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    void fatal(String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message string to be logged
     */
    void fatal(Message message);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param message   the message string to be logged
     * @param throwable A Throwable or null.
     */
    void fatal(Message message, Throwable throwable);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level.
     *
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *                        message factory.
     * @since 2.4
     */
    void fatal(Supplier<?> messageSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level) including the
     * stack trace of the {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *                        message factory.
     * @param throwable       the {@code Throwable} to log, including its stack trace.
     * @since 2.4
     */
    void fatal(Supplier<?> messageSupplier, Throwable throwable);

    /**
     * Logs a message object with the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker  the marker data specific to this log statement
     * @param throwable the error to log.
     */
    void info(Marker marker, Throwable throwable);

    /**
     * Logs a message CharSequence with the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message CharSequence to log.
     */
    void info(Marker marker, CharSequence message);

    /**
     * Logs a message CharSequence at the {@link Level#DEBUG DEBUG} level including the stack trace of the
     * {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param marker    the marker data specific to this log statement
     * @param message   the message CharSequence to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void info(Marker marker, CharSequence message, Throwable throwable);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     */
    void info(Marker marker, String message, Object p0);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     * @param p1      parameter to the message.
     */
    void info(Marker marker, String message, Object p0, Object p1);

    /**
     * Logs a message with parameters at the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param params  parameters to the message.
     */
    void info(Marker marker, String message, Object... params);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     */
    void info(Marker marker, String message, Supplier<?> p0);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     * @param p1      parameter to the message.
     */
    void info(Marker marker, String message, Supplier<?> p0, Supplier<?> p1);
    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param marker         the marker data specific to this log statement
     * @param message        the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    void info(Marker marker, String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message string to be logged
     */
    void info(Marker marker, Message message);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker    the marker data specific to this log statement
     * @param message   the message string to be logged
     * @param throwable A Throwable or null.
     */
    void info(Marker marker, Message message, Throwable throwable);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level with
     * the specified Marker.
     *
     * @param marker          the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *                        message factory.
     * @since 2.4
     */
    void info(Marker marker, Supplier<?> messageSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param marker          the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *                        message factory.
     * @param throwable       A Throwable or null.
     * @since 2.4
     */
    void info(Marker marker, Supplier<?> messageSupplier, Throwable throwable);

    /**
     * Logs a message CharSequence with the {@link Level#DEBUG DEBUG} level.
     */
    void info(Throwable throwable);

    /**
     * Logs a message CharSequence with the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message object to log.
     */
    void info(CharSequence message);

    /**
     * Logs a CharSequence at the {@link Level#DEBUG DEBUG} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param message   the message CharSequence to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void info(CharSequence message, Throwable throwable);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     */
    void info(String message, Object p0);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     * @param p1      parameter to the message.
     */
    void info(String message, Object p0, Object p1);

    /**
     * Logs a message with parameters at the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param params  parameters to the message.
     */
    void info(String message, Object... params);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param message        the message to log; the format depends on the message factory.
     * @since 2.4
     */
    void info(String message, Supplier<?> p0);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param message        the message to log; the format depends on the message factory.
     * @since 2.4
     */
    void info(String message, Supplier<?> p0, Supplier<?> p1);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param message        the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    void info(String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message string to be logged
     */
    void info(Message message);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param message   the message string to be logged
     * @param throwable A Throwable or null.
     */
    void info(Message message, Throwable throwable);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level.
     *
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *                        message factory.
     * @since 2.4
     */
    void info(Supplier<?> messageSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level) including the
     * stack trace of the {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *                        message factory.
     * @param throwable       the {@code Throwable} to log, including its stack trace.
     * @since 2.4
     */
    void info(Supplier<?> messageSupplier, Throwable throwable);

    /**
     * Logs a message object with the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker  the marker data specific to this log statement
     * @param throwable the error to log.
     */
    void trace(Marker marker, Throwable throwable);

    /**
     * Logs a message CharSequence with the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message CharSequence to log.
     */
    void trace(Marker marker, CharSequence message);

    /**
     * Logs a message CharSequence at the {@link Level#DEBUG DEBUG} level including the stack trace of the
     * {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param marker    the marker data specific to this log statement
     * @param message   the message CharSequence to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void trace(Marker marker, CharSequence message, Throwable throwable);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     */
    void trace(Marker marker, String message, Object p0);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     * @param p1      parameter to the message.
     */
    void trace(Marker marker, String message, Object p0, Object p1);

    /**
     * Logs a message with parameters at the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param params  parameters to the message.
     */
    void trace(Marker marker, String message, Object... params);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     */
    void trace(Marker marker, String message, Supplier<?> p0);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     * @param p1      parameter to the message.
     */
    void trace(Marker marker, String message, Supplier<?> p0, Supplier<?> p1);
    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param marker         the marker data specific to this log statement
     * @param message        the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    void trace(Marker marker, String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message string to be logged
     */
    void trace(Marker marker, Message message);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker    the marker data specific to this log statement
     * @param message   the message string to be logged
     * @param throwable A Throwable or null.
     */
    void trace(Marker marker, Message message, Throwable throwable);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level with
     * the specified Marker.
     *
     * @param marker          the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *                        message factory.
     * @since 2.4
     */
    void trace(Marker marker, Supplier<?> messageSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param marker          the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *                        message factory.
     * @param throwable       A Throwable or null.
     * @since 2.4
     */
    void trace(Marker marker, Supplier<?> messageSupplier, Throwable throwable);

    /**
     * Logs a message CharSequence with the {@link Level#DEBUG DEBUG} level.
     */
    void trace(Throwable throwable);

    /**
     * Logs a message CharSequence with the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message object to log.
     */
    void trace(CharSequence message);

    /**
     * Logs a CharSequence at the {@link Level#DEBUG DEBUG} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param message   the message CharSequence to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void trace(CharSequence message, Throwable throwable);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     */
    void trace(String message, Object p0);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     * @param p1      parameter to the message.
     */
    void trace(String message, Object p0, Object p1);

    /**
     * Logs a message with parameters at the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param params  parameters to the message.
     */
    void trace(String message, Object... params);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param message        the message to log; the format depends on the message factory.
     * @since 2.4
     */
    void trace(String message, Supplier<?> p0);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param message        the message to log; the format depends on the message factory.
     * @since 2.4
     */
    void trace(String message, Supplier<?> p0, Supplier<?> p1);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param message        the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    void trace(String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message string to be logged
     */
    void trace(Message message);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param message   the message string to be logged
     * @param throwable A Throwable or null.
     */
    void trace(Message message, Throwable throwable);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level.
     *
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *                        message factory.
     * @since 2.4
     */
    void trace(Supplier<?> messageSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level) including the
     * stack trace of the {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *                        message factory.
     * @param throwable       the {@code Throwable} to log, including its stack trace.
     * @since 2.4
     */
    void trace(Supplier<?> messageSupplier, Throwable throwable);

    /**
     * Logs a message object with the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker  the marker data specific to this log statement
     * @param throwable the error to log.
     */
    void warn(Marker marker, Throwable throwable);

    /**
     * Logs a message CharSequence with the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message CharSequence to log.
     */
    void warn(Marker marker, CharSequence message);

    /**
     * Logs a message CharSequence at the {@link Level#DEBUG DEBUG} level including the stack trace of the
     * {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param marker    the marker data specific to this log statement
     * @param message   the message CharSequence to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void warn(Marker marker, CharSequence message, Throwable throwable);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     */
    void warn(Marker marker, String message, Object p0);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     * @param p1      parameter to the message.
     */
    void warn(Marker marker, String message, Object p0, Object p1);

    /**
     * Logs a message with parameters at the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param params  parameters to the message.
     */
    void warn(Marker marker, String message, Object... params);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     */
    void warn(Marker marker, String message, Supplier<?> p0);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     * @param p1      parameter to the message.
     */
    void warn(Marker marker, String message, Supplier<?> p0, Supplier<?> p1);
    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param marker         the marker data specific to this log statement
     * @param message        the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    void warn(Marker marker, String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message string to be logged
     */
    void warn(Marker marker, Message message);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker    the marker data specific to this log statement
     * @param message   the message string to be logged
     * @param throwable A Throwable or null.
     */
    void warn(Marker marker, Message message, Throwable throwable);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level with
     * the specified Marker.
     *
     * @param marker          the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *                        message factory.
     * @since 2.4
     */
    void warn(Marker marker, Supplier<?> messageSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param marker          the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *                        message factory.
     * @param throwable       A Throwable or null.
     * @since 2.4
     */
    void warn(Marker marker, Supplier<?> messageSupplier, Throwable throwable);

    /**
     * Logs a message CharSequence with the {@link Level#DEBUG DEBUG} level.
     */
    void warn(Throwable throwable);

    /**
     * Logs a message CharSequence with the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message object to log.
     */
    void warn(CharSequence message);

    /**
     * Logs a CharSequence at the {@link Level#DEBUG DEBUG} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param message   the message CharSequence to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void warn(CharSequence message, Throwable throwable);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     */
    void warn(String message, Object p0);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     * @param p1      parameter to the message.
     */
    void warn(String message, Object p0, Object p1);

    /**
     * Logs a message with parameters at the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param params  parameters to the message.
     */
    void warn(String message, Object... params);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param message        the message to log; the format depends on the message factory.
     * @since 2.4
     */
    void warn(String message, Supplier<?> p0);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param message        the message to log; the format depends on the message factory.
     * @since 2.4
     */
    void warn(String message, Supplier<?> p0, Supplier<?> p1);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param message        the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    void warn(String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message string to be logged
     */
    void warn(Message message);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param message   the message string to be logged
     * @param throwable A Throwable or null.
     */
    void warn(Message message, Throwable throwable);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level.
     *
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *                        message factory.
     * @since 2.4
     */
    void warn(Supplier<?> messageSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level) including the
     * stack trace of the {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *                        message factory.
     * @param throwable       the {@code Throwable} to log, including its stack trace.
     * @since 2.4
     */
    void warn(Supplier<?> messageSupplier, Throwable throwable);

    /**
     * Logs a message object with the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker  the marker data specific to this log statement
     * @param throwable the error to log.
     */
    void log(Level level, Marker marker, Throwable throwable);

    /**
     * Logs a message CharSequence with the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message CharSequence to log.
     */
    void log(Level level, Marker marker, CharSequence message);

    /**
     * Logs a message CharSequence at the {@link Level#DEBUG DEBUG} level including the stack trace of the
     * {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param marker    the marker data specific to this log statement
     * @param message   the message CharSequence to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void log(Level level, Marker marker, CharSequence message, Throwable throwable);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     */
    void log(Level level, Marker marker, String message, Object p0);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     * @param p1      parameter to the message.
     */
    void log(Level level, Marker marker, String message, Object p0, Object p1);

    /**
     * Logs a message with parameters at the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param params  parameters to the message.
     */
    void log(Level level, Marker marker, String message, Object... params);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     */
    void log(Level level, Marker marker, String message, Supplier<?> p0);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     * @param p1      parameter to the message.
     */
    void log(Level level, Marker marker, String message, Supplier<?> p0, Supplier<?> p1);
    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param marker         the marker data specific to this log statement
     * @param message        the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    void log(Level level, Marker marker, String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker  the marker data specific to this log statement
     * @param message the message string to be logged
     */
    void log(Level level, Marker marker, Message message);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker    the marker data specific to this log statement
     * @param message   the message string to be logged
     * @param throwable A Throwable or null.
     */
    void log(Level level, Marker marker, Message message, Throwable throwable);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level with
     * the specified Marker.
     *
     * @param marker          the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *                        message factory.
     * @since 2.4
     */
    void log(Level level, Marker marker, Supplier<?> messageSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level) with the
     * specified Marker and including the stack trace of the {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param marker          the marker data specific to this log statement
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *                        message factory.
     * @param throwable       A Throwable or null.
     * @since 2.4
     */
    void log(Level level, Marker marker, Supplier<?> messageSupplier, Throwable throwable);

    /**
     * Logs a message CharSequence with the {@link Level#DEBUG DEBUG} level.
     */
    void log(Level level, Throwable throwable);

    /**
     * Logs a message CharSequence with the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message object to log.
     */
    void log(Level level, CharSequence message);

    /**
     * Logs a CharSequence at the {@link Level#DEBUG DEBUG} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param message   the message CharSequence to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    void log(Level level, CharSequence message, Throwable throwable);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     */
    void log(Level level, String message, Object p0);

    /**
     * Logs a message with parameters at debug level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0      parameter to the message.
     * @param p1      parameter to the message.
     */
    void log(Level level, String message, Object p0, Object p1);

    /**
     * Logs a message with parameters at the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param params  parameters to the message.
     */
    void log(Level level, String message, Object... params);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param message        the message to log; the format depends on the message factory.
     * @since 2.4
     */
    void log(Level level, String message, Supplier<?> p0);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param message        the message to log; the format depends on the message factory.
     * @since 2.4
     */
    void log(Level level, String message, Supplier<?> p0, Supplier<?> p1);

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link Level#DEBUG
     * DEBUG} level.
     *
     * @param message        the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since 2.4
     */
    void log(Level level, String message, Supplier<?>... paramSuppliers);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message string to be logged
     */
    void log(Level level, Message message);

    /**
     * Logs a message with the specific Marker at the {@link Level#DEBUG DEBUG} level.
     *
     * @param message   the message string to be logged
     * @param throwable A Throwable or null.
     */
    void log(Level level, Message message, Throwable throwable);

    /**
     * Logs a message which is only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level.
     *
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *                        message factory.
     * @since 2.4
     */
    void log(Level level, Supplier<?> messageSupplier);

    /**
     * Logs a message (only to be constructed if the logging level is the {@link Level#DEBUG DEBUG} level) including the
     * stack trace of the {@link Throwable} <code>throwable</code> passed as parameter.
     *
     * @param messageSupplier A function, which when called, produces the desired log message; the format depends on the
     *                        message factory.
     * @param throwable       the {@code Throwable} to log, including its stack trace.
     * @since 2.4
     */
    void log(Level level, Supplier<?> messageSupplier, Throwable throwable);

    /**
     * Gets the Level associated with the Logger.
     *
     * @return the Level associate with the Logger.
     */
    Level getLevel();

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
     * Logs a Message.
     *
     * @param level     The logging Level to check.
     * @param marker    A Marker or null.
     * @param message   The message format.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     * @param location  The location of the caller.
     * @since 2.13.0
     */
    void log(Level level, Marker marker, Message message, Throwable throwable, StackTraceElement location);

    /**
     * Construct a trace log event.
     * @return a LogBuilder.
     * @since 2.13.0
     */
    LogBuilder atTrace();

    /**
     * Construct a trace log event.
     * @return a LogBuilder.
     * @since 2.13.0
     */
    LogBuilder atDebug();

    /**
     * Construct a trace log event.
     * @return a LogBuilder.
     * @since 2.13.0
     */
    LogBuilder atInfo();

    /**
     * Construct a trace log event.
     * @return a LogBuilder.
     * @since 2.13.0
     */
    LogBuilder atWarn();

    /**
     * Construct a trace log event.
     * @return a LogBuilder.
     * @since 2.13.0
     */
    LogBuilder atError();

    /**
     * Construct a trace log event.
     * @return a LogBuilder.
     * @since 2.13.0
     */
    LogBuilder atFatal();

    /**
     * Construct a log event that will always be logged.
     * @return a LogBuilder.
     * @since 2.13.0
     */
    LogBuilder always();

    /**
     * Construct a log event.
     * @param level Any level.
     * @return a LogBuilder.
     * @since 2.13.0
     */
    LogBuilder atLevel(Level level);
}
