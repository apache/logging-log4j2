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

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.message.AsynchronouslyFormattable;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.ReusableMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.util.Strings;

/**
 * Mutable implementation of the {@code LogEvent} interface.
 * @since 2.6
 */
public class MutableLogEvent implements LogEvent, ReusableMessage {
    private static final Message EMPTY = new SimpleMessage(Strings.EMPTY);

    private int threadPriority;
    private long threadId;
    private long timeMillis;
    private long nanoTime;
    private short parameterCount;
    private boolean includeLocation;
    private boolean endOfBatch = false;
    private Level level;
    private String threadName;
    private String loggerName;
    private Message message;
    private StringBuilder messageText;
    private Object[] parameters;
    private Throwable thrown;
    private ThrowableProxy thrownProxy;
    private StringMap contextData = ContextDataFactory.createContextData();
    private Marker marker;
    private String loggerFqcn;
    private StackTraceElement source;
    private ThreadContext.ContextStack contextStack;
    transient boolean reserved = false;

    public MutableLogEvent() {
        this(new StringBuilder(Constants.INITIAL_REUSABLE_MESSAGE_SIZE), new Object[10]);
    }

    public MutableLogEvent(final StringBuilder msgText, final Object[] replacementParameters) {
        this.messageText = msgText;
        this.parameters = replacementParameters;
    }

    public Log4jLogEvent toImmutable() {
        return createMemento();
    }

    /**
     * Initialize the fields of this {@code MutableLogEvent} from another event.
     * Similar in purpose and usage as {@link org.apache.logging.log4j.core.impl.Log4jLogEvent.LogEventProxy},
     * but a mutable version.
     * <p>
     * This method is used on async logger ringbuffer slots holding MutableLogEvent objects in each slot.
     * </p>
     *
     * @param event the event to copy data from
     */
    public void initFrom(final LogEvent event) {
        this.loggerFqcn = event.getLoggerFqcn();
        this.marker = event.getMarker();
        this.level = event.getLevel();
        this.loggerName = event.getLoggerName();
        this.timeMillis = event.getTimeMillis();
        this.thrown = event.getThrown();
        this.thrownProxy = event.getThrownProxy();

        // NOTE: this ringbuffer event SHOULD NOT keep a reference to the specified
        // thread-local MutableLogEvent's context data, because then two threads would call
        // ReadOnlyStringMap.clear() on the same shared instance, resulting in data corruption.
        this.contextData.putAll(event.getContextData());

        this.contextStack = event.getContextStack();
        this.source = event.isIncludeLocation() ? event.getSource() : null;
        this.threadId = event.getThreadId();
        this.threadName = event.getThreadName();
        this.threadPriority = event.getThreadPriority();
        this.endOfBatch = event.isEndOfBatch();
        this.includeLocation = event.isIncludeLocation();
        this.nanoTime = event.getNanoTime();
        setMessage(event.getMessage());
    }

    /**
     * Clears all references this event has to other objects.
     */
    public void clear() {
        loggerFqcn = null;
        marker = null;
        level = null;
        loggerName = null;
        message = null;
        thrown = null;
        thrownProxy = null;
        source = null;
        if (contextData != null) {
            if (contextData.isFrozen()) { // came from CopyOnWrite thread context
                contextData = null;
            } else {
                contextData.clear();
            }
        }
        contextStack = null;

        // ThreadName should not be cleared: this field is set in the ReusableLogEventFactory
        // where this instance is kept in a ThreadLocal, so it usually does not change.
        // threadName = null; // no need to clear threadName

        trimMessageText();
        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                parameters[i] = null;
            }
        }

        // primitive fields that cannot be cleared:
        //timeMillis;
        //threadId;
        //threadPriority;
        //includeLocation;
        //endOfBatch;
        //nanoTime;
    }

    // ensure that excessively long char[] arrays are not kept in memory forever
    private void trimMessageText() {
        if (messageText != null && messageText.length() > Constants.MAX_REUSABLE_MESSAGE_SIZE) {
            messageText.setLength(Constants.MAX_REUSABLE_MESSAGE_SIZE);
            messageText.trimToSize();
        }
    }

    @Override
    public String getLoggerFqcn() {
        return loggerFqcn;
    }

    public void setLoggerFqcn(final String loggerFqcn) {
        this.loggerFqcn = loggerFqcn;
    }

    @Override
    public Marker getMarker() {
        return marker;
    }

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

    public void setLevel(final Level level) {
        this.level = level;
    }

    @Override
    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(final String loggerName) {
        this.loggerName = loggerName;
    }

    @Override
    public Message getMessage() {
        if (message == null) {
            return (messageText == null) ? EMPTY : this;
        }
        return message;
    }

    public void setMessage(final Message msg) {
        if (msg instanceof ReusableMessage) {
            final ReusableMessage reusable = (ReusableMessage) msg;
            reusable.formatTo(getMessageTextForWriting());
            if (parameters != null) {
                parameters = reusable.swapParameters(parameters);
                parameterCount = reusable.getParameterCount();
            }
        } else {
            // if the Message instance is reused, there is no point in freezing its message here
            if (msg != null && !canFormatMessageInBackground(msg)) {
                msg.getFormattedMessage(); // LOG4J2-763: ask message to freeze parameters
            }
            this.message = msg;
        }
    }

    private boolean canFormatMessageInBackground(final Message message) {
        return Constants.FORMAT_MESSAGES_IN_BACKGROUND // LOG4J2-898: user wants to format all msgs in background
                || message.getClass().isAnnotationPresent(AsynchronouslyFormattable.class); // LOG4J2-1718
    }

    private StringBuilder getMessageTextForWriting() {
        if (messageText == null) {
            // Should never happen:
            // only happens if user logs a custom reused message when Constants.ENABLE_THREADLOCALS is false
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
        return null;
    }

    /**
     * @see ReusableMessage#getParameters()
     */
    @Override
    public Object[] getParameters() {
        return parameters == null ? null : Arrays.copyOf(parameters, parameterCount);
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
        if (message != null) {
            return message;
        }
        final Object[] params = parameters == null ? new Object[0] : Arrays.copyOf(parameters, parameterCount);
        return new ParameterizedMessage(messageText.toString(), params);
    }

    @Override
    public Throwable getThrown() {
        return thrown;
    }

    public void setThrown(final Throwable thrown) {
        this.thrown = thrown;
    }

    @Override
    public long getTimeMillis() {
        return timeMillis;
    }

    public void setTimeMillis(final long timeMillis) {
        this.timeMillis = timeMillis;
    }

    /**
     * Returns the ThrowableProxy associated with the event, or null.
     * @return The ThrowableProxy associated with the event.
     */
    @Override
    public ThrowableProxy getThrownProxy() {
        if (thrownProxy == null && thrown != null) {
            thrownProxy = new ThrowableProxy(thrown);
        }
        return thrownProxy;
    }

    /**
     * Returns the StackTraceElement for the caller. This will be the entry that occurs right
     * before the first occurrence of FQCN as a class name.
     * @return the StackTraceElement for the caller.
     */
    @Override
    public StackTraceElement getSource() {
        if (source != null) {
            return source;
        }
        if (loggerFqcn == null || !includeLocation) {
            return null;
        }
        source = Log4jLogEvent.calcLocation(loggerFqcn);
        return source;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ReadOnlyStringMap getContextData() {
        return contextData;
    }

    @Override
    public Map<String, String> getContextMap() {
        return contextData.toMap();
    }

    public void setContextData(final StringMap mutableContextData) {
        this.contextData = mutableContextData;
    }

    @Override
    public ThreadContext.ContextStack getContextStack() {
        return contextStack;
    }

    public void setContextStack(final ThreadContext.ContextStack contextStack) {
        this.contextStack = contextStack;
    }

    @Override
    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(final long threadId) {
        this.threadId = threadId;
    }

    @Override
    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(final String threadName) {
        this.threadName = threadName;
    }

    @Override
    public int getThreadPriority() {
        return threadPriority;
    }

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

    public void setNanoTime(final long nanoTime) {
        this.nanoTime = nanoTime;
    }

    /**
     * Creates a LogEventProxy that can be serialized.
     * @return a LogEventProxy.
     */
    protected Object writeReplace() {
        return new Log4jLogEvent.LogEventProxy(this, this.includeLocation);
    }

    private void readObject(final ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Proxy required");
    }

    /**
     * Creates and returns a new immutable copy of this {@code MutableLogEvent}.
     * If {@link #isIncludeLocation()} is true, this will obtain caller location information.
     *
     * @return a new immutable copy of the data in this {@code MutableLogEvent}
     */
    public Log4jLogEvent createMemento() {
        return Log4jLogEvent.deserialize(Log4jLogEvent.serialize(this, includeLocation));
    }

    /**
     * Initializes the specified {@code Log4jLogEvent.Builder} from this {@code MutableLogEvent}.
     * @param builder the builder whose fields to populate
     */
    public void initializeBuilder(final Log4jLogEvent.Builder builder) {
        builder.setContextData(contextData) //
                .setContextStack(contextStack) //
                .setEndOfBatch(endOfBatch) //
                .setIncludeLocation(includeLocation) //
                .setLevel(getLevel()) // ensure non-null
                .setLoggerFqcn(loggerFqcn) //
                .setLoggerName(loggerName) //
                .setMarker(marker) //
                .setMessage(getNonNullImmutableMessage()) // ensure non-null & immutable
                .setNanoTime(nanoTime) //
                .setSource(source) //
                .setThreadId(threadId) //
                .setThreadName(threadName) //
                .setThreadPriority(threadPriority) //
                .setThrown(getThrown()) // may deserialize from thrownProxy
                .setThrownProxy(thrownProxy) // avoid unnecessarily creating thrownProxy
                .setTimeMillis(timeMillis);
    }

    private Message getNonNullImmutableMessage() {
        return message != null ? message : new SimpleMessage(String.valueOf(messageText));
    }
}
