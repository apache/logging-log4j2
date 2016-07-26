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
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.core.util.BiConsumer;
import org.apache.logging.log4j.core.util.TriConsumer;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the ArrayContextData class.
 */
public class ArrayContextDataTest {

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorDisallowsNegativeCapacity() throws Exception {
        new ArrayContextData(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorDisallowsZeroCapacity() throws Exception {
        new ArrayContextData(0);
    }

    @Test
    public void testConstructorIgnoresNull() throws Exception {
        assertEquals(0, new ArrayContextData(null).size());
    }

    @Test
    public void testToString() {
        final ArrayContextData original = new ArrayContextData();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");
        assertEquals("{3=3value, B=Bvalue, a=avalue}", original.toString());
    }

    @Test
    public void testSerialization() throws Exception {
        final ArrayContextData original = new ArrayContextData();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");

        final byte[] binary = serialize(original);
        final ArrayContextData copy = deserialize(binary);
        assertEquals(original, copy);
    }

    private byte[] serialize(final ArrayContextData data) throws IOException {
        final ByteArrayOutputStream arr = new ByteArrayOutputStream();
        final ObjectOutputStream out = new ObjectOutputStream(arr);
        out.writeObject(data);
        return arr.toByteArray();
    }

    private ArrayContextData deserialize(final byte[] binary) throws IOException, ClassNotFoundException {
        final ByteArrayInputStream inArr = new ByteArrayInputStream(binary);
        final ObjectInputStream in = new ObjectInputStream(inArr);
        final ArrayContextData result = (ArrayContextData) in.readObject();
        return result;
    }

    @Test
    public void testPutAll() throws Exception {
        final ArrayContextData original = new ArrayContextData();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");

        final ArrayContextData other = new ArrayContextData();
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
        final ArrayContextData original = new ArrayContextData();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");
        assertEquals(original, original); // equal to itself

        final ArrayContextData other = new ArrayContextData();
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
        final ArrayContextData original = new ArrayContextData();
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
        final ArrayContextData original = new ArrayContextData();
        original.putValue("a", "avalue");
        assertEquals(original.getCopy(), original.asMap());

        original.putValue("B", "Bvalue");
        assertEquals(original.getCopy(), original.asMap());

        original.putValue("3", "3value");
        assertEquals(original.getCopy(), original.asMap());
    }

    @Test
    public void testGetImmutableMapOrNull() throws Exception {
        final ArrayContextData original = new ArrayContextData();
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
        final ArrayContextData original = new ArrayContextData();
        original.put("a", "avalue");
        original.put("B", "Bvalue");
        original.put("3", "3value");
        original.put("c", "cvalue");
        original.put("d", "dvalue");

        assertEquals("avalue", original.getValue("a"));
        assertEquals("avalue", original.getValueAt(2));

        assertEquals("Bvalue", original.getValue("B"));
        assertEquals("Bvalue", original.getValueAt(1));

        assertEquals("3value", original.getValue("3"));
        assertEquals("3value", original.getValueAt(0));

        assertEquals("cvalue", original.getValue("c"));
        assertEquals("cvalue", original.getValueAt(3));

        assertEquals("dvalue", original.getValue("d"));
        assertEquals("dvalue", original.getValueAt(4));
    }

    @Test
    public void testPutValueInsertsInAlphabeticOrder() throws Exception {
        final ArrayContextData original = new ArrayContextData();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");
        original.putValue("c", "cvalue");
        original.putValue("d", "dvalue");

        assertEquals("avalue", original.getValue("a"));
        assertEquals("avalue", original.getValueAt(2));

        assertEquals("Bvalue", original.getValue("B"));
        assertEquals("Bvalue", original.getValueAt(1));

        assertEquals("3value", original.getValue("3"));
        assertEquals("3value", original.getValueAt(0));

        assertEquals("cvalue", original.getValue("c"));
        assertEquals("cvalue", original.getValueAt(3));

        assertEquals("dvalue", original.getValue("d"));
        assertEquals("dvalue", original.getValueAt(4));
    }

    @Test
    public void testNullKeysAllowed() {
        final ArrayContextData original = new ArrayContextData();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");
        original.putValue("c", "cvalue");
        original.putValue("d", "dvalue");
        assertEquals(5, original.size());
        assertEquals("{3=3value, B=Bvalue, a=avalue, c=cvalue, d=dvalue}", original.toString());

        original.putValue(null, "nullvalue");
        assertEquals(6, original.size());
        assertEquals("{null=nullvalue, 3=3value, B=Bvalue, a=avalue, c=cvalue, d=dvalue}", original.toString());

        original.putValue(null, "otherNullvalue");
        assertEquals("{null=otherNullvalue, 3=3value, B=Bvalue, a=avalue, c=cvalue, d=dvalue}", original.toString());
        assertEquals(6, original.size());

        original.putValue(null, "nullvalue");
        assertEquals(6, original.size());
        assertEquals("{null=nullvalue, 3=3value, B=Bvalue, a=avalue, c=cvalue, d=dvalue}", original.toString());

        original.putValue(null, "abc");
        assertEquals(6, original.size());
        assertEquals("{null=abc, 3=3value, B=Bvalue, a=avalue, c=cvalue, d=dvalue}", original.toString());
    }

    @Test
    public void testNullKeysCopiedToAsMap() {
        final ArrayContextData original = new ArrayContextData();
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
        final ArrayContextData original = new ArrayContextData();
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
        final ArrayContextData original = new ArrayContextData();
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
        final ArrayContextData original = new ArrayContextData();
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
        final ArrayContextData original = new ArrayContextData();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");

        assertEquals("avalue", original.getValue("a"));
        assertEquals("avalue", original.getValueAt(2));

        assertEquals("Bvalue", original.getValue("B"));
        assertEquals("Bvalue", original.getValueAt(1));

        assertEquals("3value", original.getValue("3"));
        assertEquals("3value", original.getValueAt(0));

        original.putValue("0", "0value");
        assertEquals("0value", original.getValue("0"));
        assertEquals("0value", original.getValueAt(0));
        assertEquals("3value", original.getValue("3"));
        assertEquals("3value", original.getValueAt(1));
        assertEquals("Bvalue", original.getValue("B"));
        assertEquals("Bvalue", original.getValueAt(2));
        assertEquals("avalue", original.getValue("a"));
        assertEquals("avalue", original.getValueAt(3));
    }

    @Test
    public void testClear() throws Exception {
        final ArrayContextData original = new ArrayContextData();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");
        assertEquals(3, original.size());

        original.clear();
        assertEquals(0, original.size());

        // ensure slots in the values array are nulled out
        Field f = ArrayContextData.class.getDeclaredField("values");
        f.setAccessible(true);
        Object[] values = (Object[]) f.get(original);
        for (int i = 0; i < values.length; i++) {
            assertNull(values[i]);
        }
    }

    @Test
    public void testIndexOfKey() throws Exception {
        final ArrayContextData original = new ArrayContextData();
        original.putValue("a", "avalue");
        assertEquals(0, original.indexOfKey("a"));

        original.putValue("B", "Bvalue");
        assertEquals(1, original.indexOfKey("a"));
        assertEquals(0, original.indexOfKey("B"));

        original.putValue("3", "3value");
        assertEquals(2, original.indexOfKey("a"));
        assertEquals(1, original.indexOfKey("B"));
        assertEquals(0, original.indexOfKey("3"));

        original.putValue("A", "AAA");
        assertEquals(3, original.indexOfKey("a"));
        assertEquals(2, original.indexOfKey("B"));
        assertEquals(1, original.indexOfKey("A"));
        assertEquals(0, original.indexOfKey("3"));

        original.putValue("C", "CCC");
        assertEquals(4, original.indexOfKey("a"));
        assertEquals(3, original.indexOfKey("C"));
        assertEquals(2, original.indexOfKey("B"));
        assertEquals(1, original.indexOfKey("A"));
        assertEquals(0, original.indexOfKey("3"));

        original.putValue("2", "222");
        assertEquals(5, original.indexOfKey("a"));
        assertEquals(4, original.indexOfKey("C"));
        assertEquals(3, original.indexOfKey("B"));
        assertEquals(2, original.indexOfKey("A"));
        assertEquals(1, original.indexOfKey("3"));
        assertEquals(0, original.indexOfKey("2"));
    }

    @Test
    public void testContainsKey() throws Exception {
        final ArrayContextData original = new ArrayContextData();
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
    public void testGetValueAt() throws Exception {
        final ArrayContextData original = new ArrayContextData();
        original.putValue("a", "avalue");
        assertEquals("a", original.getKeyAt(0));
        assertEquals("avalue", original.getValueAt(0));

        original.putValue("B", "Bvalue");
        assertEquals("B", original.getKeyAt(0));
        assertEquals("Bvalue", original.getValueAt(0));
        assertEquals("a", original.getKeyAt(1));
        assertEquals("avalue", original.getValueAt(1));

        original.putValue("3", "3value");
        assertEquals("3", original.getKeyAt(0));
        assertEquals("3value", original.getValueAt(0));
        assertEquals("B", original.getKeyAt(1));
        assertEquals("Bvalue", original.getValueAt(1));
        assertEquals("a", original.getKeyAt(2));
        assertEquals("avalue", original.getValueAt(2));
    }

    @Test
    public void testSizeAndIsEmpty() throws Exception {
        final ArrayContextData original = new ArrayContextData();
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
        final ArrayContextData original = new ArrayContextData();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");

        original.forEach(new BiConsumer<String, String>() {
            int count = 0;
            @Override
            public void accept(final String key, final String value) {
                assertEquals("key", key, original.getKeyAt(count));
                assertEquals("val", value, original.getValueAt(count));
                count++;
                assertTrue("count should not exceed size but was " + count, count <= original.size());
            }
        });
    }

    static class State {
        ArrayContextData data;
        int count;
    }
    static TriConsumer<String, String, State> COUNTER = new TriConsumer<String, String, State>() {
        @Override
        public void accept(final String key, final String value, final State state) {
            assertEquals("key", key, state.data.getKeyAt(state.count));
            assertEquals("val", value, state.data.getValueAt(state.count));
            state.count++;
            assertTrue("count should not exceed size but was " + state.count,
                    state.count <= state.data.size());
        }
    };

    @Test
    public void testForEachTriConsumer() throws Exception {
        final ArrayContextData original = new ArrayContextData();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");

        final State state = new State();
        state.data = original;
        original.forEach(COUNTER, state);
        assertEquals(state.count, original.size());
    }
}