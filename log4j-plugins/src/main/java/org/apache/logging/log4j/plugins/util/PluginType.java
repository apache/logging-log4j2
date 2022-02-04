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

import org.apache.logging.log4j.plugins.processor.PluginEntry;

/**
 * Plugin Descriptor. This is a memento object for Plugin annotations paired to their annotated classes.
 *
 * @param <T> The plug-in class, which can be any kind of class.
 * @see org.apache.logging.log4j.plugins.Plugin
 */
public class PluginType<T> {

    private final PluginEntry pluginEntry;
    private volatile Class<T> pluginClass;
    private final ClassLoader classLoader;
    private final String elementName;

    /**
     * Constructor.
     * @param pluginEntry The PluginEntry.
     * @param pluginClass The plugin Class.
     * @param elementName The name of the element.
     * @since 2.1
     */
    public PluginType(final PluginEntry pluginEntry, final Class<T> pluginClass, final String elementName) {
        this.pluginEntry = pluginEntry;
        this.pluginClass = pluginClass;
        this.elementName = elementName;
        this.classLoader = null;
    }

    /**
     * The Constructor.
     * @since 3.0
     * @param pluginEntry The PluginEntry.
     * @param classLoader The ClassLoader to use to load the Plugin.
     */
    public PluginType(final PluginEntry pluginEntry, final ClassLoader classLoader) {
        this.pluginEntry = pluginEntry;
        this.classLoader = classLoader;
        this.elementName = pluginEntry.getName();
        this.pluginClass = null;
    }


    public PluginEntry getPluginEntry() {
        return this.pluginEntry;
    }

    @SuppressWarnings("unchecked")
    public Class<T> getPluginClass() {
        if (pluginClass == null) {
            try {
                pluginClass = (Class<T>) this.classLoader.loadClass(pluginEntry.getClassName());
            } catch (ClassNotFoundException | LinkageError ex) {
                throw new IllegalStateException("No class named " + pluginEntry.getClassName() +
                        " located for element " + elementName, ex);
            }
        }
        return this.pluginClass;
    }

    public String getElementName() {
        return this.elementName;
    }

    /**
     * Return The plugin's key.
     * @return The plugin key.
     * @since 2.1
     */
    public String getKey() {
        return this.pluginEntry.getKey();
    }

    public boolean isObjectPrintable() {
        return this.pluginEntry.isPrintable();
    }

    public boolean isDeferChildren() {
        return this.pluginEntry.isDefer();
    }

    /**
     * Return the plugin category.
     * @return the Plugin category.
     * @since 2.1
     */
    public String getCategory() {
        return this.pluginEntry.getCategory();
    }

    @Override
    public String toString() {
        return "PluginType [pluginClass=" + pluginClass +
                ", key=" + pluginEntry.getKey() +
                ", elementName=" + pluginEntry.getName() +
                ", isObjectPrintable=" + pluginEntry.isPrintable() +
                ", isDeferChildren==" + pluginEntry.isDefer() +
                ", category=" + pluginEntry.getCategory() +
                "]";
    }
}
