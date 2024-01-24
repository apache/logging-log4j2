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

import com.lmax.disruptor.EventFactory;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.MementoMessage;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.core.util.Clock;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.NanoClock;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterConsumer;
import org.apache.logging.log4j.message.ParameterVisitable;
import org.apache.logging.log4j.message.ReusableMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.message.TimestampMessage;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.StringBuilders;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.util.Strings;

/**
 * When the Disruptor is started, the RingBuffer is populated with event objects. These objects are then re-used during
 * the life of the RingBuffer.
 */
public class RingBufferLogEvent implements LogEvent, ReusableMessage, CharSequence, ParameterVisitable {

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
            return new RingBufferLogEvent();
        }
    }

    private boolean populated;
    private int threadPriority;
    private long threadId;
    private final MutableInstant instant = new MutableInstant();
    private long nanoTime;
    private short parameterCount;
    private boolean includeLocation;
    private boolean endOfBatch = false;
    private Level level;
    private String threadName;
    private String loggerName;
    private Message message;
    private String messageFormat;
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

    public void setValues(
            final AsyncLogger anAsyncLogger,
            final String aLoggerName,
            final Marker aMarker,
            final String theFqcn,
            final Level aLevel,
            final Message msg,
            final Throwable aThrowable,
            final StringMap mutableContextData,
            final ContextStack aContextStack,
            final long threadId,
            final String threadName,
            final int threadPriority,
            final StackTraceElement aLocation,
            final Clock clock,
            final NanoClock nanoClock) {
        this.threadPriority = threadPriority;
        this.threadId = threadId;
        this.level = aLevel;
        this.threadName = threadName;
        this.loggerName = aLoggerName;
        setMessage(msg);
        initTime(clock);
        this.nanoTime = nanoClock.nanoTime();
        this.thrown = aThrowable;
        this.thrownProxy = null;
        this.marker = aMarker;
        this.fqcn = theFqcn;
        this.location = aLocation;
        this.contextData = mutableContextData;
        this.contextStack = aContextStack;
        this.asyncLogger = anAsyncLogger;
        this.populated = true;
    }

    private void initTime(final Clock clock) {
        if (message instanceof TimestampMessage) {
            instant.initFromEpochMilli(((TimestampMessage) message).getTimestamp(), 0);
        } else {
            instant.initFrom(clock);
        }
    }

    @Override
    public LogEvent toImmutable() {
        return createMemento();
    }

    private void setMessage(final Message msg) {
        if (msg instanceof ReusableMessage) {
            final ReusableMessage reusable = (ReusableMessage) msg;
            reusable.formatTo(getMessageTextForWriting());
            messageFormat = reusable.getFormat();
            parameters = reusable.swapParameters(parameters == null ? new Object[10] : parameters);
            parameterCount = reusable.getParameterCount();
        } else {
            this.message = InternalAsyncUtil.makeMessageImmutable(msg);
        }
    }

    private StringBuilder getMessageTextForWriting() {
        if (messageText == null) {
            // Happens the first time messageText is requested or if a user logs
            // a custom reused message when Constants.ENABLE_THREADLOCALS is false
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
     * @return {@code true} if this event is populated with data, {@code false} otherwise
     */
    public boolean isPopulated() {
        return populated;
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
        return messageFormat;
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
    public <S> void forEachParameter(final ParameterConsumer<S> action, final S state) {
        if (parameters != null) {
            for (short i = 0; i < parameterCount; i++) {
                action.accept(parameters[i], i, state);
            }
        }
    }

    @Override
    public Message memento() {
        if (message == null) {
            message = new MementoMessage(String.valueOf(messageText), messageFormat, getParameters());
        }
        return message;
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

    @Override
    public ReadOnlyStringMap getContextData() {
        return contextData;
    }

    void setContextData(final StringMap contextData) {
        this.contextData = contextData;
    }

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
        return message instanceof TimestampMessage
                ? ((TimestampMessage) message).getTimestamp()
                : instant.getEpochMillisecond();
    }

    @Override
    public Instant getInstant() {
        return instant;
    }

    @Override
    public long getNanoTime() {
        return nanoTime;
    }

    /**
     * Release references held by ring buffer to allow objects to be garbage-collected.
     */
    public void clear() {
        this.populated = false;
        this.level = null;
        this.threadName = null;
        this.loggerName = null;
        clearMessage();
        this.thrown = null;
        this.thrownProxy = null;
        clearContextData();
        this.marker = null;
        this.fqcn = null;
        this.location = null;
        this.contextStack = null;
        this.asyncLogger = null;
    }

    private void clearMessage() {
        message = null;
        messageFormat = null;
        // ensure that excessively long char[] arrays are not kept in memory forever
        if (Constants.ENABLE_THREADLOCALS) {
            StringBuilders.trimToMaxSize(messageText, Constants.MAX_REUSABLE_MESSAGE_SIZE);

            if (parameters != null) {
                Arrays.fill(parameters, null);
            }
        } else {
            // A user may have manually logged a ReusableMessage implementation, when thread locals are
            // disabled we remove the reference in order to avoid permanently holding references to these
            // buffers.
            messageText = null;
            parameters = null;
        }
    }

    private void clearContextData() {
        if (contextData != null) {
            if (contextData.isFrozen()) { // came from CopyOnWrite thread context
                contextData = null;
            } else {
                contextData.clear();
            }
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
        final Log4jLogEvent.Builder builder = new Log4jLogEvent.Builder();
        initializeBuilder(builder);
        return builder.build();
    }

    /**
     * Initializes the specified {@code Log4jLogEvent.Builder} from this {@code RingBufferLogEvent}.
     * @param builder the builder whose fields to populate
     */
    public void initializeBuilder(final Log4jLogEvent.Builder builder) {
        // If the data is not frozen, make a copy of it.
        final StringMap oldContextData = this.contextData;
        final StringMap contextData;
        if (oldContextData != null && !oldContextData.isFrozen()) {
            contextData = ContextDataFactory.createContextData();
            contextData.putAll(oldContextData);
        } else {
            contextData = oldContextData;
        }
        builder.setContextData(contextData) //
                .setContextStack(contextStack) //
                .setEndOfBatch(endOfBatch) //
                .setIncludeLocation(includeLocation) //
                .setLevel(getLevel()) // ensure non-null
                .setLoggerFqcn(fqcn) //
                .setLoggerName(loggerName) //
                .setMarker(marker) //
                .setMessage(memento()) // ensure non-null & immutable
                .setNanoTime(nanoTime) //
                .setSource(location) //
                .setThreadId(threadId) //
                .setThreadName(threadName) //
                .setThreadPriority(threadPriority) //
                .setThrown(getThrown()) // may deserialize from thrownProxy
                .setThrownProxy(thrownProxy) // avoid unnecessarily creating thrownProxy
                .setInstant(instant) //
        ;
    }
}
