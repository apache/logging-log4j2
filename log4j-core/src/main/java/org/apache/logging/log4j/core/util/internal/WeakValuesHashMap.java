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
package org.apache.logging.log4j.core.util.internal;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A {@link HashMap} implementation which is similar to {@link java.util.WeakHashMap} but for weak values instead of
 * keys. Warning: this is not a general-purpose implementation; only what's needed by @{@link InternalLoggerRegistry}
 * is implemented.
 * @param <K> - the type of keys maintained by this map
 * @param <V> – the type of mapped values
 */
class WeakValuesHashMap<K, V> extends AbstractMap<K, V> {
    private final HashMap<K, WeakValue<V>> map;
    private final ReferenceQueue<V> queue;

    public WeakValuesHashMap() {
        map = new HashMap<>(10);
        queue = new ReferenceQueue<>();
    }

    @Override
    public V put(K key, V value) {
        processQueue();
        WeakValue<V> ref = new WeakValue<>(key, value, queue);
        return unref(map.put(key, ref));
    }

    @Override
    public V get(Object key) {
        processQueue();
        return unref(map.get(key));
    }

    @Override
    public V remove(Object key) {
        return unref(map.get(key));
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        processQueue();
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        processQueue();
        return map.size();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException();
    }

    private V unref(WeakValue<V> valueRef) {
        return valueRef == null ? null : valueRef.get();
    }

    @SuppressWarnings("unchecked")
    private void processQueue() {
        for (WeakValue<V> v = (WeakValue<V>) queue.poll(); v != null; v = (WeakValue<V>) queue.poll()) {
            map.remove(v.getKey());
        }
    }

    private class WeakValue<T> extends WeakReference<T> {
        private final K key;

        private WeakValue(K key, T value, ReferenceQueue<T> queue) {
            super(value, queue);
            this.key = key;
        }

        private K getKey() {
            return key;
        }
    }
}
