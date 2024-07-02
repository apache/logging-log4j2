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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.util.ContextDataProvider;
import org.apache.logging.log4j.spi.ReadOnlyThreadContextMap;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.StringMap;

/**
 * {@code ThreadContextDataInjector} contains a number of strategies for copying key-value pairs from the various
 * {@code ThreadContext} map implementations into a {@code StringMap}. In the case of duplicate keys,
 * thread context values overwrite configuration {@code Property} values.
 * <p>
 * This class is no longer directly used by Log4j. It is only present in case it is being overridden by a user.
 * Will be removed in 3.0.0.
 * </p>
 *
 * @see org.apache.logging.log4j.ThreadContext
 * @see Property
 * @see ReadOnlyStringMap
 * @see ContextDataInjector
 * @see ContextDataInjectorFactory
 * @since 2.7
 * @deprecated Use @{link ContextData} instead.
 */
@Deprecated
public class ThreadContextDataInjector {

    private static final Logger LOGGER = StatusLogger.getLogger();

    /**
     * ContextDataProviders loaded via OSGi.
     */
    public static Collection<ContextDataProvider> contextDataProviders = new ProviderQueue();

    /**
     * Previously this method allowed ContextDataProviders to be loaded eagerly, now they
     * are loaded when this class is initialized.
     *
     * @deprecated no-op
     */
    @Deprecated
    public static void initServiceProviders() {}

    /**
     * Default {@code ContextDataInjector} for the legacy {@code Map<String, String>}-based ThreadContext (which is
     * also the ThreadContext implementation used for web applications).
     * <p>
     * This injector always puts key-value pairs into the specified reusable StringMap.
     */
    public static class ForDefaultThreadContextMap implements ContextDataInjector {

        public ForDefaultThreadContextMap() {}

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
            Map<String, String> map = new HashMap<>();
            JdkMapAdapterStringMap stringMap = new JdkMapAdapterStringMap(map, false);
            copyProperties(props, stringMap);
            ContextData.addAll(map);
            stringMap.freeze();
            return stringMap;
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

        public ForGarbageFreeThreadContextMap() {}

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
            ContextData.addAll(reusable);
            return reusable;
        }

        /*
           No longer used.
        */
        @Override
        public ReadOnlyStringMap rawContextData() {
            return ThreadContext.getThreadContextMap().getReadOnlyContextData();
        }
    }

    /**
     * Th
     */
    public static class ForCopyOnWriteThreadContextMap extends ForDefaultThreadContextMap {}

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

    private static class ProviderQueue implements Collection<ContextDataProvider> {
        @Override
        public int size() {
            return ContextData.contextDataProviders.size();
        }

        @Override
        public boolean isEmpty() {
            return ContextData.contextDataProviders.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return ContextData.contextDataProviders.contains(o);
        }

        @Override
        public Iterator<ContextDataProvider> iterator() {
            return new ProviderIterator(ContextData.contextDataProviders.iterator());
        }

        @Override
        public Object[] toArray() {
            return ContextData.contextDataProviders.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return ContextData.contextDataProviders.toArray(a);
        }

        @Override
        public boolean add(ContextDataProvider contextDataProvider) {
            return ContextData.contextDataProviders.add(contextDataProvider);
        }

        @Override
        public boolean remove(Object o) {
            return ContextData.contextDataProviders.remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return ContextData.contextDataProviders.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends ContextDataProvider> c) {
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return ContextData.contextDataProviders.removeAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return ContextData.contextDataProviders.retainAll(c);
        }

        @Override
        public void clear() {
            ContextData.contextDataProviders.clear();
        }
    }

    private static class ProviderIterator implements Iterator<ContextDataProvider> {

        private final Iterator<ContextDataProvider> iter;

        public ProviderIterator(Iterator<ContextDataProvider> iter) {
            this.iter = iter;
        }

        @Override
        public boolean hasNext() {
            return iter.hasNext();
        }

        @Override
        public ContextDataProvider next() {
            ContextDataProvider next = iter.next();
            if (next instanceof ContextDataProvider) {
                return (ContextDataProvider) next;
            } else if (next != null) {
                return new ProviderWrapper(next);
            }
            return null;
        }
    }

    private static class ProviderWrapper implements ContextDataProvider {

        private final ContextDataProvider provider;

        public ProviderWrapper(ContextDataProvider provider) {
            this.provider = provider;
        }

        @Override
        public String get(String key) {
            return provider.get(key);
        }

        @Override
        public int size() {
            return provider.size();
        }

        @Override
        public void addAll(Map<String, String> map) {
            provider.addAll(map);
        }

        @Override
        public void addAll(StringMap map) {
            provider.addAll(map);
        }

        @Override
        public Map<String, String> supplyContextData() {
            return provider.supplyContextData();
        }

        @Override
        public StringMap supplyStringMap() {
            return new JdkMapAdapterStringMap(supplyContextData(), true);
        }
    }
}
