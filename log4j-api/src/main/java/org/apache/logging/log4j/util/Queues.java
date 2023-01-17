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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import org.jctools.queues.MpmcArrayQueue;
import org.jctools.queues.MpscArrayQueue;
import org.jctools.queues.SpmcArrayQueue;
import org.jctools.queues.SpscArrayQueue;

/**
 * Provides {@link QueueFactory} and {@link Queue} instances for different use cases. When the
 * <a href="https://jctools.github.io/JCTools/">JCTools</a> library is included at runtime, then
 * the specialized lock free or wait free queues are used from there. Otherwise, {@link ArrayBlockingQueue}
 * is provided as a fallback for thread-safety. Custom implementations of {@link QueueFactory} may also be
 * created via {@link #createQueueFactory(String, String, int)}.
 */
@InternalApi
public enum Queues {
    /**
     * Provides a bounded queue for single-producer/single-consumer usage. Only one thread may offer objects
     * while only one thread may poll for them.
     */
    SPSC(Lazy.lazy(JCToolsQueueFactory.SPSC::load)),
    /**
     * Provides a bounded queue for multi-producer/single-consumer usage. Any thread may offer objects while only
     * one thread may poll for them.
     */
    MPSC(Lazy.lazy(JCToolsQueueFactory.MPSC::load)),
    /**
     * Provides a bounded queue for single-producer/multi-consumer usage. Only one thread may offer objects but
     * any thread may poll for them.
     */
    SPMC(Lazy.lazy(JCToolsQueueFactory.SPMC::load)),
    /**
     * Provides a bounded queue for multi-producer/multi-consumer usage. Any thread may offer objects and any thread
     * may poll for them.
     */
    MPMC(Lazy.lazy(JCToolsQueueFactory.MPMC::load));

    private final Lazy<BoundedQueueFactory> queueFactory;

    Queues(final Lazy<BoundedQueueFactory> queueFactory) {
        this.queueFactory = queueFactory;
    }

    public QueueFactory factory(final int capacity) {
        return new ProxyQueueFactory(queueFactory.get(), capacity);
    }

    public <E> Queue<E> create(final int capacity) {
        return queueFactory.get().create(capacity);
    }

    public static QueueFactory createQueueFactory(final String queueFactorySpec,
                                                  final String supplierPath,
                                                  final int capacity) {
        final int supplierPathSplitterIndex = supplierPath.lastIndexOf('.');
        if (supplierPathSplitterIndex < 0) {
            throw new IllegalArgumentException(
                    "invalid supplier in queue factory: " +
                            queueFactorySpec);
        }
        final String supplierClassName = supplierPath.substring(0, supplierPathSplitterIndex);
        final String supplierMethodName = supplierPath.substring(supplierPathSplitterIndex + 1);
        try {
            final Class<?> supplierClass = LoaderUtil.loadClass(supplierClassName);
            final BoundedQueueFactory queueFactory;
            if ("new".equals(supplierMethodName)) {
                final Constructor<?> supplierCtor =
                        supplierClass.getDeclaredConstructor(int.class);
                queueFactory = new ConstructorProvidedQueueFactory(
                        queueFactorySpec, supplierCtor);
            } else {
                final Method supplierMethod =
                        supplierClass.getMethod(supplierMethodName, int.class);
                queueFactory = new StaticMethodProvidedQueueFactory(
                        queueFactorySpec, supplierMethod);
            }
            return new ProxyQueueFactory(queueFactory, capacity);
        } catch (final ReflectiveOperationException | LinkageError | SecurityException error) {
            throw new RuntimeException(
                    "failed executing queue factory: " +
                            queueFactorySpec, error);
        }
    }

    static class ProxyQueueFactory implements QueueFactory {
        private final BoundedQueueFactory factory;
        private final int capacity;

        ProxyQueueFactory(final BoundedQueueFactory factory, final int capacity) {
            this.factory = factory;
            this.capacity = capacity;
        }

        @Override
        public <E> Queue<E> create() {
            return factory.create(capacity);
        }
    }

    interface BoundedQueueFactory {
        <E> Queue<E> create(final int capacity);
    }

    static class ArrayBlockingQueueFactory implements BoundedQueueFactory {
        @Override
        public <E> Queue<E> create(final int capacity) {
            return new ArrayBlockingQueue<>(capacity);
        }
    }

    enum JCToolsQueueFactory implements BoundedQueueFactory {
        SPSC {
            @Override
            public <E> Queue<E> create(final int capacity) {
                return new SpscArrayQueue<>(capacity);
            }
        },
        MPSC {
            @Override
            public <E> Queue<E> create(final int capacity) {
                return new MpscArrayQueue<>(capacity);
            }
        },
        SPMC {
            @Override
            public <E> Queue<E> create(final int capacity) {
                return new SpmcArrayQueue<>(capacity);
            }
        },
        MPMC {
            @Override
            public <E> Queue<E> create(final int capacity) {
                return new MpmcArrayQueue<>(capacity);
            }
        };

        BoundedQueueFactory load() {
            try {
                // if JCTools is unavailable at runtime, then we'll only find out once we attempt to invoke
                // BoundedQueueFactory::create which is the first time the ClassLoader will try to link the
                // referenced JCTools class causing a NoClassDefFoundError or some other LinkageError potentially.
                // also, test with a large enough capacity to avoid any IllegalArgumentExceptions from trivial queues
                create(16);
                return this;
            } catch (final LinkageError ignored) {
                return new ArrayBlockingQueueFactory();
            }
        }
    }

    static class ConstructorProvidedQueueFactory implements BoundedQueueFactory {
        private final String queueFactorySpec;
        private final Constructor<?> constructor;

        ConstructorProvidedQueueFactory(final String queueFactorySpec, final Constructor<?> constructor) {
            this.queueFactorySpec = queueFactorySpec;
            this.constructor = constructor;
        }

        @Override
        public <E> Queue<E> create(final int capacity) {
            final Constructor<Queue<E>> typedConstructor = Cast.cast(constructor);
            try {
                return typedConstructor.newInstance(capacity);
            } catch (final ReflectiveOperationException e) {
                throw new RuntimeException(
                        "queue construction failed for factory: " +
                                queueFactorySpec, e);
            }
        }
    }

    static class StaticMethodProvidedQueueFactory implements BoundedQueueFactory {
        private final String queueFactorySpec;
        private final Method method;

        StaticMethodProvidedQueueFactory(final String queueFactorySpec, final Method method) {
            this.queueFactorySpec = queueFactorySpec;
            this.method = method;
        }

        @Override
        public <E> Queue<E> create(final int capacity) {
            try {
                return Cast.cast(method.invoke(null, capacity));
            } catch (final ReflectiveOperationException e) {
                throw new RuntimeException(
                        "queue construction failed for factory: " +
                                queueFactorySpec, e);
            }
        }
    }
}
