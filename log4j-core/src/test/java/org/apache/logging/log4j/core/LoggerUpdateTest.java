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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import org.apache.logging.log4j.*;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class LoggerUpdateTest {

    private static final String CONFIG = "log4j-test2.xml";
    private ListAppender app;

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule(CONFIG);

    @Before
    public void before() {
        app = context.getListAppender("List").clear();
    }

    @Test
    public void resetLevel() {
        final org.apache.logging.log4j.Logger logger = context.getLogger("com.apache.test");
        logger.traceEntry();
        List<LogEvent> events = app.getEvents();
        assertEquals("Incorrect number of events. Expected 1, actual " + events.size(), 1, events.size());
        app.clear();
        final LoggerContext ctx = LoggerContext.getContext(false);
        final Configuration config = ctx.getConfiguration();
        final LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        /* You could also specify the actual logger name as below and it will return the LoggerConfig used by the Logger.
           LoggerConfig loggerConfig = getLoggerConfig("com.apache.test");
        */
        loggerConfig.setLevel(Level.DEBUG);
        ctx.updateLoggers();  // This causes all Loggers to refetch information from their LoggerConfig.
        logger.traceEntry();
        events = app.getEvents();
        assertEquals("Incorrect number of events. Expected 0, actual " + events.size(), 0, events.size());
    }

    @Test
    public void testUpdateLoggersPropertyListeners() throws Exception {
        final LoggerContext ctx = context.getLoggerContext();
        ctx.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                assertEquals(LoggerContext.PROPERTY_CONFIG, evt.getPropertyName());
                assertSame(ctx, evt.getSource());
            }
        });
        ctx.updateLoggers();
    }
}

