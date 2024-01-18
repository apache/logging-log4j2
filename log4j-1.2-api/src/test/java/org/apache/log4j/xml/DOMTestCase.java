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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

    private static final boolean Log4j1ActualAppender = false;

    private static final boolean AssumeThrowableRendererSupport = false;

    Logger root;

    Logger logger;

    @BeforeEach
    public void setUp() {
        root = Logger.getRootLogger();
        logger = Logger.getLogger(DOMTestCase.class);
    }

    @AfterEach
    public void tearDown() {
        root.getLoggerRepository().resetConfiguration();
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
