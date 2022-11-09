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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.apache.logging.log4j.util.Cast;

class BindingMap {
    private final Map<Key<?>, Binding<?>> bindings;

    BindingMap() {
        bindings = new ConcurrentHashMap<>();
    }

    BindingMap(final BindingMap bindings) {
        this.bindings = new ConcurrentHashMap<>(bindings.bindings);
    }

    public <T> Binding<T> get(final Key<T> key) {
        return Cast.cast(bindings.get(key));
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
        return Cast.cast(binding);
    }

    public <T> void put(final Key<T> key, final Supplier<T> factory) {
        bindings.put(key, Binding.bind(key, factory));
    }

    /**
     * @see org.apache.logging.log4j.plugins.util.OrderedComparator
     */
    public <T> Supplier<T> merge(final Key<T> key, final Supplier<T> factory) {
        final Binding<?> newBinding = bindings.merge(key, Binding.bind(key, factory), (oldBinding, binding) ->
                oldBinding.getKey().getOrder() <= binding.getKey().getOrder() ? oldBinding : binding);
        return Cast.cast(newBinding.getSupplier());
    }

    public <T> void bindIfAbsent(final Key<T> key, final Supplier<T> factory) {
        bindings.putIfAbsent(key, Binding.bind(key, factory));
    }

    public void remove(final Key<?> key) {
        bindings.remove(key);
    }
}
