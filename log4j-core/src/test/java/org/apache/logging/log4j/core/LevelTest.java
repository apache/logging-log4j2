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

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.apache.logging.log4j.Level.*;

import static org.junit.Assert.*;

/**
 *
 */
public class LevelTest {

    private static final String CONFIG = "log4j-Level.xml";
    private static LoggerContext ctx;
    private static ListAppender listAll;
    private static ListAppender listTrace;
    private static ListAppender listDebug;
    private static ListAppender listInfo;
    private static ListAppender listWarn;
    private static ListAppender listError;
    private static ListAppender listFatal;

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
            if (entry.getKey().equals("ListAll")) {
                listAll = (ListAppender) entry.getValue();
            } else if (entry.getKey().equals("ListTrace")) {
                listTrace = (ListAppender) entry.getValue();
            } else if (entry.getKey().equals("ListDebug")) {
                listDebug = (ListAppender) entry.getValue();
            } else if (entry.getKey().equals("ListInfo")) {
                listInfo = (ListAppender) entry.getValue();
            } else if (entry.getKey().equals("ListWarn")) {
                listWarn = (ListAppender) entry.getValue();
            } else if (entry.getKey().equals("ListError")) {
                listError = (ListAppender) entry.getValue();
            } else if (entry.getKey().equals("ListFatal")) {
                listFatal = (ListAppender) entry.getValue();
            }
        }
        assertNotNull("No listAll Appender", listAll);
        assertNotNull("No listTrace Appender", listTrace);
        assertNotNull("No listDebug Appender", listDebug);
        assertNotNull("No listInfo Appender", listInfo);
        assertNotNull("No listWarn Appender", listWarn);
        assertNotNull("No listError Appender", listError);
        assertNotNull("No listFatal Appender", listFatal);
        listAll.clear();
        listTrace.clear();
        listDebug.clear();
        listInfo.clear();
        listWarn.clear();
        listError.clear();
        listFatal.clear();
    }

    // Helper class
    class Expected {
        final ListAppender appender;
        final int expectedEventCount;
        final String expectedInitialEventLevel;
        final String description;

        Expected(ListAppender appender, int expectedCount, String level, String description) {
            this.appender = appender;
            this.expectedEventCount = expectedCount;
            this.expectedInitialEventLevel = level;
            this.description = description;
        }
    }

    @Test
    public void testLevelLogging() {
        Marker marker = MarkerManager.getMarker("marker");
        Message msg = new ObjectMessage("msg");
        Throwable t = new Throwable("test");
        Level[] levels = new Level[] { TRACE, DEBUG, INFO, WARN, ERROR, FATAL };
        String[] names = new String[] { "levelTest", "levelTest.Trace", "levelTest.Debug", "levelTest.Info",
                "levelTest.Warn", "levelTest.Error", "levelTest.Fatal" };
        for (Level level : levels) {
            for (String name : names) {
                org.apache.logging.log4j.Logger logger = LogManager.getLogger(name);
                logger.log(level, msg); // Message
                logger.log(level, 123); // Object
                logger.log(level, name); // String
                logger.log(level, marker, msg); // Marker, Message
                logger.log(level, marker, 123); // Marker, Object
                logger.log(level, marker, name); // marker, String
                logger.log(level, msg, t); // Message, Throwable
                logger.log(level, 123, t); // Object, Throwable
                logger.log(level, name, "param1", "param2"); // String, Object...
                logger.log(level, name, t); // String, Throwable
                logger.log(level, marker, msg, t); // Marker, Message, Throwable
                logger.log(level, marker, 123, t); // Marker, Object, Throwable
                logger.log(level, marker, name, "param1", "param2"); // Marker, String, Object...
                logger.log(level, marker, name, t); // Marker, String, Throwable
            }
        }
        // Logger "levelTest" will not receive same events as "levelTest.Trace"
        int levelCount = names.length - 1;

        final int UNIT = 14;
        Expected[] expectedResults = new Expected[] { //
        new Expected(listAll, UNIT * levelCount, "TRACE", "All"), //
                new Expected(listTrace, UNIT * levelCount--, "TRACE", "Trace"), //
                new Expected(listDebug, UNIT * levelCount--, "DEBUG", "Debug"), //
                new Expected(listInfo, UNIT * levelCount--, "INFO", "Info"), //
                new Expected(listWarn, UNIT * levelCount--, "WARN", "Warn"), //
                new Expected(listError, UNIT * levelCount--, "ERROR", "Error"), //
                new Expected(listFatal, UNIT * levelCount--, "FATAL", "Fatal"), //
        };
        for (Expected expected : expectedResults) {
            final String description = expected.description;
            final List<LogEvent> events = expected.appender.getEvents();
            assertNotNull(description + ": No events", events);
            assertTrue(description + ": Incorrect number of events. Expected " + expected.expectedEventCount + " got "
                    + events.size(), events.size() == expected.expectedEventCount);
            LogEvent event = events.get(0);
            assertTrue(
                    description + ": Expected level " + expected.expectedInitialEventLevel + ", got" + event.getLevel(),
                    event.getLevel().name().equals(expected.expectedInitialEventLevel));
        }
    }
}
