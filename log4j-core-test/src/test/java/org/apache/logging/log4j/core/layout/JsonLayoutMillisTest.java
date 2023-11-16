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
package org.apache.logging.log4j.core.layout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.categories.Layouts;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests the JsonLayout class with millis.
 */
@Category(Layouts.Json.class)
public class JsonLayoutMillisTest {

    private static final String CONFIG = "log4j2-json-layout-timestamp.xml";

    private ListAppender app;

    @Rule
    public LoggerContextRule context = new LoggerContextRule(CONFIG);

    private Logger logger;

    private void assertEventCount(final List<LogEvent> events, final int expected) {
        assertEquals("Incorrect number of events.", expected, events.size());
    }

    @Before
    public void before() {
        logger = context.getLogger("LayoutTest");
        //
        app = context.getListAppender("List").clear();
    }

    @Test
    public void testTimestamp() {
        logger.info("This is a test message");
        final List<String> message = app.getMessages();
        assertTrue("No messages", message != null && message.size() > 0);
        final String json = message.get(0);
        System.out.println(json);
        assertNotNull("No JSON message", json);
        assertTrue("No timestamp", json.contains("\"timeMillis\":"));
        assertFalse("Instant is present", json.contains("instant:"));
    }
}
