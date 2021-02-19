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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.junit.UsingAnyThreadContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@UsingAnyThreadContext
public class ThreadContextTest {
    public static void reinitThreadContext() {
        ThreadContext.init();
    }

    @Test
    public void testPush() {
        ThreadContext.push("Hello");
        ThreadContext.push("{} is {}", ThreadContextTest.class.getSimpleName(), "running");
        assertThat(ThreadContext.pop()).describedAs("Incorrect parameterized stack value").isEqualTo("ThreadContextTest is running");
        assertThat(ThreadContext.pop()).describedAs("Incorrect simple stack value").isEqualTo("Hello");
    }

    @Test
    public void testInheritanceSwitchedOffByDefault() throws Exception {
        ThreadContext.put("Greeting", "Hello");
        StringBuilder sb = new StringBuilder();
        TestThread thread = new TestThread(sb);
        thread.start();
        thread.join();
        String str = sb.toString();
        assertThat(str).describedAs("Unexpected ThreadContext value. Expected null. Actual " + str).isEqualTo("null");
        sb = new StringBuilder();
        thread = new TestThread(sb);
        thread.start();
        thread.join();
        str = sb.toString();
        assertThat(str).describedAs("Unexpected ThreadContext value. Expected null. Actual " + str).isEqualTo("null");
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
    public void testPutIfNotNull() {
        ThreadContext.clearMap();
        assertThat(ThreadContext.get("testKey")).isNull();
        ThreadContext.put("testKey", "testValue");
        assertThat(ThreadContext.get("testKey")).isEqualTo("testValue");
        assertThat(ThreadContext.get("testKey")).describedAs("Incorrect value in test key").isEqualTo("testValue");
        ThreadContext.putIfNull("testKey", "new Value");
        assertThat(ThreadContext.get("testKey")).describedAs("Incorrect value in test key").isEqualTo("testValue");
        ThreadContext.clearMap();
    }

    @Test
    public void testPutAll() {
        assertThat(ThreadContext.isEmpty()).isTrue();
        assertThat(ThreadContext.containsKey("key")).isFalse();
        final int mapSize = 10;
        final Map<String, String> newMap = new HashMap<>(mapSize);
        for (int i = 1; i <= mapSize; i++) {
            newMap.put("key" + i, "value" + i);
        }
        ThreadContext.putAll(newMap);
        assertThat(ThreadContext.isEmpty()).isFalse();
        for (int i = 1; i <= mapSize; i++) {
            assertThat(ThreadContext.containsKey("key" + i)).isTrue();
            assertThat(ThreadContext.get("key" + i)).isEqualTo("value" + i);
        }
    }

    @Test
    public void testRemove() {
        assertThat(ThreadContext.get("testKey")).isNull();
        ThreadContext.put("testKey", "testValue");
        assertThat(ThreadContext.get("testKey")).isEqualTo("testValue");

        ThreadContext.remove("testKey");
        assertThat(ThreadContext.get("testKey")).isNull();
        assertThat(ThreadContext.isEmpty()).isTrue();
    }

    @Test
    public void testRemoveAll() {
        ThreadContext.put("testKey1", "testValue1");
        ThreadContext.put("testKey2", "testValue2");
        assertThat(ThreadContext.get("testKey1")).isEqualTo("testValue1");
        assertThat(ThreadContext.get("testKey2")).isEqualTo("testValue2");
        assertThat(ThreadContext.isEmpty()).isFalse();

        ThreadContext.removeAll(Arrays.asList("testKey1", "testKey2"));
        assertThat(ThreadContext.get("testKey1")).isNull();
        assertThat(ThreadContext.get("testKey2")).isNull();
        assertThat(ThreadContext.isEmpty()).isTrue();
    }

    @Test
    public void testContainsKey() {
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
