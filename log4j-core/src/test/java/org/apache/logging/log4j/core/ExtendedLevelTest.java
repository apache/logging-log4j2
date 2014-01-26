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
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.ExtendedLevels;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class ExtendedLevelTest {

    private static final String CONFIG = "log4j-customLevel.xml";
    private static LoggerContext ctx;
    private static ListAppender list1;
    private static ListAppender list2;

    @BeforeClass
    public static void setupClass() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        ctx = (LoggerContext) LogManager.getContext(false);
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

    @Before
    public void before() {
        Configuration config = ctx.getConfiguration();
        for (final Map.Entry<String, Appender> entry : config.getAppenders().entrySet()) {
            if (entry.getKey().equals("List1")) {
                list1 = (ListAppender) entry.getValue();
            } else if (entry.getKey().equals("List2")) {
                list2 = (ListAppender) entry.getValue();
            }
        }
        assertNotNull("No List1 Appender", list1);
        assertNotNull("No List2 Appender", list2);
        list1.clear();
        list2.clear();
    }

    @Test
    public void testLevelLogging() {
        org.apache.logging.log4j.Logger logger = LogManager.getLogger("org.apache.logging.log4j.test1");
        logger.log(ExtendedLevels.DETAIL, "Detail message");
        logger.log(Level.DEBUG, "Debug message");
        List<LogEvent> events = list1.getEvents();
        assertNotNull("No events", events);
        assertTrue("Incorrect number of events. Expected 1 got " + events.size(), events.size() == 1);
        LogEvent event = events.get(0);
        assertTrue("Expected level DETAIL, got" + event.getLevel(), event.getLevel().name().equals("DETAIL"));
        logger = LogManager.getLogger("org.apache.logging.log4j.test2");
        logger.log(ExtendedLevels.NOTE, "Note message");
        logger.log(Level.INFO, "Info message");
        events = list2.getEvents();
        assertNotNull("No events", events);
        assertTrue("Incorrect number of events. Expected 1 got " + events.size(), events.size() == 1);
        event = events.get(0);
        assertTrue("Expected level NOTE, got" + event.getLevel(), event.getLevel().name().equals("NOTE"));
    }
}
