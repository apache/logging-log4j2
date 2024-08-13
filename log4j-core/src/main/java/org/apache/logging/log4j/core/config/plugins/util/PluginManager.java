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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.StackLocatorUtil;
import org.apache.logging.log4j.util.Strings;

/**
 * Loads and manages all the plugins.
 */
public class PluginManager {

    private static final CopyOnWriteArrayList<String> PACKAGES = new CopyOnWriteArrayList<>();
    private static final String LOG4J_PACKAGES = "org.apache.logging.log4j.core";
    private static final String DEPRECATION_WARNING =
            "The use of package scanning to locate Log4j plugins is deprecated.\n"
                    + "Please remove the deprecated `{}` method call from `{}`.\n"
                    + "See https://logging.apache.org/log4j/2.x/faq.html#package-scanning for details.";
    private static final String PLUGIN_DESCRIPTOR_DOC =
            "See https://logging.apache.org/log4j/2.x/faq.html#plugin-descriptors for details.";
    private static final String PLUGIN_REGISTRY_DOC =
            "See https://logging.apache.org/log4j/2.x/manual/plugins.html#plugin-registry for details.";

    private static final Logger LOGGER = StatusLogger.getLogger();

    private Map<String, PluginType<?>> plugins = new HashMap<>();
    private final String category;

    /**
     * Constructs a PluginManager for the plugin category name given.
     *
     * @param category The plugin category name.
     */
    public PluginManager(final String category) {
        this.category = category;
    }

    /**
     * Process annotated plugins.
     *
     * @deprecated Use {@link org.apache.logging.log4j.core.config.plugins.processor.PluginProcessor} instead. To do so,
     *             simply include {@code log4j-core} in your dependencies and make sure annotation processing is not
     *             disabled. By default, supported Java compilers will automatically use that plugin processor provided
     *             {@code log4j-core} is on the classpath.
     */
    @Deprecated
    // use PluginProcessor instead
    public static void main(final String[] args) {
        System.err.println("ERROR: this tool is superseded by the annotation processor included in log4j-core.");
        System.err.println("If the annotation processor does not work for you, please see the manual page:");
        System.err.println("https://logging.apache.org/log4j/2.x/manual/configuration.html#ConfigurationSyntax");
        System.exit(-1);
    }

    /**
     * Adds a package name to be scanned for plugins. Must be invoked prior to plugins being collected.
     *
     * @param p The package name. Ignored if {@code null} or empty.
     */
    @Deprecated
    public static void addPackage(final String p) {
        LOGGER.warn(DEPRECATION_WARNING, "PluginManager.addPackage()", StackLocatorUtil.getStackTraceElement(2));
        if (Strings.isBlank(p)) {
            return;
        }
        PACKAGES.addIfAbsent(p);
    }

    /**
     * Adds a list of package names to be scanned for plugins. Convenience method for {@link #addPackage(String)}.
     *
     * @param packages collection of package names to add. Empty and null package names are ignored.
     */
    @Deprecated
    public static void addPackages(final Collection<String> packages) {
        LOGGER.warn(DEPRECATION_WARNING, "PluginManager.addPackages()", StackLocatorUtil.getStackTraceElement(2));
        for (final String pkg : packages) {
            if (Strings.isNotBlank(pkg)) {
                PACKAGES.addIfAbsent(pkg);
            }
        }
    }

    // Used in tests
    static void clearPackages() {
        PACKAGES.clear();
    }

    /**
     * Returns the type of a specified plugin.
     *
     * @param name The name of the plugin.
     * @return The plugin's type.
     */
    public PluginType<?> getPluginType(final String name) {
        return plugins.get(toRootLowerCase(name));
    }

    /**
     * Returns all the matching plugins.
     *
     * @return A Map containing the name of the plugin and its type.
     */
    public Map<String, PluginType<?>> getPlugins() {
        return plugins;
    }

    /**
     * Locates all the plugins.
     */
    public void collectPlugins() {
        collectPlugins(null);
    }

    /**
     * Locates all the plugins including search of specific packages. Warns about name collisions.
     *
     * @param packages the list of packages to scan for plugins
     * @since 2.1
     */
    public void collectPlugins(final List<String> packages) {
        final String categoryLowerCase = toRootLowerCase(category);
        final Map<String, PluginType<?>> newPlugins = new LinkedHashMap<>();

        // First, iterate the Log4j2Plugin.dat files found in the main CLASSPATH
        Map<String, List<PluginType<?>>> builtInPlugins =
                PluginRegistry.getInstance().loadFromMainClassLoader();
        if (builtInPlugins.isEmpty()) {
            // If we didn't find any plugins above, someone must have messed with the log4j-core.jar.
            // Search the standard package in the hopes we can find our core plugins.
            LOGGER.warn(
                    "No Log4j plugin descriptor was found in the classpath.\n"
                            + "Falling back to scanning the `{}` package.\n{}",
                    LOG4J_PACKAGES,
                    PLUGIN_DESCRIPTOR_DOC);
            builtInPlugins = PluginRegistry.getInstance().loadFromPackage(LOG4J_PACKAGES);
        }
        mergeByName(newPlugins, builtInPlugins.get(categoryLowerCase), null);

        // Next, iterate any Log4j2Plugin.dat files from OSGi Bundles
        for (final Map<String, List<PluginType<?>>> pluginsByCategory :
                PluginRegistry.getInstance().getPluginsByCategoryByBundleId().values()) {
            mergeByName(newPlugins, pluginsByCategory.get(categoryLowerCase), null);
        }

        final List<String> scannedPluginClassNames = new ArrayList<>();
        // Next iterate any packages passed to the static addPackage method.
        for (final String pkg : PACKAGES) {
            mergeByName(
                    newPlugins,
                    PluginRegistry.getInstance().loadFromPackage(pkg).get(categoryLowerCase),
                    scannedPluginClassNames);
        }
        // Finally iterate any packages provided in the configuration (note these can be changed at runtime).
        if (packages != null) {
            for (final String pkg : packages) {
                mergeByName(
                        newPlugins,
                        PluginRegistry.getInstance().loadFromPackage(pkg).get(categoryLowerCase),
                        scannedPluginClassNames);
            }
        }
        // Report package scanning deprecation.
        if (!scannedPluginClassNames.isEmpty()) {
            final Predicate<String> customPluginPredicate = PluginManager::isCustomPlugin;
            final String standardPlugins = scannedPluginClassNames.stream()
                    .filter(customPluginPredicate.negate())
                    .collect(Collectors.joining("\n\t"));
            if (!standardPlugins.isEmpty()) {
                LOGGER.warn(
                        "The Log4j plugin descriptors for the following `{}` plugins are missing:\n\t{}\n{}",
                        category,
                        standardPlugins,
                        PLUGIN_DESCRIPTOR_DOC);
            }
            final String customPlugins = scannedPluginClassNames.stream()
                    .filter(customPluginPredicate)
                    .collect(Collectors.joining("\n\t"));
            if (!customPlugins.isEmpty()) {
                LOGGER.warn(
                        "Some custom `{}` Log4j plugins are not properly registered:\n\t{}\n"
                                + "Please consider reporting this to the maintainers of these plugins.\n{}",
                        category,
                        customPlugins,
                        PLUGIN_REGISTRY_DOC);
            }
        }

        LOGGER.debug("PluginManager '{}' found {} plugins", category, newPlugins.size());

        plugins = newPlugins;
    }

    private static boolean isCustomPlugin(final String className) {
        return !className.startsWith("org.apache.logging.log4j") && !className.startsWith("org.apache.log4j");
    }

    private static void mergeByName(
            final Map<String, PluginType<?>> newPlugins,
            final List<PluginType<?>> plugins,
            final List<String> mergedPluginClassNames) {
        if (plugins == null) {
            return;
        }
        for (final PluginType<?> pluginType : plugins) {
            final String key = pluginType.getKey();
            final PluginType<?> existing = newPlugins.get(key);
            if (existing == null) {
                newPlugins.put(key, pluginType);
                if (mergedPluginClassNames != null) {
                    mergedPluginClassNames.add(pluginType.getPluginClass().getName());
                }
            } else if (!existing.getPluginClass().equals(pluginType.getPluginClass())) {
                LOGGER.warn(
                        "Plugin [{}] is already mapped to {}, ignoring {}",
                        key,
                        existing.getPluginClass(),
                        pluginType.getPluginClass());
            }
        }
    }
}
