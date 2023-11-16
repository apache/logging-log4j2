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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.TriConsumer;

/**
 * The actual ThreadContext Map. A new ThreadContext Map is created each time it is updated and the Map stored is always
 * immutable. This means the Map can be passed to other threads without concern that it will be updated. Since it is
 * expected that the Map will be passed to many more log events than the number of keys it contains the performance
 * should be much better than if the Map was copied for each event.
 */
public class DefaultThreadContextMap implements ThreadContextMap, ReadOnlyStringMap {
    private static final long serialVersionUID = 8218007901108944053L;

    /**
     * Property name ({@value} ) for selecting {@code InheritableThreadLocal} (value "true") or plain
     * {@code ThreadLocal} (value is not "true") in the implementation.
     */
    public static final String INHERITABLE_MAP = "isThreadContextMapInheritable";

    private final boolean useMap;
    private final ThreadLocal<Map<String, String>> localMap;

    private static boolean inheritableMap;

    static {
        init();
    }

    // LOG4J2-479: by default, use a plain ThreadLocal, only use InheritableThreadLocal if configured.
    // (This method is package protected for JUnit tests.)
    static ThreadLocal<Map<String, String>> createThreadLocalMap(final boolean isMapEnabled) {
        if (inheritableMap) {
            return new InheritableThreadLocal<Map<String, String>>() {
                @Override
                protected Map<String, String> childValue(final Map<String, String> parentValue) {
                    return parentValue != null && isMapEnabled //
                            ? Collections.unmodifiableMap(new HashMap<>(parentValue)) //
                            : null;
                }
            };
        }
        // if not inheritable, return plain ThreadLocal with null as initial value
        return new ThreadLocal<>();
    }

    static void init() {
        inheritableMap = PropertiesUtil.getProperties().getBooleanProperty(INHERITABLE_MAP);
    }

    public DefaultThreadContextMap() {
        this(true);
    }

    public DefaultThreadContextMap(final boolean useMap) {
        this.useMap = useMap;
        this.localMap = createThreadLocalMap(useMap);
    }

    @Override
    public void put(final String key, final String value) {
        if (!useMap) {
            return;
        }
        Map<String, String> map = localMap.get();
        map = map == null ? new HashMap<>(1) : new HashMap<>(map);
        map.put(key, value);
        localMap.set(Collections.unmodifiableMap(map));
    }

    public void putAll(final Map<String, String> m) {
        if (!useMap) {
            return;
        }
        Map<String, String> map = localMap.get();
        map = map == null ? new HashMap<>(m.size()) : new HashMap<>(map);
        for (final Map.Entry<String, String> e : m.entrySet()) {
            map.put(e.getKey(), e.getValue());
        }
        localMap.set(Collections.unmodifiableMap(map));
    }

    @Override
    public String get(final String key) {
        final Map<String, String> map = localMap.get();
        return map == null ? null : map.get(key);
    }

    @Override
    public void remove(final String key) {
        final Map<String, String> map = localMap.get();
        if (map != null) {
            final Map<String, String> copy = new HashMap<>(map);
            copy.remove(key);
            localMap.set(Collections.unmodifiableMap(copy));
        }
    }

    public void removeAll(final Iterable<String> keys) {
        final Map<String, String> map = localMap.get();
        if (map != null) {
            final Map<String, String> copy = new HashMap<>(map);
            for (final String key : keys) {
                copy.remove(key);
            }
            localMap.set(Collections.unmodifiableMap(copy));
        }
    }

    @Override
    public void clear() {
        localMap.remove();
    }

    @Override
    public Map<String, String> toMap() {
        return getCopy();
    }

    @Override
    public boolean containsKey(final String key) {
        final Map<String, String> map = localMap.get();
        return map != null && map.containsKey(key);
    }

    @Override
    public <V> void forEach(final BiConsumer<String, ? super V> action) {
        final Map<String, String> map = localMap.get();
        if (map == null) {
            return;
        }
        for (final Map.Entry<String, String> entry : map.entrySet()) {
            // BiConsumer should be able to handle values of any type V. In our case the values are of type String.
            @SuppressWarnings("unchecked")
            final V value = (V) entry.getValue();
            action.accept(entry.getKey(), value);
        }
    }

    @Override
    public <V, S> void forEach(final TriConsumer<String, ? super V, S> action, final S state) {
        final Map<String, String> map = localMap.get();
        if (map == null) {
            return;
        }
        for (final Map.Entry<String, String> entry : map.entrySet()) {
            // TriConsumer should be able to handle values of any type V. In our case the values are of type String.
            @SuppressWarnings("unchecked")
            final V value = (V) entry.getValue();
            action.accept(entry.getKey(), value, state);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> V getValue(final String key) {
        final Map<String, String> map = localMap.get();
        return (V) (map == null ? null : map.get(key));
    }

    @Override
    public Map<String, String> getCopy() {
        final Map<String, String> map = localMap.get();
        return map == null ? new HashMap<>() : new HashMap<>(map);
    }

    @Override
    public Map<String, String> getImmutableMapOrNull() {
        return localMap.get();
    }

    @Override
    public boolean isEmpty() {
        final Map<String, String> map = localMap.get();
        return map == null || map.isEmpty();
    }

    @Override
    public int size() {
        final Map<String, String> map = localMap.get();
        return map == null ? 0 : map.size();
    }

    @Override
    public String toString() {
        final Map<String, String> map = localMap.get();
        return map == null ? "{}" : map.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        final Map<String, String> map = this.localMap.get();
        result = prime * result + ((map == null) ? 0 : map.hashCode());
        result = prime * result + Boolean.valueOf(this.useMap).hashCode();
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
        if (obj instanceof DefaultThreadContextMap) {
            final DefaultThreadContextMap other = (DefaultThreadContextMap) obj;
            if (this.useMap != other.useMap) {
                return false;
            }
        }
        if (!(obj instanceof ThreadContextMap)) {
            return false;
        }
        final ThreadContextMap other = (ThreadContextMap) obj;
        final Map<String, String> map = this.localMap.get();
        final Map<String, String> otherMap = other.getImmutableMapOrNull();
        return Objects.equals(map, otherMap);
    }
}
