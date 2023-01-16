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
package org.apache.logging.log4j.spi;

import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.logging.log4j.util.QueueFactory;

public class QueueingRecyclerFactory implements RecyclerFactory {

    private final QueueFactory queueFactory;

    public QueueingRecyclerFactory(final QueueFactory queueFactory) {
        this.queueFactory = queueFactory;
    }

    @Override
    public <V> Recycler<V> create(
            final Supplier<V> supplier,
            final Consumer<V> lazyCleaner,
            final Consumer<V> eagerCleaner) {
        final Queue<V> queue = queueFactory.create();
        return new QueueingRecycler<>(supplier, lazyCleaner, eagerCleaner, queue);
    }

    // Visible for tests.
    static class QueueingRecycler<V> implements Recycler<V> {

        private final Supplier<V> supplier;

        private final Consumer<V> lazyCleaner;

        private final Consumer<V> eagerCleaner;

        private final Queue<V> queue;

        private QueueingRecycler(
                final Supplier<V> supplier,
                final Consumer<V> lazyCleaner,
                final Consumer<V> eagerCleaner,
                final Queue<V> queue) {
            this.supplier = supplier;
            this.lazyCleaner = lazyCleaner;
            this.eagerCleaner = eagerCleaner;
            this.queue = queue;
        }

        // Visible for tests.
        Queue<V> getQueue() {
            return queue;
        }

        @Override
        public V acquire() {
            final V value = queue.poll();
            if (value == null) {
                return supplier.get();
            } else {
                lazyCleaner.accept(value);
                return value;
            }
        }

        @Override
        public void release(final V value) {
            eagerCleaner.accept(value);
            queue.offer(value);
        }

    }
}
