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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.apache.logging.log4j.junit.Named;
import org.apache.logging.log4j.junit.ReconfigurationPolicy;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.message.StringFormatterMessageFactory;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.MessageFactory2Adapter;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

@LoggerContextSource(value = LoggerTest.CONFIG, reconfigure = ReconfigurationPolicy.AFTER_EACH)
public class LoggerTest {

    static final String CONFIG = "log4j-test2.xml";
    org.apache.logging.log4j.Logger logger;
    org.apache.logging.log4j.Logger loggerChild;
    org.apache.logging.log4j.Logger loggerGrandchild;
    private final ListAppender app;
    private final ListAppender host;
    private final ListAppender noThrown;

    public LoggerTest(final LoggerContext context, @Named("List") final ListAppender app, @Named("HostTest") final ListAppender host, @Named("NoThrowable") final ListAppender noThrown) {
        logger = context.getLogger("LoggerTest");
        loggerChild = context.getLogger("LoggerTest.child");
        loggerGrandchild = context.getLogger("LoggerTest.child.grand");
        this.app = app.clear();
        this.host = host.clear();
        this.noThrown = noThrown.clear();
    }

    private void assertEventCount(final List<LogEvent> events, final int expected) {
        assertEquals(expected, events.size(), "Incorrect number of events.");
    }

    @Test
    public void builder() {
        logger.atDebug().withLocation().log("Hello");
        Marker marker = MarkerManager.getMarker("test");
        logger.atError().withMarker(marker).log("Hello {}", "John");
        logger.atWarn().withThrowable(new Throwable("This is a test")).log((Message) new SimpleMessage("Log4j rocks!"));
        final List<LogEvent> events = app.getEvents();
        assertEventCount(events, 3);
        assertEquals(
                "org.apache.logging.log4j.core.LoggerTest.builder(LoggerTest.java:81)", events.get(0).getSource().toString(),
                "Incorrect location");
        assertEquals(Level.DEBUG, events.get(0).getLevel(), "Incorrect Level");
        MatcherAssert.assertThat("Incorrect message", events.get(1).getMessage().getFormattedMessage(), equalTo("Hello John"));
        assertNotNull(events.get(2).getThrown(), "Missing Throwable");
    }

    @Test
    public void basicFlow() {
        logger.traceEntry();
        logger.traceExit();
        final List<LogEvent> events = app.getEvents();
        assertEventCount(events, 2);
    }

    @Test
    public void simpleFlow() {
        logger.entry(CONFIG);
        logger.traceExit(0);
        final List<LogEvent> events = app.getEvents();
        assertEventCount(events, 2);
    }

    @Test
    public void simpleFlowDepreacted() {
        logger.entry(CONFIG);
        logger.exit(0);
        final List<LogEvent> events = app.getEvents();
        assertEventCount(events, 2);
    }

    @Test
    public void throwing() {
        logger.throwing(new IllegalArgumentException("Test Exception"));
        final List<LogEvent> events = app.getEvents();
        assertEventCount(events, 1);
    }

    @Test
    public void catching() {
        try {
            throw new NullPointerException();
        } catch (final Exception e) {
            logger.catching(e);
        }
        final List<LogEvent> events = app.getEvents();
        assertEventCount(events, 1);
    }

    @Test
    public void debug() {
        logger.debug("Debug message");
        final List<LogEvent> events = app.getEvents();
        assertEventCount(events, 1);
    }

    @Test
    public void debugChangeLevel() {
        logger.debug("Debug message 1");
        assertEventCount(app.getEvents(), 1);
        Configurator.setLevel(logger.getName(), Level.OFF);
        logger.debug("Debug message 2");
        assertEventCount(app.getEvents(), 1);
        Configurator.setLevel(logger.getName(), Level.DEBUG);
        logger.debug("Debug message 3");
        assertEventCount(app.getEvents(), 2);
    }

    @Test
    public void debugChangeLevelAllChildrenLoggers() {
        // Use logger AND child loggers
        logger.debug("Debug message 1");
        loggerChild.debug("Debug message 1 child");
        loggerGrandchild.debug("Debug message 1 grandchild");
        assertEventCount(app.getEvents(), 3);
        Configurator.setAllLevels(logger.getName(), Level.OFF);
        logger.debug("Debug message 2");
        loggerChild.warn("Warn message 2 child");
        loggerGrandchild.fatal("Fatal message 2 grandchild");
        assertEventCount(app.getEvents(), 3);
        Configurator.setAllLevels(logger.getName(), Level.DEBUG);
        logger.debug("Debug message 3");
        loggerChild.warn("Trace message 3 child");
        loggerGrandchild.trace("Fatal message 3 grandchild");
        assertEventCount(app.getEvents(), 5);
    }

    @Test
    public void debugChangeLevelChildLogger() {
        // Use logger AND child loggers
        logger.debug("Debug message 1");
        loggerChild.debug("Debug message 1 child");
        loggerGrandchild.debug("Debug message 1 grandchild");
        assertEventCount(app.getEvents(), 3);
        Configurator.setLevel(logger.getName(), Level.OFF);
        logger.debug("Debug message 2");
        loggerChild.debug("Debug message 2 child");
        loggerGrandchild.debug("Debug message 2 grandchild");
        assertEventCount(app.getEvents(), 3);
        Configurator.setLevel(logger.getName(), Level.DEBUG);
        logger.debug("Debug message 3");
        loggerChild.debug("Debug message 3 child");
        loggerGrandchild.debug("Debug message 3 grandchild");
        assertEventCount(app.getEvents(), 6);
    }

    @Test
    public void debugChangeLevelsChildLoggers(final LoggerContext context) {
        final org.apache.logging.log4j.Logger loggerChild = context.getLogger(logger.getName() + ".child");
        // Use logger AND loggerChild
        logger.debug("Debug message 1");
        loggerChild.debug("Debug message 1 child");
        assertEventCount(app.getEvents(), 2);
        Configurator.setLevel(logger.getName(), Level.ERROR);
        Configurator.setLevel(loggerChild.getName(), Level.DEBUG);
        logger.debug("Debug message 2");
        loggerChild.debug("Debug message 2 child");
        assertEventCount(app.getEvents(), 3);
        Configurator.setLevel(logger.getName(), Level.DEBUG);
        logger.debug("Debug message 3");
        loggerChild.debug("Debug message 3 child");
        assertEventCount(app.getEvents(), 5);
    }

    @Test
    public void debugChangeLevelsMap() {
        logger.debug("Debug message 1");
        assertEventCount(app.getEvents(), 1);
        final Map<String, Level> map = new HashMap<>();
        map.put(logger.getName(), Level.OFF);
        Configurator.setLevel(map);
        logger.debug("Debug message 2");
        assertEventCount(app.getEvents(), 1);
        map.put(logger.getName(), Level.DEBUG);
        Configurator.setLevel(map);
        logger.debug("Debug message 3");
        assertEventCount(app.getEvents(), 2);
    }

    @Test
    public void debugChangeLevelsMapChildLoggers() {
        logger.debug("Debug message 1");
        loggerChild.debug("Debug message 1 C");
        loggerGrandchild.debug("Debug message 1 GC");
        assertEventCount(app.getEvents(), 3);
        final Map<String, Level> map = new HashMap<>();
        map.put(logger.getName(), Level.OFF);
        map.put(loggerChild.getName(), Level.DEBUG);
        map.put(loggerGrandchild.getName(), Level.WARN);
        Configurator.setLevel(map);
        logger.debug("Debug message 2");
        loggerChild.debug("Debug message 2 C");
        loggerGrandchild.debug("Debug message 2 GC");
        assertEventCount(app.getEvents(), 4);
        map.put(logger.getName(), Level.DEBUG);
        map.put(loggerChild.getName(), Level.OFF);
        map.put(loggerGrandchild.getName(), Level.DEBUG);
        Configurator.setLevel(map);
        logger.debug("Debug message 3");
        loggerChild.debug("Debug message 3 C");
        loggerGrandchild.debug("Debug message 3 GC");
        assertEventCount(app.getEvents(), 6);
    }

    @Test
    public void debugChangeRootLevel() {
        logger.debug("Debug message 1");
        assertEventCount(app.getEvents(), 1);
        Configurator.setRootLevel(Level.OFF);
        logger.debug("Debug message 2");
        assertEventCount(app.getEvents(), 1);
        Configurator.setRootLevel(Level.DEBUG);
        logger.debug("Debug message 3");
        assertEventCount(app.getEvents(), 2);
    }

    @Test
    public void getLogger_String_MessageFactoryMismatch(final TestInfo testInfo) {
        final Logger testLogger = testMessageFactoryMismatch(testInfo.getTestMethod().map(Method::getName).orElseThrow(AssertionError::new),
                StringFormatterMessageFactory.INSTANCE, ParameterizedMessageFactory.INSTANCE);
        testLogger.debug("%,d", Integer.MAX_VALUE);
        final List<LogEvent> events = app.getEvents();
        assertEventCount(events, 1);
        assertEquals(String.format("%,d", Integer.MAX_VALUE), events.get(0).getMessage().getFormattedMessage());
    }

    @Test
    public void getLogger_String_MessageFactoryMismatchNull(final TestInfo testInfo) {
        final Logger testLogger =  testMessageFactoryMismatch(testInfo.getTestMethod().map(Method::getName).orElseThrow(AssertionError::new), StringFormatterMessageFactory.INSTANCE, null);
        testLogger.debug("%,d", Integer.MAX_VALUE);
        final List<LogEvent> events = app.getEvents();
        assertEventCount(events, 1);
        assertEquals(String.format("%,d", Integer.MAX_VALUE), events.get(0).getMessage().getFormattedMessage());
    }

    private static Logger testMessageFactoryMismatch(final String name,
                                                     final MessageFactory messageFactory1,
            final MessageFactory messageFactory2) {
        final Logger testLogger1 = (Logger) LogManager.getLogger(name, messageFactory1);
        assertNotNull(testLogger1);
        checkMessageFactory(messageFactory1, testLogger1);
        final Logger testLogger2 = (Logger) LogManager.getLogger(name, messageFactory2);
        assertNotNull(testLogger2);
        checkMessageFactory(messageFactory2, testLogger2);
        return testLogger1;
    }

    private static void checkMessageFactory(final MessageFactory messageFactory1, final Logger testLogger1) {
        if (messageFactory1 == null) {
            assertEquals(AbstractLogger.DEFAULT_MESSAGE_FACTORY_CLASS, testLogger1.getMessageFactory().getClass());
        } else {
            MessageFactory actual = testLogger1.getMessageFactory();
            if (actual instanceof MessageFactory2Adapter) {
                actual = ((MessageFactory2Adapter) actual).getOriginal();
            }
            assertEquals(messageFactory1, actual);
        }
    }

    @Test
    public void debugObject() {
        logger.debug(new Date());
        final List<LogEvent> events = app.getEvents();
        assertEventCount(events, 1);
    }

    @Test
    public void debugWithParms() {
        logger.debug("Hello, {}", "World");
        final List<LogEvent> events = app.getEvents();
        assertEventCount(events, 1);
    }

    @Test
    public void testImpliedThrowable(final LoggerContext context) {
        final org.apache.logging.log4j.Logger testLogger = context.getLogger("org.apache.logging.log4j.hosttest");
        testLogger.debug("This is a test", new Throwable("Testing"));
        final List<String> msgs = host.getMessages();
        assertEquals(1, msgs.size(), "Incorrect number of messages. Expected 1, actual " + msgs.size());
        final String expected = "java.lang.Throwable: Testing";
        assertTrue(msgs.get(0).contains(expected), "Incorrect message data");
    }


    @Test
    public void testSuppressedThrowable(final LoggerContext context) {
        final org.apache.logging.log4j.Logger testLogger = context.getLogger("org.apache.logging.log4j.nothrown");
        testLogger.debug("This is a test", new Throwable("Testing"));
        final List<String> msgs = noThrown.getMessages();
        assertEquals(1, msgs.size(), "Incorrect number of messages. Expected 1, actual " + msgs.size());
        final String suppressed = "java.lang.Throwable: Testing";
        assertFalse(msgs.get(0).contains(suppressed), "Incorrect message data");
    }


    @Test
    public void mdc() {
        ThreadContext.put("TestYear", "2010");
        logger.debug("Debug message");
        ThreadContext.clearMap();
        logger.debug("Debug message");
        final List<LogEvent> events = app.getEvents();
        assertEventCount(events, 2);
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
        assertEventCount(events, 1);
    }

    @Test
    public void testReconfiguration(final LoggerContext context) throws Exception {
        final Configuration oldConfig = context.getConfiguration();
        final int MONITOR_INTERVAL_SECONDS = 5;
        final File file = new File("target/test-classes/" + CONFIG);
        final long orig = file.lastModified();
        final long newTime = orig + 10000;
        assertTrue(file.setLastModified(newTime), "setLastModified should have succeeded.");
        TimeUnit.SECONDS.sleep(MONITOR_INTERVAL_SECONDS + 1);
        for (int i = 0; i < 17; ++i) {
            logger.debug("Reconfigure");
        }
        Thread.sleep(100);
        for (int i = 0; i < 20; i++) {
            if (context.getConfiguration() != oldConfig) {
                break;
            }
            Thread.sleep(50);
        }
        final Configuration newConfig = context.getConfiguration();
        assertNotNull(newConfig, "No configuration");
        assertNotSame(newConfig, oldConfig, "Reconfiguration failed");
    }

    @Test
    public void testAdditivity(final LoggerContext context) throws Exception {
        final Logger localLogger = context.getLogger("org.apache.test");
        localLogger.error("Test parent additivity");
        final List<LogEvent> events = app.getEvents();
        assertEventCount(events, 1);
    }

    @Test
    public void testLevelInheritance(final LoggerContext context) throws Exception {
        final Configuration config = context.getConfiguration();
        final LoggerConfig loggerConfig = config.getLoggerConfig("org.apache.logging.log4j.core.LoggerTest");
        assertNotNull(loggerConfig);
        assertEquals(loggerConfig.getName(), "org.apache.logging.log4j.core.LoggerTest");
        assertEquals(loggerConfig.getLevel(), Level.DEBUG);
        final Logger localLogger = context.getLogger("org.apache.logging.log4j.core.LoggerTest");
        assertSame(localLogger.getLevel(), Level.DEBUG, "Incorrect level - expected DEBUG, actual " + localLogger.getLevel());
    }

    @Test
    public void paramWithExceptionTest() throws Exception {
        logger.error("Throwing with parameters {}", "TestParam", new NullPointerException("Test Exception"));
        final List<LogEvent> events = app.getEvents();
        assertNotNull(events, "Log event list not returned");
        assertEquals(1, events.size(), "Incorrect number of log events");
        final LogEvent event = events.get(0);
        final Throwable thrown = event.getThrown();
        assertNotNull(thrown, "No throwable present in log event");
        final Message msg = event.getMessage();
        assertEquals("Throwing with parameters {}", msg.getFormat());
        assertEquals("Throwing with parameters TestParam", msg.getFormattedMessage());
        assertArrayEquals(new Object[] { "TestParam", thrown }, msg.getParameters());
    }
}

