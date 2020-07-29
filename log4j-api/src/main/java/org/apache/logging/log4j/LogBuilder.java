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
import org.apache.logging.log4j.util.Supplier;


/**
 * Interface for constructing log events before logging them. Instances of LogBuilders should only be created
 * by calling one of the Logger methods that return a LogBuilder.
 */
public interface LogBuilder {

    public static final LogBuilder NOOP = new LogBuilder() {};

    default LogBuilder withMarker(Marker marker) {
        return this;
    }

    default LogBuilder withThrowable(Throwable throwable) {
        return this;
    }

    default LogBuilder withLocation() {
        return this;
    }

    default LogBuilder withLocation(StackTraceElement location) {
        return this;
    }

    default void log(CharSequence message) {
    }

    default void log(String message) {
    }

    /**
     * Logs a message with parameters.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param params parameters to the message.
     *
     * @see org.apache.logging.log4j.util.Unbox
     */
    default void log(String message, Object... params) {
    }

    default void log(String message, Supplier<?>... params) {
    }

    default void log(Message message) {
    }

    default void log(Supplier<Message> messageSupplier) {
    }

    default void log(Object message) {

    }

    /**
     * Logs a message with parameters.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     *
     * @see org.apache.logging.log4j.util.Unbox
     */
    default void log(String message, Object p0) {
    }

    /**
     * Logs a message with parameters.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     *
     * @see org.apache.logging.log4j.util.Unbox
     */
    default void log(String message, Object p0, Object p1) {
    }

    /**
     * Logs a message with parameters.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     *
     * @see org.apache.logging.log4j.util.Unbox
     */
    default void log(String message, Object p0, Object p1, Object p2) {
    }

    /**
     * Logs a message with parameters.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     *
     * @see org.apache.logging.log4j.util.Unbox
     */
    default void log(String message, Object p0, Object p1, Object p2, Object p3) {
    }

    /**
     * Logs a message with parameters.
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
    default void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
    }

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
     *
     * @see org.apache.logging.log4j.util.Unbox
     */
    default void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
    }

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
    default void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
    }

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
     * @param p7 parameter to the message.
     *
     * @see org.apache.logging.log4j.util.Unbox
     */
    default void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
            Object p7) {
    }

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
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     *
     * @see org.apache.logging.log4j.util.Unbox
     */
    default void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
            Object p7, Object p8) {
    }

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
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @param p9 parameter to the message.
     *
     * @see org.apache.logging.log4j.util.Unbox
     */
    default void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
            Object p7, Object p8, Object p9) {
    }
}
