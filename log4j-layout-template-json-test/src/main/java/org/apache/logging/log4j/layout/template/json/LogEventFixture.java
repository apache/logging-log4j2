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
package org.apache.logging.log4j.layout.template.json;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.MutableThreadContextStack;
import org.apache.logging.log4j.spi.ThreadContextStack;
import org.apache.logging.log4j.util.StringMap;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

final class LogEventFixture {

    private LogEventFixture() {}

    private static final int TIME_OVERLAPPING_CONSECUTIVE_EVENT_COUNT = 10;

    static List<LogEvent> createLiteLogEvents(final int logEventCount) {
        final List<LogEvent> logEvents = new ArrayList<>(logEventCount);
        final long startTimeMillis = System.currentTimeMillis();
        for (int logEventIndex = 0; logEventIndex < logEventCount; logEventIndex++) {
            final String logEventId = String.valueOf(logEventIndex);
            final long logEventTimeMillis = createLogEventTimeMillis(startTimeMillis, logEventIndex);
            final LogEvent logEvent = LogEventFixture.createLiteLogEvent(logEventId, logEventTimeMillis);
            logEvents.add(logEvent);
        }
        return logEvents;
    }

    private static LogEvent createLiteLogEvent(final String id, final long timeMillis) {
        final SimpleMessage message = new SimpleMessage("lite LogEvent message " + id);
        final Level level = Level.DEBUG;
        final String loggerFqcn = "f.q.c.n" + id;
        final String loggerName = "a.B" + id;
        final long nanoTime = timeMillis * 2;
        return Log4jLogEvent
                .newBuilder()
                .setLoggerName(loggerName)
                .setLoggerFqcn(loggerFqcn)
                .setLevel(level)
                .setMessage(message)
                .setTimeMillis(timeMillis)
                .setNanoTime(nanoTime)
                .build();
    }

    static List<LogEvent> createFullLogEvents(final int logEventCount) {
        final List<LogEvent> logEvents = new ArrayList<>(logEventCount);
        final long startTimeMillis = System.currentTimeMillis();
        for (int logEventIndex = 0; logEventIndex < logEventCount; logEventIndex++) {
            final String logEventId = String.valueOf(logEventIndex);
            final long logEventTimeMillis = createLogEventTimeMillis(startTimeMillis, logEventIndex);
            final LogEvent logEvent = LogEventFixture.createFullLogEvent(logEventId, logEventTimeMillis);
            logEvents.add(logEvent);
        }
        return logEvents;
    }

    private static long createLogEventTimeMillis(
            final long startTimeMillis,
            final int logEventIndex) {
        // Create event time repeating every certain number of consecutive
        // events. This is better aligned with the real-world use case and
        // gives surface to timestamp formatter caches to perform their
        // magic, which is implemented for almost all layouts.
        return startTimeMillis + logEventIndex / TIME_OVERLAPPING_CONSECUTIVE_EVENT_COUNT;
    }

    private static LogEvent createFullLogEvent(
            final String id,
            final long timeMillis) {

        // Create exception.
        final Exception sourceHelper = new Exception();
        sourceHelper.fillInStackTrace();
        final Exception cause = new NullPointerException("testNPEx-" + id);
        sourceHelper.fillInStackTrace();
        final StackTraceElement source = sourceHelper.getStackTrace()[0];
        final IOException ioException = new IOException("testIOEx-" + id, cause);
        ioException.addSuppressed(new IndexOutOfBoundsException("I am suppressed exception 1" + id));
        ioException.addSuppressed(new IndexOutOfBoundsException("I am suppressed exception 2" + id));

        // Create rest of the event attributes.
        final SimpleMessage message = new SimpleMessage("full LogEvent message " + id);
        final StringMap contextData = createContextData(id);
        final ThreadContextStack contextStack = createContextStack(id);
        final int threadId = id.hashCode();
        final String threadName = "MyThreadName" + id;
        final int threadPriority = threadId % 10;
        final Level level = Level.DEBUG;
        final String loggerFqcn = "f.q.c.n" + id;
        final String loggerName = "a.B" + id;
        final long nanoTime = timeMillis * 2;

        // Create the event.
        return Log4jLogEvent
                .newBuilder()
                .setLoggerName(loggerName)
                .setLoggerFqcn(loggerFqcn)
                .setLevel(level)
                .setMessage(message)
                .setThrown(ioException)
                .setContextData(contextData)
                .setContextStack(contextStack)
                .setThreadId(threadId)
                .setThreadName(threadName)
                .setThreadPriority(threadPriority)
                .setSource(source)
                .setTimeMillis(timeMillis)
                .setNanoTime(nanoTime)
                .build();

    }

    private static StringMap createContextData(final String id) {
        final StringMap contextData = ContextDataFactory.createContextData();
        contextData.putValue("MDC.String." + id, "String");
        contextData.putValue("MDC.BigDecimal." + id, BigDecimal.valueOf(Math.PI));
        contextData.putValue("MDC.Integer." + id, 10);
        contextData.putValue("MDC.Long." + id, Long.MAX_VALUE);
        return contextData;
    }

    private static ThreadContextStack createContextStack(final String id) {
        final ThreadContextStack contextStack = new MutableThreadContextStack();
        contextStack.clear();
        contextStack.push("stack_msg1" + id);
        contextStack.add("stack_msg2" + id);
        return contextStack;
    }

}
