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
package org.apache.logging.log4j.core.async;

import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.message.Message;

import com.lmax.disruptor.EventTranslator;

/**
 * This class is responsible for writing elements that make up a log event into
 * the ringbuffer {@code RingBufferLogEvent}. After this translator populated
 * the ringbuffer event, the disruptor will update the sequence number so that
 * the event can be consumed by another thread.
 */
public class RingBufferLogEventTranslator implements
        EventTranslator<RingBufferLogEvent> {

    private AsyncLogger asyncLogger;
    private String loggerName;
    private Marker marker;
    private String fqcn;
    private Level level;
    private Message message;
    private Throwable thrown;
    private Map<String, String> contextMap;
    private ContextStack contextStack;
    private String threadName;
    private StackTraceElement location;
    private long currentTimeMillis;
    private long nanoTime;

    // @Override
    @Override
    public void translateTo(final RingBufferLogEvent event, final long sequence) {
        event.setValues(asyncLogger, loggerName, marker, fqcn, level, message,
                thrown, contextMap, contextStack, threadName, location,
                currentTimeMillis, nanoTime);
        clear();
    }

    /**
     * Release references held by this object to allow objects to be
     * garbage-collected.
     */
    private void clear() {
        setValues(null, // asyncLogger
                null, // loggerName
                null, // marker
                null, // fqcn
                null, // level
                null, // data
                null, // t
                null, // map
                null, // contextStack
                null, // threadName
                null, // location
                0, // currentTimeMillis
                0 // nanoTime
        );
    }

    public void setValues(final AsyncLogger asyncLogger, final String loggerName,
            final Marker marker, final String fqcn, final Level level, final Message message,
            final Throwable thrown, final Map<String, String> contextMap,
            final ContextStack contextStack, final String threadName,
            final StackTraceElement location, final long currentTimeMillis, final long nanoTime) {
        this.asyncLogger = asyncLogger;
        this.loggerName = loggerName;
        this.marker = marker;
        this.fqcn = fqcn;
        this.level = level;
        this.message = message;
        this.thrown = thrown;
        this.contextMap = contextMap;
        this.contextStack = contextStack;
        this.threadName = threadName;
        this.location = location;
        this.currentTimeMillis = currentTimeMillis;
        this.nanoTime = nanoTime;
    }
}
