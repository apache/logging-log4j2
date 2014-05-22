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

import java.io.Serializable;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.message.Message;

/**
 *
 */
public interface LogEvent extends Serializable {

    /**
     * Gets the context map (also know as Mapped Diagnostic Context or MDC).
     * 
     * @return The context map, never {@code null}.
     */
    Map<String, String> getContextMap();

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
     * @return logger name, may be null.
     */
    String getLoggerName();

    /**
     * Gets the Marker associated with the event.
     * 
     * @return Marker
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
     * 
     * @return milliseconds since midnight, January 1, 1970 UTC.
     * @see java.lang.System#currentTimeMillis()
     */
    long getTimeMillis();

    /**
     * Gets the source of logging request.
     * 
     * @return source of logging request, may be null.
     */
    StackTraceElement getSource();

    /**
     * Gets thread name.
     * 
     * @return thread name, may be null.
     * @doubt guess this could go into a thread context object too. (RG) Why?
     */
    String getThreadName();

    /**
     * Gets throwable associated with logging request.
     * <p>
     * Convenience method for {@code ThrowableProxy.getThrowable();}
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
     * Returns {@code true} if this event is the last one in a batch, {@code false} otherwise. Used by asynchronous Loggers and Appenders to
     * signal to buffered downstream components when to flush to disk, as a more efficient alternative to the {@code immediateFlush=true}
     * configuration.
     * 
     * @return whether this event is the last one in a batch.
     */
    // see also LOG4J2-164
    boolean isEndOfBatch();

    /**
     * Returns whether the source of the logging request is required downstream. Asynchronous Loggers and Appenders use this flag to
     * determine whether to take a {@code StackTrace} snapshot or not before handing off this event to another thread.
     * 
     * @return {@code true} if the source of the logging request is required downstream, {@code false} otherwise.
     * @see #getSource()
     */
    // see also LOG4J2-153
    boolean isIncludeLocation();

    /**
     * Sets whether this event is the last one in a batch. Used by asynchronous Loggers and Appenders to signal to buffered downstream
     * components when to flush to disk, as a more efficient alternative to the {@code immediateFlush=true} configuration.
     * 
     * @param endOfBatch {@code true} if this event is the last one in a batch, {@code false} otherwise.
     */
    void setEndOfBatch(boolean endOfBatch);

    /**
     * Sets whether the source of the logging request is required downstream. Asynchronous Loggers and Appenders use this flag to determine
     * whether to take a {@code StackTrace} snapshot or not before handing off this event to another thread.
     * 
     * @param locationRequired {@code true} if the source of the logging request is required downstream, {@code false} otherwise.
     * @see #getSource()
     */
    void setIncludeLocation(boolean locationRequired);

}
