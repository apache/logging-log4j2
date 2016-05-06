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
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.LogEventFactory;
import org.apache.logging.log4j.core.jmx.RingBufferAdmin;

/**
 * Encapsulates the mechanism used to log asynchronously. There is one delegate per configuration, which is shared by
 * all AsyncLoggerConfig objects in the configuration.
 */
public interface AsyncLoggerConfigDelegate {

    /**
     * Creates and returns a new {@code RingBufferAdmin} that instruments the ringbuffer of this
     * {@code AsyncLoggerConfig}.
     *
     * @param contextName name of the {@code LoggerContext}
     * @param loggerConfigName name of the logger config
     * @return the RingBufferAdmin that instruments the ringbuffer
     */
    RingBufferAdmin createRingBufferAdmin(final String contextName, final String loggerConfigName);

    /**
     * Returns the {@code EventRoute} for the event with the specified level.
     *
     * @param level the level of the event to log
     * @return the {@code EventRoute}
     */
    EventRoute getEventRoute(final Level level);

    void enqueueEvent(LogEvent event, AsyncLoggerConfig asyncLoggerConfig);

    boolean tryEnqueue(LogEvent event, AsyncLoggerConfig asyncLoggerConfig);

    /**
     * Notifies the delegate what LogEventFactory an AsyncLoggerConfig is using, so the delegate can determine
     * whether to populate the ring buffer with mutable log events or not. This method may be invoced multiple times
     * for all AsyncLoggerConfigs that use this delegate.
     *
     * @param logEventFactory the factory used
     */
    void setLogEventFactory(LogEventFactory logEventFactory);
}
