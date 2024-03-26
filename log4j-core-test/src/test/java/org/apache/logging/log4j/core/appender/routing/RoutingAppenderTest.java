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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.util.List;
import org.apache.logging.log4j.EventLogger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.junit.jupiter.api.Test;

class RoutingAppenderTest {
    private static final String ALERT_LOG_FILE = "routingtest-Alert.log";
    private static final String ACTIVITY_LOG_FILE = "routingtest-Activity.log";

    @TempLoggingDir
    private static Path loggingPath;

    @Test
    @LoggerContextSource
    void routingTest(@Named("List") final ListAppender app) {
        StructuredDataMessage msg = new StructuredDataMessage("Test", "This is a test", "Service");
        EventLogger.logEvent(msg);
        final List<LogEvent> list = app.getEvents();
        assertNotNull(list, "No events generated");
        assertEquals(1, list.size(), "Incorrect number of events. Expected 1, got " + list.size());
        msg = new StructuredDataMessage("Test", "This is a test", "Alert");
        EventLogger.logEvent(msg);
        assertThat(loggingPath.resolve(ALERT_LOG_FILE))
                .as("check 'Alert' log file")
                .exists();

        msg = new StructuredDataMessage("Test", "This is a test", "Activity");
        EventLogger.logEvent(msg);
        assertThat(loggingPath.resolve(ACTIVITY_LOG_FILE))
                .as("check 'Activity' log file")
                .exists();
    }
}
