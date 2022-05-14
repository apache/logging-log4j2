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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.plugins.PluginOrder;
import org.apache.logging.log4j.plugins.Singleton;
import org.apache.logging.log4j.status.StatusLogger;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * Plugin categories are mappings of plugin keys to plugin classes where plugin keys are lower-cased
 * versions of plugin names.
 */
@Singleton
public class PluginCategory implements Iterable<PluginType<?>> {
    private static final Logger LOGGER = StatusLogger.getLogger();

    private final String key;
    private final String name;
    private final Map<String, PluginType<?>> plugins = new LinkedHashMap<>();

    public PluginCategory(final String name) {
        this(name.toLowerCase(Locale.ROOT), name);
    }

    PluginCategory(final String key, final String name) {
        this.key = key;
        this.name = name;
    }

    /**
     * Returns the key corresponding to this plugin category. These keys are lowercase versions of the category name.
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the name of this plugin category.
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
     * Returns an unmodifiable set of plugin keys in this category.
     */
    public Set<String> getPluginKeys() {
        return Collections.unmodifiableSet(plugins.keySet());
    }

    /**
     * Returns an unmodifiable collection of plugin types in this category.
     */
    public Collection<PluginType<?>> getPluginTypes() {
        return Collections.unmodifiableCollection(plugins.values());
    }

    /**
     * Returns an unmodifiable map of plugin keys to plugin types in this category.
     */
    public Map<String, PluginType<?>> asMap() {
        return Collections.unmodifiableMap(plugins);
    }

    /**
     * Gets the plugin type for the provided plugin name (case-insensitive) if available or {@code null}.
     */
    public PluginType<?> get(final String name) {
        return plugins.get(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Puts all the provided plugin types into this category.
     */
    public void putAll(final Collection<PluginType<?>> pluginTypes) {
        pluginTypes.forEach(this::put);
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
        LOGGER.trace("Put PluginCategory[{}][{}] = {}", name, key, pluginType);
    }

    /**
     * Merges the provided plugin type into this category using the given key and returns the merged result.
     * Merging is done by preferring plugins according to {@link PluginOrder} where a conflict occurs with the
     * same key.
     */
    public PluginType<?> merge(final String key, final PluginType<?> pluginType) {
        final PluginType<?> result = plugins.merge(key, pluginType, (lhs, rhs) -> {
            final int compare = PluginOrder.COMPARATOR.compare(lhs.getPluginClass(), rhs.getPluginClass());
            LOGGER.debug("PluginCategory merge for key {} with comparison result {}", key, compare);
            return compare <= 0 ? lhs : rhs;
        });
        LOGGER.trace("Merged PluginCategory[{}][{}] = {}", name, key, result);
        return result;
    }

    public int mergeAll(final PluginCategory category) {
        if (category != null) {
            final AtomicInteger addedCount = new AtomicInteger();
            category.forEach((pluginKey, pluginType) -> {
                if (pluginType == merge(pluginKey, pluginType)) {
                    addedCount.incrementAndGet();
                }
            });
            return addedCount.get();
        }
        return 0;
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
