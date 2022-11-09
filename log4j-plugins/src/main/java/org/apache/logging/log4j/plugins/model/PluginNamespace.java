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
package org.apache.logging.log4j.plugins.model;

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.plugins.Ordered;
import org.apache.logging.log4j.plugins.Singleton;
import org.apache.logging.log4j.plugins.util.OrderedComparator;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Plugin namespaces are mappings of plugin keys to plugin classes where plugin keys are lower-cased
 * versions of plugin names.
 */
@Singleton
public class PluginNamespace extends AbstractCollection<PluginType<?>> {
    private static final Logger LOGGER = StatusLogger.getLogger();

    private final String key;
    private final String name;
    private final Map<String, PluginType<?>> plugins = new LinkedHashMap<>();

    public PluginNamespace(final String name) {
        this(name.toLowerCase(Locale.ROOT), name);
    }

    public PluginNamespace(final String key, final String name) {
        this.key = key;
        this.name = name;
    }

    /**
     * Returns the key corresponding to this plugin namespace. These keys are lowercase versions of the namespace name.
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the name of this plugin namespace.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the number of plugins in this category.
     */
    public int size() {
        return plugins.size();
    }

    /**
     * Indicates if this category has no plugins.
     */
    public boolean isEmpty() {
        return plugins.isEmpty();
    }

    /**
     * Gets the plugin type for the provided plugin name (case-insensitive) if available or {@code null}.
     */
    public PluginType<?> get(final String name) {
        return plugins.get(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Puts the provided plugin type into this category.
     */
    public void put(final PluginType<?> pluginType) {
        put(pluginType.getKey(), pluginType);
    }

    /**
     * Puts the provided plugin type into this category using the given key.
     */
    public void put(final String key, final PluginType<?> pluginType) {
        plugins.put(key, pluginType);
        LOGGER.trace("Put PluginNamespace[{}][{}] = {}", name, key, pluginType);
    }

    @Override
    public boolean add(final PluginType<?> pluginType) {
        return pluginType == merge(pluginType.getKey(), pluginType);
    }

    /**
     * Merges the provided plugin type into this namespace using the given key and returns the merged result.
     * Merging is done by preferring plugins according to {@link Ordered} where a conflict occurs with the
     * same key.
     */
    public PluginType<?> merge(final String key, final PluginType<?> pluginType) {
        final PluginType<?> result = plugins.merge(key, pluginType, (lhs, rhs) -> {
            final int compare = OrderedComparator.INSTANCE.compare(lhs.getPluginClass(), rhs.getPluginClass());
            LOGGER.debug("PluginNamespace merge for key {} with comparison result {}", key, compare);
            return compare <= 0 ? lhs : rhs;
        });
        LOGGER.trace("Merged PluginNamespace[{}][{}] = {}", name, key, result);
        return result;
    }

    public void mergeAll(final PluginNamespace namespace) {
        if (namespace != null) {
            namespace.forEach(this::merge);
        }
    }

    @Override
    public Iterator<PluginType<?>> iterator() {
        return plugins.values().iterator();
    }

    /**
     * Performs the given action on all the plugin types in this category.
     */
    public void forEach(final BiConsumer<? super String, ? super PluginType<?>> biConsumer) {
        plugins.forEach(biConsumer);
    }

}
