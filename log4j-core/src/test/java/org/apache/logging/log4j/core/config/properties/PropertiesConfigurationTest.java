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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
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

class PropertiesConfigurationTest {

    @Test
    @LoggerContextSource("log4j2-properties.properties")
    void testPropertiesConfiguration(final Configuration config) {
        assertThat(LifeCycle.State.STARTED).describedAs("Incorrect State: " + config.getState()).isEqualTo(config.getState());
        final Map<String, Appender> appenders = config.getAppenders();
        assertThat(appenders).isNotNull();
        assertThat(appenders.size()).describedAs("Incorrect number of Appenders: " + appenders.size()).isEqualTo(1);
        final Map<String, LoggerConfig> loggers = config.getLoggers();
        assertThat(loggers).isNotNull();
        assertThat(loggers.size()).describedAs("Incorrect number of LoggerConfigs: " + loggers.size()).isEqualTo(2);
        final Filter filter = config.getFilter();
        assertThat(filter).describedAs("No Filter").isNotNull();
        assertTrue(filter instanceof ThresholdFilter, "Not a Threshold Filter");
        final Logger logger = LogManager.getLogger(getClass());
        logger.info("Welcome to Log4j!");
    }

    @Test
    @LoggerContextSource("log4j2-properties-root-only.properties")
    void testRootLoggerOnly(final Configuration config) {
        assertThat(LifeCycle.State.STARTED).describedAs("Incorrect State: " + config.getState()).isEqualTo(config.getState());
        final Map<String, Appender> appenders = config.getAppenders();
        assertThat(appenders).isNotNull();
        assertThat(1).describedAs("Incorrect number of Appenders: " + appenders.size()).isEqualTo(appenders.size());
        final Map<String, LoggerConfig> loggers = config.getLoggers();
        assertThat(loggers).isNotNull();
        assertThat(1).describedAs("Incorrect number of LoggerConfigs: " + loggers.size()).isEqualTo(loggers.size());
        final Filter filter = config.getFilter();
        assertThat(filter).describedAs("No Filter").isNotNull();
        assertThat(filter).isInstanceOf(ThresholdFilter.class);
        final Logger logger = LogManager.getLogger(getClass());
        logger.info("Welcome to Log4j!");
    }

    @Test
    @LoggerContextSource("log4j-rolling.properties")
    void testRollingFile(final Configuration config) {
        assertThat(LifeCycle.State.STARTED).describedAs("Incorrect State: " + config.getState()).isEqualTo(config.getState());
        final Map<String, Appender> appenders = config.getAppenders();
        assertThat(appenders).isNotNull();
        assertThat(3).describedAs("Incorrect number of Appenders: " + appenders.size()).isEqualTo(appenders.size());
        final Map<String, LoggerConfig> loggers = config.getLoggers();
        assertThat(loggers).isNotNull();
        assertThat(2).describedAs("Incorrect number of LoggerConfigs: " + loggers.size()).isEqualTo(loggers.size());
        final Filter filter = config.getFilter();
        assertThat(filter).describedAs("No Filter").isNotNull();
        assertThat(filter).isInstanceOf(ThresholdFilter.class);
        final Logger logger = LogManager.getLogger(getClass());
        logger.info("Welcome to Log4j!");
    }

    @Test
    @LoggerContextSource("log4j2-properties-trailing-space-on-level.properties")
    void testTrailingSpaceOnLevel(final Configuration config) {
        assertThat(LifeCycle.State.STARTED).describedAs("Incorrect State: " + config.getState()).isEqualTo(config.getState());
        final Map<String, Appender> appenders = config.getAppenders();
        assertThat(appenders).isNotNull();
        assertThat(1).describedAs("Incorrect number of Appenders: " + appenders.size()).isEqualTo(appenders.size());
        final Map<String, LoggerConfig> loggers = config.getLoggers();
        assertThat(loggers).isNotNull();
        assertThat(2).describedAs("Incorrect number of LoggerConfigs: " + loggers.size()).isEqualTo(loggers.size());
        final Filter filter = config.getFilter();
        assertThat(filter).describedAs("No Filter").isNotNull();
        assertThat(filter).isInstanceOf(ThresholdFilter.class);
        final Logger logger = LogManager.getLogger(getClass());

        assertThat(logger.getLevel()).describedAs("Incorrect level " + logger.getLevel()).isEqualTo(Level.DEBUG);

        logger.debug("Welcome to Log4j!");
    }
}
