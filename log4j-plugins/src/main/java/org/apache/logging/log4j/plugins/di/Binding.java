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

import java.util.function.Supplier;

import org.apache.logging.log4j.util.Lazy;

/**
 * Bindings combine a {@link Key} with a factory {@link Supplier} to get instances of the key's type.
 *
 * @param <T> type of key to bind instance factory
 */
public final class Binding<T> implements Supplier<T> {
    private final Key<T> key;
    private final Supplier<? extends T> factory;

    private Binding(final Key<T> key, final Supplier<? extends T> factory) {
        this.key = key;
        this.factory = factory;
    }

    public Key<T> getKey() {
        return key;
    }

    @Override
    public T get() {
        return factory.get();
    }

    public static <T> DSL<T> from(final Key<T> key) {
        return new DSL<>(key);
    }

    public static <T> DSL<T> from(final Class<T> type) {
        return new DSL<>(Key.forClass(type));
    }

    public static class DSL<T> {
        private final Key<T> key;

        private DSL(final Key<T> key) {
            this.key = key;
        }

        public Binding<T> to(final Supplier<? extends T> factory) {
            return new Binding<>(key, factory);
        }

        public Binding<T> toSingleton(final Supplier<? extends T> factory) {
            return new Binding<>(key, Lazy.lazy(factory));
        }

        public Binding<T> toInstance(final T instance) {
            return new Binding<>(key, () -> instance);
        }
    }
}
