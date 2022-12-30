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

import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.ThrowableProxy;
import org.apache.logging.log4j.core.async.RingBufferLogEvent;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.core.time.internal.SystemClock;
import org.apache.logging.log4j.message.LoggerNameAwareMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.TimestampMessage;
import org.apache.logging.log4j.plugins.util.Builder;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.util.Strings;

public class LogEventBuilder implements LogEvent, Builder<LogEvent> {
    private String loggerFqcn;
    private String loggerName = Strings.EMPTY;
    private Level level = Level.OFF;
    private Marker marker;
    private Message message;
    private Throwable thrown;
    private ThrowableProxy thrownProxy;
    private Clock clock;
    private final MutableInstant instant = new MutableInstant();
    private ContextDataInjector contextDataInjector;
    private ContextDataFactory contextDataFactory;
    private StringMap contextData;
    private ThreadContext.ContextStack contextStack;
    private long threadId;
    private String threadName;
    private int threadPriority;
    private StackTraceElement source;
    private boolean includeLocation;
    private boolean endOfBatch;
    private long nanoTime;

    @Override
    public String getLoggerFqcn() {
        return loggerFqcn;
    }

    public LogEventBuilder setLoggerFqcn(final String loggerFqcn) {
        this.loggerFqcn = loggerFqcn;
        return this;
    }

    @Override
    public String getLoggerName() {
        return loggerName;
    }

    public LogEventBuilder setLoggerName(final String loggerName) {
        this.loggerName = loggerName;
        return this;
    }

    @Override
    public Level getLevel() {
        if (level == null) {
            level = Level.OFF; // LOG4J2-462, LOG4J2-465
        }
        return level;
    }

    public LogEventBuilder setLevel(final Level level) {
        this.level = level;
        return this;
    }

    @Override
    public Marker getMarker() {
        return marker;
    }

    public LogEventBuilder setMarker(final Marker marker) {
        this.marker = marker;
        return this;
    }

    @Override
    public Message getMessage() {
        return message;
    }

    public LogEventBuilder setMessage(final Message message) {
        this.message = message;
        return this;
    }

    @Override
    public Throwable getThrown() {
        return thrown;
    }

    public LogEventBuilder setThrown(final Throwable thrown) {
        this.thrown = thrown;
        return this;
    }

    @Override
    public ThrowableProxy getThrownProxy() {
        return thrownProxy;
    }

    public LogEventBuilder setThrownProxy(final ThrowableProxy thrownProxy) {
        this.thrownProxy = thrownProxy;
        return this;
    }

    public Clock getClock() {
        if (clock == null) {
            clock = new SystemClock();
        }
        return clock;
    }

    public LogEventBuilder setClock(final Clock clock) {
        this.clock = clock;
        return this;
    }

    @Override
    public MutableInstant getInstant() {
        initTimeFields();
        return instant;
    }

    public LogEventBuilder setInstant(final Instant instant) {
        this.instant.initFrom(instant);
        return this;
    }

    @Override
    public long getTimeMillis() {
        return instant.getEpochMillisecond();
    }

    public LogEventBuilder setTimeMillis(final long millis) {
        this.instant.initFromEpochMilli(millis, 0);
        return this;
    }

    public ContextDataInjector getContextDataInjector() {
        return contextDataInjector;
    }

    public LogEventBuilder setContextDataInjector(final ContextDataInjector contextDataInjector) {
        this.contextDataInjector = contextDataInjector;
        return this;
    }

    public ContextDataFactory getContextDataFactory() {
        if (contextDataFactory == null) {
            contextDataFactory = new DefaultContextDataFactory();
        }
        return contextDataFactory;
    }

    public LogEventBuilder setContextDataFactory(final ContextDataFactory contextDataFactory) {
        this.contextDataFactory = contextDataFactory;
        return this;
    }

    @Override
    public StringMap getContextData() {
        if (contextData == null) {
            contextData = getContextDataFactory().createContextData();
        }
        return contextData;
    }

    public LogEventBuilder setContextData(final StringMap contextData) {
        this.contextData = contextData;
        return this;
    }

    public LogEventBuilder setContextData(final List<Property> properties) {
        if (contextDataInjector != null) {
            contextData = contextDataInjector.injectContextData(properties, getContextData());
        } else {
            contextData = getContextDataFactory().createContextData(properties.size());
            properties.forEach(property -> contextData.putValue(property.getName(), property.getValue()));
        }
        return this;
    }

    @Override
    public ThreadContext.ContextStack getContextStack() {
        if (contextStack == null) {
            contextStack = ThreadContext.EMPTY_STACK;
        }
        return contextStack;
    }

    public LogEventBuilder setContextStack(final ThreadContext.ContextStack contextStack) {
        this.contextStack = contextStack;
        return this;
    }

    @Override
    public long getThreadId() {
        if (threadId == 0) {
            threadId = Thread.currentThread().getId();
        }
        return threadId;
    }

    public LogEventBuilder setThreadId(final long threadId) {
        this.threadId = threadId;
        return this;
    }

    @Override
    public String getThreadName() {
        if (threadName == null) {
            threadName = Thread.currentThread().getName();
        }
        return threadName;
    }

    public LogEventBuilder setThreadName(final String threadName) {
        this.threadName = threadName;
        return this;
    }

    @Override
    public int getThreadPriority() {
        if (threadPriority == 0) {
            threadPriority = Thread.currentThread().getPriority();
        }
        return threadPriority;
    }

    public LogEventBuilder setThreadPriority(final int threadPriority) {
        this.threadPriority = threadPriority;
        return this;
    }

    @Override
    public StackTraceElement getSource() {
        return source;
    }

    public LogEventBuilder setSource(final StackTraceElement source) {
        this.source = source;
        return this;
    }

    @Override
    public boolean isIncludeLocation() {
        return includeLocation;
    }

    public LogEventBuilder includeLocation(final boolean includeLocation) {
        this.includeLocation = includeLocation;
        return this;
    }

    @Override
    public void setIncludeLocation(final boolean locationRequired) {
        this.includeLocation = locationRequired;
    }

    public boolean isEndOfBatch() {
        return endOfBatch;
    }

    public LogEventBuilder endOfBatch(final boolean endOfBatch) {
        this.endOfBatch = endOfBatch;
        return this;
    }

    @Override
    public void setEndOfBatch(final boolean endOfBatch) {
        this.endOfBatch = endOfBatch;
    }

    public long getNanoTime() {
        return nanoTime;
    }

    public LogEventBuilder setNanoTime(final long nanoTime) {
        this.nanoTime = nanoTime;
        return this;
    }

    public LogEventBuilder copyFrom(final LogEvent other) {
        if (other instanceof RingBufferLogEvent) {
            ((RingBufferLogEvent) other).initializeBuilder(this);
            return this;
        }
        if (other instanceof MutableLogEvent) {
            ((MutableLogEvent) other).initializeBuilder(this);
            return this;
        }
        this.loggerFqcn = other.getLoggerFqcn();
        this.marker = other.getMarker();
        this.level = other.getLevel();
        this.loggerName = other.getLoggerName();
        this.message = other.getMessage();
        this.instant.initFrom(other.getInstant());
        this.thrown = other.getThrown();
        this.contextStack = other.getContextStack();
        this.includeLocation = other.isIncludeLocation();
        this.endOfBatch = other.isEndOfBatch();
        this.nanoTime = other.getNanoTime();

        // Avoid unnecessarily initializing thrownProxy, threadName and source if possible
        if (other instanceof ImmutableLogEvent) {
            final ImmutableLogEvent evt = (ImmutableLogEvent) other;
            this.contextData = evt.getContextData();
            this.thrownProxy = evt.getThrownProxyOrNull();
            this.source = evt.getSourceOrNull();
            this.threadId = evt.getThreadIdOrZero();
            this.threadName = evt.getThreadNameOrNull();
            this.threadPriority = evt.getThreadPriorityOrZero();
        } else {
            final ReadOnlyStringMap contextData = other.getContextData();
            if (contextData instanceof StringMap) {
                this.contextData = (StringMap) contextData;
            } else {
                final StringMap data = this.contextData;
                if (data == null) {
                    this.contextData = contextDataFactory.createContextData();
                } else {
                    if (data.isFrozen()) {
                        this.contextData = contextDataFactory.createContextData();
                    } else {
                        data.clear();
                    }
                }
                this.contextData.putAll(contextData);
            }
            this.thrownProxy = other.getThrownProxy();
            this.source = other.getSource();
            this.threadId = other.getThreadId();
            this.threadName = other.getThreadName();
            this.threadPriority = other.getThreadPriority();
        }
        return this;
    }

    private void initTimeFields() {
        if (instant.getEpochMillisecond() == 0) {
            if (message instanceof TimestampMessage) {
                setTimeMillis(((TimestampMessage) message).getTimestamp());
            } else {
                instant.initFrom(getClock());
            }
        }
    }

    @Override
    public LogEvent toImmutable() {
        return get();
    }

    @Override
    public LogEvent build() {
        if (message instanceof LoggerNameAwareMessage) {
            ((LoggerNameAwareMessage) message).setLoggerName(loggerName);
        }
        initTimeFields();
        // thread values should use fields to allow for lazy initialization
        return new ImmutableLogEvent(loggerName, marker, loggerFqcn, getLevel(), message, thrown, thrownProxy,
                getContextData(), getContextStack(), threadId, threadName, threadPriority,
                source, instant, nanoTime, endOfBatch, includeLocation);
    }
}
