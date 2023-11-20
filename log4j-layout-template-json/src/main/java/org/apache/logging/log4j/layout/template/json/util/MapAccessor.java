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
package org.apache.logging.log4j.layout.template.json.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MapAccessor {

    private final Map<String, Object> map;

    public MapAccessor(final Map<String, Object> map) {
        this.map = Objects.requireNonNull(map, "map");
    }

    public String getString(final String key) {
        final String[] path = {key};
        return getObject(path, String.class);
    }

    public String getString(final String[] path) {
        return getObject(path, String.class);
    }

    public boolean getBoolean(final String key, final boolean defaultValue) {
        final String[] path = {key};
        return getBoolean(path, defaultValue);
    }

    public boolean getBoolean(final String[] path, final boolean defaultValue) {
        final Boolean value = getObject(path, Boolean.class);
        return value == null ? defaultValue : value;
    }

    public Boolean getBoolean(final String key) {
        final String[] path = {key};
        return getObject(path, Boolean.class);
    }

    public Boolean getBoolean(final String[] path) {
        return getObject(path, Boolean.class);
    }

    public Integer getInteger(final String key) {
        final String[] path = {key};
        return getInteger(path);
    }

    public Integer getInteger(final String[] path) {
        return getObject(path, Integer.class);
    }

    public boolean exists(final String key) {
        final String[] path = {key};
        return exists(path);
    }

    public boolean exists(final String[] path) {
        final Object value = getObject(path);
        return value != null;
    }

    public <E> List<E> getList(final String key, final Class<E> clazz) {
        final String[] path = {key};
        return getList(path, clazz);
    }

    public <E> List<E> getList(final String[] path, final Class<E> clazz) {

        // Access the object.
        final Object value = getObject(path);
        if (value == null) {
            return null;
        }

        // Check the type.
        if (!(value instanceof List)) {
            final String message = String.format(
                    "was expecting a List<%s> at path %s: %s (of type %s)",
                    clazz, Arrays.asList(path), value, value.getClass().getCanonicalName());
            throw new IllegalArgumentException(message);
        }

        // Check the element types.
        @SuppressWarnings("unchecked")
        final List<Object> items = (List<Object>) value;
        for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
            final Object item = items.get(itemIndex);
            if (!clazz.isInstance(item)) {
                final String message = String.format(
                        "was expecting a List<%s> item at path %s and index %d: %s (of type %s)",
                        clazz,
                        Arrays.asList(path),
                        itemIndex,
                        item,
                        item != null ? item.getClass().getCanonicalName() : null);
                throw new IllegalArgumentException(message);
            }
        }

        // Return the typed list.
        @SuppressWarnings("unchecked")
        final List<E> typedItems = (List<E>) items;
        return typedItems;
    }

    public Object getObject(final String key) {
        final String[] path = {key};
        return getObject(path, Object.class);
    }

    public <T> T getObject(final String key, final Class<T> clazz) {
        final String[] path = {key};
        return getObject(path, clazz);
    }

    public Object getObject(final String[] path) {
        return getObject(path, Object.class);
    }

    public <T> T getObject(final String[] path, final Class<T> clazz) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(clazz, "clazz");
        if (path.length == 0) {
            throw new IllegalArgumentException("empty path");
        }
        Object parent = map;
        for (final String key : path) {
            if (!(parent instanceof Map)) {
                return null;
            }
            @SuppressWarnings("unchecked")
            final Map<String, Object> parentMap = (Map<String, Object>) parent;
            parent = parentMap.get(key);
        }
        if (parent != null && !clazz.isInstance(parent)) {
            final String message = String.format(
                    "was expecting %s at path %s: %s (of type %s)",
                    clazz.getSimpleName(),
                    Arrays.asList(path),
                    parent,
                    parent.getClass().getCanonicalName());
            throw new IllegalArgumentException(message);
        }
        @SuppressWarnings("unchecked")
        final T typedValue = (T) parent;
        return typedValue;
    }

    @Override
    public boolean equals(final Object instance) {
        if (this == instance) {
            return true;
        }
        if (!(instance instanceof MapAccessor)) {
            return false;
        }
        final MapAccessor that = (MapAccessor) instance;
        return map.equals(that.map);
    }

    @Override
    public int hashCode() {
        return 31 + Objects.hashCode(map);
    }

    @Override
    public String toString() {
        return map.toString();
    }
}
