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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.log4j.ListAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.bridge.AppenderAdapter;
import org.apache.log4j.bridge.FilterAdapter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.filter.Filterable;
import org.junit.Test;

/**
 * Test configuration from Properties.
 */
public class PropertiesConfigurationTest {

    @Test
    public void testConfigureNullPointerException() throws Exception {
        try (LoggerContext loggerContext = TestConfigurator.configure("target/test-classes/LOG4J2-3247.properties")) {
            // [LOG4J2-3247] configure() should not throw an NPE.
            Configuration configuration = loggerContext.getConfiguration();
            assertNotNull(configuration);
            Appender appender = configuration.getAppender("CONSOLE");
            assertNotNull(appender);
        }
    }

    @Test
    public void testConsoleAppenderFilter() throws Exception {
        try (LoggerContext loggerContext = TestConfigurator.configure("target/test-classes/LOG4J2-3247.properties")) {
            // LOG4J2-3281 PropertiesConfiguration.buildAppender not adding filters to appender
            Configuration configuration = loggerContext.getConfiguration();
            assertNotNull(configuration);
            Appender appender = configuration.getAppender("CONSOLE");
            assertNotNull(appender);
            Filterable filterable = (Filterable) appender;
            FilterAdapter filter = (FilterAdapter) filterable.getFilter();
            assertNotNull(filter);
            assertTrue(filter.getFilter() instanceof NeutralFilterFixture);
        }
    }

    @Test
    public void testCustomAppenderFilter() throws Exception {
        try (LoggerContext loggerContext = TestConfigurator.configure("target/test-classes/LOG4J2-3281.properties")) {
            // LOG4J2-3281 PropertiesConfiguration.buildAppender not adding filters to appender
            Configuration configuration = loggerContext.getConfiguration();
            assertNotNull(configuration);
            Appender appender = configuration.getAppender("CUSTOM");
            assertNotNull(appender);
            Filterable filterable = (Filterable) appender;
            FilterAdapter filter = (FilterAdapter) filterable.getFilter();
            assertNotNull(filter);
            assertTrue(filter.getFilter() instanceof NeutralFilterFixture);
        }
    }

    @Test
    public void testListAppender() throws Exception {
        try (LoggerContext loggerContext = TestConfigurator.configure("target/test-classes/log4j1-list.properties")) {
            Logger logger = LogManager.getLogger("test");
            logger.debug("This is a test of the root logger");
            Configuration configuration = loggerContext.getConfiguration();
            Map<String, Appender> appenders = configuration.getAppenders();
            ListAppender eventAppender = null;
            ListAppender messageAppender = null;
            for (Map.Entry<String, Appender> entry : appenders.entrySet()) {
                if (entry.getKey().equals("list")) {
                    messageAppender = (ListAppender) ((AppenderAdapter.Adapter) entry.getValue()).getAppender();
                } else if (entry.getKey().equals("events")) {
                    eventAppender = (ListAppender) ((AppenderAdapter.Adapter) entry.getValue()).getAppender();
                }
            }
            assertNotNull("No Event Appender", eventAppender);
            assertNotNull("No Message Appender", messageAppender);
            List<LoggingEvent> events = eventAppender.getEvents();
            assertTrue("No events", events != null && events.size() > 0);
            List<String> messages = messageAppender.getMessages();
            assertTrue("No messages", messages != null && messages.size() > 0);
        }
    }

    @Test
    public void testProperties() throws Exception {
        try (LoggerContext loggerContext = TestConfigurator.configure("target/test-classes/log4j1-file.properties")) {
            Logger logger = LogManager.getLogger("test");
            logger.debug("This is a test of the root logger");
            File file = new File("target/temp.A1");
            assertTrue("File A1 was not created", file.exists());
            assertTrue("File A1 is empty", file.length() > 0);
            file = new File("target/temp.A2");
            assertTrue("File A2 was not created", file.exists());
            assertTrue("File A2 is empty", file.length() > 0);
        }
    }

}