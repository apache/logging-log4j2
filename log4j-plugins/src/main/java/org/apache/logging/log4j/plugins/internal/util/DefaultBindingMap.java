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

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import org.apache.logging.log4j.lang.Nullable;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.util.Cast;
import org.apache.logging.log4j.util.InternalApi;

@InternalApi
public class DefaultBindingMap implements BindingMap {
    private final HierarchicalMap<Key<?>, Supplier<?>> bindings;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    DefaultBindingMap(final HierarchicalMap<Key<?>, Supplier<?>> bindings) {
        this.bindings = bindings;
    }

    @Override
    public @Nullable <T> Supplier<T> get(final Key<T> key, final Iterable<String> aliases) {
        lock.readLock().lock();
        try {
            final Supplier<T> existing = get(key);
            if (existing != null) {
                return existing;
            }
            for (final String alias : aliases) {
                final Supplier<T> existingAlias = get(key.withName(alias));
                if (existingAlias != null) {
                    return existingAlias;
                }
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    private <T> @Nullable Supplier<T> get(final Key<T> key) {
        return Cast.cast(bindings.get(key));
    }

    @Override
    public <T> void put(final Key<? super T> key, final Supplier<T> factory) {
        lock.writeLock().lock();
        try {
            bindings.put(key, factory);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public <T> void putIfAbsent(final Key<? super T> key, final Supplier<T> factory) {
        lock.writeLock().lock();
        try {
            bindings.putIfAbsent(key, factory);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void remove(final Key<?> key) {
        lock.writeLock().lock();
        try {
            bindings.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean containsKey(final Key<?> key) {
        lock.readLock().lock();
        try {
            return bindings.containsKey(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsLocalKey(final Key<?> key) {
        lock.readLock().lock();
        try {
            return bindings.containsLocalKey(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public BindingMap newChildMap() {
        return new DefaultBindingMap(bindings.newChildMap());
    }
}
