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

import java.util.Objects;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.ThrowableProxy;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ReusableMessage;
import org.apache.logging.log4j.util.InternalApi;
import org.apache.logging.log4j.util.StackLocatorUtil;
import org.apache.logging.log4j.util.StringMap;

public class ImmutableLogEvent implements LogEvent {
    private final StringMap contextData;
    private final ThreadContext.ContextStack contextStack;
    private final String loggerFqcn;
    private final Level level;
    private final String loggerName;
    private final Marker marker;
    private Message message;
    private final MutableInstant instant = new MutableInstant();
    private StackTraceElement source;
    private String threadName;
    private long threadId;
    private int threadPriority;
    private final Throwable thrown;
    ThrowableProxy thrownProxy;
    private boolean endOfBatch;
    private boolean includeLocation;
    private final long nanoTime;

    ImmutableLogEvent(final String loggerName, final Marker marker, final String loggerFqcn, final Level level,
                      final Message message, final Throwable thrown, final ThrowableProxy thrownProxy,
                      final StringMap contextData, final ThreadContext.ContextStack contextStack,
                      final long threadId, final String threadName, final int threadPriority,
                      final StackTraceElement source, final Instant instant, final long nanoTime,
                      final boolean endOfBatch, final boolean includeLocation) {
        this.loggerName = loggerName;
        this.marker = marker;
        this.loggerFqcn = loggerFqcn;
        this.level = level;
        this.message = message;
        this.thrown = thrown;
        this.thrownProxy = thrownProxy;
        this.contextData = contextData;
        this.contextStack = contextStack;
        this.threadId = threadId;
        this.threadName = threadName;
        this.threadPriority = threadPriority;
        this.source = source;
        this.instant.initFrom(instant);
        this.nanoTime = nanoTime;
        this.endOfBatch = endOfBatch;
        this.includeLocation = includeLocation;
    }

    @Override
    public StringMap getContextData() {
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
    public MutableInstant getInstant() {
        return instant;
    }

    @Override
    public StackTraceElement getSource() {
        if (source != null) {
            return source;
        }
        if (loggerFqcn == null || !includeLocation) {
            return null;
        }
        source = StackLocatorUtil.calcLocation(loggerFqcn);
        return source;
    }

    StackTraceElement getSourceOrNull() {
        return source;
    }

    @Override
    public String getThreadName() {
        if (threadName == null) {
            threadName = Thread.currentThread().getName();
        }
        return threadName;
    }

    String getThreadNameOrNull() {
        return threadName;
    }

    @Override
    public long getThreadId() {
        if (threadId == 0) {
            threadId = Thread.currentThread().getId();
        }
        return threadId;
    }

    long getThreadIdOrZero() {
        return threadId;
    }

    @Override
    public int getThreadPriority() {
        if (threadPriority == 0) {
            threadPriority = Thread.currentThread().getPriority();
        }
        return threadPriority;
    }

    int getThreadPriorityOrZero() {
        return threadPriority;
    }

    @Override
    public Throwable getThrown() {
        return thrown;
    }

    @Override
    public ThrowableProxy getThrownProxy() {
        if (thrownProxy == null && thrown != null) {
            thrownProxy = new ThrowableProxy(thrown);
        }
        return thrownProxy;
    }

    ThrowableProxy getThrownProxyOrNull() {
        return thrownProxy;
    }

    @Override
    public boolean isEndOfBatch() {
        return endOfBatch;
    }

    @Override
    public boolean isIncludeLocation() {
        return includeLocation;
    }

    @Override
    public long getNanoTime() {
        return nanoTime;
    }

    @Override
    public LogEvent toImmutable() {
        if (message instanceof ReusableMessage) {
            freezeMessage();
        }
        return this;
    }

    @InternalApi
    public void freezeMessage() {
        final Message message = this.message;
        this.message = new MementoMessage(message.getFormattedMessage(), message.getFormat(), message.getParameters());
    }

    @Override
    public long getTimeMillis() {
        return instant.getEpochMillisecond();
    }

    @Override
    public void setEndOfBatch(final boolean endOfBatch) {
        this.endOfBatch = endOfBatch;
    }

    @Override
    public void setIncludeLocation(final boolean locationRequired) {
        this.includeLocation = locationRequired;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        final String n = loggerName.isEmpty() ? LoggerConfig.ROOT : loggerName;
        sb.append("Logger=").append(n);
        sb.append(" Level=").append(level.name());
        sb.append(" Message=").append(message == null ? null : message.getFormattedMessage());
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ImmutableLogEvent that = (ImmutableLogEvent) o;
        return threadId == that.threadId
                && threadPriority == that.threadPriority
                && endOfBatch == that.endOfBatch
                && includeLocation == that.includeLocation
                && nanoTime == that.nanoTime
                && Objects.equals(contextData, that.contextData)
                && Objects.equals(contextStack, that.contextStack)
                && Objects.equals(loggerFqcn, that.loggerFqcn)
                && level.equals(that.level)
                && loggerName.equals(that.loggerName)
                && Objects.equals(marker, that.marker)
                && message.equals(that.message)
                && instant.equals(that.instant)
                && Objects.equals(source, that.source)
                && Objects.equals(threadName, that.threadName)
                && Objects.equals(thrown, that.thrown)
                && Objects.equals(thrownProxy, that.thrownProxy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contextData, contextStack, loggerFqcn, level, loggerName, marker, message, instant, source,
                threadName, threadId, threadPriority, thrown, thrownProxy, endOfBatch, includeLocation, nanoTime);
    }
}
