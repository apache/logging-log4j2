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

import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.di.Keys;
import org.apache.logging.log4j.plugins.test.validation.ValidatingPluginWithTypedBuilder;
import org.apache.logging.log4j.plugins.util.PluginManager;
import org.apache.logging.log4j.plugins.util.PluginType;
import org.apache.logging.log4j.test.junit.StatusLoggerLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@StatusLoggerLevel("OFF")
public class ValidatingPluginWithTypedBuilderTest {

    private Node node;

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void setUp() throws Exception {
        final PluginManager manager = new PluginManager("Test");
        manager.collectPlugins();
        PluginType<ValidatingPluginWithTypedBuilder> plugin = (PluginType<ValidatingPluginWithTypedBuilder>) manager
                .getPluginType("ValidatingPluginWithTypedBuilder");
        assertNotNull(plugin, "Rebuild this module to make sure annotation processing kicks in.");
        node = new Node(null, "Validator", plugin);
    }

    @Test
    public void testNullDefaultValue() throws Exception {
        final ValidatingPluginWithTypedBuilder validatingPlugin = DI.createInjector()
                .registerBinding(Keys.SUBSTITUTOR_KEY, Function::identity)
                .configure(node);
        assertNull(validatingPlugin);
    }

    @Test
    public void testNonNullValue() throws Exception {
        node.getAttributes().put("name", "foo");
        final ValidatingPluginWithTypedBuilder validatingPlugin = DI.createInjector()
                .registerBinding(Keys.SUBSTITUTOR_KEY, Function::identity)
                .configure(node);
        assertNotNull(validatingPlugin);
        assertEquals("foo", validatingPlugin.getName());
    }
}
