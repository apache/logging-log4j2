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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.core.context.internal.UnmodifiableArrayBackedMap;
import org.apache.logging.log4j.spi.ThreadContextMap;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.TriConsumer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
@NullMarked
public class StringArrayThreadContextMap implements ThreadContextMap, ReadOnlyStringMap {
    private static final long serialVersionUID = -2635197170958057849L;

    /**
     * Property name ({@value} ) for selecting {@code InheritableThreadLocal} (value "true") or plain
     * {@code ThreadLocal} (value is not "true") in the implementation.
     */
    private static final String INHERITABLE_MAP = "isThreadContextMapInheritable";

    private ThreadLocal<Object @Nullable []> localMap;

    public StringArrayThreadContextMap() {
        this(PropertiesUtil.getProperties());
    }

    StringArrayThreadContextMap(final PropertiesUtil properties) {
        localMap = properties.getBooleanProperty(INHERITABLE_MAP)
                ? new InheritableThreadLocal<Object @Nullable []>() {
                    @Override
                    protected Object @Nullable [] childValue(final Object @Nullable [] parentValue) {
                        return parentValue != null ? Arrays.copyOf(parentValue, parentValue.length) : null;
                    }
                }
                : new ThreadLocal<>();
    }

    @Override
    public void put(final String key, final String value) {
        final Object[] state = localMap.get();
        final UnmodifiableArrayBackedMap modifiedMap =
                UnmodifiableArrayBackedMap.getInstance(state).copyAndPut(key, value);
        localMap.set(modifiedMap.getBackingArray());
    }

    @Override
    public void putAll(final Map<String, String> m) {
        final Object[] state = localMap.get();
        final UnmodifiableArrayBackedMap modifiedMap =
                UnmodifiableArrayBackedMap.getInstance(state).copyAndPutAll(m);
        localMap.set(modifiedMap.getBackingArray());
    }

    @Override
    public @Nullable String get(final String key) {
        final Object[] state = localMap.get();
        if (state == null) {
            return null;
        }
        return UnmodifiableArrayBackedMap.getInstance(state).get(key);
    }

    @Override
    public void remove(final String key) {
        final Object[] state = localMap.get();
        if (state != null) {
            final UnmodifiableArrayBackedMap modifiedMap =
                    UnmodifiableArrayBackedMap.getInstance(state).copyAndRemove(key);
            localMap.set(modifiedMap.getBackingArray());
        }
    }

    @Override
    public void removeAll(final Iterable<String> keys) {
        final Object[] state = localMap.get();
        if (state != null) {
            final UnmodifiableArrayBackedMap modifiedMap =
                    UnmodifiableArrayBackedMap.getInstance(state).copyAndRemoveAll(keys);
            localMap.set(modifiedMap.getBackingArray());
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
        final Object @Nullable [] state = localMap.get();
        return (state != null && (UnmodifiableArrayBackedMap.getInstance(state)).containsKey(key));
    }

    @Override
    public <V> void forEach(final BiConsumer<String, ? super V> action) {
        final Object[] state = localMap.get();
        if (state == null) {
            return;
        }
        final UnmodifiableArrayBackedMap map = UnmodifiableArrayBackedMap.getInstance(state);
        map.forEach(action);
    }

    @Override
    public <V, S> void forEach(final TriConsumer<String, ? super V, S> action, final S state) {
        final Object[] localState = localMap.get();
        if (localState == null) {
            return;
        }
        final UnmodifiableArrayBackedMap map = UnmodifiableArrayBackedMap.getInstance(localState);
        map.forEach(action, state);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> @Nullable V getValue(final String key) {
        return (V) get(key);
    }

    @Override
    public Map<String, String> getCopy() {
        final Object[] state = localMap.get();
        if (state == null) {
            return new HashMap<>(0);
        }
        return new HashMap<>(UnmodifiableArrayBackedMap.getInstance(state));
    }

    @Override
    public @Nullable Map<String, String> getImmutableMapOrNull() {
        final Object[] state = localMap.get();
        return (state == null ? null : UnmodifiableArrayBackedMap.getInstance(state));
    }

    @Override
    public boolean isEmpty() {
        return (size() == 0);
    }

    @Override
    public int size() {
        final Object[] state = localMap.get();
        return UnmodifiableArrayBackedMap.getInstance(state).size();
    }

    @Override
    public String toString() {
        final Object[] state = localMap.get();
        return state == null
                ? "{}"
                : UnmodifiableArrayBackedMap.getInstance(state).toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        final Object[] state = localMap.get();
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
        final Map<String, String> map = UnmodifiableArrayBackedMap.getInstance(this.localMap.get());
        final Map<String, String> otherMap = other.getImmutableMapOrNull();
        return Objects.equals(map, otherMap);
    }
}
