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

import org.apache.logging.log4j.core.helpers.Loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 */
public class PluginManager {


    private Map<String, PluginType> plugins = new HashMap<String, PluginType>();

    private static CopyOnWriteArrayList<String> packages = new CopyOnWriteArrayList<String>();

    private final String type;
    private final Class clazz;

    static {
        packages.add("org.apache.logging.log4j");
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
        return plugins.get(name);
    }

    public Map<String, PluginType> getPlugins() {
        return plugins;
    }


    public void collectPlugins() {
        ResolverUtil<?> r = new ResolverUtil();
        ClassLoader loader = Loader.getClassLoader();
        if (loader != null) {
            r.setClassLoader(loader);
        }
        ResolverUtil.Test test = new PluginTest(type, clazz);
        for (String pkg : packages) {
            r.findInPackage(test, pkg);
        }
        for (Class<?> item : r.getClasses())
        {
            Plugin p = item.getAnnotation(Plugin.class);
            String type = p.elementType().equals(Plugin.NULL) ? p.name() : p.elementType();
            plugins.put(p.name(), new PluginType(item, type, p.printObject()));
        }
    }


    /**
     * A Test that checks to see if each class is annotated with a specific annotation. If it
     * is, then the test returns true, otherwise false.
     */
    public static class PluginTest extends ResolverUtil.ClassTest {
        private final String type;
        private final Class isA;

        /** Constructs an AnnotatedWith test for the specified annotation type. */
        public PluginTest(String type) {
            this.type = type;
            this.isA = null;
        }


        /** Constructs an AnnotatedWith test for the specified annotation type. */
        public PluginTest(String type, Class isA) {
            this.type = type;
            this.isA = isA;
        }

        /** Returns true if the type is annotated with the class provided to the constructor. */
        public boolean matches(Class type) {
            return type != null && type.isAnnotationPresent(Plugin.class) &&
                this.type.equals(((Plugin)type.getAnnotation(Plugin.class)).type()) &&
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
