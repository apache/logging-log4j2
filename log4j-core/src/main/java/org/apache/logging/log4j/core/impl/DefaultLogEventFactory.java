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
package org.apache.logging.log4j.core.impl;

import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.message.Message;

/**
 * Always creates new LogEvent instances.
 */
public class DefaultLogEventFactory implements LogEventFactory, LocationAwareLogEventFactory {

    private static final DefaultLogEventFactory instance = new DefaultLogEventFactory();

    public static DefaultLogEventFactory getInstance() {
        return instance;
    }

    /**
     * Creates a log event.
     *
     * @param loggerName The name of the Logger.
     * @param marker An optional Marker.
     * @param fqcn The fully qualified class name of the caller.
     * @param level The event Level.
     * @param data The Message.
     * @param properties Properties to be added to the log event.
     * @param t An optional Throwable.
     * @return The LogEvent.
     */
    @Override
    public LogEvent createEvent(
            final String loggerName,
            final Marker marker,
            final String fqcn,
            final Level level,
            final Message data,
            final List<Property> properties,
            final Throwable t) {
        return new Log4jLogEvent(loggerName, marker, fqcn, level, data, properties, t);
    }

    /**
     * Creates a log event.
     *
     * @param loggerName The name of the Logger.
     * @param marker An optional Marker.
     * @param fqcn The fully qualified class name of the caller.
     * @param location The location of the caller
     * @param level The event Level.
     * @param data The Message.
     * @param properties Properties to be added to the log event.
     * @param t An optional Throwable.
     * @return The LogEvent.
     */
    @Override
    public LogEvent createEvent(
            final String loggerName,
            final Marker marker,
            final String fqcn,
            final StackTraceElement location,
            final Level level,
            final Message data,
            final List<Property> properties,
            final Throwable t) {
        return new Log4jLogEvent(loggerName, marker, fqcn, location, level, data, properties, t);
    }
}
