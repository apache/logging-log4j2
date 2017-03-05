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

import org.apache.logging.log4j.Level;

/**
 * Default: any logging done when queue is full bypasses the queue and logs synchronously: send the event directly to
 * the appender (in the current thread).
 */
public class DefaultAsyncQueueFullPolicy implements AsyncQueueFullPolicy {
    @Override
    public EventRoute getRoute(final long backgroundThreadId, final Level level) {

        // LOG4J2-1518: prevent deadlock when RingBuffer is full and object being logged calls
        // Logger.log in application thread
        // See also LOG4J2-471: prevent deadlock when RingBuffer is full and object
        // being logged calls Logger.log() from its toString() method in background thread
        return EventRoute.SYNCHRONOUS;
    }
}
