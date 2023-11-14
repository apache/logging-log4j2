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
package org.apache.logging.log4j.core.async;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.apache.logging.log4j.util.Strings.toRootLowerCase;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the AsyncQueueFullPolicyFactory class.
 */
@Tag("async")
public class AsyncQueueFullPolicyFactoryTest {

    @BeforeEach
    @AfterEach
    public void resetProperties() {
        System.clearProperty(Log4jPropertyKey.ASYNC_LOGGER_QUEUE_FULL_POLICY.getKey());
        System.clearProperty(Log4jPropertyKey.ASYNC_LOGGER_DISCARD_THRESHOLD.getKey());
        PropertiesUtil.getProperties().reload();
    }

    @Test
    public void testCreateReturnsDefaultRouterByDefault() {
        final AsyncQueueFullPolicy router = AsyncQueueFullPolicyFactory.create();
        assertEquals(DefaultAsyncQueueFullPolicy.class, router.getClass());
    }

    @Test
    public void testCreateReturnsDiscardingRouterIfSpecified() {
        System.setProperty(Log4jPropertyKey.ASYNC_LOGGER_QUEUE_FULL_POLICY.getKey(),
                AsyncQueueFullPolicyFactory.PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER);
        assertEquals(DiscardingAsyncQueueFullPolicy.class, AsyncQueueFullPolicyFactory.create().getClass());

        System.setProperty(Log4jPropertyKey.ASYNC_LOGGER_QUEUE_FULL_POLICY.getKey(),
                DiscardingAsyncQueueFullPolicy.class.getSimpleName());
        assertEquals(DiscardingAsyncQueueFullPolicy.class, AsyncQueueFullPolicyFactory.create().getClass());

        System.setProperty(Log4jPropertyKey.ASYNC_LOGGER_QUEUE_FULL_POLICY.getKey(),
                DiscardingAsyncQueueFullPolicy.class.getName());
        assertEquals(DiscardingAsyncQueueFullPolicy.class, AsyncQueueFullPolicyFactory.create().getClass());
    }

    @Test
    public void testCreateDiscardingRouterDefaultThresholdLevelInfo() {
        System.setProperty(Log4jPropertyKey.ASYNC_LOGGER_QUEUE_FULL_POLICY.getKey(),
                AsyncQueueFullPolicyFactory.PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER);
        assertEquals(Level.INFO, ((DiscardingAsyncQueueFullPolicy) AsyncQueueFullPolicyFactory.create()).
                getThresholdLevel());
    }

    @Test
    public void testCreateDiscardingRouterCaseInsensitive() {
        System.setProperty(Log4jPropertyKey.ASYNC_LOGGER_QUEUE_FULL_POLICY.getKey(),
                toRootLowerCase(AsyncQueueFullPolicyFactory.PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER));
        assertEquals(Level.INFO, ((DiscardingAsyncQueueFullPolicy) AsyncQueueFullPolicyFactory.create()).
                getThresholdLevel());
    }

    @Test
    public void testCreateDiscardingRouterThresholdLevelCustomizable() {
        System.setProperty(Log4jPropertyKey.ASYNC_LOGGER_QUEUE_FULL_POLICY.getKey(),
                AsyncQueueFullPolicyFactory.PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER);

        for (final Level level : Level.values()) {
            System.setProperty(Log4jPropertyKey.ASYNC_LOGGER_DISCARD_THRESHOLD.getKey(),
                    level.name());
            assertEquals(level, ((DiscardingAsyncQueueFullPolicy) AsyncQueueFullPolicyFactory.create()).
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
        System.setProperty(Log4jPropertyKey.ASYNC_LOGGER_QUEUE_FULL_POLICY.getKey(),
                CustomRouterDefaultConstructor.class.getName());
        assertEquals(CustomRouterDefaultConstructor.class, AsyncQueueFullPolicyFactory.create().getClass());
    }

    @Test
    public void testCreateReturnsDefaultRouterIfSpecifiedCustomRouterFails() {
        System.setProperty(Log4jPropertyKey.ASYNC_LOGGER_QUEUE_FULL_POLICY.getKey(),
                DoesNotImplementInterface.class.getName());
        assertEquals(DefaultAsyncQueueFullPolicy.class, AsyncQueueFullPolicyFactory.create().getClass());
    }
}
