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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import org.apache.logging.log4j.EventLogger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.CleanFiles;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 *
 */
@LoggerContextSource("log4j-routing.xml")
public class RoutingAppenderTest {
    private static final String UNKNOWN_LOG_FILE = "target/rolling1/rollingtest-Unknown.log";
    private static final String ALERT_LOG_FILE = "target/routing1/routingtest-Alert.log";
    private static final String ACTIVITY_LOG_FILE = "target/routing1/routingtest-Activity.log";

    private ListAppender app;
    private LoggerContext context = null;

    RoutingAppenderTest(LoggerContext context) {
        this.context = context;
    }

    @RegisterExtension
    CleanFiles cleanFiles = new CleanFiles(false, true, 10, UNKNOWN_LOG_FILE, ALERT_LOG_FILE, ACTIVITY_LOG_FILE);

    @BeforeEach
    public void beforeEach() {
        this.app = this.context.getConfiguration().getAppender("List");
    }

    @AfterEach
    public void tearDown() throws Exception {
        this.app.clear();
        this.context.stop();
    }

    @Test
    public void routingTest() {
        StructuredDataMessage msg = new StructuredDataMessage("Test", "This is a test", "Service");
        EventLogger.logEvent(msg);
        final List<LogEvent> list = app.getEvents();
        assertNotNull(list, "No events generated");
        assertTrue(list.size() == 1, "Incorrect number of events. Expected 1, got " + list.size());
        msg = new StructuredDataMessage("Test", "This is a test", "Alert");
        EventLogger.logEvent(msg);
        File file = new File(ALERT_LOG_FILE);
        assertTrue(file.exists(), "Alert file was not created");
        msg = new StructuredDataMessage("Test", "This is a test", "Activity");
        EventLogger.logEvent(msg);
        file = new File(ACTIVITY_LOG_FILE);
        assertTrue(file.exists(), "Activity file was not created");
    }
}
