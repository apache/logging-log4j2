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

import org.apache.logging.log4j.spi.DefaultThreadContextMap;
import org.apache.logging.log4j.test.ThreadContextUtilityClass;
import org.apache.logging.log4j.test.junit.InitializesThreadContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

/**
 * Tests {@link ThreadContext}.
 */
@SetSystemProperty(key = DefaultThreadContextMap.INHERITABLE_MAP, value = "true")
@InitializesThreadContext
public class ThreadContextInheritanceTest {

    @BeforeAll
    public static void setupClass() {
        System.setProperty(DefaultThreadContextMap.INHERITABLE_MAP, "true");
        ThreadContext.init();
    }

    @AfterAll
    public static void tearDownClass() {
        System.clearProperty(DefaultThreadContextMap.INHERITABLE_MAP);
        ThreadContext.init();
    }

    @Test
    public void testPush() {
        ThreadContext.push("Hello");
        ThreadContext.push("{} is {}", ThreadContextInheritanceTest.class.getSimpleName(), "running");
        assertEquals(
                ThreadContext.pop(), "ThreadContextInheritanceTest is running", "Incorrect parameterized stack value");
        assertEquals(ThreadContext.pop(), "Hello", "Incorrect simple stack value");
    }

    @Test
    @SetSystemProperty(key = DefaultThreadContextMap.INHERITABLE_MAP, value = "true")
    @InitializesThreadContext
    public void testInheritanceSwitchedOn() throws Exception {
        System.setProperty(DefaultThreadContextMap.INHERITABLE_MAP, "true");
        try {
            ThreadContext.clearMap();
            ThreadContext.put("Greeting", "Hello");
            StringBuilder sb = new StringBuilder();
            TestThread thread = new TestThread(sb);
            thread.start();
            thread.join();
            String str = sb.toString();
            assertEquals("Hello", str, "Unexpected ThreadContext value. Expected Hello. Actual " + str);
            sb = new StringBuilder();
            thread = new TestThread(sb);
            thread.start();
            thread.join();
            str = sb.toString();
            assertEquals("Hello", str, "Unexpected ThreadContext value. Expected Hello. Actual " + str);
        } finally {
            System.clearProperty(DefaultThreadContextMap.INHERITABLE_MAP);
        }
    }

    @Test
    @Tag("performance")
    public void perfTest() {
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

    @Test
    public void testGetImmutableContextReturnsImmutableMapIfNonEmpty() {
        ThreadContextUtilityClass.testGetImmutableContextReturnsImmutableMapIfNonEmpty();
    }

    @Test
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
