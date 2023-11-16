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

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * This class should be considered internal.
 */
@InternalApi
public final class ServiceLoaderUtil {

    private static final int MAX_BROKEN_SERVICES = 8;

    private ServiceLoaderUtil() {}

    /**
     * Retrieves the available services from the caller's classloader.
     *
     * Broken services will be ignored.
     *
     * @param <T>         The service type.
     * @param serviceType The class of the service.
     * @param lookup      The calling class data.
     * @return A stream of service instances.
     */
    public static <T> Stream<T> loadServices(final Class<T> serviceType, final Lookup lookup) {
        return loadServices(serviceType, lookup, false);
    }

    /**
     * Retrieves the available services from the caller's classloader and possibly
     * the thread context classloader.
     *
     * Broken services will be ignored.
     *
     * @param <T>         The service type.
     * @param serviceType The class of the service.
     * @param lookup      The calling class data.
     * @param useTccl     If true the thread context classloader will also be used.
     * @return A stream of service instances.
     */
    public static <T> Stream<T> loadServices(final Class<T> serviceType, final Lookup lookup, final boolean useTccl) {
        return loadServices(serviceType, lookup, useTccl, true);
    }

    static <T> Stream<T> loadServices(
            final Class<T> serviceType, final Lookup lookup, final boolean useTccl, final boolean verbose) {
        final ClassLoader classLoader = lookup.lookupClass().getClassLoader();
        Stream<T> services = loadClassloaderServices(serviceType, lookup, classLoader, verbose);
        if (useTccl) {
            final ClassLoader contextClassLoader = LoaderUtil.getThreadContextClassLoader();
            if (contextClassLoader != classLoader) {
                services = Stream.concat(
                        services, loadClassloaderServices(serviceType, lookup, contextClassLoader, verbose));
            }
        }
        if (OsgiServiceLocator.isAvailable()) {
            services = Stream.concat(services, OsgiServiceLocator.loadServices(serviceType, lookup, verbose));
        }
        final Set<Class<?>> classes = new HashSet<>();
        // only the first occurrence of a class
        return services.filter(service -> classes.add(service.getClass()));
    }

    static <T> Stream<T> loadClassloaderServices(
            final Class<T> serviceType, final Lookup lookup, final ClassLoader classLoader, final boolean verbose) {
        return StreamSupport.stream(new ServiceLoaderSpliterator<T>(serviceType, lookup, classLoader, verbose), false);
    }

    static <T> Iterable<T> callServiceLoader(
            final Lookup lookup, final Class<T> serviceType, final ClassLoader classLoader, final boolean verbose) {
        try {
            // Creates a lambda in the caller's domain that calls `ServiceLoader`
            final MethodHandle loadHandle = lookup.findStatic(
                    ServiceLoader.class,
                    "load",
                    MethodType.methodType(ServiceLoader.class, Class.class, ClassLoader.class));
            final CallSite callSite = LambdaMetafactory.metafactory(
                    lookup,
                    "run",
                    MethodType.methodType(PrivilegedAction.class, Class.class, ClassLoader.class),
                    MethodType.methodType(Object.class),
                    loadHandle,
                    MethodType.methodType(ServiceLoader.class));
            final PrivilegedAction<ServiceLoader<T>> action = (PrivilegedAction<ServiceLoader<T>>)
                    callSite.getTarget() //
                            .bindTo(serviceType)
                            .bindTo(classLoader)
                            .invoke();
            final ServiceLoader<T> serviceLoader;
            if (System.getSecurityManager() == null) {
                serviceLoader = action.run();
            } else {
                final MethodHandle privilegedHandle = lookup.findStatic(
                        AccessController.class,
                        "doPrivileged",
                        MethodType.methodType(Object.class, PrivilegedAction.class));
                serviceLoader = (ServiceLoader<T>) privilegedHandle.invoke(action);
            }
            return serviceLoader;
        } catch (Throwable e) {
            if (verbose) {
                StatusLogger.getLogger().error("Unable to load services for service {}", serviceType, e);
            }
        }
        return Collections.emptyList();
    }

    private static class ServiceLoaderSpliterator<S> implements Spliterator<S> {

        private final Iterator<S> serviceIterator;
        private final Logger logger;
        private final String serviceName;

        public ServiceLoaderSpliterator(
                final Class<S> serviceType, final Lookup lookup, final ClassLoader classLoader, final boolean verbose) {
            this.serviceIterator =
                    callServiceLoader(lookup, serviceType, classLoader, verbose).iterator();
            this.logger = verbose ? StatusLogger.getLogger() : null;
            this.serviceName = serviceType.toString();
        }

        @Override
        public boolean tryAdvance(final Consumer<? super S> action) {
            int i = MAX_BROKEN_SERVICES;
            while (i-- > 0) {
                try {
                    if (serviceIterator.hasNext()) {
                        action.accept(serviceIterator.next());
                        return true;
                    }
                } catch (ServiceConfigurationError | LinkageError e) {
                    if (logger != null) {
                        logger.warn("Unable to load service class for service {}", serviceName, e);
                    }
                } catch (Throwable e) {
                    if (logger != null) {
                        logger.warn("Unable to load service class for service {}", serviceName, e);
                    }
                    throw e;
                }
            }
            return false;
        }

        @Override
        public Spliterator<S> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            return NONNULL | IMMUTABLE;
        }
    }
}
