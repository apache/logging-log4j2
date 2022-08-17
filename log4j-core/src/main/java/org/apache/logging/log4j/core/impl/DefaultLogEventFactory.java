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
package org.apache.logging.log4j.core.impl;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.core.time.NanoClock;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.util.StringMap;

import java.util.List;

/**
 * Always creates new LogEvent instances.
 */
public class DefaultLogEventFactory implements LogEventFactory {

    public static DefaultLogEventFactory newInstance() {
        final var injector = DI.createInjector();
        injector.init();
        return injector.getInstance(DefaultLogEventFactory.class);
    }

    private final ContextDataInjector injector;
    private final Clock clock;
    private final NanoClock nanoClock;

    @Inject
    public DefaultLogEventFactory(
            final ContextDataInjector injector, final Clock clock, final NanoClock nanoClock) {
        this.injector = injector;
        this.clock = clock;
        this.nanoClock = nanoClock;
    }

    private StringMap createContextData(final List<Property> properties) {
        return injector.injectContextData(properties, ContextDataFactory.createContextData());
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
    public LogEvent createEvent(final String loggerName, final Marker marker,
                                final String fqcn, final Level level, final Message data,
                                final List<Property> properties, final Throwable t) {
        return Log4jLogEvent.newBuilder()
                .setNanoTime(nanoClock.nanoTime())
                .setClock(clock)
                .setLoggerName(loggerName)
                .setMarker(marker)
                .setLoggerFqcn(fqcn)
                .setLevel(level)
                .setMessage(data)
                .setContextData(createContextData(properties))
                .setThrown(t)
                .build();
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
    public LogEvent createEvent(final String loggerName, final Marker marker, final String fqcn,
            final StackTraceElement location, final Level level, final Message data,
            final List<Property> properties, final Throwable t) {
        return Log4jLogEvent.newBuilder()
                .setNanoTime(nanoClock.nanoTime())
                .setClock(clock)
                .setLoggerName(loggerName)
                .setMarker(marker)
                .setLoggerFqcn(fqcn)
                .setSource(location)
                .setLevel(level)
                .setMessage(data)
                .setContextData(createContextData(properties))
                .setThrown(t)
                .build();
    }
}
