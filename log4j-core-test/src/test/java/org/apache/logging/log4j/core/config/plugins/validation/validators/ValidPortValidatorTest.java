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
package org.apache.logging.log4j.core.config.plugins.validation.validators;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.logging.log4j.core.config.ConfigurationProcessor;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.model.PluginNamespace;
import org.apache.logging.log4j.plugins.model.PluginType;
import org.apache.logging.log4j.test.junit.StatusLoggerLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@StatusLoggerLevel("OFF")
public class ValidPortValidatorTest {
    private final ConfigurableInstanceFactory instanceFactory = DI.createInitializedFactory();
    private final ConfigurationProcessor processor = new ConfigurationProcessor(instanceFactory);
    private Node node;

    @BeforeEach
    public void setUp() throws Exception {
        final var key = Key.builder(PluginNamespace.class).setNamespace("Test").get();
        final PluginNamespace category = instanceFactory.getInstance(key);
        final PluginType<?> plugin = category.get("HostAndPort");
        assertNotNull(plugin, "Rebuild this module to ensure annotation processing has been done.");
        node = new Node(null, "HostAndPort", plugin);
        node.getAttributes().put("host", "localhost");
    }

    @Test
    public void testNegativePort() throws Exception {
        node.getAttributes().put("port", "-1");
        assertNull(processor.processNodeTree(node));
    }

    @Test
    public void testValidPort() throws Exception {
        node.getAttributes().put("port", "10");
        assertNotNull(processor.processNodeTree(node));
    }

    @Test
    public void testInvalidPort() throws Exception {
        node.getAttributes().put("port", "1234567890");
        assertNull(processor.processNodeTree(node));
    }
}
