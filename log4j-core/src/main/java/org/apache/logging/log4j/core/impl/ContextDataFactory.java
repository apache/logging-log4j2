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

import java.lang.reflect.Constructor;

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
    private static final String CLASS_NAME = PropertiesUtil.getProperties().getStringProperty("log4j2.ContextData");
    private static final Class<?> CACHED_CLASS = createCachedClass(CLASS_NAME);
    private static final Constructor<?> CACHED_CONSTRUCTOR = createCachedConstructor(CACHED_CLASS);

    private static final StringMap EMPTY_STRING_MAP = createContextData(1);
    static {
        EMPTY_STRING_MAP.freeze();
    }

    private static Class<?> createCachedClass(final String className) {
        if (className == null) {
            return null;
        }
        try {
            return LoaderUtil.loadClass(className);
        } catch (final Exception any) {
            return null;
        }
    }

    private static Constructor<?> createCachedConstructor(final Class<?> cachedClass) {
        if (cachedClass == null) {
            return null;
        }
        try {
            return cachedClass.getDeclaredConstructor(int.class);
        } catch (final Exception any) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static StringMap createContextData() {
        if (CACHED_CLASS == null) {
            return new SortedArrayStringMap();
        }
        try {
            return (StringMap) CACHED_CLASS.newInstance();
        } catch (final Exception any) {
            return new SortedArrayStringMap();
        }
    }

    @SuppressWarnings("unchecked")
    public static StringMap createContextData(final int initialCapacity) {
        if (CACHED_CONSTRUCTOR == null) {
            return new SortedArrayStringMap(initialCapacity);
        }
        try {
            return (StringMap) CACHED_CONSTRUCTOR.newInstance(initialCapacity);
        } catch (final Exception any) {
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
