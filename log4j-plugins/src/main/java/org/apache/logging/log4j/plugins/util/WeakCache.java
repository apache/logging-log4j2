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

package org.apache.logging.log4j.plugins.util;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

public class WeakCache<K, V> implements Cache<K, V> {

    public static <K, V> WeakCache<K, V> newCache(final Function<K, V> function) {
        return new WeakCache<>(key -> LazyValue.forSupplier(() -> function.apply(key)));
    }

    public static <K, V> WeakCache<K, V> newWeakRefCache(final Function<K, V> function) {
        return new WeakCache<>(key -> WeakLazyValue.forSupplier(() -> function.apply(key)));
    }

    private final long maxSize;
    private final Function<K, Value<? extends V>> valueFunction;
    private final Map<K, Value<? extends V>> map = new WeakHashMap<>();

    public WeakCache(final Function<K, Value<? extends V>> valueFunction) {
        this(0, valueFunction);
    }

    public WeakCache(final long maxSize, final Function<K, Value<? extends V>> valueFunction) {
        this.valueFunction = valueFunction;
        this.maxSize = maxSize;
    }

    @Override
    public synchronized <U extends V> U get(final K key) {
        final Value<? extends V> value = map.computeIfAbsent(key, valueFunction);
        if (maxSize > 0 && map.size() > maxSize) {
            map.clear();
        }
        return TypeUtil.cast(value.get());
    }

    @Override
    public void close() {
        map.clear();
    }
}
