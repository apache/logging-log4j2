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

import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAliases;
import org.apache.logging.log4j.plugins.processor.PluginService;
import org.apache.logging.log4j.plugins.test.validation.FakePlugin;
import org.apache.logging.log4j.plugins.test.validation.plugins.Log4jPlugins;
import org.apache.logging.log4j.plugins.util.PluginType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class PluginProcessorTest {

    private static PluginService pluginService;

    private final Plugin p = FakePlugin.class.getAnnotation(Plugin.class);

    @BeforeClass
    public static void setUpClass() {
        pluginService = new Log4jPlugins();
    }

    @Test
    public void testTestCategoryFound() throws Exception {
        assertNotNull("No plugin annotation on FakePlugin.", p);
        final var testCategory = pluginService.getCategory(p.category());
        assertNotEquals("No plugins were found.", 0, pluginService.size());
        assertNotNull("The category '" + p.category() + "' was not found.", testCategory);
        assertFalse(testCategory.isEmpty());
    }

    @Test
    public void testFakePluginFoundWithCorrectInformation() throws Exception {
        final var testCategory = pluginService.getCategory(p.category());
        assertNotNull(testCategory);
        final PluginType<?> type = testCategory.get(p.name());
        assertNotNull(type);
        verifyFakePluginEntry(p.name(), type);
    }

    @Test
    public void testFakePluginAliasesContainSameInformation() throws Exception {
        final PluginAliases aliases = FakePlugin.class.getAnnotation(PluginAliases.class);
        for (final String alias : aliases.value()) {
            final var testCategory = pluginService.getCategory(p.category());
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
        assertEquals(Plugin.EMPTY, p.elementType());
        assertEquals(name, fake.getName());
        assertEquals(p.printObject(), fake.isObjectPrintable());
        assertEquals(p.deferChildren(), fake.isDeferChildren());
    }

    @Test
    public void testNestedPlugin() throws Exception {
        final Plugin p = FakePlugin.Nested.class.getAnnotation(Plugin.class);
        final var testCategory = pluginService.getCategory(p.category());
        assertNotNull(testCategory);
        final PluginType<?> nested = testCategory.get(p.name());
        assertNotNull(nested);
        assertEquals(p.name().toLowerCase(), nested.getKey());
        assertEquals(FakePlugin.Nested.class.getName(), nested.getPluginEntry().getClassName());
        assertEquals(p.name(), nested.getName());
        assertEquals(Plugin.EMPTY, p.elementType());
        assertEquals(p.printObject(), nested.isObjectPrintable());
        assertEquals(p.deferChildren(), nested.isDeferChildren());
    }
}
