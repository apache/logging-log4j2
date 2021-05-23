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

package org.apache.logging.log4j.plugins.processor;

import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAliases;
import org.apache.logging.log4j.plugins.util.PluginType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class PluginProcessorTest {

    private static PluginService pluginService;

    private final Plugin p = FakePlugin.class.getAnnotation(Plugin.class);

    @BeforeClass
    public static void setUpClass() throws Exception {
        Class<?> clazz = PluginProcessor.class.getClassLoader().loadClass("org.apache.logging.log4j.plugins.plugins.Log4jPlugins");
        assertNotNull("Could not locate plugins class", clazz);
        pluginService = (PluginService) clazz.getDeclaredConstructor().newInstance();;
    }

    @Test
    public void testTestCategoryFound() throws Exception {
        assertNotNull("No plugin annotation on FakePlugin.", p);
        final List<PluginType<?>> testCategory = pluginService.getCategory(p.category());
        assertNotEquals("No plugins were found.", 0, pluginService.size());
        assertNotNull("The category '" + p.category() + "' was not found.", testCategory);
        assertFalse(testCategory.isEmpty());
    }

    @Test
    public void testFakePluginFoundWithCorrectInformation() throws Exception {
        final List<PluginType<?>> list = pluginService.getCategory(p.category());
        assertNotNull(list);
        final PluginEntry fake = getEntry(list, p.name());
        assertNotNull(fake);
        verifyFakePluginEntry(p.name(), fake);
    }

    @Test
    public void testFakePluginAliasesContainSameInformation() throws Exception {
        final PluginAliases aliases = FakePlugin.class.getAnnotation(PluginAliases.class);
        for (final String alias : aliases.value()) {
            final List<PluginType<?>> list = pluginService.getCategory(p.category());
            assertNotNull(list);
            final PluginEntry fake = getEntry(list, alias);
            assertNotNull(fake);
            verifyFakePluginEntry(alias, fake);
        }
    }

    private void verifyFakePluginEntry(final String name, final PluginEntry fake) {
        assertNotNull("The plugin '" + name.toLowerCase() + "' was not found.", fake);
        assertEquals(FakePlugin.class.getName(), fake.getClassName());
        assertEquals(name.toLowerCase(), fake.getKey());
        assertEquals(Plugin.EMPTY, p.elementType());
        assertEquals(name, fake.getName());
        assertEquals(p.printObject(), fake.isPrintable());
        assertEquals(p.deferChildren(), fake.isDefer());
    }

    @Test
    public void testNestedPlugin() throws Exception {
        final Plugin p = FakePlugin.Nested.class.getAnnotation(Plugin.class);
        final List<PluginType<?>> list = pluginService.getCategory(p.category());
        assertNotNull(list);
        final PluginEntry nested = getEntry(list, p.name());
        assertNotNull(nested);
        assertEquals(p.name().toLowerCase(), nested.getKey());
        assertEquals(FakePlugin.Nested.class.getName(), nested.getClassName());
        assertEquals(p.name(), nested.getName());
        assertEquals(Plugin.EMPTY, p.elementType());
        assertEquals(p.printObject(), nested.isPrintable());
        assertEquals(p.deferChildren(), nested.isDefer());
    }

    private PluginEntry getEntry(List<PluginType<?>> list, String name) {
        for (PluginType<?> type : list) {
            if (type.getPluginEntry().getName().equalsIgnoreCase(name)) {
                return type.getPluginEntry();
            }
        }
        return null;
    }
}
