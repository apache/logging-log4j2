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

/**
 * The actual ThreadContext Map. A new ThreadContext Map is created each time it is updated and the Map stored
 * is always immutable. This means the Map can be passed to other threads without concern that it will be updated.
 * Since it is expected that the Map will be passed to many more log events than the number of keys it contains
 * the performance should be much better than if the Map was copied for each event.
 */
public class DefaultThreadContextMap implements ThreadContextMap {

    private final boolean useMap;

    private final ThreadLocal<Map<String, String>> localMap =
        new InheritableThreadLocal<Map<String, String>>() {
            @Override
            protected Map<String, String> childValue(final Map<String, String> parentValue) {
                return parentValue == null || !useMap ? null :
                    Collections.unmodifiableMap(new HashMap<String, String>(parentValue));
            }
        };

    public DefaultThreadContextMap(final boolean useMap) {
        this.useMap = useMap;
    }

    /**
     * Put a context value (the <code>o</code> parameter) as identified
     * with the <code>key</code> parameter into the current thread's
     * context map.
     * <p/>
     * <p>If the current thread does not have a context map it is
     * created as a side effect.
     * @param key The key name.
     * @param value The key value.
     */
    @Override
    public void put(final String key, final String value) {
        if (!useMap) {
            return;
        }
        Map<String, String> map = localMap.get();
        map = map == null ? new HashMap<String, String>() : new HashMap<String, String>(map);
        map.put(key, value);
        localMap.set(Collections.unmodifiableMap(map));
    }

    /**
     * Get the context identified by the <code>key</code> parameter.
     * <p/>
     * <p>This method has no side effects.
     * @param key The key to locate.
     * @return The value associated with the key or null.
     */
    @Override
    public String get(final String key) {
        final Map<String, String> map = localMap.get();
        return map == null ? null : map.get(key);
    }

    /**
     * Remove the the context identified by the <code>key</code>
     * parameter.
     * @param key The key to remove.
     */
    @Override
    public void remove(final String key) {
        final Map<String, String> map = localMap.get();
        if (map != null) {
            final Map<String, String> copy = new HashMap<String, String>(map);
            copy.remove(key);
            localMap.set(Collections.unmodifiableMap(copy));
        }
    }

    /**
     * Clear the context.
     */
    @Override
    public void clear() {
        localMap.remove();
    }

    /**
     * Determine if the key is in the context.
     * @param key The key to locate.
     * @return True if the key is in the context, false otherwise.
     */
    @Override
    public boolean containsKey(final String key) {
        final Map<String, String> map = localMap.get();
        return map != null && map.containsKey(key);
    }

    /**
     * Returns a non-{@code null} mutable copy of the ThreadContext Map.
     * @return a non-{@code null} mutable copy of the context.
     */
    @Override
    public Map<String, String> getCopy() {
        final Map<String, String> map = localMap.get();
        return map == null ? new HashMap<String, String>() : new HashMap<String, String>(map);
    }

    /**
     * Returns either {@code null} or an immutable view of the context Map.
     * @return the Context Map.
     */
    @Override
    public Map<String, String> getImmutableMapOrNull() {
        return localMap.get();
    }

    /**
     * Returns true if the Map is empty.
     * @return true if the Map is empty, false otherwise.
     */
    @Override
    public boolean isEmpty() {
        final Map<String, String> map = localMap.get();
        return map == null || map.size() == 0;
    }
}
