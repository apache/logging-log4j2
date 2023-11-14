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

import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.logging.log4j.util.QueueFactories;

import static java.util.Objects.requireNonNull;

/**
 * A {@link RecyclerFactory} pooling objects in a queue stored in a {@link ThreadLocal}.
 * <p>
 * This strategy may not be appropriate in workloads where units of work are independent of operating system threads such as reactive streams, coroutines, or virtual threads.
 * For such use cases, see {@link QueueingRecyclerFactory}.
 * </p>
 *
 * @since 3.0.0
 */
public class ThreadLocalRecyclerFactory implements RecyclerFactory {

    /**
     * Maximum number of objects retained per thread.
     * <p>
     * This allows to acquire objects in recursive method calls and maintain minimal overhead in the scenarios where the active instance count goes far beyond this for a brief moment.
     * </p>
     */
    private final int capacity;

    public ThreadLocalRecyclerFactory(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("was expecting a `capacity` greater than 1, found: " + capacity);
        }
        this.capacity = capacity;
    }

    /**
     * @return maximum number of objects retained per thread in recyclers created
     */
    public int getCapacity() {
        return capacity;
    }

    @Override
    public <V> Recycler<V> create(final Supplier<V> supplier, final Consumer<V> cleaner) {
        requireNonNull(supplier, "supplier");
        requireNonNull(cleaner, "cleaner");
        return new ThreadLocalRecycler<>(supplier, cleaner, capacity);
    }

    // Visible for testing
    static class ThreadLocalRecycler<V> extends AbstractRecycler<V> {

        private final Consumer<V> cleaner;

        private final ThreadLocal<Queue<V>> holder;

        private ThreadLocalRecycler(
                final Supplier<V> supplier,
                final Consumer<V> cleaner,
                final int capacity) {
            super(supplier);
            this.holder = ThreadLocal.withInitial(() -> QueueFactories.SPSC.create(capacity));
            this.cleaner = cleaner;
        }

        @Override
        public V acquire() {
            final Queue<V> queue = holder.get();
            final V value = queue.poll();
            return value != null ? value : createInstance();
        }

        @Override
        public void release(final V value) {
            requireNonNull(value, "value");
            cleaner.accept(value);
            holder.get().offer(value);
        }

        // Visible for testing
        Queue<V> getQueue() {
            return holder.get();
        }

    }

}
