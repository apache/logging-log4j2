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
package org.apache.logging.log4j.core;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.junit.InitialLoggerContext;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.logging.log4j.message.StringFormatterMessageFactory;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class LoggerTest {

    private static final String CONFIG = "log4j-test2.xml";
    private ListAppender app;
    private ListAppender host;
    private ListAppender noThrown;

    @ClassRule
    public static InitialLoggerContext context = new InitialLoggerContext(CONFIG);

    @Before
    public void before() {
        app = context.getListAppender("List").clear();
        host = context.getListAppender("HostTest").clear();
        noThrown = context.getListAppender("NoThrowable").clear();
    }


    org.apache.logging.log4j.Logger logger = context.getLogger("LoggerTest");

    @Test
    public void basicFlow() {
        logger.entry();
        logger.exit();
        final List<LogEvent> events = app.getEvents();
        assertEquals("Incorrect number of events. Expected 2, actual " + events.size(), 2, events.size());
    }

    @Test
    public void simpleFlow() {
        logger.entry(CONFIG);
        logger.exit(0);
        final List<LogEvent> events = app.getEvents();
        assertEquals("Incorrect number of events. Expected 2, actual " + events.size(), 2, events.size());
    }

    @Test
    public void throwing() {
        logger.throwing(new IllegalArgumentException("Test Exception"));
        final List<LogEvent> events = app.getEvents();
        assertEquals("Incorrect number of events. Expected 1, actual " + events.size(), 1, events.size());
    }

    @Test
    public void catching() {
        try {
            throw new NullPointerException();
        } catch (final Exception e) {
            logger.catching(e);
        }
        final List<LogEvent> events = app.getEvents();
        assertEquals("Incorrect number of events. Expected 1, actual " + events.size(), 1, events.size());
    }

    @Test
    public void debug() {
        logger.debug("Debug message");
        final List<LogEvent> events = app.getEvents();
        assertEquals("Incorrect number of events. Expected 1, actual " + events.size(), 1, events.size());
    }

    @Test
    public void getLogger_String_MessageFactoryMismatch() {
        final Logger testLogger = testMessageFactoryMismatch("getLogger_String_MessageFactoryMismatch",
                StringFormatterMessageFactory.INSTANCE, ParameterizedMessageFactory.INSTANCE);
        testLogger.debug("%,d", Integer.MAX_VALUE);
        final List<LogEvent> events = app.getEvents();
        assertEquals("Incorrect number of events. Expected 1, actual " + events.size(), 1, events.size());
        assertEquals(String.format("%,d", Integer.MAX_VALUE), events.get(0).getMessage().getFormattedMessage());
    }

    @Test
    public void getLogger_String_MessageFactoryMismatchNull() {
        final Logger testLogger =  testMessageFactoryMismatch("getLogger_String_MessageFactoryMismatchNull", StringFormatterMessageFactory.INSTANCE, null);
        testLogger.debug("%,d", Integer.MAX_VALUE);
        final List<LogEvent> events = app.getEvents();
        assertEquals("Incorrect number of events. Expected 1, actual " + events.size(), 1, events.size());
        assertEquals(String.format("%,d", Integer.MAX_VALUE), events.get(0).getMessage().getFormattedMessage());
    }

    private static Logger testMessageFactoryMismatch(final String name,
                                                     final MessageFactory messageFactory1,
                                                     final MessageFactory messageFactory2) {
        final Logger testLogger = (Logger) LogManager.getLogger(name, messageFactory1);
        assertNotNull(testLogger);
        assertEquals(messageFactory1, testLogger.getMessageFactory());
        final Logger testLogger2 = (Logger) LogManager.getLogger(name, messageFactory2);
        assertEquals(messageFactory1, testLogger2.getMessageFactory());
        return testLogger;
    }

    @Test
    public void debugObject() {
        logger.debug(new Date());
        final List<LogEvent> events = app.getEvents();
        assertEquals("Incorrect number of events. Expected 1, actual " + events.size(), 1, events.size());
    }

    @Test
    public void debugWithParms() {
        logger.debug("Hello, {}", "World");
        final List<LogEvent> events = app.getEvents();
        assertEquals("Incorrect number of events. Expected 1, actual " + events.size(), 1, events.size());
    }

    @Test
    public void testImpliedThrowable() {
        final org.apache.logging.log4j.Logger testLogger = context.getLogger("org.apache.logging.log4j.hosttest");
        testLogger.debug("This is a test", new Throwable("Testing"));
        final List<String> msgs = host.getMessages();
        assertEquals("Incorrect number of messages. Expected 1, actual " + msgs.size(), 1, msgs.size());
        final String expected = "java.lang.Throwable: Testing";
        assertTrue("Incorrect message data", msgs.get(0).contains(expected));
    }


    @Test
    public void testSuppressedThrowable() {
        final org.apache.logging.log4j.Logger testLogger = context.getLogger("org.apache.logging.log4j.nothrown");
        testLogger.debug("This is a test", new Throwable("Testing"));
        final List<String> msgs = noThrown.getMessages();
        assertEquals("Incorrect number of messages. Expected 1, actual " + msgs.size(), 1, msgs.size());
        final String suppressed = "java.lang.Throwable: Testing";
        assertTrue("Incorrect message data", !msgs.get(0).contains(suppressed));
    }


    @Test
    public void mdc() {
        ThreadContext.put("TestYear", "2010");
        logger.debug("Debug message");
        ThreadContext.clearMap();
        logger.debug("Debug message");
        final List<LogEvent> events = app.getEvents();
        assertEquals("Incorrect number of events. Expected 2, actual " + events.size(), 2, events.size());
    }

    @Test
    public void structuredData() {
        ThreadContext.put("loginId", "JohnDoe");
        ThreadContext.put("ipAddress", "192.168.0.120");
        ThreadContext.put("locale", Locale.US.getDisplayName());
        final StructuredDataMessage msg = new StructuredDataMessage("Audit@18060", "Transfer Complete", "Transfer");
        msg.put("ToAccount", "123456");
        msg.put("FromAccount", "123457");
        msg.put("Amount", "200.00");
        logger.info(MarkerManager.getMarker("EVENT"), msg);
        ThreadContext.clearMap();
        final List<LogEvent> events = app.getEvents();
        assertEquals("Incorrect number of events. Expected 1, actual " + events.size(), 1, events.size());
    }

    @Test
    public void testReconfiguration() throws Exception {
        final Configuration oldConfig = context.getConfiguration();
        final int MONITOR_INTERVAL_SECONDS = 5;
        final File file = new File("target/test-classes/" + CONFIG);
        final long orig = file.lastModified();
        final long newTime = orig + 10000;
        assertTrue("setLastModified should have succeeded.", file.setLastModified(newTime));
        TimeUnit.SECONDS.sleep(MONITOR_INTERVAL_SECONDS + 1);
        for (int i = 0; i < 17; ++i) {
            logger.debug("Reconfigure");
        }
        Thread.sleep(100);
        for (int i = 0; i < 20; i++) {
            if (context.getConfiguration() == oldConfig) {
                break;
            }
            Thread.sleep(50);
        }
        final Configuration newConfig = context.getConfiguration();
        assertNotNull("No configuration", newConfig);
        assertNotSame("Reconfiguration failed", newConfig, oldConfig);
    }

    @Test
    public void testAdditivity() throws Exception {
        final Logger localLogger = context.getLogger("org.apache.test");
        localLogger.error("Test parent additivity");
        final List<LogEvent> events = app.getEvents();
        assertEquals("Incorrect number of events. Expected 1, actual " + events.size(), 1, events.size());
    }

    @Test
    public void testLevelInheritence() throws Exception {
        final Configuration config = context.getConfiguration();
        final LoggerConfig loggerConfig = config.getLoggerConfig("org.apache.logging.log4j.core.LoggerTest");
        assertNotNull(loggerConfig);
        assertEquals(loggerConfig.getName(), "org.apache.logging.log4j.core.LoggerTest");
        assertEquals(loggerConfig.getLevel(), Level.DEBUG);
        final Logger localLogger = context.getLogger("org.apache.logging.log4j.core.LoggerTest");
        assertTrue("Incorrect level - expected DEBUG, actual " + localLogger.getLevel(), localLogger.getLevel() == Level.DEBUG);
    }
}

