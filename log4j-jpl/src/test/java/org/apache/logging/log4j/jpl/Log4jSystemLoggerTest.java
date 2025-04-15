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
package org.apache.logging.log4j.jpl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.System.Logger;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Log4jSystemLoggerTest {

    public static final String LOGGER_NAME = "Test";
    protected Logger logger;
    protected ListAppender eventAppender;
    protected ListAppender stringAppender;

    @BeforeEach
    void setUp() {
        logger = System.getLogger(LOGGER_NAME);
        assertThat(logger, instanceOf(Log4jSystemLogger.class));
        eventAppender = ListAppender.getListAppender("TestAppender");
        stringAppender = ListAppender.getListAppender("StringAppender");
        assertNotNull(eventAppender);
        assertNotNull(stringAppender);
    }

    @AfterEach
    void tearDown() {
        if (eventAppender != null) {
            eventAppender.clear();
        }
        if (stringAppender != null) {
            stringAppender.clear();
        }
    }

    @Test
    void testGetName() {
        assertThat(logger.getName(), equalTo(LOGGER_NAME));
    }

    @Test
    void testIsLoggable() {
        assertThat(logger.isLoggable(Logger.Level.ERROR), equalTo(true));
    }

    @Test
    void testLog() {
        logger.log(Logger.Level.INFO, "Informative message here.");
        final List<LogEvent> events = eventAppender.getEvents();
        assertThat(events, hasSize(1));
        final LogEvent event = events.get(0);
        assertThat(event, instanceOf(Log4jLogEvent.class));
        assertEquals(Level.INFO, event.getLevel());
        assertEquals(LOGGER_NAME, event.getLoggerName());
        assertEquals("Informative message here.", event.getMessage().getFormattedMessage());
        assertEquals(Log4jSystemLogger.class.getName(), event.getLoggerFqcn());
    }

    @Test
    void testParameterizedLogging() {
        logger.log(Logger.Level.INFO, "Hello, {0}!", "World");
        final List<LogEvent> events = eventAppender.getEvents();
        assertThat(events, hasSize(1));
        final LogEvent event = events.get(0);
        assertThat(event, instanceOf(Log4jLogEvent.class));
        assertEquals(Level.INFO, event.getLevel());
        assertEquals(LOGGER_NAME, event.getLoggerName());
        assertEquals("Hello, World!", event.getMessage().getFormattedMessage());
        assertEquals(Log4jSystemLogger.class.getName(), event.getLoggerFqcn());
    }

    @Test
    void testParameterizedLoggingWithThrowable() {
        final Throwable throwable = new RuntimeException();
        logger.log(Logger.Level.INFO, "Hello, {0}!", "World", throwable);
        final List<LogEvent> events = eventAppender.getEvents();
        assertThat(events, hasSize(1));
        final LogEvent event = events.get(0);
        assertThat(event, instanceOf(Log4jLogEvent.class));
        assertEquals(Level.INFO, event.getLevel());
        assertEquals(LOGGER_NAME, event.getLoggerName());
        assertEquals("Hello, World!", event.getMessage().getFormattedMessage());
        assertEquals(Log4jSystemLogger.class.getName(), event.getLoggerFqcn());
        assertSame(throwable, event.getThrown());
    }

    @Test
    void testLogWithCallingClass() {
        final Logger log = System.getLogger("Test.CallerClass");
        log.log(Logger.Level.INFO, "Calling from LoggerTest");
        final List<String> messages = stringAppender.getMessages();
        assertThat(messages, hasSize(1));
        final String message = messages.get(0);
        assertEquals(Log4jSystemLoggerTest.class.getName(), message);
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
        logger.log(Logger.Level.INFO, "Test info " + string);
        final List<LogEvent> events = eventAppender.getEvents();
        assertThat(events, hasSize(1));
        for (final LogEvent event : events) {
            final String message = event.getMessage().getFormattedMessage();
            assertThat(message, equalTo("Test info " + string));
        }
    }
}
