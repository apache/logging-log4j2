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
package org.apache.log4j.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.VectorAppender;
import org.apache.log4j.bridge.AppenderWrapper;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.LoggerFactory;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.OptionHandler;
import org.apache.log4j.spi.ThrowableRenderer;
import org.apache.log4j.spi.ThrowableRendererSupport;
import org.apache.log4j.util.Compare;
import org.apache.log4j.util.ControlFilter;
import org.apache.log4j.util.Filter;
import org.apache.log4j.util.ISO8601Filter;
import org.apache.log4j.util.JunitTestRunnerFilter;
import org.apache.log4j.util.LineNumberFilter;
import org.apache.log4j.util.SunReflectFilter;
import org.apache.log4j.util.Transformer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class DOMTestCase {

    /**
     * CustomErrorHandler for testCategoryFactory2.
     */
    public static class CustomErrorHandler implements ErrorHandler {
        public CustomErrorHandler() {}

        public void activateOptions() {}

        @Override
        public void error(final String message) {}

        @Override
        public void error(final String message, final Exception e, final int errorCode) {}

        @Override
        public void error(final String message, final Exception e, final int errorCode, final LoggingEvent event) {}

        @Override
        public void setAppender(final Appender appender) {}

        @Override
        public void setBackupAppender(final Appender appender) {}

        @Override
        public void setLogger(final Logger logger) {}
    }

    /**
     * CustomLogger implementation for testCategoryFactory1 and 2.
     */
    private static class CustomLogger extends Logger {
        /**
         * Creates new instance.
         *
         * @param name logger name.
         */
        public CustomLogger(final String name) {
            super(name);
        }
    }

    /**
     * Creates new instances of CustomLogger.
     */
    public static class CustomLoggerFactory implements LoggerFactory {

        /**
         * Additivity, expected to be set false in configuration file.
         */
        private boolean additivity;

        /**
         * Create new instance of factory.
         */
        public CustomLoggerFactory() {
            additivity = true;
        }

        /**
         * Create new logger.
         *
         * @param name logger name.
         * @return new logger.
         */
        @Override
        public Logger makeNewLoggerInstance(final String name) {
            final Logger logger = new CustomLogger(name);
            assertFalse(additivity);
            return logger;
        }

        /**
         * Set additivity.
         *
         * @param newVal new value of additivity.
         */
        public void setAdditivity(final boolean newVal) {
            additivity = newVal;
        }
    }

    /**
     * Mock ThrowableRenderer for testThrowableRenderer. See bug 45721.
     */
    public static class MockThrowableRenderer implements ThrowableRenderer, OptionHandler {
        private boolean activated = false;
        private boolean showVersion = true;

        public MockThrowableRenderer() {}

        @Override
        public void activateOptions() {
            activated = true;
        }

        @Override
        public String[] doRender(final Throwable t) {
            return new String[0];
        }

        public boolean getShowVersion() {
            return showVersion;
        }

        public boolean isActivated() {
            return activated;
        }

        public void setShowVersion(final boolean v) {
            showVersion = v;
        }
    }

    static String TEMP_A1 = "target/output/temp.A1";
    static String TEMP_A2 = "target/output/temp.A2";
    static String FILTERED_A1 = "target/output/filtered.A1";
    static String FILTERED_A2 = "target/output/filtered.A2";
    static String EXCEPTION1 = "java.lang.Exception: Just testing";
    static String EXCEPTION2 = "\\s*at .*\\(.*\\)";
    static String EXCEPTION3 = "\\s*at .*\\(Native Method\\)";
    static String EXCEPTION4 = "\\s*at .*\\(.*Compiled Code\\)";
    static String EXCEPTION5 = "\\s*at .*\\(.*libgcj.*\\)";
    static String TEST1_1A_PAT = "(TRACE|DEBUG|INFO |WARN |ERROR|FATAL) \\w*\\.\\w* - Message \\d";
    static String TEST1_1B_PAT = "(TRACE|DEBUG|INFO |WARN |ERROR|FATAL) root - Message \\d";
    static String TEST1_2_PAT = "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3} "
            + "\\[main]\\ (TRACE|DEBUG|INFO|WARN|ERROR|FATAL) .* - Message \\d";

    private static final boolean Log4j1ActualAppender = false;

    private static final boolean AssumeThrowableRendererSupport = false;

    Logger root;

    Logger logger;

    void common() {
        final String oldThreadName = Thread.currentThread().getName();
        Thread.currentThread().setName("main");

        int i = -1;

        logger.trace("Message " + ++i);
        root.trace("Message " + i);

        logger.debug("Message " + ++i);
        root.debug("Message " + i);

        logger.info("Message " + ++i);
        root.info("Message " + i);

        logger.warn("Message " + ++i);
        root.warn("Message " + i);

        logger.error("Message " + ++i);
        root.error("Message " + i);

        logger.log(Level.FATAL, "Message " + ++i);
        root.log(Level.FATAL, "Message " + i);

        final Exception e = new Exception("Just testing");
        logger.debug("Message " + ++i, e);
        root.debug("Message " + i, e);

        logger.error("Message " + ++i, e);
        root.error("Message " + i, e);

        Thread.currentThread().setName(oldThreadName);
    }

    @BeforeEach
    public void setUp() {
        root = Logger.getRootLogger();
        logger = Logger.getLogger(DOMTestCase.class);
    }

    @AfterEach
    public void tearDown() {
        root.getLoggerRepository().resetConfiguration();
    }

    @Test
    @Disabled
    public void test1() throws Exception {
        DOMConfigurator.configure("src/test/resources/log4j1-1.2.17/input/xml/DOMTestCase1.xml");
        common();

        final ControlFilter cf1 = new ControlFilter(
                new String[] {TEST1_1A_PAT, TEST1_1B_PAT, EXCEPTION1, EXCEPTION2, EXCEPTION3, EXCEPTION4, EXCEPTION5});

        final ControlFilter cf2 = new ControlFilter(
                new String[] {TEST1_2_PAT, EXCEPTION1, EXCEPTION2, EXCEPTION3, EXCEPTION4, EXCEPTION5});

        Transformer.transform(TEMP_A1, FILTERED_A1, new Filter[] {
            cf1, new LineNumberFilter(), new SunReflectFilter(), new JunitTestRunnerFilter()
        });

        Transformer.transform(TEMP_A2, FILTERED_A2, new Filter[] {
            cf2, new LineNumberFilter(), new ISO8601Filter(), new SunReflectFilter(), new JunitTestRunnerFilter()
        });

        assertTrue(Compare.compare(FILTERED_A1, "witness/dom.A1.1"));
        assertTrue(Compare.compare(FILTERED_A2, "witness/dom.A2.1"));
    }

    /**
     * Tests processing of external entities in XML file.
     */
    @Test
    @Disabled
    public void test4() throws Exception {
        DOMConfigurator.configure("src/test/resources/log4j1-1.2.17/input/xml/DOMTest4.xml");
        common();

        final ControlFilter cf1 = new ControlFilter(
                new String[] {TEST1_1A_PAT, TEST1_1B_PAT, EXCEPTION1, EXCEPTION2, EXCEPTION3, EXCEPTION4, EXCEPTION5});

        final ControlFilter cf2 = new ControlFilter(
                new String[] {TEST1_2_PAT, EXCEPTION1, EXCEPTION2, EXCEPTION3, EXCEPTION4, EXCEPTION5});

        Transformer.transform(TEMP_A1 + ".4", FILTERED_A1 + ".4", new Filter[] {
            cf1, new LineNumberFilter(), new SunReflectFilter(), new JunitTestRunnerFilter()
        });

        Transformer.transform(TEMP_A2 + ".4", FILTERED_A2 + ".4", new Filter[] {
            cf2, new LineNumberFilter(), new ISO8601Filter(), new SunReflectFilter(), new JunitTestRunnerFilter()
        });

        assertTrue(Compare.compare(FILTERED_A1 + ".4", "witness/dom.A1.4"));
        assertTrue(Compare.compare(FILTERED_A2 + ".4", "witness/dom.A2.4"));
    }

    /**
     * Tests that loggers mentioned in logger elements use the specified categoryFactory. See bug 33708.
     */
    @Test
    @Disabled
    public void testCategoryFactory1() {
        DOMConfigurator.configure("src/test/resources/log4j1-1.2.17/input/xml/categoryfactory1.xml");
        //
        // logger not explicitly mentioned in configuration,
        // should use default factory
        final Logger logger2 = Logger.getLogger("org.apache.log4j.xml.DOMTestCase.testCategoryFactory1.2");
        assertFalse(logger2.toString(), logger2 instanceof CustomLogger);
        //
        // logger explicitly mentioned in configuration,
        // should be a CustomLogger
        final Logger logger1 = Logger.getLogger("org.apache.log4j.xml.DOMTestCase.testCategoryFactory1.1");
        assertTrue(logger1.toString(), logger1 instanceof CustomLogger);
        //
        // logger not explicitly mentioned in configuration,
        // should use default factory
        final Logger logger2Bis = Logger.getLogger("org.apache.log4j.xml.DOMTestCase.testCategoryFactory1.2");
        assertFalse(logger2Bis.toString(), logger2Bis instanceof CustomLogger);
    }

    /**
     * Tests that loggers mentioned in logger-ref elements use the specified categoryFactory. See bug 33708.
     */
    @Test
    @Disabled
    public void testCategoryFactory2() {
        DOMConfigurator.configure("src/test/resources/log4j1-1.2.17/input/xml/categoryfactory2.xml");
        //
        // logger not explicitly mentioned in configuration,
        // should use default factory
        final Logger logger2 = Logger.getLogger("org.apache.log4j.xml.DOMTestCase.testCategoryFactory2.2");
        assertFalse(logger2.toString(), logger2 instanceof CustomLogger);
        //
        // logger explicitly mentioned in configuration,
        // should be a CustomLogger
        final Logger logger1 = Logger.getLogger("org.apache.log4j.xml.DOMTestCase.testCategoryFactory2.1");
        assertTrue(logger1.toString(), logger1 instanceof CustomLogger);
        //
        // logger not explicitly mentioned in configuration,
        // should use default factory
        final Logger logger2Bis = Logger.getLogger("org.apache.log4j.xml.DOMTestCase.testCategoryFactory2.2");
        assertFalse(logger2Bis.toString(), logger2Bis instanceof CustomLogger);
    }

    /**
     * Test checks that configureAndWatch does initial configuration, see bug 33502.
     *
     * @throws Exception if IO error.
     */
    @Test
    public void testConfigureAndWatch() throws Exception {
        DOMConfigurator.configureAndWatch("src/test/resources/log4j1-1.2.17/input/xml/DOMTestCase1.xml");
        assertNotNull(Logger.getRootLogger().getAppender("A1"));
    }

    /**
     * Test for bug 47465. configure(URL) did not close opened JarURLConnection.
     *
     * @throws IOException if IOException creating properties jar.
     */
    @Test
    public void testJarURL() throws IOException {
        final File input = new File("src/test/resources/log4j1-1.2.17/input/xml/defaultInit.xml");
        System.out.println(input.getAbsolutePath());
        final File configJar = new File("target/output/xml.jar");
        final File dir = new File("target/output");
        dir.mkdirs();
        try (final InputStream inputStream = new FileInputStream(input);
                final FileOutputStream out = new FileOutputStream(configJar);
                final ZipOutputStream zos = new ZipOutputStream(out)) {
            zos.putNextEntry(new ZipEntry("log4j.xml"));
            int len;
            final byte[] buf = new byte[1024];
            while ((len = inputStream.read(buf)) > 0) {
                zos.write(buf, 0, len);
            }
            zos.closeEntry();
        }
        final URL urlInJar = new URL("jar:" + configJar.toURL() + "!/log4j.xml");
        DOMConfigurator.configure(urlInJar);
        assertTrue(configJar.delete());
        assertFalse(configJar.exists());
    }

    /**
     * Tests that loggers mentioned in logger elements use the specified loggerFactory. See bug 33708.
     */
    @Test
    @Disabled("TODO")
    public void testLoggerFactory1() {
        DOMConfigurator.configure("src/test/resources/log4j1-1.2.17/input/xml/loggerfactory1.xml");
        //
        // logger not explicitly mentioned in configuration,
        // should use default factory
        final Logger logger2 = Logger.getLogger("org.apache.log4j.xml.DOMTestCase.testLoggerFactory1.2");
        assertNotNull(logger2);
        assertFalse(logger2.toString(), logger2 instanceof CustomLogger);
        //
        // logger explicitly mentioned in configuration,
        // should be a CustomLogger
        final Logger logger1 = Logger.getLogger("org.apache.log4j.xml.DOMTestCase.testLoggerFactory1.1");
        assertNotNull(logger1);
        assertTrue(logger1.toString(), logger1 instanceof CustomLogger);
        //
        // logger not explicitly mentioned in configuration,
        // should use default factory
        final Logger logger2Bis = Logger.getLogger("org.apache.log4j.xml.DOMTestCase.testLoggerFactory1.2");
        assertNotNull(logger2Bis);
        assertFalse(logger2Bis.toString(), logger2Bis instanceof CustomLogger);
    }

    /**
     * This test checks that the subst method of an extending class is checked when evaluating parameters. See bug 43325.
     *
     */
    @Test
    public void testOverrideSubst() {
        final DOMConfigurator configurator = new DOMConfigurator() {
            protected String subst(final String value) {
                if ("target/output/temp.A1".equals(value)) {
                    return "target/output/subst-test.A1";
                }
                return value;
            }
        };
        configurator.doConfigure(
                "src/test/resources/log4j1-1.2.17/input/xml/DOMTestCase1.xml", LogManager.getLoggerRepository());
        final String name = "A1";
        final Appender appender = Logger.getRootLogger().getAppender(name);
        assertNotNull(name, appender);
        if (Log4j1ActualAppender) {
            final FileAppender a1 = (FileAppender) appender;
            assertNotNull(name, a1);
            final String file = a1.getFile();
            assertEquals("target/output/subst-test.A1", file);
        } else {
            final AppenderWrapper wrapper = (AppenderWrapper) appender;
            assertNotNull(name, wrapper);
            final org.apache.logging.log4j.core.appender.FileAppender a1 =
                    (org.apache.logging.log4j.core.appender.FileAppender) wrapper.getAppender();
            assertNotNull(wrapper.toString(), a1);
            final String file = a1.getFileName();
            assertNotNull(a1.toString(), file);
            // TODO Support this or not?
            // assertEquals("target/output/subst-test.A1", file);
        }
    }

    /**
     * Tests that reset="true" on log4j:configuration element resets repository before configuration.
     *
     * @throws Exception thrown on error.
     */
    @Test
    public void testReset() throws Exception {
        final VectorAppender appender = new VectorAppender();
        appender.setName("V1");
        Logger.getRootLogger().addAppender(appender);
        DOMConfigurator.configure("src/test/resources/log4j1-1.2.17/input/xml/testReset.xml");
        assertNull(Logger.getRootLogger().getAppender("V1"));
    }

    /**
     * Test of log4j.throwableRenderer support. See bug 45721.
     */
    @Test
    public void testThrowableRenderer1() {
        DOMConfigurator.configure("src/test/resources/log4j1-1.2.17/input/xml/throwableRenderer1.xml");
        final ThrowableRendererSupport repo = (ThrowableRendererSupport) LogManager.getLoggerRepository();
        final MockThrowableRenderer renderer = (MockThrowableRenderer) repo.getThrowableRenderer();
        LogManager.resetConfiguration();
        if (AssumeThrowableRendererSupport) {
            assertNotNull(renderer);
            assertEquals(true, renderer.isActivated());
            assertEquals(false, renderer.getShowVersion());
        }
    }
}
