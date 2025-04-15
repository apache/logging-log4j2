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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.test.junit.Tags;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests the DiscardingAsyncQueueFullPolicy class..
 */
@Tag(Tags.ASYNC_LOGGERS)
class DiscardingAsyncQueueFullPolicyTest {

    private static long currentThreadId() {
        return Thread.currentThread().getId();
    }

    private static long otherThreadId() {
        return -1;
    }

    @Test
    void testConstructorDisallowsNullThresholdLevel() {
        assertThrows(NullPointerException.class, () -> {
            new DiscardingAsyncQueueFullPolicy(null);
        });
    }

    @Test
    void testThresholdLevelIsConstructorValue() {
        assertSame(Level.ALL, new DiscardingAsyncQueueFullPolicy(Level.ALL).getThresholdLevel());
        assertSame(Level.OFF, new DiscardingAsyncQueueFullPolicy(Level.OFF).getThresholdLevel());
        assertSame(Level.INFO, new DiscardingAsyncQueueFullPolicy(Level.INFO).getThresholdLevel());
    }

    @Test
    void testGetRouteDiscardsIfThresholdCapacityReachedAndLevelEqualOrLessSpecificThanThreshold() {
        final DiscardingAsyncQueueFullPolicy router = new DiscardingAsyncQueueFullPolicy(Level.WARN);

        for (final Level level : new Level[] {Level.WARN, Level.INFO, Level.DEBUG, Level.TRACE, Level.ALL}) {
            assertEquals(EventRoute.DISCARD, router.getRoute(currentThreadId(), level), level.name());
            assertEquals(EventRoute.DISCARD, router.getRoute(otherThreadId(), level), level.name());
            assertEquals(EventRoute.DISCARD, router.getRoute(currentThreadId(), level), level.name());
            assertEquals(EventRoute.DISCARD, router.getRoute(otherThreadId(), level), level.name());
        }
    }

    @Test
    void testGetRouteDiscardsIfQueueFullAndLevelEqualOrLessSpecificThanThreshold() {
        final DiscardingAsyncQueueFullPolicy router = new DiscardingAsyncQueueFullPolicy(Level.WARN);

        for (final Level level : new Level[] {Level.WARN, Level.INFO, Level.DEBUG, Level.TRACE, Level.ALL}) {
            assertEquals(EventRoute.DISCARD, router.getRoute(currentThreadId(), level), level.name());
            assertEquals(EventRoute.DISCARD, router.getRoute(otherThreadId(), level), level.name());
        }
    }

    @Test
    void testGetRouteEnqueuesIfThresholdCapacityReachedButLevelMoreSpecificThanThreshold() {
        final DiscardingAsyncQueueFullPolicy router = new DiscardingAsyncQueueFullPolicy(Level.WARN);

        for (final Level level : new Level[] {Level.ERROR, Level.FATAL, Level.OFF}) {
            assertEquals(EventRoute.SYNCHRONOUS, router.getRoute(currentThreadId(), level), level.name());
            assertEquals(EventRoute.ENQUEUE, router.getRoute(otherThreadId(), level), level.name());
            assertEquals(EventRoute.SYNCHRONOUS, router.getRoute(currentThreadId(), level), level.name());
            assertEquals(EventRoute.ENQUEUE, router.getRoute(otherThreadId(), level), level.name());
        }
    }

    @Test
    void testGetRouteEnqueueIfOtherThreadQueueFullAndLevelMoreSpecificThanThreshold() {
        final DiscardingAsyncQueueFullPolicy router = new DiscardingAsyncQueueFullPolicy(Level.WARN);

        for (final Level level : new Level[] {Level.ERROR, Level.FATAL, Level.OFF}) {
            assertEquals(EventRoute.ENQUEUE, router.getRoute(otherThreadId(), level), level.name());
        }
    }

    @Test
    void testGetRouteSynchronousIfCurrentThreadQueueFullAndLevelMoreSpecificThanThreshold() {
        final DiscardingAsyncQueueFullPolicy router = new DiscardingAsyncQueueFullPolicy(Level.WARN);

        for (final Level level : new Level[] {Level.ERROR, Level.FATAL, Level.OFF}) {
            assertEquals(EventRoute.SYNCHRONOUS, router.getRoute(currentThreadId(), level), level.name());
        }
    }

    @Test
    void testGetDiscardCount() {
        final DiscardingAsyncQueueFullPolicy router = new DiscardingAsyncQueueFullPolicy(Level.INFO);
        assertEquals(0, DiscardingAsyncQueueFullPolicy.getDiscardCount(router), "initially");

        assertEquals(EventRoute.DISCARD, router.getRoute(-1L, Level.INFO));
        assertEquals(1, DiscardingAsyncQueueFullPolicy.getDiscardCount(router), "increase");

        assertEquals(EventRoute.DISCARD, router.getRoute(-1L, Level.INFO));
        assertEquals(2, DiscardingAsyncQueueFullPolicy.getDiscardCount(router), "increase");

        assertEquals(EventRoute.DISCARD, router.getRoute(-1L, Level.INFO));
        assertEquals(3, DiscardingAsyncQueueFullPolicy.getDiscardCount(router), "increase");
    }
}
