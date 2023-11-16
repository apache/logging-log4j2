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
package org.apache.log4j.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.log4j.CustomFileAppender;
import org.apache.log4j.CustomNoopAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.bridge.AppenderAdapter;
import org.apache.log4j.bridge.AppenderWrapper;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.FileManager;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationListener;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.junit.Test;

/**
 * Test reconfiguring with an XML configuration.
 */
public class PropertiesReconfigurationTest {

    private class TestListener implements ConfigurationListener {

        @Override
        public synchronized void onChange(final Reconfigurable reconfigurable) {
            toggle.countDown();
        }
    }

    private static final String CONFIG_CUSTOM_APPENDERS_1 = "target/test-classes/log4j1-appenders-custom-1.properties";
    private static final String CONFIG_CUSTOM_APPENDERS_2 = "target/test-classes/log4j1-appenders-custom-2.properties";

    private static final String CONFIG_FILE_APPENDER_1 = "target/test-classes/log4j1-file-1.properties";
    private static final String CONFIG_FILE_APPENDER_2 = "target/test-classes/log4j1-file-2.properties";

    private static final Duration FIVE_MINUTES = Duration.ofMinutes(5);

    private final CountDownLatch toggle = new CountDownLatch(1);

    private void assertCustomFileAppender(
            final org.apache.log4j.Appender appender,
            final boolean expectBoolean,
            final int expectInt,
            final String expectString) {
        final CustomFileAppender customAppender = (CustomFileAppender) appender;
        assertEquals(expectBoolean, customAppender.getBooleanA());
        assertEquals(expectInt, customAppender.getIntA());
        assertEquals(expectString, customAppender.getStringA());
    }

    private void assertCustomNoopAppender(
            final org.apache.log4j.Appender appender,
            final boolean expectBoolean,
            final int expectInt,
            final String expectString) {
        final CustomNoopAppender customAppender = (CustomNoopAppender) appender;
        assertEquals(expectBoolean, customAppender.getBooleanA());
        assertEquals(expectInt, customAppender.getIntA());
        assertEquals(expectString, customAppender.getStringA());
    }

    private void checkConfigureCustomAppenders(
            final String configPath,
            final boolean expectAppend,
            final int expectInt,
            final String expectString,
            final FailableConsumer<String, IOException> configurator)
            throws IOException {
        final File file = new File(configPath);
        assertTrue("No Config file", file.exists());
        try (final LoggerContext context = TestConfigurator.configure(file.toString())) {
            final Logger logger = LogManager.getLogger("test");
            logger.info("Hello");
            // V1
            checkCustomAppender("A1", expectAppend, expectInt, expectString);
            checkCustomFileAppender("A2", expectAppend, expectInt, expectString);
        }
    }

    private void checkConfigureFileAppender(final String configPath, final boolean expectAppend) throws IOException {
        final File file = new File(configPath);
        assertTrue("No Config file", file.exists());
        try (final LoggerContext context = TestConfigurator.configure(file.toString())) {
            final Logger logger = LogManager.getLogger("test");
            logger.info("Hello");
            final Configuration configuration = context.getConfiguration();
            // Core
            checkCoreFileAppender(expectAppend, configuration, "A1");
            checkCoreFileAppender(expectAppend, configuration, "A2");
            // V1
            checkFileAppender(expectAppend, "A1");
            checkFileAppender(expectAppend, "A2");
        }
    }

    private void checkCoreFileAppender(final boolean expectAppend, final Appender appender) {
        assertNotNull(appender);
        final FileAppender fileAppender = (FileAppender) appender;
        @SuppressWarnings("resource")
        final FileManager manager = fileAppender.getManager();
        assertNotNull(manager);
        assertEquals(expectAppend, manager.isAppend());
    }

    private void checkCoreFileAppender(
            final boolean expectAppend, final Configuration configuration, final String appenderName) {
        checkCoreFileAppender(expectAppend, configuration.getAppender(appenderName));
    }

    private void checkCustomAppender(
            final String appenderName, final boolean expectBoolean, final int expectInt, final String expectString) {
        final Logger logger = LogManager.getRootLogger();
        final org.apache.log4j.Appender appender = logger.getAppender(appenderName);
        assertNotNull(appender);
        assertCustomNoopAppender(appender, expectBoolean, expectInt, expectString);
        assertCustomNoopAppender(getAppenderFromContext(appenderName), expectBoolean, expectInt, expectString);
    }

    private void checkCustomFileAppender(
            final String appenderName, final boolean expectBoolean, final int expectInt, final String expectString) {
        final Logger logger = LogManager.getRootLogger();
        final org.apache.log4j.Appender appender = logger.getAppender(appenderName);
        assertNotNull(appender);
        assertCustomFileAppender(appender, expectBoolean, expectInt, expectString);
        assertCustomFileAppender(getAppenderFromContext(appenderName), expectBoolean, expectInt, expectString);
    }

    private void checkFileAppender(final boolean expectAppend, final String appenderName) {
        final Logger logger = LogManager.getRootLogger();
        final org.apache.log4j.Appender appender = logger.getAppender(appenderName);
        assertNotNull(appender);
        final AppenderWrapper appenderWrapper = (AppenderWrapper) appender;
        checkCoreFileAppender(expectAppend, appenderWrapper.getAppender());
    }

    @SuppressWarnings("unchecked")
    private <T extends org.apache.log4j.Appender> T getAppenderFromContext(final String appenderName) {
        final LoggerContext context = (LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
        final LoggerConfig loggerConfig = context.getConfiguration().getRootLogger();
        final AppenderAdapter.Adapter adapter =
                (AppenderAdapter.Adapter) loggerConfig.getAppenders().get(appenderName);
        return adapter != null ? (T) adapter.getAppender() : null;
    }

    /**
     * Tests that configuring and reconfiguring CUSTOM appenders properly pick up different settings.
     */
    @Test
    public void testCustomAppenders_TestConfigurator() throws IOException {
        checkConfigureCustomAppenders(CONFIG_CUSTOM_APPENDERS_1, true, 1, "A", TestConfigurator::configure);
        checkConfigureCustomAppenders(CONFIG_CUSTOM_APPENDERS_2, false, 2, "B", TestConfigurator::configure);
        checkConfigureCustomAppenders(CONFIG_CUSTOM_APPENDERS_1, true, 1, "A", TestConfigurator::configure);
        checkConfigureCustomAppenders(CONFIG_CUSTOM_APPENDERS_2, false, 2, "B", TestConfigurator::configure);
    }

    /**
     * Tests that configuring and reconfiguring CUSTOM appenders properly pick up different settings.
     */
    @Test
    public void testCustomAppenders_PropertyConfigurator() throws IOException {
        checkConfigureCustomAppenders(CONFIG_CUSTOM_APPENDERS_1, true, 1, "A", PropertyConfigurator::configure);
        checkConfigureCustomAppenders(CONFIG_CUSTOM_APPENDERS_2, false, 2, "B", PropertyConfigurator::configure);
        checkConfigureCustomAppenders(CONFIG_CUSTOM_APPENDERS_1, true, 1, "A", PropertyConfigurator::configure);
        checkConfigureCustomAppenders(CONFIG_CUSTOM_APPENDERS_2, false, 2, "B", PropertyConfigurator::configure);
    }

    /**
     * Tests that configuring and reconfiguring STOCK file appenders properly pick up different settings.
     */
    @Test
    public void testFileAppenders() throws Exception {
        checkConfigureFileAppender(CONFIG_FILE_APPENDER_1, false);
        checkConfigureFileAppender(CONFIG_FILE_APPENDER_2, true);
        checkConfigureFileAppender(CONFIG_FILE_APPENDER_1, false);
        checkConfigureFileAppender(CONFIG_FILE_APPENDER_2, true);
    }

    @Test
    public void testTestListener() throws Exception {
        System.setProperty(Log4j1Configuration.MONITOR_INTERVAL, "1");
        final File file = new File(CONFIG_FILE_APPENDER_1);
        assertTrue("No Config file", file.exists());
        final long configMillis = file.lastModified();
        assertTrue("Unable to modified file time", file.setLastModified(configMillis - FIVE_MINUTES.toMillis()));
        try (final LoggerContext context = TestConfigurator.configure(file.toString())) {
            final Logger logger = LogManager.getLogger("test");
            logger.info("Hello");
            final Configuration original = context.getConfiguration();
            final TestListener listener = new TestListener();
            original.addListener(listener);
            file.setLastModified(System.currentTimeMillis());
            try {
                if (!toggle.await(3, TimeUnit.SECONDS)) {
                    fail("Reconfiguration timed out");
                }
                // Allow reconfiguration to complete.
                Thread.sleep(500);
            } catch (final InterruptedException ie) {
                fail("Reconfiguration interupted");
            }
            final Configuration updated = context.getConfiguration();
            assertTrue("Configurations are the same", original != updated);
        }
    }
}
