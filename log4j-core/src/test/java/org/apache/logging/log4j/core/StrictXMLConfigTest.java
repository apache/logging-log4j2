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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class StrictXMLConfigTest {

    private static final String CONFIG = "log4j-strict1.xml";
    private static Configuration config;
    private static ListAppender app;
    private static LoggerContext ctx;

    @BeforeClass
    public static void setupClass() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        ctx = (LoggerContext) LogManager.getContext(false);
        config = ctx.getConfiguration();
        for (final Map.Entry<String, Appender> entry : config.getAppenders().entrySet()) {
            if (entry.getKey().equals("List")) {
                app = (ListAppender) entry.getValue();
                assertNotNull(app);
                break;
            }
        }
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

    org.apache.logging.log4j.Logger logger = LogManager.getLogger("LoggerTest");

    @Test
    public void basicFlow() {
        logger.entry();
        logger.exit();
        final List<LogEvent> events = app.getEvents();
        assertTrue("Incorrect number of events. Expected 2, actual " + events.size(), events.size() == 2);
        app.clear();
    }

    @Test
    public void simpleFlow() {
        logger.entry(CONFIG);
        logger.exit(0);
        final List<LogEvent> events = app.getEvents();
        assertTrue("Incorrect number of events. Expected 2, actual " + events.size(), events.size() == 2);
        app.clear();
    }

    @Test
    public void throwing() {
        logger.throwing(new IllegalArgumentException("Test Exception"));
        final List<LogEvent> events = app.getEvents();
        assertTrue("Incorrect number of events. Expected 1, actual " + events.size(), events.size() == 1);
        app.clear();
    }

    @Test
    public void catching() {
        try {
            throw new NullPointerException();
        } catch (final Exception e) {
            logger.catching(e);
        }
        final List<LogEvent> events = app.getEvents();
        assertTrue("Incorrect number of events. Expected 1, actual " + events.size(), events.size() == 1);
        app.clear();
    }

    @Test
    public void debug() {
        logger.debug("Debug message");
        final List<LogEvent> events = app.getEvents();
        assertTrue("Incorrect number of events. Expected 1, actual " + events.size(), events.size() == 1);
        app.clear();
    }

    @Test
    public void debugObject() {
        logger.debug(new Date());
        final List<LogEvent> events = app.getEvents();
        assertTrue("Incorrect number of events. Expected 1, actual " + events.size(), events.size() == 1);
        app.clear();
    }

    @Test
    public void debugWithParms() {
        logger.debug("Hello, {}", "World");
        final List<LogEvent> events = app.getEvents();
        assertTrue("Incorrect number of events. Expected 1, actual " + events.size(), events.size() == 1);
        app.clear();
    }

    @Test
    public void mdc() {

        ThreadContext.put("TestYear", "2010");
        logger.debug("Debug message");
        ThreadContext.clear();
        logger.debug("Debug message");
        final List<LogEvent> events = app.getEvents();
        assertTrue("Incorrect number of events. Expected 2, actual " + events.size(), events.size() == 2);
        app.clear();
    }

    @Test
    public void structuredData() {
        ThreadContext.put("loginId", "JohnDoe");
        ThreadContext.put("ipAddress", "192.168.0.120");
        ThreadContext.put("locale", Locale.US.getDisplayName());
        final StructuredDataMessage msg = new StructuredDataMessage("Audit@18060", "Transfer Complete", "Transfer");
        msg.put("ToAccount", "123456");
        msg.put("FromAccount", "123457");
        msg.put("Amount", "200.00");
        logger.info(MarkerManager.getMarker("EVENT"), msg);
        ThreadContext.clear();
        final List<LogEvent> events = app.getEvents();
        assertTrue("Incorrect number of events. Expected 1, actual " + events.size(), events.size() == 1);
        app.clear();
    }
}

