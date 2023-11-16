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

import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.Supplier;

/**
 * ReliabilityStrategy that uses read/write locks to prevent the LoggerConfig from stopping while it is in use.
 */
public class LockingReliabilityStrategy implements ReliabilityStrategy, LocationAwareReliabilityStrategy {
    private final LoggerConfig loggerConfig;
    private final ReadWriteLock reconfigureLock = new ReentrantReadWriteLock();
    private volatile boolean isStopping;

    public LockingReliabilityStrategy(final LoggerConfig loggerConfig) {
        this.loggerConfig = Objects.requireNonNull(loggerConfig, "loggerConfig was null");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.core.config.ReliabilityStrategy#log(org.apache.logging.log4j.util.Supplier,
     * java.lang.String, java.lang.String, org.apache.logging.log4j.Marker, org.apache.logging.log4j.Level,
     * org.apache.logging.log4j.message.Message, java.lang.Throwable)
     */
    @Override
    public void log(
            final Supplier<LoggerConfig> reconfigured,
            final String loggerName,
            final String fqcn,
            final Marker marker,
            final Level level,
            final Message data,
            final Throwable t) {

        final LoggerConfig config = getActiveLoggerConfig(reconfigured);
        try {
            config.log(loggerName, fqcn, marker, level, data, t);
        } finally {
            config.getReliabilityStrategy().afterLogEvent();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.core.config.ReliabilityStrategy#log(org.apache.logging.log4j.util.Supplier,
     * java.lang.String, java.lang.String, java.lang.StackTraceElement, org.apache.logging.log4j.Marker,
     * org.apache.logging.log4j.Level, org.apache.logging.log4j.message.Message, java.lang.Throwable)
     */
    @Override
    public void log(
            final Supplier<LoggerConfig> reconfigured,
            final String loggerName,
            final String fqcn,
            final StackTraceElement location,
            final Marker marker,
            final Level level,
            final Message data,
            final Throwable t) {
        final LoggerConfig config = getActiveLoggerConfig(reconfigured);
        try {
            config.log(loggerName, fqcn, location, marker, level, data, t);
        } finally {
            config.getReliabilityStrategy().afterLogEvent();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.core.config.ReliabilityStrategy#log(org.apache.logging.log4j.util.Supplier,
     * org.apache.logging.log4j.core.LogEvent)
     */
    @Override
    public void log(final Supplier<LoggerConfig> reconfigured, final LogEvent event) {
        final LoggerConfig config = getActiveLoggerConfig(reconfigured);
        try {
            config.log(event);
        } finally {
            config.getReliabilityStrategy().afterLogEvent();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.logging.log4j.core.config.ReliabilityStrategy#beforeLogEvent(org.apache.logging.log4j.core.config.
     * LoggerConfig, org.apache.logging.log4j.util.Supplier)
     */
    @Override
    public LoggerConfig getActiveLoggerConfig(final Supplier<LoggerConfig> next) {
        LoggerConfig result = this.loggerConfig;
        if (!beforeLogEvent()) {
            result = next.get();
            return result == this.loggerConfig
                    ? result
                    : result.getReliabilityStrategy().getActiveLoggerConfig(next);
        }
        return result;
    }

    private boolean beforeLogEvent() {
        reconfigureLock.readLock().lock();
        if (isStopping) {
            reconfigureLock.readLock().unlock();
            return false;
        }
        return true;
    }

    @Override
    public void afterLogEvent() {
        reconfigureLock.readLock().unlock();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.core.config.ReliabilityStrategy#beforeStopAppenders()
     */
    @Override
    public void beforeStopAppenders() {
        reconfigureLock.writeLock().lock();
        try {
            isStopping = true;
        } finally {
            reconfigureLock.writeLock().unlock();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.logging.log4j.core.config.ReliabilityStrategy#beforeStopConfiguration(org.apache.logging.log4j.core
     * .config.Configuration)
     */
    @Override
    public void beforeStopConfiguration(final Configuration configuration) {
        // no action
    }
}
