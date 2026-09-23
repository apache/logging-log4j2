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
package org.apache.logging.log4j.core.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.Tags;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * AsyncLogger must compute caller location when {@code includeLocation} is enabled and
 * {@code atInfo().log} passes a null location.
 */
@Tag(Tags.ASYNC_LOGGERS)
class AsyncLoggerNullLocationTest {

    @BeforeAll
    static void beforeClass() {
        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR, AsyncLoggerContextSelector.class.getName());
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, "AsyncLoggerNullLocationTest.xml");
    }

    @AfterAll
    static void afterClass() {
        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR, Strings.EMPTY);
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
    }

    @Test
    void nullLocationIsComputedWhenIncludeLocationIsEnabled() {
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        final ListAppender app = context.getConfiguration().getAppender("List");
        assertNotNull(app);
        final AsyncLogger logger = (AsyncLogger) context.getLogger("com.foo.Bar");
        final String fqcn = AbstractLogger.class.getName();

        logger.atInfo().log("null-location");

        final StackTraceElement supplied = new StackTraceElement("com.example.Caller", "supplied", "Caller.java", 7);
        logger.logMessage(Level.INFO, null, fqcn, supplied, new SimpleMessage("supplied-location"), null);

        context.stop();

        assertEquals(2, app.getEvents().size());
        final LogEvent computed = app.getEvents().get(0);
        assertNotNull(computed.getSource(), "null location should be computed when includeLocation is enabled");
        assertEquals(
                "nullLocationIsComputedWhenIncludeLocationIsEnabled",
                computed.getSource().getMethodName());

        final LogEvent explicit = app.getEvents().get(1);
        assertNotNull(explicit.getSource());
        assertEquals("com.example.Caller", explicit.getSource().getClassName());
        assertEquals("supplied", explicit.getSource().getMethodName());
        assertEquals(7, explicit.getSource().getLineNumber());
    }
}
