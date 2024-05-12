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
import aQute.bnd.annotation.spi.ServiceProvider;
import java.util.ServiceLoader;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.context.CopyOnWriteSortedArrayThreadContextMap;
import org.apache.logging.log4j.core.context.GarbageFreeSortedArrayThreadContextMap;
import org.apache.logging.log4j.core.context.StringArrayThreadContextMap;
import org.apache.logging.log4j.core.impl.internal.QueuedScopedContextProvider;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.spi.NoOpThreadContextMap;
import org.apache.logging.log4j.spi.Provider;
import org.apache.logging.log4j.spi.ScopedContextProvider;
import org.apache.logging.log4j.spi.ThreadContextMap;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Constants;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.ServiceLoaderUtil;
import org.jspecify.annotations.NullMarked;

/**
 * Binding for the Log4j API.
 */
@ServiceProvider(value = Provider.class, resolution = Resolution.OPTIONAL)
@ServiceConsumer(value = ScopedContextProvider.class, resolution = Resolution.OPTIONAL)
@NullMarked
public class Log4jProvider extends Provider {

    /**
     * Constant used to disable the {@link ThreadContextMap}.
     * <p>
     *     <strong>Warning:</strong> the value of this constant does not point to a concrete class name.
     * </p>
     * @see #getThreadContextMap
     */
    private static final String NO_OP_CONTEXT_MAP = "NoOp";

    /**
     * Constant used to select a web application-safe implementation of {@link ThreadContextMap}.
     * <p>
     *     This implementation only binds JRE classes to {@link ThreadLocal} variables.
     * </p>
     * <p>
     *     <strong>Warning:</strong> the value of this constant does not point to a concrete class name.
     * </p>
     * @see #getThreadContextMap
     */
    private static final String WEB_APP_CONTEXT_MAP = "WebApp";

    /**
     * Constant used to select a copy-on-write implementation of {@link ThreadContextMap}.
     * <p>
     *     <strong>Warning:</strong> the value of this constant does not point to a concrete class name.
     * </p>
     * @see #getThreadContextMap
     */
    private static final String COPY_ON_WRITE_CONTEXT_MAP = "CopyOnWrite";

    /**
     * Constant used to select a garbage-free implementation of {@link ThreadContextMap}.
     * <p>
     *     This implementation must ensure that common operations don't create new object instances. The drawback is
     *     the necessity to bind custom classes to {@link ThreadLocal} variables.
     * </p>
     * <p>
     *     <strong>Warning:</strong> the value of this constant does not point to a concrete class name.
     * </p>
     * @see #getThreadContextMap
     */
    private static final String GARBAGE_FREE_CONTEXT_MAP = "GarbageFree";

    // Property keys relevant for context map selection
    private static final String DISABLE_CONTEXT_MAP = "log4j2.disableThreadContextMap";
    private static final String DISABLE_THREAD_CONTEXT = "log4j2.disableThreadContext";
    private static final String THREAD_CONTEXT_MAP_PROPERTY = "log4j2.threadContextMap";
    private static final String GC_FREE_THREAD_CONTEXT_PROPERTY = "log4j2.garbagefree.threadContextMap";

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final Lazy<LoggerContextFactory> loggerContextFactoryLazy = Lazy.lazy(Log4jContextFactory::new);
    private final Lazy<ThreadContextMap> threadContextMapLazy = Lazy.lazy(Log4jProvider::createThreadContextMap);

    public Log4jProvider() {
        super(10, CURRENT_VERSION, Log4jContextFactory.class);
    }

    @Override
    public LoggerContextFactory getLoggerContextFactory() {
        return loggerContextFactoryLazy.get();
    }

    @Override
    public ThreadContextMap getThreadContextMapInstance() {
        return threadContextMapLazy.get();
    }

    private static ThreadContextMap createThreadContextMap() {
        // Properties
        final PropertiesUtil props = PropertiesUtil.getProperties();
        if (props.getBooleanProperty(DISABLE_CONTEXT_MAP) || props.getBooleanProperty(DISABLE_THREAD_CONTEXT)) {
            return NoOpThreadContextMap.INSTANCE;
        }
        String threadContextMapClass = props.getStringProperty(THREAD_CONTEXT_MAP_PROPERTY);
        // Default based on properties
        if (threadContextMapClass == null) {
            if (props.getBooleanProperty(GC_FREE_THREAD_CONTEXT_PROPERTY)) {
                threadContextMapClass = GARBAGE_FREE_CONTEXT_MAP;
            } else if (Constants.ENABLE_THREADLOCALS) {
                threadContextMapClass = COPY_ON_WRITE_CONTEXT_MAP;
            } else {
                threadContextMapClass = WEB_APP_CONTEXT_MAP;
            }
        }
        /*
         * The constructors are called explicitly to improve GraalVM support.
         *
         * The class names of the package-private implementations from version 2.23.1 must be recognized even
         * if the class is moved.
         */
        switch (threadContextMapClass) {
            case NO_OP_CONTEXT_MAP:
                return new NoOpThreadContextMap();
            case WEB_APP_CONTEXT_MAP:
                return new StringArrayThreadContextMap();
            case GARBAGE_FREE_CONTEXT_MAP:
            case "org.apache.logging.log4j.spi.GarbageFreeSortedArrayThreadContextMap":
                return new GarbageFreeSortedArrayThreadContextMap();
            case COPY_ON_WRITE_CONTEXT_MAP:
            case "org.apache.logging.log4j.spi.CopyOnWriteSortedArrayThreadContextMap":
                return new CopyOnWriteSortedArrayThreadContextMap();
            default:
                try {
                    return LoaderUtil.newCheckedInstanceOf(threadContextMapClass, ThreadContextMap.class);
                } catch (final Exception e) {
                    LOGGER.error("Unable to create instance of class {}.", threadContextMapClass, e);
                }
        }
        LOGGER.warn("Falling back to {}.", NoOpThreadContextMap.class.getName());
        return NoOpThreadContextMap.INSTANCE;
    }

    @Override
    public ScopedContextProvider getScopedContextProvider() {
        return ServiceLoaderUtil.safeStream(
                        ScopedContextProvider.class,
                        ServiceLoader.load(ScopedContextProvider.class),
                        StatusLogger.getLogger())
                .findFirst()
                .orElse(QueuedScopedContextProvider.INSTANCE);
    }

    // Used in tests
    void resetThreadContextMap() {
        threadContextMapLazy.set(null);
    }
}
