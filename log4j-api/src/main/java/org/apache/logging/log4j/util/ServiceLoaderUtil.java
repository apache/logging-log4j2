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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

public class ServiceLoaderUtil {

    private static Logger getLogger() {
        return StatusLogger.getLogger();
    }

    private ServiceLoaderUtil() {
    }

    /**
     * Retrieves the available services.
     * 
     * Broken services will be ignored.
     * 
     * @param <T>         The service type.
     * @param serviceType The class of the service.
     * @return List of available services.
     */
    public static <T> List<T> loadServices(final Class<T> serviceType) {
        return loadServices(serviceType, null);
    }

    /**
     * Instantiates a service of the required type.
     * 
     * @param <T>         The service type.
     * @param serviceType The class of the service.
     * @return A service of the given type or {@code null} if none is available.
     */
    public static <T> T getService(final Class<T> serviceType) {
        // 1. Check for a system property with the FQCN of serviceType
        String serviceClassName = PropertiesUtil.getProperties().getStringProperty(serviceType.getName());
        if (Strings.isNotEmpty(serviceClassName)) {
            try {
                @SuppressWarnings("unchecked")
                final Class<T> serviceClass = (Class<T>) Class.forName(serviceClassName);
                if (serviceType.isAssignableFrom(serviceClass)) {
                    return LoaderUtil.newInstanceOf(serviceClass);
                }
                getLogger().error("Unable to load service {}: it is not of the required type {}", serviceClass,
                        serviceType);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                    | InvocationTargetException e) {
                getLogger().error("Unable to load service class {}", serviceClassName, e);
            }
        }
        // 2. Use the ServiceLoader:
        final List<T> services = loadServices(serviceType,
                t -> getLogger().info("Unable to load service class for service {}", serviceType.getName(), t));
        if (services.size() > 0) {
            if (services.size() > 1) {
                getLogger().warn("Multiple versions of service {} are available on the classpath: {}", serviceType,
                        services);
            }
            return services.get(0);
        }
        return null;
    }

    static <T> List<T> loadServices(final Class<T> serviceType, final Consumer<Throwable> logger) {
        final List<T> services = new ArrayList<T>();
        for (final ClassLoader loader : LoaderUtil.getClassLoaders()) {
            addServices(serviceType, loader, logger, services);
        }
        return services;
    }

    /**
     * Retrieves the available services of a given type from a single classloader.
     * 
     * @param <T>         The service type.
     * @param serviceType The class of the service.
     * @param classLoader The classloader to use.
     * @param logger      An action to perform for each broken service.
     * @param services    A list to add the services.
     */
    private static <T> void addServices(final Class<T> serviceType, final ClassLoader classLoader,
            final Consumer<Throwable> logger, final Collection<T> services) {
        final Iterator<T> iterator = ServiceLoader.load(serviceType, classLoader).iterator();
        while (iterator.hasNext()) {
            try {
                final T service = iterator.next();
                if (classLoader.equals(service.getClass().getClassLoader())) {
                    services.add(service);
                }
            } catch (ServiceConfigurationError e) {
                if (logger != null) {
                    logger.accept(e);
                }
            }
        }
    }
}
