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

import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.config.plugins.util.PluginBuilder;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.core.config.plugins.validation.HostAndPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ValidPortValidatorTest {
    private PluginType<HostAndPort> plugin;
    private Node node;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        final PluginManager manager = new PluginManager("Test");
        manager.collectPlugins();
        plugin = (PluginType<HostAndPort>) manager.getPluginType("HostAndPort");
        assertNotNull(plugin, "Rebuild this module to ensure annotation processing has been done.");
        node = new Node(null, "HostAndPort", plugin);
        node.getAttributes().put("host", "localhost");
    }

    @Test
    void testNegativePort() {
        node.getAttributes().put("port", "-1");
        assertNull(buildPlugin());
    }

    @Test
    void testValidPort() {
        node.getAttributes().put("port", "10");
        assertNotNull(buildPlugin());
    }

    @Test
    void testInvalidPort() {
        node.getAttributes().put("port", "1234567890");
        assertNull(buildPlugin());
    }

    private HostAndPort buildPlugin() {
        return (HostAndPort) new PluginBuilder(plugin)
                .withConfiguration(new NullConfiguration())
                .withConfigurationNode(node)
                .build();
    }
}
