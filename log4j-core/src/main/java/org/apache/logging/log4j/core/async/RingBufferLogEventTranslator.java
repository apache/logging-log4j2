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

    public void setValues(final AsyncLogger anAsyncLogger, final String aLoggerName, final Marker aMarker,
            final String theFqcn, final Level aLevel, final Message msg, final Throwable aThrowable,
            final Map<String, String> aMap, final ContextStack aContextStack, final String aThreadName,
            final StackTraceElement aLocation, final long aCurrentTimeMillis, final long aNanoTime) {
        this.asyncLogger = anAsyncLogger;
        this.loggerName = aLoggerName;
        this.marker = aMarker;
        this.fqcn = theFqcn;
        this.level = aLevel;
        this.message = msg;
        this.thrown = aThrowable;
        this.contextMap = aMap;
        this.contextStack = aContextStack;
        this.threadName = aThreadName;
        this.location = aLocation;
        this.currentTimeMillis = aCurrentTimeMillis;
        this.nanoTime = aNanoTime;
    }
}
