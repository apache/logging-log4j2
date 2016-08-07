package org.apache.logging.log4j.core.impl;/*
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.core.ContextData;
import org.apache.logging.log4j.core.util.BiConsumer;
import org.apache.logging.log4j.core.util.TriConsumer;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the OpenHashMapContextData class.
 */
public class OpenHashMapContextDataTest {

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorDisallowsNegativeCapacity() throws Exception {
        new OpenHashMapContextData(-1);
    }

    @Test
    public void testConstructorAllowsZeroCapacity() throws Exception {
        OpenHashMapContextData data = new OpenHashMapContextData(0);
        assertEquals(2, data.arraySize);
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("unchecked")
    public void testConstructorDisallowsNullMap() throws Exception {
        assertEquals(0, new OpenHashMapContextData((Map) null).size());
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("unchecked")
    public void testConstructorDisallowsNullContextData() throws Exception {
        assertEquals(0, new OpenHashMapContextData((ContextData) null).size());
    }

    @Test
    public void testToString() {
        final OpenHashMapContextData original = new OpenHashMapContextData();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");
        assertEquals("{B=Bvalue, a=avalue, 3=3value}", original.toString());
    }

    @Test
    public void testSerialization() throws Exception {
        final OpenHashMapContextData original = new OpenHashMapContextData();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");

        final byte[] binary = serialize(original);
        final OpenHashMapContextData copy = deserialize(binary);
        assertEquals(original, copy);
    }

    private byte[] serialize(final OpenHashMapContextData data) throws IOException {
        final ByteArrayOutputStream arr = new ByteArrayOutputStream();
        final ObjectOutputStream out = new ObjectOutputStream(arr);
        out.writeObject(data);
        return arr.toByteArray();
    }

    private OpenHashMapContextData deserialize(final byte[] binary) throws IOException, ClassNotFoundException {
        final ByteArrayInputStream inArr = new ByteArrayInputStream(binary);
        final ObjectInputStream in = new ObjectInputStream(inArr);
        final OpenHashMapContextData result = (OpenHashMapContextData) in.readObject();
        return result;
    }

    @Test
    public void testPutAll() throws Exception {
        final OpenHashMapContextData original = new OpenHashMapContextData();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");

        final OpenHashMapContextData other = new OpenHashMapContextData();
        other.putAll(original);
        assertEquals(original, other);

        other.putValue("3", "otherValue");
        assertNotEquals(original, other);

        other.putValue("3", null);
        assertNotEquals(original, other);

        other.putValue("3", "3value");
        assertEquals(original, other);
    }

    @Test
    public void testEquals() {
        final OpenHashMapContextData original = new OpenHashMapContextData();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");
        assertEquals(original, original); // equal to itself

        final OpenHashMapContextData other = new OpenHashMapContextData();
        other.putValue("a", "avalue");
        assertNotEquals(original, other);

        other.putValue("B", "Bvalue");
        assertNotEquals(original, other);

        other.putValue("3", "3value");
        assertEquals(original, other);

        other.putValue("3", "otherValue");
        assertNotEquals(original, other);

        other.putValue("3", null);
        assertNotEquals(original, other);

        other.putValue("3", "3value");
        assertEquals(original, other);
    }

    @Test
    public void testAsMap() throws Exception {
        final OpenHashMapContextData original = new OpenHashMapContextData();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");

        final Map<String, Object> expected = new HashMap<>();
        expected.put("a", "avalue");
        expected.put("B", "Bvalue");
        expected.put("3", "3value");

        assertEquals(expected, original.asMap());
    }

    @Test
    public void testGetCopyDelegatesToAsMap() throws Exception {
        final OpenHashMapContextData original = new OpenHashMapContextData();
        original.putValue("a", "avalue");
        assertEquals(original.getCopy(), original.asMap());

        original.putValue("B", "Bvalue");
        assertEquals(original.getCopy(), original.asMap());

        original.putValue("3", "3value");
        assertEquals(original.getCopy(), original.asMap());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetImmutableMapOrNull() throws Exception {
        final OpenHashMapContextData original = new OpenHashMapContextData();
        original.putValue("a", "avalue");
        assertEquals(original.getImmutableMapOrNull(), original.asMap());

        original.putValue("B", "Bvalue");
        assertEquals(original.getImmutableMapOrNull(), original.asMap());

        original.putValue("3", "3value");
        assertEquals(original.getImmutableMapOrNull(), original.asMap());

        try {
            original.getImmutableMapOrNull().put("abc", "xyz");
            fail("Expected map to be immutable");
        } catch (final UnsupportedOperationException ok) {
            //ok
        }
    }

    @Test
    public void testPutInsertsInAlphabeticOrder() throws Exception {
        final OpenHashMapContextData original = new OpenHashMapContextData();
        original.put("a", "avalue");
        original.put("B", "Bvalue");
        original.put("3", "3value");
        original.put("c", "cvalue");
        original.put("d", "dvalue");

        assertEquals("avalue", original.getValue("a"));
        assertEquals("Bvalue", original.getValue("B"));
        assertEquals("3value", original.getValue("3"));
        assertEquals("cvalue", original.getValue("c"));
        assertEquals("dvalue", original.getValue("d"));
    }

    @Test
    public void testPutValueInsertsInAlphabeticOrder() throws Exception {
        final OpenHashMapContextData original = new OpenHashMapContextData();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");
        original.putValue("c", "cvalue");
        original.putValue("d", "dvalue");

        assertEquals("avalue", original.getValue("a"));
        assertEquals("Bvalue", original.getValue("B"));
        assertEquals("3value", original.getValue("3"));
        assertEquals("cvalue", original.getValue("c"));
        assertEquals("dvalue", original.getValue("d"));
    }

    @Test
    public void testNullKeysAllowed() {
        final OpenHashMapContextData original = new OpenHashMapContextData();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");
        original.putValue("c", "cvalue");
        original.putValue("d", "dvalue");
        assertEquals(5, original.size());
        assertEquals("{B=Bvalue, a=avalue, 3=3value, d=dvalue, c=cvalue}", original.toString());

        original.putValue(null, "nullvalue");
        assertEquals(6, original.size());
        assertEquals("{null=nullvalue, B=Bvalue, a=avalue, 3=3value, d=dvalue, c=cvalue}", original.toString());

        original.putValue(null, "otherNullvalue");
        assertEquals("{null=otherNullvalue, B=Bvalue, a=avalue, 3=3value, d=dvalue, c=cvalue}", original.toString());
        assertEquals(6, original.size());

        original.putValue(null, "nullvalue");
        assertEquals(6, original.size());
        assertEquals("{null=nullvalue, B=Bvalue, a=avalue, 3=3value, d=dvalue, c=cvalue}", original.toString());

        original.putValue(null, "abc");
        assertEquals(6, original.size());
        assertEquals("{null=abc, B=Bvalue, a=avalue, 3=3value, d=dvalue, c=cvalue}", original.toString());
    }

    @Test
    public void testNullKeysCopiedToAsMap() {
        final OpenHashMapContextData original = new OpenHashMapContextData();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");
        original.putValue("c", "cvalue");
        original.putValue("d", "dvalue");
        assertEquals(5, original.size());

        HashMap<String, String> expected = new HashMap<>();
        expected.put("a", "avalue");
        expected.put("B", "Bvalue");
        expected.put("3", "3value");
        expected.put("c", "cvalue");
        expected.put("d", "dvalue");
        assertEquals("initial", expected, original.asMap());

        original.putValue(null, "nullvalue");
        expected.put(null, "nullvalue");
        assertEquals(6, original.size());
        assertEquals("with null key", expected, original.asMap());

        original.putValue(null, "otherNullvalue");
        expected.put(null, "otherNullvalue");
        assertEquals(6, original.size());
        assertEquals("with null key value2", expected, original.asMap());

        original.putValue(null, "nullvalue");
        expected.put(null, "nullvalue");
        assertEquals(6, original.size());
        assertEquals("with null key value1 again", expected, original.asMap());

        original.putValue(null, "abc");
        expected.put(null, "abc");
        assertEquals(6, original.size());
        assertEquals("with null key value3", expected, original.asMap());
    }

    @Test
    public void testRemove() {
        final OpenHashMapContextData original = new OpenHashMapContextData();
        original.putValue("a", "avalue");
        assertEquals(1, original.size());
        assertEquals("avalue", original.getValue("a"));

        original.remove("a");
        assertEquals(0, original.size());
        assertNull("no a val", original.getValue("a"));

        original.remove("B");
        assertEquals(0, original.size());
        assertNull("no B val", original.getValue("B"));
    }

    @Test
    public void testNullValuesArePreserved() {
        final OpenHashMapContextData original = new OpenHashMapContextData();
        original.putValue("a", "avalue");
        assertEquals(1, original.size());
        assertEquals("avalue", original.getValue("a"));

        original.putValue("a", null);
        assertEquals(1, original.size());
        assertNull("no a val", original.getValue("a"));

        original.putValue("B", null);
        assertEquals(2, original.size());
        assertNull("no B val", original.getValue("B"));
    }

    @Test
    public void testGet() throws Exception {
        final OpenHashMapContextData original = new OpenHashMapContextData();
        original.put("a", "avalue");
        original.put("B", "Bvalue");
        original.put("3", "3value");

        assertEquals("avalue", original.get("a"));
        assertEquals("Bvalue", original.get("B"));
        assertEquals("3value", original.get("3"));

        original.putValue("0", "0value");
        assertEquals("0value", original.get("0"));
        assertEquals("3value", original.get("3"));
        assertEquals("Bvalue", original.get("B"));
        assertEquals("avalue", original.get("a"));
    }

    @Test
    public void testGetValue_GetValueAt() throws Exception {
        final OpenHashMapContextData original = new OpenHashMapContextData();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");

        assertEquals("avalue", original.getValue("a"));
        assertEquals("Bvalue", original.getValue("B"));
        assertEquals("3value", original.getValue("3"));
        original.putValue("0", "0value");
        assertEquals("0value", original.getValue("0"));
        assertEquals("3value", original.getValue("3"));
        assertEquals("Bvalue", original.getValue("B"));
        assertEquals("avalue", original.getValue("a"));
    }

    @Test
    public void testClear() throws Exception {
        final OpenHashMapContextData original = new OpenHashMapContextData();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");
        assertEquals(3, original.size());

        original.clear();
        assertEquals(0, original.size());

        // ensure slots in the values array are nulled out
        Field f = OpenHashMapContextData.class.getDeclaredField("values");
        f.setAccessible(true);
        Object[] values = (Object[]) f.get(original);
        for (int i = 0; i < values.length; i++) {
            assertNull(values[i]);
        }
    }

    @Test
    public void testContainsKey() throws Exception {
        final OpenHashMapContextData original = new OpenHashMapContextData();
        assertFalse("a", original.containsKey("a"));
        assertFalse("B", original.containsKey("B"));
        assertFalse("3", original.containsKey("3"));
        assertFalse("A", original.containsKey("A"));

        original.putValue("a", "avalue");
        assertTrue("a", original.containsKey("a"));
        assertFalse("B", original.containsKey("B"));
        assertFalse("3", original.containsKey("3"));
        assertFalse("A", original.containsKey("A"));

        original.putValue("B", "Bvalue");
        assertTrue("a", original.containsKey("a"));
        assertTrue("B", original.containsKey("B"));
        assertFalse("3", original.containsKey("3"));
        assertFalse("A", original.containsKey("A"));

        original.putValue("3", "3value");
        assertTrue("a", original.containsKey("a"));
        assertTrue("B", original.containsKey("B"));
        assertTrue("3", original.containsKey("3"));
        assertFalse("A", original.containsKey("A"));

        original.putValue("A", "AAA");
        assertTrue("a", original.containsKey("a"));
        assertTrue("B", original.containsKey("B"));
        assertTrue("3", original.containsKey("3"));
        assertTrue("A", original.containsKey("A"));
    }

    @Test
    public void testSizeAndIsEmpty() throws Exception {
        final OpenHashMapContextData original = new OpenHashMapContextData();
        assertEquals(0, original.size());
        assertTrue("initial", original.isEmpty());

        original.putValue("a", "avalue");
        assertEquals(1, original.size());
        assertFalse("size=" + original.size(), original.isEmpty());

        original.putValue("B", "Bvalue");
        assertEquals(2, original.size());
        assertFalse("size=" + original.size(), original.isEmpty());

        original.putValue("3", "3value");
        assertEquals(3, original.size());
        assertFalse("size=" + original.size(), original.isEmpty());

        original.remove("B");
        assertEquals(2, original.size());
        assertFalse("size=" + original.size(), original.isEmpty());

        original.remove("3");
        assertEquals(1, original.size());
        assertFalse("size=" + original.size(), original.isEmpty());

        original.remove("a");
        assertEquals(0, original.size());
        assertTrue("size=" + original.size(), original.isEmpty());
    }

    @Test
    public void testForEachBiConsumer() throws Exception {
        final OpenHashMapContextData original = new OpenHashMapContextData();
        final List<String> keys = new ArrayList<>(Arrays.asList("a", "B", "3", null));
        final List<String> values = new ArrayList<>(Arrays.asList("aValue", "Bvalue", "3Value", "nullValue"));
        for (int i = 0; i < keys.size(); i++) {
            original.put(keys.get(i), values.get(i));
        }

        original.forEach(new BiConsumer<String, String>() {
            int count = 0;
            @Override
            public void accept(final String key, final String value) {
                assertTrue("key exists", keys.remove(key));
                assertTrue("val exists", values.remove(value));
                assertEquals("val", value, original.getValue(key));
                count++;
                assertTrue("count should not exceed size but was " + count, count <= original.size());
            }
        });
    }

    static class State<K, V> {
        OpenHashMapContextData data;
        int count;
        List<K> keys;
        List<V> values;
    }
    private static TriConsumer<String, String, State> COUNTER = new TriConsumer<String, String, State>() {
        @Override
        public void accept(final String key, final String value, final State state) {
            assertTrue("key exists", state.keys.remove(key));
            assertTrue("val exists", state.values.remove(value));
            assertEquals("val", value, state.data.getValue(key));
            state.count++;
            assertTrue("count should not exceed size but was " + state.count,
                    state.count <= state.data.size());
        }
    };

    @Test
    public void testForEachTriConsumer() throws Exception {
        final OpenHashMapContextData original = new OpenHashMapContextData();
        final List<String> keys = Arrays.asList("a", "B", "3", null);
        final List<String> values = Arrays.asList("aValue", "Bvalue", "3Value", "nullValue");
        for (int i = 0; i < keys.size(); i++) {
            original.put(keys.get(i), values.get(i));
        }
        final State state = new State();
        state.data = original;
        state.keys = new ArrayList(keys);
        state.values = new ArrayList(values);

        original.forEach(COUNTER, state);
        assertEquals(state.count, original.size());
        assertTrue("all keys", state.keys.isEmpty());
        assertTrue("all values", state.values.isEmpty());
    }
}