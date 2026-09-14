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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.EventLogger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.CleanFiles;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests Routing appender purge facilities
 */
@Tag("sleepy")
public class RoutingAppenderWithPurgingTest {
    private static final String IDLE_LOG_FILE1 = "target/routing-purge-idle/routingtest-1.log";
    private static final String IDLE_LOG_FILE2 = "target/routing-purge-idle/routingtest-2.log";
    private static final String IDLE_LOG_FILE3 = "target/routing-purge-idle/routingtest-3.log";
    private static final String MANUAL_LOG_FILE1 = "target/routing-purge-manual/routingtest-1.log";
    private static final String MANUAL_LOG_FILE2 = "target/routing-purge-manual/routingtest-2.log";
    private static final String MANUAL_LOG_FILE3 = "target/routing-purge-manual/routingtest-3.log";

    @RegisterExtension
    CleanFiles cleanFiles = new CleanFiles(
            false,
            true,
            10,
            IDLE_LOG_FILE1,
            IDLE_LOG_FILE2,
            IDLE_LOG_FILE3,
            MANUAL_LOG_FILE1,
            MANUAL_LOG_FILE2,
            MANUAL_LOG_FILE3);

    @Test
    @Timeout(5000)
    @LoggerContextSource("log4j-routing-purge.xml")
    public void routingTest(
            final LoggerContext loggerContext,
            @Named("List") final ListAppender app,
            @Named("RoutingPurgeIdle") final RoutingAppender routingAppenderIdle,
            @Named("RoutingPurgeIdleWithHangingAppender") final RoutingAppender routingAppenderIdleWithHangingAppender,
            @Named("RoutingPurgeManual") final RoutingAppender routingAppenderManual,
            @Named("ReferencedList") final ListAppender referencedListAppender)
            throws InterruptedException {
        StructuredDataMessage msg = new StructuredDataMessage("1", "This is a test 1", "Service");
        EventLogger.logEvent(msg);
        final List<LogEvent> list = app.getEvents();
        assertNotNull(list, "No events generated");
        assertTrue(list.size() == 1, "Incorrect number of events. Expected 1, got " + list.size());
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

        assertFalse(((ListAppender) referencedListAppender).getEvents().isEmpty());

        assertEquals(
                2, routingAppenderIdle.getAppenders().size(), "Incorrect number of appenders with IdlePurgePolicy.");
        assertEquals(
                2,
                routingAppenderIdleWithHangingAppender.getAppenders().size(),
                "Incorrect number of appenders with IdlePurgePolicy with HangingAppender.");
        assertEquals(2, routingAppenderManual.getAppenders().size(), "Incorrect number of appenders manual purge.");

        Thread.sleep(3000);
        EventLogger.logEvent(msg);

        assertEquals(
                1, routingAppenderIdle.getAppenders().size(), "Incorrect number of appenders with IdlePurgePolicy.");
        assertEquals(
                2, routingAppenderManual.getAppenders().size(), "Incorrect number of appenders with manual purge.");

        routingAppenderManual.deleteAppender("1");
        routingAppenderManual.deleteAppender("2");
        routingAppenderManual.deleteAppender("3");

        assertEquals(
                1, routingAppenderIdle.getAppenders().size(), "Incorrect number of appenders with IdlePurgePolicy.");
        assertEquals(
                0, routingAppenderManual.getAppenders().size(), "Incorrect number of appenders with manual purge.");

        assertFalse(referencedListAppender.isStopped(), "Reference based routes should not be stoppable");

        msg = new StructuredDataMessage("5", "This is a test 5", "Service");
        EventLogger.logEvent(msg);

        assertEquals(
                1, routingAppenderManual.getAppenders().size(), "Incorrect number of appenders with manual purge.");

        routingAppenderManual.deleteAppender("5");
        routingAppenderManual.deleteAppender("5");

        assertEquals(
                0, routingAppenderManual.getAppenders().size(), "Incorrect number of appenders with manual purge.");
    }

    private void assertFileExistance(final String... files) {
        for (final String file : files) {
            assertTrue(new File(file).exists(), "File should exist - " + file + " file ");
        }
    }
}
