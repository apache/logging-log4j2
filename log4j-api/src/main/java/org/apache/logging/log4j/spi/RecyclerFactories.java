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

import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.util.QueueFactories;
import org.apache.logging.log4j.util.QueueFactory;
import org.apache.logging.log4j.util.StringParameterParser;

import static org.apache.logging.log4j.util.Constants.isThreadLocalsEnabled;

public final class RecyclerFactories {

    // Visible for testing
    static final int DEFAULT_QUEUE_CAPACITY = Math.max(
            2 * Runtime.getRuntime().availableProcessors() + 1,
            8);

    private RecyclerFactories() {}

    public static RecyclerFactory getDefault() {
        return isThreadLocalsEnabled()
                ? new ThreadLocalRecyclerFactory(DEFAULT_QUEUE_CAPACITY)
                : new QueueingRecyclerFactory(QueueFactories.MPMC.factory(DEFAULT_QUEUE_CAPACITY));
    }

    public static RecyclerFactory ofSpec(final String recyclerFactorySpec) {

        // TLA-, MPMC-, or ABQ-based queueing factory -- if nothing is specified.
        if (recyclerFactorySpec == null) {
            return getDefault();
        }

        // Is a dummy factory requested?
        else if (recyclerFactorySpec.equals("dummy")) {
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
            throw new IllegalArgumentException(
                    "invalid recycler factory: " + recyclerFactorySpec);
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
        final String queueFactorySpec = recyclerFactorySpec.substring(
                "queue".length() + (recyclerFactorySpec.startsWith("queue:") ? 1 : 0));
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
        final QueueFactory queueFactory = supplierPath != null
                ? QueueFactories.createQueueFactory(supplierPath, capacity)
                : QueueFactories.MPMC.factory(capacity);
        return new QueueingRecyclerFactory(queueFactory);

    }

    private static int readQueueCapacity(
            final String factorySpec,
            final Map<String, StringParameterParser.Value> parsedValues) {
        final StringParameterParser.Value capacityValue = parsedValues.get("capacity");
        if (capacityValue == null || capacityValue instanceof StringParameterParser.NullValue) {
            return DEFAULT_QUEUE_CAPACITY;
        } else {
            try {
                return Integer.parseInt(capacityValue.toString());
            } catch (final NumberFormatException error) {
                throw new IllegalArgumentException(
                        "failed reading `capacity` in recycler factory: " + factorySpec,
                        error);
            }
        }
    }

}
