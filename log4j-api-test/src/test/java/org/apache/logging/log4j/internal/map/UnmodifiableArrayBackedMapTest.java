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
package org.apache.logging.log4j.internal.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.TriConsumer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class UnmodifiableArrayBackedMapTest {
    private static final int TEST_DATA_SIZE = 5;

    private HashMap<String, String> getTestParameters() {
        return getTestParameters(TEST_DATA_SIZE);
    }

    private HashMap<String, String> getTestParameters(int numParams) {
        HashMap<String, String> params = new LinkedHashMap<>();
        for (int i = 0; i < numParams; i++) {
            params.put("" + i, "value" + i);
        }

        return params;
    }

    @Test
    public void testCopyAndPut() {
        UnmodifiableArrayBackedMap testMap = UnmodifiableArrayBackedMap.EMPTY_MAP;
        testMap = testMap.copyAndPut("6", "value6");
        assertTrue(testMap.containsKey("6"));
        assertEquals(testMap.get("6"), "value6");

        testMap = testMap.copyAndPut("6", "another value");
        assertTrue(testMap.containsKey("6"));
        assertEquals(testMap.get("6"), "another value");

        HashMap<String, String> newValues = getTestParameters();
        testMap = testMap.copyAndPutAll(newValues);
        assertEquals(testMap.get("1"), "value1");
        assertEquals(testMap.get("4"), "value4");
        assertEquals(testMap.get("6"), "another value");
    }

    @Test
    public void testCopyAndRemove() {
        // general removing from well-populated set
        HashMap<String, String> params = getTestParameters();
        UnmodifiableArrayBackedMap testMap = UnmodifiableArrayBackedMap.EMPTY_MAP.copyAndPutAll(params);
        testMap = testMap.copyAndRemove("2");
        testMap = testMap.copyAndRemove("not_present");
        assertEquals(4, testMap.size());
        assertFalse(testMap.containsKey("2"));
        assertTrue(testMap.containsKey("1"));
        assertFalse(testMap.containsValue("value2"));

        // test removing from empty set
        testMap = UnmodifiableArrayBackedMap.EMPTY_MAP.copyAndPut("test", "test");
        testMap = testMap.copyAndRemove("test");
        assertTrue(testMap.isEmpty());

        // test removing first of two elements
        testMap = UnmodifiableArrayBackedMap.EMPTY_MAP.copyAndPut("test1", "test1");
        testMap = testMap.copyAndPut("test2", "test2");
        testMap = testMap.copyAndRemove("test1");
        assertFalse(testMap.containsKey("test1"));
        assertTrue(testMap.containsKey("test2"));

        // test removing second of two elements
        testMap = UnmodifiableArrayBackedMap.EMPTY_MAP.copyAndPut("test1", "test1");
        testMap = testMap.copyAndPut("test2", "test2");
        testMap = testMap.copyAndRemove("test2");
        assertTrue(testMap.containsKey("test1"));
        assertFalse(testMap.containsKey("test2"));
    }

    @Test
    public void testCopyAndRemoveAll() {
        HashMap<String, String> initialMapContents = getTestParameters(15);
        initialMapContents.put("extra_key", "extra_value");

        HashSet<String> keysToRemove = new LinkedHashSet<>();
        keysToRemove.add("3");
        keysToRemove.add("11");
        keysToRemove.add("definitely_not_found");

        UnmodifiableArrayBackedMap testMap = UnmodifiableArrayBackedMap.EMPTY_MAP.copyAndPutAll(initialMapContents);
        testMap = testMap.copyAndRemoveAll(keysToRemove);
        assertEquals(14, testMap.size());

        assertFalse(testMap.containsKey("3"));
        assertFalse(testMap.containsValue("value3"));
        assertFalse(testMap.containsKey("11"));
        assertFalse(testMap.containsValue("value11"));

        assertTrue(testMap.containsKey("extra_key"));
        assertTrue(testMap.containsValue("extra_value"));
        assertTrue(testMap.containsKey("1"));
        assertTrue(testMap.containsValue("value1"));
        assertTrue(testMap.containsKey("0"));
        assertTrue(testMap.containsValue("value0"));
        assertTrue(testMap.containsKey("14"));
        assertTrue(testMap.containsValue("value14"));

        testMap = UnmodifiableArrayBackedMap.EMPTY_MAP.copyAndPutAll(initialMapContents);
        UnmodifiableArrayBackedMap testMapWithArrayListRemoval =
                testMap.copyAndRemoveAll(new ArrayList<>(keysToRemove));
        UnmodifiableArrayBackedMap testMapWithSetRemoval = testMap.copyAndRemoveAll(keysToRemove);
        assertEquals(testMapWithSetRemoval, testMapWithArrayListRemoval);

        testMap = UnmodifiableArrayBackedMap.EMPTY_MAP;
        assertEquals(testMap.copyAndRemoveAll(initialMapContents.keySet()).size(), 0);

        testMap = UnmodifiableArrayBackedMap.EMPTY_MAP.copyAndPut("test", "test");
        assertEquals(testMap.copyAndRemoveAll(initialMapContents.keySet()).size(), 1);
        testMap = testMap.copyAndRemoveAll(Collections.singleton("not found"));
        assertEquals(testMap.copyAndRemoveAll(testMap.keySet()).size(), 0);
        testMap = testMap.copyAndRemoveAll(Collections.singleton("test"));
        assertEquals(testMap.copyAndRemoveAll(testMap.keySet()).size(), 0);
    }

    @Test
    public void testEmptyMap() {
        assertNull(UnmodifiableArrayBackedMap.EMPTY_MAP.get("test"));
    }

    @Test
    public void testEntrySetIteratorAndSize() {
        UnmodifiableArrayBackedMap testMap = UnmodifiableArrayBackedMap.EMPTY_MAP.copyAndPutAll(getTestParameters());
        Set<Map.Entry<String, String>> entrySet = testMap.entrySet();
        int numEntriesFound = 0;
        for (@SuppressWarnings("unused") Map.Entry<String, String> entry : entrySet) {
            numEntriesFound++;
        }

        assertEquals(testMap.size(), numEntriesFound);
        assertEquals(testMap.size(), entrySet.size());
    }

    @Test
    public void testEntrySetMutatorsBlocked() {
        UnmodifiableArrayBackedMap testMap = UnmodifiableArrayBackedMap.EMPTY_MAP.copyAndPutAll(getTestParameters());
        Set<Map.Entry<String, String>> entrySet = testMap.entrySet();
        for (Map.Entry<String, String> entry : entrySet) {
            try {
                entry.setValue("test");
                fail("Entry.setValue() wasn't blocked");
            } catch (UnsupportedOperationException e) {
            }
        }
        for (@SuppressWarnings("unused") Map.Entry<String, String> entry : entrySet) {
            try {
                entrySet.add(null);
                fail("EntrySet.add() wasn't blocked");
            } catch (UnsupportedOperationException e) {
            }
        }
        for (@SuppressWarnings("unused") Map.Entry<String, String> entry : entrySet) {
            try {
                entrySet.addAll(new HashSet<>());
                fail("EntrySet.addAll() wasn't blocked");
            } catch (UnsupportedOperationException e) {
            }
        }
    }

    /**
     * Tests various situations with .equals(). Test tries comparisons in both
     * directions, to make sure that HashMap.equals(UnmodifiableArrayBackedMap) work
     * as well as UnmodifiableArrayBackedMap.equals(HashMap).
     */
    @Test
    public void testEqualsHashCodeWithIdenticalContent() {
        HashMap<String, String> params = getTestParameters();
        UnmodifiableArrayBackedMap testMap = UnmodifiableArrayBackedMap.EMPTY_MAP.copyAndPutAll(params);
        assertEquals(params, testMap);
        assertEquals(testMap, params);
        assertEquals(params.hashCode(), testMap.hashCode());
    }

    @Test
    public void testEqualsHashCodeWithOneEmptyMap() {
        HashMap<String, String> params = getTestParameters();
        UnmodifiableArrayBackedMap testMap = UnmodifiableArrayBackedMap.EMPTY_MAP.copyAndPutAll(params);
        // verify empty maps are not equal to non-empty maps
        assertNotEquals(params, UnmodifiableArrayBackedMap.EMPTY_MAP);
        assertNotEquals(new HashMap<>(), testMap);
        assertNotEquals(UnmodifiableArrayBackedMap.EMPTY_MAP, params);
        assertNotEquals(testMap, new HashMap<>());
    }

    @Test
    public void testEqualsHashCodeWithOneKeyRemoved() {
        HashMap<String, String> params = getTestParameters();
        UnmodifiableArrayBackedMap testMap = UnmodifiableArrayBackedMap.EMPTY_MAP.copyAndPutAll(params);

        params.remove("1");
        assertNotEquals(params, testMap);
        assertNotEquals(testMap, params);

        testMap = testMap.copyAndRemove("1").copyAndRemove("2");
        assertNotEquals(params, testMap);
        assertNotEquals(testMap, params);
    }

    @Test
    public void testEqualsWhenOneValueDiffers() {
        HashMap<String, String> params = getTestParameters();
        UnmodifiableArrayBackedMap testMap = UnmodifiableArrayBackedMap.EMPTY_MAP.copyAndPutAll(params);
        assertNotEquals(params, testMap.copyAndPut("1", "different value"));
        assertNotEquals(testMap.copyAndPut("1", "different value"), params);
    }

    @Test
    public void testForEachBiConsumer_JavaUtil() {
        final Map<String, String> map = UnmodifiableArrayBackedMap.EMPTY_MAP.copyAndPutAll(getTestParameters());
        final Collection<String> keys = new HashSet<>();
        map.forEach((key, value) -> keys.add(key));
        assertEquals(map.keySet(), keys);
    }

    @Test
    public void testForEachBiConsumer_Log4jUtil() {
        final ReadOnlyStringMap map = UnmodifiableArrayBackedMap.EMPTY_MAP.copyAndPutAll(getTestParameters());
        final Collection<String> keys = new HashSet<>();
        map.forEach((key, value) -> keys.add(key));
        assertEquals(map.toMap().keySet(), keys);
    }

    @Test
    public void testForEachTriConsumer() {
        UnmodifiableArrayBackedMap map = UnmodifiableArrayBackedMap.EMPTY_MAP.copyAndPutAll(getTestParameters());
        HashMap<String, String> iterationResultMap = new HashMap<>();
        TriConsumer<String, String, Map<String, String>> triConsumer =
                new TriConsumer<String, String, Map<String, String>>() {
                    @Override
                    public void accept(String k, String v, Map<String, String> s) {
                        s.put(k, v);
                    }
                };
        map.forEach(triConsumer, iterationResultMap);
        assertEquals(map, iterationResultMap);
    }

    @Test
    public void testImmutability() {
        HashMap<String, String> params = getTestParameters();
        UnmodifiableArrayBackedMap originalMap = UnmodifiableArrayBackedMap.EMPTY_MAP.copyAndPutAll(params);
        UnmodifiableArrayBackedMap modifiedMap = originalMap.copyAndPutAll(getTestParameters());
        assertEquals(originalMap, params);

        modifiedMap = modifiedMap.copyAndRemoveAll(modifiedMap.keySet());
        assertTrue(modifiedMap.isEmpty());

        assertEquals(originalMap, params);
    }

    @Test
    public void testInstanceCopy() {
        HashMap<String, String> params = getTestParameters();
        UnmodifiableArrayBackedMap testMap = UnmodifiableArrayBackedMap.EMPTY_MAP.copyAndPutAll(params);

        UnmodifiableArrayBackedMap testMap2 = new UnmodifiableArrayBackedMap(testMap);
        assertEquals(testMap, testMap2);
    }

    @Test
    public void testMutatorsBlocked() {
        UnmodifiableArrayBackedMap testMap = UnmodifiableArrayBackedMap.EMPTY_MAP.copyAndPutAll(getTestParameters());
        try {
            testMap.put("a", "a");
            fail("put() wasn't blocked");
        } catch (UnsupportedOperationException e) {
        }

        try {
            testMap.putAll(new HashMap<>());
            fail("putAll() wasn't blocked");
        } catch (UnsupportedOperationException e) {
        }

        try {
            testMap.remove("1");
            fail("remove() wasn't blocked");
        } catch (UnsupportedOperationException e) {
        }

        try {
            testMap.clear();
            fail("clear() wasn't blocked");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testNullValue() {
        UnmodifiableArrayBackedMap testMap = UnmodifiableArrayBackedMap.EMPTY_MAP;
        testMap = testMap.copyAndPut("key", null);
        assertTrue(testMap.containsKey("key"));
        assertTrue(testMap.containsValue(null));
        assertTrue(testMap.size() == 1);
        assertEquals(testMap.get("key"), null);
    }

    @Test
    public void testReads() {
        assertEquals(UnmodifiableArrayBackedMap.EMPTY_MAP.get("test"), null);
        HashMap<String, String> params = getTestParameters();
        UnmodifiableArrayBackedMap testMap = UnmodifiableArrayBackedMap.EMPTY_MAP.copyAndPutAll(params);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            assertTrue(testMap.containsKey(key));
            assertTrue(testMap.containsValue(value));
            assertEquals(testMap.get(key), params.get(key));
        }
        assertFalse(testMap.containsKey("not_present"));
        assertFalse(testMap.containsValue("not_present"));
        assertEquals(null, testMap.get("not_present"));
    }

    @Test
    public void testState() {
        UnmodifiableArrayBackedMap originalMap;
        UnmodifiableArrayBackedMap newMap;

        originalMap = UnmodifiableArrayBackedMap.EMPTY_MAP;
        newMap = UnmodifiableArrayBackedMap.getMap(originalMap.getBackingArray());
        assertEquals(originalMap, newMap);

        originalMap = UnmodifiableArrayBackedMap.EMPTY_MAP.copyAndPutAll(getTestParameters());
        newMap = UnmodifiableArrayBackedMap.getMap(originalMap.getBackingArray());
        assertEquals(originalMap, newMap);

        originalMap = UnmodifiableArrayBackedMap.EMPTY_MAP
                .copyAndPutAll(getTestParameters())
                .copyAndRemove("1");
        newMap = UnmodifiableArrayBackedMap.getMap(originalMap.getBackingArray());
        assertEquals(originalMap, newMap);
    }

    @Test
    public void testToMap() {
        UnmodifiableArrayBackedMap map = UnmodifiableArrayBackedMap.EMPTY_MAP.copyAndPut("test", "test");
        // verify same instance, not just equals()
        assertTrue(map == map.toMap());
    }

    @Test
    void copyAndRemoveAll_should_work() {

        // Create the actual map
        UnmodifiableArrayBackedMap actualMap = UnmodifiableArrayBackedMap.EMPTY_MAP;
        actualMap = actualMap.copyAndPut("outer", "two");
        actualMap = actualMap.copyAndPut("inner", "one");
        actualMap = actualMap.copyAndPut("not-in-closeable", "true");

        // Create the expected map
        UnmodifiableArrayBackedMap expectedMap = UnmodifiableArrayBackedMap.EMPTY_MAP;
        expectedMap = expectedMap.copyAndPut("outer", "two");
        expectedMap = expectedMap.copyAndPut("not-in-closeable", "true");

        // Remove the key and verify
        actualMap = actualMap.copyAndRemoveAll(Collections.singleton("inner"));
        Assertions.assertThat(actualMap).isEqualTo(expectedMap);
    }
}
