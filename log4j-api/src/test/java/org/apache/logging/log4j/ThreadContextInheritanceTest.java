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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.logging.log4j.spi.DefaultThreadContextMap;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests {@link ThreadContext}.
 */
public class ThreadContextInheritanceTest {

    @BeforeClass
    public static void setupClass() {
        System.setProperty(DefaultThreadContextMap.INHERITABLE_MAP, "true");
        ThreadContext.init();
    }

    @AfterClass
    public static void tearDownClass() {
        System.clearProperty(DefaultThreadContextMap.INHERITABLE_MAP);
        ThreadContext.init();
    }

    @Test
    public void testPush() {
        ThreadContext.push("Hello");
        ThreadContext.push("{} is {}", ThreadContextInheritanceTest.class.getSimpleName(),
                "running");
        assertEquals("Incorrect parameterized stack value",
                ThreadContext.pop(), "ThreadContextInheritanceTest is running");
        assertEquals("Incorrect simple stack value", ThreadContext.pop(),
                "Hello");
    }

    @Test
    public void testInheritanceSwitchedOn() throws Exception {
        System.setProperty(DefaultThreadContextMap.INHERITABLE_MAP, "true");
        ThreadContext.init();
        try {
            ThreadContext.clearMap();
            ThreadContext.put("Greeting", "Hello");
            StringBuilder sb = new StringBuilder();
            TestThread thread = new TestThread(sb);
            thread.start();
            thread.join();
            String str = sb.toString();
            assertTrue("Unexpected ThreadContext value. Expected Hello. Actual "
                    + str, "Hello".equals(str));
            sb = new StringBuilder();
            thread = new TestThread(sb);
            thread.start();
            thread.join();
            str = sb.toString();
            assertTrue("Unexpected ThreadContext value. Expected Hello. Actual "
                    + str, "Hello".equals(str));
        } finally {
            System.clearProperty(DefaultThreadContextMap.INHERITABLE_MAP);
        }
    }

    @Test
    public void perfTest() throws Exception {
        ThreadContextUtilityClass.perfTest();
    }

    @Test
    public void testGetContextReturnsEmptyMapIfEmpty() {
        ThreadContextUtilityClass.testGetContextReturnsEmptyMapIfEmpty();
    }

    @Test
    public void testGetContextReturnsMutableCopy() {
        ThreadContextUtilityClass.testGetContextReturnsMutableCopy();
    }

    @Test
    public void testGetImmutableContextReturnsEmptyMapIfEmpty() {
        ThreadContextUtilityClass.testGetImmutableContextReturnsEmptyMapIfEmpty();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetImmutableContextReturnsImmutableMapIfNonEmpty() {
        ThreadContextUtilityClass.testGetImmutableContextReturnsImmutableMapIfNonEmpty();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetImmutableContextReturnsImmutableMapIfEmpty() {
        ThreadContextUtilityClass.testGetImmutableContextReturnsImmutableMapIfEmpty();
    }

    @Test
    public void testGetImmutableStackReturnsEmptyStackIfEmpty() {
        ThreadContextUtilityClass.testGetImmutableStackReturnsEmptyStackIfEmpty();
    }

    @Test
    public void testPut() {
        ThreadContextUtilityClass.testPut();
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
