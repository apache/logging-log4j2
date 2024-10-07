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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.xml.XmlConfigurationFactory;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.junit.jupiter.api.Test;

/**
 * Test configuration from XML.
 */
public class XmlConfigurationTest extends AbstractLog4j1ConfigurationTest {

    private static final String SUFFIX = ".xml";

    @Override
    Configuration getConfiguration(final String configResourcePrefix) throws URISyntaxException, IOException {
        final String configResource = configResourcePrefix + SUFFIX;
        final InputStream inputStream = getResourceAsStream(configResource);
        final ConfigurationSource source = new ConfigurationSource(inputStream);
        final LoggerContext context = LoggerContext.getContext(false);
        final Configuration configuration = new XmlConfigurationFactory().getConfiguration(context, source);
        assertNotNull(configuration, "No configuration created");
        configuration.initialize();
        return configuration;
    }

    @Test
    public void testListAppender() throws Exception {
        final LoggerContext loggerContext = TestConfigurator.configure("target/test-classes/log4j1-list.xml");
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
        assertNotNull(eventAppender, "No Event Appender");
        assertNotNull(messageAppender, "No Message Appender");
        final List<LoggingEvent> events = eventAppender.getEvents();
        assertTrue(events != null && events.size() > 0, "No events");
        final List<String> messages = messageAppender.getMessages();
        assertTrue(messages != null && messages.size() > 0, "No messages");
    }

    @Test
    public void testXML() throws Exception {
        TestConfigurator.configure("target/test-classes/log4j1-file.xml");
        final Logger logger = LogManager.getLogger("test");
        logger.debug("This is a test of the root logger");
        File file = new File("target/temp.A1");
        assertTrue(file.exists(), "File A1 was not created");
        assertTrue(file.length() > 0, "File A1 is empty");
        file = new File("target/temp.A2");
        assertTrue(file.exists(), "File A2 was not created");
        assertTrue(file.length() > 0, "File A2 is empty");
    }

    @Override
    @Test
    public void testConsoleEnhancedPatternLayout() throws Exception {
        super.testConsoleEnhancedPatternLayout();
    }

    @Override
    @Test
    public void testConsoleHtmlLayout() throws Exception {
        super.testConsoleHtmlLayout();
    }

    @Override
    @Test
    public void testConsolePatternLayout() throws Exception {
        super.testConsolePatternLayout();
    }

    @Override
    @Test
    public void testConsoleSimpleLayout() throws Exception {
        super.testConsoleSimpleLayout();
    }

    @Override
    @Test
    public void testFileSimpleLayout() throws Exception {
        super.testFileSimpleLayout();
    }

    @Override
    @Test
    public void testNullAppender() throws Exception {
        super.testNullAppender();
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
    public void testSystemProperties1() throws Exception {
        super.testSystemProperties1();
    }

    @Override
    @Test
    public void testDefaultValues() throws Exception {
        super.testDefaultValues();
    }

    @Override
    @Test
    public void testMultipleFilters() throws Exception {
        super.testMultipleFilters();
    }

    @Override
    @Test
    public void testGlobalThreshold() throws Exception {
        super.testGlobalThreshold();
    }

    @Test
    public void testEnhancedRollingFileAppender() throws Exception {
        try (final LoggerContext ctx = configure("config-1.2/log4j-EnhancedRollingFileAppender")) {
            final Configuration configuration = ctx.getConfiguration();
            assertNotNull(configuration);
            testEnhancedRollingFileAppender(configuration);
            // Only supported through XML configuration
            final Appender appender = configuration.getAppender("MIXED");
            assertTrue(appender instanceof RollingFileAppender, "is RollingFileAppender");
            final TriggeringPolicy policy = ((RollingFileAppender) appender).getTriggeringPolicy();
            assertTrue(policy instanceof CompositeTriggeringPolicy, "is CompositeTriggeringPolicy");
            final TriggeringPolicy[] policies = ((CompositeTriggeringPolicy) policy).getTriggeringPolicies();
            assertEquals(2, policies.length);
            assertTrue(policies[0] instanceof TimeBasedTriggeringPolicy, "is TimeBasedTriggeringPolicy");
            assertTrue(policies[1] instanceof SizeBasedTriggeringPolicy, "is SizeBasedTriggeringPolicy");
        }
    }

    @Override
    @Test
    public void testLevelRangeFilter() throws Exception {
        super.testLevelRangeFilter();
    }
}
