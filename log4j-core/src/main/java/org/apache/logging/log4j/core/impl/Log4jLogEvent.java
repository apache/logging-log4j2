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
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.message.LoggerNameAwareMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.TimestampMessage;

/**
 * Implementation of a LogEvent.
 */
public class Log4jLogEvent implements LogEvent {

    private static final long serialVersionUID = -1351367343806656055L;
    private static final String NOT_AVAIL = "?";
    private final String fqcnOfLogger;
    private final Marker marker;
    private final Level level;
    private final String name;
    private final Message message;
    private final long timestamp;
    private final ThrowableProxy throwable;
    private final Map<String, String> mdc;
    private final ThreadContext.ContextStack ndc;
    private String threadName = null;
    private StackTraceElement location;
    private boolean includeLocation;
    private boolean endOfBatch = false;

    /**
     *
     */
    public Log4jLogEvent(final long timestamp) {
        this("", null, "", null, null, (ThrowableProxy) null, null, null, null, null, timestamp);
    }

    /**
     * Constructor.
     * @param loggerName The name of the Logger.
     * @param marker The Marker or null.
     * @param fqcn The fully qualified class name of the caller.
     * @param level The logging Level.
     * @param message The Message.
     * @param t A Throwable or null.
     */
    public Log4jLogEvent(final String loggerName, final Marker marker, final String fqcn, final Level level,
                         final Message message, final Throwable t) {
        this(loggerName, marker, fqcn, level, message, null, t);
    }

    /**
     * Constructor.
     * @param loggerName The name of the Logger.
     * @param marker The Marker or null.
     * @param fqcn The fully qualified class name of the caller.
     * @param level The logging Level.
     * @param message The Message.
     * @param properties properties to add to the event.
     * @param t A Throwable or null.
     */
    public Log4jLogEvent(final String loggerName, final Marker marker, final String fqcn, final Level level,
                         final Message message, final List<Property> properties, final Throwable t) {
        this(loggerName, marker, fqcn, level, message, t,
            createMap(properties),
            ThreadContext.getDepth() == 0 ? null : ThreadContext.cloneStack(), null,
            null, System.currentTimeMillis());
    }

    /**
     * Constructor.
     * @param loggerName The name of the Logger.
     * @param marker The Marker or null.
     * @param fqcn The fully qualified class name of the caller.
     * @param level The logging Level.
     * @param message The Message.
     * @param t A Throwable or null.
     * @param mdc The mapped diagnostic context.
     * @param ndc the nested diagnostic context.
     * @param threadName The name of the thread.
     * @param location The locations of the caller.
     * @param timestamp The timestamp of the event.
     */
    public Log4jLogEvent(final String loggerName, final Marker marker, final String fqcn, final Level level,
                         final Message message, final Throwable t,
                         final Map<String, String> mdc, final ThreadContext.ContextStack ndc, final String threadName,
                         final StackTraceElement location, final long timestamp) {
        this(loggerName, marker, fqcn, level, message, t == null ? null : new ThrowableProxy(t), mdc, ndc, threadName,
            location, timestamp);
    }

    /**
     * Create a new LogEvent.
     * @param loggerName The name of the Logger.
     * @param marker The Marker or null.
     * @param fqcn The fully qualified class name of the caller.
     * @param level The logging Level.
     * @param message The Message.
     * @param t A ThrowableProxy or null.
     * @param mdc The mapped diagnostic context.
     * @param ndc the nested diagnostic context.
     * @param threadName The name of the thread.
     * @param location The locations of the caller.
     * @param timestamp The timestamp of the event.
     */
    public static Log4jLogEvent createEvent(final String loggerName, final Marker marker, final String fqcn,
                                            final Level level, final Message message, final ThrowableProxy t,
                                            final Map<String, String> mdc, final ThreadContext.ContextStack ndc,
                                            final String threadName, final StackTraceElement location,
                                            final long timestamp) {
     return new Log4jLogEvent(loggerName, marker, fqcn, level, message, t, mdc, ndc, threadName, location, timestamp);
    }

    /**
     * Constructor.
     * @param loggerName The name of the Logger.
     * @param marker The Marker or null.
     * @param fqcn The fully qualified class name of the caller.
     * @param level The logging Level.
     * @param message The Message.
     * @param t A ThrowableProxy or null.
     * @param mdc The mapped diagnostic context.
     * @param ndc the nested diagnostic context.
     * @param threadName The name of the thread.
     * @param location The locations of the caller.
     * @param timestamp The timestamp of the event.
     */
    private Log4jLogEvent(final String loggerName, final Marker marker, final String fqcn, final Level level,
                         final Message message, final ThrowableProxy t,
                         final Map<String, String> mdc, final ThreadContext.ContextStack ndc, final String threadName,
                         final StackTraceElement location, final long timestamp) {
        name = loggerName;
        this.marker = marker;
        this.fqcnOfLogger = fqcn;
        this.level = level;
        this.message = message;
        this.throwable = t;
        this.mdc = mdc;
        this.ndc = ndc;
        this.timestamp = message instanceof TimestampMessage ? ((TimestampMessage) message).getTimestamp() : timestamp;
        this.threadName = threadName;
        this.location = location;
        if (message != null && message instanceof LoggerNameAwareMessage) {
            ((LoggerNameAwareMessage) message).setLoggerName(name);
        }
    }

    private static Map<String, String> createMap(final List<Property> properties) {
        final Map<String, String> contextMap = ThreadContext.getImmutableContext();
        if (contextMap == null && (properties == null || properties.size() == 0)) {
            return null;
        }
        if (properties == null || properties.size() == 0) {
            return contextMap; // contextMap is not null
        }
        final Map<String, String> map = new HashMap<String, String>(contextMap);

        for (final Property prop : properties) {
            if (!map.containsKey(prop.getName())) {
                map.put(prop.getName(), prop.getValue());
            }
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * Returns the logging Level.
     * @return the Level associated with this event.
     */
    @Override
    public Level getLevel() {
        return level;
    }

    /**
     * Returns the name of the Logger used to generate the event.
     * @return The Logger name.
     */
    @Override
    public String getLoggerName() {
        return name;
    }

    /**
     * Returns the Message associated with the event.
     * @return The Message.
     */
    @Override
    public Message getMessage() {
        return message;
    }

    /**
     * Returns the name of the Thread on which the event was generated.
     * @return The name of the Thread.
     */
    @Override
    public String getThreadName() {
        if (threadName == null) {
            threadName = Thread.currentThread().getName();
        }
        return threadName;
    }

    /**
     * Returns the time in milliseconds from the epoch when the event occurred.
     * @return The time the event occurred.
     */
    @Override
    public long getMillis() {
        return timestamp;
    }

    /**
     * Returns the Throwable associated with the event, or null.
     * @return The Throwable associated with the event.
     */
    @Override
    public Throwable getThrown() {
        return throwable == null ? null : throwable.getThrowable();
    }

    /**
     * Returns the ThrowableProxy associated with the event, or null.
     * @return The ThrowableProxy associated with the event.
     */
    public ThrowableProxy getThrownProxy() {
        return throwable;
    }


    /**
     * Returns the Marker associated with the event, or null.
     * @return the Marker associated with the event.
     */
    @Override
    public Marker getMarker() {
        return marker;
    }

    /**
     * The fully qualified class name of the class that was called by the caller.
     * @return the fully qualified class name of the class that is performing logging.
     */
    @Override
    public String getFQCN() {
        return fqcnOfLogger;
    }

    /**
     * Returns the immutable copy of the ThreadContext Map.
     * @return The context Map.
     */
    @Override
    public Map<String, String> getContextMap() {
        return mdc == null ? ThreadContext.EMPTY_MAP : mdc;
    }

    /**
     * Returns an immutable copy of the ThreadContext stack.
     * @return The context Stack.
     */
    @Override
    public ThreadContext.ContextStack getContextStack() {
        return ndc == null ? ThreadContext.EMPTY_STACK : ndc;
    }

    /**
     * Returns the StackTraceElement for the caller. This will be the entry that occurs right
     * before the first occurrence of FQCN as a class name.
     * @return the StackTraceElement for the caller.
     */
    @Override
    public StackTraceElement getSource() {
        if (location != null) {
            return location;
        }
        if (fqcnOfLogger == null || !includeLocation) {
            return null;
        }
        location = calcLocation(fqcnOfLogger);
        return location;
    }

    public static StackTraceElement calcLocation(final String fqcnOfLogger) {
        if (fqcnOfLogger == null) {
            return null;
        }
        final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        boolean next = false;
        for (final StackTraceElement element : stackTrace) {
            final String className = element.getClassName();
            if (next) {
                if (fqcnOfLogger.equals(className)) {
                    continue;
                }
                return element;
            }

            if (fqcnOfLogger.equals(className)) {
                next = true;
            } else if (NOT_AVAIL.equals(className)) {
                break;
            }
        }
        return null;
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

    /**
     * Creates a LogEventProxy that can be serialized.
     * @return a LogEventProxy.
     */
    protected Object writeReplace() {
        return new LogEventProxy(this, this.includeLocation);
    }

    public static Serializable serialize(final Log4jLogEvent event,
            final boolean includeLocation) {
        return new LogEventProxy(event, includeLocation);
    }

    public static Log4jLogEvent deserialize(final Serializable event) {
        if (event == null) {
            throw new NullPointerException("Event cannot be null");
        }
        if (event instanceof LogEventProxy) {
            final LogEventProxy proxy = (LogEventProxy) event;
            final Log4jLogEvent result = new Log4jLogEvent(proxy.name, proxy.marker,
                    proxy.fqcnOfLogger, proxy.level, proxy.message,
                    proxy.throwable, proxy.mdc, proxy.ndc, proxy.threadName,
                    proxy.location, proxy.timestamp);
            result.setEndOfBatch(proxy.isEndOfBatch);
            result.setIncludeLocation(proxy.isLocationRequired);
            return result;
        }
        throw new IllegalArgumentException("Event is not a serialized LogEvent: " + event.toString());
    }

    private void readObject(final ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Proxy required");
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        final String n = name.isEmpty() ? "root" : name;
        sb.append("Logger=").append(n);
        sb.append(" Level=").append(level.name());
        sb.append(" Message=").append(message.getFormattedMessage());
        return sb.toString();
    }

    /**
     * Proxy pattern used to serialize the LogEvent.
     */
    private static class LogEventProxy implements Serializable {

        private static final long serialVersionUID = -7139032940312647146L;
        private final String fqcnOfLogger;
        private final Marker marker;
        private final Level level;
        private final String name;
        private final Message message;
        private final long timestamp;
        private final ThrowableProxy throwable;
        private final Map<String, String> mdc;
        private final ThreadContext.ContextStack ndc;
        private final String threadName;
        private final StackTraceElement location;
        private final boolean isLocationRequired;
        private final boolean isEndOfBatch;

        public LogEventProxy(final Log4jLogEvent event, final boolean includeLocation) {
            this.fqcnOfLogger = event.fqcnOfLogger;
            this.marker = event.marker;
            this.level = event.level;
            this.name = event.name;
            this.message = event.message;
            this.timestamp = event.timestamp;
            this.throwable = event.throwable;
            this.mdc = event.mdc;
            this.ndc = event.ndc;
            this.location = includeLocation ? event.getSource() : null;
            this.threadName = event.getThreadName();
            this.isLocationRequired = includeLocation;
            this.isEndOfBatch = event.endOfBatch;
        }

        /**
         * Returns a Log4jLogEvent using the data in the proxy.
         * @return Log4jLogEvent.
         */
        protected Object readResolve() {
            final Log4jLogEvent result = new Log4jLogEvent(name, marker, fqcnOfLogger,
                    level, message, throwable, mdc, ndc, threadName, location,
                    timestamp);
            result.setEndOfBatch(isEndOfBatch);
            result.setIncludeLocation(isLocationRequired);
            return result;
        }
    }
}
