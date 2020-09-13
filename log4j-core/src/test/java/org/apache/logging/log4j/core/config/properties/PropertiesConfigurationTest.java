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
package org.apache.logging.log4j.core.config.properties;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.*;

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
}
