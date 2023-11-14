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
package org.apache.logging.log4j.plugins.model;

import java.lang.ref.WeakReference;

import org.apache.logging.log4j.util.Cast;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.Strings;

/**
 * Plugin Descriptor. This is a memento object for Plugin annotations paired to their annotated classes.
 *
 * @param <T> The plug-in class, which can be any kind of class.
 * @see org.apache.logging.log4j.plugins.Plugin
 */
public class PluginType<T> {

    private final PluginEntry pluginEntry;
    private final Lazy<Class<T>> pluginClass;

    /**
     * Constructor.
     * @param pluginEntry The PluginEntry.
     * @param pluginClass The plugin Class.
     * @since 3.0.0
     */
    public PluginType(
            final PluginEntry pluginEntry, final Class<T> pluginClass) {
        this.pluginEntry = pluginEntry;
        this.pluginClass = Lazy.value(pluginClass);
    }

    /**
     * The Constructor.
     * @since 3.0.0
     * @param pluginEntry The PluginEntry.
     */
    public PluginType(final PluginEntry pluginEntry, final ClassLoader classLoader) {
        this.pluginEntry = pluginEntry;
        final WeakReference<ClassLoader> classLoaderRef = new WeakReference<>(classLoader);
        this.pluginClass = Lazy.lazy(() -> {
            final ClassLoader loader = classLoaderRef.get();
            if (loader == null) {
                throw new IllegalStateException("ClassLoader has been destroyed already");
            }
            final String className = pluginEntry.getClassName();
            try {
                return Cast.cast(loader.loadClass(className));
            } catch (final ClassNotFoundException e) {
                throw new IllegalStateException("No class named " + className +
                        " located for element " + pluginEntry.getName(), e);
            }
        });
    }

    public PluginEntry getPluginEntry() {
        return this.pluginEntry;
    }

    public Class<T> getPluginClass() {
        return pluginClass.value();
    }

    public String getElementType() {
        return pluginEntry.getElementType();
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
        return this.pluginEntry.isDeferChildren();
    }

    /**
     * Return the plugin namespace.
     * @return the Plugin namespace.
     * @since 2.1
     */
    public String getNamespace() {
        return this.pluginEntry.getNamespace();
    }

    public String getName() {
        return pluginEntry.getName();
    }

    public String getElementName() {
        return Strings.trimToOptional(getElementType()).orElseGet(this::getName);
    }

    @Override
    public String toString() {
        return "PluginType [pluginClass=" + pluginClass.toString() +
                ", key=" + pluginEntry.getKey() +
                ", elementType=" + pluginEntry.getElementType() +
                ", isObjectPrintable=" + pluginEntry.isPrintable() +
                ", isDeferChildren==" + pluginEntry.isDeferChildren() +
                ", namespace=" + pluginEntry.getNamespace() +
                "]";
    }
}
