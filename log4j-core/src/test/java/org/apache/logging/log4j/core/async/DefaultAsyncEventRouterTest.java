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
 * Tests the DefaultAsyncEventRouter class.
 */
public class DefaultAsyncEventRouterTest {

    private static long currentThreadId() {
        return Thread.currentThread().getId();
    }

    private static long otherThreadId() {
        return -1;
    }

    @Test
    public void testGetRouteEnqueuesIfQueueNotFull() throws Exception {
        DefaultAsyncEventRouter router = new DefaultAsyncEventRouter();
        assertEquals(EventRoute.ENQUEUE, router.getRoute(currentThreadId(), Level.OFF, 256, 256));
        assertEquals(EventRoute.ENQUEUE, router.getRoute(otherThreadId(), Level.OFF, 256, 256));
        assertEquals(EventRoute.ENQUEUE, router.getRoute(currentThreadId(), Level.ALL, 256, 256));
        assertEquals(EventRoute.ENQUEUE, router.getRoute(otherThreadId(), Level.ALL, 256, 256));
        assertEquals(EventRoute.ENQUEUE, router.getRoute(currentThreadId(), Level.ALL, 256, 255));
        assertEquals(EventRoute.ENQUEUE, router.getRoute(otherThreadId(), Level.ALL, 256, 255));
        assertEquals(EventRoute.ENQUEUE, router.getRoute(currentThreadId(), Level.OFF, 256, 255));
        assertEquals(EventRoute.ENQUEUE, router.getRoute(otherThreadId(), Level.OFF, 256, 255));
        assertEquals(EventRoute.ENQUEUE, router.getRoute(currentThreadId(), Level.ALL, 256, 1));
        assertEquals(EventRoute.ENQUEUE, router.getRoute(otherThreadId(), Level.ALL, 256, 1));
        assertEquals(EventRoute.ENQUEUE, router.getRoute(currentThreadId(), Level.OFF, 256, 1));
        assertEquals(EventRoute.ENQUEUE, router.getRoute(otherThreadId(), Level.OFF, 256, 1));
    }

    @Test
    public void testGetRouteEnqueuesIfQueueFullAndCalledFromDifferentThread() throws Exception {
        DefaultAsyncEventRouter router = new DefaultAsyncEventRouter();
        assertEquals(EventRoute.ENQUEUE, router.getRoute(otherThreadId(), Level.ALL, 512, 0));
        assertEquals(EventRoute.ENQUEUE, router.getRoute(otherThreadId(), Level.OFF, 512, 0));
    }

    @Test
    public void testGetRouteSynchronousIfQueueFullAndCalledFromSameThread() throws Exception {
        DefaultAsyncEventRouter router = new DefaultAsyncEventRouter();
        assertEquals(EventRoute.SYNCHRONOUS, router.getRoute(currentThreadId(), Level.ALL, 512, 0));
        assertEquals(EventRoute.SYNCHRONOUS, router.getRoute(currentThreadId(), Level.OFF, 512, 0));
    }
}