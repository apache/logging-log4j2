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

import java.lang.invoke.MethodHandles.Lookup;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Registry for service instances loaded from {@link ServiceLoader}. This abstracts the differences between using a flat
 * classpath versus a module path.
 *
 * @since 3.0.0
 */
@InternalApi
public final class ServiceRegistry {
    private static final Lazy<ServiceRegistry> INSTANCE = Lazy.relaxed(ServiceRegistry::new);


    /**
     * Returns the singleton ServiceRegistry instance.
     */
    public static ServiceRegistry getInstance() {
        return INSTANCE.get();
    }

    private final Map<Class<?>, List<?>> registry = new ConcurrentHashMap<>();

    private ServiceRegistry() {
    }

    /**
     * Gets service instances loaded from the calling context and any previously registered bundle services. The {@code loaderCallerContext} parameter is
     * provided to ensure the caller can specify which calling function context to
     * load services.
     *
     * @param serviceType         service class
     * @param lookup              MethodHandle lookup created in same module as caller context
     *                            to use for loading services
     * @param validator           if non-null, used to validate service instances,
     *                            removing invalid instances from the returned list
     * @param <S>                 type of service
     * @return loaded service instances
     */
    public <S> List<S> getServices(final Class<S> serviceType, final Lookup lookup, final Predicate<S> validator) {
        return getServices(serviceType, lookup, validator, true);
    }

    /**
     * Set 'verbose' to false if the `StatusLogger` is not available yet.
     */
    <S> List<S> getServices(final Class<S> serviceType, final Lookup lookup, final Predicate<S> validator, final boolean verbose) {
        final List<?> existing = registry.get(serviceType);
        if (existing != null) {
            return Cast.cast(existing);
        }
        final List<S> services = ServiceLoaderUtil.loadServices(serviceType, lookup, false, verbose)
                .filter(validator != null ? validator : unused -> true)
                .collect(Collectors.toList());
        final List<S> oldValue = Cast.cast(registry.putIfAbsent(serviceType, services));
        return oldValue != null ? oldValue : services;
    }
}
