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
package org.apache.logging.log4j.spi;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Factory for {@link Recycler} strategies. Depending on workloads, different instance recycling strategies may be
 * most performant. For example, traditional multithreaded workloads may benefit from using thread-local instance
 * recycling while different models of concurrency or versions of the JVM may benefit from using an object pooling
 * strategy instead.
 *
 * @since 3.0.0
 */
@FunctionalInterface
public interface RecyclerFactory {

    /**
     * Creates a new recycler using the given supplier function for initial instances. These instances have
     * no cleaner function and are assumed to always be reusable.
     *
     * @param supplier function to provide new instances of a recyclable object
     * @param <V> the recyclable type
     * @return a new recycler for V-type instances
     */
    default <V> Recycler<V> create(final Supplier<V> supplier) {
        return create(supplier, ignored -> {});
    }

    /**
     * Creates a new recycler using the given functions for providing fresh instances and for cleaning up
     * existing instances for reuse. For example, a StringBuilder recycler would provide two functions:
     * a supplier function for constructing a new StringBuilder with a preselected initial capacity and
     * another function for trimming the StringBuilder to some preselected maximum capacity and setting
     * its length back to 0 as if it were a fresh StringBuilder.
     *
     * @param supplier function to provide new instances of a recyclable object
     * @param cleaner function to reset a recyclable object to a fresh state
     * @param <V> the recyclable type
     * @return a new recycler for V-type instances
     */
    <V> Recycler<V> create(Supplier<V> supplier, Consumer<V> cleaner);
}
