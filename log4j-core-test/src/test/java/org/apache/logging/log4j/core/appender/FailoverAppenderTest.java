/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.appender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.FailOnceAppender;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@LoggerContextSource("log4j-failover.xml")
public class FailoverAppenderTest {
    private final ListAppender app;
    private final FailOnceAppender foApp;
    private final Logger logger;
    private final Logger onceLogger;

    public FailoverAppenderTest(
            final LoggerContext context,
            @Named("List") final ListAppender app,
            @Named("Once") final FailOnceAppender foApp) {
        this.app = app;
        this.foApp = foApp;
        logger = context.getLogger("LoggerTest");
        onceLogger = context.getLogger("Once");
    }

    @AfterEach
    public void tearDown() throws Exception {
        app.clear();
    }

    @Test
    public void testFailover() {
        logger.error("This is a test");
        List<LogEvent> events = app.getEvents();
        assertNotNull(events);
        assertEquals(events.size(), 1, "Incorrect number of events. Should be 1 is " + events.size());
        app.clear();
        logger.error("This is a test");
        events = app.getEvents();
        assertNotNull(events);
        assertEquals(events.size(), 1, "Incorrect number of events. Should be 1 is " + events.size());
    }

    @Test
    public void testRecovery() throws Exception {
        onceLogger.error("Fail once");
        onceLogger.error("Fail again");
        List<LogEvent> events = app.getEvents();
        assertNotNull(events);
        assertEquals(events.size(), 2, "Incorrect number of events. Should be 2 is " + events.size());
        app.clear();
        Thread.sleep(1100);
        onceLogger.error("Fail after recovery interval");
        onceLogger.error("Second log message");
        events = app.getEvents();
        assertEquals(events.size(), 0, "Did not recover");
        events = foApp.drainEvents();
        assertEquals(events.size(), 2, "Incorrect number of events in primary appender");
    }
}
