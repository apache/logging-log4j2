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

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Recycling strategy that caches instances in a ThreadLocal value to allow threads to reuse objects. This strategy
 * may not be appropriate in workloads where units of work are independent of operating system threads such as
 * reactive streams, coroutines, or virtual threads.
 */
public class ThreadLocalRecyclerFactory implements RecyclerFactory {

    private static final ThreadLocalRecyclerFactory INSTANCE =
            new ThreadLocalRecyclerFactory();

    private ThreadLocalRecyclerFactory() {}

    public static ThreadLocalRecyclerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public <V> Recycler<V> create(
            final Supplier<V> supplier,
            final Consumer<V> lazyCleaner,
            final Consumer<V> eagerCleaner) {
        return new ThreadLocalRecycler<>(supplier, lazyCleaner, eagerCleaner);
    }

    // Visible for testing
    static class ThreadLocalRecycler<V> implements Recycler<V> {

        private final Supplier<V> supplier;

        private final Consumer<V> lazyCleaner;

        private final Consumer<V> eagerCleaner;

        private final ThreadLocal<Queue<V>> holder;

        private ThreadLocalRecycler(
                final Supplier<V> supplier,
                final Consumer<V> lazyCleaner,
                final Consumer<V> eagerCleaner) {
            this.supplier = supplier;
            this.lazyCleaner = lazyCleaner;
            this.eagerCleaner = eagerCleaner;
            this.holder = ThreadLocal.withInitial(ArrayDeque::new);
        }

        @Override
        public V acquire() {
            final Queue<V> queue = holder.get();
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
            holder.get().offer(value);
        }

        // Visible for testing
        Queue<V> getQueue() {
            return holder.get();
        }

    }
}
