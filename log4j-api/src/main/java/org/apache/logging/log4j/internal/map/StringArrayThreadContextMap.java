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
package org.apache.logging.log4j.internal.map;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.spi.ThreadContextMap;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.TriConsumer;

/**
 * An equivalent for DefaultThreadContxtMap, except that it's backed by
 * UnmodifiableArrayBackedMap. An instance of UnmodifiableArrayBackedMap can be
 * represented as a single Object[], which can safely be stored on the
 * ThreadLocal with no fear of classloader-related memory leaks. Performance
 * of the underlying UnmodifiableArrayBackedMap exceeds HashMap in all
 * supported operations other than get(). Note that get() performance scales
 * linearly with the current map size, and callers are advised to minimize this
 * work.
 */
public class StringArrayThreadContextMap implements ThreadContextMap, ReadOnlyStringMap {
    private static final long serialVersionUID = -2635197170958057849L;

    /**
     * Property name ({@value} ) for selecting {@code InheritableThreadLocal} (value "true") or plain
     * {@code ThreadLocal} (value is not "true") in the implementation.
     */
    public static final String INHERITABLE_MAP = "isThreadContextMapInheritable";

    private ThreadLocal<Object[]> threadLocalMapState;

    public StringArrayThreadContextMap() {
        threadLocalMapState = new ThreadLocal<>();
    }

    @Override
    public void put(final String key, final String value) {
        final Object[] state = threadLocalMapState.get();
        final UnmodifiableArrayBackedMap modifiedMap =
                UnmodifiableArrayBackedMap.getInstance(state).copyAndPut(key, value);
        threadLocalMapState.set(modifiedMap.getBackingArray());
    }

    public void putAll(final Map<String, String> m) {
        final Object[] state = threadLocalMapState.get();
        final UnmodifiableArrayBackedMap modifiedMap =
                UnmodifiableArrayBackedMap.getInstance(state).copyAndPutAll(m);
        threadLocalMapState.set(modifiedMap.getBackingArray());
    }

    @Override
    public String get(final String key) {
        final Object[] state = threadLocalMapState.get();
        if (state == null) {
            return null;
        }
        return UnmodifiableArrayBackedMap.getInstance(state).get(key);
    }

    @Override
    public void remove(final String key) {
        final Object[] state = threadLocalMapState.get();
        if (state != null) {
            final UnmodifiableArrayBackedMap modifiedMap =
                    UnmodifiableArrayBackedMap.getInstance(state).copyAndRemove(key);
            threadLocalMapState.set(modifiedMap.getBackingArray());
        }
    }

    public void removeAll(final Iterable<String> keys) {
        final Object[] state = threadLocalMapState.get();
        if (state != null) {
            final UnmodifiableArrayBackedMap modifiedMap =
                    UnmodifiableArrayBackedMap.getInstance(state).copyAndRemoveAll(keys);
            threadLocalMapState.set(modifiedMap.getBackingArray());
        }
    }

    @Override
    public void clear() {
        threadLocalMapState.remove();
    }

    @Override
    public Map<String, String> toMap() {
        return getCopy();
    }

    @Override
    public boolean containsKey(final String key) {
        final Object[] state = threadLocalMapState.get();
        return (state == null ? false : (UnmodifiableArrayBackedMap.getInstance(state)).containsKey(key));
    }

    @Override
    public <V> void forEach(final BiConsumer<String, ? super V> action) {
        final Object[] state = threadLocalMapState.get();
        if (state == null) {
            return;
        }
        final UnmodifiableArrayBackedMap map = UnmodifiableArrayBackedMap.getInstance(state);
        map.forEach(action);
    }

    @Override
    public <V, S> void forEach(final TriConsumer<String, ? super V, S> action, final S state) {
        final Object[] localState = threadLocalMapState.get();
        if (localState == null) {
            return;
        }
        final UnmodifiableArrayBackedMap map = UnmodifiableArrayBackedMap.getInstance(localState);
        map.forEach(action, state);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> V getValue(final String key) {
        return (V) get(key);
    }

    @Override
    public Map<String, String> getCopy() {
        final Object[] state = threadLocalMapState.get();
        if (state == null) {
            return new HashMap<>(0);
        }
        return new HashMap<>(UnmodifiableArrayBackedMap.getInstance(state));
    }

    @Override
    public Map<String, String> getImmutableMapOrNull() {
        final Object[] state = threadLocalMapState.get();
        return (state == null ? null : UnmodifiableArrayBackedMap.getInstance(state));
    }

    @Override
    public boolean isEmpty() {
        return (size() == 0);
    }

    @Override
    public int size() {
        final Object[] state = threadLocalMapState.get();
        return UnmodifiableArrayBackedMap.getInstance(state).size();
    }

    @Override
    public String toString() {
        final Object[] state = threadLocalMapState.get();
        return state == null
                ? "{}"
                : UnmodifiableArrayBackedMap.getInstance(state).toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        final Object[] state = threadLocalMapState.get();
        result = prime * result
                + ((state == null)
                        ? 0
                        : UnmodifiableArrayBackedMap.getInstance(state).hashCode());
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
        final Map<String, String> map = UnmodifiableArrayBackedMap.getInstance(this.threadLocalMapState.get());
        final Map<String, String> otherMap = other.getImmutableMapOrNull();
        return Objects.equals(map, otherMap);
    }
}
