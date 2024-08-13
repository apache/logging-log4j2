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
package org.apache.logging.log4j.status;

import static org.apache.logging.log4j.status.StatusLogger.DEFAULT_FALLBACK_LISTENER_LEVEL;
import static org.apache.logging.log4j.util.Constants.LOG4J2_DEBUG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.message.ParameterizedNoReferenceMessageFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.org.webcompere.systemstubs.SystemStubs;

class StatusLoggerLevelTest {

    @Test
    void effective_level_should_be_the_least_specific_one() {

        // Verify the initial level
        final StatusLogger logger = new StatusLogger();
        final Level fallbackListenerLevel = DEFAULT_FALLBACK_LISTENER_LEVEL;
        assertThat(logger.getLevel()).isEqualTo(fallbackListenerLevel);

        // Register a less specific listener
        final StatusListener listener1 = mock(StatusListener.class);
        Level listener1Level = Level.WARN;
        when(listener1.getStatusLevel()).thenReturn(listener1Level);
        logger.registerListener(listener1);
        assertThat(listener1Level).isNotEqualTo(fallbackListenerLevel); // Verify that the level is distinct
        assertThat(logger.getLevel()).isEqualTo(listener1Level); // Verify that the logger level is changed

        // Register a less specific listener
        final StatusListener listener2 = mock(StatusListener.class);
        final Level listener2Level = Level.INFO;
        when(listener2.getStatusLevel()).thenReturn(listener2Level);
        logger.registerListener(listener2);
        assertThat(listener2Level)
                .isNotEqualTo(fallbackListenerLevel)
                .isNotEqualTo(listener1Level); // Verify that the level is distinct
        assertThat(logger.getLevel()).isEqualTo(listener2Level); // Verify that the logger level is changed

        // Register a more specific listener
        final StatusListener listener3 = mock(StatusListener.class);
        final Level listener3Level = Level.ERROR;
        when(listener3.getStatusLevel()).thenReturn(listener3Level);
        logger.registerListener(listener3);
        assertThat(listener3Level)
                .isNotEqualTo(listener1Level)
                .isNotEqualTo(listener2Level); // Verify that the level is distinct
        assertThat(logger.getLevel()).isEqualTo(listener2Level); // Verify that the logger level is not changed

        // Update a registered listener level
        listener1Level = Level.DEBUG;
        when(listener1.getStatusLevel()).thenReturn(listener1Level);
        assertThat(listener1Level) // Verify that the level is distinct
                .isNotEqualTo(fallbackListenerLevel)
                .isNotEqualTo(listener2Level)
                .isNotEqualTo(listener3Level);
        assertThat(logger.getLevel()).isEqualTo(listener1Level); // Verify that the logger level is changed

        // Remove the least specific listener
        logger.removeListener(listener2);
        assertThat(logger.getLevel()).isEqualTo(listener1Level); // Verify that the level is changed

        // Remove the most specific listener
        logger.removeListener(listener3);
        assertThat(logger.getLevel()).isEqualTo(listener1Level); // Verify that the level is not changed

        // Remove the last listener
        logger.removeListener(listener1);
        assertThat(logger.getLevel()).isEqualTo(fallbackListenerLevel); // Verify that the level is changed
    }

    @Test
    void invalid_level_should_cause_fallback_to_defaults() throws Exception {

        // Create a `StatusLogger` configuration using an invalid level
        final Properties statusLoggerConfigProperties = new Properties();
        final String invalidLevelName = "FOO";
        statusLoggerConfigProperties.put(StatusLogger.DEFAULT_STATUS_LISTENER_LEVEL, invalidLevelName);
        final StatusLogger.Config[] statusLoggerConfigRef = {null};
        final String stderr = SystemStubs.tapSystemErr(
                () -> statusLoggerConfigRef[0] = new StatusLogger.Config(statusLoggerConfigProperties));
        final StatusLogger.Config statusLoggerConfig = statusLoggerConfigRef[0];

        // Verify the stderr dump
        assertThat(stderr).contains("Failed reading the level");

        // Verify the level
        assertThat(statusLoggerConfig.fallbackListenerLevel).isEqualTo(DEFAULT_FALLBACK_LISTENER_LEVEL);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void debug_mode_should_override_log_filtering(final boolean debugEnabled) {

        // Create a logger with debug enabled
        final StatusLogger.Config loggerConfig = new StatusLogger.Config(debugEnabled, 0, null);
        final Level loggerLevel = Level.ERROR;
        final StatusConsoleListener fallbackListener = mock(StatusConsoleListener.class);
        when(fallbackListener.getStatusLevel()).thenReturn(loggerLevel);
        final StatusLogger logger = new StatusLogger(
                StatusLoggerLevelTest.class.getSimpleName(),
                ParameterizedNoReferenceMessageFactory.INSTANCE,
                loggerConfig,
                fallbackListener);

        // Log at all levels
        final Level[] levels = Level.values();
        for (final Level level : levels) {
            logger.log(level, "test for level `{}`", level);
        }

        // Calculate the number of expected messages
        final int expectedMessageCount;
        if (debugEnabled) {
            expectedMessageCount = levels.length;
        } else {
            expectedMessageCount = (int) Arrays.stream(levels)
                    .filter(loggerLevel::isLessSpecificThan)
                    .count();
        }

        // Verify the fallback listener invocation
        assertThat(expectedMessageCount).isGreaterThan(0);
        verify(fallbackListener, times(expectedMessageCount)).log(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void debug_mode_should_override_listener_filtering(final boolean debugEnabled) {

        // Create a logger with debug enabled
        final StatusLogger.Config loggerConfig = new StatusLogger.Config(debugEnabled, 0, null);
        final StatusLogger logger = new StatusLogger(
                StatusLoggerLevelTest.class.getSimpleName(),
                ParameterizedNoReferenceMessageFactory.INSTANCE,
                loggerConfig,
                new StatusConsoleListener(Level.ERROR));

        // Register a listener
        final Level listenerLevel = Level.INFO;
        final StatusListener listener = mock(StatusListener.class);
        when(listener.getStatusLevel()).thenReturn(listenerLevel);
        logger.registerListener(listener);

        // Log at all levels
        final Level[] levels = Level.values();
        for (final Level level : levels) {
            logger.log(level, "test for level `{}`", level);
        }

        // Calculate the number of expected messages
        final int expectedMessageCount;
        if (debugEnabled) {
            expectedMessageCount = levels.length;
        } else {
            expectedMessageCount = (int) Arrays.stream(levels)
                    .filter(listenerLevel::isLessSpecificThan)
                    .count();
        }

        // Verify the listener invocation
        assertThat(expectedMessageCount).isGreaterThan(0);
        verify(listener, times(expectedMessageCount)).log(any());
    }

    @ParameterizedTest
    @MethodSource("debugPropertyTestCases")
    void debug_property_should_be_read_correctly(final String propertyValue, final boolean debugEnabled) {
        final Properties configProperties = new Properties();
        if (propertyValue != null) {
            configProperties.put(LOG4J2_DEBUG, propertyValue);
        }
        final StatusLogger.Config config = new StatusLogger.Config(configProperties);
        assertThat(config.debugEnabled).isEqualTo(debugEnabled);
    }

    static List<Arguments> debugPropertyTestCases() {
        List<Arguments> testCases = new ArrayList<>();
        Arrays.asList("t", "true", "True", "TRUE", "f", "fals", "falsx")
                .forEach(propertyValue -> testCases.add(Arguments.of(propertyValue, true)));
        Arrays.asList("false", "FALSE", "faLSe")
                .forEach(propertyValue -> testCases.add(Arguments.of(propertyValue, false)));
        return testCases;
    }
}
