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

import java.util.Map;

import org.apache.logging.log4j.spi.DefaultThreadContextMap;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

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
        ThreadContext.clearMap();
        final Timer complete = new Timer("ThreadContextTest");
        complete.start();
        ThreadContext.put("Var1", "value 1");
        ThreadContext.put("Var2", "value 2");
        ThreadContext.put("Var3", "value 3");
        ThreadContext.put("Var4", "value 4");
        ThreadContext.put("Var5", "value 5");
        ThreadContext.put("Var6", "value 6");
        ThreadContext.put("Var7", "value 7");
        ThreadContext.put("Var8", "value 8");
        ThreadContext.put("Var9", "value 9");
        ThreadContext.put("Var10", "value 10");
        final int loopCount = 1000000;
        final Timer timer = new Timer("ThreadContextCopy", loopCount);
        timer.start();
        for (int i = 0; i < loopCount; ++i) {
            final Map<String, String> map = ThreadContext.getImmutableContext();
            assertNotNull(map);
        }
        timer.stop();
        complete.stop();
        System.out.println(timer.toString());
        System.out.println(complete.toString());
    }

    @Test
    public void testGetContextReturnsEmptyMapIfEmpty() {
        ThreadContext.clearMap();
        assertTrue(ThreadContext.getContext().isEmpty());
    }

    @Test
    public void testGetContextReturnsMutableCopy() {
        ThreadContext.clearMap();
        final Map<String, String> map1 = ThreadContext.getContext();
        assertTrue(map1.isEmpty());
        map1.put("K", "val"); // no error
        assertEquals("val", map1.get("K"));

        // adding to copy does not affect thread context map
        assertTrue(ThreadContext.getContext().isEmpty());

        ThreadContext.put("key", "val2");
        final Map<String, String> map2 = ThreadContext.getContext();
        assertEquals(1, map2.size());
        assertEquals("val2", map2.get("key"));
        map2.put("K", "val"); // no error
        assertEquals("val", map2.get("K"));

        // first copy is not affected
        assertNotSame(map1, map2);
        assertEquals(1, map1.size());
    }

    @Test
    public void testGetImmutableContextReturnsEmptyMapIfEmpty() {
        ThreadContext.clearMap();
        assertTrue(ThreadContext.getImmutableContext().isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetImmutableContextReturnsImmutableMapIfNonEmpty() {
        ThreadContext.clearMap();
        ThreadContext.put("key", "val");
        final Map<String, String> immutable = ThreadContext.getImmutableContext();
        immutable.put("otherkey", "otherval");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetImmutableContextReturnsImmutableMapIfEmpty() {
        ThreadContext.clearMap();
        final Map<String, String> immutable = ThreadContext.getImmutableContext();
        immutable.put("otherkey", "otherval");
    }

    @Test
    public void testGetImmutableStackReturnsEmptyStackIfEmpty() {
        ThreadContext.clearStack();
        assertTrue(ThreadContext.getImmutableStack().asList().isEmpty());
    }

    @Test
    public void testPut() {
        ThreadContext.clearMap();
        assertNull(ThreadContext.get("testKey"));
        ThreadContext.put("testKey", "testValue");
        assertEquals("testValue", ThreadContext.get("testKey"));
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
