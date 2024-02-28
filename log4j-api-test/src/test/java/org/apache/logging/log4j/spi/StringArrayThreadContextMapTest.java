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
package org.apache.logging.log4j.spi;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.test.junit.InitializesThreadContext;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.apache.logging.log4j.test.junit.UsingThreadContextMap;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearSystemProperty;

/**
 * Tests the {@code StringArrayThreadContextMap} class.
 */
@UsingThreadContextMap
public class StringArrayThreadContextMapTest {

    @Test
    public void testEqualsVsSameKind() {
        final StringArrayThreadContextMap map1 = createMap();
        final StringArrayThreadContextMap map2 = createMap();
        assertEquals(map1, map1);
        assertEquals(map2, map2);
        assertEquals(map1, map2);
        assertEquals(map2, map1);
    }

    @Test
    public void testHashCodeVsSameKind() {
        final StringArrayThreadContextMap map1 = createMap();
        final StringArrayThreadContextMap map2 = createMap();
        assertEquals(map1.hashCode(), map2.hashCode());
    }

    @Test
    public void testGet() {
        final StringArrayThreadContextMap map1 = createMap();
        assertEquals(null, map1.get("test"));
        map1.put("test", "test");
        assertEquals("test", map1.get("test"));
        assertEquals(null, map1.get("not_present"));
    }

    @Test
    public void testDoesNothingIfConstructedWithUseMapIsFalse() {
        final StringArrayThreadContextMap map = new StringArrayThreadContextMap(false);
        assertTrue(map.isEmpty());
        assertFalse(map.containsKey("key"));
        map.put("key", "value");

        assertTrue(map.isEmpty());
        assertFalse(map.containsKey("key"));
        assertNull(map.get("key"));
    }

    @Test
    public void testPut() {
        final StringArrayThreadContextMap map = new StringArrayThreadContextMap(true);
        assertTrue(map.isEmpty());
        assertFalse(map.containsKey("key"));
        map.put("key", "value");

        assertFalse(map.isEmpty());
        assertTrue(map.containsKey("key"));
        assertEquals("value", map.get("key"));
    }

    @Test
    public void testPutAll() {
        final StringArrayThreadContextMap map = new StringArrayThreadContextMap(true);
        assertTrue(map.isEmpty());
        assertFalse(map.containsKey("key"));
        final int mapSize = 10;
        final Map<String, String> newMap = new HashMap<>(mapSize);
        for (int i = 1; i <= mapSize; i++) {
            newMap.put("key" + i, "value" + i);
        }
        map.putAll(newMap);
        assertFalse(map.isEmpty());
        for (int i = 1; i <= mapSize; i++) {
            assertTrue(map.containsKey("key" + i));
            assertEquals("value" + i, map.get("key" + i));
        }
    }

    /**
     * Test method for
     * {@link org.apache.logging.log4j.spi.StringArrayThreadContextMap#remove(java.lang.String)}
     * .
     */
    @Test
    public void testRemove() {
        final StringArrayThreadContextMap map = createMap();
        assertEquals("value", map.get("key"));
        assertEquals("value2", map.get("key2"));

        map.remove("key");
        assertFalse(map.containsKey("key"));
        assertEquals("value2", map.get("key2"));
    }

    @Test
    public void testRemoveAll() {
        final StringArrayThreadContextMap map = createMap();

        Map<String, String> newValues = new HashMap<>();
        newValues.put("1", "value1");
        newValues.put("2", "value2");

        map.putAll(newValues);
        map.removeAll(newValues.keySet());

        map.put("3", "value3");
    }

    @Test
    public void testClear() {
        final StringArrayThreadContextMap map = createMap();

        map.clear();
        assertTrue(map.isEmpty());
        assertFalse(map.containsKey("key"));
        assertFalse(map.containsKey("key2"));
    }

    /**
     * @return
     */
    private StringArrayThreadContextMap createMap() {
        final StringArrayThreadContextMap map = new StringArrayThreadContextMap(true);
        assertTrue(map.isEmpty());
        map.put("key", "value");
        map.put("key2", "value2");
        assertEquals("value", map.get("key"));
        assertEquals("value2", map.get("key2"));
        return map;
    }

    @Test
    public void testGetCopyReturnsMutableMap() {
        final StringArrayThreadContextMap map = new StringArrayThreadContextMap(true);
        assertTrue(map.isEmpty());
        final Map<String, String> copy = map.getCopy();
        assertTrue(copy.isEmpty());

        copy.put("key", "value"); // mutable
        assertEquals("value", copy.get("key"));

        // thread context map not affected
        assertTrue(map.isEmpty());
    }

    @Test
    public void testGetCopyReturnsMutableCopy() {
        final StringArrayThreadContextMap map = new StringArrayThreadContextMap(true);
        map.put("key1", "value1");
        assertFalse(map.isEmpty());
        final Map<String, String> copy = map.getCopy();
        assertEquals("value1", copy.get("key1")); // copy has values too

        copy.put("key", "value"); // copy is mutable
        assertEquals("value", copy.get("key"));

        // thread context map not affected
        assertFalse(map.containsKey("key"));

        // clearing context map does not affect copy
        map.clear();
        assertTrue(map.isEmpty());

        assertFalse(copy.isEmpty());
    }

    @Test
    public void testGetImmutableMapReturnsNullIfEmpty() {
        final StringArrayThreadContextMap map = new StringArrayThreadContextMap(true);
        assertTrue(map.isEmpty());
        assertNull(map.getImmutableMapOrNull());
    }

    @Test
    public void testGetImmutableMapReturnsImmutableMapIfNonEmpty() {
        final StringArrayThreadContextMap map = new StringArrayThreadContextMap(true);
        map.put("key1", "value1");
        assertFalse(map.isEmpty());

        final Map<String, String> immutable = map.getImmutableMapOrNull();
        assertEquals("value1", immutable.get("key1")); // copy has values too

        // immutable
        assertThrows(UnsupportedOperationException.class, () -> immutable.put("key", "value"));
    }

    @Test
    public void testGetImmutableMapCopyNotAffectdByContextMapChanges() {
        final StringArrayThreadContextMap map = new StringArrayThreadContextMap(true);
        map.put("key1", "value1");
        assertFalse(map.isEmpty());

        final Map<String, String> immutable = map.getImmutableMapOrNull();
        assertEquals("value1", immutable.get("key1")); // copy has values too

        // clearing context map does not affect copy
        map.clear();
        assertTrue(map.isEmpty());

        assertFalse(immutable.isEmpty());
    }

    @Test
    public void testToStringShowsMapContext() {
        final StringArrayThreadContextMap map = new StringArrayThreadContextMap(true);
        assertEquals("{}", map.toString());

        map.put("key1", "value1");
        assertEquals("{key1=value1}", map.toString());

        map.remove("key1");
        map.put("key2", "value2");
        assertEquals("{key2=value2}", map.toString());
    }

    @Test
    @ClearSystemProperty(key = StringArrayThreadContextMap.INHERITABLE_MAP)
    @InitializesThreadContext
    public void testThreadLocalNotInheritableByDefault() {
        ThreadContextMapFactory.init();
        final ThreadLocal<Object[]> threadLocal = StringArrayThreadContextMap.createThreadLocalMap(true);
        assertFalse(threadLocal instanceof InheritableThreadLocal<?>);
    }

    @Test
    @SetTestProperty(key = StringArrayThreadContextMap.INHERITABLE_MAP, value = "true")
    @InitializesThreadContext
    public void testThreadLocalInheritableIfConfigured() {
        ThreadContextMapFactory.init();
        try {
            final ThreadLocal<Object[]> threadLocal = StringArrayThreadContextMap.createThreadLocalMap(true);
            assertTrue(threadLocal instanceof InheritableThreadLocal<?>);
        } finally {
            System.clearProperty(StringArrayThreadContextMap.INHERITABLE_MAP);
        }
    }
}
