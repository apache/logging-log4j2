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
package org.apache.logging.log4j.core.appender.rewrite;

import org.apache.logging.log4j.EventLogger;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.hamcrest.MapMatchers;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.apache.logging.log4j.junit.Named;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@LoggerContextSource("log4j-rewrite.xml")
public class RewriteAppenderTest {
    private final ListAppender app;
    private final ListAppender app2;

    public RewriteAppenderTest(@Named("List") final ListAppender app, @Named("List2") final ListAppender app2) {
        this.app = app.clear();
        this.app2 = app2.clear();
    }

    @Test
    public void rewriteTest() {
        final StructuredDataMessage msg = new StructuredDataMessage("Test", "This is a test", "Service");
        msg.put("Key1", "Value1");
        msg.put("Key2", "Value2");
        EventLogger.logEvent(msg);
        final List<LogEvent> list = app.getEvents();
        assertThat(list, hasSize(1));
        final LogEvent event = list.get(0);
        final Message m = event.getMessage();
        assertThat(m, instanceOf(StructuredDataMessage.class));
        final StructuredDataMessage message = (StructuredDataMessage) m;
        final Map<String, String> map = message.getData();
        assertNotNull(map, "No Map");
        assertThat(map, MapMatchers.hasSize(3));
        final String value = map.get("Key1");
        assertEquals("Apache", value);
    }


    @Test
    public void testProperties(final LoggerContext context) {
        final Logger logger = context.getLogger(RewriteAppenderTest.class);
        logger.debug("Test properties rewrite");
        final List<String> list = app2.getMessages();
        assertThat(list, hasSize(1));
        assertThat(list.get(0), not(containsString("{user.dir}")));
        assertNotNull(list, "No events generated");
        assertEquals(list.size(), 1, "Incorrect number of events. Expected 1, got " + list.size());
        assertFalse(list.get(0).contains("{user."), "Did not resolve user name");
    }


    @Test
    public void testFilter(final LoggerContext context) {
        StructuredDataMessage msg = new StructuredDataMessage("Test", "This is a test", "Service");
        msg.put("Key1", "Value2");
        msg.put("Key2", "Value1");
        final Logger logger = context.getLogger("org.apache.logging.log4j.core.Logging");
        logger.debug(msg);
        msg = new StructuredDataMessage("Test", "This is a test", "Service");
        msg.put("Key1", "Value1");
        msg.put("Key2", "Value2");
        logger.trace(msg);

        assertThat(app.getEvents(), empty());
    }
}
