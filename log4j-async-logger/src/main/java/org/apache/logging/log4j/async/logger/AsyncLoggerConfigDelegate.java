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
package org.apache.logging.log4j.async.logger;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.async.EventRoute;

/**
 * Encapsulates the mechanism used to log asynchronously. There is one delegate per configuration, which is shared by
 * all AsyncLoggerConfig objects in the configuration.
 */
public interface AsyncLoggerConfigDelegate {

    /**
     * Returns the {@code EventRoute} for the event with the specified level.
     *
     * @param level the level of the event to log
     * @return the {@code EventRoute}
     */
    EventRoute getEventRoute(final Level level);

    /**
     * Enqueues the {@link LogEvent} on the mixed configuration ringbuffer.
     * This method must only be used after {@link #tryEnqueue(LogEvent, AsyncLoggerConfig)} returns <code>false</code>
     * indicating that the ringbuffer is full, otherwise it may incur unnecessary synchronization.
     */
    void enqueueEvent(LogEvent event, AsyncLoggerConfig asyncLoggerConfig);

    boolean tryEnqueue(LogEvent event, AsyncLoggerConfig asyncLoggerConfig);
}
