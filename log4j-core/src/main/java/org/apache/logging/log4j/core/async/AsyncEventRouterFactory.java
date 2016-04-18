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
package org.apache.logging.log4j.core.async;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * Creates {@link AsyncEventRouter} instances based on user-specified system properties. The {@code AsyncEventRouter}
 * created by this factory is used in AsyncLogger, AsyncLoggerConfig and AsyncAppender
 * to control if events are logged in the current thread, the background thread, or discarded.
 * <p>
 * Property {@code "log4j2.AsyncEventRouter"} controls the routing behaviour. If this property is not specified or has
 * value {@code "Default"}, this factory creates {@link DefaultAsyncEventRouter} objects.
 * </p> <p>
 * If this property has value {@code "Discard"}, this factory creates {@link DiscardingAsyncEventRouter} objects.
 * By default, this router discards events of level {@code INFO}, {@code DEBUG} and {@code TRACE} if the queue is full.
 * This can be adjusted with property {@code "log4j2.DiscardThreshold"} (name of the level at which to start
 * discarding).
 * </p> <p>
 * For any other value, this
 * factory interprets the value as the fully qualified name of a class implementing the {@link AsyncEventRouter}
 * interface. The class must have a default constructor.
 * </p>
 *
 * @since 2.6
 */
public class AsyncEventRouterFactory {
    static final String PROPERTY_NAME_ASYNC_EVENT_ROUTER = "log4j2.AsyncEventRouter";
    static final String PROPERTY_VALUE_DEFAULT_ASYNC_EVENT_ROUTER = "Default";
    static final String PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER = "Discard";
    static final String PROPERTY_NAME_DISCARDING_THRESHOLD_LEVEL = "log4j2.DiscardThreshold";

    private static final Logger LOGGER = StatusLogger.getLogger();

    /**
     * Creates and returns {@link AsyncEventRouter} instances based on user-specified system properties.
     * <p>
     * Property {@code "log4j2.AsyncEventRouter"} controls the routing behaviour. If this property is not specified or
     * has value {@code "Default"}, this method returns {@link DefaultAsyncEventRouter} objects.
     * </p> <p>
     * If this property has value {@code "Discard"}, this method returns {@link DiscardingAsyncEventRouter} objects.
     * </p> <p>
     * For any other value, this method interprets the value as the fully qualified name of a class implementing the
     * {@link AsyncEventRouter} interface. The class must have a default constructor.
     * </p>
     *
     * @return a new AsyncEventRouter
     */
    public static AsyncEventRouter create() {
        final String router = PropertiesUtil.getProperties().getStringProperty(PROPERTY_NAME_ASYNC_EVENT_ROUTER);
        if (router == null || PROPERTY_VALUE_DEFAULT_ASYNC_EVENT_ROUTER.equals(router)
                || DefaultAsyncEventRouter.class.getSimpleName().equals(router)
                || DefaultAsyncEventRouter.class.getName().equals(router)) {
            return new DefaultAsyncEventRouter();
        }
        if (PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER.equals(router)
                || DiscardingAsyncEventRouter.class.getSimpleName().equals(router)
                || DiscardingAsyncEventRouter.class.getName().equals(router)) {
            return createDiscardingAsyncEventRouter();
        }
        return createCustomRouter(router);
    }

    private static AsyncEventRouter createCustomRouter(final String router) {
        try {
            final Class<? extends AsyncEventRouter> cls = LoaderUtil.loadClass(router).asSubclass(AsyncEventRouter.class);
            LOGGER.debug("Creating custom AsyncEventRouter '{}'", router);
            return cls.newInstance();
        } catch (final Exception ex) {
            LOGGER.debug("Using DefaultAsyncEventRouter. Could not create custom AsyncEventRouter '{}': {}", router,
                    ex.toString());
            return new DefaultAsyncEventRouter();
        }
    }

    private static AsyncEventRouter createDiscardingAsyncEventRouter() {
        final PropertiesUtil util = PropertiesUtil.getProperties();
        final String level = util.getStringProperty(PROPERTY_NAME_DISCARDING_THRESHOLD_LEVEL, Level.INFO.name());
        final Level thresholdLevel = Level.toLevel(level, Level.INFO);
        LOGGER.debug("Creating custom DiscardingAsyncEventRouter(discardThreshold:{})", thresholdLevel);
        return new DiscardingAsyncEventRouter(thresholdLevel);
    }
}
