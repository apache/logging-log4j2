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

/**
 * Policy for deciding whether to discard the event, enqueue it or log the event on the current thread when the queue
 * is full.
 * <p>
 * The asynchronous logging queue may become full when the application is logging faster than the underlying appender
 * can keep up with for a long enough time to fill up the bounded queue. When this happens, the logging subsystem has to
 * choose what to do with the event:
 * </p>
 * <ul>
 *   <li>Enqueue the event. This will block until the background thread removes a log event from the queue and space for
 *     new events becomes available in the queue. There is a risk of causing deadlock here when the new logging call was
 *     made while processing another logging call, for example when Log4j calls {@code toString()} on a message
 *     parameter, and the parameter object makes a logging call from its {@code toString()} method.</li>
 *   <li>Bypass the queue and send the event directly to the underlying appenders. This is the default policy used by
 *     Log4j since 2.7: see {@link DefaultAsyncQueueFullPolicy}. The benefit of this approach is that
 *     events will not get lost, but the disadvantage is that the resulting log file will be confusing for users,
 *     since log events will appear in the log file in random order: new events that are directly logged are followed
 *     by older log events taken from the queue.</li>
 *   <li>Discard the event. Log4j offers a variation of this policy where log events that are more verbose than
 *     a certain threshold are discarded, and other events are sent to the underlying appenders.
 *     See {@link DiscardingAsyncQueueFullPolicy} for details.</li>
 * </ul>
 * <p>
 * See {@link AsyncQueueFullPolicyFactory} for how to install a custom policy.
 * </p>
 *
 * @see AsyncQueueFullPolicyFactory
 * @since 2.6
 */
public interface AsyncQueueFullPolicy {

    /**
     * Returns the appropriate route for the current log event, given the specified parameters.
     *
     * @param backgroundThreadId the thread ID of the background thread. Can be compared with the current thread's ID.
     * @param level the level of the log event
     * @return the appropriate route for the current event
     */
    EventRoute getRoute(final long backgroundThreadId, final Level level);
}
