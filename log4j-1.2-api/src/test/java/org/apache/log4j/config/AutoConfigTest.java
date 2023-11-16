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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.apache.log4j.ListAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.bridge.AppenderAdapter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.spi.LoggerContext;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test configuration from XML.
 */
public class AutoConfigTest {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(ConfigurationFactory.LOG4J1_EXPERIMENTAL, "true");
    }

    @Test
    public void testListAppender() {
        final Logger logger = LogManager.getLogger("test");
        logger.debug("This is a test of the root logger");
        final LoggerContext loggerContext = org.apache.logging.log4j.LogManager.getContext(false);
        final Configuration configuration =
                ((org.apache.logging.log4j.core.LoggerContext) loggerContext).getConfiguration();
        final Map<String, Appender> appenders = configuration.getAppenders();
        ListAppender eventAppender = null;
        ListAppender messageAppender = null;
        for (Map.Entry<String, Appender> entry : appenders.entrySet()) {
            if (entry.getKey().equals("list")) {
                messageAppender = (ListAppender) ((AppenderAdapter.Adapter) entry.getValue()).getAppender();
            } else if (entry.getKey().equals("events")) {
                eventAppender = (ListAppender) ((AppenderAdapter.Adapter) entry.getValue()).getAppender();
            }
        }
        assertNotNull("No Event Appender", eventAppender);
        assertNotNull("No Message Appender", messageAppender);
        final List<LoggingEvent> events = eventAppender.getEvents();
        assertTrue("No events", events != null && events.size() > 0);
        final List<String> messages = messageAppender.getMessages();
        assertTrue("No messages", messages != null && messages.size() > 0);
    }
}
