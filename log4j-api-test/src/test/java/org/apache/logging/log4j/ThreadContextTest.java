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
package org.apache.logging.log4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.test.ThreadContextUtilityClass;
import org.apache.logging.log4j.test.junit.UsingAnyThreadContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@UsingAnyThreadContext
class ThreadContextTest {

    @Test
    void testPush() {
        ThreadContext.push("Hello");
        ThreadContext.push("{} is {}", ThreadContextTest.class.getSimpleName(), "running");
        assertEquals("ThreadContextTest is running", ThreadContext.pop(), "Incorrect parameterized stack value");
        assertEquals("Hello", ThreadContext.pop(), "Incorrect simple stack value");
    }

    @Test
    void testInheritanceSwitchedOffByDefault() throws Exception {
        ThreadContext.put("Greeting", "Hello");
        StringBuilder sb = new StringBuilder();
        TestThread thread = new TestThread(sb);
        thread.start();
        thread.join();
        String str = sb.toString();
        assertEquals("null", str, "Unexpected ThreadContext value. Expected null. Actual " + str);
        sb = new StringBuilder();
        thread = new TestThread(sb);
        thread.start();
        thread.join();
        str = sb.toString();
        assertEquals("null", str, "Unexpected ThreadContext value. Expected null. Actual " + str);
    }

    @Test
    @Tag("performance")
    void perfTest() {
        ThreadContextUtilityClass.perfTest();
    }

    @Test
    void testGetContextReturnsEmptyMapIfEmpty() {
        ThreadContextUtilityClass.testGetContextReturnsEmptyMapIfEmpty();
    }

    @Test
    void testGetContextReturnsMutableCopy() {
        ThreadContextUtilityClass.testGetContextReturnsMutableCopy();
    }

    @Test
    void testGetImmutableContextReturnsEmptyMapIfEmpty() {
        ThreadContextUtilityClass.testGetImmutableContextReturnsEmptyMapIfEmpty();
    }

    @Test
    void testGetImmutableContextReturnsImmutableMapIfNonEmpty() {
        ThreadContextUtilityClass.testGetImmutableContextReturnsImmutableMapIfNonEmpty();
    }

    @Test
    void testGetImmutableContextReturnsImmutableMapIfEmpty() {
        ThreadContextUtilityClass.testGetImmutableContextReturnsImmutableMapIfEmpty();
    }

    @Test
    void testGetImmutableStackReturnsEmptyStackIfEmpty() {
        ThreadContextUtilityClass.testGetImmutableStackReturnsEmptyStackIfEmpty();
    }

    @Test
    void testPut() {
        ThreadContextUtilityClass.testPut();
    }

    @Test
    void testPutIfNotNull() {
        ThreadContext.clearMap();
        assertNull(ThreadContext.get("testKey"));
        ThreadContext.put("testKey", "testValue");
        assertEquals("testValue", ThreadContext.get("testKey"));
        assertEquals("testValue", ThreadContext.get("testKey"), "Incorrect value in test key");
        ThreadContext.putIfNull("testKey", "new Value");
        assertEquals("testValue", ThreadContext.get("testKey"), "Incorrect value in test key");
        ThreadContext.clearMap();
    }

    @Test
    void testPutAll() {
        assertTrue(ThreadContext.isEmpty());
        assertFalse(ThreadContext.containsKey("key"));
        final int mapSize = 10;
        final Map<String, String> newMap = new HashMap<>(mapSize);
        for (int i = 1; i <= mapSize; i++) {
            newMap.put("key" + i, "value" + i);
        }
        ThreadContext.putAll(newMap);
        assertFalse(ThreadContext.isEmpty());
        for (int i = 1; i <= mapSize; i++) {
            assertTrue(ThreadContext.containsKey("key" + i));
            assertEquals("value" + i, ThreadContext.get("key" + i));
        }
    }

    @Test
    void testRemove() {
        assertNull(ThreadContext.get("testKey"));
        ThreadContext.put("testKey", "testValue");
        assertEquals("testValue", ThreadContext.get("testKey"));

        ThreadContext.remove("testKey");
        assertNull(ThreadContext.get("testKey"));
        assertTrue(ThreadContext.isEmpty());
    }

    @Test
    void testRemoveAll() {
        ThreadContext.put("testKey1", "testValue1");
        ThreadContext.put("testKey2", "testValue2");
        assertEquals("testValue1", ThreadContext.get("testKey1"));
        assertEquals("testValue2", ThreadContext.get("testKey2"));
        assertFalse(ThreadContext.isEmpty());

        ThreadContext.removeAll(Arrays.asList("testKey1", "testKey2"));
        assertNull(ThreadContext.get("testKey1"));
        assertNull(ThreadContext.get("testKey2"));
        assertTrue(ThreadContext.isEmpty());
    }

    @Test
    void testContainsKey() {
        assertFalse(ThreadContext.containsKey("testKey"));
        ThreadContext.put("testKey", "testValue");
        assertTrue(ThreadContext.containsKey("testKey"));

        ThreadContext.remove("testKey");
        assertFalse(ThreadContext.containsKey("testKey"));
    }

    private static class TestThread extends Thread {

        private final StringBuilder sb;

        public TestThread(final StringBuilder sb) {
            this.sb = sb;
        }

        @Override
        public void run() {
            final String greeting = ThreadContext.get("Greeting");
            if (greeting == null) {
                sb.append("null");
            } else {
                sb.append(greeting);
            }
            ThreadContext.clearMap();
        }
    }
}
