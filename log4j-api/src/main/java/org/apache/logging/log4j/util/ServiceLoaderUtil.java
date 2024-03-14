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

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.logging.log4j.Logger;

/**
 * Handles {@link ServiceLoader} lookups with better error handling.
 */
@InternalApi
public final class ServiceLoaderUtil {

    private static final int MAX_BROKEN_SERVICES = 8;

    private ServiceLoaderUtil() {}

    public static <S> Stream<S> safeStream(final Class<S> type, @Nullable final ClassLoader classLoader) {
        requireNonNull(type, "type");
        final ClassLoader effectiveLoader = classLoader != null ? classLoader : type.getClassLoader();
        final ServiceLoader<S> serviceLoader = ServiceLoader.load(type, effectiveLoader);
        return safeStream(serviceLoader);
    }

    public static <S> Stream<S> safeStream(final ServiceLoader<S> serviceLoader) {
        requireNonNull(serviceLoader, "serviceLoader");
        final Set<Class<?>> classes = new HashSet<>();
        return StreamSupport.stream(new ServiceLoaderSpliterator<>(serviceLoader), false)
                // only the first occurrence of a class
                .filter(service -> classes.add(service.getClass()));
    }

    // Available in Log4j API 2.x
    public static <S> Stream<S> safeStream(
            final Class<S> ignoredServiceType, final ServiceLoader<S> serviceLoader, final Logger ignoredStatusLogger) {
        return safeStream(serviceLoader);
    }

    private static class ServiceLoaderSpliterator<S> extends Spliterators.AbstractSpliterator<S> {
        private final Iterator<S> serviceIterator;
        private final String serviceName;

        private ServiceLoaderSpliterator(final ServiceLoader<S> serviceLoader) {
            super(Long.MAX_VALUE, ORDERED);
            serviceIterator = serviceLoader.iterator();
            serviceName = serviceLoader.toString();
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
                    LowLevelLogUtil.logException("Unable to load implementation for " + serviceName, e);
                } catch (final Throwable e) {
                    LowLevelLogUtil.logException(
                            "Unexpected exception while loading implementation for " + serviceName, e);
                    throw e;
                }
            }
            return false;
        }
    }
}
