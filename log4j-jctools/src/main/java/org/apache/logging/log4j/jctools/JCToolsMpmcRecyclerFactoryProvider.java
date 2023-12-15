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
package org.apache.logging.log4j.jctools;

import static java.util.Objects.requireNonNull;
import static org.apache.logging.log4j.spi.recycler.Recycler.DEFAULT_CAPACITY;

import aQute.bnd.annotation.spi.ServiceProvider;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.logging.log4j.spi.LoggingSystemProperty;
import org.apache.logging.log4j.spi.recycler.AbstractRecycler;
import org.apache.logging.log4j.spi.recycler.Recycler;
import org.apache.logging.log4j.spi.recycler.RecyclerFactory;
import org.apache.logging.log4j.spi.recycler.RecyclerFactoryProvider;
import org.apache.logging.log4j.util.PropertyEnvironment;
import org.jctools.queues.MpmcArrayQueue;

/**
 * A multi-producer-multi-consumer, thread-safe {@link Recycler} factory provider implementation based on <a href="https://jctools.github.io/JCTools/">JCTools</a>.
 */
@ServiceProvider(RecyclerFactoryProvider.class)
public final class JCToolsMpmcRecyclerFactoryProvider implements RecyclerFactoryProvider {

    @Override
    public int getOrder() {
        return 600;
    }

    @Override
    public String getName() {
        return "jctools-mpmc";
    }

    @Override
    public RecyclerFactory createForEnvironment(final PropertyEnvironment environment) {
        requireNonNull(environment, "environment");
        final int capacity = environment.getIntegerProperty(LoggingSystemProperty.RECYCLER_CAPACITY, DEFAULT_CAPACITY);
        if (capacity < 1) {
            throw new IllegalArgumentException("was expecting a `capacity` greater than 1, found: " + capacity);
        }
        return new JCToolsMpmcRecyclerFactory(capacity);
    }

    // Visible for testing
    static final class JCToolsMpmcRecyclerFactory implements RecyclerFactory {

        // Visible for testing
        private final int capacity;

        private JCToolsMpmcRecyclerFactory(final int capacity) {
            this.capacity = capacity;
        }

        @Override
        public <V> Recycler<V> create(Supplier<V> supplier, Consumer<V> cleaner) {
            requireNonNull(supplier, "supplier");
            requireNonNull(cleaner, "cleaner");
            final MpmcArrayQueue<V> queue = new MpmcArrayQueue<>(capacity);
            return new JCToolsMpmcRecycler<>(supplier, cleaner, queue);
        }

        // Visible for testing
        static final class JCToolsMpmcRecycler<V> extends AbstractRecycler<V> {

            private final Consumer<V> cleaner;

            // Visible for testing
            final Queue<V> queue;

            private JCToolsMpmcRecycler(final Supplier<V> supplier, final Consumer<V> cleaner, final Queue<V> queue) {
                super(supplier);
                this.cleaner = cleaner;
                this.queue = queue;
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
}
