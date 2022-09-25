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

package org.apache.logging.log4j.core.config.plugins.processor;

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.URL;
import java.util.Enumeration;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class PluginProcessorTest {

    private static final PluginCache pluginCache = new PluginCache();

    private final Plugin p = FakePlugin.class.getAnnotation(Plugin.class);

    @BeforeClass
    public static void setUpClass() throws Exception {
        final Enumeration<URL> resources =
            PluginProcessor.class.getClassLoader().getResources(PluginProcessor.PLUGIN_CACHE_FILE);
        pluginCache.loadCacheFiles(resources);
    }

    @Test
    public void testTestCategoryFound() throws Exception {
        assertNotNull("No plugin annotation on FakePlugin.", p);
        final Map<String, PluginEntry> testCategory = pluginCache.getCategory(p.category());
        assertNotEquals("No plugins were found.", 0, pluginCache.size());
        assertNotNull("The category '" + p.category() + "' was not found.", testCategory);
        assertFalse(testCategory.isEmpty());
    }

    @Test
    public void testFakePluginFoundWithCorrectInformation() throws Exception {
        final PluginEntry fake = pluginCache.getCategory(p.category()).get(p.name().toLowerCase());
        verifyFakePluginEntry(p.name(), fake);
    }

    @Test
    public void testFakePluginAliasesContainSameInformation() throws Exception {
        final PluginAliases aliases = FakePlugin.class.getAnnotation(PluginAliases.class);
        for (final String alias : aliases.value()) {
            final PluginEntry fake = pluginCache.getCategory(p.category()).get(alias.toLowerCase());
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
        final PluginEntry nested = pluginCache.getCategory(p.category()).get(p.name().toLowerCase());
        assertNotNull(nested);
        assertEquals(p.name().toLowerCase(), nested.getKey());
        assertEquals(FakePlugin.Nested.class.getName(), nested.getClassName());
        assertEquals(p.name(), nested.getName());
        assertEquals(Plugin.EMPTY, p.elementType());
        assertEquals(p.printObject(), nested.isPrintable());
        assertEquals(p.deferChildren(), nested.isDefer());
    }
}
