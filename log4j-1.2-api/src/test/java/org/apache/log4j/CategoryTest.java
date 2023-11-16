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
package org.apache.log4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.log4j.bridge.AppenderAdapter;
import org.apache.log4j.bridge.AppenderWrapper;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.Constants;
import org.apache.logging.log4j.util.Strings;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests of Category.
 */
public class CategoryTest {

    static ConfigurationFactory cf = new BasicConfigurationFactory();

    private static final String VERSION1_APPENDER_NAME = "Version1List";
    private static final String VERSION2_APPENDER_NAME = "List";
    private static final ListAppender appender = new ListAppender(VERSION2_APPENDER_NAME);
    private static final org.apache.log4j.ListAppender version1Appender = new org.apache.log4j.ListAppender();

    @BeforeClass
    public static void setupClass() {
        appender.start();
        version1Appender.setName(VERSION1_APPENDER_NAME);
        ConfigurationFactory.setConfigurationFactory(cf);
        LoggerContext.getContext().reconfigure();
    }

    @AfterClass
    public static void cleanupClass() {
        ConfigurationFactory.removeConfigurationFactory(cf);
        appender.stop();
    }

    @Before
    public void before() {
        appender.clear();
    }

    @Test
    public void testExist() {
        assertNull(Category.exists("Does not exist for sure"));
    }

    /**
     * Tests Category.forcedLog.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testForcedLog() {
        final MockCategory category = new MockCategory("org.example.foo");
        category.setAdditivity(false);
        category.setHierarchy(LogManager.getHierarchy());
        ((org.apache.logging.log4j.core.Logger) category.getLogger()).addAppender(appender);
        // Logging a String
        category.info("Hello, World");
        List<LogEvent> list = appender.getEvents();
        int events = list.size();
        assertTrue("Number of events should be 1, was " + events, events == 1);
        LogEvent event = list.get(0);
        Message msg = event.getMessage();
        assertNotNull("No message", msg);
        // LOG4J2-3080: use message type consistently
        assertTrue("Incorrect Message type", msg instanceof SimpleMessage);
        assertEquals("Hello, World", msg.getFormat());
        appender.clear();
        // Logging a String map
        category.info(Collections.singletonMap("hello", "world"));
        list = appender.getEvents();
        events = list.size();
        assertTrue("Number of events should be 1, was " + events, events == 1);
        event = list.get(0);
        msg = event.getMessage();
        assertNotNull("No message", msg);
        assertTrue("Incorrect Message type", msg instanceof MapMessage);
        Object[] objects = msg.getParameters();
        assertEquals("world", objects[0]);
        appender.clear();
        // Logging a generic map
        category.info(Collections.singletonMap(1234L, "world"));
        list = appender.getEvents();
        events = list.size();
        assertTrue("Number of events should be 1, was " + events, events == 1);
        event = list.get(0);
        msg = event.getMessage();
        assertNotNull("No message", msg);
        assertTrue("Incorrect Message type", msg instanceof MapMessage);
        objects = msg.getParameters();
        assertEquals("world", objects[0]);
        appender.clear();
        // Logging an object
        final Object obj = new Object();
        category.info(obj);
        list = appender.getEvents();
        events = list.size();
        assertTrue("Number of events should be 1, was " + events, events == 1);
        event = list.get(0);
        msg = event.getMessage();
        assertNotNull("No message", msg);
        assertTrue("Incorrect Message type", msg instanceof ObjectMessage);
        objects = msg.getParameters();
        assertEquals(obj, objects[0]);
        appender.clear();

        category.log(Priority.INFO, "Hello, World");
        list = appender.getEvents();
        events = list.size();
        assertTrue("Number of events should be 1, was " + events, events == 1);
        event = list.get(0);
        msg = event.getMessage();
        assertNotNull("No message", msg);
        assertTrue("Incorrect Message type", msg instanceof SimpleMessage);
        assertEquals("Hello, World", msg.getFormat());
        appender.clear();
    }

    /**
     * Tests that the return type of getChainedPriority is Priority.
     *
     * @throws Exception thrown if Category.getChainedPriority can not be found.
     */
    @Test
    public void testGetChainedPriorityReturnType() throws Exception {
        final Method method = Category.class.getMethod("getChainedPriority", (Class[]) null);
        assertTrue(method.getReturnType() == Priority.class);
    }

    /**
     * Tests l7dlog(Priority, String, Throwable).
     */
    @Test
    public void testL7dlog() {
        final Logger logger = Logger.getLogger("org.example.foo");
        logger.setLevel(Level.ERROR);
        final Priority debug = Level.DEBUG;
        logger.l7dlog(debug, "Hello, World", null);
        assertTrue(appender.getEvents().isEmpty());
    }

    /**
     * Tests l7dlog(Priority, String, Object[], Throwable).
     */
    @Test
    public void testL7dlog4Param() {
        final Logger logger = Logger.getLogger("org.example.foo");
        logger.setLevel(Level.ERROR);
        final Priority debug = Level.DEBUG;
        logger.l7dlog(debug, "Hello, World", Constants.EMPTY_OBJECT_ARRAY, null);
        assertTrue(appender.getEvents().isEmpty());
    }

    /**
     * Test using a pre-existing Log4j 2 logger
     */
    @Test
    public void testExistingLog4j2Logger() {
        // create the logger using LogManager
        org.apache.logging.log4j.LogManager.getLogger("existingLogger");
        // Logger will be the one created above
        final Logger logger = Logger.getLogger("existingLogger");
        final Logger l2 = LogManager.getLogger("existingLogger");
        assertEquals(logger, l2);
        logger.setLevel(Level.ERROR);
        final Priority debug = Level.DEBUG;
        // the next line will throw an exception if the LogManager loggers
        // aren't supported by 1.2 Logger/Category
        logger.l7dlog(debug, "Hello, World", Constants.EMPTY_OBJECT_ARRAY, null);
        assertTrue(appender.getEvents().isEmpty());
    }

    /**
     * Tests setPriority(Priority).
     *
     * @deprecated
     */
    @Deprecated
    @Test
    public void testSetPriority() {
        final Logger logger = Logger.getLogger("org.example.foo");
        final Priority debug = Level.DEBUG;
        logger.setPriority(debug);
    }

    /**
     * Tests setPriority(Priority).
     *
     * @deprecated
     */
    @Deprecated
    @Test
    public void testSetPriorityNull() {
        Logger.getLogger("org.example.foo").setPriority(null);
    }

    @Test
    public void testClassName() {
        final Category category = Category.getInstance("TestCategory");
        final Layout<String> layout =
                PatternLayout.newBuilder().withPattern("%d %p %C{1.} [%t] %m%n").build();
        final ListAppender appender = new ListAppender("List2", null, layout, false, false);
        appender.start();
        category.setAdditivity(false);
        ((org.apache.logging.log4j.core.Logger) category.getLogger()).addAppender(appender);
        category.error("Test Message");
        final List<String> msgs = appender.getMessages();
        assertTrue("Incorrect number of messages. Expected 1 got " + msgs.size(), msgs.size() == 1);
        final String msg = msgs.get(0);
        appender.clear();
        final String threadName = Thread.currentThread().getName();
        final String expected = "ERROR o.a.l.CategoryTest [" + threadName + "] Test Message" + Strings.LINE_SEPARATOR;
        assertTrue(
                "Incorrect message " + Strings.dquote(msg) + " expected " + Strings.dquote(expected),
                msg.endsWith(expected));
    }

    @Test
    public void testStringLog() {
        final String payload = "payload";
        testMessageImplementation(
                payload, SimpleMessage.class, message -> assertEquals(message.getFormattedMessage(), payload));
    }

    @Test
    public void testCharSequenceLog() {
        final CharSequence payload = new CharSequence() {

            @Override
            public int length() {
                return 3;
            }

            @Override
            public char charAt(final int index) {
                return "abc".charAt(index);
            }

            @Override
            public CharSequence subSequence(final int start, final int end) {
                return "abc".subSequence(start, end);
            }

            @Override
            public String toString() {
                return "abc";
            }
        };
        testMessageImplementation(
                payload,
                SimpleMessage.class,
                message -> assertEquals(message.getFormattedMessage(), payload.toString()));
    }

    @Test
    public void testMapLog() {
        final String key = "key";
        final Object value = 0xDEADBEEF;
        final Map<String, Object> payload = Collections.singletonMap(key, value);
        testMessageImplementation(payload, MapMessage.class, message -> assertEquals(message.getData(), payload));
    }

    @Test
    public void testObjectLog() {
        final Object payload = new Object();
        testMessageImplementation(
                payload, ObjectMessage.class, message -> assertEquals(message.getParameter(), payload));
    }

    private <M extends Message> void testMessageImplementation(
            final Object messagePayload, final Class<M> expectedMessageClass, final Consumer<M> messageTester) {

        // Setup the logger and the appender.
        final Category category = Category.getInstance("TestCategory");
        final org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) category.getLogger();
        logger.addAppender(appender);

        // Log the message payload.
        category.info(messagePayload);

        // Verify collected log events.
        final List<LogEvent> events = appender.getEvents();
        assertEquals("was expecting a single event", 1, events.size());
        final LogEvent logEvent = events.get(0);

        // Verify the collected message.
        final Message message = logEvent.getMessage();
        final Class<? extends Message> actualMessageClass = message.getClass();
        assertTrue(
                "was expecting message to be instance of " + expectedMessageClass + ", found: " + actualMessageClass,
                expectedMessageClass.isAssignableFrom(actualMessageClass));
        @SuppressWarnings("unchecked")
        final M typedMessage = (M) message;
        messageTester.accept(typedMessage);
    }

    @Test
    public void testAddAppender() {
        try {
            final Logger rootLogger = LogManager.getRootLogger();
            int count = version1Appender.getEvents().size();
            rootLogger.addAppender(version1Appender);
            final Logger logger = LogManager.getLogger(CategoryTest.class);
            final org.apache.log4j.ListAppender appender = new org.apache.log4j.ListAppender();
            appender.setName("appender2");
            logger.addAppender(appender);
            // Root logger
            rootLogger.info("testAddLogger");
            assertEquals(
                    "adding at root works",
                    ++count,
                    version1Appender.getEvents().size());
            assertEquals("adding at child works", 0, appender.getEvents().size());
            // Another logger
            logger.info("testAddLogger2");
            assertEquals(
                    "adding at root works",
                    ++count,
                    version1Appender.getEvents().size());
            assertEquals("adding at child works", 1, appender.getEvents().size());
            // Call appenders
            final LoggingEvent event = new LoggingEvent();
            logger.callAppenders(event);
            assertEquals("callAppenders", ++count, version1Appender.getEvents().size());
            assertEquals("callAppenders", 2, appender.getEvents().size());
        } finally {
            LogManager.resetConfiguration();
        }
    }

    @Test
    public void testGetAppender() {
        try {
            final Logger rootLogger = LogManager.getRootLogger();
            final org.apache.logging.log4j.core.Logger v2RootLogger =
                    (org.apache.logging.log4j.core.Logger) rootLogger.getLogger();
            v2RootLogger.addAppender(AppenderAdapter.adapt(version1Appender));
            v2RootLogger.addAppender(appender);
            final List<Appender> rootAppenders = Collections.list(rootLogger.getAllAppenders());
            assertEquals("only v1 appenders", 1, rootAppenders.size());
            assertTrue("appender is a v1 ListAppender", rootAppenders.get(0) instanceof org.apache.log4j.ListAppender);
            assertEquals(
                    "explicitly named appender",
                    VERSION1_APPENDER_NAME,
                    rootLogger.getAppender(VERSION1_APPENDER_NAME).getName());
            final Appender v2ListAppender = rootLogger.getAppender(VERSION2_APPENDER_NAME);
            assertTrue("explicitly named appender", v2ListAppender instanceof AppenderWrapper);
            assertTrue(
                    "appender is a v2 ListAppender",
                    ((AppenderWrapper) v2ListAppender).getAppender() instanceof ListAppender);

            final Logger logger = LogManager.getLogger(CategoryTest.class);
            final org.apache.logging.log4j.core.Logger v2Logger =
                    (org.apache.logging.log4j.core.Logger) logger.getLogger();
            final org.apache.log4j.ListAppender loggerAppender = new org.apache.log4j.ListAppender();
            loggerAppender.setName("appender2");
            v2Logger.addAppender(AppenderAdapter.adapt(loggerAppender));
            final List<Appender> appenders = Collections.list(logger.getAllAppenders());
            assertEquals("no parent appenders", 1, appenders.size());
            assertEquals(loggerAppender, appenders.get(0));
            assertNull("no parent appenders", logger.getAppender(VERSION1_APPENDER_NAME));
            assertNull("no parent appenders", logger.getAppender(VERSION2_APPENDER_NAME));

            final Logger childLogger = LogManager.getLogger(CategoryTest.class.getName() + ".child");
            final Enumeration<Appender> childAppenders = childLogger.getAllAppenders();
            assertFalse("no parent appenders", childAppenders.hasMoreElements());
            assertNull("no parent appenders", childLogger.getAppender("appender2"));
            assertNull("no parent appenders", childLogger.getAppender(VERSION1_APPENDER_NAME));
            assertNull("no parent appenders", childLogger.getAppender(VERSION2_APPENDER_NAME));
        } finally {
            LogManager.resetConfiguration();
        }
    }

    /**
     * Derived category to check method signature of forcedLog.
     */
    private static class MockCategory extends Logger {
        /**
         * Create new instance of MockCategory.
         *
         * @param name category name
         */
        public MockCategory(final String name) {
            super(name);
        }

        /**
         * Request an info level message.
         *
         * @param msg message
         */
        public void info(final String msg) {
            final Priority info = Level.INFO;
            forcedLog(MockCategory.class.toString(), info, msg, null);
        }
    }
}
