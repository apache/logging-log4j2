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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the AsyncEventRouterFactory class.
 */
public class AsyncEventRouterFactoryTest {

    @After
    public void after() {
        clearProperties();
    }

    @Before
    public void before() {
        clearProperties();
    }

    private void clearProperties() {
        System.clearProperty(AsyncEventRouterFactory.PROPERTY_NAME_ASYNC_EVENT_ROUTER);
        System.clearProperty(AsyncEventRouterFactory.PROPERTY_NAME_DISCARDING_THRESHOLD_LEVEL);
    }

    @Test
    public void testCreateReturnsDefaultRouterByDefault() throws Exception {
        AsyncEventRouter router = AsyncEventRouterFactory.create();
        assertEquals(DefaultAsyncEventRouter.class, router.getClass());
    }

    @Test
    public void testCreateReturnsDiscardingRouterIfSpecified() throws Exception {
        System.setProperty(AsyncEventRouterFactory.PROPERTY_NAME_ASYNC_EVENT_ROUTER,
                AsyncEventRouterFactory.PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER);
        assertEquals(DiscardingAsyncEventRouter.class, AsyncEventRouterFactory.create().getClass());

        System.setProperty(AsyncEventRouterFactory.PROPERTY_NAME_ASYNC_EVENT_ROUTER,
                DiscardingAsyncEventRouter.class.getSimpleName());
        assertEquals(DiscardingAsyncEventRouter.class, AsyncEventRouterFactory.create().getClass());

        System.setProperty(AsyncEventRouterFactory.PROPERTY_NAME_ASYNC_EVENT_ROUTER,
                DiscardingAsyncEventRouter.class.getName());
        assertEquals(DiscardingAsyncEventRouter.class, AsyncEventRouterFactory.create().getClass());
    }

    @Test
    public void testCreateDiscardingRouterDefaultThresholdLevelInfo() throws Exception {
        System.setProperty(AsyncEventRouterFactory.PROPERTY_NAME_ASYNC_EVENT_ROUTER,
                AsyncEventRouterFactory.PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER);
        assertEquals(Level.INFO, ((DiscardingAsyncEventRouter) AsyncEventRouterFactory.create()).
                getThresholdLevel());
    }

    @Test
    public void testCreateDiscardingRouterThresholdLevelCustomizable() throws Exception {
        System.setProperty(AsyncEventRouterFactory.PROPERTY_NAME_ASYNC_EVENT_ROUTER,
                AsyncEventRouterFactory.PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER);

        for (Level level : Level.values()) {
            System.setProperty(AsyncEventRouterFactory.PROPERTY_NAME_DISCARDING_THRESHOLD_LEVEL,
                    level.name());
            assertEquals(level, ((DiscardingAsyncEventRouter) AsyncEventRouterFactory.create()).
                    getThresholdLevel());
        }
    }

    static class CustomRouterDefaultConstructor implements AsyncEventRouter {
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
    public void testCreateReturnsCustomRouterIfSpecified() throws Exception {
        System.setProperty(AsyncEventRouterFactory.PROPERTY_NAME_ASYNC_EVENT_ROUTER,
                CustomRouterDefaultConstructor.class.getName());
        assertEquals(CustomRouterDefaultConstructor.class, AsyncEventRouterFactory.create().getClass());
    }

    @Test
    public void testCreateReturnsDefaultRouterIfSpecifiedCustomRouterFails() throws Exception {
        System.setProperty(AsyncEventRouterFactory.PROPERTY_NAME_ASYNC_EVENT_ROUTER,
                DoesNotImplementInterface.class.getName());
        assertEquals(DefaultAsyncEventRouter.class, AsyncEventRouterFactory.create().getClass());
    }
}