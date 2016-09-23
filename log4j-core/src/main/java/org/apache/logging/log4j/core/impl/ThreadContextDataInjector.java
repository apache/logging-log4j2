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

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.ThreadContextAccess;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.spi.ThreadContextMap;

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
public class ThreadContextDataInjector  {

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
         * @param reusable a {@code StringMap} instance that may be reused to avoid creating temporary objects
         * @return a {@code StringMap} combining configuration properties with thread context data
         */
        @Override
        public StringMap injectContextData(final List<Property> props, final StringMap reusable) {
            // implementation note:
            // The DefaultThreadContextMap stores context data in a Map<String, String>, not in a ReadOnlyStringMap object.
            // Therefore we can populate the specified reusable StringMap, but
            // we need to copy the ThreadContext key-value pairs one by one.
            copyProperties(props, reusable);
            copyThreadContextMap(ThreadContext.getImmutableContext(), reusable);
            return reusable;
        }

        @Override
        public ReadOnlyStringMap rawContextData() {
            final ThreadContextMap map = ThreadContextAccess.getThreadContextMap();
            if (map instanceof ReadOnlyStringMap) {
                return (ReadOnlyStringMap) map;
            }
            final StringMap result = ContextDataFactory.createContextData();
            copyThreadContextMap(ThreadContext.getImmutableContext(), result);
            return result;
        }

        /**
         * Copies key-value pairs from the specified map into the specified {@code StringMap}.
         *
         * @param map map with key-value pairs, may be {@code null}
         * @param result the {@code StringMap} object to add the key-values to. Must be non-{@code null}.
         */
        private static void copyThreadContextMap(final Map<String, String> map, final StringMap result) {
            if (map != null) {
                for (final Map.Entry<String, String> entry : map.entrySet()) {
                    result.putValue(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    /**
     * The {@code ContextDataInjector} used when the ThreadContextMap implementation is a garbage-free
     * StringMap-based data structure.
     * <p>
     * This injector always puts key-value pairs into the specified reusable StringMap.
     */
    public static class ForGarbageFreeMutableThreadContextMap implements ContextDataInjector {
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
            // StringMap. We cannot return the ThreadContext's internal data structure because it will be
            // modified.
            copyProperties(props, reusable);

            final ReadOnlyStringMap immutableCopy = ThreadContextAccess.getThreadContextMap2().getReadOnlyContextData();
            reusable.putAll(immutableCopy);
            return reusable;
        }

        @Override
        public ReadOnlyStringMap rawContextData() {
            return ThreadContextAccess.getThreadContextMap2().getReadOnlyContextData();
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
    public static class ForCopyOnWriteMutableThreadContextMap implements ContextDataInjector {
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
            final StringMap immutableCopy = ThreadContextAccess.getThreadContextMap2().getReadOnlyContextData();
            if (props == null || props.isEmpty()) {
                return immutableCopy; // this will replace the LogEvent's context data with the returned instance
            }
            // However, if the list of Properties is non-empty we need to combine the properties and the ThreadContext
            // data. Note that we cannot reuse the specified StringMap: some Loggers may have properties defined
            // and others not, so the LogEvent's context data may have been replaced with an immutable copy from
            // the ThreadContext - this will throw an UnsupportedOperationException if we try to modify it.
            final StringMap result = ContextDataFactory.createContextData();
            copyProperties(props, result);
            result.putAll(immutableCopy);
            return result;
        }

        @Override
        public ReadOnlyStringMap rawContextData() {
            return ThreadContextAccess.getThreadContextMap2().getReadOnlyContextData();
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
