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
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;

/**
 * Wrapper class that exposes the protected AbstractLogger methods to support wrapped loggers.
 */
public class AbstractLoggerWrapper extends AbstractLogger {

    /**
     * The wrapped Logger.
     */
    protected final AbstractLogger logger;

    /**
     * Constructor that wraps and existing Logger.
     * @param logger The Logger to wrap.
     * @param name The name of the Logger.
     * @param messageFactory TODO
     */
    public AbstractLoggerWrapper(final AbstractLogger logger, final String name, final MessageFactory messageFactory) {
        super(name, messageFactory);
        this.logger = logger;
    }

    /**
     * Log an event.
     * @param marker The Marker
     * @param fqcn   The fully qualified class name of the <b>caller</b>
     * @param level  The logging level
     * @param data   The Message.
     * @param t      A Throwable or null.
     */
    @Override
    public void log(final Marker marker, final String fqcn, final Level level, final Message data, final Throwable t) {
        logger.log(marker, fqcn, level, data, t);
    }

    /**
     * Detect if the event would be logged.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param data The message.
     * @return true if the event would be logged for the Level, Marker and data, false otherwise.
     */
    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String data) {
        return logger.isEnabled(level, marker, data);
    }

    /**
     * Detect if the event would be logged.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param data The message.
     * @param t A Throwable.
     * @return true if the event would be logged for the Level, Marker, data and Throwable, false otherwise.
     */
    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String data, final Throwable t) {
        return logger.isEnabled(level, marker, data, t);
    }

    /**
     * Detect if the event would be logged.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param data The message.
     * @param p1 The parameters.
     * @return true if the event would be logged for the Level, Marker, data and parameter.
     */
    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String data, final Object... p1) {
        return logger.isEnabled(level, marker, data, p1);
    }

    /**
     * Detect if the event would be logged.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param data The message.
     * @param t A Throwable.
     * @return true if the event would be logged for the Level, Marker, Object and Throwable, false otherwise.
     */
    @Override
    public boolean isEnabled(final Level level, final Marker marker, final Object data, final Throwable t) {
        return logger.isEnabled(level, marker, data, t);
    }

    /**
     * Detect if the event would be logged.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param data The Message.
     * @param t A Throwable.
     * @return true if the event would be logged for the Level, Marker, Message and Throwable, false otherwise.
     */
    @Override
    public boolean isEnabled(final Level level, final Marker marker, final Message data, final Throwable t) {
        return logger.isEnabled(level, marker, data, t);
    }
}
