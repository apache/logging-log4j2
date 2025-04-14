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
package com.example;

import java.net.URI;
import java.util.Arrays;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.composite.CompositeConfiguration;

final class Usage {

    private static Configuration createConfiguration() {
        // tag::createConfiguration[]
        ConfigurationBuilder<BuiltConfiguration> configBuilder =
                ConfigurationBuilderFactory.newConfigurationBuilder(); // <1>
        Configuration configuration = configBuilder
                .add(
                        configBuilder // <2>
                                .newAppender("CONSOLE", "List")
                                .add(configBuilder.newLayout("JsonTemplateLayout")))
                .add(
                        configBuilder // <3>
                                .newRootLogger(Level.WARN)
                                .add(configBuilder.newAppenderRef("CONSOLE")))
                .build(false); // <4>
        // end::createConfiguration[]
    }

    private static void useConfiguration() {
        // tag::useConfiguration[]
        Configuration configuration = createConfiguration();
        try (LoggerContext loggerContext = Configurator.initialize(configuration)) {
            // Use `LoggerContext`...
        }
        // end::useConfiguration[]
    }

    private static void reconfigureActiveLoggerContext() {
        // tag::reconfigureActiveLoggerContext[]
        Configuration configuration = createConfiguration();
        Configurator.reconfigure(configuration);
        // end::reconfigureActiveLoggerContext[]
    }

    private static Configuration loadConfigurationFile() {
        // tag::loadConfigurationFile[]
        ConfigurationFactory.getInstance()
                .getConfiguration(
                        null, // <1>
                        null, // <2>
                        URI.create("uri://to/my/log4j2.xml")); // <3>
        // end::loadConfigurationFile[]
    }

    private static Configuration combineConfigurations() {
        // tag::combineConfigurations[]
        ConfigurationFactory configFactory = ConfigurationFactory.getInstance();
        AbstractConfiguration commonConfig = (AbstractConfiguration) // <2>
                configFactory.getConfiguration(null, null, URI.create("classpath:log4j2-common.xml")); // <1>
        AbstractConfiguration appConfig = (AbstractConfiguration) // <2>
                configFactory.getConfiguration(null, null, URI.create("classpath:log4j2-app.xml")); // <1>
        AbstractConfiguration runtimeConfig = ConfigurationBuilderFactory.newConfigurationBuilder()
                // ...
                .build(false); // <3>
        return new CompositeConfiguration(Arrays.asList(commonConfig, appConfig, runtimeConfig)); // <4>
        // end::combineConfigurations[]
    }
}
