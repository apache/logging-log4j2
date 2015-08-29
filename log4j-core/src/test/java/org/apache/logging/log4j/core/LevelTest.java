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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 *
 */
public class LevelTest {

    private static final String CONFIG = "log4j-Level.xml";
    private ListAppender listAll;
    private ListAppender listTrace;
    private ListAppender listDebug;
    private ListAppender listInfo;
    private ListAppender listWarn;
    private ListAppender listError;
    private ListAppender listFatal;

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule(CONFIG);

    @Before
    public void before() {
        listAll = context.getListAppender("ListAll").clear();
        listTrace = context.getListAppender("ListTrace").clear();
        listDebug = context.getListAppender("ListDebug").clear();
        listInfo = context.getListAppender("ListInfo").clear();
        listWarn = context.getListAppender("ListWarn").clear();
        listError = context.getListAppender("ListError").clear();
        listFatal = context.getListAppender("ListFatal").clear();
    }

    // Helper class
    private static class Expected {
        final ListAppender appender;
        final int expectedEventCount;
        final String expectedInitialEventLevel;
        final String description;

        Expected(final ListAppender appender, final int expectedCount, final String level, final String description) {
            this.appender = appender;
            this.expectedEventCount = expectedCount;
            this.expectedInitialEventLevel = level;
            this.description = description;
        }
    }

    @Test
    public void testLevelLogging() {
        final Marker marker = MarkerManager.getMarker("marker");
        final Message msg = new ObjectMessage("msg");
        final Throwable t = new Throwable("test");
        final Level[] levels = new Level[] { Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.FATAL };
        final String[] names = new String[] { "levelTest", "levelTest.Trace", "levelTest.Debug", "levelTest.Info",
                "levelTest.Warn", "levelTest.Error", "levelTest.Fatal" };
        for (final Level level : levels) {
            for (final String name : names) {
                final Logger logger = context.getLogger(name);
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
        final Expected[] expectedResults = new Expected[] { //
        new Expected(listAll, UNIT * levelCount, "TRACE", "All"), //
                new Expected(listTrace, UNIT * levelCount--, "TRACE", "Trace"), //
                new Expected(listDebug, UNIT * levelCount--, "DEBUG", "Debug"), //
                new Expected(listInfo, UNIT * levelCount--, "INFO", "Info"), //
                new Expected(listWarn, UNIT * levelCount--, "WARN", "Warn"), //
                new Expected(listError, UNIT * levelCount--, "ERROR", "Error"), //
                new Expected(listFatal, UNIT * levelCount--, "FATAL", "Fatal"), //
        };
        for (final Expected expected : expectedResults) {
            final String description = expected.description;
            final List<LogEvent> events = expected.appender.getEvents();
            assertNotNull(description + ": No events", events);
            assertThat(events, hasSize(expected.expectedEventCount));
            final LogEvent event = events.get(0);
            assertEquals(
                description + ": Expected level " + expected.expectedInitialEventLevel + ", got" + event.getLevel(),
                event.getLevel().name(), expected.expectedInitialEventLevel);
        }
    }
}
