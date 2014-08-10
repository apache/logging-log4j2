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
package org.apache.logging.log4j.core.config.plugins.util;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.config.plugins.processor.PluginProcessor;
import org.apache.logging.log4j.core.util.ClassLoaderResourceLoader;
import org.apache.logging.log4j.core.util.Closer;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.core.util.ResourceLoader;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Loads and manages all the plugins.
 */
public class PluginManager {

    // TODO: re-use PluginCache code from plugin processor
    private static final PluginRegistry<PluginType<?>> REGISTRY = new PluginRegistry<PluginType<?>>();
    private static final CopyOnWriteArrayList<String> PACKAGES = new CopyOnWriteArrayList<String>();
    private static final String LOG4J_PACKAGES = "org.apache.logging.log4j.core";

    private static final Logger LOGGER = StatusLogger.getLogger();

    private Map<String, PluginType<?>> plugins = new HashMap<String, PluginType<?>>();
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
        System.err.println("http://logging.apache.org/log4j/2.x/manual/configuration.html#ConfigurationSyntax");
        System.exit(-1);
    }

    /**
     * Adds a package name to be scanned for plugins. Must be invoked prior to plugins being collected.
     * 
     * @param p The package name. Ignored if {@code null} or empty.
     */
    public static void addPackage(final String p) {
        if (p == null || p.isEmpty()) {
            return;
        }
        if (PACKAGES.addIfAbsent(p)) {
            // set of available plugins could have changed, reset plugin cache for newly-retrieved managers
            REGISTRY.clear(); // TODO confirm if this is correct
        }
    }

    /**
     * Adds a list of package names to be scanned for plugins. Convenience method for {@link #addPackage(String)}.
     *
     * @param packages collection of package names to add. Ignored if {@code null} or empty.
     */
    public static void addPackages(final Collection<String> packages) {
        if (packages == null || packages.isEmpty()) {
            return;
        }
        PACKAGES.addAllAbsent(packages);
    }

    /**
     * Returns the type of a specified plugin.
     * 
     * @param name The name of the plugin.
     * @return The plugin's type.
     */
    public PluginType<?> getPluginType(final String name) {
        return plugins.get(name.toLowerCase());
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
        collectPlugins(true);
    }

    /**
     * Collects plugins, optionally obtaining them from a preload map.
     * 
     * @param preLoad if true, plugins will be obtained from the preload map.
     *
     */
    public void collectPlugins(boolean preLoad) {
        if (REGISTRY.hasCategory(category)) {
            plugins = REGISTRY.getCategory(category);
            preLoad = false;
        }
        long start = System.nanoTime();
        if (preLoad) {
            final ResourceLoader loader = new ClassLoaderResourceLoader(Loader.getClassLoader());
            loadPlugins(loader);
        }
        plugins = REGISTRY.getCategory(category);
        loadFromPackages(start, preLoad);

        long elapsed = System.nanoTime() - start;
        reportPluginLoadDuration(preLoad, elapsed);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void loadFromPackages(final long start, final boolean preLoad) {
        if (plugins == null || plugins.size() == 0) {
            if (!PACKAGES.contains(LOG4J_PACKAGES)) {
                PACKAGES.add(LOG4J_PACKAGES);
            }
        }
        final ResolverUtil resolver = new ResolverUtil();
        final ClassLoader classLoader = Loader.getClassLoader();
        if (classLoader != null) {
            resolver.setClassLoader(classLoader);
        }
        final Class<?> cls = null;
        final ResolverUtil.Test test = new PluginTest(cls);
        for (final String pkg : PACKAGES) {
            resolver.findInPackage(test, pkg);
        }
        for (final Class<?> clazz : resolver.getClasses()) {
            final Plugin plugin = clazz.getAnnotation(Plugin.class);
            final String pluginCategory = plugin.category();
            final Map<String, PluginType<?>> map = REGISTRY.getCategory(pluginCategory);
            String type = plugin.elementType().equals(Plugin.EMPTY) ? plugin.name() : plugin.elementType();
            PluginType<?> pluginType = new PluginType(clazz, type, plugin.printObject(), plugin.deferChildren());
            map.put(plugin.name().toLowerCase(), pluginType);
            final PluginAliases pluginAliases = clazz.getAnnotation(PluginAliases.class);
            if (pluginAliases != null) {
                for (String alias : pluginAliases.value()) {
                    type =  plugin.elementType().equals(Plugin.EMPTY) ? alias : plugin.elementType();
                    pluginType = new PluginType(clazz, type, plugin.printObject(), plugin.deferChildren());
                    map.put(alias.trim().toLowerCase(), pluginType);
                }
            }
        }
        plugins = REGISTRY.getCategory(category);
    }

    private void reportPluginLoadDuration(final boolean preLoad, long elapsed) {
        final StringBuilder sb = new StringBuilder("Generated plugins in ");
        DecimalFormat numFormat = new DecimalFormat("#0.000000");
        final double seconds = elapsed / (1000.0 * 1000.0 * 1000.0);
        sb.append(numFormat.format(seconds)).append(" seconds, packages: ");
        sb.append(PACKAGES);
        sb.append(", preload: ");
        sb.append(preLoad);
        sb.append(".");
        LOGGER.debug(sb.toString());
    }

    public static void loadPlugins(final ResourceLoader loader) {
        final PluginRegistry<PluginType<?>> registry = decode(loader);
        if (registry != null) {
            for (final Map.Entry<String, ConcurrentMap<String, PluginType<?>>> entry : registry.getCategories()) {
                REGISTRY.getCategory(entry.getKey()).putAll(entry.getValue());
            }
        } else {
            LOGGER.info("Plugin preloads not available from class loader {}", loader);
        }
    }

    private static PluginRegistry<PluginType<?>> decode(final ResourceLoader loader) {
        final Enumeration<URL> resources;
        try {
            resources = loader.getResources(PluginProcessor.PLUGIN_CACHE_FILE);
            if (resources == null) {
                return null;
            }
        } catch (final IOException ioe) {
            LOGGER.warn("Unable to preload plugins", ioe);
            return null;
        }
        final PluginRegistry<PluginType<?>> map = new PluginRegistry<PluginType<?>>();
        while (resources.hasMoreElements()) {
            final URL url = resources.nextElement();
            LOGGER.debug("Found Plugin Map at {}", url.toExternalForm());
            final InputStream is;
            try {
                is = url.openStream();
            } catch (final IOException e) {
                LOGGER.warn("Unable to open {}", url.toExternalForm(), e);
                continue;
            }
            final DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
            try {
                final int count = dis.readInt();
                for (int j = 0; j < count; ++j) {
                    final String category = dis.readUTF();
                    final int entries = dis.readInt();
                    final Map<String, PluginType<?>> types = map.getCategory(category);
                    for (int i = 0; i < entries; ++i) {
                        final String key = dis.readUTF();
                        final String className = dis.readUTF();
                        final String name = dis.readUTF();
                        final boolean printable = dis.readBoolean();
                        final boolean defer = dis.readBoolean();
                        try {
                            final Class<?> clazz = loader.loadClass(className);
                            @SuppressWarnings({ "unchecked", "rawtypes" })
                            final PluginType<?> pluginType = new PluginType(clazz, name, printable, defer);
                            types.put(key, pluginType);
                        } catch (final ClassNotFoundException e) {
                            LOGGER.info("Plugin [{}] could not be loaded due to missing classes.", className, e);
                        } catch (final VerifyError e) {
                            LOGGER.info("Plugin [{}] could not be loaded due to verification error.", className, e);
                        }
                    }
                }
            } catch (final IOException ex) {
                LOGGER.warn("Unable to preload plugins", ex);
            } finally {
                Closer.closeSilently(dis);
            }
        }
        return map.isEmpty() ? null : map;
    }

    /**
     * A Test that checks to see if each class is annotated with a specific annotation. If it
     * is, then the test returns true, otherwise false.
     */
    public static class PluginTest implements ResolverUtil.Test {
        private final Class<?> isA;

        /**
         * Constructs an AnnotatedWith test for the specified annotation type.
         * @param isA The class to compare against.
         */
        public PluginTest(final Class<?> isA) {
            this.isA = isA;
        }

        /**
         * Returns true if the type is annotated with the class provided to the constructor.
         * @param type The type to check for.
         * @return true if the Class is of the specified type.
         */
        @Override
        public boolean matches(final Class<?> type) {
            return type != null && type.isAnnotationPresent(Plugin.class) &&
                (isA == null || isA.isAssignableFrom(type));
        }

        @Override
        public String toString() {
            final StringBuilder msg = new StringBuilder("annotated with @" + Plugin.class.getSimpleName());
            if (isA != null) {
                msg.append(" is assignable to " + isA.getSimpleName());
            }
            return msg.toString();
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
