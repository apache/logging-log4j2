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
package org.apache.logging.log4j.layout.json.template.util;

import org.apache.logging.log4j.core.impl.JdkMapAdapterStringMap;
import org.apache.logging.log4j.util.IndexedReadOnlyStringMap;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.util.Strings;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings("DoubleBraceInitialization")
public class JsonWriterTest {

    private static final JsonWriter WRITER = JsonWriter
            .newBuilder()
            .setMaxStringLength(128)
            .setTruncatedStringSuffix("~")
            .build();

    @Test
    public void test_writeValue_null_Object() {
        expectNull(writer -> writer.writeValue(null));
    }

    @Test
    public void test_writeValue() {
        final Object value = Collections.singletonMap("a", "b");
        final String expectedJson = "{'a':'b'}".replace('\'', '"');
        final String actualJson = write(writer -> writer.writeValue(value));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    public void test_writeObject_null_StringMap() {
        expectNull(writer -> writer.writeObject((StringMap) null));
    }

    @Test
    public void test_writeObject_StringMap() {
        final StringMap map = new JdkMapAdapterStringMap(Collections.singletonMap("a", "b"));
        final String expectedJson = "{'a':'b'}".replace('\'', '"');
        final String actualJson = write(writer -> writer.writeObject(map));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    public void test_writeObject_null_IndexedReadOnlyStringMap() {
        expectNull(writer -> writer.writeObject((IndexedReadOnlyStringMap) null));
    }

    @Test
    public void test_writeObject_IndexedReadOnlyStringMap() {
        final IndexedReadOnlyStringMap map =
                new SortedArrayStringMap(new LinkedHashMap<String, Object>() {{
                    put("buzz", 1.2D);
                    put("foo", "bar");
                }});
        final String expectedJson = "{'buzz':1.2,'foo':'bar'}".replace('\'', '"');
        final String actualJson = write(writer -> writer.writeObject(map));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    public void test_writeObject_null_Map() {
        expectNull(writer -> writer.writeObject((Map<String, Object>) null));
    }

    @Test
    public void test_writeObject_Map() {
        final Map<String, Object> map = new LinkedHashMap<String, Object>() {{
            put("key1", "val1");
            put("key2", Collections.singletonMap("key2.1", "val2.1"));
            put("key3", Arrays.asList(
                    3,
                    (byte) 127,
                    4.5D,
                    4.6F,
                    Arrays.asList(true, false),
                    new BigDecimal("30.12345678901234567890123456789"),
                    new BigInteger("12345678901234567890123456789"),
                    Collections.singleton('a'),
                    Collections.singletonMap("key3.3", "val3.3")));
            put("key4", new LinkedHashMap<String, Object>() {{
                put("chars", new char[]{'a', 'b', 'c'});
                put("booleans", new boolean[]{true, false});
                put("bytes", new byte[]{1, 2});
                put("shorts", new short[]{3, 4});
                put("ints", new int[]{256, 257});
                put("longs", new long[]{2147483648L, 2147483649L});
                put("floats", new float[]{1.0F, 1.1F});
                put("doubles", new double[]{2.0D, 2.1D});
                put("objects", new Object[]{"foo", "bar"});
            }});
            put("key5\t", new Object() {
                @Override
                public String toString() {
                    return "custom-object\r";
                }
            });
            put("key6", Arrays.asList(
                    new SortedArrayStringMap(new LinkedHashMap<String, Object>() {{
                        put("buzz", 1.2D);
                        put("foo", "bar");
                    }}),
                    new JdkMapAdapterStringMap(Collections.singletonMap("a", "b"))));
            put("key7", (StringBuilderFormattable) buffer ->
                    buffer.append(7.7777777777777D));
        }};
        final String expectedJson = ("{" +
                "'key1':'val1'," +
                "'key2':{'key2.1':'val2.1'}," +
                "'key3':[" +
                "3," +
                "127," +
                "4.5," +
                "4.6," +
                "[true,false]," +
                "30.12345678901234567890123456789," +
                "12345678901234567890123456789," +
                "['a']," +
                "{'key3.3':'val3.3'}" +
                "]," +
                "'key4':{" +
                "'chars':['a','b','c']," +
                "'booleans':[true,false]," +
                "'bytes':[1,2]," +
                "'shorts':[3,4]," +
                "'ints':[256,257]," +
                "'longs':[2147483648,2147483649]," +
                "'floats':[1.0,1.1]," +
                "'doubles':[2.0,2.1]," +
                "'objects':['foo','bar']" +
                "}," +
                "'key5\\t':'custom-object\\r'," +
                "'key6':[{'buzz':1.2,'foo':'bar'},{'a':'b'}]," +
                "'key7':'7.7777777777777'" +
                "}").replace('\'', '"');
        final String actualJson = write(writer -> writer.writeObject(map));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    public void test_writeArray_null_List() {
        expectNull(writer -> writer.writeArray((List<Object>) null));
    }

    @Test
    public void test_writeArray_List() {
        final List<Object> items = Arrays.asList(
                1, 2, 3,
                "yo",
                Collections.singletonMap("foo", "bar"));
        final String expectedJson = "[1,2,3,\"yo\",{\"foo\":\"bar\"}]";
        final String actualJson = write(writer -> writer.writeArray(items));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    public void test_writeArray_null_Collection() {
        expectNull(writer -> writer.writeArray((Collection<Object>) null));
    }

    @Test
    public void test_writeArray_Collection() {
        final Collection<Object> items = Arrays.asList(
                1, 2, 3,
                Collections.singletonMap("foo", "bar"));
        final String expectedJson = "[1,2,3,{\"foo\":\"bar\"}]";
        final String actualJson = write(writer -> writer.writeArray(items));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    public void test_writeArray_null_char() {
        expectNull(writer -> writer.writeArray((char[]) null));
    }

    @Test
    public void test_writeArray_char() {
        final char[] items = {'\u0000', 'a', 'b', 'c', '\u009f'};
        final String expectedJson = "[\"\\u0000\",\"a\",\"b\",\"c\",\"\\u009F\"]";
        final String actualJson = write(writer -> writer.writeArray(items));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    public void test_writeArray_null_boolean() {
        expectNull(writer -> writer.writeArray((boolean[]) null));
    }

    @Test
    public void test_writeArray_boolean() {
        final boolean[] items = {true, false};
        final String expectedJson = "[true,false]";
        final String actualJson = write(writer -> writer.writeArray(items));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    public void test_writeArray_null_byte() {
        expectNull(writer -> writer.writeArray((byte[]) null));
    }

    @Test
    public void test_writeArray_byte() {
        final byte[] items = {Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE};
        final String expectedJson = Arrays
                .toString(items)
                .replaceAll(" ", "");
        final String actualJson = write(writer -> writer.writeArray(items));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    public void test_writeArray_null_short() {
        expectNull(writer -> writer.writeArray((short[]) null));
    }

    @Test
    public void test_writeArray_short() {
        final short[] items = {Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE};
        final String expectedJson = Arrays
                .toString(items)
                .replaceAll(" ", "");
        final String actualJson = write(writer -> writer.writeArray(items));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    public void test_writeArray_null_int() {
        expectNull(writer -> writer.writeArray((int[]) null));
    }

    @Test
    public void test_writeArray_int() {
        final int[] items = {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE};
        final String expectedJson = Arrays
                .toString(items)
                .replaceAll(" ", "");
        final String actualJson = write(writer -> writer.writeArray(items));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    public void test_writeArray_null_long() {
        expectNull(writer -> writer.writeArray((long[]) null));
    }

    @Test
    public void test_writeArray_long() {
        final long[] items = {Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE};
        final String expectedJson = Arrays
                .toString(items)
                .replaceAll(" ", "");
        final String actualJson = write(writer -> writer.writeArray(items));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    public void test_writeArray_null_float() {
        expectNull(writer -> writer.writeArray((float[]) null));
    }

    @Test
    public void test_writeArray_float() {
        final float[] items = {Float.MIN_VALUE, -1F, 0F, 1F, Float.MAX_VALUE};
        final String expectedJson = Arrays
                .toString(items)
                .replaceAll(" ", "");
        final String actualJson = write(writer -> writer.writeArray(items));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    public void test_writeArray_null_double() {
        expectNull(writer -> writer.writeArray((double[]) null));
    }

    @Test
    public void test_writeArray_double() {
        final double[] items = {Double.MIN_VALUE, -1D, 0D, 1D, Double.MAX_VALUE};
        final String expectedJson = Arrays
                .toString(items)
                .replaceAll(" ", "");
        final String actualJson = write(writer -> writer.writeArray(items));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    public void test_writeArray_null_Object() {
        expectNull(writer -> writer.writeArray((Object[]) null));
    }

    @Test
    public void test_writeArray_Object() {
        final String expectedJson = "[\"foo\",{\"bar\":\"buzz\"},null]";
        final String actualJson = write(writer ->
                writer.writeArray(new Object[]{
                        "foo",
                        Collections.singletonMap("bar", "buzz"),
                        null
                }));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    public void test_writeString_null_formattable() {
        expectNull(writer -> writer.writeString((StringBuilderFormattable) null));
    }

    @Test
    public void test_writeString_formattable() {
        final String expectedJson = "\"foo\\tbar\\tbuzz\"";
        final String actualJson = write(writer ->
                writer.writeString(stringBuilder -> {
                    stringBuilder.append("foo\tbar\tbuzz");
                }));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    public void test_writeString_formattable_excessive_string() {
        final int maxStringLength = WRITER.getMaxStringLength();
        final String excessiveString = Strings.repeat("x", maxStringLength) + 'y';
        final String expectedJson = '"' +
                excessiveString.substring(0, maxStringLength) +
                WRITER.getTruncatedStringSuffix() +
                '"';
        final String actualJson = write(writer ->
                writer.writeString(stringBuilder -> {
                    stringBuilder.append(excessiveString);
                }));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    public void test_writeString_null_seq_1() {
        expectNull(writer -> writer.writeString((CharSequence) null));
    }

    @Test
    public void test_writeString_null_seq_2() {
        expectNull(writer -> writer.writeString((CharSequence) null, 0, 4));
    }

    @Test
    public void test_writeString_seq_negative_offset() {
        Assertions
                .assertThatThrownBy(() ->
                        write(writer -> writer.writeString("a", -1, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("offset");
    }

    @Test
    public void test_writeString_seq_negative_length() {
        Assertions
                .assertThatThrownBy(() ->
                        write(writer -> writer.writeString("a", 0, -1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("length");
    }

    @Test
    public void test_writeString_excessive_seq() {
        final CharSequence seq = Strings.repeat("x", WRITER.getMaxStringLength()) + 'y';
        final String expectedJson = "\"" +
                Strings.repeat("x", WRITER.getMaxStringLength()) +
                WRITER.getTruncatedStringSuffix() +
                '"';
        final String actualJson = write(writer -> writer.writeString(seq));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    public void test_writeString_seq() {
        final String expectedJson = "\"" +
                "\\b" +
                "\\t" +
                "\\f" +
                "\\n" +
                "\\r" +
                "\\\"" +
                "\\\\" +
                "\\u007F" +
                "\"";
        final String actualJson = write(writer ->
                writer.writeString("\b" +
                        "\t" +
                        "\f" +
                        "\n" +
                        "\r" +
                        "\"" +
                        "\\" +
                        '\u007f'));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    public void test_writeString_null_buffer_1() {
        expectNull(writer -> writer.writeString((char[]) null));
    }

    @Test
    public void test_writeString_null_buffer_2() {
        expectNull(writer -> writer.writeString((char[]) null, 0, 4));
    }

    @Test
    public void test_writeString_buffer_negative_offset() {
        Assertions
                .assertThatThrownBy(() ->
                        write(writer -> writer.writeString(new char[]{'a'}, -1, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("offset");
    }

    @Test
    public void test_writeString_buffer_negative_length() {
        Assertions
                .assertThatThrownBy(() ->
                        write(writer -> writer.writeString(new char[]{'a'}, 0, -1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("length");
    }

    @Test
    public void test_writeString_excessive_buffer() {
        final char[] buffer =
                (Strings.repeat("x", WRITER.getMaxStringLength()) + 'y')
                        .toCharArray();
        final String expectedJson = "\"" +
                Strings.repeat("x", WRITER.getMaxStringLength()) +
                WRITER.getTruncatedStringSuffix() +
                '"';
        final String actualJson = write(writer -> writer.writeString(buffer));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    public void test_writeString_buffer() {
        final String expectedJson = "\"" +
                "\\b" +
                "\\t" +
                "\\f" +
                "\\n" +
                "\\r" +
                "\\\"" +
                "\\\\" +
                "\\u007F" +
                "\"";
        final String actualJson = write(writer -> {
            final String seq = "\b" +
                    "\t" +
                    "\f" +
                    "\n" +
                    "\r" +
                    "\"" +
                    "\\" +
                    '\u007f';
            final char[] buffer = seq.toCharArray();
            writer.writeString(buffer);
        });
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    public void test_writeNumber_null_BigDecimal() {
        expectNull(writer -> writer.writeNumber((BigDecimal) null));
    }

    @Test
    public void test_writeNumber_BigDecimal() {
        for (final BigDecimal number : new BigDecimal[]{
                BigDecimal.ZERO,
                BigDecimal.ONE,
                BigDecimal.TEN,
                new BigDecimal("" + Long.MAX_VALUE +
                        "" + Long.MAX_VALUE +
                        '.' + Long.MAX_VALUE +
                        "" + Long.MAX_VALUE)}) {
            final String expectedJson = String.valueOf(number);
            final String actualJson = write(writer -> writer.writeNumber(number));
            Assertions.assertThat(actualJson).isEqualTo(expectedJson);
        }
    }

    @Test
    public void test_writeNumber_null_BigInteger() {
        expectNull(writer -> writer.writeNumber((BigInteger) null));
    }

    @Test
    public void test_writeNumber_BigInteger() {
        for (final BigInteger number : new BigInteger[]{
                BigInteger.ZERO,
                BigInteger.ONE,
                BigInteger.TEN,
                new BigInteger("" + Long.MAX_VALUE + "" + Long.MAX_VALUE)}) {
            final String expectedJson = String.valueOf(number);
            final String actualJson = write(writer -> writer.writeNumber(number));
            Assertions.assertThat(actualJson).isEqualTo(expectedJson);
        }
    }

    @Test
    public void test_writeNumber_float() {
        for (final float number : new float[]{Float.MIN_VALUE, -1.0F, 0F, 1.0F, Float.MAX_VALUE}) {
            final String expectedJson = String.valueOf(number);
            final String actualJson = write(writer -> writer.writeNumber(number));
            Assertions.assertThat(actualJson).isEqualTo(expectedJson);
        }
    }

    @Test
    public void test_writeNumber_double() {
        for (final double number : new double[]{Double.MIN_VALUE, -1.0D, 0D, 1.0D, Double.MAX_VALUE}) {
            final String expectedJson = String.valueOf(number);
            final String actualJson = write(writer -> writer.writeNumber(number));
            Assertions.assertThat(actualJson).isEqualTo(expectedJson);
        }
    }

    @Test
    public void test_writeNumber_short() {
        for (final short number : new short[]{Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE}) {
            final String expectedJson = String.valueOf(number);
            final String actualJson = write(writer -> writer.writeNumber(number));
            Assertions.assertThat(actualJson).isEqualTo(expectedJson);
        }
    }

    @Test
    public void test_writeNumber_int() {
        for (final int number : new int[]{Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE}) {
            final String expectedJson = String.valueOf(number);
            final String actualJson = write(writer -> writer.writeNumber(number));
            Assertions.assertThat(actualJson).isEqualTo(expectedJson);
        }
    }

    @Test
    public void test_writeNumber_long() {
        for (final long number : new long[]{Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE}) {
            final String expectedJson = String.valueOf(number);
            final String actualJson = write(writer -> writer.writeNumber(number));
            Assertions.assertThat(actualJson).isEqualTo(expectedJson);
        }
    }

    @Test
    public void test_writeNumber_integral_and_negative_fractional() {
        Assertions
                .assertThatThrownBy(() -> write(writer -> writer.writeNumber(0, -1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("was expecting a positive fraction: -1");
    }

    @Test
    public void test_writeNumber_integral_and_zero_fractional() {
        final String expectedJson = "123";
        final String actualJson = write(writer -> writer.writeNumber(123L, 0L));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    public void test_writeNumber_integral_and_fractional() {
        final String expectedJson = "123.456";
        final String actualJson = write(writer -> writer.writeNumber(123L, 456L));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    public void test_writeBoolean_true() {
        final String expectedJson = "true";
        final String actualJson = write(writer -> writer.writeBoolean(true));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    public void test_writeBoolean_false() {
        final String expectedJson = "false";
        final String actualJson = write(writer -> writer.writeBoolean(false));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    public void test_writeNull() {
        expectNull(JsonWriter::writeNull);
    }

    private void expectNull(Consumer<JsonWriter> body) {
        final String expectedJson = "null";
        final String actualJson = write(body);
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    public void test_writeRawString_null_seq() {
        Assertions
                .assertThatThrownBy(() ->
                        write(writer ->
                                writer.writeRawString((String) null)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("seq");
    }

    @Test
    public void test_writeRawString_seq_negative_offset() {
        Assertions
                .assertThatThrownBy(() ->
                        write(writer ->
                                writer.writeRawString("a", -1, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("offset");
    }

    @Test
    public void test_writeRawString_seq_negative_length() {
        Assertions
                .assertThatThrownBy(() ->
                        write(writer ->
                                writer.writeRawString("a", 0, -1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("length");
    }

    @Test
    public void test_writeRawString_seq() {
        final String expectedJson = "this is not a valid JSON string";
        final String actualJson = write(writer -> writer.writeRawString(expectedJson));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    public void test_writeRawString_null_buffer() {
        Assertions
                .assertThatThrownBy(() -> write(writer -> writer.writeRawString((char[]) null)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("buffer");
    }

    @Test
    public void test_writeRawString_buffer_negative_offset() {
        Assertions
                .assertThatThrownBy(() ->
                        write(writer ->
                                writer.writeRawString(new char[]{'a'}, -1, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("offset");
    }

    @Test
    public void test_writeRawString_buffer_negative_length() {
        Assertions
                .assertThatThrownBy(() ->
                        write(writer ->
                                writer.writeRawString(new char[]{'a'}, 0, -1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("length");
    }

    @Test
    public void test_writeRawString_buffer() {
        final String expectedJson = "this is not a valid JSON string";
        final String actualJson = write(writer -> writer.writeRawString(expectedJson.toCharArray()));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    private static String write(Consumer<JsonWriter> body) {
        final JsonWriter writer = getCleanWriter();
        body.accept(writer);
        return writer.getStringBuilder().toString();
    }

    private static JsonWriter getCleanWriter() {
        WRITER.close();
        return WRITER;
    }

}
