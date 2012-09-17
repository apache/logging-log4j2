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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.LoggerNameAwareMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.TimestampMessage;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Implementation of a LogEvent.
 */
public class Log4jLogEvent implements LogEvent, Serializable {

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
    private final Stack<String> ndc;
    private String threadName = null;
    private StackTraceElement location;

    /**
     * Constructor.
     * @param loggerName The name of the Logger.
     * @param marker The Marker or null.
     * @param fqcn The fully qualified class name of the caller.
     * @param level The logging Level.
     * @param message The Message.
     * @param t A Throwable or null.
     */
    public Log4jLogEvent(String loggerName, Marker marker, String fqcn, Level level, Message message, Throwable t) {
        this(loggerName, marker, fqcn, level, message, t, ThreadContext.getContext(), ThreadContext.cloneStack(), null,
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
    public Log4jLogEvent(String loggerName, Marker marker, String fqcn, Level level, Message message, Throwable t,
                         Map<String, String> mdc, Stack<String> ndc, String threadName, StackTraceElement location,
                         long timestamp) {
        name = loggerName;
        this.marker = marker;
        this.fqcnOfLogger = fqcn;
        this.level = level;
        this.message = message;
        this.throwable = t == null ? null : new ThrowableProxy(t);
        this.mdc = mdc;
        this.ndc = ndc;
        this.timestamp = message instanceof TimestampMessage ? ((TimestampMessage) message).getTimestamp() : timestamp;
        this.threadName = threadName;
        this.location = location;
        if (message != null && message instanceof LoggerNameAwareMessage) {
            ((LoggerNameAwareMessage) message).setLoggerName(name);
        }
    }

    /**
     * Returns the logging Level.
     * @return the Level associated with this event.
     */
    public Level getLevel() {
        return level;
    }

    /**
     * Return the name of the Logger used to generate the event.
     * @return The Logger name.
     */
    public String getLoggerName() {
        return name;
    }

    /**
     * Return the Message associated with the event.
     * @return The Message.
     */
    public Message getMessage() {
        return message;
    }

    /**
     * Return the name of the Thread on which the event was generated.
     * @return The name of the Thread.
     */
    public String getThreadName() {
        if (threadName == null) {
            threadName = Thread.currentThread().getName();
        }
        return threadName;
    }

    /**
     * Return the time in milliseconds from the epoch when the event occurred.
     * @return The time the event occurred.
     */
    public long getMillis() {
        return timestamp;
    }

    /**
     * Return the Throwable associated with the event, or null.
     * @return The Throwable associated with the event.
     */
    public Throwable getThrown() {
        return throwable;
    }

    /**
     * Return the Marker associated with the event, or null.
     * @return the Marker associated with the event.
     */
    public Marker getMarker() {
        return marker;
    }

    /**
     * The fully qualified class name of the class that was called by the caller.
     * @return the fully qualified class name of the class that is performing logging.
     */
    public String getFQCN() {
        return fqcnOfLogger;
    }

    /**
     * @doubt Allows direct access to the map passed into the constructor, would allow appender
     * or layout to manipulate event as seen by other appenders.
     * @return The context Map.
     */
    public Map<String, String> getContextMap() {
        return mdc;
    }

    /**
     * @doubt Allows direct access to the map passed into the constructor, would allow appender
     * or layout to manipulate event as seen by other appenders.
     * @return The context Stack.
     */
    public Stack<String> getContextStack() {
        return ndc;
    }

    /**
     * Return the StackTraceElement for the caller. This will be the entry that occurs right
     * before the first occurrence of FQCN as a class name.
     * @return the StackTraceElement for the caller.
     */
    public StackTraceElement getSource() {
        if (fqcnOfLogger == null) {
            return null;
        }
        if (location == null) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            boolean next = false;
            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                if (next) {
                    if (fqcnOfLogger.equals(className)) {
                        continue;
                    }
                    location = element;
                    break;
                }

                if (fqcnOfLogger.equals(className)) {
                    next = true;
                } else if (NOT_AVAIL.equals(className)) {
                    break;
                }
            }
        }

        return location;
    }

    /**
     * Creates a LogEventProxy that can be serialized.
     * @return a LogEventProxy.
     */
    protected Object writeReplace() {
        return new LogEventProxy(this);
    }

    public static Serializable serialize(Log4jLogEvent event) {
        return new LogEventProxy(event);
    }

    public static Log4jLogEvent deserialize(Serializable event) {
        if (event == null) {
            throw new NullPointerException("Event cannot be null");
        }
        if (event instanceof LogEventProxy) {
            LogEventProxy proxy = (LogEventProxy) event;
            return new Log4jLogEvent(proxy.name, proxy.marker, proxy.fqcnOfLogger, proxy.level, proxy.message,
                proxy.throwable, proxy.mdc, proxy.ndc, proxy.threadName, proxy.location, proxy.timestamp);
        }
        throw new IllegalArgumentException("Event is not a serialized LogEvent: " + event.toString());
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Proxy required");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String n = name.length() == 0 ? "root" : name;
        sb.append("Logger=").append(n);
        sb.append(" Level=").append(level.name());
        sb.append(" Message").append(message.getFormattedMessage());
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
        private final Throwable throwable;
        private final HashMap<String, String> mdc;
        private final Stack<String> ndc;
        private String threadName;
        private StackTraceElement location;

        public LogEventProxy(Log4jLogEvent event) {
            this.fqcnOfLogger = event.fqcnOfLogger;
            this.marker = event.marker;
            this.level = event.level;
            this.name = event.name;
            this.message = event.message;
            this.timestamp = event.timestamp;
            this.throwable = event.throwable;
            this.mdc = new HashMap<String, String>(event.mdc);
            this.ndc = event.ndc;
            this.location = event.getSource();
            this.threadName = event.getThreadName();
        }

        /**
         * Return a Log4jLogEvent using the data in the proxy.
         * @return Log4jLogEvent.
         */
        protected Object readResolve() {
            return new Log4jLogEvent(name, marker, fqcnOfLogger, level, message, throwable, mdc, ndc, threadName,
                                     location, timestamp);
        }

    }

}
