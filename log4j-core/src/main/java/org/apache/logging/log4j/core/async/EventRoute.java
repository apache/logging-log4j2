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

/**
 * Enumeration over the different destinations where a log event can be sent.
 *
 * @see AsyncQueueFullPolicy
 * @see AsyncQueueFullPolicyFactory
 * @see DefaultAsyncQueueFullPolicy
 * @see DiscardingAsyncQueueFullPolicy
 * @since 2.6
 */
public enum EventRoute {
    /**
     * Enqueues the event for asynchronous logging in the background thread.
     */
    ENQUEUE,
    /**
     * Logs the event synchronously: sends the event directly to the appender (in the current thread).
     * WARNING: This may result in lines logged out of order as synchronous events may be persisted before
     * earlier events, even from the same thread, which wait in the queue.
     */
    SYNCHRONOUS,
    /**
     * Discards the event (so it is not logged at all).
     */
    DISCARD;
}
