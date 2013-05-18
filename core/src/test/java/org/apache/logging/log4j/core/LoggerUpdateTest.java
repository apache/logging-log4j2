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
package org.apache.logging.log4j.core;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.XMLConfigurationFactory;
import org.apache.logging.log4j.core.helpers.Constants;
import org.apache.logging.log4j.core.helpers.NetUtils;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.logging.log4j.message.StringFormatterMessageFactory;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.*;

/**
 *
 */
public class LoggerUpdateTest {

    private static final String CONFIG = "log4j-test2.xml";
    private static Configuration config;
    private static ListAppender<LogEvent> app;
    private static ListAppender<String> host;
    private static ListAppender<String> noThrown;
    private static LoggerContext ctx;

    @BeforeClass
    public static void setupClass() {
        System.setProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        ctx = (LoggerContext) LogManager.getContext(false);
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

    @Before
    @SuppressWarnings("unchecked")
    public void before() {
        config = ctx.getConfiguration();
        for (final Map.Entry<String, Appender<?>> entry : config.getAppenders().entrySet()) {
            if (entry.getKey().equals("List")) {
                app = (ListAppender<LogEvent>) entry.getValue();
            } else if (entry.getKey().equals("HostTest")) {
                host = (ListAppender<String>) entry.getValue();
            } else if (entry.getKey().equals("NoThrowable")) {
                noThrown = (ListAppender<String>) entry.getValue();
            }
        }
        assertNotNull("No Appender", app);
        assertNotNull("No Host Appender", host);
        app.clear();
        host.clear();
    }


    org.apache.logging.log4j.Logger logger = LogManager.getLogger("com.apache.test");

    @Test
    public void resetLevel() {
        logger.entry();
        List<LogEvent> events = app.getEvents();
        assertTrue("Incorrect number of events. Expected 1, actual " + events.size(), events.size() == 1);
        app.clear();
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        /* You could also specify the actual logger name as below and it will return the LoggerConfig used by the Logger.
           LoggerConfig loggerConfig = getLoggerConfig("com.apache.test");
        */
        loggerConfig.setLevel(Level.DEBUG);
        ctx.updateLoggers();  // This causes all Loggers to refetch information from their LoggerConfig.
        logger.entry();
        events = app.getEvents();
        assertTrue("Incorrect number of events. Expected 0, actual " + events.size(), events.size() == 0);
        app.clear();
    }
}

