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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.helpers.Loader;
import org.apache.logging.log4j.status.StatusLogger;

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

/**
 * Component that loads and manages all the plugins.
 */
public class PluginManager {

    private static final long NANOS_PER_SECOND = 1000000000L;

    private static ConcurrentMap<String, ConcurrentMap<String, PluginType>> pluginTypeMap =
        new ConcurrentHashMap<String, ConcurrentMap<String, PluginType>>();

    private static final CopyOnWriteArrayList<String> packages = new CopyOnWriteArrayList<String>();
    private static final String PATH = "org/apache/logging/log4j/core/config/plugins/";
    private static final String FILENAME = "Log4j2Plugins.dat";
    private static final String LOG4J_PACKAGES = "org.apache.logging.log4j.core";

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static String rootDir;

    private Map<String, PluginType> plugins = new HashMap<String, PluginType>();
    private final String type;
    private final Class clazz;

    /**
     * Constructor that takes only a type name.
     * @param type The type name.
     */
    public PluginManager(String type) {
        this.type = type;
        this.clazz = null;
    }

    /**
     * Constructor that takes a type name and a Class.
     * @param type The type that must be matched.
     * @param clazz The Class each match must be an instance of.
     */
    public PluginManager(String type, Class clazz) {
        this.type = type;
        this.clazz = clazz;
    }

    public static void main(String[] args) throws Exception {
        if (args == null || args.length < 1) {
            System.err.println("A target directory must be specified");
            System.exit(-1);
        }
        rootDir = args[0].endsWith("/") || args[0].endsWith("\\") ? args[0] : args[0] + "/";

        PluginManager manager = new PluginManager("Core");
        String packages = args.length == 2 ? args[1] : null;

        manager.collectPlugins(false, packages);
        encode(pluginTypeMap);
    }

    /**
     * Adds a package name to be scanned for plugins. Must be invoked prior to plugins being collected.
     * @param p The package name.
     */
    public static void addPackage(String p) {
        packages.addIfAbsent(p);
    }

    /**
     * Returns the type of a specified plugin.
     * @param name The name of the plugin.
     * @return The plugin's type.
     */
    public PluginType getPluginType(String name) {
        return plugins.get(name.toLowerCase());
    }

    /**
     * Returns all the matching plugins.
     * @return A Map containing the name of the plugin and its type.
     */
    public Map<String, PluginType> getPlugins() {
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
    public void collectPlugins(boolean preLoad, String pkgs) {
        if (pluginTypeMap.containsKey(type)) {
            plugins = pluginTypeMap.get(type);
            preLoad = false;
        }
        long start = System.nanoTime();
        ResolverUtil<?> r = new ResolverUtil();
        ClassLoader loader = Loader.getClassLoader();
        if (loader != null) {
            r.setClassLoader(loader);
        }
        if (preLoad) {
            ConcurrentMap<String, ConcurrentMap<String, PluginType>> map = decode(loader);
            if (map != null) {
                pluginTypeMap = map;
                plugins = map.get(type);
            } else {
                LOGGER.warn("Plugin preloads not available");
            }
        }
        if (plugins.size() == 0) {
            if (pkgs == null) {
                packages.add(LOG4J_PACKAGES);
            } else {
                String[] names = pkgs.split(",");
                for (String name : names) {
                    packages.add(name);
                }
            }
        }
        ResolverUtil.Test test = new PluginTest(clazz);
        for (String pkg : packages) {
            r.findInPackage(test, pkg);
        }
        for (Class<?> item : r.getClasses()) {
            Plugin p = item.getAnnotation(Plugin.class);
            String pluginType = p.type();
            if (!pluginTypeMap.containsKey(pluginType)) {
                pluginTypeMap.putIfAbsent(pluginType, new ConcurrentHashMap<String, PluginType>());
            }
            Map<String, PluginType> map = pluginTypeMap.get(pluginType);
            String type = p.elementType().equals(Plugin.EMPTY) ? p.name() : p.elementType();
            map.put(p.name().toLowerCase(), new PluginType(item, type, p.printObject(), p.deferChildren()));
        }
        long elapsed = System.nanoTime() - start;
        plugins = pluginTypeMap.get(type);
        StringBuilder sb = new StringBuilder("Generated plugins");
        sb.append(" in ");
        DecimalFormat numFormat = new DecimalFormat("#0");
        long seconds = elapsed / NANOS_PER_SECOND;
        elapsed %= NANOS_PER_SECOND;
        sb.append(numFormat.format(seconds)).append(".");
        numFormat = new DecimalFormat("000000000");
        sb.append(numFormat.format(elapsed)).append(" seconds");
        LOGGER.debug(sb.toString());
    }

    private static ConcurrentMap<String, ConcurrentMap<String, PluginType>> decode(ClassLoader loader) {
        Enumeration<URL> resources;
        try {
            resources = loader.getResources(PATH + FILENAME);
        } catch (IOException ioe) {
            LOGGER.warn("Unable to preload plugins", ioe);
            return null;
        }
        ConcurrentMap<String, ConcurrentMap<String, PluginType>> map =
            new ConcurrentHashMap<String, ConcurrentMap<String, PluginType>>();
        while (resources.hasMoreElements()) {
            try {
                URL url = resources.nextElement();
                LOGGER.debug("Found Plugin Map at {}", url.toExternalForm());
                InputStream is = url.openStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                DataInputStream dis = new DataInputStream(bis);
                int count = dis.readInt();
                for (int j = 0; j < count; ++j) {
                    String type = dis.readUTF();
                    int entries = dis.readInt();
                    ConcurrentMap<String, PluginType> types = map.get(type);
                    if (types == null) {
                        types = new ConcurrentHashMap<String, PluginType>(count);
                    }
                    for (int i = 0; i < entries; ++i) {
                        String key = dis.readUTF();
                        String className = dis.readUTF();
                        String name = dis.readUTF();
                        boolean printable = dis.readBoolean();
                        boolean defer = dis.readBoolean();
                        Class clazz = Class.forName(className);
                        types.put(key, new PluginType(clazz, name, printable, defer));
                    }
                    map.putIfAbsent(type, types);
                }
                dis.close();
            } catch (Exception ex) {
                LOGGER.warn("Unable to preload plugins", ex);
                return null;
            }
        }
        return map.size() == 0 ? null : map;
    }

    private static void encode(ConcurrentMap<String, ConcurrentMap<String, PluginType>> map) {
        String fileName = rootDir + PATH + FILENAME;
        try {
            File file = new File(rootDir + PATH);
            file.mkdirs();
            FileOutputStream fos = new FileOutputStream(fileName);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeInt(map.size());
            for (Map.Entry<String, ConcurrentMap<String, PluginType>> outer : map.entrySet()) {
                dos.writeUTF(outer.getKey());
                dos.writeInt(outer.getValue().size());
                for (Map.Entry<String, PluginType> entry : outer.getValue().entrySet()) {
                    dos.writeUTF(entry.getKey());
                    PluginType pt = entry.getValue();
                    dos.writeUTF(pt.getPluginClass().getName());
                    dos.writeUTF(pt.getElementName());
                    dos.writeBoolean(pt.isObjectPrintable());
                    dos.writeBoolean(pt.isDeferChildren());
                }
            }
            dos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * A Test that checks to see if each class is annotated with a specific annotation. If it
     * is, then the test returns true, otherwise false.
     */
    public static class PluginTest extends ResolverUtil.ClassTest {
        private final Class isA;

        /**
         * Constructs an AnnotatedWith test for the specified annotation type.
         * @param isA The class to compare against.
         */
        public PluginTest(Class isA) {
            this.isA = isA;
        }

        /**
         * Returns true if the type is annotated with the class provided to the constructor.
         * @param type The type to check for.
         * @return true if the Class is of the specified type.
         */
        public boolean matches(Class type) {
            return type != null && type.isAnnotationPresent(Plugin.class) &&
                (isA == null || isA.isAssignableFrom(type));
        }

        @Override
        public String toString() {
            StringBuilder msg = new StringBuilder("annotated with @" + Plugin.class.getSimpleName());
            if (isA != null) {
                msg.append(" is assignable to " + isA.getSimpleName());
            }
            return msg.toString();
        }
    }

}
