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
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ReusableMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.Strings;

import com.lmax.disruptor.EventFactory;

/**
 * When the Disruptor is started, the RingBuffer is populated with event objects. These objects are then re-used during
 * the life of the RingBuffer.
 */
public class RingBufferLogEvent implements LogEvent {

    /** The {@code EventFactory} for {@code RingBufferLogEvent}s. */
    public static final Factory FACTORY = new Factory();

    private static final long serialVersionUID = 8462119088943934758L;

    /**
     * Creates the events that will be put in the RingBuffer.
     */
    private static class Factory implements EventFactory<RingBufferLogEvent> {

        @Override
        public RingBufferLogEvent newInstance() {
            RingBufferLogEvent result = new RingBufferLogEvent();
            if (Constants.ENABLE_THREADLOCALS) {
                result.messageText = new StringBuilder(128);
            }
            return result;
        }
    }

    private static class StringBuilderWrapperMessage implements ReusableMessage {
        static final StringBuilderWrapperMessage INSTANCE = new StringBuilderWrapperMessage();
        private StringBuilder stringBuilder;

        @Override
        public boolean isReused() {
            return true;
        }

        @Override
        public String getFormattedMessage() {
            return null;
        }

        @Override
        public String getFormat() {
            return null;
        }

        @Override
        public Object[] getParameters() {
            return new Object[0];
        }

        @Override
        public Throwable getThrowable() {
            return null;
        }

        @Override
        public void formatTo(final StringBuilder buffer) {
            buffer.append(stringBuilder);

            // ensure that excessively long char[] arrays are not kept in memory forever
            if (stringBuilder.length() > 512) {
                stringBuilder.setLength(512);
                stringBuilder.trimToSize();
            }
        }

        public Message setFormattedMessage(final StringBuilder messageText) {
            this.stringBuilder = messageText;
            return this;
        }
    }

    private transient AsyncLogger asyncLogger;
    private String loggerName;
    private Marker marker;
    private String fqcn;
    private Level level;
    private StringBuilder messageText;
    private Message message;
    private transient Throwable thrown;
    private ThrowableProxy thrownProxy;
    private Map<String, String> contextMap;
    private ContextStack contextStack;
    private String threadName;
    private StackTraceElement location;
    private long currentTimeMillis;
    private boolean endOfBatch;
    private boolean includeLocation;
    private long nanoTime;

    public void setValues(final AsyncLogger anAsyncLogger, final String aLoggerName, final Marker aMarker,
            final String theFqcn, final Level aLevel, final Message msg, final Throwable aThrowable,
            final Map<String, String> aMap, final ContextStack aContextStack, final String aThreadName,
            final StackTraceElement aLocation, final long aCurrentTimeMillis, final long aNanoTime) {
        this.asyncLogger = anAsyncLogger;
        this.loggerName = aLoggerName;
        this.marker = aMarker;
        this.fqcn = theFqcn;
        this.level = aLevel;
        if (msg instanceof ReusableMessage && ((ReusableMessage) msg).isReused()) {
            if (messageText == null) {
                // Should never happen:
                // only happens if user logs a custom reused message when Constants.ENABLE_THREADLOCALS is false
                messageText = new StringBuilder(128);
            }
            ((ReusableMessage) msg).formatTo(messageText);
        } else {
            this.message = msg;
        }
        this.thrown = aThrowable;
        this.thrownProxy = null;
        this.contextMap = aMap;
        this.contextStack = aContextStack;
        this.threadName = aThreadName;
        this.location = aLocation;
        this.currentTimeMillis = aCurrentTimeMillis;
        this.nanoTime = aNanoTime;
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
            if (messageText == null) {
                message = new SimpleMessage(Strings.EMPTY);
            } else {
                message = StringBuilderWrapperMessage.INSTANCE.setFormattedMessage(messageText);
            }
        }
        return message;
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
    public Map<String, String> getContextMap() {
        return contextMap;
    }

    @Override
    public ContextStack getContextStack() {
        return contextStack;
    }

    @Override
    public String getThreadName() {
        return threadName;
    }

    @Override
    public StackTraceElement getSource() {
        return location;
    }

    @Override
    public long getTimeMillis() {
        return currentTimeMillis;
    }

    @Override
    public long getNanoTime() {
        return nanoTime;
    }

    /**
     * Merges the contents of the specified map into the contextMap, after replacing any variables in the property
     * values with the StrSubstitutor-supplied actual values.
     *
     * @param properties configured properties
     * @param strSubstitutor used to lookup values of variables in properties
     */
    public void mergePropertiesIntoContextMap(final Map<Property, Boolean> properties,
            final StrSubstitutor strSubstitutor) {
        if (properties == null) {
            return; // nothing to do
        }

        final Map<String, String> map = contextMap == null ? new HashMap<String, String>()
                : new HashMap<>(contextMap);

        for (final Map.Entry<Property, Boolean> entry : properties.entrySet()) {
            final Property prop = entry.getKey();
            if (map.containsKey(prop.getName())) {
                continue; // contextMap overrides config properties
            }
            final String value = entry.getValue().booleanValue() ? strSubstitutor.replace(prop.getValue()) : prop
                    .getValue();
            map.put(prop.getName(), value);
        }
        contextMap = map;
    }

    /**
     * Release references held by ring buffer to allow objects to be garbage-collected.
     */
    public void clear() {
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
        final LogEvent result = new Log4jLogEvent.Builder(this).build();
        return result;
    }

    /**
     * Initializes the specified {@code Log4jLogEvent.Builder} from this {@code RingBufferLogEvent}.
     * @param builder the builder whose fields to populate
     */
    public void initializeBuilder(Log4jLogEvent.Builder builder) {
        builder.setContextMap(contextMap) //
                .setContextStack(contextStack) //
                .setEndOfBatch(endOfBatch) //
                .setIncludeLocation(includeLocation) //
                .setLevel(getLevel()) // ensure non-null
                .setLoggerFqcn(fqcn) //
                .setLoggerName(loggerName) //
                .setMarker(marker) //
                .setMessage(getMessage()) // ensure non-null
                .setNanoTime(nanoTime) //
                .setSource(location) //
                .setThreadName(threadName) //
                .setThrown(getThrown()) // may deserialize from thrownProxy
                .setThrownProxy(thrownProxy) // avoid unnecessarily creating thrownProxy
                .setTimeMillis(currentTimeMillis);
    }
}
