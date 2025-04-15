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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
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
 *
 */
public class PropertiesRoutingAppenderTest {
    private static final String CONFIG = "log4j-routing.properties";
    private static final String UNKNOWN_LOG_FILE = "target/rolling1/rollingtestProps-Unknown.log";
    private static final String ALERT_LOG_FILE = "target/routing1/routingtestProps-Alert.log";
    private static final String ACTIVITY_LOG_FILE = "target/routing1/routingtestProps-Activity.log";

    private ListAppender app;

    private final LoggerContextRule loggerContextRule = new LoggerContextRule(CONFIG);

    @Rule
    public RuleChain rules = loggerContextRule.withCleanFilesRule(UNKNOWN_LOG_FILE, ALERT_LOG_FILE, ACTIVITY_LOG_FILE);

    @Before
    public void setUp() {
        this.app = this.loggerContextRule.getListAppender("List");
    }

    @After
    public void tearDown() {
        this.app.clear();
        this.loggerContextRule.getLoggerContext().stop();
    }

    @Test
    public void routingTest() {
        StructuredDataMessage msg = new StructuredDataMessage("Test", "This is a test", "Service");
        EventLogger.logEvent(msg);
        final List<LogEvent> list = app.getEvents();
        assertNotNull("No events generated", list);
        assertEquals("Incorrect number of events. Expected 1, got " + list.size(), 1, list.size());
        msg = new StructuredDataMessage("Test", "This is a test", "Alert");
        EventLogger.logEvent(msg);
        File file = new File(ALERT_LOG_FILE);
        assertTrue("Alert file was not created", file.exists());
        msg = new StructuredDataMessage("Test", "This is a test", "Activity");
        EventLogger.logEvent(msg);
        file = new File(ACTIVITY_LOG_FILE);
        assertTrue("Activity file was not created", file.exists());
    }
}
