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

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class AppenderRefLevelTest {

    private static final String CONFIG = "log4j-reference-level.xml";
    private ListAppender app1;
    private ListAppender app2;

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule(CONFIG);

    org.apache.logging.log4j.Logger logger1 = context.getLogger("org.apache.logging.log4j.test1");
    org.apache.logging.log4j.Logger logger2 = context.getLogger("org.apache.logging.log4j.test2");
    org.apache.logging.log4j.Logger logger3 = context.getLogger("org.apache.logging.log4j.test3");
    Marker testMarker = MarkerManager.getMarker("TEST");

    @Before
    public void before() {
        app1 = context.getListAppender("LIST1").clear();
        app2 = context.getListAppender("LIST2").clear();
    }

    @Test
    public void logger1() {
        logger1.traceEntry();
        logger1.debug("debug message");
        logger1.error("Test Message");
        logger1.info("Info Message");
        logger1.warn("warn Message");
        logger1.traceExit();
        List<LogEvent> events = app1.getEvents();
        assertEquals("Incorrect number of events. Expected 6, actual " + events.size(), 6, events.size());
        events = app2.getEvents();
        assertEquals("Incorrect number of events. Expected 1, actual " + events.size(), 1, events.size());
    }

    @Test
    public void logger2() {
        logger2.traceEntry();
        logger2.debug("debug message");
        logger2.error("Test Message");
        logger2.info("Info Message");
        logger2.warn("warn Message");
        logger2.traceExit();
        List<LogEvent> events = app1.getEvents();
        assertEquals("Incorrect number of events. Expected 2, actual " + events.size(), events.size(), 2);
        events = app2.getEvents();
        assertEquals("Incorrect number of events. Expected 4, actual " + events.size(), events.size(), 4);
    }

    @Test
    public void logger3() {
        logger3.traceEntry();
        logger3.debug(testMarker, "debug message");
        logger3.error("Test Message");
        logger3.info(testMarker, "Info Message");
        logger3.warn("warn Message");
        logger3.traceExit();
        final List<LogEvent> events = app1.getEvents();
        assertEquals("Incorrect number of events. Expected 4, actual " + events.size(), 4, events.size());
    }
}

