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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.categories.AsyncLoggers;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests the DiscardingAsyncQueueFullPolicy class..
 */
@Category(AsyncLoggers.class)
public class DiscardingAsyncQueueFullPolicyTest {

    private static long currentThreadId() {
        return Thread.currentThread().getId();
    }

    private static long otherThreadId() {
        return -1;
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorDisallowsNullThresholdLevel() {
        new DiscardingAsyncQueueFullPolicy(null);
    }

    @Test
    public void testThresholdLevelIsConstructorValue() {
        assertThat(new DiscardingAsyncQueueFullPolicy(Level.ALL).getThresholdLevel()).isSameAs(Level.ALL);
        assertThat(new DiscardingAsyncQueueFullPolicy(Level.OFF).getThresholdLevel()).isSameAs(Level.OFF);
        assertThat(new DiscardingAsyncQueueFullPolicy(Level.INFO).getThresholdLevel()).isSameAs(Level.INFO);
    }

    @Test
    public void testGetRouteDiscardsIfThresholdCapacityReachedAndLevelEqualOrLessSpecificThanThreshold()
            throws Exception {
        final DiscardingAsyncQueueFullPolicy router = new DiscardingAsyncQueueFullPolicy(Level.WARN);

        for (final Level level : new Level[] {Level.WARN, Level.INFO, Level.DEBUG, Level.TRACE, Level.ALL}) {
            assertThat(router.getRoute(currentThreadId(), level)).describedAs(level.name()).isEqualTo(EventRoute.DISCARD);
            assertThat(router.getRoute(otherThreadId(), level)).describedAs(level.name()).isEqualTo(EventRoute.DISCARD);
            assertThat(router.getRoute(currentThreadId(), level)).describedAs(level.name()).isEqualTo(EventRoute.DISCARD);
            assertThat(router.getRoute(otherThreadId(), level)).describedAs(level.name()).isEqualTo(EventRoute.DISCARD);
        }
    }

    @Test
    public void testGetRouteDiscardsIfQueueFullAndLevelEqualOrLessSpecificThanThreshold() throws Exception {
        final DiscardingAsyncQueueFullPolicy router = new DiscardingAsyncQueueFullPolicy(Level.WARN);

        for (final Level level : new Level[] {Level.WARN, Level.INFO, Level.DEBUG, Level.TRACE, Level.ALL}) {
            assertThat(router.getRoute(currentThreadId(), level)).describedAs(level.name()).isEqualTo(EventRoute.DISCARD);
            assertThat(router.getRoute(otherThreadId(), level)).describedAs(level.name()).isEqualTo(EventRoute.DISCARD);
        }
    }

    @Test
    public void testGetRouteEnqueuesIfThresholdCapacityReachedButLevelMoreSpecificThanThreshold()
            throws Exception {
        final DiscardingAsyncQueueFullPolicy router = new DiscardingAsyncQueueFullPolicy(Level.WARN);

        for (final Level level : new Level[] {Level.ERROR, Level.FATAL, Level.OFF}) {
            assertThat(router.getRoute(currentThreadId(), level)).describedAs(level.name()).isEqualTo(EventRoute.SYNCHRONOUS);
            assertThat(router.getRoute(otherThreadId(), level)).describedAs(level.name()).isEqualTo(EventRoute.ENQUEUE);
            assertThat(router.getRoute(currentThreadId(), level)).describedAs(level.name()).isEqualTo(EventRoute.SYNCHRONOUS);
            assertThat(router.getRoute(otherThreadId(), level)).describedAs(level.name()).isEqualTo(EventRoute.ENQUEUE);
        }
    }

    @Test
    public void testGetRouteEnqueueIfOtherThreadQueueFullAndLevelMoreSpecificThanThreshold() throws Exception {
        final DiscardingAsyncQueueFullPolicy router = new DiscardingAsyncQueueFullPolicy(Level.WARN);

        for (final Level level : new Level[] {Level.ERROR, Level.FATAL, Level.OFF}) {
            assertThat(router.getRoute(otherThreadId(), level)).describedAs(level.name()).isEqualTo(EventRoute.ENQUEUE);
        }
    }

    @Test
    public void testGetRouteSynchronousIfCurrentThreadQueueFullAndLevelMoreSpecificThanThreshold() throws Exception {
        final DiscardingAsyncQueueFullPolicy router = new DiscardingAsyncQueueFullPolicy(Level.WARN);

        for (final Level level : new Level[] {Level.ERROR, Level.FATAL, Level.OFF}) {
            assertThat(router.getRoute(currentThreadId(), level)).describedAs(level.name()).isEqualTo(EventRoute.SYNCHRONOUS);
        }
    }

    @Test
    public void testGetDiscardCount() throws Exception {
        final DiscardingAsyncQueueFullPolicy router = new DiscardingAsyncQueueFullPolicy(Level.INFO);
        assertThat(DiscardingAsyncQueueFullPolicy.getDiscardCount(router)).describedAs("initially").isEqualTo(0);

        assertThat(router.getRoute(-1L, Level.INFO)).isEqualTo(EventRoute.DISCARD);
        assertThat(DiscardingAsyncQueueFullPolicy.getDiscardCount(router)).describedAs("increase").isEqualTo(1);

        assertThat(router.getRoute(-1L, Level.INFO)).isEqualTo(EventRoute.DISCARD);
        assertThat(DiscardingAsyncQueueFullPolicy.getDiscardCount(router)).describedAs("increase").isEqualTo(2);

        assertThat(router.getRoute(-1L, Level.INFO)).isEqualTo(EventRoute.DISCARD);
        assertThat(DiscardingAsyncQueueFullPolicy.getDiscardCount(router)).describedAs("increase").isEqualTo(3);
    }
}
