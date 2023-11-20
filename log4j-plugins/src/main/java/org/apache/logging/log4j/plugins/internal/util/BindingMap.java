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
package org.apache.logging.log4j.plugins.internal.util;

import java.util.Collection;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.logging.log4j.plugins.di.Binding;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.util.Cast;
import org.apache.logging.log4j.util.InternalApi;

@InternalApi
public class BindingMap {
    private final HierarchicalMap<Key<?>, Binding<?>> bindings;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private BindingMap(final HierarchicalMap<Key<?>, Binding<?>> bindings) {
        this.bindings = bindings;
    }

    public <T> Binding<T> get(final Key<T> key, final Collection<String> aliases) {
        lock.readLock().lock();
        try {
            final Binding<T> existing = get(key);
            if (existing != null) {
                return existing;
            }
            for (final String alias : aliases) {
                final Binding<T> existingAlias = get(key.withName(alias));
                if (existingAlias != null) {
                    return existingAlias;
                }
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    <T> Binding<T> get(final Key<T> key) {
        return Cast.cast(bindings.get(key));
    }

    public void put(final Binding<?> binding) {
        lock.writeLock().lock();
        try {
            bindings.put(binding.getKey(), binding);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Binding<?> putIfAbsent(final Binding<?> binding) {
        lock.readLock().lock();
        try {
            final Binding<?> existing = bindings.get(binding.getKey());
            if (existing != null) {
                return existing;
            }
        } finally {
            lock.readLock().unlock();
        }
        lock.writeLock().lock();
        try {
            return bindings.putIfAbsent(binding.getKey(), binding);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public <T> Binding<T> merge(final Binding<T> binding) {
        final Key<T> key = binding.getKey();
        lock.writeLock().lock();
        try {
            final Binding<?> merged = bindings.merge(key, binding, (existingBinding, ignored) -> {
                final Binding<T> existing = Cast.cast(existingBinding);
                final Key<T> existingKey = existing.getKey();
                final int compared = existingKey.compareTo(key);
                return compared > 0 ? binding : existing;
            });
            return Cast.cast(merged);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void remove(final Key<?> key) {
        lock.writeLock().lock();
        try {
            bindings.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean containsKey(final Key<?> key) {
        lock.readLock().lock();
        try {
            return bindings.containsKey(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean containsLocalKey(final Key<?> key) {
        lock.readLock().lock();
        try {
            return bindings.containsLocalKey(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    public BindingMap newChildMap() {
        return new BindingMap(bindings.newChildMap());
    }

    public static BindingMap newRootMap() {
        return new BindingMap(HierarchicalMap.newRootMap());
    }
}
