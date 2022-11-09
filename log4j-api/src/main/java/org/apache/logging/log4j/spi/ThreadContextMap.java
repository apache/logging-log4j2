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

import java.util.Map;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.util.Cast;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;

/**
 * Service provider interface to implement custom MDC behavior for {@link org.apache.logging.log4j.ThreadContext}.
 * <p>
 * Since 2.8, {@code ThreadContextMap} implementations that implement the {@link ReadOnlyThreadContextMap} interface
 * are accessible to applications via the {@link ThreadContext#getThreadContextMap()} method.
 * </p>
 * <p>
 * Since 3.0.0, {@code ThreadContextMap} combines all its extension interfaces into default methods on this interface.
 * </p>
 */
public interface ThreadContextMap {

    /**
     * Clears the context.
     */
    void clear();

    /**
     * Determines if the key is in the context.
     * @param key The key to locate.
     * @return True if the key is in the context, false otherwise.
     */
    boolean containsKey(final String key);

    /**
     * Gets the context identified by the <code>key</code> parameter.
     *
     * <p>This method has no side effects.</p>
     * @param key The key to locate.
     * @return The value associated with the key or null.
     */
    String get(final String key);

    /**
     * Gets a non-{@code null} mutable copy of current thread's context Map.
     * @return a mutable copy of the context.
     */
    Map<String, String> getCopy();

    /**
     * Returns an immutable view on the context Map or {@code null} if the context map is empty.
     * @return an immutable context Map or {@code null}.
     */
    Map<String, String> getImmutableMapOrNull();

    /**
     * Returns true if the Map is empty.
     * @return true if the Map is empty, false otherwise.
     */
    boolean isEmpty();

    /**
     * Puts a context value (the <code>o</code> parameter) as identified
     * with the <code>key</code> parameter into the current thread's
     * context map.
     *
     * <p>If the current thread does not have a context map it is
     * created as a side effect.</p>
     * @param key The key name.
     * @param value The key value.
     */
    void put(final String key, final String value);

    /**
     * Removes the context identified by the <code>key</code>
     * parameter.
     * @param key The key to remove.
     */
    void remove(final String key);

    /**
     * Puts all given context map entries into the current thread's
     * context map.
     *
     * <p>If the current thread does not have a context map it is
     * created as a side effect.</p>
     * @param map The map.
     * @since 3.0.0
     */
    default void putAll(Map<String, String> map) {
        map.forEach(this::put);
    }

    /**
     * Returns the context data for reading. Note that regardless of whether the returned context data has been
     * {@linkplain StringMap#freeze() frozen} (made read-only) or not, callers should not attempt to modify
     * the returned data structure.
     *
     * @return the {@code StringMap}
     * @since 3.0.0
     */
    default StringMap getReadOnlyContextData() {
        if (this instanceof ReadOnlyThreadContextMap) {
            return ((ReadOnlyThreadContextMap) this).getReadOnlyContextData();
        }
        final Map<String, String> copy = getCopy();
        StringMap map = new SortedArrayStringMap(copy.size());
        copy.forEach(map::putValue);
        map.freeze();
        return map;
    }

    /**
     * Removes all given context map keys from the current thread's context map.
     *
     * <p>If the current thread does not have a context map it is
     * created as a side effect.</p>

     * @param keys The keys.
     * @since 3.0.0
     */
    default void removeAll(Iterable<String> keys) {
        keys.forEach(this::remove);
    }

    /**
     * Returns the Object value for the specified key, or {@code null} if the specified key does not exist in this
     * collection.
     *
     * @param key the key whose value to return
     * @param <V> The type of the returned value.
     * @return the value for the specified key or {@code null}
     * @since 3.0.0
     */
    default <V> V getValue(String key) {
        return Cast.cast(get(key));
    }

    /**
     * Puts the specified key-value pair into the collection.
     *
     * @param key the key to add or remove. Keys may be {@code null}.
     * @param <V> The type of the stored and returned value.
     * @param value the value to add. Values may be {@code null}.
     * @since 3.0.0
     */
    default <V> void putValue(String key, V value) {
        put(key, value != null ? value.toString() : null);
    }

    /**
     * Puts all given key-value pairs into the collection.
     *
     * @param values the map of key-value pairs to add
     * @param <V> The type of the value being added.
     * @since 3.0.0
     */
    default <V> void putAllValues(Map<String, V> values) {
        values.forEach(this::putValue);
    }
}
