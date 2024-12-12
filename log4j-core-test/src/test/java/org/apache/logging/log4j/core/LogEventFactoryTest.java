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
package org.apache.logging.log4j.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.util.List;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.impl.DefaultLogEventFactory;
import org.apache.logging.log4j.core.impl.LocationAwareLogEventFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.LogEventFactory;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.message.Message;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class LogEventFactoryTest {

    @BeforeAll
    static void setupEventFactory() throws Exception {
        System.setProperty(Constants.LOG4J_LOG_EVENT_FACTORY, TestLogEventFactory.class.getName());
        resetLogEventFactory(new TestLogEventFactory());
    }

    @AfterAll
    static void resetEventFactory() throws Exception {
        System.clearProperty(Constants.LOG4J_LOG_EVENT_FACTORY);
        resetLogEventFactory(new DefaultLogEventFactory());
    }

    private static void resetLogEventFactory(final LogEventFactory logEventFactory) throws IllegalAccessException {
        final Field field = FieldUtils.getField(LoggerConfig.class, "LOG_EVENT_FACTORY", true);
        FieldUtils.removeFinalModifier(field);
        FieldUtils.writeStaticField(field, logEventFactory, false);
    }

    @Test
    @LoggerContextSource("log4j2-config.xml")
    public void testEvent(LoggerContext context) {
        ListAppender app = context.getConfiguration().getAppender("List");
        app.clear();

        final Logger logger = context.getLogger("org.apache.test.LogEventFactory");

        logger.error("error message");

        final List<LogEvent> events = app.getEvents();
        assertNotNull(events, "No events");
        assertEquals(1, events.size(), "Incorrect number of events. Expected 1, actual " + events.size());

        final LogEvent event = events.get(0);
        assertEquals("Test", event.getLoggerName(), "TestLogEventFactory wasn't used");
    }

    public static class TestLogEventFactory implements LogEventFactory, LocationAwareLogEventFactory {

        @Override
        public LogEvent createEvent(
                final String loggerName,
                final Marker marker,
                final String fqcn,
                final Level level,
                final Message data,
                final List<Property> properties,
                final Throwable t) {
            return new Log4jLogEvent("Test", marker, fqcn, level, data, properties, t);
        }

        @Override
        public LogEvent createEvent(
                final String loggerName,
                final Marker marker,
                final String fqcn,
                final StackTraceElement location,
                final Level level,
                final Message data,
                final List<Property> properties,
                final Throwable t) {
            return new Log4jLogEvent("Test", marker, fqcn, level, data, properties, t);
        }
    }
}
