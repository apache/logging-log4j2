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
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.util.Clock;
import org.apache.logging.log4j.core.util.ClockFactory;
import org.apache.logging.log4j.message.Message;

import java.util.List;

/**
 * Garbage-free LogEventFactory that reuses a single mutable log event.
 */
public class ReusableLogEventFactory implements LogEventFactory {

    private static ThreadLocal<MutableLogEvent> mutableLogEventThreadLocal = new ThreadLocal<>();
    private static final Clock CLOCK = ClockFactory.getClock();
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
        MutableLogEvent result = mutableLogEventThreadLocal.get();
        if (result == null) {
            result = new MutableLogEvent();
            result.setThreadId(Thread.currentThread().getId());
            result.setThreadName(Thread.currentThread().getName());
            result.setThreadPriority(Thread.currentThread().getPriority());
            mutableLogEventThreadLocal.set(result);
        }

        result.setLoggerName(loggerName);
        result.setMarker(marker);
        result.setLoggerFqcn(fqcn);
        result.setLevel(level == null ? Level.OFF : level);
        result.setMessage(data);
        result.setThrown(t);
        result.setContextMap(Log4jLogEvent.createMap(properties));
        result.setContextStack(ThreadContext.getDepth() == 0 ? null : ThreadContext.cloneStack());// mutable copy
        result.setTimeMillis(CLOCK.currentTimeMillis());
        result.setNanoTime(Log4jLogEvent.getNanoClock().nanoTime());

        // TODO
//        result.setEndOfBatch();
//        result.setIncludeLocation();
//        result.setSource();
        //return new Log4jLogEvent(loggerName, marker, fqcn, level, data, properties, t);
        return result;
    }
}
