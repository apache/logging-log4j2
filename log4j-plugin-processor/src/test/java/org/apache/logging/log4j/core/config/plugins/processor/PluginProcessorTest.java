/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.logging.log4j.core.config.plugins.processor;

import java.io.BufferedInputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(JUnit4.class)
public class PluginProcessorTest {

    private static final String CACHE_FILE = "org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat";

    private static ConcurrentMap<String, ConcurrentMap<String, PluginEntry>> pluginCategories;

    private final Plugin p = FakePlugin.class.getAnnotation(Plugin.class);

    @BeforeClass
    public static void setUpClass() throws Exception {
        // TODO: refactor PluginManager.decode() into this module?
        pluginCategories = new ConcurrentHashMap<String, ConcurrentMap<String, PluginEntry>>();
        final Enumeration<URL> resources = PluginProcessor.class.getClassLoader().getResources(CACHE_FILE);
        while (resources.hasMoreElements()) {
            final URL url = resources.nextElement();
            final ObjectInput in = new ObjectInputStream(new BufferedInputStream(url.openStream()));
            try {
                final int count = in.readInt();
                for (int i = 0; i < count; i++) {
                    final String category = in.readUTF();
                    pluginCategories.putIfAbsent(category, new ConcurrentHashMap<String, PluginEntry>());
                    final ConcurrentMap<String, PluginEntry> m = pluginCategories.get(category);
                    final int entries = in.readInt();
                    for (int j = 0; j < entries; j++) {
                        final PluginEntry entry = new PluginEntry();
                        entry.setKey(in.readUTF());
                        entry.setClassName(in.readUTF());
                        entry.setName(in.readUTF());
                        entry.setPrintable(in.readBoolean());
                        entry.setDefer(in.readBoolean());
                        entry.setCategory(category);
                        m.putIfAbsent(entry.getKey(), entry);
                    }
                    pluginCategories.putIfAbsent(category, m);
                }
            } finally {
                in.close();
            }
        }
    }

    @Test
    public void testTestCategoryFound() throws Exception {
        assertNotNull("No plugin annotation on FakePlugin.", p);
        final ConcurrentMap<String, PluginEntry> testCategory = pluginCategories.get(p.category());
        assertNotEquals("No plugins were found.", 0, pluginCategories.size());
        assertNotNull("The category '" + p.category() + "' was not found.", testCategory);
    }

    @Test
    public void testFakePluginFoundWithCorrectInformation() throws Exception {
        final PluginEntry fake = pluginCategories.get(p.category()).get(p.name().toLowerCase());
        verifyFakePluginEntry(p.name(), fake);
    }

    @Test
    public void testFakePluginAliasesContainSameInformation() throws Exception {
        final PluginAliases aliases = FakePlugin.class.getAnnotation(PluginAliases.class);
        for (final String alias : aliases.value()) {
            final PluginEntry fake = pluginCategories.get(p.category()).get(alias.toLowerCase());
            verifyFakePluginEntry(alias, fake);
        }
    }

    private void verifyFakePluginEntry(final String name, final PluginEntry fake) {
        assertNotNull("The plugin '" + name.toLowerCase() + "' was not found.", fake);
        assertEquals(FakePlugin.class.getName(), fake.getClassName());
        assertEquals(name.toLowerCase(), fake.getKey());
        assertEquals("", p.elementType());
        assertEquals(name, fake.getName());
        assertEquals(p.printObject(), fake.isPrintable());
        assertEquals(p.deferChildren(), fake.isDefer());
    }
}
