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

import org.apache.logging.log4j.test.ThreadContextUtilityClass;
import org.apache.logging.log4j.test.junit.InitializesThreadContext;
import org.apache.logging.log4j.test.junit.UsingThreadContextMap;
import org.apache.logging.log4j.test.junit.UsingThreadContextStack;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

import static org.apache.logging.log4j.spi.LoggingSystemProperties.THREAD_CONTEXT_MAP_INHERITABLE;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests {@link ThreadContext}.
 */
@SetSystemProperty(key = THREAD_CONTEXT_MAP_INHERITABLE, value = "true")
@InitializesThreadContext
public class ThreadContextInheritanceTest {

    @Test
    @UsingThreadContextStack
    public void testPush() {
        ThreadContext.push("Hello");
        ThreadContext.push("{} is {}", ThreadContextInheritanceTest.class.getSimpleName(), "running");
        assertEquals(
                ThreadContext.pop(), "ThreadContextInheritanceTest is running", "Incorrect parameterized stack value");
        assertEquals(ThreadContext.pop(), "Hello", "Incorrect simple stack value");
    }

    @Test
    @SetSystemProperty(key = THREAD_CONTEXT_MAP_INHERITABLE, value = "true")
    @InitializesThreadContext
    public void testInheritanceSwitchedOn() throws Exception {
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
    }

    @Test
    @Tag("performance")
    @UsingThreadContextMap
    public void perfTest() {
        ThreadContextUtilityClass.perfTest();
    }

    @Test
    @UsingThreadContextMap
    public void testGetContextReturnsEmptyMapIfEmpty() {
        ThreadContextUtilityClass.testGetContextReturnsEmptyMapIfEmpty();
    }

    @Test
    @UsingThreadContextMap
    public void testGetContextReturnsMutableCopy() {
        ThreadContextUtilityClass.testGetContextReturnsMutableCopy();
    }

    @Test
    @UsingThreadContextMap
    public void testGetImmutableContextReturnsEmptyMapIfEmpty() {
        ThreadContextUtilityClass.testGetImmutableContextReturnsEmptyMapIfEmpty();
    }

    @Test
    @UsingThreadContextMap
    public void testGetImmutableContextReturnsImmutableMapIfNonEmpty() {
        ThreadContextUtilityClass.testGetImmutableContextReturnsImmutableMapIfNonEmpty();
    }

    @Test
    @UsingThreadContextMap
    public void testGetImmutableContextReturnsImmutableMapIfEmpty() {
        ThreadContextUtilityClass.testGetImmutableContextReturnsImmutableMapIfEmpty();
    }

    @Test
    @UsingThreadContextStack
    public void testGetImmutableStackReturnsEmptyStackIfEmpty() {
        ThreadContextUtilityClass.testGetImmutableStackReturnsEmptyStackIfEmpty();
    }

    @Test
    @UsingThreadContextMap
    public void testPut() {
        ThreadContextUtilityClass.testPut();
    }

    @Test
    @UsingThreadContextMap
    public void testRemove() {
        assertNull(ThreadContext.get("testKey"));
        ThreadContext.put("testKey", "testValue");
        assertEquals("testValue", ThreadContext.get("testKey"));

        ThreadContext.remove("testKey");
        assertNull(ThreadContext.get("testKey"));
        assertTrue(ThreadContext.isEmpty());
    }

    @Test
    @UsingThreadContextMap
    public void testContainsKey() {
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
