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
package org.apache.logging.log4j.core.config.plugins.processor;

import static org.apache.logging.log4j.util.Strings.toRootLowerCase;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 */
public class PluginCache {
    private final Map<String, Map<String, PluginEntry>> categories = new TreeMap<>();

    /**
     * Returns all categories of plugins in this cache.
     *
     * @return all categories of plugins in this cache.
     * @since 2.1
     */
    public Map<String, Map<String, PluginEntry>> getAllCategories() {
        return categories;
    }

    /**
     * Gets or creates a category of plugins.
     *
     * @param category name of category to look up.
     * @return plugin mapping of names to plugin entries.
     */
    public Map<String, PluginEntry> getCategory(final String category) {
        final String key = toRootLowerCase(category);
        return categories.computeIfAbsent(key, ignored -> new TreeMap<>());
    }

    /**
     * Stores the plugin cache to a given OutputStream.
     *
     * @param os destination to save cache to.
     * @throws IOException if an I/O exception occurs.
     */
    // NOTE: if this file format is to be changed, the filename should change and this format should still be readable
    public void writeCache(final OutputStream os) throws IOException {
        try (final DataOutputStream out = new DataOutputStream(new BufferedOutputStream(os))) {
            // See PluginManager.readFromCacheFiles for the corresponding decoder. Format may not be changed
            // without breaking existing Log4j2Plugins.dat files.
            out.writeInt(categories.size());
            for (final Map.Entry<String, Map<String, PluginEntry>> category : categories.entrySet()) {
                out.writeUTF(category.getKey());
                final Map<String, PluginEntry> m = category.getValue();
                out.writeInt(m.size());
                for (final Map.Entry<String, PluginEntry> entry : m.entrySet()) {
                    final PluginEntry plugin = entry.getValue();
                    out.writeUTF(plugin.getKey());
                    out.writeUTF(plugin.getClassName());
                    out.writeUTF(plugin.getName());
                    out.writeBoolean(plugin.isPrintable());
                    out.writeBoolean(plugin.isDefer());
                }
            }
        }
    }

    /**
     * Loads and merges all the Log4j plugin cache files specified. Usually, this is obtained via a ClassLoader.
     *
     * @param resources URLs to all the desired plugin cache files to load.
     * @throws IOException if an I/O exception occurs.
     */
    public void loadCacheFiles(final Enumeration<URL> resources) throws IOException {
        categories.clear();
        while (resources.hasMoreElements()) {
            final URL url = resources.nextElement();
            try (final DataInputStream in = new DataInputStream(new BufferedInputStream(url.openStream()))) {
                final int count = in.readInt();
                for (int i = 0; i < count; i++) {
                    final String category = in.readUTF();
                    final Map<String, PluginEntry> m = getCategory(category);
                    final int entries = in.readInt();
                    for (int j = 0; j < entries; j++) {
                        // Must always read all parts of the entry, even if not adding, so that the stream progresses
                        final String key = in.readUTF();
                        final String className = in.readUTF();
                        final String name = in.readUTF();
                        final boolean printable = in.readBoolean();
                        final boolean defer = in.readBoolean();
                        m.computeIfAbsent(key, k -> {
                            final PluginEntry entry = new PluginEntry();
                            entry.setKey(k);
                            entry.setClassName(className);
                            entry.setName(name);
                            entry.setPrintable(printable);
                            entry.setDefer(defer);
                            entry.setCategory(category);
                            return entry;
                        });
                    }
                }
            }
        }
    }

    /**
     * Gets the number of plugin categories registered.
     *
     * @return number of plugin categories in cache.
     */
    public int size() {
        return categories.size();
    }
}
