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

import static java.util.Objects.requireNonNull;

import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.logging.log4j.util.QueueFactory;

/**
 * A {@link RecyclerFactory} pooling objects in a queue created using the provided {@link QueueFactory}.
 */
public class QueueingRecyclerFactory implements RecyclerFactory {

    private final QueueFactory queueFactory;

    private final int capacity;

    public QueueingRecyclerFactory(final QueueFactory queueFactory, final int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("was expecting `capacity > 0`, found: " + capacity);
        }
        this.queueFactory = requireNonNull(queueFactory, "queueFactory");
        this.capacity = capacity;
    }

    /**
     * @return the maximum number of objects retained per thread in recyclers created
     */
    public int getCapacity() {
        return capacity;
    }

    @Override
    public <V> Recycler<V> create(final Supplier<V> supplier, final Consumer<V> cleaner) {
        requireNonNull(supplier, "supplier");
        requireNonNull(cleaner, "cleaner");
        final Queue<V> queue = queueFactory.create(capacity);
        return new QueueingRecycler<>(supplier, cleaner, queue);
    }

    // Visible for tests
    static class QueueingRecycler<V> extends AbstractRecycler<V> {

        private final Consumer<V> cleaner;

        private final Queue<V> queue;

        private QueueingRecycler(final Supplier<V> supplier, final Consumer<V> cleaner, final Queue<V> queue) {
            super(supplier);
            this.cleaner = cleaner;
            this.queue = queue;
        }

        // Visible for tests
        Queue<V> getQueue() {
            return queue;
        }

        @Override
        public V acquire() {
            final V value = queue.poll();
            return value != null ? value : createInstance();
        }

        @Override
        public void release(final V value) {
            requireNonNull(value, "value");
            cleaner.accept(value);
            queue.offer(value);
        }
    }
}
