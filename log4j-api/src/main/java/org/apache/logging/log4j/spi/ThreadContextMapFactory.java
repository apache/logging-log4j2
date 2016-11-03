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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Constants;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.ProviderUtil;

/**
 * Creates the ThreadContextMap instance used by the ThreadContext.
 * <p>
 * If {@link Constants#ENABLE_THREADLOCALS Log4j can use ThreadLocals}, a garbage-free StringMap-based context map can
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
 * @since 2.7
 */
public final class ThreadContextMapFactory {
    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final String THREAD_CONTEXT_KEY = "log4j2.threadContextMap";
    private static final String GC_FREE_THREAD_CONTEXT_KEY = "log4j2.garbagefree.threadContextMap";

    private ThreadContextMapFactory() {
    }

    public static ThreadContextMap createThreadContextMap() {
        final PropertiesUtil managerProps = PropertiesUtil.getProperties();
        final String threadContextMapName = managerProps.getStringProperty(THREAD_CONTEXT_KEY);
        final ClassLoader cl = ProviderUtil.findClassLoader();
        ThreadContextMap result = null;
        if (threadContextMapName != null) {
            try {
                final Class<?> clazz = cl.loadClass(threadContextMapName);
                if (ThreadContextMap.class.isAssignableFrom(clazz)) {
                    result = (ThreadContextMap) clazz.newInstance();
                }
            } catch (final ClassNotFoundException cnfe) {
                LOGGER.error("Unable to locate configured ThreadContextMap {}", threadContextMapName);
            } catch (final Exception ex) {
                LOGGER.error("Unable to create configured ThreadContextMap {}", threadContextMapName, ex);
            }
        }
        if (result == null && ProviderUtil.hasProviders() && LogManager.getFactory() != null) { //LOG4J2-1658
            final String factoryClassName = LogManager.getFactory().getClass().getName();
            for (final Provider provider : ProviderUtil.getProviders()) {
                if (factoryClassName.equals(provider.getClassName())) {
                    final Class<? extends ThreadContextMap> clazz = provider.loadThreadContextMap();
                    if (clazz != null) {
                        try {
                            result = clazz.newInstance();
                            break;
                        } catch (final Exception e) {
                            LOGGER.error("Unable to locate or load configured ThreadContextMap {}",
                                    provider.getThreadContextMap(), e);
                            result = createDefaultThreadContextMap();
                        }
                    }
                }
            }
        }
        if (result == null) {
            result = createDefaultThreadContextMap();
        }
        return result;
    }

    private static ThreadContextMap createDefaultThreadContextMap() {
        if (Constants.ENABLE_THREADLOCALS) {
            if (PropertiesUtil.getProperties().getBooleanProperty(GC_FREE_THREAD_CONTEXT_KEY)) {
                return new GarbageFreeSortedArrayThreadContextMap();
            }
            return new CopyOnWriteSortedArrayThreadContextMap();
        }
        return new DefaultThreadContextMap(true);
    }
}
