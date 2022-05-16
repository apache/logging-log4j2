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

package org.apache.logging.log4j.plugins.util;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.plugins.Category;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAliases;
import org.apache.logging.log4j.plugins.Singleton;
import org.apache.logging.log4j.plugins.processor.PluginCache;
import org.apache.logging.log4j.plugins.processor.PluginEntry;
import org.apache.logging.log4j.plugins.processor.PluginService;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LazyValue;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.Strings;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.logging.log4j.util.Unbox.box;

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
    private final LazyValue<Categories> mainPluginCategories = LazyValue.from(() -> {
        final Categories bundle = decodeCacheFiles(LoaderUtil.getClassLoader());
        Throwable throwable = null;
        ClassLoader errorClassLoader = null;
        boolean allFail = true;
        for (ClassLoader classLoader : LoaderUtil.getClassLoaders()) {
            try {
                loadPlugins(classLoader, bundle);
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
        if (bundle.isEmpty()) {
            // If we didn't find any plugins above, someone must have messed with the log4j-core.jar.
            // Search the standard package in the hopes we can find our core plugins.
            loadFromPackage(bundle, "org.apache.logging.log4j.core");
        }
        return bundle;
    });

    /**
     * Contains plugins found in PluginService services and legacy Log4j2Plugins.dat cache files in OSGi Bundles.
     */
    private final Map<Long, Categories> pluginCategoriesByBundleId = new ConcurrentHashMap<>();

    /**
     * Contains plugins found by searching for annotated classes at runtime.
     */
    private final Map<String, Categories> pluginCategoriesByPackage = new ConcurrentHashMap<>();

    /**
     * Resets the registry to an empty state.
     */
    public void clear() {
        mainPluginCategories.set(null);
        pluginCategoriesByPackage.clear();
        pluginCategoriesByBundleId.clear();
    }

    /**
     * Remove the bundle plugins.
     * @param bundleId The bundle id.
     * @since 2.1
     */
    public void clearBundlePlugins(final long bundleId) {
        pluginCategoriesByBundleId.remove(bundleId);
    }

    /**
     * Load plugins from a bundle.
     * @param bundleId The bundle id.
     * @param loader The ClassLoader.
     * @since 3.0.0
     */
    public void loadFromBundle(final long bundleId, final ClassLoader loader) {
        pluginCategoriesByBundleId.computeIfAbsent(bundleId, ignored -> {
            final Categories bundle = decodeCacheFiles(loader);
            loadPlugins(loader, bundle);
            return bundle;
        });
    }

    /**
     * Loads all the plugins in a Bundle.
     * @param bundleId The bundle id.
     * @param pluginsByCategory the plugins organized by category
     * @since 3.0.0
     */
    public void loadFromBundle(final long bundleId, final Map<String, PluginCategory> pluginsByCategory) {
        pluginCategoriesByBundleId.put(bundleId, new Categories(pluginsByCategory));
    }

    /**
     * Load plugins from a specific ClassLoader.
     * @param classLoader The ClassLoader.
     * @param categories The Categories to merge discovered plugins to
     * @since 3.0
     */
    private void loadPlugins(ClassLoader classLoader, Categories categories) {
        final long startTime = System.nanoTime();
        final ServiceLoader<PluginService> serviceLoader = ServiceLoader.load(PluginService.class, classLoader);
        final AtomicInteger pluginCount = new AtomicInteger();
        for (final PluginService pluginService : serviceLoader) {
            pluginService.getCategories().values().forEach(category -> pluginCount.addAndGet(categories.merge(category)));
        }
        final int numPlugins = pluginCount.get();
        LOGGER.debug(() -> {
            final long endTime = System.nanoTime();
            final DecimalFormat numFormat = new DecimalFormat("#0.000000");
            return "Took " + numFormat.format((endTime - startTime) * 1e-9) +
                    " seconds to load " + numPlugins + " plugins from " + classLoader;
        });
    }

    private Categories decodeCacheFiles(final ClassLoader classLoader) {
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
        final Categories categories = new Categories();
        final AtomicInteger pluginCount = new AtomicInteger();
        cache.getAllCategories().forEach((key, outer) ->
                outer.values().forEach(entry -> {
                    final PluginType<?> type = new PluginType<>(entry, classLoader);
                    categories.add(type);
                    pluginCount.incrementAndGet();
                }));
        final int numPlugins = pluginCount.get();
        LOGGER.debug(() -> {
            final long endTime = System.nanoTime();
            final DecimalFormat numFormat = new DecimalFormat("#0.000000");
            return "Took " + numFormat.format((endTime - startTime) * 1e-9) +
                    " seconds to load " + numPlugins + " plugins from " + classLoader;
        });
        return categories;
    }

    private void loadFromPackage(final Categories bundle, final String pkg) {
        if (Strings.isBlank(pkg)) {
            // happens when splitting an empty string
            return;
        }
        final long startTime = System.nanoTime();
        final ResolverUtil resolver = new ResolverUtil();
        final ClassLoader classLoader = LoaderUtil.getClassLoader(getClass(), LoaderUtil.class);
        if (classLoader != null) {
            resolver.setClassLoader(classLoader);
        }
        resolver.findInPackage(new PluginTest(), pkg);

        for (final Class<?> clazz : resolver.getClasses()) {
            final Plugin plugin = clazz.getAnnotation(Plugin.class);
            final Category annotation = clazz.getAnnotation(Category.class);
            final String category = annotation != null ? annotation.value() : Plugin.EMPTY;
            final String elementType = plugin.elementType();
            final String name = plugin.value();
            final String mainElementName = elementType.isEmpty() ? name : elementType;
            final PluginEntry.Builder builder = PluginEntry.builder()
                    .setName(name)
                    .setCategory(category)
                    .setClassName(clazz.getName())
                    .setPrintable(plugin.printObject())
                    .setDefer(plugin.deferChildren());
            final PluginEntry mainEntry = builder
                    .setKey(name.toLowerCase(Locale.ROOT))
                    .setElementName(mainElementName)
                    .get();
            bundle.add(new PluginType<>(mainEntry, clazz));
            final PluginAliases pluginAliases = clazz.getAnnotation(PluginAliases.class);
            if (pluginAliases != null) {
                for (final String alias : pluginAliases.value()) {
                    final String aliasElementName = elementType.equals(Plugin.EMPTY) ? alias : elementType;
                    final PluginEntry aliasEntry = builder
                            .setKey(alias.toLowerCase(Locale.ROOT))
                            .setElementName(aliasElementName)
                            .get();
                    bundle.add(new PluginType<>(aliasEntry, clazz));
                }
            }
        }
        LOGGER.debug(() -> {
            final long endTime = System.nanoTime();
            final DecimalFormat numFormat = new DecimalFormat("#0.000000");
            return "Took " + numFormat.format((endTime - startTime) * 1e-9) +
                    " seconds to load " + resolver.getClasses().size() + " plugins from package " + pkg;
        });
    }

    /**
     * Gets the registered plugins for the given category. If additional scan packages are provided, then plugins
     * are scanned and loaded from there as well.
     */
    public PluginCategory getCategory(final String categoryName, final List<String> additionalScanPackages) {
        final var category = new PluginCategory(categoryName);

        // First, iterate the PluginService services and legacy Log4j2Plugin.dat files found in the main CLASSPATH
        final Categories builtInPlugins = mainPluginCategories.get();
        if (builtInPlugins != null) {
            category.mergeAll(builtInPlugins.get(categoryName));
        }

        // Next, iterate OSGi modules that provide plugins as OSGi services
        pluginCategoriesByBundleId.values().forEach(bundle -> category.mergeAll(bundle.get(categoryName)));

        // Finally, iterate over additional packages from configuration
        if (additionalScanPackages != null) {
            for (final String pkg : additionalScanPackages) {
                category.mergeAll(pluginCategoriesByPackage.computeIfAbsent(pkg, ignored -> {
                    final var bundle = new Categories();
                    loadFromPackage(bundle, pkg);
                    return bundle;
                }).get(categoryName));
            }
        }

        LOGGER.debug("Discovered {} plugins in category '{}'", box(category.size()), categoryName);

        return category;
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

    /**
     * Bundles plugins by category from a plugin source.
     */
    private static class Categories implements Iterable<PluginCategory> {
        private final Map<String, PluginCategory> categories;

        private Categories() {
            categories = new LinkedHashMap<>();
        }

        private Categories(final Map<String, PluginCategory> categories) {
            this.categories = categories;
        }

        public boolean isEmpty() {
            return categories.isEmpty();
        }

        public int merge(final PluginCategory category) {
            final PluginCategory existingCategory = getOrCreate(category.getKey());
            int added = 0;
            for (final PluginType<?> pluginType : category) {
                if (existingCategory.add(pluginType)) {
                    added++;
                }
            }
            return added;
        }

        public void add(final PluginType<?> pluginType) {
            getOrCreate(pluginType.getCategory()).put(pluginType);
        }

        public PluginCategory get(final String category) {
            return categories.get(category.toLowerCase(Locale.ROOT));
        }

        public PluginCategory getOrCreate(final String category) {
            return categories.computeIfAbsent(category.toLowerCase(Locale.ROOT), key -> new PluginCategory(key, category));
        }

        @Override
        public Iterator<PluginCategory> iterator() {
            return categories.values().iterator();
        }
    }
}
