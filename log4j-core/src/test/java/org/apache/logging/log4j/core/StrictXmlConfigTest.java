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

import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.message.EntryMessage;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class StrictXmlConfigTest {

    private static final String CONFIG = "log4j-strict1.xml";
    private ListAppender app;

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule(CONFIG);

    @Before
    public void setUp() throws Exception {
        app = context.getListAppender("List").clear();
    }

    org.apache.logging.log4j.Logger logger = context.getLogger("LoggerTest");

    @Test
    public void basicFlow() {
        final EntryMessage entry = logger.traceEntry();
        logger.traceExit(entry);
        final List<LogEvent> events = app.getEvents();
        assertEquals("Incorrect number of events. Expected 2, actual " + events.size(), 2, events.size());
    }

    @Test
    public void basicFlowDeprecated() {
        logger.traceEntry();
        logger.traceExit();
        final List<LogEvent> events = app.getEvents();
        assertEquals("Incorrect number of events. Expected 2, actual " + events.size(), 2, events.size());
    }

    @Test
    public void simpleFlow() {
        logger.entry(CONFIG);
        logger.exit(0);
        final List<LogEvent> events = app.getEvents();
        assertEquals("Incorrect number of events. Expected 2, actual " + events.size(), 2, events.size());
    }

    @Test
    public void throwing() {
        logger.throwing(new IllegalArgumentException("Test Exception"));
        final List<LogEvent> events = app.getEvents();
        assertEquals("Incorrect number of events. Expected 1, actual " + events.size(), 1, events.size());
    }

    @Test
    public void catching() {
        try {
            throw new NullPointerException();
        } catch (final Exception e) {
            logger.catching(e);
        }
        final List<LogEvent> events = app.getEvents();
        assertEquals("Incorrect number of events. Expected 1, actual " + events.size(), 1, events.size());
    }

    @Test
    public void debug() {
        logger.debug("Debug message");
        final List<LogEvent> events = app.getEvents();
        assertEquals("Incorrect number of events. Expected 1, actual " + events.size(), 1, events.size());
    }

    @Test
    public void debugObject() {
        logger.debug(new Date());
        final List<LogEvent> events = app.getEvents();
        assertEquals("Incorrect number of events. Expected 1, actual " + events.size(), 1, events.size());
    }

    @Test
    public void debugWithParms() {
        logger.debug("Hello, {}", "World");
        final List<LogEvent> events = app.getEvents();
        assertEquals("Incorrect number of events. Expected 1, actual " + events.size(), 1, events.size());
    }

    @Test
    public void mdc() {
        ThreadContext.put("TestYear", "2010");
        logger.debug("Debug message");
        ThreadContext.clearMap();
        logger.debug("Debug message");
        final List<LogEvent> events = app.getEvents();
        assertEquals("Incorrect number of events. Expected 2, actual " + events.size(), 2, events.size());
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
        ThreadContext.clearMap();
        final List<LogEvent> events = app.getEvents();
        assertEquals("Incorrect number of events. Expected 1, actual " + events.size(), 1, events.size());
    }
}

