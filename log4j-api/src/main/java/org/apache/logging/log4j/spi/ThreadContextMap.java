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

/**
 * Service provider interface to implement custom MDC behavior for {@link org.apache.logging.log4j.ThreadContext}.
 * <p>
 * Since 2.8, {@code ThreadContextMap} implementations that implement the {@link ReadOnlyThreadContextMap} interface
 * are accessible to applications via the {@link ThreadContext#getThreadContextMap()} method.
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
}
