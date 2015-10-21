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
package org.apache.logging.log4j.core.appender.routing;

import java.io.File;
import java.util.List;

import org.apache.logging.log4j.EventLogger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.junit.CleanFiles;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import static org.junit.Assert.*;

/**
 *
 */
public class JsonRoutingAppenderTest {
    private static final String CONFIG = "log4j-routing.json";
    private static final String LOG_FILENAME = "target/rolling1/rollingtest-Unknown.log";

    private static LoggerContextRule context = new LoggerContextRule(CONFIG);

    @ClassRule
    public static RuleChain rules = RuleChain.outerRule(new CleanFiles(LOG_FILENAME)).around(context);

    @Test
    public void routingTest() {
        StructuredDataMessage msg = new StructuredDataMessage("Test", "This is a test", "Service");
        EventLogger.logEvent(msg);
        final List<LogEvent> list = context.getListAppender("List").getEvents();
        assertNotNull("No events generated", list);
        assertTrue("Incorrect number of events. Expected 1, got " + list.size(), list.size() == 1);
        msg = new StructuredDataMessage("Test", "This is a test", "Unknown");
        EventLogger.logEvent(msg);
        final File file = new File(LOG_FILENAME);
        assertTrue("File was not created", file.exists());
    }
}
