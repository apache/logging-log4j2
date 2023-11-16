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
package org.apache.logging.log4j.core.config.properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

class PropertiesConfigurationTest {

    @Test
    @LoggerContextSource("log4j2-properties.properties")
    void testPropertiesConfiguration(final Configuration config) {
        assertEquals(config.getState(), LifeCycle.State.STARTED, "Incorrect State: " + config.getState());
        final Map<String, Appender> appenders = config.getAppenders();
        assertNotNull(appenders);
        assertEquals(1, appenders.size(), "Incorrect number of Appenders: " + appenders.size());
        final Map<String, LoggerConfig> loggers = config.getLoggers();
        assertNotNull(loggers);
        assertEquals(2, loggers.size(), "Incorrect number of LoggerConfigs: " + loggers.size());
        final Filter filter = config.getFilter();
        assertNotNull(filter, "No Filter");
        assertTrue(filter instanceof ThresholdFilter, "Not a Threshold Filter");
        final Logger logger = LogManager.getLogger(getClass());
        logger.info("Welcome to Log4j!");
    }

    @Test
    @LoggerContextSource("log4j2-properties-root-only.properties")
    void testRootLoggerOnly(final Configuration config) {
        assertEquals(config.getState(), LifeCycle.State.STARTED, "Incorrect State: " + config.getState());
        final Map<String, Appender> appenders = config.getAppenders();
        assertNotNull(appenders);
        assertEquals(appenders.size(), 1, "Incorrect number of Appenders: " + appenders.size());
        final Map<String, LoggerConfig> loggers = config.getLoggers();
        assertNotNull(loggers);
        assertEquals(loggers.size(), 1, "Incorrect number of LoggerConfigs: " + loggers.size());
        final Filter filter = config.getFilter();
        assertNotNull(filter, "No Filter");
        assertThat(filter, instanceOf(ThresholdFilter.class));
        final Logger logger = LogManager.getLogger(getClass());
        logger.info("Welcome to Log4j!");
    }

    @Test
    @LoggerContextSource("log4j-rolling.properties")
    void testRollingFile(final Configuration config) {
        assertEquals(config.getState(), LifeCycle.State.STARTED, "Incorrect State: " + config.getState());
        final Map<String, Appender> appenders = config.getAppenders();
        assertNotNull(appenders);
        assertEquals(appenders.size(), 3, "Incorrect number of Appenders: " + appenders.size());
        final Map<String, LoggerConfig> loggers = config.getLoggers();
        assertNotNull(loggers);
        assertEquals(loggers.size(), 2, "Incorrect number of LoggerConfigs: " + loggers.size());
        final Filter filter = config.getFilter();
        assertNotNull(filter, "No Filter");
        assertThat(filter, instanceOf(ThresholdFilter.class));
        final Logger logger = LogManager.getLogger(getClass());
        logger.info("Welcome to Log4j!");
    }

    @Test
    @LoggerContextSource("log4j2-properties-trailing-space-on-level.properties")
    void testTrailingSpaceOnLevel(final Configuration config) {
        assertEquals(config.getState(), LifeCycle.State.STARTED, "Incorrect State: " + config.getState());
        final Map<String, Appender> appenders = config.getAppenders();
        assertNotNull(appenders);
        assertEquals(appenders.size(), 1, "Incorrect number of Appenders: " + appenders.size());
        final Map<String, LoggerConfig> loggers = config.getLoggers();
        assertNotNull(loggers);
        assertEquals(loggers.size(), 2, "Incorrect number of LoggerConfigs: " + loggers.size());
        final Filter filter = config.getFilter();
        assertNotNull(filter, "No Filter");
        assertThat(filter, instanceOf(ThresholdFilter.class));
        final Logger logger = LogManager.getLogger(getClass());

        assertEquals(Level.DEBUG, logger.getLevel(), "Incorrect level " + logger.getLevel());

        logger.debug("Welcome to Log4j!");
    }

    @Test
    @LoggerContextSource("RootLoggerLevelAppenderTest.properties")
    void testRootLoggerLevelAppender(final LoggerContext context, @Named final ListAppender app) {
        context.getRootLogger().info("Hello world!");
        final List<LogEvent> events = app.getEvents();
        assertEquals(1, events.size());
        assertEquals("Hello world!", events.get(0).getMessage().getFormattedMessage());
    }

    @Test
    @LoggerContextSource("LoggerLevelAppenderTest.properties")
    void testLoggerLevelAppender(
            final LoggerContext context, @Named final ListAppender first, @Named final ListAppender second) {
        context.getLogger(getClass()).atInfo().log("message");
        final List<LogEvent> firstEvents = first.getEvents();
        final List<LogEvent> secondEvents = second.getEvents();
        assertEquals(firstEvents, secondEvents);
        assertEquals(1, firstEvents.size());
    }

    @SetSystemProperty(key = "coreProps", value = "DEBUG, first, second")
    @Test
    @LoggerContextSource("LoggerLevelSysPropsAppenderTest.properties")
    void testLoggerLevelSysPropsAppender(
            final LoggerContext context,
            @Named final ListAppender first,
            @Named final ListAppender second,
            @Named final ListAppender third) {
        context.getLogger(getClass()).atInfo().log("message");
        context.getLogger(getClass()).atDebug().log("debug message");
        context.getRootLogger().atInfo().log("test message");
        final List<LogEvent> firstEvents = first.getEvents();
        final List<LogEvent> secondEvents = second.getEvents();
        assertEquals(firstEvents, secondEvents);
        assertEquals(2, firstEvents.size());
        final List<LogEvent> thirdEvents = third.getEvents();
        assertEquals(1, thirdEvents.size());
    }
}
