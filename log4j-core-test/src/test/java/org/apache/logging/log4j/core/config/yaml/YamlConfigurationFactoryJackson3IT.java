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
package org.apache.logging.log4j.core.config.yaml;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlConfigurationFactoryJackson3IT {

    @Test
    void usesJackson3WhenPresent() throws Exception {
        final ConfigurationSource source = ConfigurationSource.fromUri(
                getClass().getResource("/log4j-jackson3.yaml").toURI());
        final Configuration config = new YamlConfigurationFactory().getConfiguration(null, source);
        assertThat(config).isInstanceOf(Jackson3YamlConfiguration.class);
        config.initialize();
        assertThat(config.getName()).isEqualTo("Jackson3Test");
        final Appender appender = config.getAppender("yes");
        assertThat(appender).isInstanceOf(ListAppender.class);
        final LoggerConfig logger = config.getLoggerConfig("org.example");
        assertThat(logger.getName()).isEqualTo("org.example");
        assertThat(logger.getLevel()).isEqualTo(Level.DEBUG);
        assertThat(logger.isAdditive()).isTrue();
        assertThat(logger.isIncludeLocation()).isTrue();
        assertThat(logger.getAppenderRefs()).extracting(AppenderRef::getRef).containsExactly("yes");
        assertThat(config.getRootLogger().getLevel()).isEqualTo(Level.ERROR);
        assertThat(((Reconfigurable) config).reconfigure()).isInstanceOf(Jackson3YamlConfiguration.class);
    }

    @Test
    void preservesExistingFileWhenAppendUsesYaml11Alias(@TempDir final Path tempDir) throws Exception {
        final Path logFile = tempDir.resolve("app.log");
        Files.write(logFile, "seed\n".getBytes(StandardCharsets.UTF_8));

        final Configuration config = new YamlConfigurationFactory()
                .getConfiguration(null, createAppendConfigurationSource(logFile));
        assertThat(config).isInstanceOf(Jackson3YamlConfiguration.class);

        Appender appender = null;
        try {
            config.initialize();
            appender = config.getAppender("FILE");
            assertThat(appender).isNotNull();
            appender.append(Log4jLogEvent.newBuilder()
                    .setLoggerName("test")
                    .setLevel(Level.INFO)
                    .setMessage(new SimpleMessage("appended"))
                    .build());
        } finally {
            if (appender != null) appender.stop();
            config.stop();
        }

        assertThat(Files.readAllLines(logFile, StandardCharsets.UTF_8)).containsExactly("seed", "appended");
    }

    private static ConfigurationSource createAppendConfigurationSource(final Path logFile) throws IOException {
        final String yaml = String.join(
                "\n",
                "Configuration:",
                "  Appenders:",
                "    File:",
                "      name: FILE",
                "      fileName: '" + logFile.toString().replace('\\', '/') + "'",
                "      append: yes",
                "      PatternLayout:",
                "        pattern: \"%m%n\"",
                "  Loggers:",
                "    Root:",
                "      level: info",
                "      AppenderRef:",
                "        ref: FILE",
                "");
        return new ConfigurationSource(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    }
}
