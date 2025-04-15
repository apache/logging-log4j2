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
package org.apache.logging.log4j.jul.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.ConsoleHandler;
import java.util.logging.Filter;
import java.util.logging.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.jul.ApiLogger;
import org.apache.logging.log4j.jul.LevelTranslator;
import org.junit.jupiter.api.Test;

/**
 *
 */
abstract class AbstractLoggerTest {
    public static final String LOGGER_NAME = "Test";

    static final java.util.logging.Level[] LEVELS = new java.util.logging.Level[] {
        java.util.logging.Level.ALL,
        java.util.logging.Level.FINEST,
        java.util.logging.Level.FINER,
        java.util.logging.Level.FINE,
        java.util.logging.Level.CONFIG,
        java.util.logging.Level.INFO,
        java.util.logging.Level.WARNING,
        java.util.logging.Level.SEVERE,
        java.util.logging.Level.OFF
    };

    static java.util.logging.Level getEffectiveLevel(final Logger logger) {
        for (final java.util.logging.Level level : LEVELS) {
            if (logger.isLoggable(level)) {
                return level;
            }
        }
        throw new RuntimeException("No level is enabled.");
    }

    protected Logger logger;
    protected ListAppender eventAppender;
    protected ListAppender flowAppender;
    protected ListAppender stringAppender;

    @Test
    void testGetName() {
        assertThat(logger.getName()).isEqualTo(LOGGER_NAME);
    }

    @Test
    void testGlobalLogger() {
        final Logger root = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        root.info("Test info message");
        root.config("Test info message");
        root.fine("Test info message");
        final List<LogEvent> events = eventAppender.getEvents();
        assertThat(events).hasSize(3);
        for (final LogEvent event : events) {
            final String message = event.getMessage().getFormattedMessage();
            assertThat(message).isEqualTo("Test info message");
        }
    }

    @Test
    void testGlobalLoggerName() {
        final Logger root = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        assertThat(root.getName()).isEqualTo(Logger.GLOBAL_LOGGER_NAME);
    }

    @Test
    void testIsLoggable() {
        assertThat(logger.isLoggable(java.util.logging.Level.SEVERE)).isTrue();
    }

    @Test
    void testLog() {
        logger.info("Informative message here.");
        final List<LogEvent> events = eventAppender.getEvents();
        assertThat(events).hasSize(1);
        final LogEvent event = events.get(0);
        assertThat(event).isInstanceOf(Log4jLogEvent.class);
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getLoggerName()).isEqualTo(LOGGER_NAME);
        assertThat(event.getMessage().getFormattedMessage()).isEqualTo("Informative message here.");
        assertThat(event.getLoggerFqcn()).isEqualTo(ApiLogger.class.getName());
    }

    @Test
    void testLogFilter() {
        logger.setFilter(record -> false);
        logger.severe("Informative message here.");
        logger.warning("Informative message here.");
        logger.info("Informative message here.");
        logger.config("Informative message here.");
        logger.fine("Informative message here.");
        logger.finer("Informative message here.");
        logger.finest("Informative message here.");
        final List<LogEvent> events = eventAppender.getEvents();
        assertThat(events).isEmpty();
    }

    @Test
    void testAlteringLogFilter() {
        logger.setFilter(record -> {
            record.setMessage("This is not the message you are looking for.");
            return true;
        });
        logger.info("Informative message here.");
        final List<LogEvent> events = eventAppender.getEvents();
        assertThat(events).hasSize(1);
        final LogEvent event = events.get(0);
        assertThat(event).isInstanceOf(Log4jLogEvent.class);
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getLoggerName()).isEqualTo(LOGGER_NAME);
        assertThat(event.getMessage().getFormattedMessage()).isEqualTo("This is not the message you are looking for.");
        assertThat(event.getLoggerFqcn()).isEqualTo(ApiLogger.class.getName());
    }

    @Test
    void testLogParamMarkers() {
        final Logger flowLogger = Logger.getLogger("TestFlow");
        flowLogger.logp(java.util.logging.Level.FINER, "sourceClass", "sourceMethod", "ENTER {0}", "params");
        final List<LogEvent> events = flowAppender.getEvents();
        assertThat(events.get(0).getMessage().getFormattedMessage()).isEqualTo("ENTER params");
    }

    @Test
    void testLogUsingCustomLevel() {
        logger.config("Config level");
        final List<LogEvent> events = eventAppender.getEvents();
        assertThat(events).hasSize(1);
        final LogEvent event = events.get(0);
        assertThat(event.getLevel()).isEqualTo(LevelTranslator.CONFIG);
    }

    @Test
    void testLogWithCallingClass() {
        final Logger log = Logger.getLogger("Test.CallerClass");
        log.config("Calling from LoggerTest");
        final List<String> messages = stringAppender.getMessages();
        assertThat(messages).hasSize(1);
        final String message = messages.get(0);
        assertThat(message).isEqualTo(AbstractLoggerTest.class.getName());
    }

    @Test
    void testCurlyBraces() {
        testMessage("{message}");
    }

    @Test
    void testPercent() {
        testMessage("message%s");
    }

    @Test
    void testPercentAndCurlyBraces() {
        testMessage("message{%s}");
    }

    private void testMessage(final String string) {
        final Logger root = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        root.info("Test info " + string);
        root.config("Test info " + string);
        root.fine("Test info " + string);
        final List<LogEvent> events = eventAppender.getEvents();
        assertThat(events).hasSize(3);
        for (final LogEvent event : events) {
            final String message = event.getMessage().getFormattedMessage();
            assertThat(message).isEqualTo("Test info " + string);
        }
    }

    @Test
    void testFlowMessages() {
        final Logger flowLogger = Logger.getLogger("TestFlow");
        flowLogger.entering("com.example.TestSourceClass1", "testSourceMethod1(String)");
        flowLogger.entering("com.example.TestSourceClass2", "testSourceMethod2(String)", "TestParam");
        flowLogger.entering(
                "com.example.TestSourceClass3", "testSourceMethod3(String)", new Object[] {"TestParam0", "TestParam1"});
        final List<LogEvent> events = flowAppender.getEvents();
        assertThat(events).hasSize(3);
        assertThat(events.get(0).getMessage().getFormattedMessage()).isEqualTo("Enter");
        assertThat(events.get(1).getMessage().getFormattedMessage()).isEqualTo("Enter params(TestParam)");
        assertThat(events.get(2).getMessage().getFormattedMessage()).isEqualTo("Enter params(TestParam0, TestParam1)");
    }

    @Test
    void testLambdasGlobalLogger() {
        testLambdaMessages("message");
    }

    @Test
    void testLambdasCurlyBraces() {
        testLambdaMessages("{message}");
    }

    @Test
    void testLambdasPercent() {
        testLambdaMessages("message%s");
    }

    @Test
    void testLambdasPercentAndCurlyBraces() {
        testLambdaMessages("message{%s}");
    }

    /**
     * This assertion must be true, even if {@code setLevel} has no effect on the logging implementation.
     *
     * @see <a href="https://github.com/apache/logging-log4j2/issues/3119">GH issue #3119</a>
     */
    @Test
    void testSetAndGetLevel() {
        final Logger logger = Logger.getLogger(AbstractLoggerTest.class.getName() + ".testSetAndGetLevel");
        // The logger under test should have no explicit configuration
        assertThat(logger.getLevel()).isNull();

        for (java.util.logging.Level level : LEVELS) {
            logger.setLevel(level);
            assertThat(logger.getLevel()).as("Level set using `setLevel()`").isEqualTo(level);
        }
    }

    /**
     * The value of `useParentHandlers` should be recorded even if it is not effective.
     */
    @Test
    void testSetUseParentHandlers() {
        final Logger logger = Logger.getLogger(AbstractLoggerTest.class.getName() + ".testSetUseParentHandlers");

        for (boolean useParentHandlers : new boolean[] {false, true}) {
            logger.setUseParentHandlers(useParentHandlers);
            assertThat(logger.getUseParentHandlers()).isEqualTo(useParentHandlers);
        }
    }

    /**
     * The programmatically configured handlers should be recorded, even if they are not used.
     */
    @Test
    void testAddAndRemoveHandlers() {
        final Logger logger = Logger.getLogger(AbstractLoggerTest.class.getName() + ".testAddAndRemoveHandlers");

        assertThat(logger.getHandlers()).isEmpty();
        // Add a handler
        ConsoleHandler handler = new ConsoleHandler();
        logger.addHandler(handler);
        assertThat(logger.getHandlers()).hasSize(1).containsExactly(handler);
        // Remove handler
        logger.removeHandler(handler);
        assertThat(logger.getHandlers()).isEmpty();
    }

    /**
     * The programmatically configured filters should be recorded, even if they are not used.
     */
    @Test
    void testSetFilter() {
        final Logger logger = Logger.getLogger(AbstractLoggerTest.class.getName() + ".testSetFilter");

        assertThat(logger.getFilter()).isNull();
        // Set filter
        Filter denyAllFilter = record -> false;
        logger.setFilter(denyAllFilter);
        assertThat(logger.getFilter()).isEqualTo(denyAllFilter);
        // Remove filter
        logger.setFilter(null);
        assertThat(logger.getFilter()).isNull();
    }

    /**
     * The programmatically configured resource bundles should be recorded, even if they are not used.
     */
    @Test
    void testSetResourceBundle() {
        final Logger logger = Logger.getLogger(AbstractLoggerTest.class.getName() + ".testSetResourceBundle");

        assertThat(logger.getResourceBundle()).isNull();
        // Set resource bundle
        ResourceBundle bundle = ResourceBundle.getBundle("testResourceBundle");
        logger.setResourceBundle(bundle);
        assertThat(logger.getResourceBundle()).isSameAs(bundle);
    }

    private void testLambdaMessages(final String string) {
        final Logger root = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        root.info(() -> "Test info " + string);
        root.config(() -> "Test info " + string);
        root.fine(() -> "Test info " + string);
        final List<LogEvent> events = eventAppender.getEvents();
        assertThat(events).hasSize(3);
        for (final LogEvent event : events) {
            final String message = event.getMessage().getFormattedMessage();
            assertThat(message).isEqualTo("Test info " + string);
        }
    }
}
