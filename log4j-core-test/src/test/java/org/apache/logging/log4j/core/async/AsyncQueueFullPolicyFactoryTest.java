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

import static org.apache.logging.log4j.util.Strings.toRootLowerCase;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.test.junit.Tags;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests the AsyncQueueFullPolicyFactory class.
 */
@Tag(Tags.ASYNC_LOGGERS)
class AsyncQueueFullPolicyFactoryTest {

    @BeforeEach
    @AfterEach
    void resetProperties() {
        System.clearProperty(AsyncQueueFullPolicyFactory.PROPERTY_NAME_ASYNC_EVENT_ROUTER);
        System.clearProperty(AsyncQueueFullPolicyFactory.PROPERTY_NAME_DISCARDING_THRESHOLD_LEVEL);
    }

    @Test
    void testCreateReturnsDefaultRouterByDefault() {
        final AsyncQueueFullPolicy router = AsyncQueueFullPolicyFactory.create();
        assertEquals(DefaultAsyncQueueFullPolicy.class, router.getClass());
    }

    @Test
    void testCreateReturnsDiscardingRouterIfSpecified() {
        System.setProperty(
                AsyncQueueFullPolicyFactory.PROPERTY_NAME_ASYNC_EVENT_ROUTER,
                AsyncQueueFullPolicyFactory.PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER);
        assertEquals(
                DiscardingAsyncQueueFullPolicy.class,
                AsyncQueueFullPolicyFactory.create().getClass());

        System.setProperty(
                AsyncQueueFullPolicyFactory.PROPERTY_NAME_ASYNC_EVENT_ROUTER,
                DiscardingAsyncQueueFullPolicy.class.getSimpleName());
        assertEquals(
                DiscardingAsyncQueueFullPolicy.class,
                AsyncQueueFullPolicyFactory.create().getClass());

        System.setProperty(
                AsyncQueueFullPolicyFactory.PROPERTY_NAME_ASYNC_EVENT_ROUTER,
                DiscardingAsyncQueueFullPolicy.class.getName());
        assertEquals(
                DiscardingAsyncQueueFullPolicy.class,
                AsyncQueueFullPolicyFactory.create().getClass());
    }

    @Test
    void testCreateDiscardingRouterDefaultThresholdLevelInfo() {
        System.setProperty(
                AsyncQueueFullPolicyFactory.PROPERTY_NAME_ASYNC_EVENT_ROUTER,
                AsyncQueueFullPolicyFactory.PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER);
        assertEquals(
                Level.INFO,
                ((DiscardingAsyncQueueFullPolicy) AsyncQueueFullPolicyFactory.create()).getThresholdLevel());
    }

    @Test
    void testCreateDiscardingRouterCaseInsensitive() {
        System.setProperty(
                AsyncQueueFullPolicyFactory.PROPERTY_NAME_ASYNC_EVENT_ROUTER,
                toRootLowerCase(AsyncQueueFullPolicyFactory.PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER));
        assertEquals(
                Level.INFO,
                ((DiscardingAsyncQueueFullPolicy) AsyncQueueFullPolicyFactory.create()).getThresholdLevel());
    }

    @Test
    void testCreateDiscardingRouterThresholdLevelCustomizable() {
        System.setProperty(
                AsyncQueueFullPolicyFactory.PROPERTY_NAME_ASYNC_EVENT_ROUTER,
                AsyncQueueFullPolicyFactory.PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER);

        for (final Level level : Level.values()) {
            System.setProperty(AsyncQueueFullPolicyFactory.PROPERTY_NAME_DISCARDING_THRESHOLD_LEVEL, level.name());
            assertEquals(
                    level, ((DiscardingAsyncQueueFullPolicy) AsyncQueueFullPolicyFactory.create()).getThresholdLevel());
        }
    }

    public static class CustomRouterDefaultConstructor implements AsyncQueueFullPolicy {
        public CustomRouterDefaultConstructor() {}

        @Override
        public EventRoute getRoute(final long backgroundThreadId, final Level level) {
            return null;
        }
    }

    public static class DoesNotImplementInterface {}

    @Test
    void testCreateReturnsCustomRouterIfSpecified() {
        System.setProperty(
                AsyncQueueFullPolicyFactory.PROPERTY_NAME_ASYNC_EVENT_ROUTER,
                CustomRouterDefaultConstructor.class.getName());
        assertEquals(
                CustomRouterDefaultConstructor.class,
                AsyncQueueFullPolicyFactory.create().getClass());
    }

    @Test
    void testCreateReturnsDefaultRouterIfSpecifiedCustomRouterFails() {
        System.setProperty(
                AsyncQueueFullPolicyFactory.PROPERTY_NAME_ASYNC_EVENT_ROUTER,
                DoesNotImplementInterface.class.getName());
        assertEquals(
                DefaultAsyncQueueFullPolicy.class,
                AsyncQueueFullPolicyFactory.create().getClass());
    }
}
