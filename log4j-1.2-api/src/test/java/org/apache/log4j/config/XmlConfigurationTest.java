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

import org.apache.log4j.ListAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.bridge.AppenderAdapter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.xml.XmlConfigurationFactory;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test configuration from XML.
 */
public class XmlConfigurationTest {

    static LoggerContext configureXml(final String configLocation) throws IOException {
        final Path path = Paths.get(configLocation);
        final InputStream is = Files.newInputStream(path);
        final ConfigurationSource source = new ConfigurationSource(is, path.toFile());
        final LoggerContextFactory factory = org.apache.logging.log4j.LogManager.getFactory();
        final LoggerContext context = (LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
        final Configuration configuration = new XmlConfigurationFactory().getConfiguration(context, source);
        assertNotNull("No configuration created", configuration);
        Configurator.reconfigure(configuration);
        return context;
    }

    @Test
    public void testListAppender() throws Exception {
        final LoggerContext loggerContext = configureXml("target/test-classes/log4j1-list.xml");
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

    @Test
    public void testXML() throws Exception {
        configureXml("target/test-classes/log4j1-file.xml");
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
