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
import org.apache.logging.log4j.util.LazyValue;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Plugin Descriptor. This is a memento object for Plugin annotations paired to their annotated classes.
 *
 * @param <T> The plug-in class, which can be any kind of class.
 * @see org.apache.logging.log4j.plugins.Plugin
 */
public class PluginType<T> {

    private final PluginEntry pluginEntry;
    private final Supplier<Class<T>> pluginClass;
    private final Supplier<Set<Class<?>>> implementedInterfaces;

    /**
     * Constructor.
     * @param pluginEntry The PluginEntry.
     * @param pluginClass The plugin Class.
     * @since 3.0.0
     */
    public PluginType(
            final PluginEntry pluginEntry, final Class<T> pluginClass) {
        this.pluginEntry = pluginEntry;
        this.pluginClass = () -> pluginClass;
        final var interfaces = Set.of(pluginClass.getInterfaces());
        this.implementedInterfaces = () -> interfaces;
    }

    /**
     * The Constructor.
     * @since 3.0.0
     * @param pluginEntry The PluginEntry.
     */
    public PluginType(final PluginEntry pluginEntry, final ClassLoader classLoader) {
        this.pluginEntry = pluginEntry;
        final LazyValue<Class<T>> classProvider = LazyValue.from(() -> {
            try {
                return TypeUtil.cast(classLoader.loadClass(pluginEntry.getClassName()));
            } catch (final ClassNotFoundException e) {
                throw new IllegalStateException("No class named " + pluginEntry.getClassName() +
                        " located for element " + pluginEntry.getName(), e);
            }
        });
        this.pluginClass = classProvider;
        final Class<?>[] interfaces = pluginEntry.getInterfaces();
        if (interfaces != null) {
            final var implementedInterfaces = Set.of(interfaces);
            this.implementedInterfaces = () -> implementedInterfaces;
        } else {
            this.implementedInterfaces = classProvider.map(clazz -> Set.of(clazz.getInterfaces()));
        }
    }

    public PluginEntry getPluginEntry() {
        return this.pluginEntry;
    }

    public Class<T> getPluginClass() {
        return pluginClass.get();
    }

    public Set<Class<?>> getImplementedInterfaces() {
        return implementedInterfaces.get();
    }

    public String getElementName() {
        return pluginEntry.getElementName();
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

    public String getName() {
        return pluginEntry.getName();
    }

    @Override
    public String toString() {
        return "PluginType [pluginClass=" + pluginClass.get() +
                ", key=" + pluginEntry.getKey() +
                ", elementName=" + pluginEntry.getElementName() +
                ", isObjectPrintable=" + pluginEntry.isPrintable() +
                ", isDeferChildren==" + pluginEntry.isDefer() +
                ", category=" + pluginEntry.getCategory() +
                "]";
    }
}
