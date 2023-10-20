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

import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;

@LoggerContextSource("log4j-test2.xml")
public class LoggerUpdateTest {

    private final ListAppender app;

    public LoggerUpdateTest(@Named("List") final ListAppender app) {
        this.app = app.clear();
    }

    @Test
    public void resetLevel(final LoggerContext context) {
        final org.apache.logging.log4j.Logger logger = context.getLogger("com.apache.test");
        logger.traceEntry();
        List<LogEvent> events = app.getEvents();
        assertThat(events).hasSize(1);
        app.clear();
        final Configuration config = context.getConfiguration();
        final LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        /* You could also specify the actual logger name as below and it will return the LoggerConfig used by the Logger.
           LoggerConfig loggerConfig = getLoggerConfig("com.apache.test");
        */
        loggerConfig.setLevel(Level.DEBUG);
        context.updateLoggers();  // This causes all Loggers to refetch information from their LoggerConfig.
        logger.traceEntry();
        events = app.getEvents();
        assertThat(events).isEmpty();
    }

    @Test
    @Timeout(3)
    public void testUpdateLoggersPropertyListeners(final LoggerContext context) throws Exception {
        final Configuration config = context.getConfiguration();
        context.addConfigurationStartedListener(configuration -> assertSame(config, configuration));
        context.updateLoggers();
    }
}
