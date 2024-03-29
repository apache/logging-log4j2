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

import static org.apache.logging.log4j.core.async.AsyncQueueFullPolicyFactory.DISCARDING_POLICY;
import static org.apache.logging.log4j.util.Strings.toRootLowerCase;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.impl.CoreProperties.QueueFullPolicyProperties;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests the AsyncQueueFullPolicyFactory class.
 */
@Tag("parallel")
public class AsyncQueueFullPolicyFactoryTest {

    private static final QueueFullPolicyProperties DEFAULT = QueueFullPolicyProperties.defaultValue();

    private static AsyncQueueFullPolicy createPolicy(final QueueFullPolicyProperties properties) {
        return AsyncQueueFullPolicyFactory.create(properties, StatusLogger.getLogger());
    }

    @Test
    public void testCreateReturnsDefaultRouterByDefault() {
        assertThat(createPolicy(DEFAULT)).isInstanceOf(DefaultAsyncQueueFullPolicy.class);
    }

    @Test
    public void testCreateReturnsDiscardingRouterIfSpecified() {
        assertThat(createPolicy(DEFAULT.withType(DISCARDING_POLICY)))
                .isInstanceOf(DiscardingAsyncQueueFullPolicy.class);

        assertThat(createPolicy(DEFAULT.withType(DiscardingAsyncQueueFullPolicy.class.getSimpleName())))
                .isInstanceOf(DiscardingAsyncQueueFullPolicy.class);

        assertThat(createPolicy(DEFAULT.withType(DiscardingAsyncQueueFullPolicy.class.getName())))
                .isInstanceOf(DiscardingAsyncQueueFullPolicy.class);
    }

    @Test
    public void testCreateDiscardingRouterDefaultThresholdLevelInfo() {
        final DiscardingAsyncQueueFullPolicy policy =
                (DiscardingAsyncQueueFullPolicy) createPolicy(DEFAULT.withType(DISCARDING_POLICY));
        assertThat(policy.getThresholdLevel()).isEqualTo(Level.INFO);
    }

    @Test
    public void testCreateDiscardingRouterCaseInsensitive() {
        final DiscardingAsyncQueueFullPolicy policy =
                (DiscardingAsyncQueueFullPolicy) createPolicy(DEFAULT.withType(toRootLowerCase(DISCARDING_POLICY)));
        assertThat(policy.getThresholdLevel()).isEqualTo(Level.INFO);
    }

    @Test
    public void testCreateDiscardingRouterThresholdLevelCustomizable() {
        final QueueFullPolicyProperties config = DEFAULT.withType(DISCARDING_POLICY);
        for (final Level level : Level.values()) {
            final DiscardingAsyncQueueFullPolicy policy =
                    (DiscardingAsyncQueueFullPolicy) createPolicy(config.withLevel(level));
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
        assertThat(createPolicy(DEFAULT.withType(CustomRouterDefaultConstructor.class.getName())))
                .isInstanceOf(CustomRouterDefaultConstructor.class);
    }

    @Test
    public void testCreateReturnsDefaultRouterIfSpecifiedCustomRouterFails() {
        assertThat(createPolicy(DEFAULT.withType(DoesNotImplementInterface.class.getName())))
                .isInstanceOf(DefaultAsyncQueueFullPolicy.class);
    }
}
