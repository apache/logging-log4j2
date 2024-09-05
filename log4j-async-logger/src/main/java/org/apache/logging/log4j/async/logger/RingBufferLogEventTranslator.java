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
package org.apache.logging.log4j.async.logger;

import com.lmax.disruptor.EventTranslator;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.core.time.NanoClock;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.StackLocatorUtil;
import org.apache.logging.log4j.util.StringMap;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * This class is responsible for writing elements that make up a log event into
 * the ringbuffer {@code RingBufferLogEvent}. After this translator populated
 * the ringbuffer event, the disruptor will update the sequence number so that
 * the event can be consumed by another thread.
 */
@NullMarked
public class RingBufferLogEventTranslator implements EventTranslator<RingBufferLogEvent> {

    private ContextDataInjector contextDataInjector;
    private AsyncLogger asyncLogger;
    String loggerName;
    protected @Nullable Marker marker;
    protected Level level;
    protected @Nullable Message message;
    protected @Nullable Throwable thrown;
    private ContextStack contextStack;
    private long threadId = Thread.currentThread().getId();
    private String threadName = Thread.currentThread().getName();
    private int threadPriority = Thread.currentThread().getPriority();
    private Clock clock;
    private NanoClock nanoClock;
    // Location data
    protected String fqcn;
    private @Nullable StackTraceElement location;
    private boolean requiresLocation;

    // Called right before relinquishing control from the thread.
    @Override
    public void translateTo(final RingBufferLogEvent event, final long sequence) {
        try {
            final StringMap contextData = event.getContextData();
            // Compute location if necessary
            event.setValues(
                    asyncLogger,
                    loggerName,
                    marker,
                    fqcn,
                    level,
                    message,
                    thrown,
                    // config properties are taken care of in the EventHandler thread
                    // in the AsyncLogger#actualAsyncLog method
                    contextDataInjector.injectContextData(null, contextData),
                    contextStack,
                    threadId,
                    threadName,
                    threadPriority,
                    // compute location if necessary
                    requiresLocation ? StackLocatorUtil.calcLocation(fqcn) : location,
                    clock,
                    nanoClock);
        } finally {
            clear(); // clear the translator
        }
    }

    /**
     * Release references held by this object to allow objects to be garbage-collected.
     */
    void clear() {
        setBasicValues(null, null, null, null, null, null, null, null, null, null, null, null, false);
    }

    public void setBasicValues(
            final AsyncLogger asyncLogger,
            final String loggerName,
            final @Nullable Marker marker,
            final String fqcn,
            final Level level,
            final @Nullable Message message,
            final @Nullable Throwable throwable,
            final ContextStack contextStack,
            final @Nullable StackTraceElement location,
            final Clock clock,
            final NanoClock nanoClock,
            final ContextDataInjector contextDataInjector,
            final boolean includeLocation) {
        this.asyncLogger = asyncLogger;
        this.loggerName = loggerName;
        this.marker = marker;
        this.fqcn = fqcn;
        this.level = level;
        this.message = message;
        this.thrown = throwable;
        this.contextStack = contextStack;
        this.location = location;
        this.clock = clock;
        this.nanoClock = nanoClock;
        this.contextDataInjector = contextDataInjector;
        this.requiresLocation = location == null && includeLocation;
    }

    public void updateThreadValues() {
        final Thread currentThread = Thread.currentThread();
        this.threadId = currentThread.getId();
        this.threadName = currentThread.getName();
        this.threadPriority = currentThread.getPriority();
    }
}
