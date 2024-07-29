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
package org.apache.logging.log4j.core.impl;

import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.spi.ServiceConsumer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.ContextDataProvider;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.ServiceLoaderUtil;
import org.apache.logging.log4j.util.StringMap;

/**
 * General purpose utility class for accessing data accessible through ContextDataProviders.
 */
@ServiceConsumer(value = ContextDataProvider.class, resolution = Resolution.OPTIONAL)
public final class ContextData {

    private static final Logger LOGGER = StatusLogger.getLogger();
    /**
     * ContextDataProviders loaded via OSGi.
     */
    public static Collection<ContextDataProvider> contextDataProviders = new ConcurrentLinkedDeque<>();

    private static final List<ContextDataProvider> SERVICE_PROVIDERS = getServiceProviders();

    private ContextData() {}

    private static List<ContextDataProvider> getServiceProviders() {
        final List<ContextDataProvider> providers = new ArrayList<>();
        ServiceLoaderUtil.safeStream(
                        ContextDataProvider.class,
                        ServiceLoader.load(ContextDataProvider.class, ContextData.class.getClassLoader()),
                        LOGGER)
                .forEach(providers::add);
        return Collections.unmodifiableList(providers);
    }

    public static void addProvider(final ContextDataProvider provider) {
        contextDataProviders.add(provider);
    }

    private static List<ContextDataProvider> getProviders() {
        final List<ContextDataProvider> providers =
                new ArrayList<>(contextDataProviders.size() + SERVICE_PROVIDERS.size());
        providers.addAll(contextDataProviders);
        providers.addAll(SERVICE_PROVIDERS);
        return providers;
    }

    public static int size() {
        final List<ContextDataProvider> providers = getProviders();
        final AtomicInteger count = new AtomicInteger(0);
        providers.forEach((provider) -> count.addAndGet(provider.size()));
        return count.get();
    }

    /**
     * Populates the provided StringMap with data from the Context.
     * @param stringMap the StringMap to contain the results.
     */
    public static void addAll(final StringMap stringMap) {
        final List<ContextDataProvider> providers = getProviders();
        providers.forEach((provider) -> provider.addAll(stringMap));
    }

    /**
     * Populates the provided Map with data from the Context.
     * @param map the Map to contain the results.
     * @return the Map. Useful for chaining operations.
     */
    public static Map<String, String> addAll(final Map<String, String> map) {
        final List<ContextDataProvider> providers = getProviders();
        providers.forEach((provider) -> provider.addAll(map));
        return map;
    }

    public static String getValue(final String key) {
        final List<ContextDataProvider> providers = getProviders();
        for (final ContextDataProvider provider : providers) {
            final String value = provider.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
