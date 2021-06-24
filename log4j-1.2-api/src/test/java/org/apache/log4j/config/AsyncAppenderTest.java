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
import org.apache.log4j.xml.XmlConfigurationFactory;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test configuration from XML.
 */
public class AsyncAppenderTest {

    @Test
    public void testAsyncXml() throws Exception {
        LoggerContext loggerContext = configure("target/test-classes/log4j1-async.xml");
        Logger logger = LogManager.getLogger("test");
        logger.debug("This is a test of the root logger");
        Thread.sleep(50);
        Configuration configuration = loggerContext.getConfiguration();
        Map<String, Appender> appenders = configuration.getAppenders();
        ListAppender messageAppender = null;
        for (Map.Entry<String, Appender> entry : appenders.entrySet()) {
            if (entry.getKey().equals("list")) {
                messageAppender = (ListAppender) ((AppenderAdapter.Adapter) entry.getValue()).getAppender();
            }
        }
        assertNotNull("No Message Appender", messageAppender);
        List<String> messages = messageAppender.getMessages();
        assertTrue("No messages", messages != null && messages.size() > 0);
    }

    @Test
    public void testAsyncProperties() throws Exception {
        LoggerContext loggerContext = configure("target/test-classes/log4j1-async.properties");
        Logger logger = LogManager.getLogger("test");
        logger.debug("This is a test of the root logger");
        Thread.sleep(50);
        Configuration configuration = loggerContext.getConfiguration();
        Map<String, Appender> appenders = configuration.getAppenders();
        ListAppender messageAppender = null;
        for (Map.Entry<String, Appender> entry : appenders.entrySet()) {
            if (entry.getKey().equals("list")) {
                messageAppender = (ListAppender) ((AppenderAdapter.Adapter) entry.getValue()).getAppender();
            }
        }
        assertNotNull("No Message Appender", messageAppender);
        List<String> messages = messageAppender.getMessages();
        assertTrue("No messages", messages != null && messages.size() > 0);
    }


    private LoggerContext configure(String configLocation) throws Exception {
        File file = new File(configLocation);
        InputStream is = new FileInputStream(file);
        ConfigurationSource source = new ConfigurationSource(is, file);
        LoggerContextFactory factory = org.apache.logging.log4j.LogManager.getFactory();
        LoggerContext context = (LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
        Configuration configuration;
        if (configLocation.endsWith(".xml")) {
            configuration = new XmlConfigurationFactory().getConfiguration(context, source);
        } else {
            configuration = new PropertiesConfigurationFactory().getConfiguration(context, source);
        }
        assertNotNull("No configuration created", configuration);
        Configurator.reconfigure(configuration);
        return context;
    }

}
