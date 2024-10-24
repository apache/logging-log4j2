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

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests the DefaultAsyncQueueFullPolicy class.
 */
@Tag("AsyncLoggers")
public class DefaultAsyncQueueFullPolicyTest {

    private static long currentThreadId() {
        return Thread.currentThread().getId();
    }

    private static long otherThreadId() {
        return -1;
    }

    @Test
    public void testGetRouteEnqueuesIfQueueFullAndCalledFromDifferentThread() throws Exception {
        final DefaultAsyncQueueFullPolicy router = new DefaultAsyncQueueFullPolicy();
        assertEquals(EventRoute.ENQUEUE, router.getRoute(otherThreadId(), Level.ALL));
        assertEquals(EventRoute.ENQUEUE, router.getRoute(otherThreadId(), Level.OFF));
    }

    @Test
    public void testGetRouteSynchronousIfQueueFullAndCalledFromSameThread() throws Exception {
        final DefaultAsyncQueueFullPolicy router = new DefaultAsyncQueueFullPolicy();
        assertEquals(EventRoute.SYNCHRONOUS, router.getRoute(currentThreadId(), Level.ALL));
        assertEquals(EventRoute.SYNCHRONOUS, router.getRoute(currentThreadId(), Level.OFF));
    }
}
