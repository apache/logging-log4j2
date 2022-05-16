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

package org.apache.logging.log4j.plugins.di;


import org.apache.logging.log4j.plugins.util.TypeUtil;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

class BindingMap {
    private final Map<Key<?>, Binding<?>> bindings;

    BindingMap() {
        bindings = new ConcurrentHashMap<>();
    }

    BindingMap(final BindingMap bindings) {
        this.bindings = new ConcurrentHashMap<>(bindings.bindings);
    }

    public <T> Binding<T> get(final Key<T> key) {
        return TypeUtil.cast(bindings.get(key));
    }

    public <T> Binding<T> get(final Key<T> key, final Collection<String> aliases) {
        var binding = bindings.get(key);
        if (binding == null) {
            for (final String alias : aliases) {
                binding = bindings.get(key.withName(alias));
                if (binding != null) {
                    break;
                }
            }
        }
        return TypeUtil.cast(binding);
    }

    public <T> void put(final Key<T> key, final Supplier<T> factory) {
        bindings.put(key, Binding.bind(key, factory));
    }

    public <T> boolean putIfAbsent(final Key<T> key, final Supplier<T> factory) {
        return bindings.putIfAbsent(key, Binding.bind(key, factory)) == null;
    }

    public <T> Supplier<T> bindIfAbsent(final Key<T> key, final Supplier<T> factory) {
        return TypeUtil.cast(bindings.computeIfAbsent(key, k -> Binding.bind(key, factory)).getSupplier());
    }

    public void remove(final Key<?> key) {
        bindings.remove(key);
    }
}
