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
package org.apache.logging.log4j.core.test.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.apache.logging.log4j.plugins.di.DI;
import org.junit.jupiter.api.Test;

public abstract class AbstractNestedLoggerConfigTest {

    @Test
    public void testInheritParentDefaultLevel() throws IOException {
        final Configuration configuration = loadConfiguration("/default-level.xml");
        try {
            assertEquals(Level.ERROR, configuration.getLoggerConfig("com.foo").getLevel());
        } finally {
            configuration.stop();
        }
    }

    @Test
    public void testInheritParentLevel() throws IOException {
        final Configuration configuration = loadConfiguration("/inherit-level.xml");
        try {
            assertEquals(Level.TRACE, configuration.getLoggerConfig("com.foo").getLevel());
        } finally {
            configuration.stop();
        }
    }

    private Configuration loadConfiguration(final String resourcePath) throws IOException {
        try (final InputStream in = getClass().getResourceAsStream(getClass().getSimpleName() + resourcePath)) {
            final Configuration configuration = new XmlConfiguration(
                    new LoggerContext("test", null, (URI) null, DI.createInitializedFactory()),
                    new ConfigurationSource(in));
            configuration.initialize();
            configuration.start();
            return configuration;
        }
    }
}
