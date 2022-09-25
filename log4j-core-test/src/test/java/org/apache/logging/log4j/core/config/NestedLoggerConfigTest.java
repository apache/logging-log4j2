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
package org.apache.logging.log4j.core.config;

import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Tests for LoggerConfig hierarchies.
 */
@RunWith(Parameterized.class)
public class NestedLoggerConfigTest {

    @Parameterized.Parameters(name = "{0}")
    public static List<String> data() throws IOException {
        return ImmutableList.of("logger-config/LoggerConfig/", "logger-config/AsyncLoggerConfig/");
    }

    private final String prefix;

    public NestedLoggerConfigTest(final String prefix) {
        this.prefix = prefix;
    }

    @Test
    public void testInheritParentDefaultLevel() throws IOException {
        final Configuration configuration = loadConfiguration(prefix + "default-level.xml");
        try {
            assertEquals(Level.ERROR, configuration.getLoggerConfig("com.foo").getLevel());
        } finally {
            configuration.stop();
        }
    }

    @Test
    public void testInheritParentLevel() throws IOException {
        final Configuration configuration = loadConfiguration(prefix + "inherit-level.xml");
        try {
            assertEquals(Level.TRACE, configuration.getLoggerConfig("com.foo").getLevel());
        } finally {
            configuration.stop();
        }
    }

    private Configuration loadConfiguration(final String resourcePath) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            final Configuration configuration = new XmlConfiguration(new LoggerContext("test"), new ConfigurationSource(in));
            configuration.initialize();
            configuration.start();
            return configuration;
        }
    }
}
