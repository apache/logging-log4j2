package org.apache.logging.log4j.util;/*
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
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;

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

        try {
            original.asMap().put("abc", "xyz");
            fail("Expected map to be immutable");
        } catch (final UnsupportedOperationException ok) {
            //ok
        }
    }

    @Test
    public void testPutAll_KeepsExistingValues() {
        final ArrayContextData original = new ArrayContextData();
        original.putValue("a", "aaa");
        original.putValue("b", "bbb");
        original.putValue("c", "ccc");
        assertEquals("size", 3, original.size());

        // add empty context data
        original.putAll(new ArrayContextData());
        assertEquals("size after put empty", 3, original.size());
        assertEquals("aaa", original.getValue("a"));
        assertEquals("bbb", original.getValue("b"));
        assertEquals("ccc", original.getValue("c"));

        final ArrayContextData other = new ArrayContextData();
        other.putValue("1", "111");
        other.putValue("2", "222");
        other.putValue("3", "333");
        original.putAll(other);

        assertEquals("size after put other", 6, original.size());
        assertEquals("aaa", original.getValue("a"));
        assertEquals("bbb", original.getValue("b"));
        assertEquals("ccc", original.getValue("c"));
        assertEquals("111", original.getValue("1"));
        assertEquals("222", original.getValue("2"));
        assertEquals("333", original.getValue("3"));
    }

    @Test
    public void testPutAllSelfDoesNotModify() {
        final ArrayContextData original = new ArrayContextData();
        original.putValue("a", "aaa");
        original.putValue("b", "bbb");
        original.putValue("c", "ccc");
        assertEquals("size", 3, original.size());

        // putAll with self
        original.putAll(original);
        assertEquals("size after put empty", 3, original.size());
        assertEquals("aaa", original.getValue("a"));
        assertEquals("bbb", original.getValue("b"));
        assertEquals("ccc", original.getValue("c"));
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testConcurrentModificationBiConsumerPut() {
        final ArrayContextData original = new ArrayContextData();
        original.putValue("a", "aaa");
        original.forEach(new BiConsumer<String, Object>() {
            @Override
            public void accept(final String s, final Object o) {
                original.putValue("c", "other");
            }
        });
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testConcurrentModificationBiConsumerPutValue() {
        final ArrayContextData original = new ArrayContextData();
        original.putValue("a", "aaa");
        original.forEach(new BiConsumer<String, Object>() {
            @Override
            public void accept(final String s, final Object o) {
                original.putValue("c", "other");
            }
        });
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testConcurrentModificationBiConsumerRemove() {
        final ArrayContextData original = new ArrayContextData();
        original.putValue("a", "aaa");
        original.forEach(new BiConsumer<String, Object>() {
            @Override
            public void accept(final String s, final Object o) {
                original.remove("a");
            }
        });
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testConcurrentModificationBiConsumerClear() {
        final ArrayContextData original = new ArrayContextData();
        original.putValue("a", "aaa");
        original.forEach(new BiConsumer<String, Object>() {
            @Override
            public void accept(final String s, final Object o) {
                original.clear();
            }
        });
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testConcurrentModificationTriConsumerPut() {
        final ArrayContextData original = new ArrayContextData();
        original.putValue("a", "aaa");
        original.forEach(new TriConsumer<String, Object, Object>() {
            @Override
            public void accept(final String s, final Object o, final Object o2) {
                original.putValue("c", "other");
            }
        }, null);
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testConcurrentModificationTriConsumerPutValue() {
        final ArrayContextData original = new ArrayContextData();
        original.putValue("a", "aaa");
        original.forEach(new TriConsumer<String, Object, Object>() {
            @Override
            public void accept(final String s, final Object o, final Object o2) {
                original.putValue("c", "other");
            }
        }, null);
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testConcurrentModificationTriConsumerRemove() {
        final ArrayContextData original = new ArrayContextData();
        original.putValue("a", "aaa");
        original.forEach(new TriConsumer<String, Object, Object>() {
            @Override
            public void accept(final String s, final Object o, final Object o2) {
                original.remove("a");
            }
        }, null);
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testConcurrentModificationTriConsumerClear() {
        final ArrayContextData original = new ArrayContextData();
        original.putValue("a", "aaa");
        original.forEach(new TriConsumer<String, Object, Object>() {
            @Override
            public void accept(final String s, final Object o, final Object o2) {
                original.clear();
            }
        }, null);
    }

    @Test
    public void testInitiallyNotFrozen() {
        assertFalse(new ArrayContextData().isFrozen());
    }

    @Test
    public void testIsFrozenAfterCallingFreeze() {
        final ArrayContextData original = new ArrayContextData();
        assertFalse("before freeze", original.isFrozen());
        original.freeze();
        assertTrue("after freeze", original.isFrozen());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testFreezeProhibitsPutValue() {
        final ArrayContextData original = new ArrayContextData();
        original.freeze();
        original.putValue("a", "aaa");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testFreezeProhibitsRemove() {
        final ArrayContextData original = new ArrayContextData();
        original.putValue("b", "bbb");
        original.freeze();
        original.remove("b"); // existing key: modifies the collection
    }

    @Test
    public void testFreezeAllowsRemoveOfNonExistingKey() {
        final ArrayContextData original = new ArrayContextData();
        original.putValue("b", "bbb");
        original.freeze();
        original.remove("a"); // no actual modification
    }

    @Test
    public void testFreezeAllowsRemoveIfEmpty() {
        final ArrayContextData original = new ArrayContextData();
        original.freeze();
        original.remove("a"); // no exception
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testFreezeProhibitsClear() {
        final ArrayContextData original = new ArrayContextData();
        original.putValue("a", "aaa");
        original.freeze();
        original.clear();
    }

    @Test
    public void testFreezeAllowsClearIfEmpty() {
        final ArrayContextData original = new ArrayContextData();
        original.freeze();
        original.clear();
    }

    @Test
    public void testPutInsertsInAlphabeticOrder() throws Exception {
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