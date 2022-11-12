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
package org.apache.logging.log4j.spi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;

import static org.apache.logging.log4j.spi.LoggingSystem.THREAD_CONTEXT_DEFAULT_INITIAL_CAPACITY;
import static org.apache.logging.log4j.spi.LoggingSystemProperties.*;

/**
 * {@code SortedArrayStringMap}-based implementation of the {@code ThreadContextMap} interface that attempts not to
 * create temporary objects. Adding and removing key-value pairs will not create temporary objects.
 * <p>
 * This implementation does <em>not</em> make a copy of its contents on every operation, so this data structure cannot
 * be passed to log events. Instead, client code needs to copy the contents when interacting with another thread.
 * </p>
 * @since 2.7
 */
class GarbageFreeSortedArrayThreadContextMap implements ReadOnlyThreadContextMap, ThreadContextMap  {

    protected final ThreadLocal<StringMap> localMap;
    protected final int initialCapacity;

    public GarbageFreeSortedArrayThreadContextMap() {
        this(
                PropertiesUtil.getProperties().getBooleanProperty(THREAD_CONTEXT_MAP_INHERITABLE),
                PropertiesUtil.getProperties().getIntegerProperty(THREAD_CONTEXT_INITIAL_CAPACITY, THREAD_CONTEXT_DEFAULT_INITIAL_CAPACITY)
        );
    }

    GarbageFreeSortedArrayThreadContextMap(final boolean inheritableMap, final  int initialCapacity) {
        this.localMap = createThreadLocalMap(inheritableMap);
        this.initialCapacity = initialCapacity;
    }

    // LOG4J2-479: by default, use a plain ThreadLocal, only use InheritableThreadLocal if configured.
    // (This method is package protected for JUnit tests.)
    private ThreadLocal<StringMap> createThreadLocalMap(final boolean inheritableMap) {
        if (inheritableMap) {
            return new InheritableThreadLocal<>() {
                @Override
                protected StringMap childValue(final StringMap parentValue) {
                    return parentValue != null ? createStringMap(parentValue) : null;
                }
            };
        }
        // if not inheritable, return plain ThreadLocal with null as initial value
        return new ThreadLocal<>();
    }

    /**
     * Returns an implementation of the {@code StringMap} used to back this thread context map.
     * <p>
     * Subclasses may override.
     * </p>
     * @return an implementation of the {@code StringMap} used to back this thread context map
     */
    protected StringMap createStringMap() {
        return createStringMap(initialCapacity);
    }

    /**
     * Returns an implementation of the {@code StringMap} used to back this thread context map.
     * <p>
     * Subclasses may override.
     * </p>
     * @return an implementation of the {@code StringMap} used to back this thread context map
     * @param initialCapacity initial capacity of the StringMap
     */
    protected StringMap createStringMap(final int initialCapacity) {
        return new SortedArrayStringMap(initialCapacity);
    }

    /**
     * Returns an implementation of the {@code StringMap} used to back this thread context map, pre-populated
     * with the contents of the specified context data.
     * <p>
     * Subclasses may override.
     * </p>
     * @param original the key-value pairs to initialize the returned context data with
     * @return an implementation of the {@code StringMap} used to back this thread context map
     */
    protected StringMap createStringMap(final ReadOnlyStringMap original) {
        return new SortedArrayStringMap(original);
    }

    private StringMap getThreadLocalMap() {
        StringMap map = localMap.get();
        if (map == null) {
            map = createStringMap();
            localMap.set(map);
        }
        return map;
    }

    @Override
    public void put(final String key, final String value) {
        getThreadLocalMap().putValue(key, value);
    }

    @Override
    public void putValue(final String key, final Object value) {
        getThreadLocalMap().putValue(key, value);
    }

    @Override
    public void putAll(final Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        final StringMap map = getThreadLocalMap();
        for (final Map.Entry<String, String> entry : values.entrySet()) {
            map.putValue(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public <V> void putAllValues(final Map<String, V> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        final StringMap map = getThreadLocalMap();
        for (final Map.Entry<String, V> entry : values.entrySet()) {
            map.putValue(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public String get(final String key) {
        return getValue(key);
    }

    @Override
    public <V> V getValue(final String key) {
        final StringMap map = localMap.get();
        return map == null ? null : map.getValue(key);
    }

    @Override
    public void remove(final String key) {
        final StringMap map = localMap.get();
        if (map != null) {
            map.remove(key);
        }
    }

    @Override
    public void removeAll(final Iterable<String> keys) {
        final StringMap map = localMap.get();
        if (map != null) {
            for (final String key : keys) {
                map.remove(key);
            }
        }
    }

    @Override
    public void clear() {
        final StringMap map = localMap.get();
        if (map != null) {
            map.clear();
        }
    }

    @Override
    public boolean containsKey(final String key) {
        final StringMap map = localMap.get();
        return map != null && map.containsKey(key);
    }

    @Override
    public Map<String, String> getCopy() {
        final StringMap map = localMap.get();
        return map == null ? new HashMap<>() : map.toMap();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StringMap getReadOnlyContextData() {
        StringMap map = localMap.get();
        if (map == null) {
            map = createStringMap();
            localMap.set(map);
        }
        return map;
    }

    @Override
    public Map<String, String> getImmutableMapOrNull() {
        final StringMap map = localMap.get();
        return map == null ? null : Collections.unmodifiableMap(map.toMap());
    }

    @Override
    public boolean isEmpty() {
        final StringMap map = localMap.get();
        return map == null || map.size() == 0;
    }

    @Override
    public String toString() {
        final StringMap map = localMap.get();
        return map == null ? "{}" : map.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        final StringMap map = this.localMap.get();
        result = prime * result + ((map == null) ? 0 : map.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ThreadContextMap)) {
            return false;
        }
        final ThreadContextMap other = (ThreadContextMap) obj;
        final Map<String, String> map = this.getImmutableMapOrNull();
        final Map<String, String> otherMap = other.getImmutableMapOrNull();
        return Objects.equals(map, otherMap);
    }
}
