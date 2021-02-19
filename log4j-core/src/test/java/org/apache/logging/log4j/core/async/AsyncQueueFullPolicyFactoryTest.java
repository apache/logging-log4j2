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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import java.util.Locale;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.categories.AsyncLoggers;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests the AsyncQueueFullPolicyFactory class.
 */
@Category(AsyncLoggers.class)
public class AsyncQueueFullPolicyFactoryTest {

    @Before
    @After
    public void resetProperties() throws Exception {
        System.clearProperty(AsyncQueueFullPolicyFactory.PROPERTY_NAME_ASYNC_EVENT_ROUTER);
        System.clearProperty(AsyncQueueFullPolicyFactory.PROPERTY_NAME_DISCARDING_THRESHOLD_LEVEL);
        PropertiesUtil.getProperties().reload();
    }

    @Test
    public void testCreateReturnsDefaultRouterByDefault() throws Exception {
        final AsyncQueueFullPolicy router = AsyncQueueFullPolicyFactory.create();
        assertThat(router.getClass()).isEqualTo(DefaultAsyncQueueFullPolicy.class);
    }

    @Test
    public void testCreateReturnsDiscardingRouterIfSpecified() throws Exception {
        System.setProperty(AsyncQueueFullPolicyFactory.PROPERTY_NAME_ASYNC_EVENT_ROUTER,
                AsyncQueueFullPolicyFactory.PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER);
        assertThat(AsyncQueueFullPolicyFactory.create().getClass()).isEqualTo(DiscardingAsyncQueueFullPolicy.class);

        System.setProperty(AsyncQueueFullPolicyFactory.PROPERTY_NAME_ASYNC_EVENT_ROUTER,
                DiscardingAsyncQueueFullPolicy.class.getSimpleName());
        assertThat(AsyncQueueFullPolicyFactory.create().getClass()).isEqualTo(DiscardingAsyncQueueFullPolicy.class);

        System.setProperty(AsyncQueueFullPolicyFactory.PROPERTY_NAME_ASYNC_EVENT_ROUTER,
                DiscardingAsyncQueueFullPolicy.class.getName());
        assertThat(AsyncQueueFullPolicyFactory.create().getClass()).isEqualTo(DiscardingAsyncQueueFullPolicy.class);
    }

    @Test
    public void testCreateDiscardingRouterDefaultThresholdLevelInfo() throws Exception {
        System.setProperty(AsyncQueueFullPolicyFactory.PROPERTY_NAME_ASYNC_EVENT_ROUTER,
                AsyncQueueFullPolicyFactory.PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER);
        assertThat(((DiscardingAsyncQueueFullPolicy) AsyncQueueFullPolicyFactory.create()).
                getThresholdLevel()).isEqualTo(Level.INFO);
    }

    @Test
    public void testCreateDiscardingRouterCaseInsensitive() {
        System.setProperty(AsyncQueueFullPolicyFactory.PROPERTY_NAME_ASYNC_EVENT_ROUTER,
                AsyncQueueFullPolicyFactory.PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER.toLowerCase(Locale.ENGLISH));
        assertThat(((DiscardingAsyncQueueFullPolicy) AsyncQueueFullPolicyFactory.create()).
                getThresholdLevel()).isEqualTo(Level.INFO);
    }

    @Test
    public void testCreateDiscardingRouterThresholdLevelCustomizable() throws Exception {
        System.setProperty(AsyncQueueFullPolicyFactory.PROPERTY_NAME_ASYNC_EVENT_ROUTER,
                AsyncQueueFullPolicyFactory.PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER);

        for (final Level level : Level.values()) {
            System.setProperty(AsyncQueueFullPolicyFactory.PROPERTY_NAME_DISCARDING_THRESHOLD_LEVEL,
                    level.name());
            assertThat(((DiscardingAsyncQueueFullPolicy) AsyncQueueFullPolicyFactory.create()).
                    getThresholdLevel()).isEqualTo(level);
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
    public void testCreateReturnsCustomRouterIfSpecified() throws Exception {
        System.setProperty(AsyncQueueFullPolicyFactory.PROPERTY_NAME_ASYNC_EVENT_ROUTER,
                CustomRouterDefaultConstructor.class.getName());
        assertThat(AsyncQueueFullPolicyFactory.create().getClass()).isEqualTo(CustomRouterDefaultConstructor.class);
    }

    @Test
    public void testCreateReturnsDefaultRouterIfSpecifiedCustomRouterFails() throws Exception {
        System.setProperty(AsyncQueueFullPolicyFactory.PROPERTY_NAME_ASYNC_EVENT_ROUTER,
                DoesNotImplementInterface.class.getName());
        assertThat(AsyncQueueFullPolicyFactory.create().getClass()).isEqualTo(DefaultAsyncQueueFullPolicy.class);
    }
}
