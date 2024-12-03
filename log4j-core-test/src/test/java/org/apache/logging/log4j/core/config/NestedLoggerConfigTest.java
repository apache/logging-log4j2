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

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for LoggerConfig hierarchies.
 */
public class NestedLoggerConfigTest {

    public static Stream<String> data() {
        return Stream.of("logger-config/LoggerConfig/", "logger-config/AsyncLoggerConfig/");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void testInheritParentDefaultLevel(String prefix) throws IOException {
        final Configuration configuration = loadConfiguration(prefix + "default-level.xml");
        try {
            assertEquals(Level.ERROR, configuration.getLoggerConfig("com.foo").getLevel());
        } finally {
            configuration.stop();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void testInheritParentLevel(String prefix) throws IOException {
        final Configuration configuration = loadConfiguration(prefix + "inherit-level.xml");
        try {
            assertEquals(Level.TRACE, configuration.getLoggerConfig("com.foo").getLevel());
        } finally {
            configuration.stop();
        }
    }

    private Configuration loadConfiguration(final String resourcePath) throws IOException {
        try (final InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            final Configuration configuration =
                    new XmlConfiguration(new LoggerContext("test"), new ConfigurationSource(in));
            configuration.initialize();
            configuration.start();
            return configuration;
        }
    }
}
