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

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.Supplier;

/**
 * Interface for constructing log events before logging them. Instances of LogBuilders should only be created
 * by calling one of the Logger methods that return a LogBuilder.
 */
public interface LogBuilder {
    /** NOOP Logbuilder */
    LogBuilder NOOP = new LogBuilder() {};

    /**
     * Includes a Marker in the log event. Interface default method does nothing.
     * @param marker The Marker to log.
     * @return The LogBuilder.
     */
    default LogBuilder withMarker(final Marker marker) {
        return this;
    }

    /**
     * Includes a Throwable in the log event. Interface default method does nothing.
     * @param throwable The Throwable to log.
     * @return the LogBuilder.
     */
    default LogBuilder withThrowable(final Throwable throwable) {
        return this;
    }

    /**
     * An implementation will calculate the caller's stack frame and include it in the log event.
     * Interface default method does nothing.
     * @return The LogBuilder.
     */
    default LogBuilder withLocation() {
        return this;
    }

    /**
     * Adds the specified stack trace element to the log event. Interface default method does nothing.
     * @param location The stack trace element to include in the log event.
     * @return The LogBuilder.
     */
    default LogBuilder withLocation(final StackTraceElement location) {
        return this;
    }

    /**
     * Causes all the data collected to be logged along with the message. Interface default method does nothing.
     * @param message The message to log.
     */
    default void log(final CharSequence message) {}

    /**
     * Causes all the data collected to be logged along with the message. Interface default method does nothing.
     * @param message The message to log.
     */
    default void log(final String message) {}

    /**
     * Logs a message with parameters. Interface default method does nothing.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param params parameters to the message.
     *
     * @see org.apache.logging.log4j.util.Unbox
     */
    default void log(final String message, final Object... params) {}

    /**
     * Causes all the data collected to be logged along with the message and parameters.
     * Interface default method does nothing.
     * @param message The message.
     * @param params Parameters to the message.
     */
    default void log(final String message, final Supplier<?>... params) {}

    /**
     * Causes all the data collected to be logged along with the message. Interface default method does nothing.
     * @param message The message to log.
     */
    default void log(final Message message) {}

    /**
     * Causes all the data collected to be logged along with the message. Interface default method does nothing.
     * @param messageSupplier The supplier of the message to log.
     */
    default void log(final Supplier<Message> messageSupplier) {}

    /**
     * Causes all the data collected to be logged along with the message.
     *
     * @param messageSupplier The supplier of the message to log.
     * @return the message logger or {@code null} if no logging occurred.
     * @since 2.20
     */
    default Message logAndGet(final Supplier<Message> messageSupplier) {
        return null;
    }

    /**
     * Causes all the data collected to be logged along with the message. Interface default method does nothing.
     * @param message The message to log.
     */
    default void log(final Object message) {}

    /**
     * Logs a message with parameters. Interface default method does nothing.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     *
     * @see org.apache.logging.log4j.util.Unbox
     */
    default void log(final String message, final Object p0) {}

    /**
     * Logs a message with parameters. Interface default method does nothing.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     *
     * @see org.apache.logging.log4j.util.Unbox
     */
    default void log(final String message, final Object p0, final Object p1) {}

    /**
     * Logs a message with parameters. Interface default method does nothing.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     *
     * @see org.apache.logging.log4j.util.Unbox
     */
    default void log(final String message, final Object p0, final Object p1, final Object p2) {}

    /**
     * Logs a message with parameters. Interface default method does nothing.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     *
     * @see org.apache.logging.log4j.util.Unbox
     */
    default void log(final String message, final Object p0, final Object p1, final Object p2, final Object p3) {}

    /**
     * Logs a message with parameters. Interface default method does nothing.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     *
     * @see org.apache.logging.log4j.util.Unbox
     */
    default void log(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4) {}

    /**
     * Logs a message with parameters. Interface default method does nothing.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     *
     * @see org.apache.logging.log4j.util.Unbox
     */
    default void log(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {}

    /**
     * Logs a message with parameters.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     *
     * @see org.apache.logging.log4j.util.Unbox
     */
    default void log(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {}

    /**
     * Logs a message with parameters. Interface default method does nothing.
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
     *
     * @see org.apache.logging.log4j.util.Unbox
     */
    default void log(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {}

    /**
     * Logs a message with parameters. Interface default method does nothing.
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
     *
     * @see org.apache.logging.log4j.util.Unbox
     */
    default void log(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {}

    /**
     * Logs a message with parameters. Interface default method does nothing.
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
     *
     * @see org.apache.logging.log4j.util.Unbox
     */
    default void log(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9) {}

    /**
     * Causes all the data collected to be logged. Default implementatoin does nothing.
     */
    default void log() {}
}
