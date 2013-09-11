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
package org.apache.logging.log4j.core.config.plugins;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.helpers.Closer;
import org.apache.logging.log4j.core.helpers.Loader;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Loads and manages all the plugins.
 */
public class PluginManager {

    private static final long NANOS_PER_SECOND = 1000000000L;

    private static ConcurrentMap<String, ConcurrentMap<String, PluginType<?>>> pluginTypeMap =
        new ConcurrentHashMap<String, ConcurrentMap<String, PluginType<?>>>();

    private static final CopyOnWriteArrayList<String> PACKAGES = new CopyOnWriteArrayList<String>();
    private static final String PATH = "org/apache/logging/log4j/core/config/plugins/";
    private static final String FILENAME = "Log4j2Plugins.dat";
    private static final String LOG4J_PACKAGES = "org.apache.logging.log4j.core";

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static String rootDir;

    private Map<String, PluginType<?>> plugins = new HashMap<String, PluginType<?>>();
    private final String type;
    private final Class<?> clazz;

    /**
     * Constructor that takes only a type name.
     * @param type The type name.
     */
    public PluginManager(final String type) {
        this.type = type;
        this.clazz = null;
    }

    /**
     * Constructor that takes a type name and a Class.
     * @param type The type that must be matched.
     * @param clazz The Class each match must be an instance of.
     */
    public PluginManager(final String type, final Class<?> clazz) {
        this.type = type;
        this.clazz = clazz;
    }

    public static void main(final String[] args) throws Exception {
        if (args == null || args.length < 1) {
            System.err.println("A target directory must be specified");
            System.exit(-1);
        }
        rootDir = args[0].endsWith("/") || args[0].endsWith("\\") ? args[0] : args[0] + "/";

        final PluginManager manager = new PluginManager("Core");
        final String packages = args.length == 2 ? args[1] : null;

        manager.collectPlugins(false, packages);
        encode(pluginTypeMap);
    }

    /**
     * Adds a package name to be scanned for plugins. Must be invoked prior to plugins being collected.
     * @param p The package name.
     */
    public static void addPackage(final String p) {
        if (PACKAGES.addIfAbsent(p))
        {
            //set of available plugins could have changed, reset plugin cache for newly-retrieved managers
            pluginTypeMap.clear();
        }
    }

    /**
     * Returns the type of a specified plugin.
     * @param name The name of the plugin.
     * @return The plugin's type.
     */
    public PluginType<?> getPluginType(final String name) {
        return plugins.get(name.toLowerCase());
    }

    /**
     * Returns all the matching plugins.
     * @return A Map containing the name of the plugin and its type.
     */
    public Map<String, PluginType<?>> getPlugins() {
        return plugins;
    }

    /**
     * Locates all the plugins.
     */
    public void collectPlugins() {
        collectPlugins(true, null);
    }

    /**
     * Collects plugins, optionally obtaining them from a preload map.
     * @param preLoad if true, plugins will be obtained from the preload map.
     * @param pkgs A comma separated list of package names to scan for plugins. If
     * null the default Log4j package name will be used.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void collectPlugins(boolean preLoad, final String pkgs) {
        if (pluginTypeMap.containsKey(type)) {
            plugins = pluginTypeMap.get(type);
            preLoad = false;
        }
        final long start = System.nanoTime();
        final ResolverUtil resolver = new ResolverUtil();
        final ClassLoader classLoader = Loader.getClassLoader();
        if (classLoader != null) {
            resolver.setClassLoader(classLoader);
        }
        if (preLoad) {
            final ConcurrentMap<String, ConcurrentMap<String, PluginType<?>>> map = decode(classLoader);
            if (map != null) {
                pluginTypeMap = map;
                plugins = map.get(type);
            } else {
                LOGGER.warn("Plugin preloads not available from class loader {}", classLoader);
            }
        }
        if (plugins == null || plugins.size() == 0) {
            if (pkgs == null) {
                if (!PACKAGES.contains(LOG4J_PACKAGES)) {
                    PACKAGES.add(LOG4J_PACKAGES);
                }
            } else {
                final String[] names = pkgs.split(",");
                for (final String name : names) {
                    PACKAGES.add(name);
                }
            }
        }
        final ResolverUtil.Test test = new PluginTest(clazz);
        for (final String pkg : PACKAGES) {
            resolver.findInPackage(test, pkg);
        }
        for (final Class<?> clazz : resolver.getClasses()) {
            final Plugin plugin = clazz.getAnnotation(Plugin.class);
            final String pluginCategory = plugin.category();
            if (!pluginTypeMap.containsKey(pluginCategory)) {
                pluginTypeMap.putIfAbsent(pluginCategory, new ConcurrentHashMap<String, PluginType<?>>());
            }
            final Map<String, PluginType<?>> map = pluginTypeMap.get(pluginCategory);
            String type = plugin.elementType().equals(Plugin.EMPTY) ? plugin.name() : plugin.elementType();
            PluginType pluginType = new PluginType(clazz, type, plugin.printObject(), plugin.deferChildren());
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
        long elapsed = System.nanoTime() - start;
        plugins = pluginTypeMap.get(type);
        final StringBuilder sb = new StringBuilder("Generated plugins");
        sb.append(" in ");
        DecimalFormat numFormat = new DecimalFormat("#0");
        final long seconds = elapsed / NANOS_PER_SECOND;
        elapsed %= NANOS_PER_SECOND;
        sb.append(numFormat.format(seconds)).append('.');
        numFormat = new DecimalFormat("000000000");
        sb.append(numFormat.format(elapsed)).append(" seconds");
        LOGGER.debug(sb.toString());
    }

    @SuppressWarnings({ "unchecked" })
    private static ConcurrentMap<String, ConcurrentMap<String, PluginType<?>>> decode(final ClassLoader classLoader) {
        Enumeration<URL> resources;
        try {
            resources = classLoader.getResources(PATH + FILENAME);
        } catch (final IOException ioe) {
            LOGGER.warn("Unable to preload plugins", ioe);
            return null;
        }
        final ConcurrentMap<String, ConcurrentMap<String, PluginType<?>>> map =
            new ConcurrentHashMap<String, ConcurrentMap<String, PluginType<?>>>();
        while (resources.hasMoreElements()) {
            DataInputStream dis = null;
            try {
                final URL url = resources.nextElement();
                LOGGER.debug("Found Plugin Map at {}", url.toExternalForm());
                final InputStream is = url.openStream();
                final BufferedInputStream bis = new BufferedInputStream(is);
                dis = new DataInputStream(bis);
                final int count = dis.readInt();
                for (int j = 0; j < count; ++j) {
                    final String type = dis.readUTF();
                    final int entries = dis.readInt();
                    ConcurrentMap<String, PluginType<?>> types = map.get(type);
                    if (types == null) {
                        types = new ConcurrentHashMap<String, PluginType<?>>(count);
                    }
                    for (int i = 0; i < entries; ++i) {
                        final String key = dis.readUTF();
                        final String className = dis.readUTF();
                        final String name = dis.readUTF();
                        final boolean printable = dis.readBoolean();
                        final boolean defer = dis.readBoolean();
                        final Class<?> clazz = Class.forName(className);
                        types.put(key, new PluginType(clazz, name, printable, defer));
                    }
                    map.putIfAbsent(type, types);
                }
            } catch (final Exception ex) {
                LOGGER.warn("Unable to preload plugins", ex);
                return null;
            } finally {
                Closer.closeSilent(dis);
            }
        }
        return map.size() == 0 ? null : map;
    }

    private static void encode(final ConcurrentMap<String, ConcurrentMap<String, PluginType<?>>> map) {
        final String fileName = rootDir + PATH + FILENAME;
        DataOutputStream dos = null;
        try {
            final File file = new File(rootDir + PATH);
            file.mkdirs();
            final FileOutputStream fos = new FileOutputStream(fileName);
            final BufferedOutputStream bos = new BufferedOutputStream(fos);
            dos = new DataOutputStream(bos);
            dos.writeInt(map.size());
            for (final Map.Entry<String, ConcurrentMap<String, PluginType<?>>> outer : map.entrySet()) {
                dos.writeUTF(outer.getKey());
                dos.writeInt(outer.getValue().size());
                for (final Map.Entry<String, PluginType<?>> entry : outer.getValue().entrySet()) {
                    dos.writeUTF(entry.getKey());
                    final PluginType<?> pt = entry.getValue();
                    dos.writeUTF(pt.getPluginClass().getName());
                    dos.writeUTF(pt.getElementName());
                    dos.writeBoolean(pt.isObjectPrintable());
                    dos.writeBoolean(pt.isDeferChildren());
                }
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
        } finally {
            Closer.closeSilent(dos);
        }
    }

    /**
     * A Test that checks to see if each class is annotated with a specific annotation. If it
     * is, then the test returns true, otherwise false.
     */
    public static class PluginTest extends ResolverUtil.ClassTest {
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
    }

}
