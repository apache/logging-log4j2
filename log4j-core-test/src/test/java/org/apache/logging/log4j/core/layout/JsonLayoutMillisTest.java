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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests the JsonLayout class with millis.
 */
@Tag("Layouts.Json")
@LoggerContextSource("log4j2-json-layout-timestamp.xml")
public class JsonLayoutMillisTest {

    private Logger logger;

    private void assertEventCount(final List<LogEvent> events, final int expected) {
        assertEquals(events.size(), expected, "Incorrect number of events.");
    }

    @BeforeEach
    public void before(final LoggerContext context, @Named("List") final ListAppender app) {
        logger = context.getLogger("LayoutTest");
        //
        app.clear();
    }

    @Test
    public void testTimestamp(@Named("List") final ListAppender app) {
        logger.info("This is a test message");
        final List<String> message = app.getMessages();
        assertTrue(message != null && !message.isEmpty(), "No messages");
        final String json = message.get(0);
        System.out.println(json);
        assertNotNull(json, "No JSON message");
        assertTrue(json.contains("\"timeMillis\":"), "No timestamp");
        assertFalse(json.contains("instant:"), "Instant is present");
    }
}
