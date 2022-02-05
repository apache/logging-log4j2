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
package org.apache.log4j.config;

import org.apache.log4j.ListAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.bridge.AppenderAdapter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.test.junit.UsingThreadContextMap;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test RewriteAppender
 */
@UsingThreadContextMap
public class MapRewriteAppenderTest {

    @Test
    @LoggerContextSource(value = "log4j1-mapRewrite.xml", v1config = true)
    public void testRewrite() {
        Logger logger = LogManager.getLogger("test");
        Map<String, String> map = new HashMap<>();
        map.put("message", "This is a test");
        map.put("hello", "world");
        logger.debug(map);
        LoggerContext context = (LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
        Configuration configuration = context.getConfiguration();
        Map<String, Appender> appenders = configuration.getAppenders();
        ListAppender eventAppender = null;
        for (Map.Entry<String, Appender> entry : appenders.entrySet()) {
            if (entry.getKey().equals("events")) {
                eventAppender = (ListAppender) ((AppenderAdapter.Adapter) entry.getValue()).getAppender();
            }
        }
        assertNotNull(eventAppender, "No Event Appender");
        List<LoggingEvent> events = eventAppender.getEvents();
        assertTrue(events != null && events.size() > 0, "No events");
        assertNotNull(events.get(0).getProperties(), "No properties in the event");
        assertTrue(events.get(0).getProperties().containsKey("hello"), "Key was not inserted");
        assertEquals("world", events.get(0).getProperties().get("hello"), "Key value is incorrect");
    }
}
