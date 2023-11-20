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
import org.apache.logging.log4j.plugins.Singleton;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.LoaderUtil;

/**
 * Registry singleton for PluginType maps partitioned by source type and then by category names.
 */
@Singleton
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
    private final Lazy<Namespaces> namespacesLazy = Lazy.lazy(() -> {
        final Namespaces namespaces = decodeCacheFiles(LoaderUtil.getClassLoader());
        Throwable throwable = null;
        ClassLoader errorClassLoader = null;
        boolean allFail = true;
        for (ClassLoader classLoader : LoaderUtil.getClassLoaders()) {
            try {
                loadPlugins(classLoader, namespaces);
                allFail = false;
            } catch (Throwable ex) {
                if (throwable == null) {
                    throwable = ex;
                    errorClassLoader = classLoader;
                }
            }
        }
        if (allFail && throwable != null) {
            LOGGER.debug("Unable to retrieve provider from ClassLoader {}", errorClassLoader, throwable);
        }
        return namespaces;
    });

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
    private void loadPlugins(ClassLoader classLoader, Namespaces namespaces) {
        final long startTime = System.nanoTime();
        final ServiceLoader<PluginService> serviceLoader = ServiceLoader.load(PluginService.class, classLoader);
        final AtomicInteger pluginCount = new AtomicInteger();
        for (final PluginService pluginService : serviceLoader) {
            pluginService
                    .getNamespaces()
                    .values()
                    .forEach(category -> pluginCount.addAndGet(namespaces.merge(category)));
        }
        final int numPlugins = pluginCount.get();
        LOGGER.debug(() -> {
            final long endTime = System.nanoTime();
            final DecimalFormat numFormat = new DecimalFormat("#0.000000");
            return "Took " + numFormat.format((endTime - startTime) * 1e-9) + " seconds to load " + numPlugins
                    + " plugins from " + classLoader;
        });
    }

    private Namespaces decodeCacheFiles(final ClassLoader classLoader) {
        final long startTime = System.nanoTime();
        final PluginCache cache = new PluginCache();
        try {
            final Enumeration<URL> resources = classLoader.getResources(PLUGIN_CACHE_FILE);
            if (resources == null) {
                LOGGER.info("Plugin preloads not available from class loader {}", classLoader);
            } else {
                cache.loadCacheFiles(resources);
            }
        } catch (final IOException ioe) {
            LOGGER.warn("Unable to preload plugins", ioe);
        }
        final Namespaces namespaces = new Namespaces();
        final AtomicInteger pluginCount = new AtomicInteger();
        cache.getAllNamespaces().forEach((key, outer) -> outer.values().forEach(entry -> {
            final PluginType<?> type = new PluginType<>(entry, classLoader);
            namespaces.add(type);
            pluginCount.incrementAndGet();
        }));
        final int numPlugins = pluginCount.get();
        LOGGER.debug(() -> {
            final long endTime = System.nanoTime();
            final DecimalFormat numFormat = new DecimalFormat("#0.000000");
            return "Took " + numFormat.format((endTime - startTime) * 1e-9) + " seconds to load " + numPlugins
                    + " plugins from " + classLoader;
        });
        return namespaces;
    }

    /**
     * Gets the registered plugins for the given namespace. If additional scan packages are provided, then plugins
     * are scanned and loaded from there as well.
     */
    public PluginNamespace getNamespace(final String namespace) {
        final var pluginNamespace = new PluginNamespace(namespace);

        // First, iterate the PluginService services
        final Namespaces builtInPlugins = namespacesLazy.value();
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
