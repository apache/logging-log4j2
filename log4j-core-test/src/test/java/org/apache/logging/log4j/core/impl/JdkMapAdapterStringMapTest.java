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
package org.apache.logging.log4j.core.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.TriConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests the JdkMapAdapterStringMap class.
 */
class JdkMapAdapterStringMapTest {

    @Test
    void testConstructorDisallowsNull() {
        assertThrows(NullPointerException.class, () -> new JdkMapAdapterStringMap(null, false));
    }

    @Test
    void testToString() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "avalue");
        original.putValue("a2", "bvalue");
        original.putValue("B", "Bvalue");
        original.putValue("C", "Cvalue");
        original.putValue("3", "3value");
        assertEquals("{3=3value, B=Bvalue, C=Cvalue, a=avalue, a2=bvalue}", original.toString());
    }

    @Test
    void testSerialization() throws Exception {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");

        final byte[] binary = serialize(original);
        final JdkMapAdapterStringMap copy = deserialize(binary);
        assertEquals(original, copy);
    }

    private byte[] serialize(final JdkMapAdapterStringMap data) throws IOException {
        final ByteArrayOutputStream arr = new ByteArrayOutputStream();
        final ObjectOutputStream out = new ObjectOutputStream(arr);
        out.writeObject(data);
        return arr.toByteArray();
    }

    private JdkMapAdapterStringMap deserialize(final byte[] binary) throws IOException, ClassNotFoundException {
        final ByteArrayInputStream inArr = new ByteArrayInputStream(binary);
        final ObjectInputStream in = new ObjectInputStream(inArr);
        final JdkMapAdapterStringMap result = (JdkMapAdapterStringMap) in.readObject();
        return result;
    }

    @Test
    void testPutAll() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");

        final JdkMapAdapterStringMap other = new JdkMapAdapterStringMap();
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
    void testPutAll_overwritesSameKeys2() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "aORIG");
        original.putValue("b", "bORIG");
        original.putValue("c", "cORIG");
        original.putValue("d", "dORIG");
        original.putValue("e", "eORIG");

        final JdkMapAdapterStringMap other = new JdkMapAdapterStringMap();
        other.putValue("1", "11");
        other.putValue("2", "22");
        other.putValue("a", "aa");
        other.putValue("c", "cc");
        original.putAll(other);

        assertEquals(7, original.size(), "size after put other");
        assertEquals("aa", original.getValue("a"));
        assertEquals("bORIG", original.getValue("b"));
        assertEquals("cc", original.getValue("c"));
        assertEquals("dORIG", original.getValue("d"));
        assertEquals("eORIG", original.getValue("e"));
        assertEquals("11", original.getValue("1"));
        assertEquals("22", original.getValue("2"));
    }

    @Test
    void testPutAll_nullKeyInLargeOriginal() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue(null, "nullORIG");
        original.putValue("a", "aORIG");
        original.putValue("b", "bORIG");
        original.putValue("c", "cORIG");
        original.putValue("d", "dORIG");
        original.putValue("e", "eORIG");

        final JdkMapAdapterStringMap other = new JdkMapAdapterStringMap();
        other.putValue("1", "11");
        other.putValue("a", "aa");
        original.putAll(other);

        assertEquals(7, original.size(), "size after put other");
        assertEquals("aa", original.getValue("a"));
        assertEquals("bORIG", original.getValue("b"));
        assertEquals("cORIG", original.getValue("c"));
        assertEquals("dORIG", original.getValue("d"));
        assertEquals("eORIG", original.getValue("e"));
        assertEquals("11", original.getValue("1"));
        assertEquals("nullORIG", original.getValue(null));
    }

    @Test
    void testPutAll_nullKeyInSmallOriginal() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue(null, "nullORIG");
        original.putValue("a", "aORIG");
        original.putValue("b", "bORIG");

        final JdkMapAdapterStringMap other = new JdkMapAdapterStringMap();
        other.putValue("1", "11");
        other.putValue("2", "22");
        other.putValue("3", "33");
        other.putValue("a", "aa");
        original.putAll(other);

        assertEquals(6, original.size(), "size after put other");
        assertEquals("aa", original.getValue("a"));
        assertEquals("bORIG", original.getValue("b"));
        assertEquals("11", original.getValue("1"));
        assertEquals("22", original.getValue("2"));
        assertEquals("33", original.getValue("3"));
        assertEquals("nullORIG", original.getValue(null));
    }

    @Test
    void testPutAll_nullKeyInSmallAdditional() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "aORIG");
        original.putValue("b", "bORIG");
        original.putValue("c", "cORIG");
        original.putValue("d", "dORIG");
        original.putValue("e", "eORIG");

        final JdkMapAdapterStringMap other = new JdkMapAdapterStringMap();
        other.putValue(null, "nullNEW");
        other.putValue("1", "11");
        other.putValue("a", "aa");
        original.putAll(other);

        assertEquals(7, original.size(), "size after put other");
        assertEquals("aa", original.getValue("a"));
        assertEquals("bORIG", original.getValue("b"));
        assertEquals("cORIG", original.getValue("c"));
        assertEquals("dORIG", original.getValue("d"));
        assertEquals("eORIG", original.getValue("e"));
        assertEquals("11", original.getValue("1"));
        assertEquals("nullNEW", original.getValue(null));
    }

    @Test
    void testPutAll_nullKeyInLargeAdditional() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "aORIG");
        original.putValue("b", "bORIG");

        final JdkMapAdapterStringMap other = new JdkMapAdapterStringMap();
        other.putValue(null, "nullNEW");
        other.putValue("1", "11");
        other.putValue("2", "22");
        other.putValue("3", "33");
        other.putValue("a", "aa");
        original.putAll(other);

        assertEquals(6, original.size(), "size after put other");
        assertEquals("aa", original.getValue("a"));
        assertEquals("bORIG", original.getValue("b"));
        assertEquals("11", original.getValue("1"));
        assertEquals("22", original.getValue("2"));
        assertEquals("33", original.getValue("3"));
        assertEquals("nullNEW", original.getValue(null));
    }

    @Test
    void testPutAll_nullKeyInBoth_LargeOriginal() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue(null, "nullORIG");
        original.putValue("a", "aORIG");
        original.putValue("b", "bORIG");
        original.putValue("c", "cORIG");
        original.putValue("d", "dORIG");
        original.putValue("e", "eORIG");

        final JdkMapAdapterStringMap other = new JdkMapAdapterStringMap();
        other.putValue(null, "nullNEW");
        other.putValue("1", "11");
        other.putValue("a", "aa");
        original.putAll(other);

        assertEquals(7, original.size(), "size after put other");
        assertEquals("aa", original.getValue("a"));
        assertEquals("bORIG", original.getValue("b"));
        assertEquals("cORIG", original.getValue("c"));
        assertEquals("dORIG", original.getValue("d"));
        assertEquals("eORIG", original.getValue("e"));
        assertEquals("11", original.getValue("1"));
        assertEquals("nullNEW", original.getValue(null));
    }

    @Test
    void testPutAll_nullKeyInBoth_SmallOriginal() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue(null, "nullORIG");
        original.putValue("a", "aORIG");
        original.putValue("b", "bORIG");

        final JdkMapAdapterStringMap other = new JdkMapAdapterStringMap();
        other.putValue(null, "nullNEW");
        other.putValue("1", "11");
        other.putValue("2", "22");
        other.putValue("3", "33");
        other.putValue("a", "aa");
        original.putAll(other);

        assertEquals(6, original.size(), "size after put other");
        assertEquals("aa", original.getValue("a"));
        assertEquals("bORIG", original.getValue("b"));
        assertEquals("11", original.getValue("1"));
        assertEquals("22", original.getValue("2"));
        assertEquals("33", original.getValue("3"));
        assertEquals("nullNEW", original.getValue(null));
    }

    @Test
    void testPutAll_overwritesSameKeys1() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "aORIG");
        original.putValue("b", "bORIG");
        original.putValue("c", "cORIG");

        final JdkMapAdapterStringMap other = new JdkMapAdapterStringMap();
        other.putValue("1", "11");
        other.putValue("2", "22");
        other.putValue("a", "aa");
        other.putValue("c", "cc");
        original.putAll(other);

        assertEquals(5, original.size(), "size after put other");
        assertEquals("aa", original.getValue("a"));
        assertEquals("bORIG", original.getValue("b"));
        assertEquals("cc", original.getValue("c"));
        assertEquals("11", original.getValue("1"));
        assertEquals("22", original.getValue("2"));
    }

    @Test
    void testEquals() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");
        assertEquals(original, original); // equal to itself

        final JdkMapAdapterStringMap other = new JdkMapAdapterStringMap();
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
    void testToMap() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");

        final Map<String, Object> expected = new HashMap<>();
        expected.put("a", "avalue");
        expected.put("B", "Bvalue");
        expected.put("3", "3value");

        assertEquals(expected, original.toMap());

        try {
            original.toMap().put("abc", "xyz");
        } catch (final UnsupportedOperationException ex) {
            fail("Expected map to be mutable, but " + ex);
        }
    }

    @Test
    void testPutAll_KeepsExistingValues() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "aaa");
        original.putValue("b", "bbb");
        original.putValue("c", "ccc");
        assertEquals(3, original.size(), "size");

        // add empty context data
        original.putAll(new JdkMapAdapterStringMap());
        assertEquals(3, original.size(), "size after put empty");
        assertEquals("aaa", original.getValue("a"));
        assertEquals("bbb", original.getValue("b"));
        assertEquals("ccc", original.getValue("c"));

        final JdkMapAdapterStringMap other = new JdkMapAdapterStringMap();
        other.putValue("1", "111");
        other.putValue("2", "222");
        other.putValue("3", "333");
        original.putAll(other);

        assertEquals(6, original.size(), "size after put other");
        assertEquals("aaa", original.getValue("a"));
        assertEquals("bbb", original.getValue("b"));
        assertEquals("ccc", original.getValue("c"));
        assertEquals("111", original.getValue("1"));
        assertEquals("222", original.getValue("2"));
        assertEquals("333", original.getValue("3"));
    }

    @Test
    void testPutAll_sizePowerOfTwo() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "aaa");
        original.putValue("b", "bbb");
        original.putValue("c", "ccc");
        original.putValue("d", "ddd");
        assertEquals(4, original.size(), "size");

        // add empty context data
        original.putAll(new JdkMapAdapterStringMap());
        assertEquals(4, original.size(), "size after put empty");
        assertEquals("aaa", original.getValue("a"));
        assertEquals("bbb", original.getValue("b"));
        assertEquals("ccc", original.getValue("c"));
        assertEquals("ddd", original.getValue("d"));

        final JdkMapAdapterStringMap other = new JdkMapAdapterStringMap();
        other.putValue("1", "111");
        other.putValue("2", "222");
        other.putValue("3", "333");
        other.putValue("4", "444");
        original.putAll(other);

        assertEquals(8, original.size(), "size after put other");
        assertEquals("aaa", original.getValue("a"));
        assertEquals("bbb", original.getValue("b"));
        assertEquals("ccc", original.getValue("c"));
        assertEquals("ddd", original.getValue("d"));
        assertEquals("111", original.getValue("1"));
        assertEquals("222", original.getValue("2"));
        assertEquals("333", original.getValue("3"));
        assertEquals("444", original.getValue("4"));
    }

    @Test
    void testPutAll_largeAddition() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue(null, "nullVal");
        original.putValue("a", "aaa");
        original.putValue("b", "bbb");
        original.putValue("c", "ccc");
        original.putValue("d", "ddd");
        assertEquals(5, original.size(), "size");

        final JdkMapAdapterStringMap other = new JdkMapAdapterStringMap();
        for (int i = 0; i < 500; i++) {
            other.putValue(String.valueOf(i), String.valueOf(i));
        }
        other.putValue(null, "otherVal");
        original.putAll(other);

        assertEquals(505, original.size(), "size after put other");
        assertEquals("otherVal", original.getValue(null));
        assertEquals("aaa", original.getValue("a"));
        assertEquals("bbb", original.getValue("b"));
        assertEquals("ccc", original.getValue("c"));
        assertEquals("ddd", original.getValue("d"));
        for (int i = 0; i < 500; i++) {
            assertEquals(String.valueOf(i), original.getValue(String.valueOf(i)));
        }
    }

    @Test
    void testPutAllSelfDoesNotModify() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "aaa");
        original.putValue("b", "bbb");
        original.putValue("c", "ccc");
        assertEquals(3, original.size(), "size");

        // putAll with self
        original.putAll(original);
        assertEquals(3, original.size(), "size after put empty");
        assertEquals("aaa", original.getValue("a"));
        assertEquals("bbb", original.getValue("b"));
        assertEquals("ccc", original.getValue("c"));
    }

    @Test
    void testNoConcurrentModificationBiConsumerPut() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "aaa");
        original.putValue("b", "aaa");
        original.putValue("c", "aaa");
        original.putValue("d", "aaa");
        original.putValue("e", "aaa");
        original.forEach((s, o) -> original.putValue("c" + s, "other"));
    }

    @Test
    void testNoConcurrentModificationBiConsumerPutValue() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "aaa");
        original.putValue("b", "aaa");
        original.putValue("c", "aaa");
        original.putValue("d", "aaa");
        original.putValue("e", "aaa");
        original.forEach((s, o) -> original.putValue("c" + s, "other"));
    }

    @Test
    void testNoConcurrentModificationBiConsumerRemove() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "aaa");
        original.putValue("b", "aaa");
        original.putValue("c", "aaa");
        original.forEach((s, o) -> original.remove("a"));
    }

    @Test
    void testNoConcurrentModificationBiConsumerClear() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "aaa");
        original.putValue("b", "aaa");
        original.putValue("c", "aaa");
        original.putValue("d", "aaa");
        original.putValue("e", "aaa");
        original.forEach((s, o) -> original.clear());
    }

    @Test
    void testNoConcurrentModificationTriConsumerPut() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "aaa");
        original.putValue("b", "aaa");
        original.putValue("d", "aaa");
        original.putValue("e", "aaa");
        original.forEach((s, o, o2) -> original.putValue("c", "other"), null);
    }

    @Test
    void testNoConcurrentModificationTriConsumerPutValue() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "aaa");
        original.putValue("b", "aaa");
        original.putValue("c", "aaa");
        original.putValue("d", "aaa");
        original.putValue("e", "aaa");
        original.forEach((s, o, o2) -> original.putValue("c" + s, "other"), null);
    }

    @Test
    void testNoConcurrentModificationTriConsumerRemove() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "aaa");
        original.putValue("b", "aaa");
        original.putValue("c", "aaa");
        original.forEach((s, o, o2) -> original.remove("a"), null);
    }

    @Test
    void testNoConcurrentModificationTriConsumerClear() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "aaa");
        original.putValue("b", "aaa");
        original.putValue("c", "aaa");
        original.putValue("d", "aaa");
        original.forEach((s, o, o2) -> original.clear(), null);
    }

    @Test
    void testInitiallyNotFrozen() {
        assertFalse(new JdkMapAdapterStringMap().isFrozen());
    }

    @Test
    void testIsFrozenAfterCallingFreeze() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        assertFalse(original.isFrozen(), "before freeze");
        original.freeze();
        assertTrue(original.isFrozen(), "after freeze");
    }

    @Test
    void testFreezeProhibitsPutValue() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.freeze();
        assertThrows(UnsupportedOperationException.class, () -> original.putValue("a", "aaa"));
    }

    @Test
    void testFreezeProhibitsRemove() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("b", "bbb");
        original.freeze();
        assertThrows(
                UnsupportedOperationException.class,
                () -> original.remove("b")); // existing key: modifies the collection
    }

    @Test
    void testFreezeAllowsRemoveOfNonExistingKey() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("b", "bbb");
        original.freeze();
        original.remove("a"); // no actual modification
    }

    @Test
    void testFreezeAllowsRemoveIfEmpty() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.freeze();
        original.remove("a"); // no exception
    }

    @Test
    void testFreezeProhibitsClear() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "aaa");
        original.freeze();
        assertThrows(UnsupportedOperationException.class, original::clear);
    }

    @Test
    void testFreezeAllowsClearIfEmpty() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.freeze();
        original.clear();
    }

    @Test
    void testNullKeysAllowed() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");
        original.putValue("c", "cvalue");
        original.putValue("d", "dvalue");
        assertEquals(5, original.size());

        original.putValue(null, "nullvalue");
        assertEquals(6, original.size());
        assertEquals("nullvalue", original.getValue(null));

        original.putValue(null, "otherNullvalue");
        assertEquals("otherNullvalue", original.getValue(null));
        assertEquals(6, original.size());

        original.putValue(null, "nullvalue");
        assertEquals(6, original.size());
        assertEquals("nullvalue", original.getValue(null));

        original.putValue(null, "abc");
        assertEquals(6, original.size());
        assertEquals("abc", original.getValue(null));
    }

    @Test
    void testNullKeysCopiedToAsMap() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");
        original.putValue("c", "cvalue");
        original.putValue("d", "dvalue");
        assertEquals(5, original.size());

        final HashMap<String, String> expected = new HashMap<>();
        expected.put("a", "avalue");
        expected.put("B", "Bvalue");
        expected.put("3", "3value");
        expected.put("c", "cvalue");
        expected.put("d", "dvalue");
        assertEquals(expected, original.toMap(), "initial");

        original.putValue(null, "nullvalue");
        expected.put(null, "nullvalue");
        assertEquals(6, original.size());
        assertEquals(expected, original.toMap(), "with null key");

        original.putValue(null, "otherNullvalue");
        expected.put(null, "otherNullvalue");
        assertEquals(6, original.size());
        assertEquals(expected, original.toMap(), "with null key value2");

        original.putValue(null, "nullvalue");
        expected.put(null, "nullvalue");
        assertEquals(6, original.size());
        assertEquals(expected, original.toMap(), "with null key value1 again");

        original.putValue(null, "abc");
        expected.put(null, "abc");
        assertEquals(6, original.size());
        assertEquals(expected, original.toMap(), "with null key value3");
    }

    @Test
    void testRemove() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "avalue");
        assertEquals(1, original.size());
        assertEquals("avalue", original.getValue("a"));

        original.remove("a");
        assertEquals(0, original.size());
        assertNull(original.getValue("a"), "no a val");

        original.remove("B");
        assertEquals(0, original.size());
        assertNull(original.getValue("B"), "no B val");
    }

    @Test
    void testRemoveWhenFull() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "avalue");
        original.putValue("b", "bvalue");
        original.putValue("c", "cvalue");
        original.putValue("d", "dvalue"); // default capacity = 4
        original.remove("d");
    }

    @Test
    void testNullValuesArePreserved() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "avalue");
        assertEquals(1, original.size());
        assertEquals("avalue", original.getValue("a"));

        original.putValue("a", null);
        assertEquals(1, original.size());
        assertNull(original.getValue("a"), "no a val");

        original.putValue("B", null);
        assertEquals(2, original.size());
        assertNull(original.getValue("B"), "no B val");
    }

    @Test
    void testGet() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
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
    void testClear() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");
        assertEquals(3, original.size());

        original.clear();
        assertEquals(0, original.size());
    }

    @Test
    void testContainsKey() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        assertFalse(original.containsKey("a"), "a");
        assertFalse(original.containsKey("B"), "B");
        assertFalse(original.containsKey("3"), "3");
        assertFalse(original.containsKey("A"), "A");

        original.putValue("a", "avalue");
        assertTrue(original.containsKey("a"), "a");
        assertFalse(original.containsKey("B"), "B");
        assertFalse(original.containsKey("3"), "3");
        assertFalse(original.containsKey("A"), "A");

        original.putValue("B", "Bvalue");
        assertTrue(original.containsKey("a"), "a");
        assertTrue(original.containsKey("B"), "B");
        assertFalse(original.containsKey("3"), "3");
        assertFalse(original.containsKey("A"), "A");

        original.putValue("3", "3value");
        assertTrue(original.containsKey("a"), "a");
        assertTrue(original.containsKey("B"), "B");
        assertTrue(original.containsKey("3"), "3");
        assertFalse(original.containsKey("A"), "A");

        original.putValue("A", "AAA");
        assertTrue(original.containsKey("a"), "a");
        assertTrue(original.containsKey("B"), "B");
        assertTrue(original.containsKey("3"), "3");
        assertTrue(original.containsKey("A"), "A");
    }

    @Test
    void testSizeAndIsEmpty() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        assertEquals(0, original.size());
        assertTrue(original.isEmpty(), "initial");

        original.putValue("a", "avalue");
        assertEquals(1, original.size());
        assertFalse(original.isEmpty(), "size=" + original.size());

        original.putValue("B", "Bvalue");
        assertEquals(2, original.size());
        assertFalse(original.isEmpty(), "size=" + original.size());

        original.putValue("3", "3value");
        assertEquals(3, original.size());
        assertFalse(original.isEmpty(), "size=" + original.size());

        original.remove("B");
        assertEquals(2, original.size());
        assertFalse(original.isEmpty(), "size=" + original.size());

        original.remove("3");
        assertEquals(1, original.size());
        assertFalse(original.isEmpty(), "size=" + original.size());

        original.remove("a");
        assertEquals(0, original.size());
        assertTrue(original.isEmpty(), "size=" + original.size());
    }

    @Test
    void testForEachBiConsumer() throws Exception {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");

        original.forEach(new BiConsumer<String, String>() {
            int count = 0;

            @Override
            public void accept(final String key, final String value) {
                //                assertEquals("key", key, original.getKeyAt(count));
                //                assertEquals("val", value, original.getValueAt(count));
                count++;
                assertTrue(count <= original.size(), "count should not exceed size but was " + count);
            }
        });
    }

    static class State {
        JdkMapAdapterStringMap data;
        int count;
    }

    static TriConsumer<String, String, JdkMapAdapterStringMapTest.State> COUNTER = (key, value, state) -> {
        //            assertEquals("key", key, state.data.getKeyAt(state.count));
        //            assertEquals("val", value, state.data.getValueAt(state.count));
        state.count++;
        assertTrue(state.count <= state.data.size(), "count should not exceed size but was " + state.count);
    };

    @Test
    void testForEachTriConsumer() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");

        final JdkMapAdapterStringMapTest.State state = new JdkMapAdapterStringMapTest.State();
        state.data = original;
        original.forEach(COUNTER, state);
        assertEquals(state.count, original.size());
    }

    static Stream<Arguments> testImmutability() {
        return Stream.of(
                Arguments.of(new HashMap<>(), false),
                Arguments.of(Collections.emptyMap(), true),
                Arguments.of(Collections.unmodifiableMap(new HashMap<>()), true));
    }

    @ParameterizedTest
    @MethodSource
    void testImmutability(final Map<String, String> map, final boolean frozen) {
        assertThat(new JdkMapAdapterStringMap(map).isFrozen()).as("Frozen").isEqualTo(frozen);
    }
}
