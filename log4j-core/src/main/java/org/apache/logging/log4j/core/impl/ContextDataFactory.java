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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;

/**
 * Factory for creating the StringMap instances used to initialize LogEvents'
 * {@linkplain LogEvent#getContextData() context data}. When context data is
 * {@linkplain ContextDataInjector injected} into the log event, these StringMap
 * instances may be either populated with key-value pairs from the context, or completely replaced altogether.
 * <p>
 * By default returns {@code SortedArrayStringMap} objects. Can be configured by setting system property
 * {@code "log4j2.ContextData"} to the fully qualified class name of a class implementing the
 * {@code StringMap} interface. The class must have a public default constructor, and if possible should also have a
 * public constructor that takes a single {@code int} argument for the initial capacity.
 * </p>
 *
 * @see LogEvent#getContextData()
 * @see ContextDataInjector
 * @see SortedArrayStringMap
 * @since 2.7
 */
public class ContextDataFactory {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final String CLASS_NAME = PropertiesUtil.getProperties().getStringProperty("log4j2.ContextData");
    private static final Class<? extends StringMap> CACHED_CLASS = createCachedClass(CLASS_NAME);
    private static final MethodHandle DEFAULT_CONSTRUCTOR = createDefaultConstructor(CACHED_CLASS);
    private static final MethodHandle INITIAL_CAPACITY_CONSTRUCTOR = createInitialCapacityConstructor(CACHED_CLASS);

    private static final StringMap EMPTY_STRING_MAP = createContextData(1);
    static {
        EMPTY_STRING_MAP.freeze();
    }

    private static Class<? extends StringMap> createCachedClass(final String className) {
        if (className == null) {
            return null;
        }
        try {
            return LoaderUtil.loadClass(className).asSubclass(StringMap.class);
        } catch (final Exception any) {
            return null;
        }
    }

    private static MethodHandle createDefaultConstructor(final Class<? extends StringMap> cachedClass) {
        if (cachedClass == null) {
            return null;
        }
        try {
            return LOOKUP.findConstructor(cachedClass, MethodType.methodType(void.class));
        } catch (final NoSuchMethodException | IllegalAccessException ignored) {
            return null;
        }
    }

    private static MethodHandle createInitialCapacityConstructor(final Class<? extends StringMap> cachedClass) {
        if (cachedClass == null) {
            return null;
        }
        try {
            return LOOKUP.findConstructor(cachedClass, MethodType.methodType(void.class, int.class));
        } catch (final NoSuchMethodException | IllegalAccessException ignored) {
            return null;
        }
    }

    public static StringMap createContextData() {
        if (DEFAULT_CONSTRUCTOR == null) {
            return new SortedArrayStringMap();
        }
        try {
            return (StringMap) DEFAULT_CONSTRUCTOR.invoke();
        } catch (final Throwable ignored) {
            return new SortedArrayStringMap();
        }
    }

    public static StringMap createContextData(final int initialCapacity) {
        if (INITIAL_CAPACITY_CONSTRUCTOR == null) {
            return new SortedArrayStringMap(initialCapacity);
        }
        try {
            return (StringMap) INITIAL_CAPACITY_CONSTRUCTOR.invoke(initialCapacity);
        } catch (final Throwable ignored) {
            return new SortedArrayStringMap(initialCapacity);
        }
    }

    /**
     * An empty pre-frozen StringMap. The returned object may be shared.
     *
     * @return an empty pre-frozen StringMap
     */
    public static StringMap emptyFrozenContextData() {
        return EMPTY_STRING_MAP;
    }
}
