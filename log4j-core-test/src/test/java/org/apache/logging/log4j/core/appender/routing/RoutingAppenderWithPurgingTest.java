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
package org.apache.logging.log4j.core.appender.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.EventLogger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * Tests Routing appender purge facilities
 */
public class RoutingAppenderWithPurgingTest {
    private static final String CONFIG = "log4j-routing-purge.xml";
    private static final String IDLE_LOG_FILE1 = "target/routing-purge-idle/routingtest-1.log";
    private static final String IDLE_LOG_FILE2 = "target/routing-purge-idle/routingtest-2.log";
    private static final String IDLE_LOG_FILE3 = "target/routing-purge-idle/routingtest-3.log";
    private static final String MANUAL_LOG_FILE1 = "target/routing-purge-manual/routingtest-1.log";
    private static final String MANUAL_LOG_FILE2 = "target/routing-purge-manual/routingtest-2.log";
    private static final String MANUAL_LOG_FILE3 = "target/routing-purge-manual/routingtest-3.log";

    private ListAppender app;
    private RoutingAppender routingAppenderIdle;
    private RoutingAppender routingAppenderIdleWithHangingAppender;
    private RoutingAppender routingAppenderManual;

    private final LoggerContextRule loggerContextRule = new LoggerContextRule(CONFIG);

    @Rule
    public RuleChain chain = loggerContextRule.withCleanFilesRule(
            IDLE_LOG_FILE1, IDLE_LOG_FILE2, IDLE_LOG_FILE3, MANUAL_LOG_FILE1, MANUAL_LOG_FILE2, MANUAL_LOG_FILE3);

    @Before
    public void setUp() {
        this.app = this.loggerContextRule.getListAppender("List");
        this.routingAppenderIdle =
                this.loggerContextRule.getRequiredAppender("RoutingPurgeIdle", RoutingAppender.class);
        this.routingAppenderIdleWithHangingAppender = this.loggerContextRule.getRequiredAppender(
                "RoutingPurgeIdleWithHangingAppender", RoutingAppender.class);
        this.routingAppenderManual =
                this.loggerContextRule.getRequiredAppender("RoutingPurgeManual", RoutingAppender.class);
    }

    @After
    public void tearDown() {
        this.app.clear();
        this.loggerContextRule.getLoggerContext().stop();
    }

    @Test(timeout = 5000)
    public void routingTest() throws InterruptedException {
        StructuredDataMessage msg = new StructuredDataMessage("1", "This is a test 1", "Service");
        EventLogger.logEvent(msg);
        final List<LogEvent> list = app.getEvents();
        assertNotNull("No events generated", list);
        assertEquals("Incorrect number of events. Expected 1, got " + list.size(), 1, list.size());
        msg = new StructuredDataMessage("2", "This is a test 2", "Service");
        EventLogger.logEvent(msg);
        msg = new StructuredDataMessage("3", "This is a test 3", "Service");
        EventLogger.logEvent(msg);
        // '2' is a referenced list appender
        final String[] files = {IDLE_LOG_FILE1, IDLE_LOG_FILE3, MANUAL_LOG_FILE1, MANUAL_LOG_FILE3};
        assertFileExistance(files);
        final Set<String> expectedAppenderKeys = new HashSet<>(2);
        expectedAppenderKeys.add("1");
        expectedAppenderKeys.add("3");
        assertEquals(expectedAppenderKeys, routingAppenderManual.getAppenders().keySet());

        assertFalse(((ListAppender) loggerContextRule.getAppender("ReferencedList"))
                .getEvents()
                .isEmpty());

        assertEquals(
                "Incorrect number of appenders with IdlePurgePolicy.",
                2,
                routingAppenderIdle.getAppenders().size());
        assertEquals(
                "Incorrect number of appenders with IdlePurgePolicy with HangingAppender.",
                2,
                routingAppenderIdleWithHangingAppender.getAppenders().size());
        assertEquals(
                "Incorrect number of appenders manual purge.",
                2,
                routingAppenderManual.getAppenders().size());

        Thread.sleep(3000);
        EventLogger.logEvent(msg);

        assertEquals(
                "Incorrect number of appenders with IdlePurgePolicy.",
                1,
                routingAppenderIdle.getAppenders().size());
        assertEquals(
                "Incorrect number of appenders with manual purge.",
                2,
                routingAppenderManual.getAppenders().size());

        routingAppenderManual.deleteAppender("1");
        routingAppenderManual.deleteAppender("2");
        routingAppenderManual.deleteAppender("3");

        assertEquals(
                "Incorrect number of appenders with IdlePurgePolicy.",
                1,
                routingAppenderIdle.getAppenders().size());
        assertEquals(
                "Incorrect number of appenders with manual purge.",
                0,
                routingAppenderManual.getAppenders().size());

        assertFalse(
                "Reference based routes should not be stoppable",
                loggerContextRule.getAppender("ReferencedList").isStopped());

        msg = new StructuredDataMessage("5", "This is a test 5", "Service");
        EventLogger.logEvent(msg);

        assertEquals(
                "Incorrect number of appenders with manual purge.",
                1,
                routingAppenderManual.getAppenders().size());

        routingAppenderManual.deleteAppender("5");
        routingAppenderManual.deleteAppender("5");

        assertEquals(
                "Incorrect number of appenders with manual purge.",
                0,
                routingAppenderManual.getAppenders().size());
    }

    private void assertFileExistance(final String... files) {
        for (final String file : files) {
            assertTrue("File should exist - " + file + " file ", new File(file).exists());
        }
    }
}
