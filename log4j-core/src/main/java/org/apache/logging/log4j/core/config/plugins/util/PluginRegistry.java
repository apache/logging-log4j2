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

    public ConcurrentMap<String, T> getCategory(final String category) {
        if (category == null) {
            throw new IllegalArgumentException("Category name cannot be null.");
        }
        final String key = category.toLowerCase();
        categories.putIfAbsent(key, new ConcurrentHashMap<String, T>());
        return categories.get(key);
    }

    public int getCategoryCount() {
        return categories.size();
    }

    public boolean isEmpty() {
        return categories.isEmpty();
    }

    public void clear() {
        categories.clear();
    }

    public boolean hasCategory(final String key) {
        return categories.containsKey(key);
    }

    public Set<Map.Entry<String, ConcurrentMap<String, T>>> getCategories() {
        return categories.entrySet();
    }
}
