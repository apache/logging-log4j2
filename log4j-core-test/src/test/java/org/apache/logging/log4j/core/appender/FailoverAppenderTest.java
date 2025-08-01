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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.FailOnceAppender;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.junit.jupiter.api.Test;

public class FailoverAppenderTest {

    @Test
    @LoggerContextSource("log4j-failover.xml")
    void testFailover(final LoggerContext context, @Named("List") final ListAppender app) {
        final Logger logger = context.getLogger("LoggerTest");
        logger.error("This is a test");
        List<LogEvent> events = app.getEvents();
        assertNotNull(events);
        assertEquals(1, events.size(), "Incorrect number of events. Should be 1 is " + events.size());
        app.clear();
        logger.error("This is a test");
        events = app.getEvents();
        assertNotNull(events);
        assertEquals(1, events.size(), "Incorrect number of events. Should be 1 is " + events.size());
    }

    @Test
    @LoggerContextSource("log4j-failover.xml")
    void testRecovery(
            final LoggerContext context,
            @Named("List") final ListAppender app,
            @Named("Once") final FailOnceAppender foApp)
            throws Exception {
        final Logger onceLogger = context.getLogger("Once");
        onceLogger.error("Fail once");
        onceLogger.error("Fail again");
        List<LogEvent> events = app.getEvents();
        assertNotNull(events);
        assertEquals(2, events.size(), "Incorrect number of events. Should be 2 is " + events.size());
        app.clear();
        Thread.sleep(1100);
        onceLogger.error("Fail after recovery interval");
        onceLogger.error("Second log message");
        events = app.getEvents();
        assertEquals(0, events.size(), "Did not recover");
        events = foApp.drainEvents();
        assertEquals(2, events.size(), "Incorrect number of events in primary appender");
    }

    @Test
    @LoggerContextSource("log4j-failover-location.xml")
    void testRequiresLocation(final LoggerContext context) {
        final FailoverAppender appender = context.getConfiguration().getAppender("Failover");
        assertTrue(appender.requiresLocation());
    }
}
