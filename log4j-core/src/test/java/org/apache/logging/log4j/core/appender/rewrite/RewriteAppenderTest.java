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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.EventLogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class RewriteAppenderTest {
    private static final String CONFIG = "log4j-rewrite.xml";
    private static Configuration config;
    private static ListAppender app;
    private static ListAppender app2;
    private static LoggerContext ctx;

    @BeforeClass
    public static void setupClass() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        ctx = (LoggerContext) LogManager.getContext(false);
        config = ctx.getConfiguration();
        for (final Map.Entry<String, Appender> entry : config.getAppenders().entrySet()) {
            if (entry.getKey().equals("List")) {
                app = (ListAppender) entry.getValue();
            } else if (entry.getKey().equals("List2")) {
                app2 = (ListAppender) entry.getValue();
            }
        }
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

    @Test
    public void rewriteTest() {
        final StructuredDataMessage msg = new StructuredDataMessage("Test", "This is a test", "Service");
        msg.put("Key1", "Value1");
        msg.put("Key2", "Value2");
        EventLogger.logEvent(msg);
        final List<LogEvent> list = app.getEvents();
        assertNotNull("No events generated", list);
        assertTrue("Incorrect number of events. Expected 1, got " + list.size(), list.size() == 1);
        final LogEvent event = list.get(0);
        final Message m = event.getMessage();
        assertTrue("Message is not a MapMessage", m instanceof MapMessage);
        final MapMessage message = (MapMessage) m;
        final Map<String, String> map = message.getData();
        assertNotNull("No Map", map);
        assertTrue("Incorrect number of map entries, expected 3 got " + map.size(), map.size() == 3);
        final String value = map.get("Key1");
        assertEquals("Apache", value);
        app.clear();
    }


    @Test
    public void testProperties() {
        final Logger logger = LogManager.getLogger(RewriteAppenderTest.class);
        logger.debug("Test properties rewrite");
        final List<String> list = app2.getMessages();
        assertNotNull("No events generated", list);
        assertTrue("Incorrect number of events. Expected 1, got " + list.size(), list.size() == 1);
        assertFalse("Did not resolve user name", list.get(0).contains("{user.dir}"));
    }


    @Test
    public void testFilter() {
        StructuredDataMessage msg = new StructuredDataMessage("Test", "This is a test", "Service");
        msg.put("Key1", "Value2");
        msg.put("Key2", "Value1");
        final Logger logger = LogManager.getLogger("org.apache.logging.log4j.core.Logging");
        logger.debug(msg);
        msg = new StructuredDataMessage("Test", "This is a test", "Service");
        msg.put("Key1", "Value1");
        msg.put("Key2", "Value2");
        logger.trace(msg);

        final List<LogEvent> list = app.getEvents();
        assertTrue("Events were generated", list == null || list.size() == 0);
        app.clear();
    }
}
