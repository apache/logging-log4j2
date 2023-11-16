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
package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.Supplier;

/**
 * Interface to ensure delivery of log events to the appropriate Appenders while including location information.
 */
public interface LocationAwareReliabilityStrategy {
    /**
     * Logs an event.
     *
     * @param reconfigured supplies the next LoggerConfig if the strategy's LoggerConfig is no longer active
     * @param loggerName The name of the Logger.
     * @param fqcn The fully qualified class name of the caller.
     * @param location The location of the caller or null.
     * @param marker A Marker or null if none is present.
     * @param level The event Level.
     * @param data The Message.
     * @param t A Throwable or null.
     * @since 3.0
     */
    void log(
            Supplier<LoggerConfig> reconfigured,
            String loggerName,
            String fqcn,
            StackTraceElement location,
            Marker marker,
            Level level,
            Message data,
            Throwable t);
}
