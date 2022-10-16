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

package org.apache.logging.log4j.util3;

import org.apache.logging.log4j.util.Lazy;

import java.lang.invoke.MethodHandles.Lookup;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Registry for service instances loaded from {@link ServiceLoader}. This abstracts the differences between using a flat
 * classpath, a module path, and OSGi modules.
 *
 * @since 3.0.0
 */
public class ServiceRegistry {
    private static final Lazy<ServiceRegistry> INSTANCE = Lazy.relaxed(ServiceRegistry::new);
    @SuppressWarnings("unchecked")
    private static <T> T cast(final Object o) {
        return (T) o;
    }


    /**
     * Returns the singleton ServiceRegistry instance.
     */
    public static ServiceRegistry getInstance() {
        return INSTANCE.get();
    }

    private final Map<Class<?>, List<?>> mainServices = new ConcurrentHashMap<>();
    private final Map<Long, Map<Class<?>, List<?>>> bundleServices = new ConcurrentHashMap<>();

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
        final List<S> services = cast(mainServices.computeIfAbsent(serviceType,
                ignored -> ServiceLoaderUtil.loadServices(serviceType, lookup)
                        .filter(validator != null ? validator : unused -> true)
                        .collect(Collectors.toList())));
        return Stream.concat(services.stream(), bundleServices.values().stream().flatMap(map -> {
            final Stream<S> stream = map.getOrDefault(serviceType, List.of()).stream().map(serviceType::cast);
            return validator != null ? stream.filter(validator) : stream;
        })).distinct().collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Loads and registers services from an OSGi context.
     *
     * @param serviceType       service class
     * @param bundleId          bundle id to load services from
     * @param bundleClassLoader bundle ClassLoader to load services from
     * @param <S>               type of service
     */
    public <S> void loadServicesFromBundle(
            final Class<S> serviceType, final long bundleId, final ClassLoader bundleClassLoader) {
        final List<S> services = new ArrayList<>();
        for (final S service : ServiceLoader.load(serviceType, bundleClassLoader)) {
            services.add(service);
        }
        registerBundleServices(serviceType, bundleId, services);
    }

    /**
     * Registers a list of service instances from an OSGi context.
     *
     * @param serviceType service class
     * @param bundleId    bundle id where services are being registered
     * @param services    list of services to register for this bundle
     * @param <S>         type of service
     */
    public <S> void registerBundleServices(final Class<S> serviceType, final long bundleId, final List<S> services) {
        bundleServices.computeIfAbsent(bundleId, ignored -> new ConcurrentHashMap<>())
                .computeIfAbsent(serviceType, ignored -> new ArrayList<>())
                .addAll(cast(services));
    }

    /**
     * Unregisters all services instances from an OSGi context.
     *
     * @param bundleId bundle id where services are being unregistered
     */
    public void unregisterBundleServices(final long bundleId) {
        bundleServices.remove(bundleId);
    }
}
