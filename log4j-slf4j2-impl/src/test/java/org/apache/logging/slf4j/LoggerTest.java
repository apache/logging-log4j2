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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.apache.logging.log4j.util.Strings;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.spi.LocationAwareLogger;
import org.slf4j.spi.LoggingEventBuilder;

/**
 *
 */
public class LoggerTest {

    private static final String CONFIG = "log4j-test1.xml";

    @ClassRule
    public static LoggerContextRule ctx = new LoggerContextRule(CONFIG);

    Logger logger = LoggerFactory.getLogger("LoggerTest");
    XLogger xlogger = XLoggerFactory.getXLogger("LoggerTest");

    @Test
    public void basicFlow() {
        xlogger.entry();
        verify("List", "o.a.l.s.LoggerTest entry MDC{}" + Strings.LINE_SEPARATOR);
        xlogger.exit();
        verify("List", "o.a.l.s.LoggerTest exit MDC{}" + Strings.LINE_SEPARATOR);
    }

    @Test
    public void simpleFlow() {
        xlogger.entry(CONFIG);
        verify("List", "o.a.l.s.LoggerTest entry with (log4j-test1.xml) MDC{}" + Strings.LINE_SEPARATOR);
        xlogger.exit(0);
        verify("List", "o.a.l.s.LoggerTest exit with (0) MDC{}" + Strings.LINE_SEPARATOR);
    }

    @Test
    public void throwing() {
        xlogger.throwing(new IllegalArgumentException("Test Exception"));
        verify("List", "o.a.l.s.LoggerTest throwing MDC{}" + Strings.LINE_SEPARATOR);
    }

    @Test
    public void catching() {
        try {
            throw new NullPointerException();
        } catch (final Exception e) {
            xlogger.catching(e);
            verify("List", "o.a.l.s.LoggerTest catching MDC{}" + Strings.LINE_SEPARATOR);
        }
    }

    @Test
    public void debug() {
        logger.debug("Debug message");
        verify("List", "o.a.l.s.LoggerTest Debug message MDC{}" + Strings.LINE_SEPARATOR);
    }

    @Test
    public void debugNoParms() {
        logger.debug("Debug message {}");
        verify("List", "o.a.l.s.LoggerTest Debug message {} MDC{}" + Strings.LINE_SEPARATOR);
        logger.debug("Debug message {}", (Object[]) null);
        verify("List", "o.a.l.s.LoggerTest Debug message {} MDC{}" + Strings.LINE_SEPARATOR);
        ((LocationAwareLogger) logger)
                .log(null, Log4jLogger.class.getName(), LocationAwareLogger.DEBUG_INT, "Debug message {}", null, null);
        verify("List", "o.a.l.s.LoggerTest Debug message {} MDC{}" + Strings.LINE_SEPARATOR);
    }

    @Test
    public void debugWithParms() {
        logger.debug("Hello, {}", "World");
        verify("List", "o.a.l.s.LoggerTest Hello, World MDC{}" + Strings.LINE_SEPARATOR);
    }

    @Test
    public void mdc() {

        MDC.put("TestYear", "2010");
        logger.debug("Debug message");
        verify("List", "o.a.l.s.LoggerTest Debug message MDC{TestYear=2010}" + Strings.LINE_SEPARATOR);
        MDC.clear();
        logger.debug("Debug message");
        verify("List", "o.a.l.s.LoggerTest Debug message MDC{}" + Strings.LINE_SEPARATOR);
    }

    @Test
    public void mdcStack() {
        MDC.pushByKey("TestYear", "2010");
        logger.debug("Debug message");
        verify("List", "o.a.l.s.LoggerTest Debug message MDC{TestYear=2010}" + Strings.LINE_SEPARATOR);
        MDC.pushByKey("TestYear", "2011");
        logger.debug("Debug message");
        verify("List", "o.a.l.s.LoggerTest Debug message MDC{TestYear=2011}" + Strings.LINE_SEPARATOR);
        MDC.popByKey("TestYear");
        logger.debug("Debug message");
        verify("List", "o.a.l.s.LoggerTest Debug message MDC{TestYear=2010}" + Strings.LINE_SEPARATOR);
        MDC.clear();
        logger.debug("Debug message");
        verify("List", "o.a.l.s.LoggerTest Debug message MDC{}" + Strings.LINE_SEPARATOR);
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/LOG4J2-793">LOG4J2-793</a>
     */
    @Test
    public void supportsCustomSLF4JMarkers() {
        final Marker marker = new CustomFlatMarker("TEST");
        logger.debug(marker, "Test");
        verify("List", "o.a.l.s.LoggerTest Test MDC{}" + Strings.LINE_SEPARATOR);
    }

    @Test
    public void testRootLogger() {
        final Logger l = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        assertNotNull("No Root Logger", l);
        assertEquals(Logger.ROOT_LOGGER_NAME, l.getName());
    }

    @Test
    public void doubleSubst() {
        logger.debug("Hello, {}", "Log4j {}");
        verify("List", "o.a.l.s.LoggerTest Hello, Log4j {} MDC{}" + Strings.LINE_SEPARATOR);
        xlogger.debug("Hello, {}", "Log4j {}");
        verify("List", "o.a.l.s.LoggerTest Hello, Log4j {} MDC{}" + Strings.LINE_SEPARATOR);
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
        final ListAppender appender = ctx.getListAppender("UnformattedList");
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
        final ListAppender listApp = ctx.getListAppender(name);
        assertNotNull("Missing Appender", listApp);
        return listApp;
    }

    private void verify(final String name, final String expected) {
        final ListAppender listApp = getAppenderByName(name);
        final List<String> events = listApp.getMessages();
        assertTrue("Incorrect number of messages. Expected 1 Actual " + events.size(), events.size() == 1);
        final String actual = events.get(0);
        assertEquals("Incorrect message. Expected " + expected + ". Actual " + actual, expected, actual);
        listApp.clear();
    }

    private void verifyThrowable(final Throwable expected) {
        final ListAppender listApp = getAppenderByName("UnformattedList");
        final List<LogEvent> events = listApp.getEvents();
        assertEquals("Incorrect number of messages", 1, events.size());
        final LogEvent actual = events.get(0);
        assertEquals("Incorrect throwable.", expected, actual.getThrown());
        listApp.clear();
    }

    @Before
    @After
    public void cleanup() {
        MDC.clear();
        ctx.getListAppender("List").clear();
        ctx.getListAppender("UnformattedList").clear();
    }
}
