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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.config.plugins.util.PluginBuilder;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.core.config.plugins.validation.HostAndPort;
import org.apache.logging.log4j.test.junit.StatusLoggerLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@StatusLoggerLevel("FATAL")
class ValidHostValidatorTest {

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
    }

    @Test
    void testNullHost() {
        assertNull(buildPlugin());
    }

    @Test
    void testInvalidIpAddress() {
        node.getAttributes().put("host", "256.256.256.256");
        node.getAttributes().put("port", "1");
        final HostAndPort plugin = buildPlugin();
        assertNull(plugin, "Expected null, but got: " + plugin);
    }

    @Test
    void testLocalhost() {
        node.getAttributes().put("host", "localhost");
        node.getAttributes().put("port", "1");
        final HostAndPort hostAndPort = buildPlugin();
        assertNotNull(hostAndPort);
        assertTrue(hostAndPort.isValid());
    }

    private HostAndPort buildPlugin() {
        return (HostAndPort) new PluginBuilder(plugin)
                .withConfiguration(new NullConfiguration())
                .withConfigurationNode(node)
                .build();
    }
}
