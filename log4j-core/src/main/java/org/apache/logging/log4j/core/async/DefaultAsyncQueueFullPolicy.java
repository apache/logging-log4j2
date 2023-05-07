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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.util.Log4jThread;

/**
 * Default router: enqueue the event for asynchronous logging in the background thread, unless the current thread is the
 * background thread and the queue is full (enqueueing would cause a deadlock). In that case send the event directly to
 * the appender (in the current thread).
 */
public class DefaultAsyncQueueFullPolicy implements AsyncQueueFullPolicy {
    @Override
    public EventRoute getRoute(final long backgroundThreadId, final Level level) {

        // LOG4J2-471: prevent deadlock when RingBuffer is full and object
        // being logged calls Logger.log() from its toString() method
        final Thread currentThread = Thread.currentThread();
        if (currentThread.getId() == backgroundThreadId
                // Threads owned by log4j are most likely to result in
                // deadlocks because they generally consume events.
                // This prevents deadlocks between AsyncLoggerContext
                // disruptors.
                || currentThread instanceof Log4jThread) {
            return EventRoute.SYNCHRONOUS;
        }
        return EventRoute.ENQUEUE;
    }
}
