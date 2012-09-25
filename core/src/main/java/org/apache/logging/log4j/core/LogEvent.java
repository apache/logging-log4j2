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
import org.apache.logging.log4j.message.Message;

import java.io.Serializable;
import java.util.Map;

/**
 *
 */
public interface LogEvent extends Serializable {

     /**
     * Get level.
     * @return level.
     */
    Level getLevel();

    /**
     * Get logger name.
     * @return logger name, may be null.
     */
    String getLoggerName();

    /**
     * Get source of logging request.
     * @return source of logging request, may be null.
     */
    StackTraceElement getSource();

    /**
     * Get the message associated with the event.
     *
     * @return message.
     */
    Message getMessage();

    /**
     * Get the Marker associated with the event.
     * @return Marker
     */
    Marker getMarker();

    /**
     * Get thread name.
     * @return thread name, may be null.
     * @doubt guess this could go into a thread context object too.
     * (RG) Why?
     */
    String getThreadName();


    /**
     * Get event time in milliseconds since 1970.
     * @return milliseconds since 1970.
     */
    long getMillis();


    /**
     * Get throwable associated with logging request.
     * @return throwable, may be null.
     */
    Throwable getThrown();


    /**
     * Get the MDC data.
     *
     * @return A copy of the Mapped Diagnostic Context or null.
     */
    Map<String, String> getContextMap();

    /**
     * Get the NDC data.
     *
     * @return A copy of the Nested Diagnostic Context or null;
     */
    ThreadContext.ContextStack getContextStack();

    /**
     * Returns the fully qualified class name of the caller of the logging api.
     * @return The fully qualified class name of the caller.
     */
    String getFQCN();

}
