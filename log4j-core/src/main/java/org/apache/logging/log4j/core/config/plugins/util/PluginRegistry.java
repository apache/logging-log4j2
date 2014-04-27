/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.logging.log4j.core.config.plugins.util;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Registry for PluginType maps partitioned by category names.
 *
 * @param <T> plugin information object such as PluginType or PluginEntry.
 */
public class PluginRegistry<T extends Serializable> {
    private final ConcurrentMap<String, ConcurrentMap<String, T>> categories =
        new ConcurrentHashMap<String, ConcurrentMap<String, T>>();

    /**
     * Gets or creates a plugin category if not already available. Category names are case-insensitive. The
     * ConcurrentMap that is returned should also be treated as a case-insensitive plugin map where names should be
     * converted to lowercase before retrieval or storage.
     *
     * @param category the plugin category to look up or create.
     * @return the plugin map for the given category name.
     * @throws IllegalArgumentException if the argument is {@code null}
     */
    public ConcurrentMap<String, T> getCategory(final String category) {
        if (category == null) {
            throw new IllegalArgumentException("Category name cannot be null.");
        }
        final String key = category.toLowerCase();
        categories.putIfAbsent(key, new ConcurrentHashMap<String, T>());
        return categories.get(key);
    }

    /**
     * Returns the number of plugin categories currently available. This is primarily useful for serialization.
     *
     * @return the number of plugin categories.
     */
    public int getCategoryCount() {
        return categories.size();
    }

    /**
     * Indicates whether or not any plugin categories have been registered. Note that this does not necessarily
     * indicate if any plugins are registered as categories may be empty.
     *
     * @return {@code true} if there any categories registered.
     */
    public boolean isEmpty() {
        return categories.isEmpty();
    }

    /**
     * Resets the registry to an empty state.
     */
    public void clear() {
        categories.clear();
    }

    /**
     * Indicates whether or not the given category name is registered and has plugins in that category.
     *
     * @param category the plugin category name to check.
     * @return {@code true} if the category exists and has plugins registered.
     * @throws IllegalArgumentException if the argument is {@code null}
     */
    public boolean hasCategory(final String category) {
        if (category == null) {
            throw new IllegalArgumentException("Category name cannot be null.");
        }
        final String key = category.toLowerCase();
        return categories.containsKey(key) && !categories.get(key).isEmpty();
    }

    /**
     * Gets an entry set for iterating over the registered plugin categories.
     *
     * @return an entry set of the registered plugin categories.
     */
    public Set<Map.Entry<String, ConcurrentMap<String, T>>> getCategories() {
        return categories.entrySet();
    }
}
