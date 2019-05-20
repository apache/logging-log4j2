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
 * {@link EventRoute#SYNCHRONOUS Synchronous} policy which logs using the non-asynchronous
 * code path when the asynchronous logging queue is full.
 * WARNING: This results in lines logged out of order as synchronous events may be persisted before earlier
 * events, even from the same thread, which wait in the queue.
 *
 * @since 2.12.0
 */
public final class SynchronousAsyncQueueFullPolicy implements AsyncQueueFullPolicy {
    @Override
    public EventRoute getRoute(final long backgroundThreadId, final Level level) {
        return EventRoute.SYNCHRONOUS;
    }
}
