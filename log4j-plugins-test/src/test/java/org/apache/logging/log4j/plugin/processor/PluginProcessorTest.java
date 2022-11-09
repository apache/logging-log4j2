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
package org.apache.logging.log4j.plugin.processor;

import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAliases;
import org.apache.logging.log4j.plugins.di.Keys;
import org.apache.logging.log4j.plugins.model.PluginService;
import org.apache.logging.log4j.plugins.model.PluginType;
import org.apache.logging.log4j.plugins.test.validation.FakePlugin;
import org.apache.logging.log4j.plugins.test.validation.plugins.Log4jPlugins;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class PluginProcessorTest {

    private static PluginService pluginService;

    private final Plugin p = FakePlugin.class.getAnnotation(Plugin.class);
    private final Configurable c = FakePlugin.class.getAnnotation(Configurable.class);
    private final String ns = Keys.getNamespace(FakePlugin.class);

    @BeforeClass
    public static void setUpClass() {
        pluginService = new Log4jPlugins();
    }

    @Test
    public void testTestCategoryFound() throws Exception {
        assertNotNull("No plugin annotation on FakePlugin.", p);
        final var namespace = pluginService.getNamespace(ns);
        assertNotEquals("No plugins were found.", 0, pluginService.size());
        assertNotNull("The namespace '" + ns + "' was not found.", namespace);
        assertFalse(namespace.isEmpty());
    }

    @Test
    public void testFakePluginFoundWithCorrectInformation() throws Exception {
        final var testCategory = pluginService.getNamespace(ns);
        assertNotNull(testCategory);
        final PluginType<?> type = testCategory.get(p.value());
        assertNotNull(type);
        verifyFakePluginEntry(p.value(), type);
    }

    @Test
    public void testFakePluginAliasesContainSameInformation() throws Exception {
        final PluginAliases aliases = FakePlugin.class.getAnnotation(PluginAliases.class);
        for (final String alias : aliases.value()) {
            final var testCategory = pluginService.getNamespace(ns);
            assertNotNull(testCategory);
            final PluginType<?> type = testCategory.get(alias);
            assertNotNull(type);
            verifyFakePluginEntry(alias, type);
        }
    }

    private void verifyFakePluginEntry(final String name, final PluginType<?> fake) {
        assertNotNull("The plugin '" + name.toLowerCase() + "' was not found.", fake);
        assertEquals(FakePlugin.class.getName(), fake.getPluginEntry().getClassName());
        assertEquals(name.toLowerCase(), fake.getKey());
        assertEquals(Plugin.EMPTY, c.elementType());
        assertEquals(p.value(), fake.getName());
        assertEquals(c.printObject(), fake.isObjectPrintable());
        assertEquals(c.deferChildren(), fake.isDeferChildren());
    }

    @Test
    public void testNestedPlugin() throws Exception {
        final Plugin p = FakePlugin.Nested.class.getAnnotation(Plugin.class);
        final var testCategory = pluginService.getNamespace(Keys.getNamespace(FakePlugin.Nested.class));
        assertNotNull(testCategory);
        final PluginType<?> nested = testCategory.get(p.value());
        assertNotNull(nested);
        assertEquals(p.value().toLowerCase(), nested.getKey());
        assertEquals(FakePlugin.Nested.class.getName(), nested.getPluginEntry().getClassName());
        assertEquals(p.value(), nested.getName());
    }
}
