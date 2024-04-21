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

import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.impl.ContextData;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.LogEventFactory;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.apache.logging.log4j.core.test.junit.TestBinding;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.StringMap;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class LogEventFactoryTest {

    @Test
    @TestBinding(api = LogEventFactory.class, implementation = TestLogEventFactory.class)
    @LoggerContextSource(value = "log4j2-config.xml")
    public void testEvent(@Named("List") final ListAppender app, final LoggerContext context) {
        final org.apache.logging.log4j.Logger logger = context.getLogger("org.apache.test.LogEventFactory");
        logger.error("error message");
        final List<LogEvent> events = app.getEvents();
        assertNotNull(events, "No events");
        assertEquals(1, events.size(), "Incorrect number of events. Expected 1, actual " + events.size());
        final LogEvent event = events.get(0);
        assertEquals("Test", event.getLoggerName(), "TestLogEventFactory wasn't used");
    }

    public static class TestLogEventFactory implements LogEventFactory {

        @Override
        public LogEvent createEvent(
                final String loggerName,
                final Marker marker,
                final String fqcn,
                final Level level,
                final Message data,
                final List<Property> properties,
                final Throwable t) {
            StringMap contextData = ContextDataFactory.createContextData();
            if (properties != null && !properties.isEmpty()) {
                for (Property property : properties) {
                    contextData.putValue(property.getName(), property.getValue());
                }
            }
            ContextData.addAll(contextData);
            return Log4jLogEvent.newBuilder()
                    .setLoggerName("Test")
                    .setMarker(marker)
                    .setLoggerFqcn(fqcn)
                    .setLevel(level)
                    .setMessage(data)
                    .setContextData(contextData)
                    .setThrown(t)
                    .build();
        }
    }
}
