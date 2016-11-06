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

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.logging.log4j.core.util.SortedProperties;
import org.apache.logging.log4j.util.Strings;

/**
 *
 */
public class PluginCache {

    public enum Format {

        DAT {
            @Override
            public void loadCacheFiles(final PluginCache pluginCache, final URL url) throws IOException {
                try (final DataInputStream in = new DataInputStream(new BufferedInputStream(url.openStream()))) {
                    final int count = in.readInt();
                    for (int i = 0; i < count; i++) {
                        final String category = in.readUTF();
                        final Map<String, PluginEntry> m = pluginCache.getCategory(category);
                        final int entries = in.readInt();
                        for (int j = 0; j < entries; j++) {
                            final PluginEntry entry = new PluginEntry();
                            entry.setKey(in.readUTF());
                            entry.setClassName(in.readUTF());
                            entry.setName(in.readUTF());
                            entry.setPrintable(in.readBoolean());
                            entry.setDefer(in.readBoolean());
                            entry.setCategory(category);
                            if (!m.containsKey(entry.getKey())) {
                                m.put(entry.getKey(), entry);
                            }
                        }
                    }
                }
            }

            @Override
            public void writeCache(final PluginCache pluginCache, final OutputStream os) throws IOException {
                try (final DataOutputStream out = new DataOutputStream(new BufferedOutputStream(os))) {
                    // See PluginManager.readFromCacheFiles for the corresponding decoder. Format may not be changed
                    // without breaking existing Log4j2Plugins.dat files.
                    out.writeInt(pluginCache.categories.size());
                    for (final Map.Entry<String, Map<String, PluginEntry>> category : pluginCache.categories
                            .entrySet()) {
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
        },

        PROPERTIES {
            @Override
            public void loadCacheFiles(final PluginCache pluginCache, final URL url) throws IOException {
                // TODO Auto-generated method stub

            }

            @Override
            public void writeCache(final PluginCache pluginCache, final OutputStream os) throws IOException {
                toProperties(pluginCache).store(os, COMMENT);
            }

        },

        PROPERTIES_XML {
            @Override
            public String getExtension() {
                return ".properties.xml";
            }

            @Override
            public void loadCacheFiles(final PluginCache pluginCache, final URL url) throws IOException {
                // TODO Auto-generated method stub

            }

            @Override
            public void writeCache(final PluginCache pluginCache, final OutputStream os) throws IOException {
                toProperties(pluginCache).storeToXML(os, COMMENT);
            }
        },

        XML {
            @Override
            public void loadCacheFiles(final PluginCache pluginCache, final URL url) throws IOException {
                try (final XMLDecoder out = new XMLDecoder(url.openStream())) {
                    @SuppressWarnings("unchecked")
                    final Map<String, Map<String, PluginEntry>> object = (Map<String, Map<String, PluginEntry>>) out.readObject();
                    pluginCache.getAllCategories().putAll(object);
                }
            }

            @Override
            public void writeCache(final PluginCache pluginCache, final OutputStream os) {
                try (final XMLEncoder out = new XMLEncoder(os)) {
                    out.writeObject(pluginCache.categories);
                }
            }
        };

        private static final String COMMENT = "Log4j2 plugin cache file";

        /**
         * Parses a comma-separated list of {@code Format}s.
         * 
         * @param formatsStr
         *            input
         * @param defaultFormats
         *            The default Formats if the input is null or empty.
         * @return a non-null array
         */
        public static Format[] parse(final String formatsStr, final Format... defaultFormats) {
            if (Strings.isBlank(formatsStr)) {
                return defaultFormats;
            }
            final String[] split = formatsStr.split("\\s*,\\s*");
            final Format[] formats = new Format[split.length];
            for (int i = 0; i < formats.length; i++) {
                formats[i] = Format.valueOf(split[i]);
            }
            return formats;
        }

        static Properties toProperties(final PluginCache pluginCache) {
            final SortedProperties prop = new SortedProperties();
            for (final Entry<String, Map<String, PluginEntry>> mainEntry : pluginCache.categories.entrySet()) {
                for (final Entry<String, PluginEntry> entry : mainEntry.getValue().entrySet()) {
                    final String key = mainEntry.getKey() + "." + entry.getKey();
                    final PluginEntry pluginEntry = entry.getValue();
                    prop.put(key + ".key", pluginEntry.getKey().toString());
                    prop.put(key + ".classname", pluginEntry.getClassName().toString());
                    prop.put(key + ".name", pluginEntry.getName().toString());
                    prop.put(key + ".defer", "" + pluginEntry.isDefer());
                    prop.put(key + ".printable", "" + pluginEntry.isPrintable());
                }
            }
            return prop;
        }

        public String getExtension() {
            return "." + toString().toLowerCase(Locale.ROOT);
        }

        public abstract void loadCacheFiles(PluginCache pluginCache, URL url) throws IOException;

        public abstract void writeCache(PluginCache pluginCache, OutputStream os) throws IOException;

    }

    private final Map<String, Map<String, PluginEntry>> categories = new LinkedHashMap<>();

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
     * @param category
     *            name of category to look up.
     * @return plugin mapping of names to plugin entries.
     */
    public Map<String, PluginEntry> getCategory(final String category) {
        final String key = category.toLowerCase();
        if (!categories.containsKey(key)) {
            categories.put(key, new LinkedHashMap<String, PluginEntry>());
        }
        return categories.get(key);
    }

    public void loadCacheFiles(final ClassLoader classLoader) throws IOException {
        categories.clear();
        for (final Format format : Format.values()) {
            final Enumeration<URL> resources = classLoader
                    .getResources(PluginProcessor.PLUGIN_CACHE_FILE_BASE + format.getExtension());
            while (resources.hasMoreElements()) {
                final URL url = resources.nextElement();
                format.loadCacheFiles(this, url);
            }
        }
    }

    /**
     * Loads and merges all the Log4j plugin cache files specified. Usually, this is obtained via a ClassLoader.
     *
     * @param resources
     *            URLs to all the desired plugin cache files to load.
     * @throws IOException
     *             if an I/O exception occurs.
     * @deprecated Use {@link #loadCacheFiles(ClassLoader)}.
     */
    @Deprecated
    public void loadCacheFiles(final Enumeration<URL> resources) throws IOException {
        categories.clear();
        while (resources.hasMoreElements()) {
            Format.DAT.loadCacheFiles(this, resources.nextElement());
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

    /**
     * Stores the plugin cache to a given OutputStream.
     *
     * @param os
     *            destination to save cache to.
     * @throws IOException
     *             if an I/O exception occurs.
     * @deprecated Use {@link #writeCache(OutputStream, String)} or
     *             {@link Format#writeCache(PluginCache, OutputStream)}.
     */
    @Deprecated
    // NOTE: if this file format is to be changed, the filename should change and this format should still be readable
    public void writeCache(final OutputStream os) throws IOException {
        Format.DAT.writeCache(this, os);
    }

    /**
     * Stores the plugin cache to a given OutputStream.
     *
     * @param os
     *            destination to save cache to.
     * @throws IOException
     *             if an I/O exception occurs.
     */
    // NOTE: if this file format is to be changed, the filename should change and this format should still be readable
    public void writeCache(final OutputStream os, final String formatsStr) throws IOException {
        if (Strings.isBlank(formatsStr)) {
            Format.DAT.writeCache(this, os);
            return;
        }
        for (final String formatStr : formatsStr.split("\\s*,\\s*")) {
            Format.valueOf(formatStr).writeCache(this, os);
        }
    }
}
