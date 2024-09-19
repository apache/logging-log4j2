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

import static org.apache.logging.log4j.internal.map.UnmodifiableArrayBackedMap.getMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.TriConsumer;

/**
 * The default implementation of {@link ThreadContextMap}
 * <p>
 *      An instance of UnmodifiableArrayBackedMap can be represented as a single {@code Object[]}, which can safely
 *      be stored on the {@code ThreadLocal} with no fear of classloader-related memory leaks.
 *  </p>
 *  <p>
 *      Performance of the underlying {@link org.apache.logging.log4j.internal.map.UnmodifiableArrayBackedMap} exceeds
 *      {@link HashMap} in all supported operations other than {@code get()}. Note that {@code get()} performance scales
 *      linearly with the current map size, and callers are advised to minimize this work.
 * </p>
 */
public class DefaultThreadContextMap implements ThreadContextMap, ReadOnlyStringMap {
    private static final long serialVersionUID = -2635197170958057849L;

    /**
     * Property name ({@value}) for selecting {@code InheritableThreadLocal} (value "true") or plain
     * {@code ThreadLocal} (value is not "true") in the implementation.
     */
    public static final String INHERITABLE_MAP = "isThreadContextMapInheritable";

    private ThreadLocal<Object[]> localState;

    public DefaultThreadContextMap() {
        this(PropertiesUtil.getProperties());
    }

    /**
     * @deprecated Since 2.24.0. Use {@link NoOpThreadContextMap} for a no-op implementation.
     */
    @Deprecated
    public DefaultThreadContextMap(final boolean ignored) {
        this(PropertiesUtil.getProperties());
    }

    DefaultThreadContextMap(final PropertiesUtil properties) {
        localState = properties.getBooleanProperty(INHERITABLE_MAP)
                ? new InheritableThreadLocal<Object[]>() {
                    @Override
                    protected Object[] childValue(final Object[] parentValue) {
                        return parentValue;
                    }
                }
                : new ThreadLocal<>();
    }

    @Override
    public void put(final String key, final String value) {
        final Object[] state = localState.get();
        localState.set(getMap(state).copyAndPut(key, value).getBackingArray());
    }

    public void putAll(final Map<String, String> m) {
        final Object[] state = localState.get();
        localState.set(getMap(state).copyAndPutAll(m).getBackingArray());
    }

    @Override
    public String get(final String key) {
        final Object[] state = localState.get();
        return state == null ? null : getMap(state).get(key);
    }

    @Override
    public void remove(final String key) {
        final Object[] state = localState.get();
        if (state != null) {
            localState.set(getMap(state).copyAndRemove(key).getBackingArray());
        }
    }

    public void removeAll(final Iterable<String> keys) {
        final Object[] state = localState.get();
        if (state != null) {
            localState.set(getMap(state).copyAndRemoveAll(keys).getBackingArray());
        }
    }

    @Override
    public void clear() {
        localState.remove();
    }

    @Override
    public Map<String, String> toMap() {
        return getCopy();
    }

    @Override
    public boolean containsKey(final String key) {
        final Object[] state = localState.get();
        return state != null && getMap(state).containsKey(key);
    }

    @Override
    public <V> void forEach(final BiConsumer<String, ? super V> action) {
        final Object[] state = localState.get();
        if (state == null) {
            return;
        }
        getMap(state).forEach(action);
    }

    @Override
    public <V, S> void forEach(final TriConsumer<String, ? super V, S> action, final S state) {
        final Object[] localState = this.localState.get();
        if (localState == null) {
            return;
        }
        getMap(localState).forEach(action, state);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> V getValue(final String key) {
        return (V) get(key);
    }

    @Override
    public Map<String, String> getCopy() {
        final Object[] state = localState.get();
        if (state == null) {
            return new HashMap<>(0);
        }
        return new HashMap<>(getMap(state));
    }

    @Override
    public Map<String, String> getImmutableMapOrNull() {
        final Object[] state = localState.get();
        return (state == null ? null : getMap(state));
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public int size() {
        final Object[] state = localState.get();
        return getMap(state).size();
    }

    @Override
    public String toString() {
        final Object[] state = localState.get();
        return state == null ? "{}" : getMap(state).toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        final Object[] state = localState.get();
        result = prime * result + ((state == null) ? 0 : getMap(state).hashCode());
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
        final Map<String, String> map = getMap(localState.get());
        final Map<String, String> otherMap = other.getImmutableMapOrNull();
        return Objects.equals(map, otherMap);
    }
}
