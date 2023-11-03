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
package org.apache.logging.log4j.util;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Provides a lazily-initialized value from a {@code Supplier<T>}.
 *
 * @param <T> type of value
 */
public interface Lazy<T> extends Supplier<T> {
    /**
     * Returns the value held by this lazy. This may cause the value to initialize if it hasn't been already.
     */
    T value();

    /**
     * Returns the value held by this lazy. This may cause the value to initialize if it hasn't been already.
     */
    @Override
    default T get() {
        return value();
    }

    /**
     * Creates a new lazy value derived from this lazy value using the provided value mapping function.
     */
    default <R> Lazy<R> map(final Function<? super T, ? extends R> function) {
        return lazy(() -> function.apply(value()));
    }

    /**
     * Indicates whether this lazy value has been initialized.
     */
    boolean isInitialized();

    /**
     * Sets this lazy value to the provided value.
     *
     * @throws UnsupportedOperationException if this type of lazy value cannot be updated
     */
    void set(final T newValue);

    /**
     * Creates a strict lazy value using the provided Supplier. The supplier is guaranteed to only be invoked by at
     * most one thread, and all threads will see the same published value when this returns.
     */
    static <T> Lazy<T> lazy(final Supplier<T> supplier) {
        Objects.requireNonNull(supplier);
        return new LazyUtil.SafeLazy<>(supplier);
    }

    /**
     * Creates a lazy value using the provided constant value.
     */
    static <T> Lazy<T> value(final T value) {
        return new LazyUtil.Constant<>(value);
    }

    /**
     * Creates a lazy value using a weak reference to the provided value.
     */
    static <T> Lazy<T> weak(final T value) {
        return new LazyUtil.WeakConstant<>(value);
    }

    /**
     * Creates a pure lazy value using the provided Supplier to initialize the value. The supplier may be invoked more
     * than once, and the return value should be a purely computed value as the result may be a different instance
     * each time. This is useful for building cache tables and other pure computations.
     */
    static <T> Lazy<T> pure(final Supplier<T> supplier) {
        Objects.requireNonNull(supplier);
        return new LazyUtil.PureLazy<>(supplier);
    }
}
