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
package org.apache.logging.log4j.core.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.TriConsumer;
import org.junit.jupiter.api.Test;

/**
 * Tests the JdkMapAdapterStringMap class.
 */
public class JdkMapAdapterStringMapTest {

    @Test
    public void testConstructorDisallowsNull() {
        assertThatThrownBy(() -> new JdkMapAdapterStringMap(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testToString() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "avalue");
        original.putValue("a2", "bvalue");
        original.putValue("B", "Bvalue");
        original.putValue("C", "Cvalue");
        original.putValue("3", "3value");
        assertThat(original.toString()).isEqualTo("{3=3value, B=Bvalue, C=Cvalue, a=avalue, a2=bvalue}");
    }

    @Test
    public void testSerialization() throws Exception {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");

        final byte[] binary = serialize(original);
        final JdkMapAdapterStringMap copy = deserialize(binary);
        assertThat(copy).isEqualTo(original);
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
    public void testPutAll() throws Exception {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");

        final JdkMapAdapterStringMap other = new JdkMapAdapterStringMap();
        other.putAll(original);
        assertThat(other).isEqualTo(original);

        other.putValue("3", "otherValue");
        assertThat(other).isNotEqualTo(original);

        other.putValue("3", null);
        assertThat(other).isNotEqualTo(original);

        other.putValue("3", "3value");
        assertThat(other).isEqualTo(original);
    }

    @Test
    public void testPutAll_overwritesSameKeys2() throws Exception {
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

        assertThat(original.size()).describedAs("size after put other").isEqualTo(7);
        assertThat(original.<String>getValue("a")).isEqualTo("aa");
        assertThat(original.<String>getValue("b")).isEqualTo("bORIG");
        assertThat(original.<String>getValue("c")).isEqualTo("cc");
        assertThat(original.<String>getValue("d")).isEqualTo("dORIG");
        assertThat(original.<String>getValue("e")).isEqualTo("eORIG");
        assertThat(original.<String>getValue("1")).isEqualTo("11");
        assertThat(original.<String>getValue("2")).isEqualTo("22");
    }

    @Test
    public void testPutAll_nullKeyInLargeOriginal() throws Exception {
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

        assertThat(original.size()).describedAs("size after put other").isEqualTo(7);
        assertThat(original.<String>getValue("a")).isEqualTo("aa");
        assertThat(original.<String>getValue("b")).isEqualTo("bORIG");
        assertThat(original.<String>getValue("c")).isEqualTo("cORIG");
        assertThat(original.<String>getValue("d")).isEqualTo("dORIG");
        assertThat(original.<String>getValue("e")).isEqualTo("eORIG");
        assertThat(original.<String>getValue("1")).isEqualTo("11");
        assertThat(original.<String>getValue(null)).isEqualTo("nullORIG");
    }

    @Test
    public void testPutAll_nullKeyInSmallOriginal() throws Exception {
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

        assertThat(original.size()).describedAs("size after put other").isEqualTo(6);
        assertThat(original.<String>getValue("a")).isEqualTo("aa");
        assertThat(original.<String>getValue("b")).isEqualTo("bORIG");
        assertThat(original.<String>getValue("1")).isEqualTo("11");
        assertThat(original.<String>getValue("2")).isEqualTo("22");
        assertThat(original.<String>getValue("3")).isEqualTo("33");
        assertThat(original.<String>getValue(null)).isEqualTo("nullORIG");
    }

    @Test
    public void testPutAll_nullKeyInSmallAdditional() throws Exception {
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

        assertThat(original.size()).describedAs("size after put other").isEqualTo(7);
        assertThat(original.<String>getValue("a")).isEqualTo("aa");
        assertThat(original.<String>getValue("b")).isEqualTo("bORIG");
        assertThat(original.<String>getValue("c")).isEqualTo("cORIG");
        assertThat(original.<String>getValue("d")).isEqualTo("dORIG");
        assertThat(original.<String>getValue("e")).isEqualTo("eORIG");
        assertThat(original.<String>getValue("1")).isEqualTo("11");
        assertThat(original.<String>getValue(null)).isEqualTo("nullNEW");
    }

    @Test
    public void testPutAll_nullKeyInLargeAdditional() throws Exception {
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

        assertThat(original.size()).describedAs("size after put other").isEqualTo(6);
        assertThat(original.<String>getValue("a")).isEqualTo("aa");
        assertThat(original.<String>getValue("b")).isEqualTo("bORIG");
        assertThat(original.<String>getValue("1")).isEqualTo("11");
        assertThat(original.<String>getValue("2")).isEqualTo("22");
        assertThat(original.<String>getValue("3")).isEqualTo("33");
        assertThat(original.<String>getValue(null)).isEqualTo("nullNEW");
    }

    @Test
    public void testPutAll_nullKeyInBoth_LargeOriginal() throws Exception {
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

        assertThat(original.size()).describedAs("size after put other").isEqualTo(7);
        assertThat(original.<String>getValue("a")).isEqualTo("aa");
        assertThat(original.<String>getValue("b")).isEqualTo("bORIG");
        assertThat(original.<String>getValue("c")).isEqualTo("cORIG");
        assertThat(original.<String>getValue("d")).isEqualTo("dORIG");
        assertThat(original.<String>getValue("e")).isEqualTo("eORIG");
        assertThat(original.<String>getValue("1")).isEqualTo("11");
        assertThat(original.<String>getValue(null)).isEqualTo("nullNEW");
    }

    @Test
    public void testPutAll_nullKeyInBoth_SmallOriginal() throws Exception {
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

        assertThat(original.size()).describedAs("size after put other").isEqualTo(6);
        assertThat(original.<String>getValue("a")).isEqualTo("aa");
        assertThat(original.<String>getValue("b")).isEqualTo("bORIG");
        assertThat(original.<String>getValue("1")).isEqualTo("11");
        assertThat(original.<String>getValue("2")).isEqualTo("22");
        assertThat(original.<String>getValue("3")).isEqualTo("33");
        assertThat(original.<String>getValue(null)).isEqualTo("nullNEW");
    }

    @Test
    public void testPutAll_overwritesSameKeys1() throws Exception {
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

        assertThat(original.size()).describedAs("size after put other").isEqualTo(5);
        assertThat(original.<String>getValue("a")).isEqualTo("aa");
        assertThat(original.<String>getValue("b")).isEqualTo("bORIG");
        assertThat(original.<String>getValue("c")).isEqualTo("cc");
        assertThat(original.<String>getValue("1")).isEqualTo("11");
        assertThat(original.<String>getValue("2")).isEqualTo("22");
    }

    @Test
    public void testEquals() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");
        assertThat(original).isEqualTo(original); // equal to itself

        final JdkMapAdapterStringMap other = new JdkMapAdapterStringMap();
        other.putValue("a", "avalue");
        assertThat(other).isNotEqualTo(original);

        other.putValue("B", "Bvalue");
        assertThat(other).isNotEqualTo(original);

        other.putValue("3", "3value");
        assertThat(other).isEqualTo(original);

        other.putValue("3", "otherValue");
        assertThat(other).isNotEqualTo(original);

        other.putValue("3", null);
        assertThat(other).isNotEqualTo(original);

        other.putValue("3", "3value");
        assertThat(other).isEqualTo(original);
    }

    @Test
    public void testToMap() throws Exception {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");

        final Map<String, Object> expected = new HashMap<>();
        expected.put("a", "avalue");
        expected.put("B", "Bvalue");
        expected.put("3", "3value");

        assertThat(original.toMap()).isEqualTo(expected);

        try {
            original.toMap().put("abc", "xyz");
        } catch (final UnsupportedOperationException ex) {
            fail("Expected map to be mutable, but " + ex);
        }
    }

    @Test
    public void testPutAll_KeepsExistingValues() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "aaa");
        original.putValue("b", "bbb");
        original.putValue("c", "ccc");
        assertThat(original.size()).describedAs("size").isEqualTo(3);

        // add empty context data
        original.putAll(new JdkMapAdapterStringMap());
        assertThat(original.size()).describedAs("size after put empty").isEqualTo(3);
        assertThat(original.<String>getValue("a")).isEqualTo("aaa");
        assertThat(original.<String>getValue("b")).isEqualTo("bbb");
        assertThat(original.<String>getValue("c")).isEqualTo("ccc");

        final JdkMapAdapterStringMap other = new JdkMapAdapterStringMap();
        other.putValue("1", "111");
        other.putValue("2", "222");
        other.putValue("3", "333");
        original.putAll(other);

        assertThat(original.size()).describedAs("size after put other").isEqualTo(6);
        assertThat(original.<String>getValue("a")).isEqualTo("aaa");
        assertThat(original.<String>getValue("b")).isEqualTo("bbb");
        assertThat(original.<String>getValue("c")).isEqualTo("ccc");
        assertThat(original.<String>getValue("1")).isEqualTo("111");
        assertThat(original.<String>getValue("2")).isEqualTo("222");
        assertThat(original.<String>getValue("3")).isEqualTo("333");
    }

    @Test
    public void testPutAll_sizePowerOfTwo() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "aaa");
        original.putValue("b", "bbb");
        original.putValue("c", "ccc");
        original.putValue("d", "ddd");
        assertThat(original.size()).describedAs("size").isEqualTo(4);

        // add empty context data
        original.putAll(new JdkMapAdapterStringMap());
        assertThat(original.size()).describedAs("size after put empty").isEqualTo(4);
        assertThat(original.<String>getValue("a")).isEqualTo("aaa");
        assertThat(original.<String>getValue("b")).isEqualTo("bbb");
        assertThat(original.<String>getValue("c")).isEqualTo("ccc");
        assertThat(original.<String>getValue("d")).isEqualTo("ddd");

        final JdkMapAdapterStringMap other = new JdkMapAdapterStringMap();
        other.putValue("1", "111");
        other.putValue("2", "222");
        other.putValue("3", "333");
        other.putValue("4", "444");
        original.putAll(other);

        assertThat(original.size()).describedAs("size after put other").isEqualTo(8);
        assertThat(original.<String>getValue("a")).isEqualTo("aaa");
        assertThat(original.<String>getValue("b")).isEqualTo("bbb");
        assertThat(original.<String>getValue("c")).isEqualTo("ccc");
        assertThat(original.<String>getValue("d")).isEqualTo("ddd");
        assertThat(original.<String>getValue("1")).isEqualTo("111");
        assertThat(original.<String>getValue("2")).isEqualTo("222");
        assertThat(original.<String>getValue("3")).isEqualTo("333");
        assertThat(original.<String>getValue("4")).isEqualTo("444");
    }

    @Test
    public void testPutAll_largeAddition() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue(null, "nullVal");
        original.putValue("a", "aaa");
        original.putValue("b", "bbb");
        original.putValue("c", "ccc");
        original.putValue("d", "ddd");
        assertThat(original.size()).describedAs("size").isEqualTo(5);

        final JdkMapAdapterStringMap other = new JdkMapAdapterStringMap();
        for (int i = 0 ; i < 500; i++) {
            other.putValue(String.valueOf(i), String.valueOf(i));
        }
        other.putValue(null, "otherVal");
        original.putAll(other);

        assertThat(original.size()).describedAs("size after put other").isEqualTo(505);
        assertThat(original.<String>getValue(null)).isEqualTo("otherVal");
        assertThat(original.<String>getValue("a")).isEqualTo("aaa");
        assertThat(original.<String>getValue("b")).isEqualTo("bbb");
        assertThat(original.<String>getValue("c")).isEqualTo("ccc");
        assertThat(original.<String>getValue("d")).isEqualTo("ddd");
        for (int i = 0 ; i < 500; i++) {
            assertThat(original.<String>getValue(String.valueOf(i))).isEqualTo(String.valueOf(i));
        }
    }

    @Test
    public void testPutAllSelfDoesNotModify() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "aaa");
        original.putValue("b", "bbb");
        original.putValue("c", "ccc");
        assertThat(original.size()).describedAs("size").isEqualTo(3);

        // putAll with self
        original.putAll(original);
        assertThat(original.size()).describedAs("size after put empty").isEqualTo(3);
        assertThat(original.<String>getValue("a")).isEqualTo("aaa");
        assertThat(original.<String>getValue("b")).isEqualTo("bbb");
        assertThat(original.<String>getValue("c")).isEqualTo("ccc");
    }

    @Test
    public void testNoConcurrentModificationBiConsumerPut() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "aaa");
        original.putValue("b", "aaa");
        original.putValue("c", "aaa");
        original.putValue("d", "aaa");
        original.putValue("e", "aaa");
        original.forEach((s, o) -> original.putValue("c" + s, "other"));
    }

    @Test
    public void testNoConcurrentModificationBiConsumerPutValue() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "aaa");
        original.putValue("b", "aaa");
        original.putValue("c", "aaa");
        original.putValue("d", "aaa");
        original.putValue("e", "aaa");
        original.forEach((s, o) -> original.putValue("c" + s, "other"));
    }

    @Test
    public void testNoConcurrentModificationBiConsumerRemove() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "aaa");
        original.putValue("b", "aaa");
        original.putValue("c", "aaa");
        original.forEach((s, o) -> original.remove("a"));
    }

    @Test
    public void testNoConcurrentModificationBiConsumerClear() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "aaa");
        original.putValue("b", "aaa");
        original.putValue("c", "aaa");
        original.putValue("d", "aaa");
        original.putValue("e", "aaa");
        original.forEach((s, o) -> original.clear());
    }

    @Test
    public void testNoConcurrentModificationTriConsumerPut() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "aaa");
        original.putValue("b", "aaa");
        original.putValue("d", "aaa");
        original.putValue("e", "aaa");
        original.forEach((s, o, o2) -> original.putValue("c", "other"), null);
    }

    @Test
    public void testNoConcurrentModificationTriConsumerPutValue() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "aaa");
        original.putValue("b", "aaa");
        original.putValue("c", "aaa");
        original.putValue("d", "aaa");
        original.putValue("e", "aaa");
        original.forEach((s, o, o2) -> original.putValue("c" + s, "other"), null);
    }

    @Test
    public void testNoConcurrentModificationTriConsumerRemove() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "aaa");
        original.putValue("b", "aaa");
        original.putValue("c", "aaa");
        original.forEach((s, o, o2) -> original.remove("a"), null);
    }

    @Test
    public void testNoConcurrentModificationTriConsumerClear() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "aaa");
        original.putValue("b", "aaa");
        original.putValue("c", "aaa");
        original.putValue("d", "aaa");
        original.forEach((s, o, o2) -> original.clear(), null);
    }

    @Test
    public void testInitiallyNotFrozen() {
        assertThat(new JdkMapAdapterStringMap().isFrozen()).isFalse();
    }

    @Test
    public void testIsFrozenAfterCallingFreeze() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        assertFalse(original.isFrozen(), "before freeze");
        original.freeze();
        assertTrue(original.isFrozen(), "after freeze");
    }

    @Test
    public void testFreezeProhibitsPutValue() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.freeze();
        assertThatThrownBy(() -> original.putValue("a", "aaa")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testFreezeProhibitsRemove() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("b", "bbb");
        original.freeze();
        assertThatThrownBy(() -> original.remove("b")).isInstanceOf(UnsupportedOperationException.class); // existing key: modifies the collection
    }

    @Test
    public void testFreezeAllowsRemoveOfNonExistingKey() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("b", "bbb");
        original.freeze();
        original.remove("a"); // no actual modification
    }

    @Test
    public void testFreezeAllowsRemoveIfEmpty() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.freeze();
        original.remove("a"); // no exception
    }

    @Test
    public void testFreezeProhibitsClear() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "aaa");
        original.freeze();
        assertThatThrownBy(original::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testFreezeAllowsClearIfEmpty() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.freeze();
        original.clear();
    }

    @Test
    public void testNullKeysAllowed() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");
        original.putValue("c", "cvalue");
        original.putValue("d", "dvalue");
        assertThat(original.size()).isEqualTo(5);

        original.putValue(null, "nullvalue");
        assertThat(original.size()).isEqualTo(6);
        assertThat(original.<String>getValue(null)).isEqualTo("nullvalue");

        original.putValue(null, "otherNullvalue");
        assertThat(original.<String>getValue(null)).isEqualTo("otherNullvalue");
        assertThat(original.size()).isEqualTo(6);

        original.putValue(null, "nullvalue");
        assertThat(original.size()).isEqualTo(6);
        assertThat(original.<String>getValue(null)).isEqualTo("nullvalue");

        original.putValue(null, "abc");
        assertThat(original.size()).isEqualTo(6);
        assertThat(original.<String>getValue(null)).isEqualTo("abc");

    }

    @Test
    public void testNullKeysCopiedToAsMap() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");
        original.putValue("c", "cvalue");
        original.putValue("d", "dvalue");
        assertThat(original.size()).isEqualTo(5);

        final HashMap<String, String> expected = new HashMap<>();
        expected.put("a", "avalue");
        expected.put("B", "Bvalue");
        expected.put("3", "3value");
        expected.put("c", "cvalue");
        expected.put("d", "dvalue");
        assertThat(original.toMap()).describedAs("initial").isEqualTo(expected);

        original.putValue(null, "nullvalue");
        expected.put(null, "nullvalue");
        assertThat(original.size()).isEqualTo(6);
        assertThat(original.toMap()).describedAs("with null key").isEqualTo(expected);

        original.putValue(null, "otherNullvalue");
        expected.put(null, "otherNullvalue");
        assertThat(original.size()).isEqualTo(6);
        assertThat(original.toMap()).describedAs("with null key value2").isEqualTo(expected);

        original.putValue(null, "nullvalue");
        expected.put(null, "nullvalue");
        assertThat(original.size()).isEqualTo(6);
        assertThat(original.toMap()).describedAs("with null key value1 again").isEqualTo(expected);

        original.putValue(null, "abc");
        expected.put(null, "abc");
        assertThat(original.size()).isEqualTo(6);
        assertThat(original.toMap()).describedAs("with null key value3").isEqualTo(expected);
    }

    @Test
    public void testRemove() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "avalue");
        assertThat(original.size()).isEqualTo(1);
        assertThat(original.<String>getValue("a")).isEqualTo("avalue");

        original.remove("a");
        assertThat(original.size()).isEqualTo(0);
        assertThat(original.<String>getValue("a")).describedAs("no a val").isNull();

        original.remove("B");
        assertThat(original.size()).isEqualTo(0);
        assertThat(original.<String>getValue("B")).describedAs("no B val").isNull();
    }

    @Test
    public void testRemoveWhenFull() throws Exception {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "avalue");
        original.putValue("b", "bvalue");
        original.putValue("c", "cvalue");
        original.putValue("d", "dvalue"); // default capacity = 4
        original.remove("d");
    }

    @Test
    public void testNullValuesArePreserved() {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "avalue");
        assertThat(original.size()).isEqualTo(1);
        assertThat(original.<String>getValue("a")).isEqualTo("avalue");

        original.putValue("a", null);
        assertThat(original.size()).isEqualTo(1);
        assertThat(original.<String>getValue("a")).describedAs("no a val").isNull();

        original.putValue("B", null);
        assertThat(original.size()).isEqualTo(2);
        assertThat(original.<String>getValue("B")).describedAs("no B val").isNull();
    }

    @Test
    public void testGet() throws Exception {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");

        assertThat(original.<String>getValue("a")).isEqualTo("avalue");
        assertThat(original.<String>getValue("B")).isEqualTo("Bvalue");
        assertThat(original.<String>getValue("3")).isEqualTo("3value");

        original.putValue("0", "0value");
        assertThat(original.<String>getValue("0")).isEqualTo("0value");
        assertThat(original.<String>getValue("3")).isEqualTo("3value");
        assertThat(original.<String>getValue("B")).isEqualTo("Bvalue");
        assertThat(original.<String>getValue("a")).isEqualTo("avalue");
    }

    @Test
    public void testClear() throws Exception {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");
        assertThat(original.size()).isEqualTo(3);

        original.clear();
        assertThat(original.size()).isEqualTo(0);
    }

    @Test
    public void testContainsKey() throws Exception {
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
    public void testSizeAndIsEmpty() throws Exception {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        assertThat(original.size()).isEqualTo(0);
        assertTrue(original.isEmpty(), "initial");

        original.putValue("a", "avalue");
        assertThat(original.size()).isEqualTo(1);
        assertFalse(original.isEmpty(), "size=" + original.size());

        original.putValue("B", "Bvalue");
        assertThat(original.size()).isEqualTo(2);
        assertFalse(original.isEmpty(), "size=" + original.size());

        original.putValue("3", "3value");
        assertThat(original.size()).isEqualTo(3);
        assertFalse(original.isEmpty(), "size=" + original.size());

        original.remove("B");
        assertThat(original.size()).isEqualTo(2);
        assertFalse(original.isEmpty(), "size=" + original.size());

        original.remove("3");
        assertThat(original.size()).isEqualTo(1);
        assertFalse(original.isEmpty(), "size=" + original.size());

        original.remove("a");
        assertThat(original.size()).isEqualTo(0);
        assertTrue(original.isEmpty(), "size=" + original.size());
    }

    @Test
    public void testForEachBiConsumer() throws Exception {
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
        assertTrue(
                state.count <= state.data.size(), "count should not exceed size but was " + state.count);
    };

    @Test
    public void testForEachTriConsumer() throws Exception {
        final JdkMapAdapterStringMap original = new JdkMapAdapterStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");

        final JdkMapAdapterStringMapTest.State state = new JdkMapAdapterStringMapTest.State();
        state.data = original;
        original.forEach(COUNTER, state);
        assertThat(original.size()).isEqualTo(state.count);
    }

}
