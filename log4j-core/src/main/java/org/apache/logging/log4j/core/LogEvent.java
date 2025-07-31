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
package org.apache.logging.log4j.core;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ReusableMessage;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.jspecify.annotations.Nullable;

/**
 * Provides contextual information about a logged message. Besides containing a
 * {@link org.apache.logging.log4j.message.Message}, a LogEvent has a corresponding
 * {@link org.apache.logging.log4j.Level} that the message was logged at. If a
 * {@link org.apache.logging.log4j.Marker} was used, then it is included here. The contents of the
 * {@link org.apache.logging.log4j.ThreadContext} at the time of the log call are provided via
 * {@link #getContextData()} and {@link #getContextStack()}. If a {@link java.lang.Throwable} was included in the log
 * call, then it is provided via {@link #getThrown()}.
 */
public interface LogEvent {

    /**
     * Returns an immutable version of this log event, which MAY BE a copy of this event.
     *
     * @return an immutable version of this log event
     * @since 2.8.1
     */
    LogEvent toImmutable();

    /**
     * Returns a copy of this log event.
     * <p>
     *     Location information for both events might be computed by this method.
     * </p>
     * <p>
     *   <strong>Warning:</strong> If {@code event.getMessage()} is an instance of {@link ReusableMessage}, this method
     *   remove the parameter references from the original message. Callers should:
     * </p>
     * <ol>
     *   <li>Either make sure that the {@code event} will not be used again.</li>
     *   <li>Or call {@link LogEvent#toImmutable()} before calling this method.</li>
     * </ol>
     * @since 3.0.0
     */
    default LogEvent toMemento() {
        return new Log4jLogEvent.Builder(this).build();
    }

    /**
     * Returns the {@code ReadOnlyStringMap} object holding context data key-value pairs.
     * <p>
     * Context data (also known as Mapped Diagnostic Context or MDC) is data that is set by the application to be
     * included in all subsequent log events. The default source for context data is the {@link ThreadContext} (and
     * <a href="https://logging.apache.org/log4j/2.x/manual/configuration.html#PropertySubstitution">properties</a>
     * configured on the Logger that logged the event), but users can configure a custom {@link ContextDataInjector}
     * to inject key-value pairs from any arbitrary source.
     *
     * @return the {@code ReadOnlyStringMap} object holding context data key-value pairs
     * @see ContextDataInjector
     * @see ThreadContext
     * @since 2.7
     */
    ReadOnlyStringMap getContextData();

    /**
     * Gets the context stack (also known as Nested Diagnostic Context or NDC).
     *
     * @return The context stack, never {@code null}.
     */
    ThreadContext.ContextStack getContextStack();

    /**
     * Returns the fully qualified class name of the caller of the logging API.
     *
     * @return The fully qualified class name of the caller.
     */
    String getLoggerFqcn();

    /**
     * Gets the level.
     *
     * @return level.
     */
    Level getLevel();

    /**
     * Gets the logger name.
     *
     * @return logger name, may be {@code null}.
     */
    String getLoggerName();

    /**
     * Gets the Marker associated with the event.
     *
     * @return Marker or {@code null} if no Marker was defined on this LogEvent
     */
    Marker getMarker();

    /**
     * Gets the message associated with the event.
     *
     * @return message.
     */
    Message getMessage();

    /**
     * Gets event time in milliseconds since midnight, January 1, 1970 UTC.
     * Use {@link #getInstant()} to get higher precision timestamp information if available on this platform.
     *
     * @return the milliseconds component of this log event's {@linkplain #getInstant() timestamp}
     * @see java.lang.System#currentTimeMillis()
     */
    long getTimeMillis();

    /**
     * Returns the Instant when the message was logged.
     * <p>
     * <b>Caution</b>: if this {@code LogEvent} implementation is mutable and reused for multiple consecutive log messages,
     * then the {@code Instant} object returned by this method is also mutable and reused.
     * Client code should not keep a reference to the returned object but make a copy instead.
     * </p>
     *
     * @return the {@code Instant} holding Instant details for this log event
     * @since 2.11.0
     */
    Instant getInstant();

    /**
     * Gets the source of the logging call or computes it.
     * <p>
     *     The implementation must be idempotent: multiple calls to this method must always return the same value.
     * </p>
     * @return source of logging request, may be {@code null} if {@link #isIncludeLocation()} is {@code false}.
     */
    StackTraceElement getSource();

    /**
     * Gets the source of the logging call.
     * <p>
     *     This method must be side effect free.
     * </p>
     * @return source of logging request or {@code  null} if currently unknown.
     */
    @Nullable
    StackTraceElement peekSource();

    /**
     * Gets the thread name.
     *
     * @return thread name, may be null.
     */
    String getThreadName();

    /**
     * Gets the thread ID.
     *
     * @return thread ID.
     * @since 2.6
     */
    long getThreadId();

    /**
     * Gets the thread priority.
     *
     * @return thread priority.
     * @since 2.6
     */
    int getThreadPriority();

    /**
     * Gets throwable associated with logging request.
     *
     * @return throwable, may be null.
     */
    Throwable getThrown();

    /**
     * Returns {@code true} if this event is the last one in a batch, {@code false} otherwise. Used by asynchronous
     * Loggers and Appenders to signal to buffered downstream components when to flush to disk, as a more efficient
     * alternative to the {@code immediateFlush=true} configuration.
     *
     * @return whether this event is the last one in a batch.
     */
    // see also LOG4J2-164
    boolean isEndOfBatch();

    /**
     * Returns whether the source of the logging request is required downstream. Asynchronous Loggers and Appenders use
     * this flag to determine whether to take a {@code StackTrace} snapshot or not before handing off this event to
     * another thread.
     *
     * @return {@code true} if the source of the logging request is required downstream, {@code false} otherwise.
     * @see #getSource()
     */
    // see also LOG4J2-153
    boolean isIncludeLocation();

    /**
     * Sets whether this event is the last one in a batch. Used by asynchronous Loggers and Appenders to signal to
     * buffered downstream components when to flush to disk, as a more efficient alternative to the
     * {@code immediateFlush=true} configuration.
     *
     * @param endOfBatch {@code true} if this event is the last one in a batch, {@code false} otherwise.
     */
    void setEndOfBatch(boolean endOfBatch);

    /**
     * Sets whether the source of the logging request is required downstream. Asynchronous Loggers and Appenders use
     * this flag to determine whether to take a {@code StackTrace} snapshot or not before handing off this event to
     * another thread.
     *
     * @param locationRequired {@code true} if the source of the logging request is required downstream, {@code false}
     *                         otherwise.
     * @see #getSource()
     */
    void setIncludeLocation(boolean locationRequired);

    /**
     * Returns the value of the running Java Virtual Machine's high-resolution time source when this event was created,
     * or a dummy value if it is known that this value will not be used downstream.
     * @return The value of the running Java Virtual Machine's high-resolution time source when this event was created.
     * @since Log4J 2.4
     */
    long getNanoTime();
}
