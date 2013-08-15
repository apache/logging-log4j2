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
package org.apache.logging.log4j.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.helpers.Constants;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.LogEventFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class LogEventFactoryTest {

    private static final String CONFIG = "log4j2-config.xml";
    private static Configuration config;
    private static ListAppender app;
    private static LoggerContext ctx;

    @BeforeClass
    public static void setupClass() {
        System.setProperty(Constants.LOG4J_LOG_EVENT_FACTORY, TestLogEventFactory.class.getName());
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        ctx = (LoggerContext) LogManager.getContext(false);
        config = ctx.getConfiguration();
        for (final Map.Entry<String, Appender> entry : config.getAppenders().entrySet()) {
            if (entry.getKey().equals("List")) {
                app = (ListAppender) entry.getValue();
                break;
            }
        }
        if (app == null) {
            fail("No List Appender could be found");
        }
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

    @Before
    public void before() {
        app.clear();
    }

    @Test
    public void testEvent() {
        final org.apache.logging.log4j.Logger logger = LogManager.getLogger("org.apache.test.LogEventFactory");
        logger.error("error message");
        final List<LogEvent> events = app.getEvents();
        assertNotNull("No events", events);
        assertTrue("Incorrect number of events. Expected 1, actual " + events.size(), events.size() == 1);
        final LogEvent event = events.get(0);
        assertTrue("Test LogEventFactory wasn't used", event.getLoggerName().equals("Test"));
    }

    public static class TestLogEventFactory implements LogEventFactory {

        @Override
        public LogEvent createEvent(final String loggerName, final Marker marker,
                                    final String fqcn, final Level level, final Message data,
                                    final List<Property> properties, final Throwable t) {
            return new Log4jLogEvent("Test", marker, fqcn, level, data, properties, t);
        }
    }
}

