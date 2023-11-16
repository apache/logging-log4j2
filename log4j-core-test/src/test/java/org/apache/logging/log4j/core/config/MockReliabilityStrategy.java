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

import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.Supplier;
import org.opentest4j.MultipleFailuresError;

/**
 * Mock object for validating the behavior of a configuration interacting with ReliabilityStrategy.
 */
public class MockReliabilityStrategy implements ReliabilityStrategy {

    private final LoggerConfig config;
    private final List<AssertionError> errors = Collections.synchronizedList(new ArrayList<>());

    public MockReliabilityStrategy(final LoggerConfig config) {
        this.config = config;
    }

    @Override
    public void log(
            final Supplier<LoggerConfig> reconfigured,
            final String loggerName,
            final String fqcn,
            final Marker marker,
            final Level level,
            final Message data,
            final Throwable t) {
        config.log(loggerName, fqcn, marker, level, data, t);
    }

    @Override
    public void log(final Supplier<LoggerConfig> reconfigured, final LogEvent event) {
        config.log(event);
    }

    @Override
    public LoggerConfig getActiveLoggerConfig(final Supplier<LoggerConfig> next) {
        return config;
    }

    @Override
    public void afterLogEvent() {
        // no-op
    }

    @Override
    public void beforeStopAppenders() {
        checkState(LifeCycle.State.STOPPED, config);
        for (final Appender appender : config.getAppenders().values()) {
            checkState(LifeCycle.State.STARTED, appender);
        }
    }

    @Override
    public void beforeStopConfiguration(final Configuration configuration) {
        checkState(LifeCycle.State.STOPPING, configuration);
        checkState(LifeCycle.State.STARTED, config);
    }

    void rethrowAssertionErrors() {
        synchronized (errors) {
            if (!errors.isEmpty()) {
                throw new MultipleFailuresError(null, errors);
            }
        }
    }

    private void checkState(final LifeCycle.State expected, final LifeCycle object) {
        try {
            assertSame(
                    expected,
                    object.getState(),
                    () -> "Expected state " + expected + " for LifeCycle object " + object);
        } catch (final AssertionError e) {
            errors.add(e);
        }
    }
}
