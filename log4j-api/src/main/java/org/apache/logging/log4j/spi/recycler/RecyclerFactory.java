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
package org.apache.logging.log4j.spi.recycler;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Contract for {@link Recycler} factories.
 *
 * @since 3.0.0
 */
public interface RecyclerFactory {

    /**
     * Creates a new recycler using the given supplier function for initial instances.
     *
     * @param supplier a function to provide initial instances
     * @param <V> the recyclable type
     * @return a new recycler
     */
    default <V> Recycler<V> create(final Supplier<V> supplier) {
        return create(supplier, ignored -> {});
    }

    /**
     * Creates a new recycler using the given supplier and cleaner functions.
     * <p>
     * The provided supplier needs to make sure that generated instances are always clean.
     * </p>
     * <p>
     * Recycled instances are always guaranteed to be clean.
     * The cleaning of an instance can take place either just before acquisition or prior to admitting it back into the reusable instances pool.
     * The moment when the cleaning will be carried out is implementation dependent.
     * Though a released instance should ideally be cleaned immediately to avoid keeping references to unused objects.
     * </p>
     *
     * @param supplier a function to provide initial (and clean!) instances
     * @param cleaner function to reset an instance before reuse
     * @param <V> the recyclable type
     * @return a new recycler
     */
    <V> Recycler<V> create(Supplier<V> supplier, Consumer<V> cleaner);
}
