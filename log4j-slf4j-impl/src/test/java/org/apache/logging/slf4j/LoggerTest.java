/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.slf4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.apache.logging.log4j.util.Strings;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.ext.EventData;
import org.slf4j.ext.EventLogger;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

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
        ((LocationAwareLogger)logger).log(null, Log4jLogger.class.getName(), LocationAwareLogger.DEBUG_INT,
            "Debug message {}", null, null);
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
        verify("List", "o.a.l.s.LoggerTest Hello, Log4j Log4j {} MDC{}" + Strings.LINE_SEPARATOR);
    }

    @Test
    public void testEventLogger() {
        MDC.put("loginId", "JohnDoe");
        MDC.put("ipAddress", "192.168.0.120");
        MDC.put("locale", Locale.US.getDisplayName());
        final EventData data = new EventData();
        data.setEventType("Transfer");
        data.setEventId("Audit@18060");
        data.setMessage("Transfer Complete");
        data.put("ToAccount", "123456");
        data.put("FromAccount", "123457");
        data.put("Amount", "200.00");
        EventLogger.logEvent(data);
        MDC.clear();
        verify("EventLogger", "o.a.l.s.LoggerTest Transfer [Audit@18060 Amount=\"200.00\" FromAccount=\"123457\" ToAccount=\"123456\"] Transfer Complete" + Strings.LINE_SEPARATOR);
    }

    private void verify(final String name, final String expected) {
        final ListAppender listApp = ctx.getListAppender(name);
        assertNotNull("Missing Appender", listApp);
        final List<String> events = listApp.getMessages();
        assertTrue("Incorrect number of messages. Expected 1 Actual " + events.size(), events.size()== 1);
        final String actual = events.get(0);
        assertEquals("Incorrect message. Expected " + expected + ". Actual " + actual, expected, actual);
        listApp.clear();
    }

    @Before
    @After
    public void cleanup() {
        MDC.clear();
        ctx.getListAppender("List").clear();
        ctx.getListAppender("EventLogger").clear();
    }
}
