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
package org.apache.logging.log4j.core.config.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.junit.Test;

/**
 *
 */
public class ConfigurationAssemblerTest {

    @Test
    public void testBuildConfiguration() throws Exception {
        final ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        CustomConfigurationFactory.addTestFixtures("config name", builder);
        Configuration configuration = builder.build();
        Configurator.initialize(configuration);
        validate(configuration);
    }

    @Test
    public void testCustomConfigurationFactory() throws Exception {
        try {
            System.setProperty(ConfigurationFactory.CONFIGURATION_FACTORY_PROPERTY,
                    "org.apache.logging.log4j.core.config.builder.CustomConfigurationFactory");
            Configuration config = ((LoggerContext) LogManager.getContext(false)).getConfiguration();
            validate(config);
        } finally {
            System.getProperties().remove(ConfigurationFactory.CONFIGURATION_FACTORY_PROPERTY);
        }
    }

    private void validate(Configuration config) {
        assertNotNull(config);
        assertNotNull(config.getName());
        assertFalse(config.getName().isEmpty());
        assertNotNull("No configuration created", config);
        assertEquals("Incorrect State: " + config.getState(), config.getState(), LifeCycle.State.STARTED);
        Map<String, Appender> appenders = config.getAppenders();
        assertNotNull(appenders);
        assertTrue("Incorrect number of Appenders: " + appenders.size(), appenders.size() == 1);
        Map<String, LoggerConfig> loggers = config.getLoggers();
        assertNotNull(loggers);
        assertTrue("Incorrect number of LoggerConfigs: " + loggers.size(), loggers.size() == 2);
        Filter filter = config.getFilter();
        assertNotNull("No Filter", filter);
        assertTrue("Not a Threshold Filter", filter instanceof ThresholdFilter);
        Logger logger = LogManager.getLogger(getClass());
        logger.info("Welcome to Log4j!");
    }
}
