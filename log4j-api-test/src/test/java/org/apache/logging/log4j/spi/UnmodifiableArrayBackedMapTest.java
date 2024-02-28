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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
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
    public void testCopyAndPut() {
        UnmodifiableArrayBackedMap testMap = UnmodifiableArrayBackedMap.EMPTY_MAP;
        testMap = testMap.copyAndPut("1", "value1");
        assertTrue(testMap.containsKey("1"));
        assertEquals(testMap.get("1"), "value1");

        testMap = testMap.copyAndPut("1", "another value");
        assertTrue(testMap.containsKey("1"));
        assertEquals(testMap.get("1"), "another value");

        HashMap<String, String> newValues = getTestParameters();
        testMap = testMap.copyAndPutAll(newValues);
        assertEquals(testMap.get("1"), "value1");
        assertEquals(testMap.get("4"), "value4");
    }

    @Test
    public void testInstanceCopy() {
        HashMap<String, String> params = getTestParameters();
        UnmodifiableArrayBackedMap testMap = UnmodifiableArrayBackedMap.EMPTY_MAP.copyAndPutAll(params);

        UnmodifiableArrayBackedMap testMap2 = new UnmodifiableArrayBackedMap(testMap);
        assertEquals(testMap, testMap2);
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
    public void testCopyAndRemove() {
        HashMap<String, String> params = getTestParameters();
        UnmodifiableArrayBackedMap testMap = UnmodifiableArrayBackedMap.EMPTY_MAP.copyAndPutAll(params);
        testMap = testMap.copyAndRemove("2");
        testMap = testMap.copyAndRemove("not_present");
        assertEquals(4, testMap.size());
        assertFalse(testMap.containsKey("2"));
        assertTrue(testMap.containsKey("1"));
        assertFalse(testMap.containsValue("value2"));
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
    public void testEqualsWhenOneValueDiffers() {
        HashMap<String, String> params = getTestParameters();
        UnmodifiableArrayBackedMap testMap = UnmodifiableArrayBackedMap.EMPTY_MAP.copyAndPutAll(params);
        assertNotEquals(params, testMap.copyAndPut("1", "different value"));
        assertNotEquals(testMap.copyAndPut("1", "different value"), params);
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
    public void testState() {
        UnmodifiableArrayBackedMap originalMap;
        UnmodifiableArrayBackedMap newMap;

        originalMap = UnmodifiableArrayBackedMap.EMPTY_MAP;
        newMap = UnmodifiableArrayBackedMap.getInstance(originalMap.getBackingArray());
        assertEquals(originalMap, newMap);

        originalMap = UnmodifiableArrayBackedMap.EMPTY_MAP.copyAndPutAll(getTestParameters());
        newMap = UnmodifiableArrayBackedMap.getInstance(originalMap.getBackingArray());
        assertEquals(originalMap, newMap);

        originalMap = UnmodifiableArrayBackedMap.EMPTY_MAP
                .copyAndPutAll(getTestParameters())
                .copyAndRemove("1");
        newMap = UnmodifiableArrayBackedMap.getInstance(originalMap.getBackingArray());
        assertEquals(originalMap, newMap);
    }
}
