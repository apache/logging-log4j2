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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.ConfigurationProcessor;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.model.PluginNamespace;
import org.apache.logging.log4j.plugins.model.PluginType;
import org.apache.logging.log4j.plugins.test.validation.ValidatingPlugin;
import org.apache.logging.log4j.test.junit.StatusLoggerLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@StatusLoggerLevel("OFF")
public class RequiredValidatorTest {

    private final ConfigurableInstanceFactory instanceFactory = DI.createInitializedFactory();
    private final ConfigurationProcessor processor = new ConfigurationProcessor(instanceFactory);
    private Node node;

    @BeforeEach
    public void setUp() throws Exception {
        final PluginNamespace category = instanceFactory.getInstance(Core.PLUGIN_NAMESPACE_KEY);
        final PluginType<?> pluginType = category.get("Validator");
        assertNotNull(pluginType, "Rebuild this module to make sure annotation processing kicks in.");
        node = new Node(null, "Validator", pluginType);
    }

    @Test
    public void testNullDefaultValue() throws Exception {
        final ValidatingPlugin validatingPlugin = processor.processNodeTree(node);
        assertNull(validatingPlugin);
    }

    @Test
    public void testNonNullValue() throws Exception {
        node.getAttributes().put("name", "foo");
        final ValidatingPlugin validatingPlugin = processor.processNodeTree(node);
        assertEquals("foo", validatingPlugin.getName());
    }
}
