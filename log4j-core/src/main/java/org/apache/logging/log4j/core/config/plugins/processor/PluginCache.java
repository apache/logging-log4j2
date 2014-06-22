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
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.core.config.plugins.util.PluginRegistry;
import org.apache.logging.log4j.core.util.Closer;

/**
 *
 */
public class PluginCache {
    private final transient PluginRegistry<PluginEntry> pluginCategories = new PluginRegistry<PluginEntry>();

    /**
     * Gets or creates a category of plugins.
     *
     * @param category name of category to look up.
     * @return plugin mapping of names to plugin entries.
     */
    public ConcurrentMap<String, PluginEntry> getCategory(final String category) {
        return pluginCategories.getCategory(category);
    }

    /**
     * Stores the plugin cache to a given OutputStream.
     *
     * @param os destination to save cache to.
     * @throws IOException
     */
    public void writeCache(final OutputStream os) throws IOException {
        final DataOutputStream out = new DataOutputStream(new BufferedOutputStream(os));
        try {
            out.writeInt(pluginCategories.getCategoryCount());
            for (final Map.Entry<String, ConcurrentMap<String, PluginEntry>> category : pluginCategories.getCategories()) {
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
        } finally {
            out.close();
        }
    }

    /**
     * Loads and merges all the Log4j plugin cache files specified. Usually, this is obtained via a ClassLoader.
     *
     * @param resources URLs to all the desired plugin cache files to load.
     * @throws IOException
     */
    public void loadCacheFiles(final Enumeration<URL> resources) throws IOException {
        pluginCategories.clear();
        while (resources.hasMoreElements()) {
            final URL url = resources.nextElement();
            loadCacheFile(url.openStream());
        }
    }

    public void loadCacheFile(final InputStream is) throws IOException {
        final DataInputStream in = new DataInputStream(new BufferedInputStream(is));
        try {
            final int count = in.readInt();
            for (int i = 0; i < count; i++) {
                final String category = in.readUTF();
                final ConcurrentMap<String, PluginEntry> m = pluginCategories.getCategory(category);
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
            }
        } finally {
            Closer.closeSilent(in);
            Closer.closeSilent(is);
        }
    }

    /**
     * Gets the number of plugin categories registered.
     *
     * @return number of plugin categories in cache.
     */
    public int size() {
        return pluginCategories.getCategoryCount();
    }
}
