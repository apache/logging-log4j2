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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

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
        assertThat(rbUS).isNotNull();

        rbFR = ResourceBundle.getBundle("L7D", new Locale("fr", "FR"));
        assertThat(rbFR).describedAs("Got a null resource bundle.").isNotNull();

        rbCH = ResourceBundle.getBundle("L7D", new Locale("fr", "CH"));
        assertThat(rbCH).describedAs("Got a null resource bundle.").isNotNull();

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
            ((org.apache.logging.log4j.core.Logger) loggerA.getLogger()).addAppender(coutingAppender);

            assertThat(coutingAppender.counter).isEqualTo(0);
            loggerAB.debug(MSG);
            assertThat(coutingAppender.counter).isEqualTo(1);
            loggerAB.info(MSG);
            assertThat(coutingAppender.counter).isEqualTo(2);
            loggerAB.warn(MSG);
            assertThat(coutingAppender.counter).isEqualTo(3);
            loggerAB.error(MSG);
            assertThat(coutingAppender.counter).isEqualTo(4);
            coutingAppender.stop();
        } finally {
            ((org.apache.logging.log4j.core.Logger) loggerA.getLogger()).removeAppender(coutingAppender);
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
            ((org.apache.logging.log4j.core.Logger) a.getLogger()).addAppender(ca1);
            ((org.apache.logging.log4j.core.Logger) abc.getLogger()).addAppender(ca2);

            assertThat(ca1.counter).isEqualTo(0);
            assertThat(ca2.counter).isEqualTo(0);

            ab.debug(MSG);
            assertThat(ca1.counter).isEqualTo(1);
            assertThat(ca2.counter).isEqualTo(0);

            abc.debug(MSG);
            assertThat(ca1.counter).isEqualTo(2);
            assertThat(ca2.counter).isEqualTo(1);

            x.debug(MSG);
            assertThat(ca1.counter).isEqualTo(2);
            assertThat(ca2.counter).isEqualTo(1);
            ca1.stop();
            ca2.stop();
        } finally {
            ((org.apache.logging.log4j.core.Logger) a.getLogger()).removeAppender(ca1);
            ((org.apache.logging.log4j.core.Logger) abc.getLogger()).removeAppender(ca2);
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
            ((org.apache.logging.log4j.core.Logger) root.getLogger()).addAppender(caRoot);
            ((org.apache.logging.log4j.core.Logger) a.getLogger()).addAppender(caA);
            ((org.apache.logging.log4j.core.Logger) abc.getLogger()).addAppender(caABC);

            assertThat(caRoot.counter).isEqualTo(0);
            assertThat(caA.counter).isEqualTo(0);
            assertThat(caABC.counter).isEqualTo(0);

            ab.setAdditivity(false);

            a.debug(MSG);
            assertThat(caRoot.counter).isEqualTo(1);
            assertThat(caA.counter).isEqualTo(1);
            assertThat(caABC.counter).isEqualTo(0);

            ab.debug(MSG);
            assertThat(caRoot.counter).isEqualTo(1);
            assertThat(caA.counter).isEqualTo(1);
            assertThat(caABC.counter).isEqualTo(0);

            abc.debug(MSG);
            assertThat(caRoot.counter).isEqualTo(1);
            assertThat(caA.counter).isEqualTo(1);
            assertThat(caABC.counter).isEqualTo(1);
            caRoot.stop();
            caA.stop();
            caABC.stop();
        } finally {
            ((org.apache.logging.log4j.core.Logger) root.getLogger()).removeAppender(caRoot);
            ((org.apache.logging.log4j.core.Logger) a.getLogger()).removeAppender(caA);
            ((org.apache.logging.log4j.core.Logger) abc.getLogger()).removeAppender(caABC);
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
        assertThat(rbUS).isSameAs(t);

        final Logger x = Logger.getLogger("x");
        final Logger x_y = Logger.getLogger("x.y");
        final Logger x_y_z = Logger.getLogger("x.y.z");

        t = x.getResourceBundle();
        assertThat(rbUS).isSameAs(t);
        t = x_y.getResourceBundle();
        assertThat(rbUS).isSameAs(t);
        t = x_y_z.getResourceBundle();
        assertThat(rbUS).isSameAs(t);
    }

    @Test
    public void testRB2() {
        final Logger root = Logger.getRootLogger();
        root.setResourceBundle(rbUS);
        ResourceBundle t = root.getResourceBundle();
        assertThat(t == rbUS).isTrue();

        final Logger x = Logger.getLogger("x");
        final Logger x_y = Logger.getLogger("x.y");
        final Logger x_y_z = Logger.getLogger("x.y.z");

        x_y.setResourceBundle(rbFR);
        t = x.getResourceBundle();
        assertThat(rbUS).isSameAs(t);
        t = x_y.getResourceBundle();
        assertThat(rbFR).isSameAs(t);
        t = x_y_z.getResourceBundle();
        assertThat(rbFR).isSameAs(t);
    }

    @Test
    public void testRB3() {
        final Logger root = Logger.getRootLogger();
        root.setResourceBundle(rbUS);
        ResourceBundle t = root.getResourceBundle();
        assertThat(t == rbUS).isTrue();

        final Logger x = Logger.getLogger("x");
        final Logger x_y = Logger.getLogger("x.y");
        final Logger x_y_z = Logger.getLogger("x.y.z");

        x_y.setResourceBundle(rbFR);
        x_y_z.setResourceBundle(rbCH);
        t = x.getResourceBundle();
        assertThat(rbUS).isSameAs(t);
        t = x_y.getResourceBundle();
        assertThat(rbFR).isSameAs(t);
        t = x_y_z.getResourceBundle();
        assertThat(rbCH).isSameAs(t);
    }

    @Test
    public void testExists() {
        final Logger a = Logger.getLogger("a");
        final Logger a_b = Logger.getLogger("a.b");
        final Logger a_b_c = Logger.getLogger("a.b.c");

        Logger t;
        t = LogManager.exists("xx");
        assertThat(t).isNull();
        t = LogManager.exists("a");
        assertThat(t).isSameAs(a);
        t = LogManager.exists("a.b");
        assertThat(t).isSameAs(a_b);
        t = LogManager.exists("a.b.c");
        assertThat(t).isSameAs(a_b_c);
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
        ((org.apache.logging.log4j.core.Logger) root.getLogger()).addAppender(appender);
        root.setLevel(Level.INFO);

        final Logger tracer = Logger.getLogger("com.example.Tracer");
        tracer.setLevel(Level.TRACE);

        tracer.trace("Message 1");
        root.trace("Discarded Message");
        root.trace("Discarded Message");

        final List<LogEvent> msgs = appender.getEvents();
        assertThat(msgs).hasSize(1);
        final LogEvent event = msgs.get(0);
        assertThat(event.getLevel()).isEqualTo(org.apache.logging.log4j.Level.TRACE);
        assertThat(event.getMessage().getFormat()).isEqualTo("Message 1");
        appender.stop();
        ((org.apache.logging.log4j.core.Logger) root.getLogger()).removeAppender(appender);
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
            ((org.apache.logging.log4j.core.Logger) root.getLogger()).addAppender(appender);
            root.setLevel(Level.INFO);

            final Logger tracer = Logger.getLogger("com.example.Tracer");
            tracer.setLevel(Level.TRACE);
            final NullPointerException ex = new NullPointerException();

            tracer.trace("Message 1", ex);
            root.trace("Discarded Message", ex);
            root.trace("Discarded Message", ex);

            final List<LogEvent> msgs = appender.getEvents();
            assertThat(msgs).hasSize(1);
            final LogEvent event = msgs.get(0);
            assertThat(event.getLevel()).isEqualTo(org.apache.logging.log4j.Level.TRACE);
            assertThat(event.getMessage().getFormattedMessage()).isEqualTo("Message 1");
            appender.stop();
        } finally {
            ((org.apache.logging.log4j.core.Logger) root.getLogger()).removeAppender(appender);
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
            ((org.apache.logging.log4j.core.Logger) root.getLogger()).addAppender(appender);
            root.setLevel(Level.INFO);

            final Logger tracer = Logger.getLogger("com.example.Tracer");
            tracer.setLevel(Level.TRACE);

            assertThat(tracer.isTraceEnabled()).isTrue();
            assertThat(root.isTraceEnabled()).isFalse();
            appender.stop();
        } finally {
            ((org.apache.logging.log4j.core.Logger) root.getLogger()).removeAppender(appender);
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testLog() {
        final PatternLayout layout = PatternLayout.newBuilder().setPattern("%d %C %L %m").build();
        final ListAppender appender = new ListAppender("List", null, layout, false, false);
        appender.start();
        final Logger root = Logger.getRootLogger();
        try {
            ((org.apache.logging.log4j.core.Logger) root.getLogger()).addAppender(appender);
            root.setLevel(Level.INFO);
            final MyLogger log = new MyLogger(root);
            log.logInfo("This is a test", null);
            root.log(Priority.INFO, "Test msg2", null);
            root.log(Priority.INFO, "Test msg3");
            final List<String> msgs = appender.getMessages();
            assertThat(msgs).describedAs("Incorrect number of messages").hasSize(3);
            final String msg = msgs.get(0);
            assertThat(msg.contains(LoggerTest.class.getName())).describedAs("Message contains incorrect class name: " + msg).isTrue();
            appender.stop();
        } finally {
            ((org.apache.logging.log4j.core.Logger) root.getLogger()).removeAppender(appender);
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
            super("Counter", null, null, true, Property.EMPTY_ARRAY);
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

