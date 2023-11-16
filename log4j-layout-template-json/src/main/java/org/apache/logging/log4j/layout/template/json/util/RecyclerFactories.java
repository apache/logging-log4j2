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
package org.apache.logging.log4j.layout.template.json.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Supplier;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.util.LoaderUtil;
import org.jctools.queues.MpmcArrayQueue;

public final class RecyclerFactories {

    private RecyclerFactories() {}

    private static final String JCTOOLS_QUEUE_CLASS_SUPPLIER_PATH = "org.jctools.queues.MpmcArrayQueue.new";

    private static final boolean JCTOOLS_QUEUE_CLASS_AVAILABLE = isJctoolsQueueClassAvailable();

    private static boolean isJctoolsQueueClassAvailable() {
        try {
            final String className = JCTOOLS_QUEUE_CLASS_SUPPLIER_PATH.replaceAll("\\.new$", "");
            LoaderUtil.loadClass(className);
            return true;
        } catch (final ClassNotFoundException ignored) {
            return false;
        }
    }

    public static RecyclerFactory ofSpec(final String recyclerFactorySpec) {

        // Determine the default capacity.
        final int defaultCapacity = Math.max(2 * Runtime.getRuntime().availableProcessors() + 1, 8);

        // TLA-, MPMC-, or ABQ-based queueing factory -- if nothing is specified.
        if (recyclerFactorySpec == null) {
            if (Constants.ENABLE_THREADLOCALS) {
                return ThreadLocalRecyclerFactory.getInstance();
            } else {
                final Supplier<Queue<Object>> queueSupplier = JCTOOLS_QUEUE_CLASS_AVAILABLE
                        ? () -> new MpmcArrayQueue<>(defaultCapacity)
                        : () -> new ArrayBlockingQueue<>(defaultCapacity);
                return new QueueingRecyclerFactory(queueSupplier);
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
            throw new IllegalArgumentException("invalid recycler factory: " + recyclerFactorySpec);
        }
    }

    private static RecyclerFactory readQueueingRecyclerFactory(
            final String recyclerFactorySpec, final int defaultCapacity) {

        // Parse the spec.
        final String queueFactorySpec =
                recyclerFactorySpec.substring("queue".length() + (recyclerFactorySpec.startsWith("queue:") ? 1 : 0));
        final Map<String, StringParameterParser.Value> parsedValues = StringParameterParser.parse(
                queueFactorySpec, new LinkedHashSet<>(Arrays.asList("supplier", "capacity")));

        // Read the supplier path.
        final StringParameterParser.Value supplierValue = parsedValues.get("supplier");
        final String supplierPath;
        if (supplierValue == null || supplierValue instanceof StringParameterParser.NullValue) {
            supplierPath = JCTOOLS_QUEUE_CLASS_AVAILABLE
                    ? JCTOOLS_QUEUE_CLASS_SUPPLIER_PATH
                    : "java.util.concurrent.ArrayBlockingQueue.new";
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
                capacity = Integers.parseInt(capacityValue.toString());
            } catch (final NumberFormatException error) {
                throw new IllegalArgumentException(
                        "failed reading capacity in queueing recycler " + "factory: " + queueFactorySpec, error);
            }
        }

        // Execute the read spec.
        return createRecyclerFactory(queueFactorySpec, supplierPath, capacity);
    }

    private static RecyclerFactory createRecyclerFactory(
            final String queueFactorySpec, final String supplierPath, final int capacity) {
        final int supplierPathSplitterIndex = supplierPath.lastIndexOf('.');
        if (supplierPathSplitterIndex < 0) {
            throw new IllegalArgumentException("invalid supplier in queueing recycler factory: " + queueFactorySpec);
        }
        final String supplierClassName = supplierPath.substring(0, supplierPathSplitterIndex);
        final String supplierMethodName = supplierPath.substring(supplierPathSplitterIndex + 1);
        try {
            final Class<?> supplierClass = LoaderUtil.loadClass(supplierClassName);
            final Supplier<Queue<Object>> queueSupplier;
            if ("new".equals(supplierMethodName)) {
                final Constructor<?> supplierCtor = supplierClass.getDeclaredConstructor(int.class);
                queueSupplier = () -> {
                    try {
                        @SuppressWarnings("unchecked")
                        final Queue<Object> typedQueue = (Queue<Object>) supplierCtor.newInstance(capacity);
                        return typedQueue;
                    } catch (final Exception error) {
                        throw new RuntimeException(
                                "recycler queue construction failed for factory: " + queueFactorySpec, error);
                    }
                };
            } else {
                final Method supplierMethod = supplierClass.getMethod(supplierMethodName, int.class);
                queueSupplier = () -> {
                    try {
                        @SuppressWarnings("unchecked")
                        final Queue<Object> typedQueue = (Queue<Object>) supplierMethod.invoke(null, capacity);
                        return typedQueue;
                    } catch (final Exception error) {
                        throw new RuntimeException(
                                "recycler queue construction failed for factory: " + queueFactorySpec, error);
                    }
                };
            }
            return new QueueingRecyclerFactory(queueSupplier);
        } catch (final Exception error) {
            throw new RuntimeException("failed executing queueing recycler factory: " + queueFactorySpec, error);
        }
    }
}
