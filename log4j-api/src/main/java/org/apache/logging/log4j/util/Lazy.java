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
    T value();

    @Override
    default T get() {
        return value();
    }

    default <R> Lazy<R> map(final Function<? super T, ? extends R> function) {
        return lazy(() -> function.apply(value()));
    }

    boolean isInitialized();

    void set(final T newValue);

    /**
     * Creates a lazy value using the provided Supplier for initialization guarded by a Lock.
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
     * Creates a relaxed lazy value using the provided Supplier for initialization which may be invoked more than once
     * in order to set the initialized value.
     */
    static <T> Lazy<T> relaxed(final Supplier<T> supplier) {
        Objects.requireNonNull(supplier);
        return new LazyUtil.ReleaseAcquireLazy<>(supplier);
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
