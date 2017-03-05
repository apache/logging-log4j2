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
package org.apache.logging.log4j.core.config.plugins.validation.validators;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.config.plugins.util.PluginBuilder;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.core.config.plugins.validation.HostAndPort;
import org.apache.logging.log4j.junit.StatusLoggerRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

public class ValidHostValidatorTest {

    @Rule
    public StatusLoggerRule rule = new StatusLoggerRule(Level.FATAL);

    private PluginType<HostAndPort> plugin;
    private Node node;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        final PluginManager manager = new PluginManager("Test");
        manager.collectPlugins();
        plugin = (PluginType<HostAndPort>) manager.getPluginType("HostAndPort");
        assertNotNull("Rebuild this module to ensure annotation processing has been done.", plugin);
        node = new Node(null, "HostAndPort", plugin);
    }

    @Test
    public void testNullHost() throws Exception {
        assertNull(buildPlugin());
    }

    @Test
    public void testInvalidIpAddress() throws Exception {
        node.getAttributes().put("host", "256.256.256.256");
        node.getAttributes().put("port", "1");
        final HostAndPort plugin = buildPlugin();
        assertNull("Expected null, but got: " + plugin, plugin);
    }

    @Test
    public void testLocalhost() throws Exception {
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