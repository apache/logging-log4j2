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
package org.apache.logging.log4j.core.context.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.logging.log4j.spi.ThreadContextMap;
import org.apache.logging.log4j.test.spi.ThreadContextMapSuite;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.junit.jupiter.api.Test;

public class GarbageFreeSortedArrayThreadContextMapTest extends ThreadContextMapSuite {

    private GarbageFreeSortedArrayThreadContextMap createThreadContextMap() {
        return new GarbageFreeSortedArrayThreadContextMap();
    }

    private ThreadContextMap createInheritableThreadContextMap() {
        final Properties props = new Properties();
        props.setProperty("log4j2.isThreadContextMapInheritable", "true");
        final PropertiesUtil util = new PropertiesUtil(props);
        return new GarbageFreeSortedArrayThreadContextMap(util);
    }

    @Test
    void singleValue() {
        singleValue(createThreadContextMap());
    }

    @Test
    void testPutAll() {
        final GarbageFreeSortedArrayThreadContextMap map = createThreadContextMap();
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
        final GarbageFreeSortedArrayThreadContextMap map = createMap();

        map.clear();
        assertTrue(map.isEmpty());
        assertFalse(map.containsKey("key"));
        assertFalse(map.containsKey("key2"));
    }

    private GarbageFreeSortedArrayThreadContextMap createMap() {
        final GarbageFreeSortedArrayThreadContextMap map = createThreadContextMap();
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
    void threadLocalNotInheritableByDefault() {
        threadLocalNotInheritableByDefault(createThreadContextMap());
    }

    @Test
    void threadLocalInheritableIfConfigured() {
        threadLocalInheritableIfConfigured(createInheritableThreadContextMap());
    }
}
