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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 */
public class PluginCache {
    private final Map<String, Map<String, PluginEntry>> categories =
        new TreeMap<>();

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
        final String key = category.toLowerCase();
        return categories.computeIfAbsent(key, ignored -> new TreeMap<>());
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
                        m.computeIfAbsent(key, k -> new PluginEntry(k, className, name, printable, defer, category));
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
