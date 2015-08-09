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

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Used for internal unit testing the Logger class.
 */
public class LoggerTest {

    Appender a1;
    Appender a2;

    static ResourceBundle rbUS;
    static ResourceBundle rbFR;
    static ResourceBundle rbCH;

    // A short message.
    static String MSG = "M";

    static ConfigurationFactory configurationFactory = new BasicConfigurationFactory();

    @BeforeClass
    public static void setUpClass() {
        rbUS = ResourceBundle.getBundle("L7D", new Locale("en", "US"));
        assertNotNull(rbUS);

        rbFR = ResourceBundle.getBundle("L7D", new Locale("fr", "FR"));
        assertNotNull("Got a null resource bundle.", rbFR);

        rbCH = ResourceBundle.getBundle("L7D", new Locale("fr", "CH"));
        assertNotNull("Got a null resource bundle.", rbCH);

        ConfigurationFactory.setConfigurationFactory(configurationFactory);
    }

    @AfterClass
    public static void tearDownClass() {
        ConfigurationFactory.removeConfigurationFactory(configurationFactory);
    }

    @After
    public void tearDown() {
        LoggerContext.getContext().reconfigure();
        a1 = null;
        a2 = null;
    }

    /**
     * Add an appender and see if it can be retrieved.
     *  Skipping this test as the Appender interface isn't compatible with legacy Log4j.
    public void testAppender1() {
        logger = Logger.getLogger("test");
        a1 = new ListAppender("testAppender1");
        logger.addAppender(a1);

        Enumeration enumeration = logger.getAllAppenders();
        Appender aHat = (Appender) enumeration.nextElement();
        assertEquals(a1, aHat);
    } */

    /**
     * Add an appender X, Y, remove X and check if Y is the only
     * remaining appender.
     * Skipping this test as the Appender interface isn't compatible with legacy Log4j.
    public void testAppender2() {
        a1 = new FileAppender();
        a1.setName("testAppender2.1");
        a2 = new FileAppender();
        a2.setName("testAppender2.2");

        logger = Logger.getLogger("test");
        logger.addAppender(a1);
        logger.addAppender(a2);
        logger.removeAppender("testAppender2.1");
        Enumeration enumeration = logger.getAllAppenders();
        Appender aHat = (Appender) enumeration.nextElement();
        assertEquals(a2, aHat);
        assertTrue(!enumeration.hasMoreElements());
    }  */

    /**
     * Test if logger a.b inherits its appender from a.
     */
    @Test
    public void testAdditivity1() {
        final Logger loggerA = Logger.getLogger("a");
        final Logger loggerAB = Logger.getLogger("a.b");
        final CountingAppender coutingAppender = new CountingAppender();
        coutingAppender.start();
        try {
            loggerA.getLogger().addAppender(coutingAppender);

            assertEquals(0, coutingAppender.counter);
            loggerAB.debug(MSG);
            assertEquals(1, coutingAppender.counter);
            loggerAB.info(MSG);
            assertEquals(2, coutingAppender.counter);
            loggerAB.warn(MSG);
            assertEquals(3, coutingAppender.counter);
            loggerAB.error(MSG);
            assertEquals(4, coutingAppender.counter);
            coutingAppender.stop();
        } finally {
            loggerA.getLogger().removeAppender(coutingAppender);
        }
    }

    /**
     * Test multiple additivity.
     */
    @Test
    public void testAdditivity2() {
        final Logger a = Logger.getLogger("a");
        final Logger ab = Logger.getLogger("a.b");
        final Logger abc = Logger.getLogger("a.b.c");
        final Logger x = Logger.getLogger("x");

        final CountingAppender ca1 = new CountingAppender();
        ca1.start();
        final CountingAppender ca2 = new CountingAppender();
        ca2.start();

        try {
            a.getLogger().addAppender(ca1);
            abc.getLogger().addAppender(ca2);

            assertEquals(ca1.counter, 0);
            assertEquals(ca2.counter, 0);

            ab.debug(MSG);
            assertEquals(ca1.counter, 1);
            assertEquals(ca2.counter, 0);

            abc.debug(MSG);
            assertEquals(ca1.counter, 2);
            assertEquals(ca2.counter, 1);

            x.debug(MSG);
            assertEquals(ca1.counter, 2);
            assertEquals(ca2.counter, 1);
            ca1.stop();
            ca2.stop();
        } finally {
            a.getLogger().removeAppender(ca1);
            abc.getLogger().removeAppender(ca2);
        }}

    /**
     * Test additivity flag.
     */
    @Test
    public void testAdditivity3() {
        final Logger root = Logger.getRootLogger();
        final Logger a = Logger.getLogger("a");
        final Logger ab = Logger.getLogger("a.b");
        final Logger abc = Logger.getLogger("a.b.c");
        Logger.getLogger("x");

        final CountingAppender caRoot = new CountingAppender();
        caRoot.start();
        final CountingAppender caA = new CountingAppender();
        caA.start();
        final CountingAppender caABC = new CountingAppender();
        caABC.start();
        try {
            root.getLogger().addAppender(caRoot);
            a.getLogger().addAppender(caA);
            abc.getLogger().addAppender(caABC);

            assertEquals(caRoot.counter, 0);
            assertEquals(caA.counter, 0);
            assertEquals(caABC.counter, 0);

            ab.setAdditivity(false);

            a.debug(MSG);
            assertEquals(caRoot.counter, 1);
            assertEquals(caA.counter, 1);
            assertEquals(caABC.counter, 0);

            ab.debug(MSG);
            assertEquals(caRoot.counter, 1);
            assertEquals(caA.counter, 1);
            assertEquals(caABC.counter, 0);

            abc.debug(MSG);
            assertEquals(caRoot.counter, 1);
            assertEquals(caA.counter, 1);
            assertEquals(caABC.counter, 1);
            caRoot.stop();
            caA.stop();
            caABC.stop();
        } finally {
            root.getLogger().removeAppender(caRoot);
            a.getLogger().removeAppender(caA);
            abc.getLogger().removeAppender(caABC);
        }}

    /* Don't support getLoggerRepository
    public void testDisable1() {
        CountingAppender caRoot = new CountingAppender();
        Logger root = Logger.getRootLogger();
        root.getLogger().addAppender(caRoot);

        LoggerRepository h = LogManager.getLoggerRepository();
        //h.disableDebug();
        h.setThreshold((Level) Level.INFO);
        assertEquals(caRoot.counter, 0);

        root.debug(MSG);
        assertEquals(caRoot.counter, 0);
        root.info(MSG);
        assertEquals(caRoot.counter, 1);
        root.log(Level.WARN, MSG);
        assertEquals(caRoot.counter, 2);
        root.warn(MSG);
        assertEquals(caRoot.counter, 3);

        //h.disableInfo();
        h.setThreshold((Level) Level.WARN);
        root.debug(MSG);
        assertEquals(caRoot.counter, 3);
        root.info(MSG);
        assertEquals(caRoot.counter, 3);
        root.log(Level.WARN, MSG);
        assertEquals(caRoot.counter, 4);
        root.error(MSG);
        assertEquals(caRoot.counter, 5);
        root.log(Level.ERROR, MSG);
        assertEquals(caRoot.counter, 6);

        //h.disableAll();
        h.setThreshold(Level.OFF);
        root.debug(MSG);
        assertEquals(caRoot.counter, 6);
        root.info(MSG);
        assertEquals(caRoot.counter, 6);
        root.log(Level.WARN, MSG);
        assertEquals(caRoot.counter, 6);
        root.error(MSG);
        assertEquals(caRoot.counter, 6);
        root.log(Level.FATAL, MSG);
        assertEquals(caRoot.counter, 6);
        root.log(Level.FATAL, MSG);
        assertEquals(caRoot.counter, 6);

        //h.disable(Level.FATAL);
        h.setThreshold(Level.OFF);
        root.debug(MSG);
        assertEquals(caRoot.counter, 6);
        root.info(MSG);
        assertEquals(caRoot.counter, 6);
        root.log(Level.WARN, MSG);
        assertEquals(caRoot.counter, 6);
        root.error(MSG);
        assertEquals(caRoot.counter, 6);
        root.log(Level.ERROR, MSG);
        assertEquals(caRoot.counter, 6);
        root.log(Level.FATAL, MSG);
        assertEquals(caRoot.counter, 6);
    }  */

    @Test
    public void testRB1() {
        final Logger root = Logger.getRootLogger();
        root.setResourceBundle(rbUS);
        ResourceBundle t = root.getResourceBundle();
        assertSame(t, rbUS);

        final Logger x = Logger.getLogger("x");
        final Logger x_y = Logger.getLogger("x.y");
        final Logger x_y_z = Logger.getLogger("x.y.z");

        t = x.getResourceBundle();
        assertSame(t, rbUS);
        t = x_y.getResourceBundle();
        assertSame(t, rbUS);
        t = x_y_z.getResourceBundle();
        assertSame(t, rbUS);
    }

    @Test
    public void testRB2() {
        final Logger root = Logger.getRootLogger();
        root.setResourceBundle(rbUS);
        ResourceBundle t = root.getResourceBundle();
        assertTrue(t == rbUS);

        final Logger x = Logger.getLogger("x");
        final Logger x_y = Logger.getLogger("x.y");
        final Logger x_y_z = Logger.getLogger("x.y.z");

        x_y.setResourceBundle(rbFR);
        t = x.getResourceBundle();
        assertSame(t, rbUS);
        t = x_y.getResourceBundle();
        assertSame(t, rbFR);
        t = x_y_z.getResourceBundle();
        assertSame(t, rbFR);
    }

    @Test
    public void testRB3() {
        final Logger root = Logger.getRootLogger();
        root.setResourceBundle(rbUS);
        ResourceBundle t = root.getResourceBundle();
        assertTrue(t == rbUS);

        final Logger x = Logger.getLogger("x");
        final Logger x_y = Logger.getLogger("x.y");
        final Logger x_y_z = Logger.getLogger("x.y.z");

        x_y.setResourceBundle(rbFR);
        x_y_z.setResourceBundle(rbCH);
        t = x.getResourceBundle();
        assertSame(t, rbUS);
        t = x_y.getResourceBundle();
        assertSame(t, rbFR);
        t = x_y_z.getResourceBundle();
        assertSame(t, rbCH);
    }

    @Test
    public void testExists() {
        final Logger a = Logger.getLogger("a");
        final Logger a_b = Logger.getLogger("a.b");
        final Logger a_b_c = Logger.getLogger("a.b.c");

        Logger t;
        t = LogManager.exists("xx");
        assertNull(t);
        t = LogManager.exists("a");
        assertSame(a, t);
        t = LogManager.exists("a.b");
        assertSame(a_b, t);
        t = LogManager.exists("a.b.c");
        assertSame(a_b_c, t);
    }
    /* Don't support hierarchy
    public void testHierarchy1() {
        Hierarchy h = new Hierarchy(new RootLogger((Level) Level.ERROR));
        Logger a0 = h.getLogger("a");
        assertEquals("a", a0.getName());
        assertNull(a0.getLevel());
        assertSame(Level.ERROR, a0.getEffectiveLevel());

        Logger a1 = h.getLogger("a");
        assertSame(a0, a1);
    } */

    /**
     * Tests logger.trace(Object).
     */
    @Test
    public void testTrace() {
        final ListAppender appender = new ListAppender("List");
        appender.start();
        final Logger root = Logger.getRootLogger();
        root.getLogger().addAppender(appender);
        root.setLevel(Level.INFO);

        final Logger tracer = Logger.getLogger("com.example.Tracer");
        tracer.setLevel(Level.TRACE);

        tracer.trace("Message 1");
        root.trace("Discarded Message");
        root.trace("Discarded Message");

        final List<LogEvent> msgs = appender.getEvents();
        assertEquals(1, msgs.size());
        final LogEvent event = msgs.get(0);
        assertEquals(org.apache.logging.log4j.Level.TRACE, event.getLevel());
        assertEquals("Message 1", event.getMessage().getFormat());
        appender.stop();
        root.getLogger().removeAppender(appender);
    }

    /**
     * Tests logger.trace(Object, Exception).
     */
    @Test
    public void testTraceWithException() {
        final ListAppender appender = new ListAppender("List");
        appender.start();
        final Logger root = Logger.getRootLogger();
        try {
            root.getLogger().addAppender(appender);
            root.setLevel(Level.INFO);

            final Logger tracer = Logger.getLogger("com.example.Tracer");
            tracer.setLevel(Level.TRACE);
            final NullPointerException ex = new NullPointerException();

            tracer.trace("Message 1", ex);
            root.trace("Discarded Message", ex);
            root.trace("Discarded Message", ex);

            final List<LogEvent> msgs = appender.getEvents();
            assertEquals(1, msgs.size());
            final LogEvent event = msgs.get(0);
            assertEquals(org.apache.logging.log4j.Level.TRACE, event.getLevel());
            assertEquals("Message 1", event.getMessage().getFormattedMessage());
            appender.stop();
        } finally {
            root.getLogger().removeAppender(appender);
        }
    }

    /**
     * Tests isTraceEnabled.
     */
    @Test
    public void testIsTraceEnabled() {
        final ListAppender appender = new ListAppender("List");
        appender.start();
        final Logger root = Logger.getRootLogger();
        try {
            root.getLogger().addAppender(appender);
            root.setLevel(Level.INFO);

            final Logger tracer = Logger.getLogger("com.example.Tracer");
            tracer.setLevel(Level.TRACE);

            assertTrue(tracer.isTraceEnabled());
            assertFalse(root.isTraceEnabled());
            appender.stop();
        } finally {
            root.getLogger().removeAppender(appender);
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testLog() {
        final PatternLayout layout = PatternLayout.newBuilder().withPattern("%d %C %L %m").build();
        final ListAppender appender = new ListAppender("List", null, layout, false, false);
        appender.start();
        final Logger root = Logger.getRootLogger();
        try {
            root.getLogger().addAppender(appender);
            root.setLevel(Level.INFO);
            final MyLogger log = new MyLogger(root);
            log.logInfo("This is a test", null);
            root.log(Priority.INFO, "Test msg2", null);
            root.log(Priority.INFO, "Test msg3");
            final List<String> msgs = appender.getMessages();
            assertTrue("Incorrect number of messages", msgs.size() == 3);
            final String msg = msgs.get(0);
            assertTrue("Message contains incorrect class name: " + msg, msg.contains(LoggerTest.class.getName()));
            appender.stop();
        } finally {
            root.getLogger().removeAppender(appender);
        }
    }

    private static class MyLogger {

        private final Logger logger;

        public MyLogger(final Logger logger) {
            this.logger = logger;
        }

        @SuppressWarnings("deprecation")
        public void logInfo(final String msg, final Throwable t) {
            logger.log(MyLogger.class.getName(), Priority.INFO, msg, t);
        }
    }

    private static class CountingAppender extends AbstractAppender {

        private static final long serialVersionUID = 1L;

        int counter;

        CountingAppender() {
            super("Counter", null, null);
            counter = 0;
        }

        @Override
        public void append(final LogEvent event) {
            counter++;
        }

        public boolean requiresLayout() {
            return true;
        }
    }
}

