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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.net.URL;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.json.JsonConfigurationFactory;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.junit.jupiter.api.Test;

class YamlConfigurationFactoryMissingDependenciesIT {

    @Test
    void inactiveFactoryRejectsDirectUseWithoutDisablingJson() {
        final ClassLoader loader = getClass().getClassLoader();
        assertDoesNotThrow(() -> Class.forName("com.fasterxml.jackson.core.JsonParser", false, loader));
        assertDoesNotThrow(() -> Class.forName("com.fasterxml.jackson.databind.ObjectMapper", false, loader));
        assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName("com.fasterxml.jackson.dataformat.yaml.YAMLFactory", false, loader));

        assertArrayEquals(new String[] {".json", ".jsn"}, new JsonConfigurationFactory().getSupportedTypes());

        final YamlConfigurationFactory factory = new YamlConfigurationFactory();
        assertFalse(factory.isActive());
        assertThrows(IllegalStateException.class, factory::getSupportedTypes);
        assertThrows(
                IllegalStateException.class, () -> factory.getConfiguration(null, ConfigurationSource.NULL_SOURCE));
        assertThrows(
                IllegalStateException.class,
                () -> factory.getConfiguration(null, "test", URI.create("classpath:log4j-test1.yaml")));
        assertThrows(
                IllegalStateException.class,
                () -> factory.getConfiguration(
                        null,
                        "test",
                        URI.create("classpath:log4j-test1.yaml"),
                        getClass().getClassLoader()));
    }

    @Test
    void aggregateFactorySkipsInactiveFactories() throws Exception {
        final URL resource = getClass().getResource("/log4j-test1.xml");
        assertNotNull(resource);
        try (final LoggerContext context = new LoggerContext("test")) {
            final Configuration configuration =
                    ConfigurationFactory.getInstance().getConfiguration(context, "test", resource.toURI());
            assertInstanceOf(XmlConfiguration.class, configuration);
            assertEquals(
                    resource.toURI(), configuration.getConfigurationSource().getURI());
        }
    }
}
