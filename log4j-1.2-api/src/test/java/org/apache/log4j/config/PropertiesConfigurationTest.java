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
package org.apache.log4j.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.ListAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.bridge.AppenderAdapter;
import org.apache.log4j.bridge.FilterAdapter;
import org.apache.log4j.bridge.FilterWrapper;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.apache.logging.log4j.core.filter.Filterable;
import org.apache.logging.log4j.core.filter.LevelRangeFilter;
import org.junit.Test;

/**
 * Test configuration from Properties.
 */
public class PropertiesConfigurationTest extends AbstractLog4j1ConfigurationTest {

    private static final String TEST_KEY = "log4j.test.tmpdir";
    private static final String SUFFIX = ".properties";

    @Override
    Configuration getConfiguration(String configResourcePrefix) throws URISyntaxException, IOException {
        final String configResource = configResourcePrefix + SUFFIX;
        final InputStream inputStream = ClassLoader.getSystemResourceAsStream(configResource);
        final ConfigurationSource source = new ConfigurationSource(inputStream);
        final LoggerContext context = LoggerContext.getContext(false);
        final Configuration configuration = new PropertiesConfigurationFactory().getConfiguration(context, source);
        assertNotNull("No configuration created", configuration);
        configuration.initialize();
        return configuration;
    }

    @Test
    public void testConfigureNullPointerException() throws Exception {
        try (LoggerContext loggerContext = TestConfigurator.configure("target/test-classes/LOG4J2-3247.properties")) {
            // [LOG4J2-3247] configure() should not throw an NPE.
            final Configuration configuration = loggerContext.getConfiguration();
            assertNotNull(configuration);
            final Appender appender = configuration.getAppender("CONSOLE");
            assertNotNull(appender);
        }
    }

    @Test
    public void testConsoleAppenderFilter() throws Exception {
        try (LoggerContext loggerContext = TestConfigurator.configure("target/test-classes/LOG4J2-3247.properties")) {
            // LOG4J2-3281 PropertiesConfiguration.buildAppender not adding filters to appender
            final Configuration configuration = loggerContext.getConfiguration();
            assertNotNull(configuration);
            final Appender appender = configuration.getAppender("CONSOLE");
            assertNotNull(appender);
            final Filterable filterable = (Filterable) appender;
            final FilterAdapter filter = (FilterAdapter) filterable.getFilter();
            assertNotNull(filter);
            assertTrue(filter.getFilter() instanceof NeutralFilterFixture);
        }
    }

    @Test
    public void testCustomAppenderFilter() throws Exception {
        try (LoggerContext loggerContext = TestConfigurator.configure("target/test-classes/LOG4J2-3281.properties")) {
            // LOG4J2-3281 PropertiesConfiguration.buildAppender not adding filters to appender
            final Configuration configuration = loggerContext.getConfiguration();
            assertNotNull(configuration);
            final Appender appender = configuration.getAppender("CUSTOM");
            assertNotNull(appender);
            final Filterable filterable = (Filterable) appender;
            final FilterAdapter filter = (FilterAdapter) filterable.getFilter();
            assertNotNull(filter);
            assertTrue(filter.getFilter() instanceof NeutralFilterFixture);
        }
    }
    
    @Test
    public void testConsoleAppenderLevelRangeFilter() throws Exception {
        try (LoggerContext loggerContext = TestConfigurator.configure("target/test-classes/LOG4J2-3326.properties")) {
            final Configuration configuration = loggerContext.getConfiguration();
            assertNotNull(configuration);
            final Appender appender = configuration.getAppender("CUSTOM");
            assertNotNull(appender);
            final Filterable filterable = (Filterable) appender;
            final CompositeFilter filter = (CompositeFilter) filterable.getFilter();
            final org.apache.logging.log4j.core.Filter[] filters = filter.getFiltersArray();
            final LevelRangeFilter customFilterReal = (LevelRangeFilter) ((FilterWrapper) ((FilterAdapter) filters[0]).getFilter()).getFilter();
            assertEquals(Level.ALL, customFilterReal.getMinLevel());
            final LevelRangeFilter defaultFilter = (LevelRangeFilter) ((FilterWrapper) ((FilterAdapter) filters[1]).getFilter()).getFilter();
            assertEquals(Level.TRACE, defaultFilter.getMinLevel());
      }
    }

    @Test
    public void testListAppender() throws Exception {
        try (LoggerContext loggerContext = TestConfigurator.configure("target/test-classes/log4j1-list.properties")) {
            final Logger logger = LogManager.getLogger("test");
            logger.debug("This is a test of the root logger");
            final Configuration configuration = loggerContext.getConfiguration();
            final Map<String, Appender> appenders = configuration.getAppenders();
            ListAppender eventAppender = null;
            ListAppender messageAppender = null;
            for (final Map.Entry<String, Appender> entry : appenders.entrySet()) {
                if (entry.getKey().equals("list")) {
                    messageAppender = (ListAppender) ((AppenderAdapter.Adapter) entry.getValue()).getAppender();
                } else if (entry.getKey().equals("events")) {
                    eventAppender = (ListAppender) ((AppenderAdapter.Adapter) entry.getValue()).getAppender();
                }
            }
            assertNotNull("No Event Appender", eventAppender);
            assertNotNull("No Message Appender", messageAppender);
            final List<LoggingEvent> events = eventAppender.getEvents();
            assertTrue("No events", events != null && events.size() > 0);
            final List<String> messages = messageAppender.getMessages();
            assertTrue("No messages", messages != null && messages.size() > 0);
        }
    }

    @Test
    public void testProperties() throws Exception {
        try (LoggerContext loggerContext = TestConfigurator.configure("target/test-classes/log4j1-file.properties")) {
            final Logger logger = LogManager.getLogger("test");
            logger.debug("This is a test of the root logger");
            File file = new File("target/temp.A1");
            assertTrue("File A1 was not created", file.exists());
            assertTrue("File A1 is empty", file.length() > 0);
            file = new File("target/temp.A2");
            assertTrue("File A2 was not created", file.exists());
            assertTrue("File A2 is empty", file.length() > 0);
        }
    }

    @Test
    public void testSystemProperties() throws Exception {
        final String testPathLocation = "target";
        System.setProperty(TEST_KEY, testPathLocation);
        try (LoggerContext loggerContext = TestConfigurator.configure("target/test-classes/config-1.2/log4j-FileAppender-with-props.properties")) {
            // [LOG4J2-3312] Bridge does not convert properties.
            final Configuration configuration = loggerContext.getConfiguration();
            assertNotNull(configuration);
            final String name = "FILE_APPENDER";
            final Appender appender = configuration.getAppender(name);
            assertNotNull(name, appender);
            assertTrue(appender.getClass().getName(), appender instanceof FileAppender);
            final FileAppender fileAppender = (FileAppender) appender;
            // Two slashes because that's how the config file is setup.
            assertEquals(testPathLocation + "/hadoop.log", fileAppender.getFileName());
        } finally {
            System.clearProperty(TEST_KEY);
        }
    }

    @Override
    @Test
    public void testConsoleCapitalization() throws Exception {
        super.testConsoleCapitalization();
    }

    @Override
    @Test
    public void testConsoleTtccLayout() throws Exception {
        super.testConsoleTtccLayout();
    }

    @Override
    @Test
    public void testRollingFileAppender() throws Exception {
        super.testRollingFileAppender();
    }

    @Override
    @Test
    public void testDailyRollingFileAppender() throws Exception {
        super.testDailyRollingFileAppender();
    }

    @Override
    @Test
    public void testRollingFileAppenderWithProperties() throws Exception {
        super.testRollingFileAppenderWithProperties();
    }

    @Override
    @Test
    public void testSystemProperties1() throws Exception {
        super.testSystemProperties1();
    }

    @Override
    @Test
    public void testSystemProperties2() throws Exception {
        super.testSystemProperties2();
    }
}