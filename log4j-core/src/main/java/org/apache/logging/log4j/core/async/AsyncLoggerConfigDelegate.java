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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.jmx.RingBufferAdmin;

/**
 * Encapsulates the mechanism used to log asynchronously. There is one delegate per configuration, which is shared by
 * all AsyncLoggerConfig objects in the configuration.
 */
public interface AsyncLoggerConfigDelegate {

    /**
     * If possible, delegates the invocation of {@code callAppenders} to the background thread and returns {@code true}.
     * If this is not possible (if it detects that delegating to the background thread would cause deadlock because the
     * current call to Logger.log() originated from the background thread and the ringbuffer is full) then this method
     * does nothing and returns {@code false}. It is the responsibility of the caller to process the event when this
     * method returns {@code false}.
     * 
     * @param event the event to delegate to the background thread
     * @param asyncLoggerConfig the logger config to call from the background thread
     * @return {@code true} if delegation was successful, {@code false} if the calling thread needs to process the event
     *         itself
     */
    boolean tryCallAppendersInBackground(LogEvent event, AsyncLoggerConfig asyncLoggerConfig);

    /**
     * Creates and returns a new {@code RingBufferAdmin} that instruments the ringbuffer of this
     * {@code AsyncLoggerConfig}.
     * 
     * @param contextName name of the {@code LoggerContext}
     * @param loggerConfigName name of the logger config
     * @return the RingBufferAdmin that instruments the ringbuffer
     */
    RingBufferAdmin createRingBufferAdmin(String contextName, String loggerConfigName);
}
