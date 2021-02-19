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
package org.apache.logging.log4j.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests the SortedArrayStringMap class.
 */
public class SortedArrayStringMapTest {

    @Test
    public void testConstructorDisallowsNegativeCapacity() {
        assertThatThrownBy(() -> new SortedArrayStringMap(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testConstructorAllowsZeroCapacity() {
        SortedArrayStringMap sortedArrayStringMap = new SortedArrayStringMap(0);
        assertThat(sortedArrayStringMap.size()).isEqualTo(0);
    }

    @Test
    public void testConstructorIgnoresNull() {
        assertThat(new SortedArrayStringMap((SortedArrayStringMap) null).size()).isEqualTo(0);
    }

    @Test
    public void testToString() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");
        assertThat(original.toString()).isEqualTo("{3=3value, B=Bvalue, a=avalue}");
    }

    @Test
    public void testSerialization() throws Exception {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");

        final byte[] binary = serialize(original);
        final SortedArrayStringMap copy = deserialize(binary);
        assertThat(copy).isEqualTo(original);
    }

    @Test
    public void testSerializationOfNonSerializableValue() throws Exception {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("unserializable", new Object());

        final byte[] binary = serialize(original);
        final SortedArrayStringMap copy = deserialize(binary);

        final SortedArrayStringMap expected = new SortedArrayStringMap();
        expected.putValue("a", "avalue");
        expected.putValue("B", "Bvalue");
        expected.putValue("unserializable", null);
        assertThat(copy).isEqualTo(expected);
    }

    @Test
    public void testDeserializationOfUnknownClass() throws Exception {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "avalue");
        original.putValue("serializableButNotInClasspathOfDeserializer", new org.junit.runner.Result());
        original.putValue("zz", "last");

        final File file = new File("target/SortedArrayStringMap.ser");
        try (FileOutputStream fout = new FileOutputStream(file, false)) {
            fout.write(serialize(original));
            fout.flush();
        }
        final String classpath = createClassPath(SortedArrayStringMap.class, DeserializerHelper.class);
        final Process process = new ProcessBuilder("java", "-cp", classpath,
                DeserializerHelper.class.getName(), file.getPath()).start();
        final BufferedReader in = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        final int exitValue = process.waitFor();

        file.delete();
        if (exitValue != 0) {
            final StringBuilder sb = new StringBuilder();
            sb.append("DeserializerHelper exited with error code ").append(exitValue);
            sb.append(". Classpath='").append(classpath);
            sb.append("'. Process output: ");
            int c = -1;
            while ((c = in.read()) != -1) {
                sb.append((char) c);
            }
            fail(sb.toString());
        }
    }

    private String createClassPath(final Class<?>... classes) throws Exception {
        final StringBuilder result = new StringBuilder();
        for (final Class<?> cls : classes) {
            if (result.length() > 0) {
                result.append(File.pathSeparator);
            }
            result.append(createClassPath(cls));
        }
        return result.toString();
    }

    private String createClassPath(final Class<?> cls) throws Exception {
        final String resource = "/" + cls.getName().replace('.', '/') + ".class";
        final URL url = cls.getResource(resource);
        String location = url.toString();
        if (location.startsWith("jar:")) {
            location = location.substring("jar:".length(), location.indexOf('!'));
        }
        if (location.startsWith("file:/")) {
            location = location.substring("file:/".length());
        }
        if (location.endsWith(resource)) {
            location = location.substring(0, location.length() - resource.length());
        }
        if (!new File(location).exists()) {
            location = File.separator + location;
        }
        location = URLDecoder.decode(location, Charset.defaultCharset().name()); // replace %20 with ' ' etc
        return location.isEmpty() ? "." : location;
    }

    private byte[] serialize(final SortedArrayStringMap data) throws IOException {
        final ByteArrayOutputStream arr = new ByteArrayOutputStream();
        final ObjectOutputStream out = new ObjectOutputStream(arr);
        out.writeObject(data);
        return arr.toByteArray();
    }

    private SortedArrayStringMap deserialize(final byte[] binary) throws IOException, ClassNotFoundException {
        final ByteArrayInputStream inArr = new ByteArrayInputStream(binary);
        try (final ObjectInputStream in = new FilteredObjectInputStream(inArr)) {
            final SortedArrayStringMap result = (SortedArrayStringMap) in.readObject();
            return result;
        }
    }

    @Test
    public void testPutAll() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");

        final SortedArrayStringMap other = new SortedArrayStringMap();
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
    public void testPutAll_overwritesSameKeys2() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "aORIG");
        original.putValue("b", "bORIG");
        original.putValue("c", "cORIG");
        original.putValue("d", "dORIG");
        original.putValue("e", "eORIG");

        final SortedArrayStringMap other = new SortedArrayStringMap();
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
    public void testPutAll_nullKeyInLargeOriginal() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue(null, "nullORIG");
        original.putValue("a", "aORIG");
        original.putValue("b", "bORIG");
        original.putValue("c", "cORIG");
        original.putValue("d", "dORIG");
        original.putValue("e", "eORIG");

        final SortedArrayStringMap other = new SortedArrayStringMap();
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
    public void testPutAll_nullKeyInSmallOriginal() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue(null, "nullORIG");
        original.putValue("a", "aORIG");
        original.putValue("b", "bORIG");

        final SortedArrayStringMap other = new SortedArrayStringMap();
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
    public void testPutAll_nullKeyInSmallAdditional() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "aORIG");
        original.putValue("b", "bORIG");
        original.putValue("c", "cORIG");
        original.putValue("d", "dORIG");
        original.putValue("e", "eORIG");

        final SortedArrayStringMap other = new SortedArrayStringMap();
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
    public void testPutAll_nullKeyInLargeAdditional() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "aORIG");
        original.putValue("b", "bORIG");

        final SortedArrayStringMap other = new SortedArrayStringMap();
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
    public void testPutAll_nullKeyInBoth_LargeOriginal() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue(null, "nullORIG");
        original.putValue("a", "aORIG");
        original.putValue("b", "bORIG");
        original.putValue("c", "cORIG");
        original.putValue("d", "dORIG");
        original.putValue("e", "eORIG");

        final SortedArrayStringMap other = new SortedArrayStringMap();
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
    public void testPutAll_nullKeyInBoth_SmallOriginal() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue(null, "nullORIG");
        original.putValue("a", "aORIG");
        original.putValue("b", "bORIG");

        final SortedArrayStringMap other = new SortedArrayStringMap();
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
    public void testPutAll_overwritesSameKeys1() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "aORIG");
        original.putValue("b", "bORIG");
        original.putValue("c", "cORIG");

        final SortedArrayStringMap other = new SortedArrayStringMap();
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
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");
        assertThat(original).isEqualTo(original); // equal to itself

        final SortedArrayStringMap other = new SortedArrayStringMap();
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
    public void testToMap() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");

        final Map<String, Object> expected = new HashMap<>();
        expected.put("a", "avalue");
        expected.put("B", "Bvalue");
        expected.put("3", "3value");

        assertThat(original.toMap()).isEqualTo(expected);

        assertDoesNotThrow(() -> original.toMap().put("abc", "xyz"), "Expected map to be mutable");
    }

    @Test
    public void testPutAll_KeepsExistingValues() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "aaa");
        original.putValue("b", "bbb");
        original.putValue("c", "ccc");
        assertThat(original.size()).describedAs("size").isEqualTo(3);

        // add empty context data
        original.putAll(new SortedArrayStringMap());
        assertThat(original.size()).describedAs("size after put empty").isEqualTo(3);
        assertThat(original.<String>getValue("a")).isEqualTo("aaa");
        assertThat(original.<String>getValue("b")).isEqualTo("bbb");
        assertThat(original.<String>getValue("c")).isEqualTo("ccc");

        final SortedArrayStringMap other = new SortedArrayStringMap();
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
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "aaa");
        original.putValue("b", "bbb");
        original.putValue("c", "ccc");
        original.putValue("d", "ddd");
        assertThat(original.size()).describedAs("size").isEqualTo(4);

        // add empty context data
        original.putAll(new SortedArrayStringMap());
        assertThat(original.size()).describedAs("size after put empty").isEqualTo(4);
        assertThat(original.<String>getValue("a")).isEqualTo("aaa");
        assertThat(original.<String>getValue("b")).isEqualTo("bbb");
        assertThat(original.<String>getValue("c")).isEqualTo("ccc");
        assertThat(original.<String>getValue("d")).isEqualTo("ddd");

        final SortedArrayStringMap other = new SortedArrayStringMap();
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
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue(null, "nullVal");
        original.putValue("a", "aaa");
        original.putValue("b", "bbb");
        original.putValue("c", "ccc");
        original.putValue("d", "ddd");
        assertThat(original.size()).describedAs("size").isEqualTo(5);

        final SortedArrayStringMap other = new SortedArrayStringMap();
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
        final SortedArrayStringMap original = new SortedArrayStringMap();
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
    public void testConcurrentModificationBiConsumerPut() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "aaa");
        assertThatThrownBy(() -> original.forEach((s, o) -> original.putValue("c", "other"))).isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    public void testConcurrentModificationBiConsumerPutValue() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "aaa");
        assertThatThrownBy(() -> original.forEach((s, o) -> original.putValue("c", "other"))).isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    public void testConcurrentModificationBiConsumerRemove() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "aaa");
        assertThatThrownBy(() -> original.forEach((s, o) -> original.remove("a"))).isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    public void testConcurrentModificationBiConsumerClear() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "aaa");
        assertThatThrownBy(() -> original.forEach((s, o) -> original.clear())).isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    public void testConcurrentModificationTriConsumerPut() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "aaa");
        assertThatThrownBy(() -> original.forEach((s, o, o2) -> original.putValue("c", "other"), null)).isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    public void testConcurrentModificationTriConsumerPutValue() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "aaa");
        assertThatThrownBy(() -> original.forEach((s, o, o2) -> original.putValue("c", "other"), null)).isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    public void testConcurrentModificationTriConsumerRemove() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "aaa");
        assertThatThrownBy(() -> original.forEach((s, o, o2) -> original.remove("a"), null)).isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    public void testConcurrentModificationTriConsumerClear() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "aaa");
        assertThatThrownBy(() -> original.forEach((s, o, o2) -> original.clear(), null)).isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    public void testInitiallyNotFrozen() {
        assertThat(new SortedArrayStringMap().isFrozen()).isFalse();
    }

    @Test
    public void testIsFrozenAfterCallingFreeze() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        assertFalse(original.isFrozen(), "before freeze");
        original.freeze();
        assertTrue(original.isFrozen(), "after freeze");
    }

    @Test
    public void testFreezeProhibitsPutValue() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.freeze();
        assertThatThrownBy(() -> original.putValue("a", "aaa")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testFreezeProhibitsRemove() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("b", "bbb");
        original.freeze();
        assertThatThrownBy(() -> original.remove("b")).isInstanceOf(UnsupportedOperationException.class); // existing key: modifies the collection
    }

    @Test
    public void testFreezeAllowsRemoveOfNonExistingKey() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("b", "bbb");
        original.freeze();
        assertThatCode(() -> original.remove("a")).doesNotThrowAnyException();
    }

    @Test
    public void testFreezeAllowsRemoveIfEmpty() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.freeze();
        assertThatCode(() -> original.remove("a")).doesNotThrowAnyException();
    }

    @Test
    public void testFreezeProhibitsClear() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "aaa");
        original.freeze();
        assertThrows(UnsupportedOperationException.class, original::clear);
    }

    @Test
    public void testFreezeAllowsClearIfEmpty() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.freeze();
        assertDoesNotThrow(original::clear);
    }

    @Test
    public void testPutInsertsInAlphabeticOrder() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");
        original.putValue("c", "cvalue");
        original.putValue("d", "dvalue");

        assertThat(original.<String>getValue("a")).isEqualTo("avalue");
        assertThat(original.<String>getValueAt(2)).isEqualTo("avalue");

        assertThat(original.<String>getValue("B")).isEqualTo("Bvalue");
        assertThat(original.<String>getValueAt(1)).isEqualTo("Bvalue");

        assertThat(original.<String>getValue("3")).isEqualTo("3value");
        assertThat(original.<String>getValueAt(0)).isEqualTo("3value");

        assertThat(original.<String>getValue("c")).isEqualTo("cvalue");
        assertThat(original.<String>getValueAt(3)).isEqualTo("cvalue");

        assertThat(original.<String>getValue("d")).isEqualTo("dvalue");
        assertThat(original.<String>getValueAt(4)).isEqualTo("dvalue");
    }

    @Test
    public void testPutValueInsertsInAlphabeticOrder() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");
        original.putValue("c", "cvalue");
        original.putValue("d", "dvalue");

        assertThat(original.<String>getValue("a")).isEqualTo("avalue");
        assertThat(original.<String>getValueAt(2)).isEqualTo("avalue");

        assertThat(original.<String>getValue("B")).isEqualTo("Bvalue");
        assertThat(original.<String>getValueAt(1)).isEqualTo("Bvalue");

        assertThat(original.<String>getValue("3")).isEqualTo("3value");
        assertThat(original.<String>getValueAt(0)).isEqualTo("3value");

        assertThat(original.<String>getValue("c")).isEqualTo("cvalue");
        assertThat(original.<String>getValueAt(3)).isEqualTo("cvalue");

        assertThat(original.<String>getValue("d")).isEqualTo("dvalue");
        assertThat(original.<String>getValueAt(4)).isEqualTo("dvalue");
    }

    @Test
    public void testNullKeysAllowed() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");
        original.putValue("c", "cvalue");
        original.putValue("d", "dvalue");
        assertThat(original.size()).isEqualTo(5);
        assertThat(original.toString()).isEqualTo("{3=3value, B=Bvalue, a=avalue, c=cvalue, d=dvalue}");

        original.putValue(null, "nullvalue");
        assertThat(original.size()).isEqualTo(6);
        assertThat(original.toString()).isEqualTo("{null=nullvalue, 3=3value, B=Bvalue, a=avalue, c=cvalue, d=dvalue}");

        original.putValue(null, "otherNullvalue");
        assertThat(original.toString()).isEqualTo("{null=otherNullvalue, 3=3value, B=Bvalue, a=avalue, c=cvalue, d=dvalue}");
        assertThat(original.size()).isEqualTo(6);

        original.putValue(null, "nullvalue");
        assertThat(original.size()).isEqualTo(6);
        assertThat(original.toString()).isEqualTo("{null=nullvalue, 3=3value, B=Bvalue, a=avalue, c=cvalue, d=dvalue}");

        original.putValue(null, "abc");
        assertThat(original.size()).isEqualTo(6);
        assertThat(original.toString()).isEqualTo("{null=abc, 3=3value, B=Bvalue, a=avalue, c=cvalue, d=dvalue}");
    }

    @Test
    public void testNullKeysCopiedToAsMap() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
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
        final SortedArrayStringMap original = new SortedArrayStringMap();
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
    public void testRemoveNullsOutRemovedSlot() throws Exception {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "avalue");
        original.putValue("b", "bvalue");
        original.putValue("c", "cvalue");
        original.putValue("d", "dvalue");
        original.remove("a");
        original.remove("b");
        original.remove("c");
        original.remove("d");
        assertThat(original.<String>getValueAt(0)).isNull();

        // ensure slots in the values array are nulled out
        final Field f = SortedArrayStringMap.class.getDeclaredField("values");
        f.setAccessible(true);
        final Object[] values = (Object[]) f.get(original);
        assertAll(Arrays.stream(values).map(value -> () -> assertThat(value).isNull()));
    }

    @Test
    public void testRemoveWhenFull() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "avalue");
        original.putValue("b", "bvalue");
        original.putValue("c", "cvalue");
        original.putValue("d", "dvalue"); // default capacity = 4
        original.remove("d");
    }

    @Test
    public void testNullValuesArePreserved() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
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
    public void testGet() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
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
    public void testGetValue_GetValueAt() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");

        assertThat(original.<String>getValue("a")).isEqualTo("avalue");
        assertThat(original.<String>getValueAt(2)).isEqualTo("avalue");

        assertThat(original.<String>getValue("B")).isEqualTo("Bvalue");
        assertThat(original.<String>getValueAt(1)).isEqualTo("Bvalue");

        assertThat(original.<String>getValue("3")).isEqualTo("3value");
        assertThat(original.<String>getValueAt(0)).isEqualTo("3value");

        original.putValue("0", "0value");
        assertThat(original.<String>getValue("0")).isEqualTo("0value");
        assertThat(original.<String>getValueAt(0)).isEqualTo("0value");
        assertThat(original.<String>getValue("3")).isEqualTo("3value");
        assertThat(original.<String>getValueAt(1)).isEqualTo("3value");
        assertThat(original.<String>getValue("B")).isEqualTo("Bvalue");
        assertThat(original.<String>getValueAt(2)).isEqualTo("Bvalue");
        assertThat(original.<String>getValue("a")).isEqualTo("avalue");
        assertThat(original.<String>getValueAt(3)).isEqualTo("avalue");
    }

    @Test
    public void testClear() throws Exception {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");
        assertThat(original.size()).isEqualTo(3);

        original.clear();
        assertThat(original.size()).isEqualTo(0);

        // ensure slots in the values array are nulled out
        final Field f = SortedArrayStringMap.class.getDeclaredField("values");
        f.setAccessible(true);
        final Object[] values = (Object[]) f.get(original);
        assertAll(Arrays.stream(values).map(value -> () -> assertThat(value).isNull()));
    }

    @Test
    public void testIndexOfKey() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "avalue");
        assertThat(original.indexOfKey("a")).isEqualTo(0);

        original.putValue("B", "Bvalue");
        assertThat(original.indexOfKey("a")).isEqualTo(1);
        assertThat(original.indexOfKey("B")).isEqualTo(0);

        original.putValue("3", "3value");
        assertThat(original.indexOfKey("a")).isEqualTo(2);
        assertThat(original.indexOfKey("B")).isEqualTo(1);
        assertThat(original.indexOfKey("3")).isEqualTo(0);

        original.putValue("A", "AAA");
        assertThat(original.indexOfKey("a")).isEqualTo(3);
        assertThat(original.indexOfKey("B")).isEqualTo(2);
        assertThat(original.indexOfKey("A")).isEqualTo(1);
        assertThat(original.indexOfKey("3")).isEqualTo(0);

        original.putValue("C", "CCC");
        assertThat(original.indexOfKey("a")).isEqualTo(4);
        assertThat(original.indexOfKey("C")).isEqualTo(3);
        assertThat(original.indexOfKey("B")).isEqualTo(2);
        assertThat(original.indexOfKey("A")).isEqualTo(1);
        assertThat(original.indexOfKey("3")).isEqualTo(0);

        original.putValue("2", "222");
        assertThat(original.indexOfKey("a")).isEqualTo(5);
        assertThat(original.indexOfKey("C")).isEqualTo(4);
        assertThat(original.indexOfKey("B")).isEqualTo(3);
        assertThat(original.indexOfKey("A")).isEqualTo(2);
        assertThat(original.indexOfKey("3")).isEqualTo(1);
        assertThat(original.indexOfKey("2")).isEqualTo(0);
    }

    @Test
    public void testContainsKey() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
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
    public void testGetValueAt() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "avalue");
        assertThat(original.getKeyAt(0)).isEqualTo("a");
        assertThat(original.<String>getValueAt(0)).isEqualTo("avalue");

        original.putValue("B", "Bvalue");
        assertThat(original.getKeyAt(0)).isEqualTo("B");
        assertThat(original.<String>getValueAt(0)).isEqualTo("Bvalue");
        assertThat(original.getKeyAt(1)).isEqualTo("a");
        assertThat(original.<String>getValueAt(1)).isEqualTo("avalue");

        original.putValue("3", "3value");
        assertThat(original.getKeyAt(0)).isEqualTo("3");
        assertThat(original.<String>getValueAt(0)).isEqualTo("3value");
        assertThat(original.getKeyAt(1)).isEqualTo("B");
        assertThat(original.<String>getValueAt(1)).isEqualTo("Bvalue");
        assertThat(original.getKeyAt(2)).isEqualTo("a");
        assertThat(original.<String>getValueAt(2)).isEqualTo("avalue");
    }

    @Test
    public void testSizeAndIsEmpty() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
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
    public void testForEachBiConsumer() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");

        original.forEach(new BiConsumer<String, String>() {
            int count = 0;
            @Override
            public void accept(final String key, final String value) {
                assertThat(original.getKeyAt(count)).describedAs("key").isEqualTo(key);
                assertThat(original.<String>getValueAt(count)).describedAs("val").isEqualTo(value);
                count++;
                assertTrue(count <= original.size(), "count should not exceed size but was " + count);
            }
        });
    }

    static class State {
        SortedArrayStringMap data;
        int count;
    }
    static TriConsumer<String, String, State> COUNTER = (key, value, state) -> {
        assertThat(state.data.getKeyAt(state.count)).describedAs("key").isEqualTo(key);
        assertThat(state.data.<String>getValueAt(state.count)).describedAs("val").isEqualTo(value);
        state.count++;
        assertTrue(
                state.count <= state.data.size(), "count should not exceed size but was " + state.count);
    };

    @Test
    public void testForEachTriConsumer() {
        final SortedArrayStringMap original = new SortedArrayStringMap();
        original.putValue("a", "avalue");
        original.putValue("B", "Bvalue");
        original.putValue("3", "3value");

        final State state = new State();
        state.data = original;
        original.forEach(COUNTER, state);
        assertThat(original.size()).isEqualTo(state.count);
    }
}
