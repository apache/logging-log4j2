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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Recycling strategy that caches instances in a ThreadLocal value to allow threads to reuse objects. This strategy
 * may not be appropriate in workloads where units of work are independent of operating system threads such as
 * reactive streams, coroutines, or virtual threads.
 *
 * @param <V> the recyclable type
 */
public class ThreadLocalRecycler<V> implements Recycler<V> {

    private final Supplier<V> supplier;
    private final Consumer<V> cleaner;

    private final ThreadLocal<V> holder;
    private final boolean referenceCountingEnabled;
    private final ThreadLocal<AtomicInteger> activeReferenceCount = ThreadLocal.withInitial(AtomicInteger::new);

    public ThreadLocalRecycler(
            final Supplier<V> supplier,
            final Consumer<V> cleaner) {
        this(supplier, cleaner, false);
    }

    public ThreadLocalRecycler(
            final Supplier<V> supplier,
            final Consumer<V> cleaner,
            final boolean referenceCountingEnabled) {
        this.supplier = supplier;
        this.cleaner = cleaner;
        this.holder = ThreadLocal.withInitial(supplier);
        this.referenceCountingEnabled = referenceCountingEnabled;
    }

    @Override
    public V acquire() {
        final V value;
        if (referenceCountingEnabled) {
            final AtomicInteger count = activeReferenceCount.get();
            if (count.compareAndSet(0, 1)) {
                value = holder.get();
            } else {
                count.incrementAndGet();
                value = supplier.get();
            }
        } else {
            value = holder.get();
        }
        cleaner.accept(value);
        return value;
    }

    @Override
    public void release(final V value) {
        if (referenceCountingEnabled) {
            activeReferenceCount.get().decrementAndGet();
        }
    }

    // Visible for testing
    int getActiveReferenceCount() {
        return activeReferenceCount.get().get();
    }

}
