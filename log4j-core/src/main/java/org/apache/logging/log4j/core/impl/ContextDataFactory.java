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

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.CoreProperties.LogEventProperties;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;

/**
 * Factory for creating the StringMap instances used to initialize LogEvents' {@linkplain LogEvent#getContextData()
 * context data}. When context data is {@linkplain ContextDataProvider injected} into the log event, these StringMap
 * instances may be either populated with key-value pairs from the context, or completely replaced altogether.
 * <p>
 *     By default returns {@code SortedArrayStringMap} objects. Can be configured by setting system property
 *     {@code "log4j.logEvent.contextData.type"} to the fully qualified class name of a class implementing the {@code
 *     StringMap} interface. The class must have a public default constructor, and if possible should also have a
 *     public constructor that takes a single {@code int} argument for the initial capacity.
 * </p>
 *
 * @see LogEvent#getContextData()
 * @see SortedArrayStringMap
 * @since 2.7
 */
public final class ContextDataFactory {
    private static final Class<? extends StringMap> CACHED_CLASS = PropertyEnvironment.getGlobal()
            .getProperty(LogEventProperties.class)
            .contextData()
            .type();

    /**
     * Due to GraalVM limitations this can not be a {@link java.lang.invoke.MethodHandle}.
     * <p>
     *     See <a href="https://issues.apache.org/jira/browse/LOG4J2-2649">LOG4J2-2649</a>.
     * </p>
     */
    private static final Constructor<? extends StringMap> DEFAULT_CONSTRUCTOR = createDefaultConstructor();

    private static final Constructor<? extends StringMap> INITIAL_CAPACITY_CONSTRUCTOR =
            createInitialCapacityConstructor();

    private static final StringMap EMPTY_STRING_MAP = createContextData(0);

    static {
        EMPTY_STRING_MAP.freeze();
    }

    private ContextDataFactory() {}

    private static Constructor<? extends StringMap> createDefaultConstructor() {
        if (CACHED_CLASS == null) {
            return null;
        }
        try {
            return CACHED_CLASS.getConstructor();
        } catch (final NoSuchMethodException | IllegalAccessError ignored) {
            return null;
        }
    }

    private static Constructor<? extends StringMap> createInitialCapacityConstructor() {
        if (CACHED_CLASS == null) {
            return null;
        }
        try {
            return CACHED_CLASS.getConstructor(int.class);
        } catch (final NoSuchMethodException | IllegalAccessError ignored) {
            return null;
        }
    }

    public static StringMap createContextData() {
        if (DEFAULT_CONSTRUCTOR == null) {
            return new SortedArrayStringMap();
        }
        try {
            return DEFAULT_CONSTRUCTOR.newInstance();
        } catch (final Throwable ignored) {
            return new SortedArrayStringMap();
        }
    }

    public static StringMap createContextData(final int initialCapacity) {
        if (INITIAL_CAPACITY_CONSTRUCTOR == null) {
            return new SortedArrayStringMap(initialCapacity);
        }
        try {
            return INITIAL_CAPACITY_CONSTRUCTOR.newInstance(initialCapacity);
        } catch (final Throwable ignored) {
            return new SortedArrayStringMap(initialCapacity);
        }
    }

    public static StringMap createContextData(final Map<String, String> context) {
        final StringMap contextData = createContextData(context.size());
        for (final Entry<String, String> entry : context.entrySet()) {
            contextData.putValue(entry.getKey(), entry.getValue());
        }
        return contextData;
    }

    public static StringMap createContextData(final ReadOnlyStringMap readOnlyStringMap) {
        final StringMap contextData = createContextData(readOnlyStringMap.size());
        contextData.putAll(readOnlyStringMap);
        return contextData;
    }

    /**
     * An empty pre-frozen IndexedStringMap. The returned object may be shared.
     *
     * @return an empty pre-frozen IndexedStringMap
     */
    public static StringMap emptyFrozenContextData() {
        return EMPTY_STRING_MAP;
    }
}
