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

import static org.assertj.core.api.Assertions.assertThat;
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
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.CustomLevelConfig;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.Constants;
import org.junit.jupiter.api.Test;

public class ConfigurationAssemblerTest {

    @Test
    public void testBuildConfiguration() throws Exception {
        try {
            System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR,
                    "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
            final ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
            CustomConfigurationFactory.addTestFixtures("config name", builder);
            final Configuration configuration = builder.build();
            try (LoggerContext ctx = Configurator.initialize(configuration)) {
                validate(configuration);
            }
        } finally {
            System.getProperties().remove(Constants.LOG4J_CONTEXT_SELECTOR);
        }
    }

    @Test
    public void testCustomConfigurationFactory() throws Exception {
        try {
            System.setProperty(ConfigurationFactory.CONFIGURATION_FACTORY_PROPERTY,
                    "org.apache.logging.log4j.core.config.builder.CustomConfigurationFactory");
            System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR,
                    "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
            final Configuration config = ((LoggerContext) LogManager.getContext(false)).getConfiguration();
            validate(config);
        } finally {
            System.getProperties().remove(Constants.LOG4J_CONTEXT_SELECTOR);
            System.getProperties().remove(ConfigurationFactory.CONFIGURATION_FACTORY_PROPERTY);
        }
    }

    private void validate(final Configuration config) {
        assertThat(config).isNotNull();
        assertThat(config.getName()).isNotNull();
        assertThat(config.getName().isEmpty()).isFalse();
        assertThat(config).describedAs("No configuration created").isNotNull();
        assertThat(LifeCycle.State.STARTED).describedAs("Incorrect State: " + config.getState()).isEqualTo(config.getState());
        final Map<String, Appender> appenders = config.getAppenders();
        assertThat(appenders).isNotNull();
        assertThat(appenders).describedAs("Incorrect number of Appenders: " + appenders.size()).hasSize(1);
        final ConsoleAppender consoleAppender = (ConsoleAppender)appenders.get("Stdout");
        final PatternLayout gelfLayout = (PatternLayout)consoleAppender.getLayout();
        final Map<String, LoggerConfig> loggers = config.getLoggers();
        assertThat(loggers).isNotNull();
        assertThat(loggers).describedAs("Incorrect number of LoggerConfigs: " + loggers.size()).hasSize(2);
        final LoggerConfig rootLoggerConfig = loggers.get("");
        assertThat(rootLoggerConfig.getLevel()).isEqualTo(Level.ERROR);
        assertThat(rootLoggerConfig.isIncludeLocation()).isFalse();
        final LoggerConfig loggerConfig = loggers.get("org.apache.logging.log4j");
        assertThat(loggerConfig.getLevel()).isEqualTo(Level.DEBUG);
        assertThat(loggerConfig.isIncludeLocation()).isTrue();
        final Filter filter = config.getFilter();
        assertThat(filter).describedAs("No Filter").isNotNull();
        assertThat(filter).isInstanceOf(ThresholdFilter.class);
        final List<CustomLevelConfig> customLevels = config.getCustomLevels();
        assertThat(filter).describedAs("No CustomLevels").isNotNull();
        assertThat(customLevels).hasSize(1);
        final CustomLevelConfig customLevel = customLevels.get(0);
        assertThat(customLevel.getLevelName()).isEqualTo("Panic");
        assertThat(customLevel.getIntLevel()).isEqualTo(17);
        final Logger logger = LogManager.getLogger(getClass());
        logger.info("Welcome to Log4j!");
    }
}
