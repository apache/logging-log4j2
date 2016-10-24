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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 *
 */
public class PropertiesConfigurationRootLoggerOnlyTest {

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule("log4j2-properties-root-only.properties");

    @Test
    public void testPropertiesConfiguration() {
        final Configuration config = context.getConfiguration();
        assertNotNull("No configuration created", config);
        assertEquals("Incorrect State: " + config.getState(), config.getState(), LifeCycle.State.STARTED);
        final Map<String, Appender> appenders = config.getAppenders();
        assertNotNull(appenders);
        assertTrue("Incorrect number of Appenders: " + appenders.size(), appenders.size() == 1);
        final Map<String, LoggerConfig> loggers = config.getLoggers();
        assertNotNull(loggers);
        assertTrue("Incorrect number of LoggerConfigs: " + loggers.size(), loggers.size() == 1);
        final Filter filter = config.getFilter();
        assertNotNull("No Filter", filter);
        assertTrue("Not a Threshold Filter", filter instanceof ThresholdFilter);
        final Logger logger = LogManager.getLogger(getClass());
        logger.info("Welcome to Log4j!");
    }
}
