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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.helpers.Constants;
import org.apache.logging.log4j.core.helpers.NetUtils;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.logging.log4j.message.StringFormatterMessageFactory;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class LoggerTest {

    private static final String CONFIG = "log4j-test2.xml";
    private static Configuration config;
    private static ListAppender app;
    private static ListAppender host;
    private static ListAppender noThrown;
    private static LoggerContext ctx;

    @BeforeClass
    public static void setupClass() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        ctx = (LoggerContext) LogManager.getContext(false);
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

    @Before
    public void before() {
        config = ctx.getConfiguration();
        for (final Map.Entry<String, Appender> entry : config.getAppenders().entrySet()) {
            if (entry.getKey().equals("List")) {
                app = (ListAppender) entry.getValue();
            } else if (entry.getKey().equals("HostTest")) {
                host = (ListAppender) entry.getValue();
            } else if (entry.getKey().equals("NoThrowable")) {
                noThrown = (ListAppender) entry.getValue();
            }
        }
        assertNotNull("No Appender", app);
        assertNotNull("No Host Appender", host);
        app.clear();
        host.clear();
    }


    org.apache.logging.log4j.Logger logger = LogManager.getLogger("LoggerTest");

    @Test
    public void basicFlow() {
        logger.entry();
        logger.exit();
        final List<LogEvent> events = app.getEvents();
        assertTrue("Incorrect number of events. Expected 2, actual " + events.size(), events.size() == 2);
        app.clear();
    }

    @Test
    public void simpleFlow() {
        logger.entry(CONFIG);
        logger.exit(0);
        final List<LogEvent> events = app.getEvents();
        assertTrue("Incorrect number of events. Expected 2, actual " + events.size(), events.size() == 2);
        app.clear();
    }

    @Test
    public void throwing() {
        logger.throwing(new IllegalArgumentException("Test Exception"));
        final List<LogEvent> events = app.getEvents();
        assertTrue("Incorrect number of events. Expected 1, actual " + events.size(), events.size() == 1);
        app.clear();
    }

    @Test
    public void catching() {
        try {
            throw new NullPointerException();
        } catch (final Exception e) {
            logger.catching(e);
        }
        final List<LogEvent> events = app.getEvents();
        assertTrue("Incorrect number of events. Expected 1, actual " + events.size(), events.size() == 1);
        app.clear();
    }

    @Test
    public void debug() {
        logger.debug("Debug message");
        final List<LogEvent> events = app.getEvents();
        assertTrue("Incorrect number of events. Expected 1, actual " + events.size(), events.size() == 1);
        app.clear();
    }

    @Test
    public void getLogger_String_MessageFactoryMismatch() {
        final Logger testLogger = testMessageFactoryMismatch("getLogger_String_MessageFactoryMismatch",
                StringFormatterMessageFactory.INSTANCE, ParameterizedMessageFactory.INSTANCE);
        testLogger.debug("%,d", Integer.MAX_VALUE);
        final List<LogEvent> events = app.getEvents();
        assertTrue("Incorrect number of events. Expected 1, actual " + events.size(), events.size() == 1);
        assertEquals(String.format("%,d", Integer.MAX_VALUE), events.get(0).getMessage().getFormattedMessage());
    }

    @Test
    public void getLogger_String_MessageFactoryMismatchNull() {
        final Logger testLogger =  testMessageFactoryMismatch("getLogger_String_MessageFactoryMismatchNull", StringFormatterMessageFactory.INSTANCE, null);
        testLogger.debug("%,d", Integer.MAX_VALUE);
        final List<LogEvent> events = app.getEvents();
        assertTrue("Incorrect number of events. Expected 1, actual " + events.size(), events.size() == 1);
        assertEquals(String.format("%,d", Integer.MAX_VALUE), events.get(0).getMessage().getFormattedMessage());
    }

    private Logger testMessageFactoryMismatch(final String name, final MessageFactory messageFactory1, final MessageFactory messageFactory2) {
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
        assertTrue("Incorrect number of events. Expected 1, actual " + events.size(), events.size() == 1);
        app.clear();
    }

    @Test
    public void debugWithParms() {
        logger.debug("Hello, {}", "World");
        final List<LogEvent> events = app.getEvents();
        assertTrue("Incorrect number of events. Expected 1, actual " + events.size(), events.size() == 1);
        app.clear();
    }

    @Test
    public void testHostname() {
        final org.apache.logging.log4j.Logger testLogger = LogManager.getLogger("org.apache.logging.log4j.hosttest");
        testLogger.debug("Hello, {}", "World");
        final List<String> msgs = host.getMessages();
        assertTrue("Incorrect number of events. Expected 1, actual " + msgs.size(), msgs.size() == 1);
        final String expected = NetUtils.getLocalHostname() + Constants.LINE_SEP;
        assertTrue("Incorrect hostname - expected " + expected + " actual - " + msgs.get(0),
            msgs.get(0).endsWith(expected));

    }

    @Test
    public void testImpliedThrowable() {
        final org.apache.logging.log4j.Logger testLogger = LogManager.getLogger("org.apache.logging.log4j.hosttest");
        testLogger.debug("This is a test", new Throwable("Testing"));
        final List<String> msgs = host.getMessages();
        assertTrue("Incorrect number of messages. Expected 1, actual " + msgs.size(), msgs.size() == 1);
        final String expected = "java.lang.Throwable: Testing";
        assertTrue("Incorrect message data", msgs.get(0).contains(expected));
    }


    @Test
    public void testSuppressedThrowable() {
        final org.apache.logging.log4j.Logger testLogger = LogManager.getLogger("org.apache.logging.log4j.nothrown");
        testLogger.debug("This is a test", new Throwable("Testing"));
        final List<String> msgs = noThrown.getMessages();
        assertTrue("Incorrect number of messages. Expected 1, actual " + msgs.size(), msgs.size() == 1);
        final String suppressed = "java.lang.Throwable: Testing";
        assertTrue("Incorrect message data", !msgs.get(0).contains(suppressed));
    }


    @Test
    public void mdc() {
        ThreadContext.put("TestYear", "2010");
        logger.debug("Debug message");
        ThreadContext.clear();
        logger.debug("Debug message");
        final List<LogEvent> events = app.getEvents();
        assertTrue("Incorrect number of events. Expected 2, actual " + events.size(), events.size() == 2);
        app.clear();
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
        ThreadContext.clear();
        final List<LogEvent> events = app.getEvents();
        assertTrue("Incorrect number of events. Expected 1, actual " + events.size(), events.size() == 1);
        app.clear();
    }

    @Test
    public void testReconfiguration() throws Exception {
        final File file = new File("target/test-classes/" + CONFIG);
        final long orig = file.lastModified();
        final long newTime = orig + 10000;
        file.setLastModified(newTime);
        Thread.sleep(6000);
        for (int i = 0; i < 17; ++i) {
            logger.debug("Reconfigure");
        }
        final Configuration cfg = ctx.getConfiguration();
        assertNotNull("No configuration", cfg);
        assertTrue("Reconfiguration failed", cfg != config);
    }

    @Test
    public void testAdditivity() throws Exception {
        final Logger localLogger = (Logger) LogManager.getLogger("org.apache.test");
        localLogger.error("Test parent additivity");
        final List<LogEvent> events = app.getEvents();
        assertTrue("Incorrect number of events. Expected 1, actual " + events.size(), events.size() == 1);
    }
}

