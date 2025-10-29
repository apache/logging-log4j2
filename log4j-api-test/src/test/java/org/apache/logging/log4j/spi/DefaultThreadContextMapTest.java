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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.logging.log4j.test.junit.UsingThreadContextMap;
import org.apache.logging.log4j.test.spi.ThreadContextMapSuite;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@code DefaultThreadContextMap} class.
 */
@UsingThreadContextMap
class DefaultThreadContextMapTest extends ThreadContextMapSuite {

    private ThreadContextMap createThreadContextMap() {
        return new DefaultThreadContextMap();
    }

    private ThreadContextMap createInheritableThreadContextMap() {
        final Properties props = new Properties();
        props.setProperty("log4j2.isThreadContextMapInheritable", "true");
        final PropertiesUtil util = new PropertiesUtil(props);
        return new DefaultThreadContextMap(util);
    }

    @Test
    void singleValue() {
        singleValue(createThreadContextMap());
    }

    @Test
    void testPutAll() {
        final DefaultThreadContextMap map = new DefaultThreadContextMap();
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

    @Test
    void testClear() {
        final DefaultThreadContextMap map = createMap();

        map.clear();
        assertTrue(map.isEmpty());
        assertFalse(map.containsKey("key"));
        assertFalse(map.containsKey("key2"));
    }

    private DefaultThreadContextMap createMap() {
        final DefaultThreadContextMap map = new DefaultThreadContextMap();
        assertTrue(map.isEmpty());
        map.put("key", "value");
        map.put("key2", "value2");
        assertEquals("value", map.get("key"));
        assertEquals("value2", map.get("key2"));
        return map;
    }

    @Test
    void getCopyReturnsMutableCopy() {
        getCopyReturnsMutableCopy(createThreadContextMap());
    }

    @Test
    void getImmutableMapReturnsNullIfEmpty() {
        getImmutableMapReturnsNullIfEmpty(createThreadContextMap());
    }

    @Test
    void getImmutableMapReturnsImmutableMapIfNonEmpty() {
        getImmutableMapReturnsImmutableMapIfNonEmpty(createThreadContextMap());
    }

    @Test
    void getImmutableMapCopyNotAffectedByContextMapChanges() {
        getImmutableMapCopyNotAffectedByContextMapChanges(createThreadContextMap());
    }

    @Test
    void testToStringShowsMapContext() {
        final DefaultThreadContextMap map = new DefaultThreadContextMap();
        assertEquals("{}", map.toString());

        map.put("key1", "value1");
        assertEquals("{key1=value1}", map.toString());

        map.remove("key1");
        map.put("key2", "value2");
        assertEquals("{key2=value2}", map.toString());
    }

    @Test
    void threadLocalNotInheritableByDefault() {
        threadLocalNotInheritableByDefault(createThreadContextMap());
    }

    @Test
    void threadLocalInheritableIfConfigured() {
        threadLocalInheritableIfConfigured(createInheritableThreadContextMap());
    }

    @Test
    void testGetCopyWithEmptyMap() {
        final DefaultThreadContextMap contextMap = new DefaultThreadContextMap();

        // Verify map is empty
        assertTrue(contextMap.isEmpty());
        assertEquals(0, contextMap.size());

        // Get copy of empty map
        final Map<String, String> copy = contextMap.getCopy();

        // Verify copy is empty HashMap
        assertThat(copy).isInstanceOf(HashMap.class);
        assertTrue(copy.isEmpty());
        assertEquals(0, copy.size());

        // Verify copy is independent
        copy.put("test", "value");
        assertTrue(contextMap.isEmpty());
    }

    @Test
    void testGetCopyWithSingleElement() {
        final DefaultThreadContextMap contextMap = new DefaultThreadContextMap();

        // Add single element
        contextMap.put("key1", "value1");
        assertEquals(1, contextMap.size());
        assertEquals("value1", contextMap.get("key1"));

        // Get copy
        final Map<String, String> copy = contextMap.getCopy();

        // Verify copy contains identical data
        assertThat(copy).isInstanceOf(HashMap.class);
        assertEquals(1, copy.size());
        assertEquals("value1", copy.get("key1"));
        assertTrue(copy.containsKey("key1"));

        // Verify copy is independent
        assertNotSame(copy, contextMap.toMap());
        copy.put("key2", "value2");
        assertEquals(1, contextMap.size());
        assertFalse(contextMap.containsKey("key2"));
    }

    @Test
    void testGetCopyWithMultipleElements() {
        final DefaultThreadContextMap contextMap = new DefaultThreadContextMap();

        // Add multiple elements
        final Map<String, String> testData = new HashMap<>();
        testData.put("key1", "value1");
        testData.put("key2", "value2");
        testData.put("key3", "value3");
        testData.put("key4", "value4");
        testData.put("key5", "value5");

        contextMap.putAll(testData);
        assertEquals(5, contextMap.size());

        // Get copy
        final Map<String, String> copy = contextMap.getCopy();

        // Verify copy contains identical data
        assertThat(copy).isInstanceOf(HashMap.class);
        assertEquals(5, copy.size());

        // Verify all entries match
        assertEquals(testData, copy);

        // Verify copy is independent
        copy.clear();
        assertEquals(5, contextMap.size());
    }

    @Test
    void testGetCopyReturnsHashMap() {
        final DefaultThreadContextMap contextMap = new DefaultThreadContextMap();

        // Test with empty map
        Map<String, String> copy = contextMap.getCopy();
        assertThat(copy).isInstanceOf(HashMap.class);

        // Test with populated map
        contextMap.put("key", "value");
        copy = contextMap.getCopy();
        assertThat(copy).isInstanceOf(HashMap.class);
    }

    @Test
    void testGetCopyIndependence() {
        final DefaultThreadContextMap contextMap = new DefaultThreadContextMap();

        // Setup initial data
        contextMap.put("key1", "value1");
        contextMap.put("key2", "value2");

        final Map<String, String> copy1 = contextMap.getCopy();
        final Map<String, String> copy2 = contextMap.getCopy();

        // Verify copies are independent of each other
        assertNotSame(copy1, copy2);
        assertEquals(copy1, copy2);

        // Modify first copy
        copy1.put("key3", "value3");
        copy1.remove("key1");

        // Verify second copy is unaffected
        assertEquals(2, copy2.size());
        assertTrue(copy2.containsKey("key1"));
        assertFalse(copy2.containsKey("key3"));

        // Verify original map is unaffected
        assertEquals(2, contextMap.size());
        assertTrue(contextMap.containsKey("key1"));
        assertFalse(contextMap.containsKey("key3"));
    }
}
