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

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Provides a lazily-initialized value from a {@code Supplier<T>}.
 *
 * @param <T> type of value
 */
public final class LazyValue<T> implements Supplier<T> {

    /**
     * Creates a lazy value using the provided Supplier for initialization.
     */
    public static <T> LazyValue<T> from(final Supplier<T> supplier) {
        return new LazyValue<>(supplier);
    }

    private final Supplier<T> supplier;
    private volatile T value;

    /**
     * Constructs a lazy value using the provided Supplier for initialization.
     *
     * @param supplier value to lazily initialize
     */
    public LazyValue(final Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T get() {
        T value = this.value;
        if (value == null) {
            synchronized (this) {
                value = this.value;
                if (value == null) {
                    this.value = value = supplier.get();
                }
            }
        }
        return value;
    }

    /**
     * Creates a LazyValue that maps the result of this LazyValue to another value.
     *
     * @param function mapping function to transform the result of this lazy value
     * @param <R>      the return type of the new lazy value
     * @return the new lazy value
     */
    public <R> LazyValue<R> map(final Function<? super T, ? extends R> function) {
        return from(() -> function.apply(get()));
    }
}
