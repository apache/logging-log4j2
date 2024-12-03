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
package org.apache.log4j.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.ListAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.bridge.AppenderAdapter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Test RewriteAppender
 */
public class MapRewriteAppenderTest {

    @BeforeAll
    public static void beforeAll() {
        System.setProperty(
                ConfigurationFactory.LOG4J1_CONFIGURATION_FILE_PROPERTY, "target/test-classes/log4j1-mapRewrite.xml");
    }

    @AfterEach
    public void after() {
        ThreadContext.clearMap();
    }

    @Test
    public void testRewrite() {
        final Logger logger = LogManager.getLogger("test");
        final Map<String, String> map = new HashMap<>();
        map.put("message", "This is a test");
        map.put("hello", "world");
        logger.debug(map);
        final LoggerContext context = (LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
        final Configuration configuration = context.getConfiguration();
        final Map<String, Appender> appenders = configuration.getAppenders();
        ListAppender eventAppender = null;
        for (Map.Entry<String, Appender> entry : appenders.entrySet()) {
            if (entry.getKey().equals("events")) {
                eventAppender = (ListAppender) ((AppenderAdapter.Adapter) entry.getValue()).getAppender();
            }
        }
        assertNotNull(eventAppender, "No Event Appender");
        final List<LoggingEvent> events = eventAppender.getEvents();
        assertTrue(events != null && events.size() > 0, "No events");
        assertNotNull(events.get(0).getProperties(), "No properties in the event");
        assertTrue(events.get(0).getProperties().containsKey("hello"), "Key was not inserted");
        assertEquals("world", events.get(0).getProperties().get("hello"), "Key value is incorrect");
    }
}
