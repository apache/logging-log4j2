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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.waitAtMost;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.util.Source;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

public class MonitorResourcesTest {

    @Test
    void test_reconfiguration(@TempDir(cleanup = CleanupMode.ON_SUCCESS) final Path tempDir) throws IOException {
        final ConfigurationBuilder<BuiltConfiguration> configBuilder =
                ConfigurationBuilderFactory.newConfigurationBuilder();
        final Path configFile = tempDir.resolve("log4j.xml");
        final Path externalResourceFile1 = tempDir.resolve("external-resource-1.txt");
        final Path externalResourceFile2 = tempDir.resolve("external-resource-2.txt");
        final ConfigurationSource configSource = new ConfigurationSource(new Source(configFile), new byte[] {}, 0);
        final int monitorInterval = 3;

        final ComponentBuilder<?> monitorResourcesComponent = configBuilder.newComponent("MonitorResources");
        monitorResourcesComponent.addComponent(configBuilder
                .newComponent("MonitorResource")
                .addAttribute("uri", externalResourceFile1.toUri().toString()));
        monitorResourcesComponent.addComponent(configBuilder
                .newComponent("MonitorResource")
                .addAttribute("uri", externalResourceFile2.toUri().toString()));

        final Configuration config = configBuilder
                .setConfigurationSource(configSource)
                .setMonitorInterval(String.valueOf(monitorInterval))
                .addComponent(monitorResourcesComponent)
                .build();

        try (final LoggerContext loggerContext = Configurator.initialize(config)) {
            assertMonitorResourceFileNames(
                    loggerContext,
                    configFile.getFileName().toString(),
                    externalResourceFile1.getFileName().toString(),
                    externalResourceFile2.getFileName().toString());
            Files.write(externalResourceFile2, Collections.singletonList("a change"));
            waitAtMost(2 * monitorInterval, TimeUnit.SECONDS).until(() -> loggerContext.getConfiguration() != config);
        }
    }

    @Test
    @LoggerContextSource("config/MonitorResource/log4j.xml")
    void test_config_of_type_XML(final LoggerContext loggerContext) {
        assertMonitorResourceFileNames(loggerContext, "log4j.xml");
    }

    @Test
    @LoggerContextSource("config/MonitorResource/log4j.json")
    void test_config_of_type_JSON(final LoggerContext loggerContext) {
        assertMonitorResourceFileNames(loggerContext, "log4j.json");
    }

    @Test
    @LoggerContextSource("config/MonitorResource/log4j.yaml")
    void test_config_of_type_YAML(final LoggerContext loggerContext) {
        assertMonitorResourceFileNames(loggerContext, "log4j.yaml");
    }

    @Test
    @LoggerContextSource("config/MonitorResource/log4j.properties")
    void test_config_of_type_properties(final LoggerContext loggerContext) {
        assertMonitorResourceFileNames(loggerContext, "log4j.properties");
    }

    private static void assertMonitorResourceFileNames(final LoggerContext loggerContext, final String configFileName) {
        assertMonitorResourceFileNames(loggerContext, configFileName, "external-file-1.txt", "external-file-2.txt");
    }

    private static void assertMonitorResourceFileNames(
            final LoggerContext loggerContext, final String configFileName, final String... externalResourceFileNames) {
        final Set<Source> sources = loggerContext
                .getConfiguration()
                .getWatchManager()
                .getConfigurationWatchers()
                .keySet();
        final Set<String> actualFileNames =
                sources.stream().map(source -> source.getFile().getName()).collect(Collectors.toSet());
        final Set<String> expectedFileNames = new LinkedHashSet<>();
        expectedFileNames.add(configFileName);
        expectedFileNames.addAll(Arrays.asList(externalResourceFileNames));
        assertThat(actualFileNames).as("watch manager sources: %s", sources).isEqualTo(expectedFileNames);
    }
}
