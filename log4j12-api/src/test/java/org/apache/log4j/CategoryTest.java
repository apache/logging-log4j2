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

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.status.StatusConsoleListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.List;


/**
 * Tests of Category.
 */
public class CategoryTest {

    static ConfigurationFactory cf = new BasicConfigurationFactory();

    private static final String LINE_SEP = System.getProperty("line.separator");

    private ListAppender appender = new ListAppender("List");

    @BeforeClass
    public static void setupClass() {
        ConfigurationFactory.setConfigurationFactory(cf);
        LoggerContext ctx = (LoggerContext) org.apache.logging.log4j.LogManager.getContext();
        ctx.reconfigure();
    }

    @AfterClass
    public static void cleanupClass() {
        ConfigurationFactory.removeConfigurationFactory(cf);
    }

    /**
     * Tests Category.forcedLog.
     */
    @Test
    public void testForcedLog() {
        MockCategory category = new MockCategory("org.example.foo");
        category.setAdditivity(false);
        category.getLogger().addAppender(appender);
        category.info("Hello, World");
        int events = appender.getEvents().size();
        assertTrue("Number of events should be 1, was " + events, events == 1);
        appender.clear();
    }

    /**
     * Tests that the return type of getChainedPriority is Priority.
     *
     * @throws Exception thrown if Category.getChainedPriority can not be found.
     */
    @Test
    public void testGetChainedPriorityReturnType() throws Exception {
        Method method = Category.class.getMethod("getChainedPriority", (Class[]) null);
        assertTrue(method.getReturnType() == Priority.class);
    }

    /**
     * Tests l7dlog(Priority, String, Throwable).
     */
    @Test
    public void testL7dlog() {
        Logger logger = Logger.getLogger("org.example.foo");
        logger.setLevel(Level.ERROR);
        Priority debug = Level.DEBUG;
        logger.l7dlog(debug, "Hello, World", null);
        assertTrue(appender.getEvents().size() == 0);
    }

    /**
     * Tests l7dlog(Priority, String, Object[], Throwable).
     */
    @Test
    public void testL7dlog4Param() {
        Logger logger = Logger.getLogger("org.example.foo");
        logger.setLevel(Level.ERROR);
        Priority debug = Level.DEBUG;
        logger.l7dlog(debug, "Hello, World", new Object[0], null);
        assertTrue(appender.getEvents().size() == 0);
    }

    /**
     * Test using a pre-existing log4j2 logger
     */
    @Test
    public void testExistingLog4j2Logger() {
        // create the logger using LogManager
        org.apache.logging.log4j.LogManager.getLogger("existingLogger");
        // Logger will be the one created above
        Logger logger = Logger.getLogger("existingLogger");
        Logger l2 = LogManager.getLogger("existingLogger");
        assertEquals(logger, l2);
        logger.setLevel(Level.ERROR);
        Priority debug = Level.DEBUG;
        // the next line will throw an exception if the LogManager loggers
        // aren't supported by 1.2 Logger/Category
        logger.l7dlog(debug, "Hello, World", new Object[0], null);
        assertTrue(appender.getEvents().size() == 0);
    }

    /**
     * Tests setPriority(Priority).
     *
     * @deprecated
     */
    @Deprecated
    @Test
    public void testSetPriority() {
        Logger logger = Logger.getLogger("org.example.foo");
        Priority debug = Level.DEBUG;
        logger.setPriority(debug);
    }

    @Test
    public void testClassName() {
        Category category = Category.getInstance("TestCategory");
        Layout layout = PatternLayout.createLayout("%d %p %C{1.} [%t] %m%n", null, null, null);
        ListAppender appender = new ListAppender("List2", null, layout, false, false);
        appender.start();
        category.setAdditivity(false);
        category.getLogger().addAppender(appender);
        category.error("Test Message");
        List<String> msgs = appender.getMessages();
        assertTrue("Incorrect number of messages. Expected 1 got " + msgs.size(), msgs.size() == 1);
        String msg = msgs.get(0);
        appender.clear();
        String expected = "ERROR o.a.l.CategoryTest [main] Test Message" + LINE_SEP;
        assertTrue("Incorrect message \"" + msg + "\"" + " expected \"" + expected +"\"", msg.endsWith(expected));
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
            Priority info = Level.INFO;
            forcedLog(MockCategory.class.toString(), info, msg, null);
        }
    }
}
