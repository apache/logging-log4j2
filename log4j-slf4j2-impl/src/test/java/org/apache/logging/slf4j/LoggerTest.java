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
package org.apache.logging.slf4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.spi.LocationAwareLogger;
import org.slf4j.spi.LoggingEventBuilder;

/**
 *
 */
@LoggerContextSource("log4j-test1.xml")
public class LoggerTest {

    private final Logger logger;
    private final LoggerContext ctx;

    @Test
    public void debug() {
        logger.debug("Debug message");
        verify("o.a.l.s.LoggerTest Debug message MDC{}" + Strings.LINE_SEPARATOR);
    }

    public LoggerTest(final LoggerContext context) {
        this.ctx = context;
        this.logger = LoggerFactory.getLogger("LoggerTest");
    }

    @Test
    public void debugNoParms() {
        logger.debug("Debug message {}");
        verify("o.a.l.s.LoggerTest Debug message {} MDC{}" + Strings.LINE_SEPARATOR);
        logger.debug("Debug message {}", (Object[]) null);
        verify("o.a.l.s.LoggerTest Debug message {} MDC{}" + Strings.LINE_SEPARATOR);
        ((LocationAwareLogger) logger)
                .log(null, Log4jLogger.class.getName(), LocationAwareLogger.DEBUG_INT, "Debug message {}", null, null);
        verify("o.a.l.s.LoggerTest Debug message {} MDC{}" + Strings.LINE_SEPARATOR);
    }

    @Test
    public void debugWithParms() {
        logger.debug("Hello, {}", "World");
        verify("o.a.l.s.LoggerTest Hello, World MDC{}" + Strings.LINE_SEPARATOR);
    }

    @Test
    public void mdc() {

        MDC.put("TestYear", "2010");
        logger.debug("Debug message");
        verify("o.a.l.s.LoggerTest Debug message MDC{TestYear=2010}" + Strings.LINE_SEPARATOR);
        MDC.clear();
        logger.debug("Debug message");
        verify("o.a.l.s.LoggerTest Debug message MDC{}" + Strings.LINE_SEPARATOR);
    }

    @Test
    public void mdcStack() {
        MDC.pushByKey("TestYear", "2010");
        logger.debug("Debug message");
        verify("o.a.l.s.LoggerTest Debug message MDC{TestYear=2010}" + Strings.LINE_SEPARATOR);
        MDC.pushByKey("TestYear", "2011");
        logger.debug("Debug message");
        verify("o.a.l.s.LoggerTest Debug message MDC{TestYear=2011}" + Strings.LINE_SEPARATOR);
        MDC.popByKey("TestYear");
        logger.debug("Debug message");
        verify("o.a.l.s.LoggerTest Debug message MDC{TestYear=2010}" + Strings.LINE_SEPARATOR);
        MDC.clear();
        logger.debug("Debug message");
        verify("o.a.l.s.LoggerTest Debug message MDC{}" + Strings.LINE_SEPARATOR);
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/LOG4J2-793">LOG4J2-793</a>
     */
    @Test
    public void supportsCustomSLF4JMarkers() {
        final Marker marker = new CustomFlatMarker("TEST");
        logger.debug(marker, "Test");
        verify("o.a.l.s.LoggerTest Test MDC{}" + Strings.LINE_SEPARATOR);
    }

    @Test
    public void testRootLogger() {
        final Logger l = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        assertNotNull(l, "No Root Logger");
        assertEquals(Logger.ROOT_LOGGER_NAME, l.getName());
    }

    @Test
    public void doubleSubst() {
        logger.debug("Hello, {}", "Log4j {}");
        verify("o.a.l.s.LoggerTest Hello, Log4j {} MDC{}" + Strings.LINE_SEPARATOR);
    }

    @Test
    public void testThrowable() {
        final Throwable expected = new RuntimeException();
        logger.debug("Hello {}", expected);
        verifyThrowable(expected);
        logger.debug("Hello {}", (Object) expected);
        verifyThrowable(null);
        logger.debug("Hello", expected);
        verifyThrowable(expected);
        logger.debug("Hello {}! {}", "World!", expected);
        verifyThrowable(null);
        logger.debug("Hello {}!", "World!", expected);
        verifyThrowable(expected);
        final LocationAwareLogger lal = (LocationAwareLogger) logger;
        lal.log(null, LoggerTest.class.getName(), LocationAwareLogger.DEBUG_INT, "Hello {}", null, expected);
        verifyThrowable(expected);
        lal.log(
                null,
                LoggerTest.class.getName(),
                LocationAwareLogger.DEBUG_INT,
                "Hello {}",
                new Object[] {expected},
                null);
        verifyThrowable(null);
        lal.log(
                null,
                LoggerTest.class.getName(),
                LocationAwareLogger.DEBUG_INT,
                "Hello {}",
                new Object[] {"World!", expected},
                null);
        verifyThrowable(expected);
    }

    @Test
    public void testLazyLoggingEventBuilder() {
        final ListAppender appender = ctx.getConfiguration().getAppender("UnformattedList");
        final Level oldLevel = ctx.getRootLogger().getLevel();
        try {
            Configurator.setRootLevel(Level.ERROR);
            final LoggingEventBuilder builder = logger.makeLoggingEventBuilder(org.slf4j.event.Level.DEBUG);
            Configurator.setRootLevel(Level.DEBUG);
            builder.log();
            assertThat(appender.getEvents()).hasSize(1).map(LogEvent::getLevel).containsExactly(Level.DEBUG);
        } finally {
            Configurator.setRootLevel(oldLevel);
        }
    }

    private ListAppender getAppenderByName(final String name) {
        final ListAppender listApp = ctx.getConfiguration().getAppender(name);
        assertNotNull(listApp, "Missing Appender");
        return listApp;
    }

    private void verify(final String expected) {
        final ListAppender listApp = getAppenderByName("List");
        final List<String> events = listApp.getMessages();
        assertEquals(1, events.size(), "Incorrect number of messages. Expected 1 Actual " + events.size());
        final String actual = events.get(0);
        assertEquals(expected, actual, "Incorrect message. Expected \" + expected + \". Actual \" + actual");
        listApp.clear();
    }

    private void verifyThrowable(final Throwable expected) {
        final ListAppender listApp = getAppenderByName("UnformattedList");
        final List<LogEvent> events = listApp.getEvents();
        assertEquals(1, events.size(), "Incorrect number of messages");
        final LogEvent actual = events.get(0);
        assertEquals(expected, actual.getThrown(), "Incorrect throwable.");
        listApp.clear();
    }

    @BeforeEach
    @AfterEach
    public void cleanup(
            @Named("List") final ListAppender list, @Named("UnformattedList") final ListAppender unformattedList) {
        MDC.clear();
        list.clear();
        unformattedList.clear();
    }
}
