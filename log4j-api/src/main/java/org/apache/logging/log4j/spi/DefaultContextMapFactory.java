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

package org.apache.logging.log4j.spi;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertyEnvironment;
import org.apache.logging.log4j.util.ReflectionUtil;
import org.apache.logging.log4j.util3.Constants;
import org.apache.logging.log4j.util3.LoaderUtil;
import org.apache.logging.log4j.util3.PropertiesUtil;

/**
 * Creates the ThreadContextMap instance used by the ThreadContext.
 * <p>
 * If {@linkplain Constants#isThreadLocalsEnabled() Log4j can use ThreadLocals}, a garbage-free StringMap-based context map can
 * be installed by setting system property {@code log4j2.garbagefree.threadContextMap} to {@code true}.
 * </p><p>
 * Furthermore, any custom {@code ThreadContextMap} can be installed by setting system property
 * {@code log4j2.threadContextMap} to the fully qualified class name of the class implementing the
 * {@code ThreadContextMap} interface. (Also implement the {@code ReadOnlyThreadContextMap} interface if your custom
 * {@code ThreadContextMap} implementation should be accessible to applications via the
 * {@link ThreadContext#getThreadContextMap()} method.)
 * </p><p>
 * Instead of system properties, the above can also be specified in a properties file named
 * {@code log4j2.component.properties} in the classpath.
 * </p>
 *
 * @see ThreadContextMap
 * @see ReadOnlyThreadContextMap
 * @see org.apache.logging.log4j.ThreadContext
 * @since 3.0.0
 */
public class DefaultContextMapFactory implements ThreadContextMap.Factory {
    private static final String THREAD_CONTEXT_KEY = "log4j2.threadContextMap";
    private static final String GC_FREE_THREAD_CONTEXT_KEY = "log4j2.garbagefree.threadContextMap";
    static final String DISABLE_ALL = "disableThreadContext";
    static final String DISABLE_MAP = "disableThreadContextMap";

    private final String threadContextMapName;
    private final boolean mapEnabled;
    private final boolean garbageFreeEnabled;
    private final boolean inheritableMap;
    private final boolean threadLocalsEnabled;
    private final int initialCapacity;
    private final Logger logger = StatusLogger.getLogger();

    public DefaultContextMapFactory() {
        this(PropertiesUtil.getProperties());
    }

    public DefaultContextMapFactory(final PropertyEnvironment properties) {
        threadContextMapName = properties.getStringProperty(THREAD_CONTEXT_KEY);
        garbageFreeEnabled = properties.getBooleanProperty(GC_FREE_THREAD_CONTEXT_KEY);
        mapEnabled = !(properties.getBooleanProperty(DISABLE_ALL) || properties.getBooleanProperty(DISABLE_MAP));
        inheritableMap = properties.getBooleanProperty(Constants.INHERITABLE_MAP);
        threadLocalsEnabled = properties.getBooleanProperty(Constants.THREAD_LOCALS_ENABLED);
        initialCapacity = properties.getIntegerProperty(Constants.PROPERTY_NAME_INITIAL_CAPACITY, Constants.DEFAULT_INITIAL_CAPACITY);
    }

    @Override
    public ThreadContextMap createThreadContextMap() {
        if (!mapEnabled) {
            return new NoOpThreadContextMap();
        }
        if (threadContextMapName != null) {
            final ClassLoader loader = LoaderUtil.getThreadContextClassLoader();
            try {
                final Class<? extends ThreadContextMap> threadContextMapClass = loader.loadClass(threadContextMapName)
                        .asSubclass(ThreadContextMap.class);
                return ReflectionUtil.instantiate(threadContextMapClass);
            } catch (final ClassNotFoundException e) {
                logger.error("Unable to locate configured ThreadContextMap {}", threadContextMapName);
            } catch (final Exception e) {
                logger.error("Unable to create configured ThreadContextMap {}", threadContextMapName, e);
            }
        }
        if (threadLocalsEnabled) {
            if (garbageFreeEnabled) {
                return new GarbageFreeSortedArrayThreadContextMap(initialCapacity, inheritableMap);
            }
            return new CopyOnWriteSortedArrayThreadContextMap(initialCapacity, inheritableMap);
        }
        return new DefaultThreadContextMap(true, inheritableMap);
    }
}
