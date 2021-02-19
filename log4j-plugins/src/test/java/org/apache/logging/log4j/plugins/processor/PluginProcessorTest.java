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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAliases;
import org.apache.logging.log4j.plugins.util.PluginType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PluginProcessorTest {

    private static PluginService pluginService;

    private final Plugin p = FakePlugin.class.getAnnotation(Plugin.class);

    @BeforeClass
    public static void setUpClass() throws Exception {
        Class<?> clazz = PluginProcessor.class.getClassLoader().loadClass("org.apache.logging.log4j.plugins.plugins.Log4jPlugins");
        assertThat(clazz).describedAs("Could not locate plugins class").isNotNull();
        pluginService = (PluginService) clazz.getDeclaredConstructor().newInstance();;
    }

    @Test
    public void testTestCategoryFound() throws Exception {
        assertThat(p).describedAs("No plugin annotation on FakePlugin.").isNotNull();
        final List<PluginType<?>> testCategory = pluginService.getCategory(p.category());
        assertThat(pluginService.size()).describedAs("No plugins were found.").isNotEqualTo(0);
        assertThat(testCategory).describedAs("The category '" + p.category() + "' was not found.").isNotNull();
        assertThat(testCategory.isEmpty()).isFalse();
    }

    @Test
    public void testFakePluginFoundWithCorrectInformation() throws Exception {
        final List<PluginType<?>> list = pluginService.getCategory(p.category());
        assertThat(list).isNotNull();
        final PluginEntry fake = getEntry(list, p.name());
        assertThat(fake).isNotNull();
        verifyFakePluginEntry(p.name(), fake);
    }

    @Test
    public void testFakePluginAliasesContainSameInformation() throws Exception {
        final PluginAliases aliases = FakePlugin.class.getAnnotation(PluginAliases.class);
        for (final String alias : aliases.value()) {
            final List<PluginType<?>> list = pluginService.getCategory(p.category());
            assertThat(list).isNotNull();
            final PluginEntry fake = getEntry(list, alias);
            assertThat(fake).isNotNull();
            verifyFakePluginEntry(alias, fake);
        }
    }

    private void verifyFakePluginEntry(final String name, final PluginEntry fake) {
        assertThat(fake).describedAs("The plugin '" + name.toLowerCase() + "' was not found.").isNotNull();
        assertThat(fake.getClassName()).isEqualTo(FakePlugin.class.getName());
        assertThat(fake.getKey()).isEqualTo(name.toLowerCase());
        assertThat(p.elementType()).isEqualTo(Plugin.EMPTY);
        assertThat(fake.getName()).isEqualTo(name);
        assertThat(fake.isPrintable()).isEqualTo(p.printObject());
        assertThat(fake.isDefer()).isEqualTo(p.deferChildren());
    }

    @Test
    public void testNestedPlugin() throws Exception {
        final Plugin p = FakePlugin.Nested.class.getAnnotation(Plugin.class);
        final List<PluginType<?>> list = pluginService.getCategory(p.category());
        assertThat(list).isNotNull();
        final PluginEntry nested = getEntry(list, p.name());
        assertThat(nested).isNotNull();
        assertThat(nested.getKey()).isEqualTo(p.name().toLowerCase());
        assertThat(nested.getClassName()).isEqualTo(FakePlugin.Nested.class.getName());
        assertThat(nested.getName()).isEqualTo(p.name());
        assertThat(p.elementType()).isEqualTo(Plugin.EMPTY);
        assertThat(nested.isPrintable()).isEqualTo(p.printObject());
        assertThat(nested.isDefer()).isEqualTo(p.deferChildren());
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
