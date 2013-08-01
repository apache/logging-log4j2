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
package org.apache.logging.log4j.spi;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

/**
 * Tests the {@code DefaultThreadContextMap} class.
 */
public class DefaultThreadContextMapTest {

    @Test
    public void testDoesNothingIfConstructedWithUseMapIsFalse() {
        final DefaultThreadContextMap map = new DefaultThreadContextMap(false);
        assertTrue(map.isEmpty());
        assertFalse(map.containsKey("key"));
        map.put("key", "value");

        assertTrue(map.isEmpty());
        assertFalse(map.containsKey("key"));
        assertNull(map.get("key"));
    }

    @Test
    public void testPut() {
        final DefaultThreadContextMap map = new DefaultThreadContextMap(true);
        assertTrue(map.isEmpty());
        assertFalse(map.containsKey("key"));
        map.put("key", "value");

        assertFalse(map.isEmpty());
        assertTrue(map.containsKey("key"));
        assertEquals("value", map.get("key"));
    }

    /**
     * Test method for
     * {@link org.apache.logging.log4j.spi.DefaultThreadContextMap#remove(java.lang.String)}
     * .
     */
    @Test
    public void testRemove() {
        final DefaultThreadContextMap map = new DefaultThreadContextMap(true);
        assertTrue(map.isEmpty());
        map.put("key", "value");
        map.put("key2", "value2");
        assertEquals("value", map.get("key"));
        assertEquals("value2", map.get("key2"));

        map.remove("key");
        assertFalse(map.containsKey("key"));
        assertEquals("value2", map.get("key2"));
    }

    @Test
    public void testClear() {
        final DefaultThreadContextMap map = new DefaultThreadContextMap(true);
        assertTrue(map.isEmpty());
        map.put("key", "value");
        map.put("key2", "value2");
        assertEquals("value", map.get("key"));
        assertEquals("value2", map.get("key2"));

        map.clear();
        assertTrue(map.isEmpty());
        assertFalse(map.containsKey("key"));
        assertFalse(map.containsKey("key2"));
    }

    @Test
    public void testGetCopyReturnsMutableMap() {
        final DefaultThreadContextMap map = new DefaultThreadContextMap(true);
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
        final DefaultThreadContextMap map = new DefaultThreadContextMap(true);
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
        final DefaultThreadContextMap map = new DefaultThreadContextMap(true);
        assertTrue(map.isEmpty());
        assertNull(map.getImmutableMapOrNull());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetImmutableMapReturnsImmutableMapIfNonEmpty() {
        final DefaultThreadContextMap map = new DefaultThreadContextMap(true);
        map.put("key1", "value1");
        assertFalse(map.isEmpty());

        final Map<String, String> immutable = map.getImmutableMapOrNull();
        assertEquals("value1", immutable.get("key1")); // copy has values too

        // immutable
        immutable.put("key", "value"); // error
    }

    @Test
    public void testGetImmutableMapCopyNotAffectdByContextMapChanges() {
        final DefaultThreadContextMap map = new DefaultThreadContextMap(true);
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
        final DefaultThreadContextMap map = new DefaultThreadContextMap(true);
        assertEquals("{}", map.toString());

        map.put("key1", "value1");
        assertEquals("{key1=value1}", map.toString());

        map.remove("key1");
        map.put("key2", "value2");
        assertEquals("{key2=value2}", map.toString());
    }
}
