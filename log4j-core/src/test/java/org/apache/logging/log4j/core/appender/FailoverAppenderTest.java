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
package org.apache.logging.log4j.core.appender;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.appender.FailOnceAppender;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class FailoverAppenderTest {
    private static final String CONFIG = "log4j-failover.xml";
    private static Configuration config;
    private static ListAppender app;
    private static FailOnceAppender foApp;
    private static LoggerContext ctx;

    @BeforeClass
    public static void setupClass() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        ctx = (LoggerContext) LogManager.getContext(false);
        config = ctx.getConfiguration();
        for (final Map.Entry<String, Appender> entry : config.getAppenders().entrySet()) {
            if (entry.getKey().equals("List")) {
                app = (ListAppender) entry.getValue();
            } else if (entry.getKey().equals("Once")) {
                foApp = (FailOnceAppender) entry.getValue();
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
    org.apache.logging.log4j.Logger onceLogger = LogManager.getLogger("Once");

    @Test
    public void testFailover() {
        app.clear();
        logger.error("This is a test");
        List<LogEvent> events = app.getEvents();
        assertNotNull(events);
        assertTrue("Incorrect number of events. Should be 1 is " + events.size(), events.size() == 1);
        app.clear();
        logger.error("This is a test");
        events = app.getEvents();
        assertNotNull(events);
        assertTrue("Incorrect number of events. Should be 1 is " + events.size(), events.size() == 1);
    }

    @Test
    public void testRecovery() throws Exception {
        app.clear();
        onceLogger.error("Fail once");
        onceLogger.error("Fail again");
        List<LogEvent> events = app.getEvents();
        assertNotNull(events);
        assertTrue("Incorrect number of events. Should be 2 is " + events.size(), events.size() == 2);
        app.clear();
        Thread.sleep(1100);
        onceLogger.error("Fail after recovery interval");
        events = app.getEvents();
        assertTrue("Did not recover", events.size() == 0);
        events = foApp.getEvents();
        assertTrue("No events in primary appender", events.size() == 1);
    }
}
