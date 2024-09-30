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
package org.apache.logging.log4j.conversant;

import static java.util.Objects.requireNonNull;

import aQute.bnd.annotation.spi.ServiceProvider;
import com.conversantmedia.util.concurrent.DisruptorBlockingQueue;
import com.conversantmedia.util.concurrent.SpinPolicy;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.kit.recycler.Recycler;
import org.apache.logging.log4j.kit.recycler.RecyclerFactory;
import org.apache.logging.log4j.kit.recycler.RecyclerFactoryProvider;
import org.apache.logging.log4j.kit.recycler.RecyclerProperties;
import org.apache.logging.log4j.kit.recycler.support.AbstractRecycler;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * A {@link Recycler} factory provider implementation based on
 * <a href="https://github.com/conversant/disruptor>Conversant's Disruptor BlockingQueue</a>.
 *
 * @since 3.0.0
 */
@ServiceProvider(value = RecyclerFactoryProvider.class)
public final class DisruptorRecyclerFactoryProvider implements RecyclerFactoryProvider {

    private static final Logger LOGGER = StatusLogger.getLogger();

    @Override
    public int getOrder() {
        return 600;
    }

    @Override
    public String getName() {
        return "conversant-disruptor";
    }

    @Override
    public RecyclerFactory createForEnvironment(final PropertyEnvironment environment) {
        requireNonNull(environment, "environment");
        return new DisruptorRecyclerFactory(
                environment.getProperty(RecyclerProperties.class),
                environment.getProperty(DisruptorRecyclerProperties.class));
    }

    private static final class DisruptorRecyclerFactory implements RecyclerFactory {

        /**
         * Minimum capacity of the disruptor
         */
        private static final int MIN_CAPACITY = 8;

        private final int capacity;
        private final SpinPolicy spinPolicy;

        private DisruptorRecyclerFactory(
                final RecyclerProperties recyclerProperties, final DisruptorRecyclerProperties disruptorProperties) {
            this.capacity = validateCapacity(recyclerProperties.capacity());
            this.spinPolicy = disruptorProperties.spinPolicy();
        }

        @Override
        public <V> Recycler<V> create(final Supplier<V> supplier, final Consumer<V> cleaner) {
            requireNonNull(supplier, "supplier");
            requireNonNull(cleaner, "cleaner");
            final DisruptorBlockingQueue<V> queue = new DisruptorBlockingQueue<>(capacity, spinPolicy);
            return new DisruptorRecycler<>(supplier, cleaner, queue);
        }

        private static Integer validateCapacity(final int capacity) {
            if (capacity < MIN_CAPACITY) {
                LOGGER.warn(
                        "Invalid DisruptorBlockingQueue capacity {}, using minimum size {}.", capacity, MIN_CAPACITY);
                return MIN_CAPACITY;
            }
            final int roundedCapacity = Integers.ceilingNextPowerOfTwo(capacity);
            if (capacity != roundedCapacity) {
                LOGGER.warn(
                        "Invalid DisruptorBlockingQueue size {}, using rounded size {}.", capacity, roundedCapacity);
            }
            return roundedCapacity;
        }

        private static final class DisruptorRecycler<V> extends AbstractRecycler<V> {

            private final Consumer<V> cleaner;

            private final Queue<V> queue;

            private DisruptorRecycler(final Supplier<V> supplier, final Consumer<V> cleaner, final Queue<V> queue) {
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
