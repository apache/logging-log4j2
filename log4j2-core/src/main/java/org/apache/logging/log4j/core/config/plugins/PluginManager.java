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

    static {
        packages.add("org.apache.logging.log4j");
    }

    public PluginManager(String type) {
        this.type = type;
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
        ResolverUtil.Test test = new PluginTest(type);
        for (String pkg : packages) {
            r.findInPackage(test, pkg);
        }
        for (Class<?> item : r.getClasses())
        {
            Plugin p = item.getAnnotation(Plugin.class);
            plugins.put(p.name(), new PluginType(item));
        }
    }


    /**
     * A Test that checks to see if each class is annotated with a specific annotation. If it
     * is, then the test returns true, otherwise false.
     */
    public static class PluginTest extends ResolverUtil.ClassTest {
        private String type;

        /** Constructs an AnnotatedWith test for the specified annotation type. */
        public PluginTest(String type) {
            this.type = type;
        }

        /** Returns true if the type is annotated with the class provided to the constructor. */
        public boolean matches(Class type) {
            return type != null && type.isAnnotationPresent(Plugin.class) &&
                this.type.equals(((Plugin)type.getAnnotation(Plugin.class)).type());
        }

        @Override public String toString() {
            return "annotated with @" + Plugin.class.getSimpleName();
        }
    }
}
