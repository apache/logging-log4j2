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
package org.apache.logging.log4j.script.config.builder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.CustomLevelConfig;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.junit.jupiter.api.Test;

public class ConfigurationAssemblerTest {

    @Test
    public void testBuildConfiguration() throws Exception {
        final ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        CustomConfigurationFactory.addTestFixtures("config name", builder);
        final Configuration configuration = builder.build();
        try (final LoggerContext ctx = Configurator.initialize(configuration)) {
            validate(configuration);
        }
    }

    @Test
    public void testCustomConfigurationFactory() throws Exception {
        try {
            System.setProperty(
                    Log4jPropertyKey.CONFIG_CONFIGURATION_FACTORY_CLASS_NAME.getSystemKey(),
                    "org.apache.logging.log4j.script.config.builder.CustomConfigurationFactory");
            final Configuration config = ((LoggerContext) LogManager.getContext(false)).getConfiguration();
            validate(config);
        } finally {
            System.getProperties().remove(Log4jPropertyKey.CONFIG_CONFIGURATION_FACTORY_CLASS_NAME.getSystemKey());
        }
    }

    private void validate(final Configuration config) {
        assertNotNull(config);
        assertNotNull(config.getName());
        assertFalse(config.getName().isEmpty());
        assertNotNull(config, "No configuration created");
        assertEquals(config.getState(), LifeCycle.State.STARTED, "Incorrect State: " + config.getState());
        final Map<String, Appender> appenders = config.getAppenders();
        assertNotNull(appenders);
        assertEquals(appenders.size(), 1, "Incorrect number of Appenders: " + appenders.size());
        final Map<String, LoggerConfig> loggers = config.getLoggers();
        assertNotNull(loggers);
        assertEquals(loggers.size(), 2, "Incorrect number of LoggerConfigs: " + loggers.size());
        final LoggerConfig rootLoggerConfig = loggers.get("");
        assertEquals(Level.ERROR, rootLoggerConfig.getLevel());
        assertFalse(rootLoggerConfig.isIncludeLocation());
        final LoggerConfig loggerConfig = loggers.get("org.apache.logging.log4j");
        assertEquals(Level.DEBUG, loggerConfig.getLevel());
        assertTrue(loggerConfig.isIncludeLocation());
        final Filter filter = config.getFilter();
        assertNotNull(filter, "No Filter");
        assertThat(filter, instanceOf(ThresholdFilter.class));
        final List<CustomLevelConfig> customLevels = config.getCustomLevels();
        assertNotNull(filter, "No CustomLevels");
        assertEquals(1, customLevels.size());
        final CustomLevelConfig customLevel = customLevels.get(0);
        assertEquals("Panic", customLevel.getLevelName());
        assertEquals(17, customLevel.getIntLevel());
        final Logger logger = LogManager.getLogger(getClass());
        logger.info("Welcome to Log4j!");
    }
}
