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

import static org.awaitility.Awaitility.waitAtMost;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.properties.PropertiesConfiguration;
import org.apache.logging.log4j.core.util.Source;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

public class MonitorUriTest {

    private static final int MONITOR_INTERVAL = 3;

    @Test
    void testReconfigureOnChangeInMonitorUri(@TempDir(cleanup = CleanupMode.ON_SUCCESS) final Path tempDir)
            throws IOException {
        ConfigurationBuilder<PropertiesConfiguration> configBuilder =
                ConfigurationBuilderFactory.newConfigurationBuilder(PropertiesConfiguration.class);
        Path config = tempDir.resolve("config.xml");
        Path monitorUri = tempDir.resolve("monitorUri.xml");
        ConfigurationSource configSource = new ConfigurationSource(new Source(config), new byte[] {}, 0);
        Configuration configuration = configBuilder
                .setConfigurationSource(configSource)
                .setMonitorInterval(String.valueOf(MONITOR_INTERVAL))
                .add(configBuilder.newMonitorUri(monitorUri.toUri().toString()))
                .build();

        try (LoggerContext loggerContext = Configurator.initialize(configuration)) {
            Files.write(monitorUri, Collections.singletonList("a change"));
            waitAtMost(MONITOR_INTERVAL + 2, TimeUnit.SECONDS)
                    .until(() -> loggerContext.getConfiguration() != configuration);
        }
    }
}
