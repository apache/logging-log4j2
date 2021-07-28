/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.test.appender.ListAppender;
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

    private static ListAppender appender = new ListAppender("List");

    @BeforeClass
    public static void setupClass() {
        appender.start();
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

    /**
     * Tests Category.forcedLog.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testForcedLog() {
        final MockCategory category = new MockCategory("org.example.foo");
        category.setAdditivity(false);
        ((org.apache.logging.log4j.core.Logger) category.getLogger()).addAppender(appender);
        category.info("Hello, World");
        final List<LogEvent> list = appender.getEvents();
        int events = list.size();
        assertTrue("Number of events should be 1, was " + events, events == 1);
        LogEvent event = list.get(0);
        Message msg = event.getMessage();
        assertNotNull("No message", msg);
        assertTrue("Incorrect Message type", msg instanceof ObjectMessage);
        Object[] objects = msg.getParameters();
        assertTrue("Incorrect Object type", objects[0] instanceof String);
        appender.clear();
        category.log(Priority.INFO, "Hello, World");
        events = list.size();
        assertTrue("Number of events should be 1, was " + events, events == 1);
        event = list.get(0);
        msg = event.getMessage();
        assertNotNull("No message", msg);
        assertTrue("Incorrect Message type", msg instanceof ObjectMessage);
        objects = msg.getParameters();
        assertTrue("Incorrect Object type", objects[0] instanceof String);
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
        final Layout<String> layout = PatternLayout.newBuilder().withPattern("%d %p %C{1.} [%t] %m%n").build();
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
        assertTrue("Incorrect message " + Strings.dquote(msg) + " expected " + Strings.dquote(expected), msg.endsWith(expected));
    }

    @Test
    public void testStringLog() {
        final String payload = "payload";
        testMessageImplementation(
                payload,
                SimpleMessage.class,
                message -> assertEquals(message.getFormattedMessage(), payload));
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
        testMessageImplementation(
                payload,
                MapMessage.class,
                message -> assertEquals(message.getData(), payload));
    }

    @Test
    public void testObjectLog() {
        final Object payload = new Object();
        testMessageImplementation(
                payload,
                ObjectMessage.class,
                message -> assertEquals(message.getParameter(), payload));
    }

    private <M extends Message> void testMessageImplementation(
            final Object messagePayload,
            final Class<M> expectedMessageClass,
            final Consumer<M> messageTester) {

        // Setup the logger and the appender.
        final Category category = Category.getInstance("TestCategory");
        final org.apache.logging.log4j.core.Logger logger =
                (org.apache.logging.log4j.core.Logger) category.getLogger();
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
