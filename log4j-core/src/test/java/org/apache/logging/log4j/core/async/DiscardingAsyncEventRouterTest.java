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
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the DiscardingAsyncEventRouter class..
 */
public class DiscardingAsyncEventRouterTest {

    private static long currentThreadId() {
        return Thread.currentThread().getId();
    }

    private static long otherThreadId() {
        return -1;
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorDisallowsNullThresholdLevel() {
        new DiscardingAsyncEventRouter(128, 0.5F, null);
    }

    @Test
    public void testThresholdLevelIsConstructorValue() {
        assertSame(Level.ALL, new DiscardingAsyncEventRouter(128, 0.5F, Level.ALL).getThresholdLevel());
        assertSame(Level.OFF, new DiscardingAsyncEventRouter(128, 0.5F, Level.OFF).getThresholdLevel());
        assertSame(Level.INFO, new DiscardingAsyncEventRouter(128, 0.5F, Level.INFO).getThresholdLevel());
    }

    @Test
    public void testThresholdRemainingCapacityBasedOnConstructorValues() {
        // discard when queue full
        assertEquals(0, new DiscardingAsyncEventRouter(4, 1F, Level.ALL).getThresholdQueueRemainingCapacity());

        // discard when queue half full
        assertEquals(2, new DiscardingAsyncEventRouter(4, 0.5F, Level.ALL).getThresholdQueueRemainingCapacity());

        // discard even if queue empty
        assertEquals(4, new DiscardingAsyncEventRouter(4, 0F, Level.ALL).getThresholdQueueRemainingCapacity());
    }

    @Test
    public void testGetRouteEnqueuesIfThresholdCapacityNotReached() throws Exception {
        DiscardingAsyncEventRouter router = new DiscardingAsyncEventRouter(256, 0.5F, Level.WARN);
        assertEquals(EventRoute.ENQUEUE, router.getRoute(currentThreadId(), Level.OFF, 256, 256));
        assertEquals(EventRoute.ENQUEUE, router.getRoute(otherThreadId(), Level.OFF, 256, 256));
        assertEquals(EventRoute.ENQUEUE, router.getRoute(currentThreadId(), Level.ALL, 256, 256));
        assertEquals(EventRoute.ENQUEUE, router.getRoute(otherThreadId(), Level.ALL, 256, 256));
        assertEquals(EventRoute.ENQUEUE, router.getRoute(currentThreadId(), Level.ALL, 256, 255));
        assertEquals(EventRoute.ENQUEUE, router.getRoute(otherThreadId(), Level.ALL, 256, 255));
        assertEquals(EventRoute.ENQUEUE, router.getRoute(currentThreadId(), Level.OFF, 256, 255));
        assertEquals(EventRoute.ENQUEUE, router.getRoute(otherThreadId(), Level.OFF, 256, 255));
        assertEquals(EventRoute.ENQUEUE, router.getRoute(currentThreadId(), Level.FATAL, 256, 129));
        assertEquals(EventRoute.ENQUEUE, router.getRoute(otherThreadId(), Level.FATAL, 256, 129));
        assertEquals(EventRoute.ENQUEUE, router.getRoute(currentThreadId(), Level.TRACE, 256, 129));
        assertEquals(EventRoute.ENQUEUE, router.getRoute(otherThreadId(), Level.TRACE, 256, 129));
    }

    @Test
    public void testGetRouteDiscardsIfThresholdCapacityReachedAndLevelEqualOrLessSpecificThanThreshold()
            throws Exception {
        DiscardingAsyncEventRouter router = new DiscardingAsyncEventRouter(256, 0.5F, Level.WARN);

        for (Level level : new Level[] {Level.WARN, Level.INFO, Level.DEBUG, Level.TRACE, Level.ALL}) {
            assertEquals(level.name(), EventRoute.DISCARD, router.getRoute(currentThreadId(), level, 256, 1));
            assertEquals(level.name(), EventRoute.DISCARD, router.getRoute(otherThreadId(), level, 256, 1));
            assertEquals(level.name(), EventRoute.DISCARD, router.getRoute(currentThreadId(), level, 256, 128));
            assertEquals(level.name(), EventRoute.DISCARD, router.getRoute(otherThreadId(), level, 256, 128));
        }
    }

    @Test
    public void testGetRouteDiscardsIfQueueFullAndLevelEqualOrLessSpecificThanThreshold() throws Exception {
        DiscardingAsyncEventRouter router = new DiscardingAsyncEventRouter(256, 0.5F, Level.WARN);

        for (Level level : new Level[] {Level.WARN, Level.INFO, Level.DEBUG, Level.TRACE, Level.ALL}) {
            assertEquals(level.name(), EventRoute.DISCARD, router.getRoute(currentThreadId(), level, 256, 0));
            assertEquals(level.name(), EventRoute.DISCARD, router.getRoute(otherThreadId(), level, 256, 0));
        }
    }

    @Test
    public void testGetRouteEnqueuesIfThresholdCapacityReachedButLevelMoreSpecificThanThreshold()
            throws Exception {
        DiscardingAsyncEventRouter router = new DiscardingAsyncEventRouter(256, 0.5F, Level.WARN);

        for (Level level : new Level[] {Level.ERROR, Level.FATAL, Level.OFF}) {
            assertEquals(level.name(), EventRoute.ENQUEUE, router.getRoute(currentThreadId(), level, 256, 1));
            assertEquals(level.name(), EventRoute.ENQUEUE, router.getRoute(otherThreadId(), level, 256, 1));
            assertEquals(level.name(), EventRoute.ENQUEUE, router.getRoute(currentThreadId(), level, 256, 128));
            assertEquals(level.name(), EventRoute.ENQUEUE, router.getRoute(otherThreadId(), level, 256, 128));
        }
    }

    @Test
    public void testGetRouteEnqueuesIfOtherThreadQueueFullAndLevelMoreSpecificThanThreshold() throws Exception {
        DiscardingAsyncEventRouter router = new DiscardingAsyncEventRouter(256, 0.5F, Level.WARN);

        for (Level level : new Level[] {Level.ERROR, Level.FATAL, Level.OFF}) {
            assertEquals(level.name(), EventRoute.ENQUEUE, router.getRoute(otherThreadId(), level, 256, 0));
        }
    }

    @Test
    public void testGetRouteSynchronousIfCurrentThreadQueueFullAndLevelMoreSpecificThanThreshold() throws Exception {
        DiscardingAsyncEventRouter router = new DiscardingAsyncEventRouter(256, 0.5F, Level.WARN);

        for (Level level : new Level[] {Level.ERROR, Level.FATAL, Level.OFF}) {
            assertEquals(level.name(), EventRoute.SYNCHRONOUS, router.getRoute(currentThreadId(), level, 256, 0));
        }
    }

    @Test
    public void testGetDiscardCount() throws Exception {
        DiscardingAsyncEventRouter router = new DiscardingAsyncEventRouter(4, 0F, Level.INFO);
        assertEquals("initially", 0, DiscardingAsyncEventRouter.getDiscardCount(router));

        assertEquals(EventRoute.DISCARD, router.getRoute(-1L, Level.INFO, 256, 0));
        assertEquals("increase", 1, DiscardingAsyncEventRouter.getDiscardCount(router));

        assertEquals(EventRoute.DISCARD, router.getRoute(-1L, Level.INFO, 256, 0));
        assertEquals("increase", 2, DiscardingAsyncEventRouter.getDiscardCount(router));

        assertEquals(EventRoute.DISCARD, router.getRoute(-1L, Level.INFO, 256, 0));
        assertEquals("increase", 3, DiscardingAsyncEventRouter.getDiscardCount(router));
    }
}