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
package org.apache.logging.log4j;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class ThreadContextTest {

    @Test
    public void testPush() {
        ThreadContext.push("Hello");
        ThreadContext.push("{} is {}", ThreadContextTest.class.getSimpleName(),
                "running");
        assertEquals("Incorrect parameterized stack value",
                ThreadContext.pop(), "ThreadContextTest is running");
        assertEquals("Incorrect simple stack value", ThreadContext.pop(),
                "Hello");
    }

    @Test
    public void testInheritanceSwitchedOffByDefault() throws Exception {
        ThreadContext.clearMap();
        ThreadContext.put("Greeting", "Hello");
        StringBuilder sb = new StringBuilder();
        TestThread thread = new TestThread(sb);
        thread.start();
        thread.join();
        String str = sb.toString();
        assertTrue("Unexpected ThreadContext value. Expected null. Actual "
                + str, "null".equals(str));
        sb = new StringBuilder();
        thread = new TestThread(sb);
        thread.start();
        thread.join();
        str = sb.toString();
        assertTrue("Unexpected ThreadContext value. Expected null. Actual "
                + str, "null".equals(str));
    }

    @Test
    public void perfTest() throws Exception {
        UtilityClass.perfTest();
    }

    @Test
    public void testGetContextReturnsEmptyMapIfEmpty() {
        UtilityClass.testGetContextReturnsEmptyMapIfEmpty();
    }

    @Test
    public void testGetContextReturnsMutableCopy() {
        UtilityClass.testGetContextReturnsMutableCopy();
    }

    @Test
    public void testGetImmutableContextReturnsEmptyMapIfEmpty() {
        UtilityClass.testGetImmutableContextReturnsEmptyMapIfEmpty();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetImmutableContextReturnsImmutableMapIfNonEmpty() {
        UtilityClass.testGetImmutableContextReturnsImmutableMapIfNonEmpty();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetImmutableContextReturnsImmutableMapIfEmpty() {
        UtilityClass.testGetImmutableContextReturnsImmutableMapIfEmpty();
    }

    @Test
    public void testGetImmutableStackReturnsEmptyStackIfEmpty() {
        UtilityClass.testGetImmutableStackReturnsEmptyStackIfEmpty();
    }

    @Test
    public void testPut() {
        UtilityClass.testPut();
    }

    @Test
    public void testPutAll() {
        ThreadContext.clearMap();
        //
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
    public void testRemove() {
        ThreadContext.clearMap();
        assertNull(ThreadContext.get("testKey"));
        ThreadContext.put("testKey", "testValue");
        assertEquals("testValue", ThreadContext.get("testKey"));

        ThreadContext.remove("testKey");
        assertNull(ThreadContext.get("testKey"));
        assertTrue(ThreadContext.isEmpty());
    }

    @Test
    public void testContainsKey() {
        ThreadContext.clearMap();
        assertFalse(ThreadContext.containsKey("testKey"));
        ThreadContext.put("testKey", "testValue");
        assertTrue(ThreadContext.containsKey("testKey"));

        ThreadContext.remove("testKey");
        assertFalse(ThreadContext.containsKey("testKey"));
    }

    private class TestThread extends Thread {

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
