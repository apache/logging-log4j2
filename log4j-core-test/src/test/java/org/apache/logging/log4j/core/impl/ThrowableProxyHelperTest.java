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
package org.apache.logging.log4j.core.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/**
 * Tests ThrowableProxyHelper.
 */
public class ThrowableProxyHelperTest {

    /**
     * We populate dummy stack trace and array of stack trace elements in the right order
     * It supposed to always trigger fast path so cache won't be populated
     * This simulates the case when current thread's and throwable stack traces have the same elements
     */
    @Test
    public void testSuccessfulCacheHit() {
        final Map<String, ThrowableProxyHelper.CacheEntry> map = new HashMap<>();
        final Deque<Class<?>> stack = new ArrayDeque<>(3);
        final StackTraceElement[] stackTraceElements = new StackTraceElement[3];
        stackTraceElements[0] = new StackTraceElement(Integer.class.getName(), "toString", "Integer.java", 1);
        stack.addLast(Integer.class);
        stackTraceElements[1] = new StackTraceElement(Float.class.getName(), "toString", "Float.java", 1);
        stack.addLast(Float.class);
        stackTraceElements[2] = new StackTraceElement(Double.class.getName(), "toString", "Double.java", 1);
        stack.addLast(Double.class);
        final Throwable throwable = new IllegalStateException("This is a test");
        final ThrowableProxy proxy = new ThrowableProxy(throwable);
        ThrowableProxyHelper.toExtendedStackTrace(proxy, stack, map, null, stackTraceElements);
        assertTrue(map.isEmpty());
    }

    /**
     * We populate dummy stack trace and array of stack trace elements in the wrong order
     * It will trigger fast path only once so cache will have two items
     */
    @Test
    public void testFailedCacheHit() {
        final Map<String, ThrowableProxyHelper.CacheEntry> map = new HashMap<>();
        final Deque<Class<?>> stack = new ArrayDeque<>(3);
        final StackTraceElement[] stackTraceElements = new StackTraceElement[3];
        stackTraceElements[0] = new StackTraceElement(Integer.class.getName(), "toString", "Integer.java", 1);
        stack.addFirst(Integer.class);
        stackTraceElements[1] = new StackTraceElement(Float.class.getName(), "toString", "Float.java", 1);
        stack.addFirst(Float.class);
        stackTraceElements[2] = new StackTraceElement(Double.class.getName(), "toString", "Double.java", 1);
        stack.addFirst(Double.class);
        final Throwable throwable = new IllegalStateException("This is a test");
        final ThrowableProxy proxy = new ThrowableProxy(throwable);
        ThrowableProxyHelper.toExtendedStackTrace(proxy, stack, map, null, stackTraceElements);
        assertFalse(map.isEmpty());
        // Integer will match, so fast path won't cache it, only Float and Double will appear in cache after class
        // loading
        assertTrue(map.containsKey(Double.class.getName()));
        assertTrue(map.containsKey(Float.class.getName()));
    }
}
