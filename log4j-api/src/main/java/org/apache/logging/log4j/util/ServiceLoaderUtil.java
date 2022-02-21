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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Loads all valid instances of a service.
 */
public class ServiceLoaderUtil {
    private static final Logger LOGGER = StatusLogger.getLogger();

    public static <S> List<S> loadServices(final Class<S> clazz, final Function<ModuleLayer, ServiceLoader<S>> loader,
            final Predicate<S> validator) {
        final List<S> services = new ArrayList<>();
        final ModuleLayer moduleLayer = ServiceLoaderUtil.class.getModule().getLayer();
        if (moduleLayer == null) {
            final ClassLoader[] classLoaders = LoaderUtil.getClassLoaders();
            Throwable throwable = null;
            ClassLoader errorClassLoader = null;
            for (ClassLoader classLoader : classLoaders) {
                try {
                    final ServiceLoader<S> serviceLoader = ServiceLoader.load(clazz, classLoader);
                    for (final S service : serviceLoader) {
                        if (!services.contains(service) && (validator == null || validator.test(service))) {
                            services.add(service);
                        }
                    }
                } catch (final Throwable ex) {
                    if (throwable == null) {
                        throwable = ex;
                        errorClassLoader = classLoader;
                    }
                }
            }
            if (services.size() == 0 && throwable != null) {
                LOGGER.debug("Unable to retrieve provider from ClassLoader {}", errorClassLoader, throwable);
            }
        } else {
            final ServiceLoader<S> serviceLoader = loader.apply(moduleLayer);
            for (final S service : serviceLoader) {
                if (!services.contains(service) && (validator == null || validator.test(service))) {
                    services.add(service);
                }
            }
        }
        return services;
    }
}
