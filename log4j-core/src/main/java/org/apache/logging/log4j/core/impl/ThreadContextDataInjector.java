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

import aQute.bnd.annotation.Cardinality;
import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.spi.ServiceConsumer;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.util.ContextDataProvider;
import org.apache.logging.log4j.spi.ReadOnlyThreadContextMap;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.ServiceLoaderUtil;
import org.apache.logging.log4j.util.StringMap;

/**
 * {@code ThreadContextDataInjector} contains a number of strategies for copying key-value pairs from the various
 * {@code ThreadContext} map implementations into a {@code StringMap}. In the case of duplicate keys,
 * thread context values overwrite configuration {@code Property} values.
 * <p>
 * These are the default {@code ContextDataInjector} objects returned by the {@link ContextDataInjectorFactory}.
 * </p>
 *
 * @see org.apache.logging.log4j.ThreadContext
 * @see Property
 * @see ReadOnlyStringMap
 * @see ContextDataInjector
 * @see ContextDataInjectorFactory
 * @since 2.7
 */
@ServiceConsumer(
        value = ContextDataProvider.class,
        resolution = Resolution.OPTIONAL,
        cardinality = Cardinality.MULTIPLE)
public class ThreadContextDataInjector {

    private static final Logger LOGGER = StatusLogger.getLogger();

    /**
     * ContextDataProviders loaded via OSGi.
     */
    public static Collection<ContextDataProvider> contextDataProviders = new ConcurrentLinkedDeque<>();

    private static final List<ContextDataProvider> SERVICE_PROVIDERS = getServiceProviders();

    /**
     * Previously this method allowed ContextDataProviders to be loaded eagerly, now they
     * are loaded when this class is initialized.
     *
     * @deprecated no-op
     */
    @Deprecated
    public static void initServiceProviders() {}

    private static List<ContextDataProvider> getServiceProviders() {
        final List<ContextDataProvider> providers = new ArrayList<>();
        ServiceLoaderUtil.loadServices(ContextDataProvider.class, MethodHandles.lookup(), false)
                .forEach(providers::add);
        return Collections.unmodifiableList(providers);
    }

    /**
     * Default {@code ContextDataInjector} for the legacy {@code Map<String, String>}-based ThreadContext (which is
     * also the ThreadContext implementation used for web applications).
     * <p>
     * This injector always puts key-value pairs into the specified reusable StringMap.
     */
    public static class ForDefaultThreadContextMap implements ContextDataInjector {

        private final List<ContextDataProvider> providers;

        public ForDefaultThreadContextMap() {
            providers = getProviders();
        }

        /**
         * Puts key-value pairs from both the specified list of properties as well as the thread context into the
         * specified reusable StringMap.
         *
         * @param props list of configuration properties, may be {@code null}
         * @param ignore a {@code StringMap} instance from the log event
         * @return a {@code StringMap} combining configuration properties with thread context data
         */
        @Override
        public StringMap injectContextData(final List<Property> props, final StringMap ignore) {

            final Map<String, String> copy;

            if (providers.size() == 1) {
                copy = providers.get(0).supplyContextData();
            } else {
                copy = new HashMap<>();
                for (ContextDataProvider provider : providers) {
                    copy.putAll(provider.supplyContextData());
                }
            }

            // The DefaultThreadContextMap stores context data in a Map<String, String>.
            // This is a copy-on-write data structure so we are sure ThreadContext changes will not affect our copy.
            // If there are no configuration properties or providers returning a thin wrapper around the copy
            // is faster than copying the elements into the LogEvent's reusable StringMap.
            if ((props == null || props.isEmpty())) {
                // this will replace the LogEvent's context data with the returned instance.
                // NOTE: must mark as frozen or downstream components may attempt to modify (UnsupportedOperationEx)
                return copy.isEmpty() ? ContextDataFactory.emptyFrozenContextData() : frozenStringMap(copy);
            }
            // If the list of Properties is non-empty we need to combine the properties and the ThreadContext
            // data. Note that we cannot reuse the specified StringMap: some Loggers may have properties defined
            // and others not, so the LogEvent's context data may have been replaced with an immutable copy from
            // the ThreadContext - this will throw an UnsupportedOperationException if we try to modify it.
            final StringMap result = new JdkMapAdapterStringMap(new HashMap<>(copy), false);
            for (int i = 0; i < props.size(); i++) {
                final Property prop = props.get(i);
                if (!copy.containsKey(prop.getName())) {
                    result.putValue(prop.getName(), prop.getValue());
                }
            }
            result.freeze();
            return result;
        }

        private static JdkMapAdapterStringMap frozenStringMap(final Map<String, String> copy) {
            return new JdkMapAdapterStringMap(copy, true);
        }

        @Override
        public ReadOnlyStringMap rawContextData() {
            final ReadOnlyThreadContextMap map = ThreadContext.getThreadContextMap();
            if (map != null) {
                return map.getReadOnlyContextData();
            }
            // note: default ThreadContextMap is null
            final Map<String, String> copy = ThreadContext.getImmutableContext();
            return copy.isEmpty()
                    ? ContextDataFactory.emptyFrozenContextData()
                    : new JdkMapAdapterStringMap(copy, true);
        }
    }

    /**
     * The {@code ContextDataInjector} used when the ThreadContextMap implementation is a garbage-free
     * StringMap-based data structure.
     * <p>
     * This injector always puts key-value pairs into the specified reusable StringMap.
     */
    public static class ForGarbageFreeThreadContextMap implements ContextDataInjector {
        private final List<ContextDataProvider> providers;

        public ForGarbageFreeThreadContextMap() {
            this.providers = getProviders();
        }

        /**
         * Puts key-value pairs from both the specified list of properties as well as the thread context into the
         * specified reusable StringMap.
         *
         * @param props list of configuration properties, may be {@code null}
         * @param reusable a {@code StringMap} instance that may be reused to avoid creating temporary objects
         * @return a {@code StringMap} combining configuration properties with thread context data
         */
        @Override
        public StringMap injectContextData(final List<Property> props, final StringMap reusable) {
            // When the ThreadContext is garbage-free, we must copy its key-value pairs into the specified reusable
            // StringMap. We cannot return the ThreadContext's internal data structure because it may be modified later
            // and such modifications should not be reflected in the log event.
            copyProperties(props, reusable);
            for (int i = 0; i < providers.size(); ++i) {
                reusable.putAll(providers.get(i).supplyStringMap());
            }
            return reusable;
        }

        @Override
        public ReadOnlyStringMap rawContextData() {
            return ThreadContext.getThreadContextMap().getReadOnlyContextData();
        }
    }

    /**
     * The {@code ContextDataInjector} used when the ThreadContextMap implementation is a copy-on-write
     * StringMap-based data structure.
     * <p>
     * If there are no configuration properties, this injector will return the thread context's internal data
     * structure. Otherwise the configuration properties are combined with the thread context key-value pairs into the
     * specified reusable StringMap.
     */
    public static class ForCopyOnWriteThreadContextMap implements ContextDataInjector {
        private final List<ContextDataProvider> providers;

        public ForCopyOnWriteThreadContextMap() {
            this.providers = getProviders();
        }
        /**
         * If there are no configuration properties, this injector will return the thread context's internal data
         * structure. Otherwise the configuration properties are combined with the thread context key-value pairs into the
         * specified reusable StringMap.
         *
         * @param props list of configuration properties, may be {@code null}
         * @param ignore a {@code StringMap} instance from the log event
         * @return a {@code StringMap} combining configuration properties with thread context data
         */
        @Override
        public StringMap injectContextData(final List<Property> props, final StringMap ignore) {
            // If there are no configuration properties we want to just return the ThreadContext's StringMap:
            // it is a copy-on-write data structure so we are sure ThreadContext changes will not affect our copy.
            if (providers.size() == 1 && (props == null || props.isEmpty())) {
                // this will replace the LogEvent's context data with the returned instance
                return providers.get(0).supplyStringMap();
            }
            int count = props == null ? 0 : props.size();
            final StringMap[] maps = new StringMap[providers.size()];
            for (int i = 0; i < providers.size(); ++i) {
                maps[i] = providers.get(i).supplyStringMap();
                count += maps[i].size();
            }
            // However, if the list of Properties is non-empty we need to combine the properties and the ThreadContext
            // data. Note that we cannot reuse the specified StringMap: some Loggers may have properties defined
            // and others not, so the LogEvent's context data may have been replaced with an immutable copy from
            // the ThreadContext - this will throw an UnsupportedOperationException if we try to modify it.
            final StringMap result = ContextDataFactory.createContextData(count);
            copyProperties(props, result);
            for (StringMap map : maps) {
                result.putAll(map);
            }
            return result;
        }

        @Override
        public ReadOnlyStringMap rawContextData() {
            return ThreadContext.getThreadContextMap().getReadOnlyContextData();
        }
    }

    /**
     * Copies key-value pairs from the specified property list into the specified {@code StringMap}.
     *
     * @param properties list of configuration properties, may be {@code null}
     * @param result the {@code StringMap} object to add the key-values to. Must be non-{@code null}.
     */
    public static void copyProperties(final List<Property> properties, final StringMap result) {
        if (properties != null) {
            for (int i = 0; i < properties.size(); i++) {
                final Property prop = properties.get(i);
                result.putValue(prop.getName(), prop.getValue());
            }
        }
    }

    private static List<ContextDataProvider> getProviders() {
        final List<ContextDataProvider> providers =
                new ArrayList<>(contextDataProviders.size() + SERVICE_PROVIDERS.size());
        providers.addAll(contextDataProviders);
        providers.addAll(SERVICE_PROVIDERS);
        return providers;
    }
}
