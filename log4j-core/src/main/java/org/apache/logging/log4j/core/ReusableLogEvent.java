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
import org.apache.logging.log4j.util.StringMap;

public interface ReusableLogEvent extends LogEvent {
    /**
     * Clears all references this event has to other objects.
     */
    void clear();

    void setLoggerFqcn(final String loggerFqcn);

    void setMarker(final Marker marker);

    void setLevel(final Level level);

    void setLoggerName(final String loggerName);

    void setMessage(final Message message);

    void setThrown(final Throwable thrown);

    void setTimeMillis(final long timeMillis);

    void setInstant(final Instant instant);

    void setSource(final StackTraceElement source);

    @Override
    StringMap getContextData();

    void setContextData(final StringMap mutableContextData);

    void setContextStack(final ThreadContext.ContextStack contextStack);

    void setThreadId(final long threadId);

    void setThreadName(final String threadName);

    void setThreadPriority(final int threadPriority);

    void setNanoTime(final long nanoTime);

    /**
     * Initializes the specified {@code Log4jLogEvent.Builder} from this {@code ReusableLogEvent}.
     * @param builder the builder whose fields to populate
     */
    void initializeBuilder(final Log4jLogEvent.Builder builder);
}
