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
package org.apache.logging.log4j.core.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.spi.ObjectThreadContextMap;
import org.apache.logging.log4j.spi.ReadOnlyThreadContextMap;
import org.apache.logging.log4j.spi.ThreadContextMap2;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.jspecify.annotations.Nullable;

/**
 * Commons base class for {@link StringMap}-based implementations of {@code ThreadContextMap}.
 */
public abstract class AbstractSortedArrayThreadContextMap implements ReadOnlyThreadContextMap, ObjectThreadContextMap {

    /**
     * Property name ({@value} ) for selecting {@code InheritableThreadLocal} (value "true") or plain
     * {@code ThreadLocal} (value is not "true") in the implementation.
     */
    public static final String INHERITABLE_MAP = "isThreadContextMapInheritable";

    /**
     * The default initial capacity.
     */
    protected static final int DEFAULT_INITIAL_CAPACITY = 16;

    /**
     * System property name that can be used to control the data structure's initial capacity.
     */
    protected static final String PROPERTY_NAME_INITIAL_CAPACITY = "log4j2.ThreadContext.initial.capacity";

    private static final StringMap EMPTY_CONTEXT_DATA = new SortedArrayStringMap(1);

    static {
        EMPTY_CONTEXT_DATA.freeze();
    }

    private final int initialCapacity;
    protected final ThreadLocal<@Nullable StringMap> localMap;

    protected AbstractSortedArrayThreadContextMap(final PropertiesUtil properties) {
        initialCapacity = properties.getIntegerProperty(PROPERTY_NAME_INITIAL_CAPACITY, DEFAULT_INITIAL_CAPACITY);
        localMap = properties.getBooleanProperty(INHERITABLE_MAP)
                ? new InheritableThreadLocal<StringMap>() {
                    @Override
                    protected @Nullable StringMap childValue(final @Nullable StringMap parentValue) {
                        return copyStringMap(parentValue);
                    }
                }
                : new ThreadLocal<>();
    }

    /**
     * Creates a copy of {@link StringMap} used to back this thread context map.
     *
     * @param value the context data of the current thread.
     * @return the context data for a new thread.
     */
    protected abstract @Nullable StringMap copyStringMap(@Nullable StringMap value);

    /**
     * Returns an implementation of the {@code StringMap} used to back this thread context map.
     * <p>
     * Subclasses may override.
     * </p>
     * @return an implementation of the {@code StringMap} used to back this thread context map
     */
    protected StringMap createStringMap() {
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

    protected @Nullable StringMap getMutableLocalMapOrNull() {
        return localMap.get();
    }

    protected StringMap getMutableLocalMap() {
        StringMap map = getMutableLocalMapOrNull();
        if (map == null) {
            map = createStringMap();
            localMap.set(map);
        }
        return map;
    }

    @Override
    public final void put(final String key, final String value) {
        putValue(key, value);
    }

    @Override
    public void putValue(final String key, final Object value) {
        getMutableLocalMap().putValue(key, value);
    }

    @Override
    public final void putAll(final Map<String, String> values) {
        putAllValues(values);
    }

    @Override
    public <V> void putAllValues(final Map<String, V> values) {
        if (values.isEmpty()) {
            return;
        }
        final StringMap map = getMutableLocalMap();
        values.forEach(map::putValue);
    }

    @Override
    public final @Nullable String get(final String key) {
        final Object value = getValue(key);
        return value != null ? value.toString() : null;
    }

    @Override
    public final <V> @Nullable V getValue(final String key) {
        final StringMap map = localMap.get();
        return map == null ? null : (V) map.getValue(key);
    }

    @Override
    public void remove(final String key) {
        final StringMap map = getMutableLocalMapOrNull();
        if (map != null) {
            map.remove(key);
        }
    }

    @Override
    public void removeAll(final Iterable<String> keys) {
        final StringMap map = getMutableLocalMapOrNull();
        if (map != null) {
            keys.forEach(map::remove);
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

    @Override
    public StringMap getReadOnlyContextData() {
        final StringMap map = localMap.get();
        return map == null ? EMPTY_CONTEXT_DATA : map;
    }

    @Override
    public @Nullable Map<String, String> getImmutableMapOrNull() {
        final StringMap map = localMap.get();
        return map == null ? null : Collections.unmodifiableMap(map.toMap());
    }

    @Override
    public boolean isEmpty() {
        final StringMap map = localMap.get();
        return map == null || map.isEmpty();
    }

    @Override
    public String toString() {
        final StringMap map = localMap.get();
        return map == null ? "{}" : map.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(localMap.get());
    }

    @Override
    public boolean equals(final @Nullable Object obj) {
        return obj == this
                || obj instanceof ObjectThreadContextMap
                        && Objects.equals(getReadOnlyContextData(), ((ThreadContextMap2) obj).getReadOnlyContextData());
    }
}
