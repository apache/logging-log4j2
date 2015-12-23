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
import org.apache.logging.log4j.util.PropertiesUtil;

import java.lang.reflect.Constructor;

/**
 * Creates {@link AsyncEventRouter} instances based on user-specified system properties. The {@code AsyncEventRouter}
 * created by this factory is used in AsyncLogger, AsyncLoggerConfig and AsyncAppender
 * to control if events are logged in the current thread, the background thread, or discarded.
 * <p>
 * Property {@code "log4j2.AsyncEventRouter"} controls the routing behaviour. If this property is not specified or has
 * value {@code "Default"}, this factory creates {@link DefaultAsyncEventRouter} objects.
 * </p> <p>
 * If this property has value {@code "Discard"}, this factory creates {@link DiscardingAsyncEventRouter} objects.
 * By default, this router discards events of level {@code INFO}, {@code DEBUG} and {@code TRACE} if the queue is at
 * 80% capacity or more. This can be adjusted with properties {@code "log4j2.DiscardQueueRatio"} (must be a float
 * between 0.0 and 1.0) and {@code "log4j2.DiscardThreshold"} (name of the level at which to start discarding).
 * </p> <p>
 * For any other value, this
 * factory interprets the value as the fully qualified name of a class implementing the {@link AsyncEventRouter}
 * interface. If a constructor with a single {@code int} argument exists, this constructor is invoked with the queue
 * size, otherwise the default constructor is called.
 * </p>
 *
 * @since 2.5.1
 */
public class AsyncEventRouterFactory {
    static final String PROPERTY_NAME_ASYNC_EVENT_ROUTER = "log4j2.AsyncEventRouter";
    static final String PROPERTY_VALUE_DEFAULT_ASYNC_EVENT_ROUTER = "Default";
    static final String PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER = "Discard";
    static final String PROPERTY_NAME_DISCARDING_QUEUE_RATIO = "log4j2.DiscardQueueRatio";
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
     * {@link AsyncEventRouter} interface. If a constructor with a single {@code int} argument exists, this constructor
     * is invoked with the queue size, otherwise the default constructor is called.
     * </p>
     *
     * @param queueSize the queue size
     * @return a new AsyncEventRouter
     */
    public static AsyncEventRouter create(final int queueSize) {
        final String router = PropertiesUtil.getProperties().getStringProperty(PROPERTY_NAME_ASYNC_EVENT_ROUTER);
        if (router == null || PROPERTY_VALUE_DEFAULT_ASYNC_EVENT_ROUTER.equals(router)
                || DefaultAsyncEventRouter.class.getSimpleName().equals(router)
                || DefaultAsyncEventRouter.class.getName().equals(router)) {
            return new DefaultAsyncEventRouter();
        }
        if (PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER.equals(router)
                || DiscardingAsyncEventRouter.class.getSimpleName().equals(router)
                || DiscardingAsyncEventRouter.class.getName().equals(router)) {
            return createDiscardingAsyncEventRouter(queueSize);
        }
        return createCustomRouter(router, queueSize);
    }

    private static AsyncEventRouter createCustomRouter(final String router, final int queueSize) {
        try {
            @SuppressWarnings("unchecked")
            final Class<AsyncEventRouter> cls = (Class<AsyncEventRouter>) Class.forName(router);
            try {
                // if the custom router has a constructor taking an int, pass it the queue size
                Constructor<AsyncEventRouter> constructor = cls.getDeclaredConstructor(new Class[]{int.class});
                LOGGER.debug("Creating custom AsyncEventRouter '{}({})'", router, queueSize);
                return constructor.newInstance(new Object[]{queueSize});
            } catch (final Exception e) {
                // otherwise we try the default constructor
                LOGGER.debug("Creating custom AsyncEventRouter '{}'", router);
                return cls.newInstance();
            }
        } catch (final Exception ex) {
            LOGGER.debug("Using DefaultAsyncEventRouter. Could not create custom AsyncEventRouter '{}': {}", router,
                    ex.toString());
            return new DefaultAsyncEventRouter();
        }
    }

    private static AsyncEventRouter createDiscardingAsyncEventRouter(final int queueSize) {
        final PropertiesUtil util = PropertiesUtil.getProperties();
        final float ratio = (float) util.getDoubleProperty(PROPERTY_NAME_DISCARDING_QUEUE_RATIO, 0.8);
        final String level = util.getStringProperty(PROPERTY_NAME_DISCARDING_THRESHOLD_LEVEL, Level.INFO.name());
        final Level thresholdLevel = Level.toLevel(level, Level.INFO);
        LOGGER.debug("Creating custom DiscardingAsyncEventRouter(discardThreshold:{}, discardRatio:{})", thresholdLevel,
                ratio);
        return new DiscardingAsyncEventRouter(queueSize, ratio, thresholdLevel);
    }
}
