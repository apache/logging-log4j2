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
import org.apache.logging.log4j.core.impl.CoreProperties.ThreadContextProperties;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.spi.DefaultThreadContextMap;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.spi.NoOpThreadContextMap;
import org.apache.logging.log4j.spi.Provider;
import org.apache.logging.log4j.spi.ThreadContextMap;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Constants;
import org.apache.logging.log4j.util.LoaderUtil;
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

    // Name of the context map implementations
    private static final String WEB_APP_CLASS_NAME = "org.apache.logging.log4j.spi.DefaultThreadContextMap";
    private static final String GARBAGE_FREE_CLASS_NAME =
            "org.apache.logging.log4j.core.context.internal.GarbageFreeSortedArrayThreadContextMap";

    private static final Logger LOGGER = StatusLogger.getLogger();

    final ConfigurableInstanceFactory instanceFactory;

    public Log4jProvider() {
        this(DI.createInitializedFactory());
    }

    @Inject
    public Log4jProvider(final ConfigurableInstanceFactory instanceFactory) {
        super(10, CURRENT_VERSION, Log4jContextFactory.class);
        this.instanceFactory = instanceFactory;
        instanceFactory.registerBinding(Key.forClass(Provider.class), () -> this);
        instanceFactory.registerBinding(Key.forClass(Log4jProvider.class), () -> this);
        instanceFactory.registerBinding(Key.forClass(ThreadContextMap.class), this::createThreadContextMap);
    }

    @Override
    public LoggerContextFactory getLoggerContextFactory() {
        return instanceFactory.getInstance(Key.forClass(LoggerContextFactory.class));
    }

    @Override
    public String getThreadContextMap() {
        final PropertyEnvironment environment = instanceFactory.getInstance(PropertyEnvironment.class);
        final ThreadContextProperties threadContext = environment.getProperty(ThreadContextProperties.class);
        if (threadContext.enable() && threadContext.map().enable()) {
            if (threadContext.map().type() != null) {
                return threadContext.map().type();
            }
            return Constants.ENABLE_THREADLOCALS && threadContext.map().garbageFree()
                    ? GARBAGE_FREE_CONTEXT_MAP
                    : WEB_APP_CONTEXT_MAP;
        }
        return NO_OP_CONTEXT_MAP;
    }

    @Override
    public ThreadContextMap getThreadContextMapInstance() {
        return instanceFactory.getInstance(Key.forClass(ThreadContextMap.class));
    }

    private ThreadContextMap createThreadContextMap() {
        final String threadContextMapClass = getThreadContextMap();
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
        Key<ThreadContextMap> key = Key.forClass(ThreadContextMap.class);
        instanceFactory.removeBinding(key);
        instanceFactory.registerBinding(key, this::createThreadContextMap);
    }
}
