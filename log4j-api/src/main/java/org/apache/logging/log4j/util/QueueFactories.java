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
package org.apache.logging.log4j.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import org.jctools.queues.MpmcArrayQueue;
import org.jctools.queues.SpscArrayQueue;

/**
 * Provides {@link QueueFactory} and {@link Queue} instances for different use cases.
 * <p>
 * Implementations provided by <a href="https://jctools.github.io/JCTools/">JCTools</a> will be preferred, if available at runtime.
 * Otherwise, {@link ArrayBlockingQueue} will be used.
 * </p>
 *
 * @since 3.0.0
 */
@InternalApi
public enum QueueFactories {

    /**
     * Provides a bounded queue for single-producer/single-consumer usage.
     */
    SPSC(Lazy.lazy(JCToolsQueueFactory.SPSC::load)),

    /**
     * Provides a bounded queue for multi-producer/multi-consumer usage.
     */
    MPMC(Lazy.lazy(JCToolsQueueFactory.MPMC::load));

    private final Lazy<BoundedQueueFactory> queueFactory;

    QueueFactories(final Lazy<BoundedQueueFactory> queueFactory) {
        this.queueFactory = queueFactory;
    }

    public QueueFactory factory(final int capacity) {
        return new ProxyQueueFactory(queueFactory.get(), capacity);
    }

    public <E> Queue<E> create(final int capacity) {
        return queueFactory.get().create(capacity);
    }

    /**
     * Creates a {@link QueueFactory} producing queues of provided capacity from the provided supplier.
     * <p>
     * A supplier path must be formatted as follows:
     * <ul>
     * <li>{@code <fully-qualified-class-name>.new} – the class constructor accepting a single {@code int} argument (denoting the capacity) will be used (e.g., {@code org.jctools.queues.MpmcArrayQueue.new})</li>
     * <li>{@code <fully-qualified-class-name>.<static-factory-method>} – the static factory method accepting a single {@code int} argument (denoting the capacity) will be used (e.g., {@code com.acme.Queues.createBoundedQueue})</li>
     * </ul>
     * </p>
     *
     * @param supplierPath a queue supplier path (e.g., {@code org.jctools.queues.MpmcArrayQueue.new}, {@code com.acme.Queues.createBoundedQueue})
     * @param capacity the capacity that will be passed to the queue supplier
     * @return a new {@link QueueFactory} instance
     */
    public static QueueFactory createQueueFactory(final String supplierPath, final int capacity) {
        final int supplierPathSplitterIndex = supplierPath.lastIndexOf('.');
        if (supplierPathSplitterIndex < 0) {
            final String message = String.format("invalid queue factory supplier path: `%s`", supplierPath);
            throw new IllegalArgumentException(message);
        }
        final String supplierClassName = supplierPath.substring(0, supplierPathSplitterIndex);
        final String supplierMethodName = supplierPath.substring(supplierPathSplitterIndex + 1);
        try {
            final Class<?> supplierClass = LoaderUtil.loadClass(supplierClassName);
            final BoundedQueueFactory queueFactory;
            if ("new".equals(supplierMethodName)) {
                final Constructor<?> supplierCtor = supplierClass.getDeclaredConstructor(int.class);
                queueFactory = new ConstructorProvidedQueueFactory(supplierCtor);
            } else {
                final Method supplierMethod = supplierClass.getMethod(supplierMethodName, int.class);
                queueFactory = new StaticMethodProvidedQueueFactory(supplierMethod);
            }
            return new ProxyQueueFactory(queueFactory, capacity);
        } catch (final ReflectiveOperationException | LinkageError | SecurityException error) {
            final String message =
                    String.format("failed to create the queue factory using the supplier path `%s`", supplierPath);
            throw new RuntimeException(message, error);
        }
    }

    private static final class ProxyQueueFactory implements QueueFactory {

        private final BoundedQueueFactory factory;

        private final int capacity;

        private ProxyQueueFactory(final BoundedQueueFactory factory, final int capacity) {
            this.factory = factory;
            this.capacity = capacity;
        }

        @Override
        public <E> Queue<E> create() {
            return factory.create(capacity);
        }
    }

    @FunctionalInterface
    private interface BoundedQueueFactory {

        <E> Queue<E> create(final int capacity);
    }

    private static final class ArrayBlockingQueueFactory implements BoundedQueueFactory {

        private static final ArrayBlockingQueueFactory INSTANCE = new ArrayBlockingQueueFactory();

        private ArrayBlockingQueueFactory() {}

        @Override
        public <E> Queue<E> create(final int capacity) {
            return new ArrayBlockingQueue<>(capacity);
        }
    }

    private enum JCToolsQueueFactory implements BoundedQueueFactory {
        SPSC {
            @Override
            public <E> Queue<E> create(final int capacity) {
                return new SpscArrayQueue<>(capacity);
            }
        },

        MPMC {
            @Override
            public <E> Queue<E> create(final int capacity) {
                return new MpmcArrayQueue<>(capacity);
            }
        };

        private BoundedQueueFactory load() {
            try {
                // Test with a large enough capacity to avoid any `IllegalArgumentExceptions` from trivial queues
                create(16);
                return this;
            } catch (final LinkageError ignored) {
                return ArrayBlockingQueueFactory.INSTANCE;
            }
        }
    }

    private static final class ConstructorProvidedQueueFactory implements BoundedQueueFactory {

        private final Constructor<?> constructor;

        private ConstructorProvidedQueueFactory(final Constructor<?> constructor) {
            this.constructor = constructor;
        }

        @Override
        public <E> Queue<E> create(final int capacity) {
            final Constructor<Queue<E>> typedConstructor = Cast.cast(constructor);
            try {
                return typedConstructor.newInstance(capacity);
            } catch (final ReflectiveOperationException error) {
                throw new RuntimeException("queue construction failure", error);
            }
        }
    }

    private static final class StaticMethodProvidedQueueFactory implements BoundedQueueFactory {

        private final Method method;

        private StaticMethodProvidedQueueFactory(final Method method) {
            this.method = method;
        }

        @Override
        public <E> Queue<E> create(final int capacity) {
            try {
                return Cast.cast(method.invoke(null, capacity));
            } catch (final ReflectiveOperationException error) {
                throw new RuntimeException("queue construction failure", error);
            }
        }
    }
}
