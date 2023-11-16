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
package org.apache.logging.log4j.core.config.plugins.util;

import static org.apache.logging.log4j.util.Strings.toRootLowerCase;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.config.plugins.processor.PluginCache;
import org.apache.logging.log4j.core.config.plugins.processor.PluginEntry;
import org.apache.logging.log4j.core.config.plugins.processor.PluginProcessor;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;

/**
 * Registry singleton for PluginType maps partitioned by source type and then by category names.
 */
public class PluginRegistry {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static volatile PluginRegistry INSTANCE;
    private static final Object INSTANCE_LOCK = new Object();

    /**
     * Contains plugins found in Log4j2Plugins.dat cache files in the main CLASSPATH.
     */
    private final AtomicReference<Map<String, List<PluginType<?>>>> pluginsByCategoryRef = new AtomicReference<>();

    /**
     * Contains plugins found in Log4j2Plugins.dat cache files in OSGi Bundles.
     */
    private final ConcurrentMap<Long, Map<String, List<PluginType<?>>>> pluginsByCategoryByBundleId =
            new ConcurrentHashMap<>();

    /**
     * Contains plugins found by searching for annotated classes at runtime.
     */
    private final ConcurrentMap<String, Map<String, List<PluginType<?>>>> pluginsByCategoryByPackage =
            new ConcurrentHashMap<>();

    private PluginRegistry() {}

    /**
     * Returns the global PluginRegistry instance.
     *
     * @return the global PluginRegistry instance.
     * @since 2.1
     */
    public static PluginRegistry getInstance() {
        PluginRegistry result = INSTANCE;
        if (result == null) {
            synchronized (INSTANCE_LOCK) {
                result = INSTANCE;
                if (result == null) {
                    INSTANCE = result = new PluginRegistry();
                }
            }
        }
        return result;
    }

    /**
     * Resets the registry to an empty state.
     */
    public void clear() {
        pluginsByCategoryRef.set(null);
        pluginsByCategoryByPackage.clear();
        pluginsByCategoryByBundleId.clear();
    }

    /**
     * @since 2.1
     */
    public Map<Long, Map<String, List<PluginType<?>>>> getPluginsByCategoryByBundleId() {
        return pluginsByCategoryByBundleId;
    }

    /**
     * @since 2.1
     */
    public Map<String, List<PluginType<?>>> loadFromMainClassLoader() {
        final Map<String, List<PluginType<?>>> existing = pluginsByCategoryRef.get();
        if (existing != null) {
            // already loaded
            return existing;
        }
        final Map<String, List<PluginType<?>>> newPluginsByCategory = decodeCacheFiles(Loader.getClassLoader());

        // Note multiple threads could be calling this method concurrently. Both will do the work,
        // but only one will be allowed to store the result in the AtomicReference.
        // Return the map produced by whichever thread won the race, so all callers will get the same result.
        if (pluginsByCategoryRef.compareAndSet(null, newPluginsByCategory)) {
            return newPluginsByCategory;
        }
        return pluginsByCategoryRef.get();
    }

    /**
     * @since 2.1
     */
    public void clearBundlePlugins(final long bundleId) {
        pluginsByCategoryByBundleId.remove(bundleId);
    }

    /**
     * @since 2.1
     */
    public Map<String, List<PluginType<?>>> loadFromBundle(final long bundleId, final ClassLoader loader) {
        Map<String, List<PluginType<?>>> existing = pluginsByCategoryByBundleId.get(bundleId);
        if (existing != null) {
            // already loaded from this classloader
            return existing;
        }
        final Map<String, List<PluginType<?>>> newPluginsByCategory = decodeCacheFiles(loader);

        // Note multiple threads could be calling this method concurrently. Both will do the work,
        // but only one will be allowed to store the result in the outer map.
        // Return the inner map produced by whichever thread won the race, so all callers will get the same result.
        existing = pluginsByCategoryByBundleId.putIfAbsent(bundleId, newPluginsByCategory);
        if (existing != null) {
            return existing;
        }
        return newPluginsByCategory;
    }

    private Map<String, List<PluginType<?>>> decodeCacheFiles(final ClassLoader loader) {
        final long startTime = System.nanoTime();
        final PluginCache cache = new PluginCache();
        try {
            final Enumeration<URL> resources = loader.getResources(PluginProcessor.PLUGIN_CACHE_FILE);
            if (resources == null) {
                LOGGER.info("Plugin preloads not available from class loader {}", loader);
            } else {
                cache.loadCacheFiles(resources);
            }
        } catch (final IOException ioe) {
            LOGGER.warn("Unable to preload plugins", ioe);
        }
        final Map<String, List<PluginType<?>>> newPluginsByCategory = new HashMap<>();
        int pluginCount = 0;
        for (final Map.Entry<String, Map<String, PluginEntry>> outer :
                cache.getAllCategories().entrySet()) {
            final String categoryLowerCase = outer.getKey();
            final List<PluginType<?>> types = new ArrayList<>(outer.getValue().size());
            newPluginsByCategory.put(categoryLowerCase, types);
            for (final Map.Entry<String, PluginEntry> inner : outer.getValue().entrySet()) {
                final PluginEntry entry = inner.getValue();
                final String className = entry.getClassName();
                try {
                    final Class<?> clazz = loader.loadClass(className);
                    final PluginType<?> type = new PluginType<>(entry, clazz, entry.getName());
                    types.add(type);
                    ++pluginCount;
                } catch (final ClassNotFoundException e) {
                    LOGGER.info("Plugin [{}] could not be loaded due to missing classes.", className, e);
                } catch (final LinkageError e) {
                    LOGGER.info("Plugin [{}] could not be loaded due to linkage error.", className, e);
                }
            }
        }
        final int numPlugins = pluginCount;
        LOGGER.debug(() -> {
            final long endTime = System.nanoTime();
            final StringBuilder sb = new StringBuilder("Took ");
            final DecimalFormat numFormat = new DecimalFormat("#0.000000");
            sb.append(numFormat.format((endTime - startTime) * 1e-9));
            sb.append(" seconds to load ").append(numPlugins);
            sb.append(" plugins from ").append(loader);
            return sb.toString();
        });
        return newPluginsByCategory;
    }

    /**
     * @since 2.1
     */
    public Map<String, List<PluginType<?>>> loadFromPackage(final String pkg) {
        if (Strings.isBlank(pkg)) {
            // happens when splitting an empty string
            return Collections.emptyMap();
        }
        Map<String, List<PluginType<?>>> existing = pluginsByCategoryByPackage.get(pkg);
        if (existing != null) {
            // already loaded this package
            return existing;
        }

        final long startTime = System.nanoTime();
        final ResolverUtil resolver = new ResolverUtil();
        final ClassLoader classLoader = Loader.getClassLoader();
        if (classLoader != null) {
            resolver.setClassLoader(classLoader);
        }
        resolver.findInPackage(new PluginTest(), pkg);

        final Map<String, List<PluginType<?>>> newPluginsByCategory = new HashMap<>();
        for (final Class<?> clazz : resolver.getClasses()) {
            final Plugin plugin = clazz.getAnnotation(Plugin.class);
            final String categoryLowerCase = toRootLowerCase(plugin.category());
            List<PluginType<?>> list = newPluginsByCategory.get(categoryLowerCase);
            if (list == null) {
                newPluginsByCategory.put(categoryLowerCase, list = new ArrayList<>());
            }
            final PluginEntry mainEntry = new PluginEntry();
            final String mainElementName =
                    plugin.elementType().equals(Plugin.EMPTY) ? plugin.name() : plugin.elementType();
            mainEntry.setKey(toRootLowerCase(plugin.name()));
            mainEntry.setName(plugin.name());
            mainEntry.setCategory(plugin.category());
            mainEntry.setClassName(clazz.getName());
            mainEntry.setPrintable(plugin.printObject());
            mainEntry.setDefer(plugin.deferChildren());
            final PluginType<?> mainType = new PluginType<>(mainEntry, clazz, mainElementName);
            list.add(mainType);
            final PluginAliases pluginAliases = clazz.getAnnotation(PluginAliases.class);
            if (pluginAliases != null) {
                for (final String alias : pluginAliases.value()) {
                    final PluginEntry aliasEntry = new PluginEntry();
                    final String aliasElementName =
                            plugin.elementType().equals(Plugin.EMPTY) ? alias.trim() : plugin.elementType();
                    aliasEntry.setKey(toRootLowerCase(alias.trim()));
                    aliasEntry.setName(plugin.name());
                    aliasEntry.setCategory(plugin.category());
                    aliasEntry.setClassName(clazz.getName());
                    aliasEntry.setPrintable(plugin.printObject());
                    aliasEntry.setDefer(plugin.deferChildren());
                    final PluginType<?> aliasType = new PluginType<>(aliasEntry, clazz, aliasElementName);
                    list.add(aliasType);
                }
            }
        }
        LOGGER.debug(() -> {
            final long endTime = System.nanoTime();
            final StringBuilder sb = new StringBuilder("Took ");
            final DecimalFormat numFormat = new DecimalFormat("#0.000000");
            sb.append(numFormat.format((endTime - startTime) * 1e-9));
            sb.append(" seconds to load ").append(resolver.getClasses().size());
            sb.append(" plugins from package ").append(pkg);
            return sb.toString();
        });

        // Note multiple threads could be calling this method concurrently. Both will do the work,
        // but only one will be allowed to store the result in the outer map.
        // Return the inner map produced by whichever thread won the race, so all callers will get the same result.
        existing = pluginsByCategoryByPackage.putIfAbsent(pkg, newPluginsByCategory);
        if (existing != null) {
            return existing;
        }
        return newPluginsByCategory;
    }

    /**
     * A Test that checks to see if each class is annotated with the 'Plugin' annotation. If it
     * is, then the test returns true, otherwise false.
     *
     * @since 2.1
     */
    public static class PluginTest implements ResolverUtil.Test {
        @Override
        public boolean matches(final Class<?> type) {
            return type != null && type.isAnnotationPresent(Plugin.class);
        }

        @Override
        public String toString() {
            return "annotated with @" + Plugin.class.getSimpleName();
        }

        @Override
        public boolean matches(final URI resource) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean doesMatchClass() {
            return true;
        }

        @Override
        public boolean doesMatchResource() {
            return false;
        }
    }
}
