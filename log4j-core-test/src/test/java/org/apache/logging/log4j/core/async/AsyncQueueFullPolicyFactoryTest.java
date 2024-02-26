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

import static org.apache.logging.log4j.core.async.AsyncQueueFullPolicyFactory.PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER;
import static org.apache.logging.log4j.util.Strings.toRootLowerCase;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.impl.CoreKeys;
import org.apache.logging.log4j.test.junit.Tags;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests the AsyncQueueFullPolicyFactory class.
 */
@Tag(Tags.PARALLEL)
public class AsyncQueueFullPolicyFactoryTest {

    private static final CoreKeys.AsyncQueueFullPolicy DEFAULT = CoreKeys.AsyncQueueFullPolicy.defaultValue();

    @Test
    public void testCreateReturnsDefaultRouterByDefault() {
        assertThat(AsyncQueueFullPolicyFactory.create(DEFAULT)).isInstanceOf(DefaultAsyncQueueFullPolicy.class);
    }

    @Test
    public void testCreateReturnsDiscardingRouterIfSpecified() {
        assertThat(AsyncQueueFullPolicyFactory.create(
                        DEFAULT.withClassName(PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER)))
                .isInstanceOf(DiscardingAsyncQueueFullPolicy.class);

        assertThat(AsyncQueueFullPolicyFactory.create(
                        DEFAULT.withClassName(DiscardingAsyncQueueFullPolicy.class.getSimpleName())))
                .isInstanceOf(DiscardingAsyncQueueFullPolicy.class);

        assertThat(AsyncQueueFullPolicyFactory.create(
                        DEFAULT.withClassName(DiscardingAsyncQueueFullPolicy.class.getName())))
                .isInstanceOf(DiscardingAsyncQueueFullPolicy.class);
    }

    @Test
    public void testCreateDiscardingRouterDefaultThresholdLevelInfo() {
        final DiscardingAsyncQueueFullPolicy policy = (DiscardingAsyncQueueFullPolicy)
                AsyncQueueFullPolicyFactory.create(DEFAULT.withClassName(PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER));
        assertThat(policy.getThresholdLevel()).isEqualTo(Level.INFO);
    }

    @Test
    public void testCreateDiscardingRouterCaseInsensitive() {
        final DiscardingAsyncQueueFullPolicy policy =
                (DiscardingAsyncQueueFullPolicy) AsyncQueueFullPolicyFactory.create(
                        DEFAULT.withClassName(toRootLowerCase(PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER)));
        assertThat(policy.getThresholdLevel()).isEqualTo(Level.INFO);
    }

    @Test
    public void testCreateDiscardingRouterThresholdLevelCustomizable() {
        final CoreKeys.AsyncQueueFullPolicy config =
                DEFAULT.withClassName(PROPERTY_VALUE_DISCARDING_ASYNC_EVENT_ROUTER);
        for (final Level level : Level.values()) {
            final DiscardingAsyncQueueFullPolicy policy =
                    (DiscardingAsyncQueueFullPolicy) AsyncQueueFullPolicyFactory.create(config.withLevel(level));
            assertThat(policy.getThresholdLevel()).isEqualTo(level);
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
    public void testCreateReturnsCustomRouterIfSpecified() {
        assertThat(AsyncQueueFullPolicyFactory.create(
                        DEFAULT.withClassName(CustomRouterDefaultConstructor.class.getName())))
                .isInstanceOf(CustomRouterDefaultConstructor.class);
    }

    @Test
    public void testCreateReturnsDefaultRouterIfSpecifiedCustomRouterFails() {
        assertThat(AsyncQueueFullPolicyFactory.create(DEFAULT.withClassName(DoesNotImplementInterface.class.getName())))
                .isInstanceOf(DefaultAsyncQueueFullPolicy.class);
    }
}
