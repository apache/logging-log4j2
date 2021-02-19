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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.logging.log4j.junit.UsingAnyThreadContext;
import org.apache.logging.log4j.spi.DefaultThreadContextMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

/**
 * Tests {@link ThreadContext}.
 */
@UsingAnyThreadContext
@ResourceLock(Resources.SYSTEM_PROPERTIES)
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
        assertThat("ThreadContextInheritanceTest is running").describedAs("Incorrect parameterized stack value").isEqualTo(ThreadContext.pop());
        assertThat("Hello").describedAs("Incorrect simple stack value").isEqualTo(ThreadContext.pop());
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
            assertThat(str).describedAs("Unexpected ThreadContext value. Expected Hello. Actual " + str).isEqualTo("Hello");
            sb = new StringBuilder();
            thread = new TestThread(sb);
            thread.start();
            thread.join();
            str = sb.toString();
            assertThat(str).describedAs("Unexpected ThreadContext value. Expected Hello. Actual " + str).isEqualTo("Hello");
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
        assertThat(ThreadContext.get("testKey")).isNull();
        ThreadContext.put("testKey", "testValue");
        assertThat(ThreadContext.get("testKey")).isEqualTo("testValue");

        ThreadContext.remove("testKey");
        assertThat(ThreadContext.get("testKey")).isNull();
        assertThat(ThreadContext.isEmpty()).isTrue();
    }

    @Test
    public void testContainsKey() {
        ThreadContext.clearMap();
        assertThat(ThreadContext.containsKey("testKey")).isFalse();
        ThreadContext.put("testKey", "testValue");
        assertThat(ThreadContext.containsKey("testKey")).isTrue();

        ThreadContext.remove("testKey");
        assertThat(ThreadContext.containsKey("testKey")).isFalse();
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
