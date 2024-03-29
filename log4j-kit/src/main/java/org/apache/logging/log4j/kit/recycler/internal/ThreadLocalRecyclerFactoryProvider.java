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
package org.apache.logging.log4j.kit.recycler.internal;

import static java.util.Objects.requireNonNull;
import static org.apache.logging.log4j.util.LoaderUtil.isClassAvailable;

import aQute.bnd.annotation.spi.ServiceProvider;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.kit.recycler.Recycler;
import org.apache.logging.log4j.kit.recycler.RecyclerFactory;
import org.apache.logging.log4j.kit.recycler.RecyclerFactoryProvider;
import org.apache.logging.log4j.kit.recycler.RecyclerProperties;
import org.apache.logging.log4j.kit.recycler.support.AbstractRecycler;

/**
 * A {@link Recycler} factory provider such that the recycler pools objects in a fixed-size queue stored in a {@link ThreadLocal}.
 * <p>
 * This strategy may not be appropriate in workloads where units of work are independent of operating system threads such as reactive streams, coroutines, or virtual threads.
 * For such use cases, see {@link QueueingRecyclerFactoryProvider}.
 * </p>
 *
 * @since 3.0.0
 */
@ServiceProvider(RecyclerFactoryProvider.class)
public final class ThreadLocalRecyclerFactoryProvider implements RecyclerFactoryProvider {

    private static final boolean SERVLET_API_PRESENT =
            isClassAvailable("javax.servlet.Servlet") || isClassAvailable("jakarta.servlet.Servlet");

    @Override
    public int getOrder() {
        return SERVLET_API_PRESENT ? Integer.MAX_VALUE : 700;
    }

    @Override
    public String getName() {
        return "threadLocal";
    }

    @Override
    public RecyclerFactory createForEnvironment(final PropertyEnvironment environment) {
        requireNonNull(environment, "environment");
        final int capacity = environment.getProperty(RecyclerProperties.class).capacity();
        return new ThreadLocalRecyclerFactory(capacity);
    }

    // Visible for testing
    static final class ThreadLocalRecyclerFactory implements RecyclerFactory {

        /**
         * Maximum number of objects retained per thread.
         * <p>
         * This allows to acquire objects in recursive method calls and maintain minimal overhead in the scenarios where the active instance count goes far beyond this for a brief moment.
         * </p>
         */
        // Visible for testing
        final int capacity;

        private ThreadLocalRecyclerFactory(int capacity) {
            this.capacity = capacity;
        }

        @Override
        public <V> Recycler<V> create(final Supplier<V> supplier, final Consumer<V> cleaner) {
            requireNonNull(supplier, "supplier");
            requireNonNull(cleaner, "cleaner");
            return new ThreadLocalRecycler<>(supplier, cleaner, capacity);
        }

        // Visible for testing
        static final class ThreadLocalRecycler<V> extends AbstractRecycler<V> {

            private final Consumer<V> cleaner;

            // Visible for testing
            final ThreadLocal<Queue<V>> queueRef;

            private ThreadLocalRecycler(final Supplier<V> supplier, final Consumer<V> cleaner, final int capacity) {
                super(supplier);
                this.queueRef = ThreadLocal.withInitial(() -> new ArrayQueue<>(capacity));
                this.cleaner = cleaner;
            }

            @Override
            public V acquire() {
                final Queue<V> queue = queueRef.get();
                final V value = queue.poll();
                return value != null ? value : createInstance();
            }

            @Override
            public void release(final V value) {
                requireNonNull(value, "value");
                cleaner.accept(value);
                final Queue<V> queue = queueRef.get();
                queue.offer(value);
            }
        }
    }
}
