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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.Supplier;

/**
 * ReliabilityStrategy that counts the number of threads that have started to log an event but have not completed yet,
 * and waits for these threads to finish before allowing the appenders to be stopped.
 */
public class AwaitCompletionReliabilityStrategy implements ReliabilityStrategy, LocationAwareReliabilityStrategy {
    private static final int MAX_RETRIES = 3;
    private final AtomicInteger counter = new AtomicInteger();
    private final AtomicBoolean shutdown = new AtomicBoolean();
    private final Lock shutdownLock = new ReentrantLock();
    private final Condition noLogEvents = shutdownLock.newCondition(); // should only be used when shutdown == true
    private final LoggerConfig loggerConfig;

    public AwaitCompletionReliabilityStrategy(final LoggerConfig loggerConfig) {
        this.loggerConfig = Objects.requireNonNull(loggerConfig, "loggerConfig is null");
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
        return counter.incrementAndGet() > 0;
    }

    @Override
    public void afterLogEvent() {
        if (counter.decrementAndGet() == 0 && shutdown.get()) {
            signalCompletionIfShutdown();
        }
    }

    private void signalCompletionIfShutdown() {
        final Lock lock = shutdownLock;
        lock.lock();
        try {
            noLogEvents.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.core.config.ReliabilityStrategy#beforeStopAppenders()
     */
    @Override
    public void beforeStopAppenders() {
        waitForCompletion();
    }

    /**
     * Waits for all log events to complete before returning.
     */
    private void waitForCompletion() {
        shutdownLock.lock();
        try {
            if (shutdown.compareAndSet(false, true)) {
                int retries = 0;
                // repeat while counter is non-zero
                while (!counter.compareAndSet(0, Integer.MIN_VALUE)) {

                    // counter was non-zero
                    if (counter.get() < 0) { // this should not happen
                        return; // but if it does, we are already done
                    }
                    // counter greater than zero, wait for afterLogEvent to decrease count
                    try {
                        noLogEvents.await(retries + 1, TimeUnit.SECONDS);
                    } catch (final InterruptedException ie) {
                        if (++retries > MAX_RETRIES) {
                            break;
                        }
                    }
                }
            }
        } finally {
            shutdownLock.unlock();
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
