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

import java.util.Arrays;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.ReusableLogEvent;
import org.apache.logging.log4j.core.async.InternalAsyncUtil;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.core.time.NanoClock;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.kit.recycler.Recycler;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterConsumer;
import org.apache.logging.log4j.message.ParameterVisitable;
import org.apache.logging.log4j.message.ReusableMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.message.TimestampMessage;
import org.apache.logging.log4j.util.StackLocatorUtil;
import org.apache.logging.log4j.util.StringBuilders;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.util.Strings;
import org.jspecify.annotations.Nullable;

/**
 * Mutable implementation of the {@code ReusableLogEvent} interface.
 * @since 2.6
 * @see Recycler
 */
public class MutableLogEvent implements ReusableLogEvent, ReusableMessage, ParameterVisitable {
    private static final Message EMPTY = new SimpleMessage(Strings.EMPTY);

    private int threadPriority;
    private long threadId;
    private final MutableInstant instant = new MutableInstant();
    private long nanoTime;
    private short parameterCount;
    private boolean endOfBatch = false;
    private Level level;
    private String threadName;
    private String loggerName;
    private Message message;
    private String messageFormat;
    private StringBuilder messageText;
    private Object[] parameters;
    private Throwable thrown;
    private StringMap contextData = ContextDataFactory.createContextData();
    private Marker marker;
    private ThreadContext.ContextStack contextStack;
    // Location information
    private String loggerFqcn;
    private @Nullable StackTraceElement source;
    private boolean includeLocation;

    public MutableLogEvent() {
        // messageText and the parameter array are lazily initialized
        this(null, null);
    }

    public MutableLogEvent(final StringBuilder msgText, final Object[] replacementParameters) {
        this.messageText = msgText;
        this.parameters = replacementParameters;
    }

    /**
     * {@inheritDoc}
     * <p>
     *   If {@link #isIncludeLocation()} is true, caller information for this instance will also be computed.
     * </p>
     */
    @Override
    public LogEvent toImmutable() {
        return toMemento();
    }

    /**
     * Initialize the fields of this {@code ReusableLogEvent} from another event.
     * <p>
     * This method is used on async logger ringbuffer slots holding ReusableLogEvent objects in each slot.
     * </p>
     *
     * @param event the event to copy data from
     */
    public void moveValuesFrom(final LogEvent event) {
        this.marker = event.getMarker();
        this.level = event.getLevel();
        this.loggerName = event.getLoggerName();
        this.thrown = event.getThrown();

        this.instant.initFrom(event.getInstant());

        // NOTE: this ringbuffer event SHOULD NOT keep a reference to the specified
        // thread-local MutableLogEvent's context data, because then two threads would call
        // ReadOnlyStringMap.clear() on the same shared instance, resulting in data corruption.
        this.contextData.putAll(event.getContextData());

        this.contextStack = event.getContextStack();
        this.threadId = event.getThreadId();
        this.threadName = event.getThreadName();
        this.threadPriority = event.getThreadPriority();
        this.endOfBatch = event.isEndOfBatch();
        this.includeLocation = event.isIncludeLocation();
        this.nanoTime = event.getNanoTime();
        // Location data
        this.loggerFqcn = event.getLoggerFqcn();
        this.source = event.peekSource();
        this.includeLocation = event.isIncludeLocation();
        setMessage(event.getMessage());
    }

    @Override
    public void clear() {
        marker = null;
        level = null;
        loggerName = null;
        message = null;
        messageFormat = null;
        thrown = null;
        if (contextData != null) {
            if (contextData.isFrozen()) { // came from CopyOnWrite thread context
                contextData = null;
            } else {
                contextData.clear();
            }
        }
        contextStack = null;
        // Location information
        loggerFqcn = null;
        source = null;

        // ThreadName should not be cleared: this field is set in the ReusableLogEventFactory
        // where this instance is kept in a ThreadLocal, so it usually does not change.
        // threadName = null; // no need to clear threadName

        // ensure that excessively long char[] arrays are not kept in memory forever
        StringBuilders.trimToMaxSize(messageText, Constants.MAX_REUSABLE_MESSAGE_SIZE);

        if (parameters != null) {
            Arrays.fill(parameters, null);
        }

        // primitive fields that cannot be cleared:
        // timeMillis;
        // threadId;
        // threadPriority;
        // includeLocation;
        // endOfBatch;
        // nanoTime;
    }

    @Override
    public String getLoggerFqcn() {
        return loggerFqcn;
    }

    @Override
    public void setLoggerFqcn(final String loggerFqcn) {
        this.loggerFqcn = loggerFqcn;
    }

    @Override
    public Marker getMarker() {
        return marker;
    }

    @Override
    public void setMarker(final Marker marker) {
        this.marker = marker;
    }

    @Override
    public Level getLevel() {
        if (level == null) {
            level = Level.OFF; // LOG4J2-462, LOG4J2-465
        }
        return level;
    }

    @Override
    public void setLevel(final Level level) {
        this.level = level;
    }

    @Override
    public String getLoggerName() {
        return loggerName;
    }

    @Override
    public void setLoggerName(final String loggerName) {
        this.loggerName = loggerName;
    }

    @Override
    public Message getMessage() {
        if (message == null) {
            return messageText == null ? EMPTY : this;
        }
        return message;
    }

    /**
     * Sets the log message of the event.
     *
     * <p>
     *   <strong>Warning:</strong> This method <strong>mutates</strong> the state of the {@code message}
     *   parameter:
     * </p>
     * <ol>
     *   <li>
     *     If the message is a {@link ReusableMessage}, this method will remove its
     *     parameter references, which prevents it from being used again.
     *   </li>
     *   <li>
     *     Otherwise the lazy {@link Message#getFormattedMessage()} message might be called.
     *     See <a href="https://logging.apache.org/log4j/3.x/manual/systemproperties.html#log4j.async.formatMessagesInBackground">{@code log4j.async.formatMessagesInBackground}</a>
     *     for details.
     *   </li>
     * </ol>
     * @param message The log message. The object passed will be <strong>modified</strong> by this method and should not be reused.
     */
    @Override
    public void setMessage(final Message message) {
        if (message instanceof final ReusableMessage reusable) {
            reusable.formatTo(getMessageTextForWriting());
            messageFormat = message.getFormat();
            parameters = reusable.swapParameters(parameters == null ? new Object[10] : parameters);
            parameterCount = reusable.getParameterCount();
        } else {
            this.message = InternalAsyncUtil.makeMessageImmutable(message);
        }
    }

    private StringBuilder getMessageTextForWriting() {
        if (messageText == null) {
            // Happens the first time messageText is requested
            messageText = new StringBuilder(Constants.INITIAL_REUSABLE_MESSAGE_SIZE);
        }
        messageText.setLength(0);
        return messageText;
    }

    /**
     * @see ReusableMessage#getFormattedMessage()
     */
    @Override
    public String getFormattedMessage() {
        return messageText.toString();
    }

    /**
     * @see ReusableMessage#getFormat()
     */
    @Override
    public String getFormat() {
        return messageFormat;
    }

    /**
     * @see ReusableMessage#getParameters()
     */
    @Override
    public Object[] getParameters() {
        return parameters == null ? null : Arrays.copyOf(parameters, parameterCount);
    }

    @Override
    public <S> void forEachParameter(final ParameterConsumer<S> action, final S state) {
        if (parameters != null) {
            for (short i = 0; i < parameterCount; i++) {
                action.accept(parameters[i], i, state);
            }
        }
    }

    /**
     * @see ReusableMessage#getThrowable()
     */
    @Override
    public Throwable getThrowable() {
        return getThrown();
    }

    /**
     * @see ReusableMessage#formatTo(StringBuilder)
     */
    @Override
    public void formatTo(final StringBuilder buffer) {
        buffer.append(messageText);
    }

    /**
     * Replaces this ReusableMessage's parameter array with the specified value and return the original array
     * @param emptyReplacement the parameter array that can be used for subsequent uses of this reusable message
     * @return the original parameter array
     * @see ReusableMessage#swapParameters(Object[])
     */
    @Override
    public Object[] swapParameters(final Object[] emptyReplacement) {
        final Object[] result = this.parameters;
        this.parameters = emptyReplacement;
        return result;
    }

    /*
     * @see ReusableMessage#getParameterCount
     */
    @Override
    public short getParameterCount() {
        return parameterCount;
    }

    @Override
    public Message memento() {
        if (message == null) {
            message = new MementoMessage(String.valueOf(messageText), messageFormat, getParameters());
        }
        return message;
    }

    @Override
    public Throwable getThrown() {
        return thrown;
    }

    @Override
    public void setThrown(final Throwable thrown) {
        this.thrown = thrown;
    }

    void initTime(final Clock clock, final NanoClock nanoClock) {
        if (message instanceof TimestampMessage) {
            instant.initFromEpochMilli(((TimestampMessage) message).getTimestamp(), 0);
        } else {
            instant.initFrom(clock);
        }
        nanoTime = nanoClock.nanoTime();
    }

    @Override
    public long getTimeMillis() {
        return instant.getEpochMillisecond();
    }

    @Override
    public void setTimeMillis(final long timeMillis) {
        this.instant.initFromEpochMilli(timeMillis, 0);
    }

    @Override
    public Instant getInstant() {
        return instant;
    }

    @Override
    public void setInstant(final Instant instant) {
        this.instant.initFrom(instant);
    }

    @Override
    public void setSource(final StackTraceElement source) {
        this.source = source;
    }

    /**
     * Returns the StackTraceElement for the caller. This will be the entry that occurs right
     * before the first occurrence of FQCN as a class name.
     * @return the StackTraceElement for the caller.
     */
    @Override
    public StackTraceElement getSource() {
        if (source == null && loggerFqcn != null) {
            return source = includeLocation ? StackLocatorUtil.calcLocation(loggerFqcn) : null;
        }
        return peekSource();
    }

    @Override
    public @Nullable StackTraceElement peekSource() {
        return source;
    }

    @Override
    public StringMap getContextData() {
        return contextData;
    }

    @Override
    public void setContextData(final StringMap mutableContextData) {
        this.contextData = mutableContextData;
    }

    @Override
    public ThreadContext.ContextStack getContextStack() {
        return contextStack;
    }

    @Override
    public void setContextStack(final ThreadContext.ContextStack contextStack) {
        this.contextStack = contextStack;
    }

    @Override
    public long getThreadId() {
        return threadId;
    }

    @Override
    public void setThreadId(final long threadId) {
        this.threadId = threadId;
    }

    @Override
    public String getThreadName() {
        return threadName;
    }

    @Override
    public void setThreadName(final String threadName) {
        this.threadName = threadName;
    }

    @Override
    public int getThreadPriority() {
        return threadPriority;
    }

    @Override
    public void setThreadPriority(final int threadPriority) {
        this.threadPriority = threadPriority;
    }

    @Override
    public boolean isIncludeLocation() {
        return includeLocation;
    }

    @Override
    public void setIncludeLocation(final boolean includeLocation) {
        this.includeLocation = includeLocation;
    }

    @Override
    public boolean isEndOfBatch() {
        return endOfBatch;
    }

    @Override
    public void setEndOfBatch(final boolean endOfBatch) {
        this.endOfBatch = endOfBatch;
    }

    @Override
    public long getNanoTime() {
        return nanoTime;
    }

    @Override
    public void setNanoTime(final long nanoTime) {
        this.nanoTime = nanoTime;
    }
}
