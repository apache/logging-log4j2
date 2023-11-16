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
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.Supplier;

/**
 * Interface for objects that know how to ensure delivery of log events to the appropriate appenders, even during and
 * after the configuration has been modified while the system is actively used.
 */
public interface ReliabilityStrategy {

    /**
     * Logs an event.
     *
     * @param reconfigured supplies the next LoggerConfig if the strategy's LoggerConfig is no longer active
     * @param loggerName The name of the Logger.
     * @param fqcn The fully qualified class name of the caller.
     * @param marker A Marker or null if none is present.
     * @param level The event Level.
     * @param data The Message.
     * @param t A Throwable or null.
     */
    void log(
            Supplier<LoggerConfig> reconfigured,
            String loggerName,
            String fqcn,
            Marker marker,
            Level level,
            Message data,
            Throwable t);

    /**
     * Logs an event.
     *
     * @param reconfigured supplies the next LoggerConfig if the strategy's LoggerConfig is no longer active
     * @param event The log event.
     */
    void log(Supplier<LoggerConfig> reconfigured, LogEvent event);

    /**
     * For internal use by the ReliabilityStrategy; returns the LoggerConfig to use.
     *
     * @param next supplies the next LoggerConfig if the strategy's LoggerConfig is no longer active
     * @return the currently active LoggerConfig
     */
    LoggerConfig getActiveLoggerConfig(Supplier<LoggerConfig> next);

    /**
     * Called after a log event was logged.
     */
    void afterLogEvent();

    /**
     * Called before all appenders are stopped.
     */
    void beforeStopAppenders();

    /**
     * Called before the configuration is stopped.
     *
     * @param configuration the configuration that will be stopped
     */
    void beforeStopConfiguration(Configuration configuration);
}
