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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Bundles plugins by category from a plugin source.
 */
public class PluginBundle {
    private final Map<String, PluginCategory> categories = new LinkedHashMap<>();

    public int size() {
        return categories.size();
    }

    public boolean isEmpty() {
        return categories.isEmpty();
    }

    public void put(final String category, final List<PluginType<?>> pluginTypes) {
        final var pluginCategory = new PluginCategory(category);
        pluginCategory.putAll(pluginTypes);
        categories.put(pluginCategory.getKey(), pluginCategory);
    }

    public void put(final PluginCategory category) {
        categories.put(category.getKey(), category);
    }

    public int merge(final PluginCategory category) {
        final PluginCategory existingCategory = getOrCreate(category.getKey());
        final AtomicInteger addedCount = new AtomicInteger();
        category.forEach((key, plugin) -> {
            final var merged = existingCategory.merge(key, plugin);
            if (merged == plugin) {
                addedCount.incrementAndGet();
            }
        });
        return addedCount.get();
    }

    public void add(final PluginType<?> pluginType) {
        getOrCreate(pluginType.getCategory()).put(pluginType);
    }

    public void addAll(final String category, final Collection<PluginType<?>> pluginTypes) {
        getOrCreate(category).putAll(pluginTypes);
    }

    public PluginCategory get(final String category) {
        return categories.get(category.toLowerCase(Locale.ROOT));
    }

    public PluginCategory getOrCreate(final String category) {
        return categories.computeIfAbsent(category.toLowerCase(Locale.ROOT), key -> new PluginCategory(key, category));
    }

    public void forEach(final Consumer<? super PluginCategory> consumer) {
        categories.values().forEach(consumer);
    }

    public void forEach(final BiConsumer<? super String, ? super PluginCategory> biConsumer) {
        categories.forEach(biConsumer);
    }
}
