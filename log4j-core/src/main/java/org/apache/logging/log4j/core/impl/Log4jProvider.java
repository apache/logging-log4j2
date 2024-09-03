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
import aQute.bnd.annotation.spi.ServiceProvider;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.context.internal.GarbageFreeSortedArrayThreadContextMap;
import org.apache.logging.log4j.spi.DefaultThreadContextMap;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.spi.NoOpThreadContextMap;
import org.apache.logging.log4j.spi.Provider;
import org.apache.logging.log4j.spi.ThreadContextMap;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.jspecify.annotations.NullMarked;

/**
 * Binding for the Log4j API.
 */
@ServiceProvider(value = Provider.class, resolution = Resolution.OPTIONAL)
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

    // Name of the context map implementations
    private static final String WEB_APP_CLASS_NAME = "org.apache.logging.log4j.spi.DefaultThreadContextMap";
    private static final String GARBAGE_FREE_CLASS_NAME =
            "org.apache.logging.log4j.core.context.internal.GarbageFreeSortedArrayThreadContextMap";

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final Lazy<LoggerContextFactory> loggerContextFactoryLazy = Lazy.lazy(Log4jContextFactory::new);
    private final Lazy<ThreadContextMap> threadContextMapLazy = Lazy.lazy(this::createThreadContextMap);

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

    private ThreadContextMap createThreadContextMap() {
        // Properties
        final PropertiesUtil props = PropertiesUtil.getProperties();
        if (props.getBooleanProperty(DISABLE_CONTEXT_MAP) || props.getBooleanProperty(DISABLE_THREAD_CONTEXT)) {
            return NoOpThreadContextMap.INSTANCE;
        }
        String threadContextMapClass = props.getStringProperty(THREAD_CONTEXT_MAP_PROPERTY);
        // Default based on properties
        if (threadContextMapClass == null) {
            threadContextMapClass = props.getBooleanProperty(GC_FREE_THREAD_CONTEXT_PROPERTY)
                    ? GARBAGE_FREE_CONTEXT_MAP
                    : WEB_APP_CONTEXT_MAP;
        }
        /*
         * The constructors are called explicitly to improve GraalVM support.
         *
         * The class names of the package-private implementations from version 2.23.1 must be recognized even
         * if the class is moved.
         */
        switch (threadContextMapClass) {
            case NO_OP_CONTEXT_MAP:
                return NoOpThreadContextMap.INSTANCE;
            case WEB_APP_CONTEXT_MAP:
            case WEB_APP_CLASS_NAME:
                return new DefaultThreadContextMap();
                // Old FQCN of the garbage-free context map
            case "org.apache.logging.log4j.spi.GarbageFreeSortedArrayThreadContextMap":
            case GARBAGE_FREE_CONTEXT_MAP:
            case GARBAGE_FREE_CLASS_NAME:
                return new GarbageFreeSortedArrayThreadContextMap();
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

    // Used in tests
    void resetThreadContextMap() {
        threadContextMapLazy.set(null);
    }
}
