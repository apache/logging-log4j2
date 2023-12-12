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

import static org.apache.logging.log4j.util.LowLevelLogUtil.log;

import aQute.bnd.annotation.spi.ServiceConsumer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Queue;
import java.util.ServiceLoader;
import java.util.concurrent.ArrayBlockingQueue;
import org.apache.logging.log4j.spi.QueueFactory;
import org.apache.logging.log4j.util.Cast;
import org.apache.logging.log4j.util.InternalApi;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.ServiceLoaderUtil;

/**
 * Provides the default {@link QueueFactory} instance.
 *
 * @since 3.0.0
 */
@InternalApi
@ServiceConsumer(QueueFactory.class)
public final class QueueFactories {

    /**
     * The default {@link QueueFactory} instance.
     */
    public static final QueueFactory INSTANCE = findInstance();

    private static QueueFactory findInstance() {
        final ServiceLoader<QueueFactory> serviceLoader =
                ServiceLoader.load(QueueFactory.class, QueueFactory.class.getClassLoader());
        final List<QueueFactory> factories =
                ServiceLoaderUtil.safeStream(serviceLoader).toList();
        if (factories.isEmpty()) {
            return ArrayBlockingQueue::new;
        } else {
            final int factoryCount = factories.size();
            if (factoryCount > 1) {
                log("Log4j was expecting a single `QueueFactory` provider, found:");
                for (int factoryIndex = 0; factoryIndex < factoryCount; factoryIndex++) {
                    log((factoryIndex + 1) + ". `" + factories.get(factoryIndex) + "`");
                }
                log("Log4j will use the first `QueueFactory` provider as the default.");
            }
            return factories.get(0);
        }
    }

    private QueueFactories() {}

    /**
     * Creates a {@link QueueFactory} using the provided supplier.
     * <p>
     * A supplier path must be formatted as follows:
     * <ul>
     * <li>{@code <fully-qualified-class-name>.new} – the class constructor accepting a single {@code int} argument (denoting the capacity) will be used (e.g., {@code org.jctools.queues.MpmcArrayQueue.new})</li>
     * <li>{@code <fully-qualified-class-name>.<static-factory-method>} – the static factory method accepting a single {@code int} argument (denoting the capacity) will be used (e.g., {@code com.acme.Queues.createBoundedQueue})</li>
     * </ul>
     * </p>
     *
     * @param supplierPath a queue supplier path (e.g., {@code org.jctools.queues.MpmcArrayQueue.new}, {@code com.acme.Queues.createBoundedQueue})
     * @return a new {@link QueueFactory} instance
     */
    public static QueueFactory ofSupplier(final String supplierPath) {
        final int supplierPathSplitterIndex = supplierPath.lastIndexOf('.');
        if (supplierPathSplitterIndex < 0) {
            final String message = String.format("invalid queue factory supplier path: `%s`", supplierPath);
            throw new IllegalArgumentException(message);
        }
        final String supplierClassName = supplierPath.substring(0, supplierPathSplitterIndex);
        final String supplierMethodName = supplierPath.substring(supplierPathSplitterIndex + 1);
        try {
            final Class<?> supplierClass = LoaderUtil.loadClass(supplierClassName);
            if ("new".equals(supplierMethodName)) {
                final Constructor<?> supplierCtor = supplierClass.getDeclaredConstructor(int.class);
                return new ConstructorProvidedQueueFactory(supplierCtor);
            } else {
                final Method supplierMethod = supplierClass.getMethod(supplierMethodName, int.class);
                return new StaticMethodProvidedQueueFactory(supplierMethod);
            }
        } catch (final ReflectiveOperationException | LinkageError | SecurityException error) {
            final String message =
                    String.format("failed to create the queue factory using the supplier path `%s`", supplierPath);
            throw new RuntimeException(message, error);
        }
    }

    private static final class ConstructorProvidedQueueFactory implements QueueFactory {

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

    private static final class StaticMethodProvidedQueueFactory implements QueueFactory {

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
