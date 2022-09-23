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

import org.apache.logging.log4j.util.Timer;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ThreadContextUtilityClass {

    public static void perfTest() {
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


    public static void testGetContextReturnsEmptyMapIfEmpty() {
        ThreadContext.clearMap();
        assertTrue(ThreadContext.getContext().isEmpty());
    }


    public static void testGetContextReturnsMutableCopy() {
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

    public static void testGetImmutableContextReturnsEmptyMapIfEmpty() {
        ThreadContext.clearMap();
        assertTrue(ThreadContext.getImmutableContext().isEmpty());
    }


    public static void testGetImmutableContextReturnsImmutableMapIfNonEmpty() {
        ThreadContext.clearMap();
        ThreadContext.put("key", "val");
        final Map<String, String> immutable = ThreadContext.getImmutableContext();
        assertThrows(UnsupportedOperationException.class, () -> immutable.put("otherkey", "otherval"));
    }

    public static void testGetImmutableContextReturnsImmutableMapIfEmpty() {
        ThreadContext.clearMap();
        final Map<String, String> immutable = ThreadContext.getImmutableContext();
        assertThrows(UnsupportedOperationException.class, () -> immutable.put("otherkey", "otherval"));
    }

    public static void testGetImmutableStackReturnsEmptyStackIfEmpty() {
        ThreadContext.clearStack();
        assertTrue(ThreadContext.getImmutableStack().asList().isEmpty());
    }


    public static void testPut() {
        ThreadContext.clearMap();
        assertNull(ThreadContext.get("testKey"));
        ThreadContext.put("testKey", "testValue");
        assertEquals("testValue", ThreadContext.get("testKey"));
    }
}
