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
package org.apache.logging.log4j.core.async;

import com.lmax.disruptor.EventTranslator;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.impl.ContextDataInjectorFactory;
import org.apache.logging.log4j.core.util.Clock;
import org.apache.logging.log4j.core.util.NanoClock;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.StringMap;

/**
 * This class is responsible for writing elements that make up a log event into
 * the ringbuffer {@code RingBufferLogEvent}. After this translator populated
 * the ringbuffer event, the disruptor will update the sequence number so that
 * the event can be consumed by another thread.
 */
public class RingBufferLogEventTranslator implements EventTranslator<RingBufferLogEvent> {

    private static final ContextDataInjector INJECTOR = ContextDataInjectorFactory.createInjector();
    private AsyncLogger asyncLogger;
    String loggerName;
    protected Marker marker;
    protected String fqcn;
    protected Level level;
    protected Message message;
    protected Throwable thrown;
    private ContextStack contextStack;
    private long threadId = Thread.currentThread().getId();
    private String threadName = Thread.currentThread().getName();
    private int threadPriority = Thread.currentThread().getPriority();
    private StackTraceElement location;
    private Clock clock;
    private NanoClock nanoClock;

    // @Override
    @Override
    public void translateTo(final RingBufferLogEvent event, final long sequence) {
        try {
            final ReadOnlyStringMap contextData = event.getContextData();
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
                    INJECTOR.injectContextData(null, contextData instanceof StringMap ? (StringMap) contextData : null),
                    contextStack,
                    threadId,
                    threadName,
                    threadPriority,
                    location,
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
        setBasicValues(
                null, // asyncLogger
                null, // loggerName
                null, // marker
                null, // fqcn
                null, // level
                null, // data
                null, // t
                null, // contextStack
                null, // location
                null, // clock
                null // nanoClock
                );
    }

    public void setBasicValues(
            final AsyncLogger anAsyncLogger,
            final String aLoggerName,
            final Marker aMarker,
            final String theFqcn,
            final Level aLevel,
            final Message msg,
            final Throwable aThrowable,
            final ContextStack aContextStack,
            final StackTraceElement aLocation,
            final Clock aClock,
            final NanoClock aNanoClock) {
        this.asyncLogger = anAsyncLogger;
        this.loggerName = aLoggerName;
        this.marker = aMarker;
        this.fqcn = theFqcn;
        this.level = aLevel;
        this.message = msg;
        this.thrown = aThrowable;
        this.contextStack = aContextStack;
        this.location = aLocation;
        this.clock = aClock;
        this.nanoClock = aNanoClock;
    }

    public void updateThreadValues() {
        final Thread currentThread = Thread.currentThread();
        this.threadId = currentThread.getId();
        this.threadName = currentThread.getName();
        this.threadPriority = currentThread.getPriority();
    }
}
