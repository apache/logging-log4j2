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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 */
public class PluginManager {

    private static long NANOS_PER_SECOND = 1000000000L;
    private Map<String, PluginType> plugins = new HashMap<String, PluginType>();

    private static ConcurrentMap<String, ConcurrentMap<String, PluginType>> pluginTypeMap =
        new ConcurrentHashMap<String, ConcurrentMap<String, PluginType>>();

    private static CopyOnWriteArrayList<String> packages = new CopyOnWriteArrayList<String>();
    private static final String PATH = "org/apache/logging/log4j/core/config/plugins/";
    private static final String PREFIX = "Log4j2";
    private static final String FILENAME = "Log4j2Plugins.dat";
    private static final String LOG4J_PACKAGES = "org.apache.logging.log4j.core";

    private static Logger logger = StatusLogger.getLogger();

    private final String type;
    private final Class clazz;

    public static void main(String[] args) throws Exception {
        if (args == null || args.length == 0) {
            System.err.println("At least 1 type must be specified.");
            System.exit(-1);
        }
        List<String> list = new ArrayList<String>();
        for (String arg : args) {
            String[] types = arg.trim().split(",");
            for (String type : types) {
                if (type.trim().length() == 0) {
                    continue;
                }
                list.add(type);
            }
        }

        PluginManager manager = new PluginManager("Core");
        manager.collectPlugins(false);
        encode(pluginTypeMap);
    }

    public PluginManager(String type) {
        this.type = type;
        this.clazz = null;
    }


    public PluginManager(String type, Class clazz) {
        this.type = type;
        this.clazz = clazz;
    }

    public static void addPackage(String p) {
        packages.addIfAbsent(p);
    }

    public PluginType getPluginType(String name) {
        return plugins.get(name.toLowerCase());
    }

    public Map<String, PluginType> getPlugins() {
        return plugins;
    }

    public void collectPlugins() {
        collectPlugins(true);
    }


    public void collectPlugins(boolean preLoad) {
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
            if (map != null){
                pluginTypeMap = map;
                plugins = map.get(type);
            } else {
                logger.warn("Plugin preloads not available");
            }
        }
        if (plugins.size() == 0) {
            packages.add(LOG4J_PACKAGES);
        }
        ResolverUtil.Test test = new PluginTest(clazz);
        for (String pkg : packages) {
            r.findInPackage(test, pkg);
        }
        for (Class<?> item : r.getClasses())
        {
            Plugin p = item.getAnnotation(Plugin.class);
            String pluginType = p.type();
            if (!pluginTypeMap.containsKey(pluginType)) {
                pluginTypeMap.putIfAbsent(pluginType, new ConcurrentHashMap<String, PluginType>());
            }
            Map<String, PluginType> map = pluginTypeMap.get(pluginType);
            String type = p.elementType().equals(Plugin.NULL) ? p.name() : p.elementType();
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
        logger.debug(sb.toString());
    }

    private static String createClassName(String type, String suffix) {
        return PREFIX + type + suffix;
    }

    private static ConcurrentMap<String, ConcurrentMap<String, PluginType>> decode(ClassLoader loader) {
        String resource = PATH + FILENAME;
        try {
            InputStream is = loader.getResourceAsStream(resource);
            BufferedInputStream bis = new BufferedInputStream(is);
            DataInputStream dis = new DataInputStream(bis);
            int count = dis.readInt();
            ConcurrentMap<String, ConcurrentMap<String, PluginType>> map =
                new ConcurrentHashMap<String, ConcurrentMap<String, PluginType>>(count);
            for (int j=0; j < count; ++j) {
                String type = dis.readUTF();
                int entries = dis.readInt();
                ConcurrentMap<String, PluginType> types = new ConcurrentHashMap<String, PluginType>(count);
                for (int i=0; i < entries; ++i) {
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
            return map;
        } catch (Exception ex) {
            logger.warn("Unable to preload plugins", ex);
            return null;
        }
    }

    private static void encode(ConcurrentMap<String, ConcurrentMap<String, PluginType>> map) {
        String fileName = "target/classes/" + PATH + FILENAME;
        try {
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

        /** Constructs an AnnotatedWith test for the specified annotation type. */
        public PluginTest(Class isA) {
            this.isA = isA;
        }

        /** Returns true if the type is annotated with the class provided to the constructor. */
        public boolean matches(Class type) {
            return type != null && type.isAnnotationPresent(Plugin.class) &&
                (isA == null || isA.isAssignableFrom(type));
        }

        @Override public String toString() {
            StringBuilder msg = new StringBuilder("annotated with @" + Plugin.class.getSimpleName());
            if (isA != null) {
                msg.append(" is assignable to " + isA.getSimpleName());
            }
            return msg.toString();
        }
    }

}
