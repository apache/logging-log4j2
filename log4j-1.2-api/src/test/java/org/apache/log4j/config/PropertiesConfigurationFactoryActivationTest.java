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
package org.apache.log4j.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URL;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.WritesSystemProperty;

@WritesSystemProperty
class PropertiesConfigurationFactoryActivationTest {

    @Test
    void activationTracksTheCompatibilityProperty() throws Exception {
        final String property = ConfigurationFactory.LOG4J1_EXPERIMENTAL;
        final String previousValue = System.getProperty(property);
        final PropertiesConfigurationFactory factory = new PropertiesConfigurationFactory();
        try {
            System.setProperty(property, "false");
            assertFalse(factory.isActive());
            assertInactive(factory);

            System.setProperty(property, "true");
            assertTrue(factory.isActive());
            assertArrayEquals(new String[] {".properties"}, factory.getSupportedTypes());
            final URL resource = getClass().getResource("/config-1.2/log4j-console-SimpleLayout.properties");
            final ConfigurationSource source = ConfigurationSource.fromUri(resource.toURI());
            try (final LoggerContext context = new LoggerContext("test")) {
                assertInstanceOf(PropertiesConfiguration.class, factory.getConfiguration(context, source));
            }

            System.setProperty(property, "false");
            assertFalse(factory.isActive());
            assertInactive(factory);
        } finally {
            if (previousValue == null) {
                System.clearProperty(property);
            } else {
                System.setProperty(property, previousValue);
            }
        }
    }

    private static void assertInactive(final PropertiesConfigurationFactory factory) {
        assertThrows(IllegalStateException.class, factory::getSupportedTypes);
        assertThrows(
                IllegalStateException.class, () -> factory.getConfiguration(null, ConfigurationSource.NULL_SOURCE));
        assertThrows(
                IllegalStateException.class,
                () -> factory.getConfiguration(null, "test", URI.create("classpath:log4j.properties")));
        assertThrows(
                IllegalStateException.class,
                () -> factory.getConfiguration(
                        null,
                        "test",
                        URI.create("classpath:log4j.properties"),
                        PropertiesConfigurationFactoryActivationTest.class.getClassLoader()));
    }
}
