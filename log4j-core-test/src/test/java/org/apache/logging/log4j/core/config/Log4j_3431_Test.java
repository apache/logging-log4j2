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
package org.apache.logging.log4j.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the change for Log4j issue #3431.
 * <p>
 *   The configuration name should not be set to a default if a name already exists.
 * </p>
 *
 * @see <a href="https://github.com/apache/logging-log4j2/issues/3431"/>
 */
@SuppressWarnings("NewClassNamingConvention")
class Log4j_3431_Test {

    /**
     * Tests that the name of a configurations with no defined loggers is <strong>not</strong> reset when
     * the configuration is started.
     */
    @Test
    void testConfigurationDefaults_WithName() {

        try (final LoggerContext ctx = new LoggerContext("Log4j_3431_Test")) {

            final String name = "Log4j_3431_Configuration";

            Configuration config = ConfigurationBuilderFactory.newConfigurationBuilder()
                    .setConfigurationName(name)
                    .setConfigurationSource(ConfigurationSource.NULL_SOURCE)
                    .build(false);

            // a configuration with no defined loggers should trigger AbstractConfiguration 'setToDefault()'
            //   from 'doConfigure()'

            ctx.start(config);

            assertEquals(name, config.getName(), "The name of the configuration should be '" + name + "'");
        }
    }

    /**
     * Tests that the name of a configurations with no defined loggers is set to a default when
     * the configuration is started.
     */
    @Test
    void testConfigurationDefaults_WithNoName() {

        try (final LoggerContext ctx = new LoggerContext("Log4j_3431_Test")) {

            final String name = "Log4j_3431_Configuration";

            Configuration config = ConfigurationBuilderFactory.newConfigurationBuilder()
                    .setConfigurationSource(ConfigurationSource.NULL_SOURCE)
                    .build(false);

            // a configuration with no defined loggers should trigger AbstractConfiguration 'setToDefault()'
            //   from 'doConfigure()'

            ctx.start(config);

            final String expectedPrefix = DefaultConfiguration.DEFAULT_NAME + "@";
            Assertions.assertThatCharSequence(config.getName())
                    .withFailMessage("The name of the configuration should start with '" + expectedPrefix + "'.")
                    .isNotBlank()
                    .startsWith(expectedPrefix);
        }
    }
}
