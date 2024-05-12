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
package org.apache.logging.log4j.spi;

import java.util.Map;
import org.apache.logging.log4j.ThreadContext;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Service provider interface to implement custom MDC behavior for {@link org.apache.logging.log4j.ThreadContext}.
 * <p>
 * Since 2.8, {@code ThreadContextMap} implementations that implement the {@link ReadOnlyThreadContextMap} interface
 * are accessible to applications via the {@link ThreadContext#getThreadContextMap()} method.
 * </p>
 */
@ProviderType
@NullMarked
public interface ThreadContextMap {

    /**
     * Clears the context.
     */
    void clear();

    /**
     * Determines if the key is in the context.
     * @param key The key to locate, not {@code null}.
     * @return True if the key is in the context, false otherwise.
     */
    boolean containsKey(final String key);

    /**
     * Gets the {@link String} value for the specified key if it exists or {@code null}.
     * <p>
     *     This method has no side effects.
     * </p>
     * @param key The key to locate, not {@code null}.
     * @return The value associated with the key or {@code null}.
     */
    @Nullable
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
    @Nullable
    Map<String, String> getImmutableMapOrNull();

    /**
     * Returns true if the Map is empty.
     * @return true if the Map is empty, false otherwise.
     */
    boolean isEmpty();

    /**
     * Sets the context value for the given key in the current thread's context map.
     * <p>
     *     If the current thread does not have a context map it is created as a side effect.
     * </p>
     * @param key The key to add, not {@code null}.
     * @param value The value to add, not {@code null}.
     */
    void put(final String key, final String value);

    /**
     * Puts all given context map entries into the current thread's context map.
     * <p>
     *     If the current thread does not have a context map it is created as a side effect.
     * </p>
     * @param map The map to add, not {@code null}.
     * @since 2.24.0
     */
    default void putAll(final Map<String, String> map) {
        map.forEach(this::put);
    }

    /**
     * Removes the context entry corresponding to the given key.
     * @param key The key to remove, not {@code null}.
     */
    void remove(final String key);

    /**
     * Removes all given context map keys from the current thread's context map.
     * <p>
     *     If the current thread does not have a context map it is created as a side effect.
     * </p>
     *
     * @param keys The keys, {@code null}.
     * @since 2.24.0
     */
    default void removeAll(final Iterable<String> keys) {
        keys.forEach(this::remove);
    }
}
