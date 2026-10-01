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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.junit.jupiter.api.Test;

class JsonConfigurationFactoryMissingDependenciesIT {

    @Test
    void inactiveFactoryExposesTypesAndDeclinesConfiguration() {
        final ClassLoader loader = getClass().getClassLoader();
        assertDoesNotThrow(() -> Class.forName("com.fasterxml.jackson.core.JsonParser", false, loader));
        assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName("com.fasterxml.jackson.databind.ObjectMapper", false, loader));

        final JsonConfigurationFactory factory = new JsonConfigurationFactory();
        assertFalse(factory.isActive());
        assertArrayEquals(new String[] {".json", ".jsn"}, factory.getSupportedTypes());
        assertNull(factory.getConfiguration(null, ConfigurationSource.NULL_SOURCE));
        assertNull(factory.getConfiguration(null, "test", URI.create("classpath:log4j-test1.json")));
        assertNull(factory.getConfiguration(
                null,
                "test",
                URI.create("classpath:log4j-test1.json"),
                getClass().getClassLoader()));
    }
}
