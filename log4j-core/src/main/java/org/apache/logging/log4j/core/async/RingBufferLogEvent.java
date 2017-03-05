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

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.message.AsynchronouslyFormattable;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.ReusableMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.message.TimestampMessage;
import org.apache.logging.log4j.util.Strings;

import com.lmax.disruptor.EventFactory;

/**
 * When the Disruptor is started, the RingBuffer is populated with event objects. These objects are then re-used during
 * the life of the RingBuffer.
 */
public class RingBufferLogEvent implements LogEvent, ReusableMessage, CharSequence {

    /** The {@code EventFactory} for {@code RingBufferLogEvent}s. */
    public static final Factory FACTORY = new Factory();

    private static final long serialVersionUID = 8462119088943934758L;
    private static final Message EMPTY = new SimpleMessage(Strings.EMPTY);

    /**
     * Creates the events that will be put in the RingBuffer.
     */
    private static class Factory implements EventFactory<RingBufferLogEvent> {

        @Override
        public RingBufferLogEvent newInstance() {
            final RingBufferLogEvent result = new RingBufferLogEvent();
            if (Constants.ENABLE_THREADLOCALS) {
                result.messageText = new StringBuilder(Constants.INITIAL_REUSABLE_MESSAGE_SIZE);
                result.parameters = new Object[10];
            }
            return result;
        }
    }

    private int threadPriority;
    private long threadId;
    private long currentTimeMillis;
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
    private transient Throwable thrown;
    private ThrowableProxy thrownProxy;
    private StringMap contextData = ContextDataFactory.createContextData();
    private Marker marker;
    private String fqcn;
    private StackTraceElement location;
    private ContextStack contextStack;

    private transient AsyncLogger asyncLogger;

    public void setValues(final AsyncLogger anAsyncLogger, final String aLoggerName, final Marker aMarker,
            final String theFqcn, final Level aLevel, final Message msg, final Throwable aThrowable,
            final StringMap mutableContextData, final ContextStack aContextStack, final long threadId,
            final String threadName, final int threadPriority, final StackTraceElement aLocation,
            final long aCurrentTimeMillis, final long aNanoTime) {
        this.threadPriority = threadPriority;
        this.threadId = threadId;
        this.currentTimeMillis = aCurrentTimeMillis;
        this.nanoTime = aNanoTime;
        this.level = aLevel;
        this.threadName = threadName;
        this.loggerName = aLoggerName;
        setMessage(msg);
        this.thrown = aThrowable;
        this.thrownProxy = null;
        this.marker = aMarker;
        this.fqcn = theFqcn;
        this.location = aLocation;
        this.contextData = mutableContextData;
        this.contextStack = aContextStack;
        this.asyncLogger = anAsyncLogger;
    }

    @Override
    public LogEvent toImmutable() {
        return createMemento();
    }
    
    private void setMessage(final Message msg) {
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
     * Event processor that reads the event from the ringbuffer can call this method.
     *
     * @param endOfBatch flag to indicate if this is the last event in a batch from the RingBuffer
     */
    public void execute(final boolean endOfBatch) {
        this.endOfBatch = endOfBatch;
        asyncLogger.actualAsyncLog(this);
    }

    /**
     * Returns {@code true} if this event is the end of a batch, {@code false} otherwise.
     *
     * @return {@code true} if this event is the end of a batch, {@code false} otherwise
     */
    @Override
    public boolean isEndOfBatch() {
        return endOfBatch;
    }

    @Override
    public void setEndOfBatch(final boolean endOfBatch) {
        this.endOfBatch = endOfBatch;
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
    public String getLoggerName() {
        return loggerName;
    }

    @Override
    public Marker getMarker() {
        return marker;
    }

    @Override
    public String getLoggerFqcn() {
        return fqcn;
    }

    @Override
    public Level getLevel() {
        if (level == null) {
            level = Level.OFF; // LOG4J2-462, LOG4J2-465
        }
        return level;
    }

    @Override
    public Message getMessage() {
        if (message == null) {
            return messageText == null ? EMPTY : this;
        }
        return message;
    }

    /**
     * @see ReusableMessage#getFormattedMessage()
     */
    @Override
    public String getFormattedMessage() {
        return messageText != null // LOG4J2-1527: may be null in web apps
                ? messageText.toString() // note: please keep below "redundant" braces for readability
                : (message == null ? null : message.getFormattedMessage());
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

    // CharSequence impl

    @Override
    public int length() {
        return messageText.length();
    }

    @Override
    public char charAt(final int index) {
        return messageText.charAt(index);
    }

    @Override
    public CharSequence subSequence(final int start, final int end) {
        return messageText.subSequence(start, end);
    }


    private Message getNonNullImmutableMessage() {
        return message != null ? message : new SimpleMessage(String.valueOf(messageText));
    }

    @Override
    public Throwable getThrown() {
        // after deserialization, thrown is null but thrownProxy may be non-null
        if (thrown == null) {
            if (thrownProxy != null) {
                thrown = thrownProxy.getThrowable();
            }
        }
        return thrown;
    }

    @Override
    public ThrowableProxy getThrownProxy() {
        // lazily instantiate the (expensive) ThrowableProxy
        if (thrownProxy == null) {
            if (thrown != null) {
                thrownProxy = new ThrowableProxy(thrown);
            }
        }
        return this.thrownProxy;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ReadOnlyStringMap getContextData() {
        return contextData;
    }

    void setContextData(final StringMap contextData) {
        this.contextData = contextData;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> getContextMap() {
        return contextData.toMap();
    }

    @Override
    public ContextStack getContextStack() {
        return contextStack;
    }

    @Override
    public long getThreadId() {
        return threadId;
    }

    @Override
    public String getThreadName() {
        return threadName;
    }

    @Override
    public int getThreadPriority() {
        return threadPriority;
    }

    @Override
    public StackTraceElement getSource() {
        return location;
    }

    @Override
    public long getTimeMillis() {
        return message instanceof TimestampMessage ? ((TimestampMessage) message).getTimestamp() :currentTimeMillis;
    }

    @Override
    public long getNanoTime() {
        return nanoTime;
    }

    /**
     * Release references held by ring buffer to allow objects to be garbage-collected.
     */
    public void clear() {
        this.asyncLogger = null;
        this.loggerName = null;
        this.marker = null;
        this.fqcn = null;
        this.level = null;
        this.message = null;
        this.thrown = null;
        this.thrownProxy = null;
        this.contextStack = null;
        this.location = null;
        if (contextData != null) {
            if (contextData.isFrozen()) { // came from CopyOnWrite thread context
                contextData = null;
            } else {
                contextData.clear();
            }
        }

        trimMessageText();

        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                parameters[i] = null;
            }
        }
    }

    // ensure that excessively long char[] arrays are not kept in memory forever
    private void trimMessageText() {
        if (messageText != null && messageText.length() > Constants.MAX_REUSABLE_MESSAGE_SIZE) {
            messageText.setLength(Constants.MAX_REUSABLE_MESSAGE_SIZE);
            messageText.trimToSize();
        }
    }

    private void writeObject(final java.io.ObjectOutputStream out) throws IOException {
        getThrownProxy(); // initialize the ThrowableProxy before serializing
        out.defaultWriteObject();
    }

    /**
     * Creates and returns a new immutable copy of this {@code RingBufferLogEvent}.
     *
     * @return a new immutable copy of the data in this {@code RingBufferLogEvent}
     */
    public LogEvent createMemento() {
        return new Log4jLogEvent.Builder(this).build();
        
    }

    /**
     * Initializes the specified {@code Log4jLogEvent.Builder} from this {@code RingBufferLogEvent}.
     * @param builder the builder whose fields to populate
     */
    public void initializeBuilder(final Log4jLogEvent.Builder builder) {
        builder.setContextData(contextData) //
                .setContextStack(contextStack) //
                .setEndOfBatch(endOfBatch) //
                .setIncludeLocation(includeLocation) //
                .setLevel(getLevel()) // ensure non-null
                .setLoggerFqcn(fqcn) //
                .setLoggerName(loggerName) //
                .setMarker(marker) //
                .setMessage(getNonNullImmutableMessage()) // ensure non-null & immutable
                .setNanoTime(nanoTime) //
                .setSource(location) //
                .setThreadId(threadId) //
                .setThreadName(threadName) //
                .setThreadPriority(threadPriority) //
                .setThrown(getThrown()) // may deserialize from thrownProxy
                .setThrownProxy(thrownProxy) // avoid unnecessarily creating thrownProxy
                .setTimeMillis(currentTimeMillis);
    }

}
