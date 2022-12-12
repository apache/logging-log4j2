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

import java.util.Locale;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.impl.Log4jProperties;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.di.Injector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the AsyncQueueFullPolicyFactory class.
 */
@Tag("async")
@ClearSystemProperty(key = Log4jProperties.ASYNC_LOGGER_QUEUE_FULL_POLICY)
@ClearSystemProperty(key = Log4jProperties.ASYNC_LOGGER_DISCARD_THRESHOLD)
public class AsyncQueueFullPolicyFactoryTest {

    private final Injector injector = DI.createInjector();

    @BeforeEach
    void setUp() {
        injector.init();
    }

    @Test
    public void testCreateReturnsDefaultRouterByDefault() {
        System.clearProperty(Log4jProperties.ASYNC_LOGGER_QUEUE_FULL_POLICY);
        final AsyncQueueFullPolicy router = injector.getInstance(AsyncQueueFullPolicy.class);
        assertEquals(DefaultAsyncQueueFullPolicy.class, router.getClass());
    }

    @Test
    public void testCreateReturnsDiscardingRouterIfSpecified() {
        System.setProperty(Log4jProperties.ASYNC_LOGGER_QUEUE_FULL_POLICY,
                AsyncQueueFullPolicyFactory.PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER);
        assertEquals(DiscardingAsyncQueueFullPolicy.class, injector.getInstance(AsyncQueueFullPolicy.class).getClass());

        System.setProperty(Log4jProperties.ASYNC_LOGGER_QUEUE_FULL_POLICY,
                DiscardingAsyncQueueFullPolicy.class.getSimpleName());
        assertEquals(DiscardingAsyncQueueFullPolicy.class, injector.getInstance(AsyncQueueFullPolicy.class).getClass());

        System.setProperty(Log4jProperties.ASYNC_LOGGER_QUEUE_FULL_POLICY,
                DiscardingAsyncQueueFullPolicy.class.getName());
        assertEquals(DiscardingAsyncQueueFullPolicy.class, injector.getInstance(AsyncQueueFullPolicy.class).getClass());
    }

    @Test
    public void testCreateDiscardingRouterDefaultThresholdLevelInfo() {
        System.setProperty(Log4jProperties.ASYNC_LOGGER_QUEUE_FULL_POLICY,
                AsyncQueueFullPolicyFactory.PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER);
        assertEquals(Level.INFO, ((DiscardingAsyncQueueFullPolicy) injector.getInstance(AsyncQueueFullPolicy.class)).
                getThresholdLevel());
    }

    @Test
    public void testCreateDiscardingRouterCaseInsensitive() {
        System.setProperty(Log4jProperties.ASYNC_LOGGER_QUEUE_FULL_POLICY,
                AsyncQueueFullPolicyFactory.PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER.toLowerCase(Locale.ENGLISH));
        assertEquals(Level.INFO, ((DiscardingAsyncQueueFullPolicy) injector.getInstance(AsyncQueueFullPolicy.class)).
                getThresholdLevel());
    }

    @Test
    public void testCreateDiscardingRouterThresholdLevelCustomizable() {
        System.setProperty(Log4jProperties.ASYNC_LOGGER_QUEUE_FULL_POLICY,
                AsyncQueueFullPolicyFactory.PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER);

        for (final Level level : Level.values()) {
            System.setProperty(Log4jProperties.ASYNC_LOGGER_DISCARD_THRESHOLD,
                    level.name());
            assertEquals(level, ((DiscardingAsyncQueueFullPolicy) injector.getInstance(AsyncQueueFullPolicy.class)).
                    getThresholdLevel());
        }
    }

    static class CustomRouterDefaultConstructor implements AsyncQueueFullPolicy {
        public CustomRouterDefaultConstructor() {
        }

        @Override
        public EventRoute getRoute(final long backgroundThreadId, final Level level) {
            return null;
        }
    }

    static class DoesNotImplementInterface {
    }

    @Test
    public void testCreateReturnsCustomRouterIfSpecified() {
        System.setProperty(Log4jProperties.ASYNC_LOGGER_QUEUE_FULL_POLICY,
                CustomRouterDefaultConstructor.class.getName());
        assertEquals(CustomRouterDefaultConstructor.class, injector.getInstance(AsyncQueueFullPolicy.class).getClass());
    }

    @Test
    public void testCreateReturnsDefaultRouterIfSpecifiedCustomRouterFails() {
        System.setProperty(Log4jProperties.ASYNC_LOGGER_QUEUE_FULL_POLICY,
                DoesNotImplementInterface.class.getName());
        assertEquals(DefaultAsyncQueueFullPolicy.class, injector.getInstance(AsyncQueueFullPolicy.class).getClass());
    }
}
