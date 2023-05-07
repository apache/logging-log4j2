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

import java.util.Objects;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.message.LoggerNameAwareMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ReusableMessage;
import org.apache.logging.log4j.message.TimestampMessage;
import org.apache.logging.log4j.util.InternalApi;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.util.Strings;

/**
 * Immutable copy of a LogEvent.
 *
 * @since 3.0.0
 */
@InternalApi
public class MementoLogEvent implements LogEvent {
    private final String loggerFqcn;
    private final String loggerName;
    private final MutableInstant instant = new MutableInstant();
    private final long nanoTime;
    private final Level level;
    private final Marker marker;
    private boolean locationRequired;
    private boolean endOfBatch;
    private final Message message;
    private final ReadOnlyStringMap contextData;
    private final ThreadContext.ContextStack contextStack;
    private final StackTraceElement source;
    private final String threadName;
    private final long threadId;
    private final int threadPriority;
    private final Throwable thrown;
    private final ThrowableProxy thrownProxy;

    public MementoLogEvent(final LogEvent event) {
        loggerFqcn = event.getLoggerFqcn();
        loggerName = event.getLoggerName();
        instant.initFrom(event.getInstant());
        nanoTime = event.getNanoTime();
        level = event.getLevel();
        marker = event.getMarker();
        boolean includeLocation = event.isIncludeLocation();
        locationRequired = includeLocation;
        endOfBatch = event.isEndOfBatch();
        message = mementoOfMessage(event);
        if (instant.getEpochMillisecond() == 0 && message instanceof TimestampMessage) {
            instant.initFromEpochMilli(((TimestampMessage) message).getTimestamp(), 0);
        }
        contextData = memento(event.getContextData());
        contextStack = event.getContextStack();
        source = includeLocation ? event.getSource() : null;
        threadName = event.getThreadName();
        threadId = event.getThreadId();
        threadPriority = event.getThreadPriority();
        thrown = event.getThrown();
        thrownProxy = event.getThrownProxy();
    }

    public MementoLogEvent(final LogEvent event, final boolean includeLocation) {
        loggerFqcn = event.getLoggerFqcn();
        loggerName = event.getLoggerName();
        instant.initFrom(event.getInstant());
        nanoTime = event.getNanoTime();
        level = event.getLevel();
        marker = event.getMarker();
        locationRequired = includeLocation;
        endOfBatch = event.isEndOfBatch();
        message = mementoOfMessage(event);
        if (instant.getEpochMillisecond() == 0 && message instanceof TimestampMessage) {
            instant.initFromEpochMilli(((TimestampMessage) message).getTimestamp(), 0);
        }
        contextData = memento(event.getContextData());
        contextStack = event.getContextStack();
        source = includeLocation ? event.getSource() : null;
        threadName = event.getThreadName();
        threadId = event.getThreadId();
        threadPriority = event.getThreadPriority();
        thrown = event.getThrown();
        thrownProxy = event.getThrownProxy();
    }

    @Override
    public LogEvent toImmutable() {
        return this;
    }

    @Override
    public LogEvent toMemento(boolean includeLocation) {
        if (locationRequired || !includeLocation) {
            return this;
        }
        return LogEvent.super.toMemento(true);
    }

    @Override
    public LogEvent toMemento() {
        return this;
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
        return loggerFqcn;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public String getLoggerName() {
        return loggerName;
    }

    @Override
    public Marker getMarker() {
        return marker;
    }

    @Override
    public Message getMessage() {
        return message;
    }

    @Override
    public long getTimeMillis() {
        return instant.getEpochMillisecond();
    }

    @Override
    public Instant getInstant() {
        return instant;
    }

    @Override
    public StackTraceElement getSource() {
        return source;
    }

    @Override
    public String getThreadName() {
        return threadName;
    }

    @Override
    public long getThreadId() {
        return threadId;
    }

    @Override
    public int getThreadPriority() {
        return threadPriority;
    }

    @Override
    public Throwable getThrown() {
        return thrown;
    }

    @Override
    public ThrowableProxy getThrownProxy() {
        return thrownProxy;
    }

    @Override
    public boolean isEndOfBatch() {
        return endOfBatch;
    }

    @Override
    public boolean isIncludeLocation() {
        return locationRequired;
    }

    @Override
    public void setEndOfBatch(boolean endOfBatch) {
        this.endOfBatch = endOfBatch;
    }

    @Override
    public void setIncludeLocation(boolean locationRequired) {
        this.locationRequired = locationRequired;
    }

    @Override
    public long getNanoTime() {
        return nanoTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MementoLogEvent that = (MementoLogEvent) o;
        return nanoTime == that.nanoTime && locationRequired == that.locationRequired &&
                endOfBatch == that.endOfBatch && threadId == that.threadId && threadPriority == that.threadPriority &&
                Objects.equals(loggerFqcn, that.loggerFqcn) && Objects.equals(loggerName, that.loggerName) &&
                Objects.equals(instant, that.instant) && Objects.equals(level, that.level) &&
                Objects.equals(marker, that.marker) && Objects.equals(message, that.message) &&
                Objects.equals(contextData, that.contextData) && Objects.equals(contextStack, that.contextStack) &&
                Objects.equals(source, that.source) && Objects.equals(threadName, that.threadName) &&
                Objects.equals(thrown, that.thrown) && Objects.equals(thrownProxy, that.thrownProxy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loggerFqcn, loggerName, instant, nanoTime, level, marker, locationRequired, endOfBatch,
                message, contextData, contextStack, source, threadName, threadId, threadPriority, thrown, thrownProxy);
    }

    @Override
    public String toString() {
        final String n = loggerName.isEmpty() ? LoggerConfig.ROOT : loggerName;
        return "Logger=" + n + " Level=" + level.name() + " Message=" +
                (message == null ? Strings.EMPTY : message.getFormattedMessage());
    }

    private static ReadOnlyStringMap memento(final ReadOnlyStringMap readOnlyMap) {
        if (readOnlyMap instanceof StringMap && !((StringMap) readOnlyMap).isFrozen()) {
            StringMap data = ContextDataFactory.createContextData(readOnlyMap);
            data.freeze();
            return data;
        }
        // otherwise immutable
        return readOnlyMap;
    }

    private static Message mementoOfMessage(final LogEvent event) {
        Message message = event.getMessage();
        if (message instanceof LoggerNameAwareMessage) {
            ((LoggerNameAwareMessage) message).setLoggerName(event.getLoggerName());
        }
        return message instanceof ReusableMessage ? ((ReusableMessage) message).memento() : message;
    }
}
