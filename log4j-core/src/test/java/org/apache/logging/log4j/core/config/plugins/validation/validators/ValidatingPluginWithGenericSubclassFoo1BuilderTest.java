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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.config.plugins.util.PluginBuilder;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.core.config.plugins.validation.PluginWithGenericSubclassFoo1Builder;
import org.junit.Before;
import org.junit.Test;

public class ValidatingPluginWithGenericSubclassFoo1BuilderTest {

    private PluginType<PluginWithGenericSubclassFoo1Builder> plugin;
    private Node node;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        final PluginManager manager = new PluginManager("Test");
        manager.collectPlugins();
        plugin = (PluginType<PluginWithGenericSubclassFoo1Builder>) manager.getPluginType("PluginWithGenericSubclassFoo1Builder");
        assertNotNull("Rebuild this module to make sure annotaion processing kicks in.", plugin);
        node = new Node(null, "Validator", plugin);
    }

    @Test
    public void testNullDefaultValue() throws Exception {
        final PluginWithGenericSubclassFoo1Builder validatingPlugin = (PluginWithGenericSubclassFoo1Builder) new PluginBuilder(plugin)
            .withConfiguration(new NullConfiguration())
            .withConfigurationNode(node)
            .build();
        assertNull(validatingPlugin);
    }

    @Test
    public void testNonNullValue() throws Exception {
        node.getAttributes().put("thing", "thing1");
        node.getAttributes().put("foo1", "foo1");
        final PluginWithGenericSubclassFoo1Builder validatingPlugin = (PluginWithGenericSubclassFoo1Builder) new PluginBuilder(plugin)
            .withConfiguration(new NullConfiguration())
            .withConfigurationNode(node)
            .build();
        assertNotNull(validatingPlugin);
        assertEquals("thing1", validatingPlugin.getThing());
        assertEquals("foo1", validatingPlugin.getFoo1());
    }
}
