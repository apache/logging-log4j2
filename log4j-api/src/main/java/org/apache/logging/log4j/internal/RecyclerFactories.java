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
package org.apache.logging.log4j.internal;

import static java.util.Objects.requireNonNull;
import static org.apache.logging.log4j.util.Constants.isThreadLocalsEnabled;

import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.spi.DummyRecyclerFactory;
import org.apache.logging.log4j.spi.QueueFactory;
import org.apache.logging.log4j.spi.RecyclerFactory;
import org.apache.logging.log4j.util.InternalApi;

/**
 * Stores the default {@link RecyclerFactory} instance.
 */
@InternalApi
public final class RecyclerFactories {

    /**
     * The default recycler capacity.
     */
    public static final int CAPACITY = Math.max(2 * Runtime.getRuntime().availableProcessors() + 1, 8);

    /**
     * The default recycler instance.
     */
    public static final RecyclerFactory INSTANCE = isThreadLocalsEnabled()
            ? new ThreadLocalRecyclerFactory(CAPACITY)
            : new QueueingRecyclerFactory(QueueFactories.INSTANCE, CAPACITY);

    private RecyclerFactories() {}

    /**
     * Creates a {@link RecyclerFactory} instance using the provided specification.
     * <p>
     * The recycler factory specification string must be formatted as follows:
     * </p>
     * <pre>{@code
     * recyclerFactorySpec            = dummySpec
     *                                | threadLocalRecyclerFactorySpec
     *                                | queueingRecyclerFactorySpec
     *
     * dummySpec                      = "dummy"
     *
     * threadLocalRecyclerFactorySpec = "threadLocal" , [ ":" , capacityArg ]
     * capacityArg                    = "capacity=" , integer
     *
     * queueingRecyclerFactorySpec    = "queue" , [ ":" , queueingRecyclerFactoryArgs ]
     * queueingRecyclerFactoryArgs    = queueingRecyclerFactoryArg , [ "," , queueingRecyclerFactoryArg ]*
     * queueingRecyclerFactoryArg     = capacityArg
     *                                | queueSupplierArg
     * queueSupplierArg               = ( classPath , ".new" )
     *                                | ( classPath , "." , methodName )
     * }</pre>
     * <p>
     * If not specified, {@code capacity} will be set to {@code max(8, 2*C+1)}, where {@code C} denotes the value returned by {@link Runtime#availableProcessors()}.
     * </p>
     * <p>
     * You can find some examples below.
     * </p>
     * <ul>
     * <li><code>{@code dummy}</code></li>
     * <li><code>{@code threadLocal}</code></li>
     * <li><code>{@code threadLocal:capacity=13}</code></li>
     * <li><code>{@code queue}</code></li>
     * <li><code>{@code queue:supplier=java.util.ArrayDeque.new}</code></li>
     * <li><code>{@code queue:capacity=100}</code></li>
     * <li><code>{@code queue:supplier=com.acme.AwesomeQueue.create,capacity=42}</code></li>
     * </ul>
     * @param recyclerFactorySpec the recycler factory specification string
     * @return a recycler factory instance
     */
    public static RecyclerFactory ofSpec(final String recyclerFactorySpec) {

        // Check arguments
        requireNonNull(recyclerFactorySpec, "recyclerFactorySpec");

        // Is a dummy factory requested?
        if (recyclerFactorySpec.equals("dummy")) {
            return DummyRecyclerFactory.getInstance();
        }

        // Is a TLA factory requested?
        else if (recyclerFactorySpec.startsWith("threadLocal")) {
            return readThreadLocalRecyclerFactory(recyclerFactorySpec);
        }

        // Is a queueing factory requested?
        else if (recyclerFactorySpec.startsWith("queue")) {
            return readQueueingRecyclerFactory(recyclerFactorySpec);
        }

        // Bogus input, bail out.
        else {
            throw new IllegalArgumentException("invalid recycler factory: " + recyclerFactorySpec);
        }
    }

    private static RecyclerFactory readThreadLocalRecyclerFactory(final String recyclerFactorySpec) {

        // Parse the spec
        final String queueFactorySpec = recyclerFactorySpec.substring(
                "threadLocal".length() + (recyclerFactorySpec.startsWith("threadLocal:") ? 1 : 0));
        final Map<String, StringParameterParser.Value> parsedValues =
                StringParameterParser.parse(queueFactorySpec, Set.of("capacity"));

        // Read the capacity
        final int capacity = readQueueCapacity(queueFactorySpec, parsedValues);

        // Execute the read spec
        return new ThreadLocalRecyclerFactory(capacity);
    }

    private static RecyclerFactory readQueueingRecyclerFactory(final String recyclerFactorySpec) {

        // Parse the spec
        final String queueFactorySpec =
                recyclerFactorySpec.substring("queue".length() + (recyclerFactorySpec.startsWith("queue:") ? 1 : 0));
        final Map<String, StringParameterParser.Value> parsedValues =
                StringParameterParser.parse(queueFactorySpec, Set.of("supplier", "capacity"));

        // Read the capacity
        final int capacity = readQueueCapacity(queueFactorySpec, parsedValues);

        // Read the supplier path
        final StringParameterParser.Value supplierValue = parsedValues.get("supplier");
        final String supplierPath = supplierValue == null || supplierValue instanceof StringParameterParser.NullValue
                ? null
                : supplierValue.toString();

        // Execute the read spec
        final QueueFactory queueFactory =
                supplierPath != null ? QueueFactories.ofSupplier(supplierPath) : QueueFactories.INSTANCE;
        return new QueueingRecyclerFactory(queueFactory, capacity);
    }

    private static int readQueueCapacity(
            final String factorySpec, final Map<String, StringParameterParser.Value> parsedValues) {
        final StringParameterParser.Value capacityValue = parsedValues.get("capacity");
        if (capacityValue == null || capacityValue instanceof StringParameterParser.NullValue) {
            return CAPACITY;
        } else {
            final int capacity;
            try {
                capacity = Integer.parseInt(capacityValue.toString());
            } catch (final NumberFormatException error) {
                throw new IllegalArgumentException(
                        "failed reading `capacity` in recycler factory: " + factorySpec, error);
            }
            if (capacity < 1) {
                throw new IllegalArgumentException(
                        "was expecting `capacity > 0` in the recycler factory: " + factorySpec);
            }
            return capacity;
        }
    }
}
