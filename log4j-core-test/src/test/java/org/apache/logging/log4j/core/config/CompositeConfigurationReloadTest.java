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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.composite.CompositeConfiguration;
import org.apache.logging.log4j.core.config.xml.XmlConfigurationFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Isolated
class CompositeConfigurationReloadTest {

    @TempDir
    Path directory;

    private final XmlConfigurationFactory factory = new XmlConfigurationFactory();
    private ConfigurationFactory previousFactory;

    @BeforeEach
    void setUp() {
        previousFactory = ConfigurationFactory.getInstance();
        ConfigurationFactory.setConfigurationFactory(factory);
    }

    @AfterEach
    void tearDown() {
        ConfigurationFactory.setConfigurationFactory(previousFactory);
    }

    @ParameterizedTest
    @CsvSource({"true, false", "false, true", "true, true"})
    void retainsCurrentConfigurationUntilAllSourcesCanReload(final boolean removeFirst, final boolean removeSecond)
            throws Exception {
        final Path first = writeConfiguration("first", Level.INFO);
        final Path second = writeConfiguration("second", Level.INFO);
        try (LoggerContext context = new LoggerContext("compositeReloadFailure")) {
            final CompositeConfiguration original = loadConfiguration(context, first, second);
            context.start(original);
            assertLevels(context, Level.INFO, Level.INFO);
            if (removeFirst) {
                Files.delete(first);
            } else {
                writeConfiguration("first", Level.DEBUG);
            }
            if (removeSecond) {
                Files.delete(second);
            } else {
                writeConfiguration("second", Level.DEBUG);
            }

            context.onChange(original);
            assertSame(original, context.getConfiguration());
            assertLevels(context, Level.INFO, Level.INFO);

            writeConfiguration("first", Level.WARN);
            writeConfiguration("second", Level.DEBUG);
            context.onChange(original);
            assertNotSame(original, context.getConfiguration());
            assertLevels(context, Level.WARN, Level.DEBUG);
        }
    }

    @Test
    void reloadsWhenAllSourcesAreAvailable() throws Exception {
        final Path first = writeConfiguration("first", Level.INFO);
        final Path second = writeConfiguration("second", Level.INFO);
        try (LoggerContext context = new LoggerContext("compositeReloadSuccess")) {
            final CompositeConfiguration original = loadConfiguration(context, first, second);
            context.start(original);
            writeConfiguration("first", Level.WARN);
            writeConfiguration("second", Level.DEBUG);

            context.onChange(original);
            assertNotSame(original, context.getConfiguration());
            assertLevels(context, Level.WARN, Level.DEBUG);
        }
    }

    private static void assertLevels(final LoggerContext context, final Level first, final Level second) {
        final Configuration configuration = context.getConfiguration();
        assertEquals(first, configuration.getLoggerConfig("first").getLevel());
        assertEquals(second, configuration.getLoggerConfig("second").getLevel());
    }

    private CompositeConfiguration loadConfiguration(final LoggerContext context, final Path first, final Path second) {
        return new CompositeConfiguration(Arrays.asList(
                (AbstractConfiguration) factory.getConfiguration(context, "first", first.toUri()),
                (AbstractConfiguration) factory.getConfiguration(context, "second", second.toUri())));
    }

    private Path writeConfiguration(final String name, final Level level) throws Exception {
        final Path path = directory.resolve(name + ".xml");
        final String xml = "<Configuration name=\"" + name + "\"><Loggers><Logger name=\"" + name + "\" level=\""
                + level + "\"/><Root level=\"ERROR\"/></Loggers></Configuration>";
        Files.write(path, xml.getBytes(StandardCharsets.UTF_8));
        return path;
    }
}
