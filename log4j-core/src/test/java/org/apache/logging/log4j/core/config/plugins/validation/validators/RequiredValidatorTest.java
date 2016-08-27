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

import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.config.plugins.util.PluginBuilder;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.core.config.plugins.validation.ValidatingPlugin;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RequiredValidatorTest {

    private PluginType<ValidatingPlugin> plugin;
    private Node node;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        final PluginManager manager = new PluginManager("Test");
        manager.collectPlugins();
        plugin = (PluginType<ValidatingPlugin>) manager.getPluginType("Validator");
        assertNotNull("Rebuild this module to make sure annotaion processing kicks in.", plugin);
        node = new Node(null, "Validator", plugin);
    }

    @Test
    public void testNullDefaultValue() throws Exception {
        final ValidatingPlugin validatingPlugin = (ValidatingPlugin) new PluginBuilder(plugin)
            .withConfiguration(new NullConfiguration())
            .withConfigurationNode(node)
            .build();
        assertNull(validatingPlugin);
    }

    @Test
    public void testNonNullValue() throws Exception {
        node.getAttributes().put("name", "foo");
        final ValidatingPlugin validatingPlugin = (ValidatingPlugin) new PluginBuilder(plugin)
            .withConfiguration(new NullConfiguration())
            .withConfigurationNode(node)
            .build();
        assertNotNull(validatingPlugin);
        assertEquals("foo", validatingPlugin.getName());
    }
}
