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
package org.apache.logging.log4j.jul;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.logging.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.junit.Test;

/**
 *
 */
public abstract class AbstractLoggerTest {
    public static final String LOGGER_NAME = "Test";
    protected Logger logger;
    protected ListAppender eventAppender;
    protected ListAppender flowAppender;
    protected ListAppender stringAppender;

    @Test
    public void testGetName() throws Exception {
        assertThat(logger.getName()).isEqualTo(LOGGER_NAME);
    }

    @Test
    public void testGlobalLogger() throws Exception {
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
    public void testGlobalLoggerName() throws Exception {
        final Logger root = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        assertThat(root.getName()).isEqualTo(Logger.GLOBAL_LOGGER_NAME);
    }

    @Test
    public void testIsLoggable() throws Exception {
        assertThat(logger.isLoggable(java.util.logging.Level.SEVERE)).isTrue();
    }

    @Test
    public void testLog() throws Exception {
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
    public void testLogFilter() throws Exception {
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
    public void testAlteringLogFilter() throws Exception {
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
    public void testLogParamMarkers() {
        final Logger flowLogger = Logger.getLogger("TestFlow");
        flowLogger.logp(java.util.logging.Level.FINER, "sourceClass", "sourceMethod", "ENTER {0}", "params");
        final List<LogEvent> events = flowAppender.getEvents();
        assertThat(events.get(0).getMessage().getFormattedMessage()).isEqualTo("ENTER params");
    }

    @Test
    public void testLogUsingCustomLevel() throws Exception {
        logger.config("Config level");
        final List<LogEvent> events = eventAppender.getEvents();
        assertThat(events).hasSize(1);
        final LogEvent event = events.get(0);
        assertThat(event.getLevel()).isEqualTo(LevelTranslator.CONFIG);
    }

    @Test
    public void testLogWithCallingClass() throws Exception {
        final Logger log = Logger.getLogger("Test.CallerClass");
        log.config("Calling from LoggerTest");
        final List<String> messages = stringAppender.getMessages();
        assertThat(messages).hasSize(1);
        final String message = messages.get(0);
        assertThat(message).isEqualTo(AbstractLoggerTest.class.getName());
    }

    @Test
    public void testCurlyBraces() {
        testMessage("{message}");
    }

    @Test
    public void testPercent() {
        testMessage("message%s");
    }

    @Test
    public void testPercentAndCurlyBraces() {
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
    public void testFlowMessages() {
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
    public void testLambdasGlobalLogger() {
        testLambdaMessages("message");
    }

    @Test
    public void testLambdasCurlyBraces() {
        testLambdaMessages("{message}");
    }

    @Test
    public void testLambdasPercent() {
        testLambdaMessages("message%s");
    }

    @Test
    public void testLambdasPercentAndCurlyBraces() {
        testLambdaMessages("message{%s}");
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
