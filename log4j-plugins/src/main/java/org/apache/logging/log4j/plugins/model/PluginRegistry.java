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
package org.apache.logging.log4j.plugins.model;

import static org.apache.logging.log4j.util.Unbox.box;

import aQute.bnd.annotation.Cardinality;
import aQute.bnd.annotation.spi.ServiceConsumer;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Singleton;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.ServiceLoaderUtil;

/**
 * Registry singleton for PluginType maps partitioned by source type and then by category names.
 */
@Singleton
@ServiceConsumer(value = PluginService.class, cardinality = Cardinality.MULTIPLE)
public class PluginRegistry {

    /**
     * The location of the plugin cache data file for compatibility with Log4j 2.x plugins.
     */
    private static final String PLUGIN_CACHE_FILE =
            "META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat";

    private static final Logger LOGGER = StatusLogger.getLogger();

    /**
     * Contains plugins found from {@link PluginService} services and legacy Log4j2Plugins.dat cache files in the main CLASSPATH.
     */
    private final Lazy<Namespaces> namespacesLazy;

    @Inject
    public PluginRegistry(final ClassLoader classLoader) {
        namespacesLazy = Lazy.lazy(() -> {
            final Namespaces namespaces = decodeCacheFiles(classLoader);
            try {
                loadPlugins(classLoader, namespaces);
            } catch (final Throwable e) {
                LOGGER.debug("Unable to retrieve provider from ClassLoader {}", classLoader, e);
            }
            return namespaces;
        });
    }

    /**
     * Resets the registry to an empty state.
     */
    public void clear() {
        namespacesLazy.set(null);
    }

    /**
     * Load plugins from a specific ClassLoader.
     * @param classLoader The ClassLoader.
     * @param namespaces The Namespaces to merge discovered plugins to
     * @since 3.0
     */
    private void loadPlugins(final ClassLoader classLoader, final Namespaces namespaces) {
        final long startTime = System.nanoTime();
        final AtomicInteger pluginCount = new AtomicInteger();
        ServiceLoaderUtil.safeStream(
                        PluginService.class,
                        ServiceLoader.load(PluginService.class, classLoader),
                        StatusLogger.getLogger())
                .forEach(pluginService -> {
                    pluginService
                            .getNamespaces()
                            .values()
                            .forEach(category -> pluginCount.addAndGet(namespaces.merge(category)));
                });
        reportLoadTime(classLoader, startTime, pluginCount);
    }

    private Namespaces decodeCacheFiles(final ClassLoader classLoader) {
        final long startTime = System.nanoTime();
        final PluginIndex index = new PluginIndex();
        try {
            final Enumeration<URL> resources = classLoader.getResources(PLUGIN_CACHE_FILE);
            if (resources == null) {
                LOGGER.info("Plugin preloads not available from class loader {}", classLoader);
            } else {
                while (resources.hasMoreElements()) {
                    final URL url = resources.nextElement();
                    try (final DataInputStream in = new DataInputStream(new BufferedInputStream(url.openStream()))) {
                        final int count = in.readInt();
                        for (int i = 0; i < count; i++) {
                            final var builder = PluginEntry.builder().setNamespace(in.readUTF());
                            final int entries = in.readInt();
                            for (int j = 0; j < entries; j++) {
                                // Must always read all parts of the entry, even if not adding, so that the stream
                                // progresses
                                final var entry = builder.setKey(in.readUTF())
                                        .setClassName(in.readUTF())
                                        .setName(in.readUTF())
                                        .setPrintable(in.readBoolean())
                                        .setDeferChildren(in.readBoolean())
                                        .get();
                                index.add(entry);
                            }
                        }
                    }
                }
            }
        } catch (final IOException ioe) {
            LOGGER.warn("Unable to preload plugins", ioe);
        }
        final Namespaces namespaces = new Namespaces();
        final AtomicInteger pluginCount = new AtomicInteger();
        index.forEach(entry -> {
            final PluginType<?> type = new PluginType<>(entry, classLoader);
            namespaces.add(type);
            pluginCount.incrementAndGet();
        });
        reportLoadTime(classLoader, startTime, pluginCount);
        return namespaces;
    }

    private void reportLoadTime(final ClassLoader classLoader, final long startTime, final AtomicInteger pluginCount) {
        final int numPlugins = pluginCount.get();
        LOGGER.info(() -> {
            final long endTime = System.nanoTime();
            final DecimalFormat numFormat = new DecimalFormat("#0.000000");
            return "Took " + numFormat.format((endTime - startTime) * 1e-9) + " seconds to load " + numPlugins
                    + " plugins from " + classLoader;
        });
    }

    /**
     * Gets the registered plugins for the given namespace. If additional scan packages are provided, then plugins
     * are scanned and loaded from there as well.
     */
    public PluginNamespace getNamespace(final String namespace) {
        final var pluginNamespace = new PluginNamespace(namespace);

        // First, iterate the PluginService services
        final Namespaces builtInPlugins = namespacesLazy.get();
        if (builtInPlugins != null) {
            pluginNamespace.mergeAll(builtInPlugins.get(namespace));
        }

        LOGGER.debug("Discovered {} plugins in namespace '{}'", box(pluginNamespace.size()), namespace);

        return pluginNamespace;
    }

    /**
     * Bundles plugins by namespace from a plugin source.
     */
    private static final class Namespaces implements Iterable<PluginNamespace> {
        private final Map<String, PluginNamespace> namespaces;

        private Namespaces() {
            namespaces = new LinkedHashMap<>();
        }

        public boolean isEmpty() {
            return namespaces.isEmpty();
        }

        public int merge(final PluginNamespace category) {
            final PluginNamespace existingCategory = getOrCreate(category.getKey());
            int added = 0;
            for (final PluginType<?> pluginType : category) {
                if (existingCategory.add(pluginType)) {
                    added++;
                }
            }
            return added;
        }

        public void add(final PluginType<?> pluginType) {
            getOrCreate(pluginType.getNamespace()).put(pluginType);
        }

        public PluginNamespace get(final String category) {
            return namespaces.get(category.toLowerCase(Locale.ROOT));
        }

        public PluginNamespace getOrCreate(final String category) {
            return namespaces.computeIfAbsent(
                    category.toLowerCase(Locale.ROOT), key -> new PluginNamespace(key, category));
        }

        @Override
        public Iterator<PluginNamespace> iterator() {
            return namespaces.values().iterator();
        }
    }
}
