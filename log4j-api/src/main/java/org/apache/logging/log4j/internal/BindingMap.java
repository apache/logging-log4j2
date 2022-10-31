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

package org.apache.logging.log4j.internal;

import org.apache.logging.log4j.util3.Cast;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Mapping between classes and instance suppliers.
 */
public class BindingMap {
    private final Map<Class<?>, Binding<?>> bindings = new ConcurrentHashMap<>();

    private <T> Binding<T> getBinding(final Class<T> key) {
        return Cast.cast(bindings.get(key));
    }

    private <T> Supplier<? extends T> getSupplier(final Class<T> key) {
        final Binding<T> binding = getBinding(key);
        return binding == null ? null : binding.getSupplier();
    }

    public <T> Supplier<? extends T> getOrBindSupplier(final Class<T> key, final Supplier<? extends T> newSupplier) {
        final Binding<T> binding = Cast.cast(bindings.computeIfAbsent(key, ignored -> Binding.bind(key, newSupplier)));
        return binding.getSupplier();
    }

    public <T> T getInstance(final Class<T> key) {
        final Supplier<? extends T> supplier = getSupplier(key);
        if (supplier == null) {
            throw new NoSuchElementException("No binding registered for " + key);
        }
        return supplier.get();
    }

    public <T> BindingMap bind(final Class<T> key, final Supplier<? extends T> supplier) {
        bindings.put(key, Binding.bind(key, supplier));
        return this;
    }
}
