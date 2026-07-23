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
package org.apache.logging.log4j.core.config.json;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.net.URL;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.junit.jupiter.api.Test;

class JsonConfigurationFactoryMissingDependenciesIT {

    @Test
    void inactiveFactoryRejectsDirectUse() {
        final JsonConfigurationFactory factory = new JsonConfigurationFactory();
        assertFalse(factory.isActive());
        assertThrows(IllegalStateException.class, factory::getSupportedTypes);
        assertThrows(
                IllegalStateException.class, () -> factory.getConfiguration(null, ConfigurationSource.NULL_SOURCE));
        assertThrows(
                IllegalStateException.class,
                () -> factory.getConfiguration(null, "test", URI.create("classpath:log4j-test1.json")));
        assertThrows(
                IllegalStateException.class,
                () -> factory.getConfiguration(
                        null,
                        "test",
                        URI.create("classpath:log4j-test1.json"),
                        getClass().getClassLoader()));
    }

    @Test
    void aggregateFactorySkipsInactiveFactories() throws Exception {
        final URL resource = getClass().getResource("/log4j-test1.xml");
        assertNotNull(resource);
        try (final LoggerContext context = new LoggerContext("test")) {
            assertNotNull(ConfigurationFactory.getInstance().getConfiguration(context, "test", resource.toURI()));
        }
    }
}
