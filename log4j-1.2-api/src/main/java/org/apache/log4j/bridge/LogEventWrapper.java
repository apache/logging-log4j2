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
package org.apache.log4j.bridge;

import org.apache.log4j.NDC;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.MutableThreadContextStack;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Exposes a Log4j 1 logging event as a Log4j 2 LogEvent.
 */
public class LogEventWrapper implements LogEvent {

    private final LoggingEvent event;
    private final ContextDataMap contextData;
    private final MutableThreadContextStack contextStack;
    private Thread thread;

    public LogEventWrapper(LoggingEvent event) {
        this.event = event;
        this.contextData = new ContextDataMap(event.getProperties());
        this.contextStack = new MutableThreadContextStack(NDC.cloneStack());
        this.thread = Objects.equals(event.getThreadName(), Thread.currentThread().getName())
                ? Thread.currentThread() : null;
    }

    @Override
    public LogEvent toImmutable() {
        return this;
    }

    @Override
    public Map<String, String> getContextMap() {
        return contextData;
    }

    @Override
    public ReadOnlyStringMap getContextData() {
        return contextData;
    }

    @Override
    public ThreadContext.ContextStack getContextStack() {
        return contextStack;
    }

    @Override
    public String getLoggerFqcn() {
        return null;
    }

    @Override
    public Level getLevel() {
        return OptionConverter.convertLevel(event.getLevel());
    }

    @Override
    public String getLoggerName() {
        return event.getLoggerName();
    }

    @Override
    public Marker getMarker() {
        return null;
    }

    @Override
    public Message getMessage() {
        return new SimpleMessage(event.getRenderedMessage());
    }

    @Override
    public long getTimeMillis() {
        return event.getTimeStamp();
    }

    @Override
    public Instant getInstant() {
        MutableInstant mutable = new MutableInstant();
        mutable.initFromEpochMilli(event.getTimeStamp(), 0);
        return mutable;
    }

    @Override
    public StackTraceElement getSource() {
        LocationInfo info = event.getLocationInformation();
        return new StackTraceElement(info.getClassName(), info.getMethodName(), info.getFileName(),
                Integer.parseInt(info.getLineNumber()));
    }

    @Override
    public String getThreadName() {
        return event.getThreadName();
    }

    @Override
    public long getThreadId() {
        Thread thread = getThread();
        return thread != null ? thread.getId() : 0;
    }

    @Override
    public int getThreadPriority() {
        Thread thread = getThread();
        return thread != null ? thread.getPriority() : 0;
    }

    private Thread getThread() {
        if (thread == null && event.getThreadName() != null) {
            for (Thread thread : Thread.getAllStackTraces().keySet()) {
                if (thread.getName().equals(event.getThreadName())) {
                    this.thread = thread;
                    return thread;
                }
            }
        }
        return thread;
    }

    @Override
    public Throwable getThrown() {
        ThrowableInformation throwableInformation = event.getThrowableInformation();
        return throwableInformation == null ? null : throwableInformation.getThrowable();
    }

    @Override
    public ThrowableProxy getThrownProxy() {
        return null;
    }

    @Override
    public boolean isEndOfBatch() {
        return false;
    }

    @Override
    public boolean isIncludeLocation() {
        return false;
    }

    @Override
    public void setEndOfBatch(boolean endOfBatch) {

    }

    @Override
    public void setIncludeLocation(boolean locationRequired) {

    }

    @Override
    public long getNanoTime() {
        return 0;
    }

    private static class ContextDataMap extends HashMap<String, String> implements ReadOnlyStringMap {

        ContextDataMap(Map<String, String> map) {
            if (map != null) {
                super.putAll(map);
            }
        }

        @Override
        public Map<String, String> toMap() {
            return this;
        }

        @Override
        public boolean containsKey(String key) {
            return super.containsKey(key);
        }

        @Override
        public <V> void forEach(BiConsumer<String, ? super V> action) {
            super.forEach((k,v) -> action.accept(k, (V) v));
        }

        @Override
        public <V, S> void forEach(TriConsumer<String, ? super V, S> action, S state) {
            super.forEach((k,v) -> action.accept(k, (V) v, state));
        }

        @Override
        public <V> V getValue(String key) {
            return (V) super.get(key);
        }
    }
}
