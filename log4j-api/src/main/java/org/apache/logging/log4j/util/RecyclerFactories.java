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

import org.jctools.queues.MpmcArrayQueue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Supplier;

import static org.apache.logging.log4j.util.Constants.isThreadLocalsEnabled;

public final class RecyclerFactories {

    private RecyclerFactories() {}

    private static final String JCTOOLS_FACTORY_CLASS_NAME =
            RecyclerFactories.class.getName() + "$MpmcArrayQueueFactory";

    private interface QueueFactory {
        <V> Queue<V> create(final int capacity);
    }

    private static class ArrayBlockingQueueFactory implements QueueFactory {
        @Override
        public <V> Queue<V> create(final int capacity) {
            return new ArrayBlockingQueue<>(capacity);
        }
    }

    @SuppressWarnings("unused") // loaded via reflection to check for presence of JCTools
    private static class MpmcArrayQueueFactory implements QueueFactory {
        @Override
        public <V> Queue<V> create(final int capacity) {
            return new MpmcArrayQueue<>(capacity);
        }
    }

    private static <V> Supplier<Queue<V>> getQueueSupplier(final int capacity) {
        final ClassLoader classLoader = RecyclerFactories.class.getClassLoader();
        Class<? extends QueueFactory> factoryClass;
        try {
            // try to load RecyclerFactories.MpmcArrayQueueFactory; a linkage error should occur if JCTools is unavailable
            factoryClass = classLoader.loadClass(JCTOOLS_FACTORY_CLASS_NAME)
                    .asSubclass(QueueFactory.class);
        } catch (final ClassNotFoundException | LinkageError ignored) {
            factoryClass = ArrayBlockingQueueFactory.class;
        }
        final QueueFactory queueFactory = ReflectionUtil.instantiate(factoryClass);
        return () -> queueFactory.create(capacity);
    }

    public static RecyclerFactory ofSpec(final String recyclerFactorySpec) {

        // Determine the default capacity.
        final int defaultCapacity = Math.max(
                2 * Runtime.getRuntime().availableProcessors() + 1,
                8);

        // TLA-, MPMC-, or ABQ-based queueing factory -- if nothing is specified.
        if (recyclerFactorySpec == null) {
            if (isThreadLocalsEnabled()) {
                return ThreadLocalRecyclerFactory.getInstance();
            } else {
                return new QueueingRecyclerFactory(getQueueSupplier(defaultCapacity));
            }
        }

        // Is a dummy factory requested?
        else if (recyclerFactorySpec.equals("dummy")) {
            return DummyRecyclerFactory.getInstance();
        }

        // Is a TLA factory requested?
        else if (recyclerFactorySpec.equals("threadLocal")) {
            return ThreadLocalRecyclerFactory.getInstance();
        }

        // Is a queueing factory requested?
        else if (recyclerFactorySpec.startsWith("queue")) {
            return readQueueingRecyclerFactory(recyclerFactorySpec, defaultCapacity);
        }

        // Bogus input, bail out.
        else {
            throw new IllegalArgumentException(
                    "invalid recycler factory: " + recyclerFactorySpec);
        }

    }

    private static RecyclerFactory readQueueingRecyclerFactory(
            final String recyclerFactorySpec,
            final int defaultCapacity) {

        // Parse the spec.
        final String queueFactorySpec = recyclerFactorySpec.substring(
                "queue".length() +
                        (recyclerFactorySpec.startsWith("queue:")
                                ? 1
                                : 0));
        final Map<String, StringParameterParser.Value> parsedValues =
                StringParameterParser.parse(
                        queueFactorySpec,
                        new LinkedHashSet<>(Arrays.asList("supplier", "capacity")));

        // Read the supplier path.
        final StringParameterParser.Value supplierValue = parsedValues.get("supplier");
        final String supplierPath;
        if (supplierValue == null || supplierValue instanceof StringParameterParser.NullValue) {
            supplierPath = null;
        } else {
            supplierPath = supplierValue.toString();
        }

        // Read the capacity.
        final StringParameterParser.Value capacityValue = parsedValues.get("capacity");
        final int capacity;
        if (capacityValue == null || capacityValue instanceof StringParameterParser.NullValue) {
            capacity = defaultCapacity;
        } else {
            try {
                capacity = Integer.parseInt(capacityValue.toString());
            } catch (final NumberFormatException error) {
                throw new IllegalArgumentException(
                        "failed reading capacity in queueing recycler " +
                                "factory: " + queueFactorySpec, error);
            }
        }

        // Execute the read spec.
        if (supplierPath == null) {
            return new QueueingRecyclerFactory(getQueueSupplier(capacity));
        }
        return createRecyclerFactory(queueFactorySpec, supplierPath, capacity);

    }

    private static RecyclerFactory createRecyclerFactory(
            final String queueFactorySpec,
            final String supplierPath,
            final int capacity) {
        final int supplierPathSplitterIndex = supplierPath.lastIndexOf('.');
        if (supplierPathSplitterIndex < 0) {
            throw new IllegalArgumentException(
                    "invalid supplier in queueing recycler factory: " +
                            queueFactorySpec);
        }
        final String supplierClassName = supplierPath.substring(0, supplierPathSplitterIndex);
        final String supplierMethodName = supplierPath.substring(supplierPathSplitterIndex + 1);
        try {
            final Class<?> supplierClass = LoaderUtil.loadClass(supplierClassName);
            final Supplier<Queue<Object>> queueSupplier;
            if ("new".equals(supplierMethodName)) {
                final Constructor<?> supplierCtor =
                        supplierClass.getDeclaredConstructor(int.class);
                queueSupplier = () -> {
                    try {
                        @SuppressWarnings("unchecked")
                        final Queue<Object> typedQueue =
                                (Queue<Object>) supplierCtor.newInstance(capacity);
                        return typedQueue;
                    } catch (final Exception error) {
                        throw new RuntimeException(
                                "recycler queue construction failed for factory: " +
                                        queueFactorySpec, error);
                    }
                };
            } else {
                final Method supplierMethod =
                        supplierClass.getMethod(supplierMethodName, int.class);
                queueSupplier = () -> {
                    try {
                        @SuppressWarnings("unchecked")
                        final Queue<Object> typedQueue =
                                (Queue<Object>) supplierMethod.invoke(null, capacity);
                        return typedQueue;
                    } catch (final Exception error) {
                        throw new RuntimeException(
                                "recycler queue construction failed for factory: " +
                                        queueFactorySpec, error);
                    }
                };
            }
            return new QueueingRecyclerFactory(queueSupplier);
        } catch (final Exception error) {
            throw new RuntimeException(
                    "failed executing queueing recycler factory: " +
                            queueFactorySpec, error);
        }
    }

}
