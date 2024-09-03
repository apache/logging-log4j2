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
package org.apache.logging.log4j.core.impl;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.util.Strings;
import org.apache.logging.log4j.util.TriConsumer;

/**
 * Provides a read-only {@code StringMap} view of a {@code Map<String, String>}.
 */
public class JdkMapAdapterStringMap implements StringMap {
    private static final long serialVersionUID = -7348247784983193612L;
    private static final String FROZEN = "Frozen collection cannot be modified";
    private static final Comparator<? super String> NULL_FIRST_COMPARATOR = (left, right) -> {
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    };
    // Cache of known unmodifiable map implementations.
    // It is a cache, no need to synchronise it between threads.
    private static Map<Class<?>, Void> UNMODIFIABLE_MAPS_CACHE = new WeakHashMap<>();

    private final Map<String, String> map;
    private boolean immutable = false;
    private transient String[] sortedKeys;

    public JdkMapAdapterStringMap() {
        this(new HashMap<>(), false);
    }

    /**
     * @deprecated for performance reasons since 2.23.
     *             Use {@link #JdkMapAdapterStringMap(Map, boolean)} instead.
     */
    @Deprecated
    public JdkMapAdapterStringMap(final Map<String, String> map) {
        this.map = Objects.requireNonNull(map, "map");
        // Known immutable implementations
        if (UNMODIFIABLE_MAPS_CACHE.containsKey(map.getClass())) {
            immutable = true;
        } else {
            // Check with a NO-OP replacement
            try {
                map.replace(Strings.EMPTY, Strings.EMPTY, Strings.EMPTY);
            } catch (final UnsupportedOperationException ignored) {
                final WeakHashMap<Class<?>, Void> cache = new WeakHashMap<>(UNMODIFIABLE_MAPS_CACHE);
                cache.put(map.getClass(), null);
                UNMODIFIABLE_MAPS_CACHE = cache;
                immutable = true;
            }
        }
    }

    /**
     * Constructs a new {@link StringMap}, based on a JDK map.
     * <p>
     *     The underlying map should not be modified after this call.
     * </p>
     * <p>
     *     If the {@link Map} implementation does not allow modifications, {@code frozen} should be set to {@code true}.
     * </p>
     * @param map a JDK map,
     * @param frozen if {@code true} this collection will be immutable.
     */
    public JdkMapAdapterStringMap(final Map<String, String> map, final boolean frozen) {
        this.map = Objects.requireNonNull(map, "map");
        this.immutable = frozen;
    }

    @Override
    public Map<String, String> toMap() {
        return new HashMap<>(map);
    }

    private void assertNotFrozen() {
        if (immutable) {
            throw new UnsupportedOperationException(FROZEN);
        }
    }

    @Override
    public boolean containsKey(final String key) {
        return map.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> void forEach(final BiConsumer<String, ? super V> action) {
        final String[] keys = getSortedKeys();
        for (int i = 0; i < keys.length; i++) {
            action.accept(keys[i], (V) map.get(keys[i]));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V, S> void forEach(final TriConsumer<String, ? super V, S> action, final S state) {
        final String[] keys = getSortedKeys();
        for (int i = 0; i < keys.length; i++) {
            action.accept(keys[i], (V) map.get(keys[i]), state);
        }
    }

    private String[] getSortedKeys() {
        if (sortedKeys == null) {
            sortedKeys = map.keySet().toArray(Strings.EMPTY_ARRAY);
            Arrays.sort(sortedKeys, NULL_FIRST_COMPARATOR);
        }
        return sortedKeys;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> V getValue(final String key) {
        return (V) map.get(key);
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public void clear() {
        if (map.isEmpty()) {
            return;
        }
        assertNotFrozen();
        map.clear();
        sortedKeys = null;
    }

    @Override
    public void freeze() {
        immutable = true;
    }

    @Override
    public boolean isFrozen() {
        return immutable;
    }

    @Override
    public void putAll(final ReadOnlyStringMap source) {
        assertNotFrozen();
        source.forEach(PUT_ALL, map);
        sortedKeys = null;
    }

    private static TriConsumer<String, String, Map<String, String>> PUT_ALL =
            (key, value, stringStringMap) -> stringStringMap.put(key, value);

    @Override
    public void putValue(final String key, final Object value) {
        assertNotFrozen();
        map.put(key, value == null ? null : String.valueOf(value));
        sortedKeys = null;
    }

    @Override
    public void remove(final String key) {
        if (!map.containsKey(key)) {
            return;
        }
        assertNotFrozen();
        map.remove(key);
        sortedKeys = null;
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder(map.size() * 13);
        result.append('{');
        final String[] keys = getSortedKeys();
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) {
                result.append(", ");
            }
            result.append(keys[i]).append('=').append(map.get(keys[i]));
        }
        result.append('}');
        return result.toString();
    }

    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof JdkMapAdapterStringMap)) {
            return false;
        }
        final JdkMapAdapterStringMap other = (JdkMapAdapterStringMap) object;
        return map.equals(other.map) && immutable == other.immutable;
    }

    @Override
    public int hashCode() {
        return map.hashCode() + (immutable ? 31 : 0);
    }
}
