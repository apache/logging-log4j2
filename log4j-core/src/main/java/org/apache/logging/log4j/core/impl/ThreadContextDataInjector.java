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
package org.apache.logging.log4j.core.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.spi.ReadOnlyThreadContextMap;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
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
public class ThreadContextDataInjector {

    /**
     * Default {@code ContextDataInjector} for the legacy {@code Map<String, String>}-based ThreadContext (which is
     * also the ThreadContext implementation used for web applications).
     * <p>
     * This injector always puts key-value pairs into the specified reusable StringMap.
     */
    public static class ForDefaultThreadContextMap implements ContextDataInjector {

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

            final Map<String, String> copy = ThreadContext.getImmutableContext();

            // The DefaultThreadContextMap stores context data in a Map<String, String>.
            // This is a copy-on-write data structure so we are sure ThreadContext changes will not affect our copy.
            // If there are no configuration properties returning a thin wrapper around the copy
            // is faster than copying the elements into the LogEvent's reusable StringMap.
            if (props == null || props.isEmpty()) {
                // this will replace the LogEvent's context data with the returned instance.
                // NOTE: must mark as frozen or downstream components may attempt to modify (UnsupportedOperationEx)
                return copy.isEmpty() ? ContextDataFactory.emptyFrozenContextData() : frozenStringMap(copy);
            }
            // If the list of Properties is non-empty we need to combine the properties and the ThreadContext
            // data. Note that we cannot reuse the specified StringMap: some Loggers may have properties defined
            // and others not, so the LogEvent's context data may have been replaced with an immutable copy from
            // the ThreadContext - this will throw an UnsupportedOperationException if we try to modify it.
            final StringMap result = new JdkMapAdapterStringMap(new HashMap<>(copy));
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
            final JdkMapAdapterStringMap result = new JdkMapAdapterStringMap(copy);
            result.freeze();
            return result;
        }

        @Override
        public ReadOnlyStringMap rawContextData() {
            final ReadOnlyThreadContextMap map = ThreadContext.getThreadContextMap();
            if (map instanceof ReadOnlyStringMap) {
                return (ReadOnlyStringMap) map;
            }
            // note: default ThreadContextMap is null
            final Map<String, String> copy = ThreadContext.getImmutableContext();
            return copy.isEmpty() ? ContextDataFactory.emptyFrozenContextData() : new JdkMapAdapterStringMap(copy);
        }
    }

    /**
     * The {@code ContextDataInjector} used when the ThreadContextMap implementation is a garbage-free
     * StringMap-based data structure.
     * <p>
     * This injector always puts key-value pairs into the specified reusable StringMap.
     */
    public static class ForGarbageFreeThreadContextMap implements ContextDataInjector {
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

            final ReadOnlyStringMap immutableCopy = ThreadContext.getThreadContextMap().getReadOnlyContextData();
            reusable.putAll(immutableCopy);
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
            final StringMap immutableCopy = ThreadContext.getThreadContextMap().getReadOnlyContextData();
            if (props == null || props.isEmpty()) {
                return immutableCopy; // this will replace the LogEvent's context data with the returned instance
            }
            // However, if the list of Properties is non-empty we need to combine the properties and the ThreadContext
            // data. Note that we cannot reuse the specified StringMap: some Loggers may have properties defined
            // and others not, so the LogEvent's context data may have been replaced with an immutable copy from
            // the ThreadContext - this will throw an UnsupportedOperationException if we try to modify it.
            final StringMap result = ContextDataFactory.createContextData(props.size() + immutableCopy.size());
            copyProperties(props, result);
            result.putAll(immutableCopy);
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
}
