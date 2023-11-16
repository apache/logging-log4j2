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
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.util.StackLocatorUtil;

/**
 * Wrapper class that exposes the protected AbstractLogger methods to support wrapped loggers.
 */
public class ExtendedLoggerWrapper extends AbstractLogger {

    private static final long serialVersionUID = 1L;

    /**
     * The wrapped Logger.
     */
    protected final ExtendedLogger logger;

    /**
     * Constructor that wraps and existing Logger.
     *
     * @param logger The Logger to wrap.
     * @param name The name of the Logger.
     * @param messageFactory TODO
     */
    public ExtendedLoggerWrapper(final ExtendedLogger logger, final String name, final MessageFactory messageFactory) {
        super(name, messageFactory);
        this.logger = logger;
    }

    @Override
    public Level getLevel() {
        return logger.getLevel();
    }

    /**
     * Detect if the event would be logged.
     *
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The Message.
     * @param t A Throwable.
     * @return true if the event would be logged for the Level, Marker, Message and Throwable, false otherwise.
     */
    @Override
    public boolean isEnabled(final Level level, final Marker marker, final Message message, final Throwable t) {
        return logger.isEnabled(level, marker, message, t);
    }

    /**
     * Detect if the event would be logged.
     *
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message CharSequence.
     * @param t A Throwable.
     * @return true if the event would be logged for the Level, Marker, Object and Throwable, false otherwise.
     */
    @Override
    public boolean isEnabled(final Level level, final Marker marker, final CharSequence message, final Throwable t) {
        return logger.isEnabled(level, marker, message, t);
    }

    /**
     * Detect if the event would be logged.
     *
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message.
     * @param t A Throwable.
     * @return true if the event would be logged for the Level, Marker, Object and Throwable, false otherwise.
     */
    @Override
    public boolean isEnabled(final Level level, final Marker marker, final Object message, final Throwable t) {
        return logger.isEnabled(level, marker, message, t);
    }

    /**
     * Detect if the event would be logged.
     *
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message.
     * @return true if the event would be logged for the Level, Marker, message and parameter.
     */
    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message) {
        return logger.isEnabled(level, marker, message);
    }

    /**
     * Detect if the event would be logged.
     *
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message.
     * @param params The parameters.
     * @return true if the event would be logged for the Level, Marker, message and parameter.
     */
    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object... params) {
        return logger.isEnabled(level, marker, message, params);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0) {
        return logger.isEnabled(level, marker, message, p0);
    }

    @Override
    public boolean isEnabled(
            final Level level, final Marker marker, final String message, final Object p0, final Object p1) {
        return logger.isEnabled(level, marker, message, p0, p1);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2) {
        return logger.isEnabled(level, marker, message, p0, p1, p2);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3) {
        return logger.isEnabled(level, marker, message, p0, p1, p2, p3);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4) {
        return logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        return logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        return logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        return logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        return logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
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
            final Object p9) {
        return logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    /**
     * Detect if the event would be logged.
     *
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message.
     * @param t A Throwable.
     * @return true if the event would be logged for the Level, Marker, message and Throwable, false otherwise.
     */
    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Throwable t) {
        return logger.isEnabled(level, marker, message, t);
    }

    /**
     * Always log an event. This tends to be already guarded by an enabled check, so this method should not check for
     * the logger level again
     *
     * @param fqcn The fully qualified class name of the <b>caller</b>
     * @param level The logging level
     * @param marker The Marker
     * @param message The Message.
     * @param t A Throwable or null.
     */
    @Override
    public void logMessage(
            final String fqcn, final Level level, final Marker marker, final Message message, final Throwable t) {
        if (logger instanceof LocationAwareLogger && requiresLocation()) {
            ((LocationAwareLogger) logger)
                    .logMessage(level, marker, fqcn, StackLocatorUtil.calcLocation(fqcn), message, t);
        } else {
            logger.logMessage(fqcn, level, marker, message, t);
        }
    }
}
