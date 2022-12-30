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
package org.apache.logging.log4j.core;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.impl.LogEventBuilder;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

/**
 * Provides the context in which a message was emitted by a logger. A LogEvent contains a
 * {@linkplain #getMessage() message}, {@linkplain #getLevel() level}, optional {@linkplain #getMarker() marker},
 * optional {@linkplain #getThrown() exception}, and the contents of the {@link ThreadContext}
 * {@linkplain #getContextData() map} and {@linkplain #getContextStack() stack}.
 *
 * <h3>Serializable Deprecation</h3>
 * <p>In Log4j 2.x, this interface implemented {@code Serializable}. In Log4j 3.x, this is no longer used.
 * See <a href="https://issues.apache.org/jira/browse/LOG4J2-3228">LOG4J2-3228</a>.</p>
 */
public interface LogEvent {

    /**
     * Creates a new LogEventBuilder.
     *
     * @since 3.0.0
     */
    static LogEventBuilder builder() {
        return new LogEventBuilder();
    }

    /**
     * Creates a new LogEventBuilder using an existing LogEvent.
     *
     * @param other existing log event to copy data from
     * @return new LogEventBuilder with copied data
     * @since 3.0.0
     */
    static LogEventBuilder builderFrom(final LogEvent other) {
        return new LogEventBuilder().copyFrom(other);
    }

    /**
     * Returns an immutable copy of this log event.
     *
     * @return copy of this
     * @since 3.0.0
     */
    default LogEvent copy() {
        return builderFrom(this).get();
    }

    /**
     * Returns an immutable copy of this log event.
     *
     * @param includeLocation whether to calculate the caller location
     * @return copy of this
     * @since 3.0.0
     */
    default LogEvent copy(final boolean includeLocation) {
        return builderFrom(this).includeLocation(includeLocation).get();
    }

    /**
     * Returns an immutable version of this log event, which MAY BE a copy of this event.
     *
     * @return an immutable version of this log event
     */
    LogEvent toImmutable();

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
     * Gets the source of logging request.
     *
     * @return source of logging request, may be null.
     */
    StackTraceElement getSource();

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
     * <p>Convenience method for {@code ThrowableProxy.getThrowable();}</p>
     *
     * @return throwable, may be null.
     */
    Throwable getThrown();

    /**
     * Gets throwable proxy associated with logging request.
     *
     * @return throwable, may be null.
     */
    ThrowableProxy getThrownProxy();

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
