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

import static java.util.Objects.requireNonNull;

import aQute.bnd.annotation.baseline.BaselineIgnore;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.logging.log4j.Logger;

/**
 * An utility class to retrieve services in a safe way.
 * <p>
 *     This class should be considered internal.
 * </p>
 * <p>
 *     A common source of {@link ServiceLoader} failures, when running in a multi-classloader environment, is the
 *     presence of multiple classes with the same class name in the same classloader hierarchy. Since {@code
 *     ServiceLoader} retrieves services by class name, it is entirely possible that the registered services don't
 *     extend the required interface and cause an exception to be thrown by {@code ServiceLoader}.
 * </p>
 * <p>
 *     The purpose of this class is to:
 * </p>
 * <ol>
 *     <li>skip faulty services, allowing for a partial retrieval of the good ones,</li>
 *     <li>allow to integrate other sources of services like OSGi services.</li>
 * </ol>
 */
@InternalApi
@BaselineIgnore("2.24.0")
public final class ServiceLoaderUtil {

    private static final int MAX_BROKEN_SERVICES = 8;

    private ServiceLoaderUtil() {}

    /**
     * Retrieves services registered with {@link ServiceLoader}
     * <p>
     *     It ignores the most common service loading errors.
     * </p>
     * @param serviceType The service type to use for OSGi service retrieval.
     * @param serviceLoader The service loader instance to use.
     * @param logger The logger to use to report service failures.
     * @return A stream of all correctly loaded services.
     * @since 2.24.0
     */
    public static <S> Stream<S> safeStream(
            final Class<S> serviceType, final ServiceLoader<? extends S> serviceLoader, final Logger logger) {
        requireNonNull(serviceLoader, "serviceLoader");
        final Collection<Class<?>> classes = new HashSet<>();
        final Stream<S> services =
                StreamSupport.stream(new ServiceLoaderSpliterator<>(serviceType, serviceLoader, logger), false);
        // Caller class may be null
        final Class<?> callerClass = StackLocatorUtil.getCallerClass(2);
        final Stream<S> allServices = OsgiServiceLocator.isAvailable() && callerClass != null
                ? Stream.concat(services, OsgiServiceLocator.loadServices(serviceType, callerClass, logger))
                : services;
        return allServices
                // only the first occurrence of a class
                .filter(service -> classes.add(service.getClass()));
    }

    private static final class ServiceLoaderSpliterator<S> extends Spliterators.AbstractSpliterator<S> {
        private final String serviceName;
        private final Iterator<? extends S> serviceIterator;
        private final Logger logger;

        private ServiceLoaderSpliterator(
                final Class<S> serviceType, final Iterable<? extends S> serviceLoader, final Logger logger) {
            super(Long.MAX_VALUE, ORDERED | NONNULL | IMMUTABLE);
            this.serviceName = serviceType.getName();
            this.serviceIterator = serviceLoader.iterator();
            this.logger = logger;
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
                } catch (final ServiceConfigurationError | LinkageError e) {
                    logger.warn("Unable to load implementation for service {}", serviceName, e);
                } catch (final Exception e) {
                    logger.warn("Unexpected exception  while loading implementation for service {}", serviceName, e);
                    throw e;
                }
            }
            return false;
        }
    }
}
