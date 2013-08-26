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

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class AppenderRefLevelTest {

    private static final String CONFIG = "log4j-reference-level.xml";
    private static Configuration config;
    private static ListAppender app1;
    private static ListAppender app2;
    private static LoggerContext ctx;

    @BeforeClass
    public static void setupClass() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        ctx = (LoggerContext) LogManager.getContext(false);
        config = ctx.getConfiguration();
        for (final Map.Entry<String, Appender> entry : config.getAppenders().entrySet()) {
            if (entry.getKey().equals("LIST1")) {
                app1 = (ListAppender) entry.getValue();
            } else if (entry.getKey().equals("LIST2")) {
                app2 = (ListAppender) entry.getValue();
            }
        }
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

    org.apache.logging.log4j.Logger logger1 = LogManager.getLogger("org.apache.logging.log4j.test1");
    org.apache.logging.log4j.Logger logger2 = LogManager.getLogger("org.apache.logging.log4j.test2");
    org.apache.logging.log4j.Logger logger3 = LogManager.getLogger("org.apache.logging.log4j.test3");
    Marker testMarker = MarkerManager.getMarker("TEST");

    @Before
    public void before() {
        app1.clear();
        app2.clear();
    }

    @Test
    public void logger1() {
        logger1.entry();
        logger1.debug("debug message");
        logger1.error("Test Message");
        logger1.info("Info Message");
        logger1.warn("warn Message");
        logger1.exit();
        List<LogEvent> events = app1.getEvents();
        assertTrue("Incorrect number of events. Expected 6, actual " + events.size(), events.size() == 6);
        events = app2.getEvents();
        assertTrue("Incorrect number of events. Expected 1, actual " + events.size(), events.size() == 1);
    }

    @Test
    public void logger2() {
        logger2.entry();
        logger2.debug("debug message");
        logger2.error("Test Message");
        logger2.info("Info Message");
        logger2.warn("warn Message");
        logger2.exit();
        List<LogEvent> events = app1.getEvents();
        assertTrue("Incorrect number of events. Expected 2, actual " + events.size(), events.size() == 2);
        events = app2.getEvents();
        assertTrue("Incorrect number of events. Expected 4, actual " + events.size(), events.size() == 4);
    }

    @Test
    public void logger3() {
        logger3.entry();
        logger3.debug(testMarker, "debug message");
        logger3.error("Test Message");
        logger3.info(testMarker, "Info Message");
        logger3.warn("warn Message");
        logger3.exit();
        final List<LogEvent> events = app1.getEvents();
        assertTrue("Incorrect number of events. Expected 4, actual " + events.size(), events.size() == 4);
    }
}

