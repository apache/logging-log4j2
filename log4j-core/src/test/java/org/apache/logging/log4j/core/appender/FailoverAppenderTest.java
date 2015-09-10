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

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.FailOnceAppender;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class FailoverAppenderTest {
    private ListAppender app;
    private FailOnceAppender foApp;
    private Logger logger;
    private Logger onceLogger;

    @ClassRule
    public static LoggerContextRule init = new LoggerContextRule("log4j-failover.xml");

    @Before
    public void setUp() throws Exception {
        app = init.getListAppender("List");
        foApp = (FailOnceAppender) init.getAppender("Once");
        logger = init.getLogger("LoggerTest");
        onceLogger = init.getLogger("Once");
    }

    @After
    public void tearDown() throws Exception {
        if (app != null) {
            app.clear();
        }
    }

    @Test
    public void testFailover() {
        logger.error("This is a test");
        List<LogEvent> events = app.getEvents();
        assertNotNull(events);
        assertEquals("Incorrect number of events. Should be 1 is " + events.size(), events.size(), 1);
        app.clear();
        logger.error("This is a test");
        events = app.getEvents();
        assertNotNull(events);
        assertEquals("Incorrect number of events. Should be 1 is " + events.size(), events.size(), 1);
    }

    @Test
    public void testRecovery() throws Exception {
        onceLogger.error("Fail once");
        onceLogger.error("Fail again");
        List<LogEvent> events = app.getEvents();
        assertNotNull(events);
        assertEquals("Incorrect number of events. Should be 2 is " + events.size(), events.size(), 2);
        app.clear();
        Thread.sleep(1100);
        onceLogger.error("Fail after recovery interval");
        onceLogger.error("Second log message");
        events = app.getEvents();
        assertEquals("Did not recover", events.size(), 0);
        events = foApp.getEvents();
        assertEquals("Incorrect number of events in primary appender", events.size(), 2);
    }
}
