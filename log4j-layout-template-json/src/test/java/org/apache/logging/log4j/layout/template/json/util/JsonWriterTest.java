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
package org.apache.logging.log4j.layout.template.json.util;

import org.apache.logging.log4j.core.impl.JdkMapAdapterStringMap;
import org.apache.logging.log4j.layout.template.json.JacksonFixture;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.IndexedReadOnlyStringMap;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.util.Strings;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("DoubleBraceInitialization")
class JsonWriterTest {

    private static final JsonWriter WRITER = JsonWriter
            .newBuilder()
            .setMaxStringLength(128)
            .setTruncatedStringSuffix("~")
            .build();

    private static final int SURROGATE_CODE_POINT = 65536;

    private static final char[] SURROGATE_PAIR = new char[2];
    static {
        // noinspection ResultOfMethodCallIgnored
        Character.toChars(SURROGATE_CODE_POINT, SURROGATE_PAIR, 0);
    }

    private static final char HI_SURROGATE = SURROGATE_PAIR[0];

    private static final char LO_SURROGATE = SURROGATE_PAIR[1];

    private static synchronized <V> V withLockedWriterReturning(
            final Function<JsonWriter, V> consumer) {
        synchronized (WRITER) {
            return consumer.apply(WRITER);
        }
    }

    private static synchronized void withLockedWriter(
            final Consumer<JsonWriter> consumer) {
        synchronized (WRITER) {
            consumer.accept(WRITER);
        }
    }

    @Test
    void test_close() {
        withLockedWriter(writer -> {
            writer.writeString("x");
            writer.close();
            assertStringBuilderReset(writer);
        });
    }

    @Test
    void test_close_after_excessive_write() {
        withLockedWriter(writer -> {
            final String text = Strings.repeat("x", writer.getMaxStringLength());
            writer.writeString(text);
            writer.writeString(text);
            writer.close();
            assertStringBuilderReset(writer);
        });
    }

    private static void assertStringBuilderReset(final JsonWriter writer) {
        Assertions
                .assertThat(writer.getStringBuilder().capacity())
                .isEqualTo(writer.getMaxStringLength());
        Assertions
                .assertThat(writer.getStringBuilder().length())
                .isEqualTo(0);
    }

    @Test
    void test_surrogate_code_point() {
        Assertions
                .assertThat(HI_SURROGATE)
                .matches(Character::isHighSurrogate, "is high surrogate");
        Assertions
                .assertThat(LO_SURROGATE)
                .matches(Character::isLowSurrogate, "is low surrogate");
        Assertions
                .assertThat(Character.isSurrogatePair(HI_SURROGATE, LO_SURROGATE))
                .as("is surrogate pair")
                .isTrue();
        Assertions
                .assertThat(SURROGATE_CODE_POINT)
                .matches(Character::isDefined, "is defined");
    }

    @Test
    void test_use_null_Runnable() {
        Assertions
                .assertThatThrownBy(() -> withLockedWriter(writer -> writer.use(null)))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("runnable");
    }

    @Test
    void test_use_failing_Runnable() {
        final RuntimeException exception = new RuntimeException();
        withLockedWriter(writer -> {
            final int initialLength = writer.getStringBuilder().length();
            Assertions
                    .assertThatThrownBy(() -> writer.use(() -> {
                        writer.writeString("extending the buffer");
                        throw exception;
                    }))
                    .isSameAs(exception);
            Assertions.assertThat(writer.getStringBuilder()).hasSize(initialLength);
        });
    }

    @Test
    void test_writeValue_null_Object() {
        expectNull(writer -> writer.writeValue(null));
    }

    @Test
    void test_writeValue() {
        final Object value = Collections.singletonMap("a", "b");
        final String expectedJson = "{'a':'b'}".replace('\'', '"');
        final String actualJson = withLockedWriterReturning(writer ->
                writer.use(() -> writer.writeValue(value)));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    void test_writeObject_null_StringMap() {
        expectNull(writer -> writer.writeObject((StringMap) null));
    }

    @Test
    void test_writeObject_StringMap() {
        final StringMap map = new JdkMapAdapterStringMap(Collections.singletonMap("a", "b"));
        final String expectedJson = "{'a':'b'}".replace('\'', '"');
        final String actualJson = withLockedWriterReturning(writer ->
                writer.use(() -> writer.writeObject(map)));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    void test_writeObject_null_IndexedReadOnlyStringMap() {
        expectNull(writer -> writer.writeObject((IndexedReadOnlyStringMap) null));
    }

    @Test
    void test_writeObject_IndexedReadOnlyStringMap() {
        final IndexedReadOnlyStringMap map =
                new SortedArrayStringMap(new LinkedHashMap<String, Object>() {{
                    put("buzz", 1.2D);
                    put("foo", "bar");
                }});
        final String expectedJson = "{'buzz':1.2,'foo':'bar'}".replace('\'', '"');
        final String actualJson = withLockedWriterReturning(writer ->
                writer.use(() -> writer.writeObject(map)));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    void test_writeObject_null_Map() {
        expectNull(writer -> writer.writeObject((Map<String, Object>) null));
    }

    @Test
    void test_writeObject_Map() {
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
        final String actualJson = withLockedWriterReturning(writer ->
                writer.use(() -> writer.writeObject(map)));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    void test_writeArray_null_List() {
        expectNull(writer -> writer.writeArray((List<Object>) null));
    }

    @Test
    void test_writeArray_List() {
        final List<Object> items = Arrays.asList(
                1, 2, 3,
                "yo",
                Collections.singletonMap("foo", "bar"));
        final String expectedJson = "[1,2,3,\"yo\",{\"foo\":\"bar\"}]";
        final String actualJson = withLockedWriterReturning(writer ->
                writer.use(() -> writer.writeArray(items)));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    void test_writeArray_null_Collection() {
        expectNull(writer -> writer.writeArray((Collection<Object>) null));
    }

    @Test
    void test_writeArray_Collection() {
        final Collection<Object> items = Arrays.asList(
                1, 2, 3,
                Collections.singletonMap("foo", "bar"));
        final String expectedJson = "[1,2,3,{\"foo\":\"bar\"}]";
        final String actualJson = withLockedWriterReturning(writer ->
                writer.use(() -> writer.writeArray(items)));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    void test_writeArray_null_char() {
        expectNull(writer -> writer.writeArray((char[]) null));
    }

    @Test
    void test_writeArray_char() {
        final char[] items = {'\u0000', 'a', 'b', 'c', '\u007f'};
        final String expectedJson = "[\"\\u0000\",\"a\",\"b\",\"c\",\"\u007F\"]";
        final String actualJson = withLockedWriterReturning(writer ->
                writer.use(() -> writer.writeArray(items)));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    void test_writeArray_null_boolean() {
        expectNull(writer -> writer.writeArray((boolean[]) null));
    }

    @Test
    void test_writeArray_boolean() {
        final boolean[] items = {true, false};
        final String expectedJson = "[true,false]";
        final String actualJson = withLockedWriterReturning(writer ->
                writer.use(() -> writer.writeArray(items)));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    void test_writeArray_null_byte() {
        expectNull(writer -> writer.writeArray((byte[]) null));
    }

    @Test
    void test_writeArray_byte() {
        final byte[] items = {Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE};
        final String expectedJson = Arrays
                .toString(items)
                .replaceAll(" ", "");
        final String actualJson = withLockedWriterReturning(writer ->
                writer.use(() -> writer.writeArray(items)));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    void test_writeArray_null_short() {
        expectNull(writer -> writer.writeArray((short[]) null));
    }

    @Test
    void test_writeArray_short() {
        final short[] items = {Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE};
        final String expectedJson = Arrays
                .toString(items)
                .replaceAll(" ", "");
        final String actualJson = withLockedWriterReturning(writer ->
                writer.use(() -> writer.writeArray(items)));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    void test_writeArray_null_int() {
        expectNull(writer -> writer.writeArray((int[]) null));
    }

    @Test
    void test_writeArray_int() {
        final int[] items = {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE};
        final String expectedJson = Arrays
                .toString(items)
                .replaceAll(" ", "");
        final String actualJson = withLockedWriterReturning(writer ->
                writer.use(() -> writer.writeArray(items)));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    void test_writeArray_null_long() {
        expectNull(writer -> writer.writeArray((long[]) null));
    }

    @Test
    void test_writeArray_long() {
        final long[] items = {Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE};
        final String expectedJson = Arrays
                .toString(items)
                .replaceAll(" ", "");
        final String actualJson = withLockedWriterReturning(writer ->
                writer.use(() -> writer.writeArray(items)));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    void test_writeArray_null_float() {
        expectNull(writer -> writer.writeArray((float[]) null));
    }

    @Test
    void test_writeArray_float() {
        final float[] items = {Float.MIN_VALUE, -1F, 0F, 1F, Float.MAX_VALUE};
        final String expectedJson = Arrays
                .toString(items)
                .replaceAll(" ", "");
        final String actualJson = withLockedWriterReturning(writer ->
                writer.use(() -> writer.writeArray(items)));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    void test_writeArray_null_double() {
        expectNull(writer -> writer.writeArray((double[]) null));
    }

    @Test
    void test_writeArray_double() {
        final double[] items = {Double.MIN_VALUE, -1D, 0D, 1D, Double.MAX_VALUE};
        final String expectedJson = Arrays
                .toString(items)
                .replaceAll(" ", "");
        final String actualJson = withLockedWriterReturning(writer ->
                writer.use(() -> writer.writeArray(items)));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    void test_writeArray_null_Object() {
        expectNull(writer -> writer.writeArray((Object[]) null));
    }

    @Test
    void test_writeArray_Object() {
        final String expectedJson = "[\"foo\",{\"bar\":\"buzz\"},null]";
        final String actualJson = withLockedWriterReturning(writer ->
                writer.use(() ->
                        writer.writeArray(new Object[]{
                                "foo",
                                Collections.singletonMap("bar", "buzz"),
                                null
                        })));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    void test_writeString_null_emitter() {
        Assertions
                .assertThatThrownBy(() ->
                        withLockedWriter(writer ->
                                writer.use(() ->
                                        writer.writeString(null, 0L))))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("emitter");
    }

    @Test
    void test_writeString_emitter() {
        final String state = "there-is-no-spoon";
        final BiConsumer<StringBuilder, String> emitter = StringBuilder::append;
        final String expectedJson = '"' + state + '"';
        final String actualJson = withLockedWriterReturning(writer ->
                writer.use(() -> writer.writeString(emitter, state)));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    void test_writeString_emitter_excessive_string() {
        withLockedWriter(writer -> {
            final int maxStringLength = writer.getMaxStringLength();
            final String excessiveString = Strings.repeat("x", maxStringLength) + 'y';
            final String expectedJson = '"' +
                    excessiveString.substring(0, maxStringLength) +
                    writer.getTruncatedStringSuffix() +
                    '"';
            final BiConsumer<StringBuilder, String> emitter = StringBuilder::append;
            final String actualJson =
                    writer.use(() -> writer.writeString(emitter, excessiveString));
            Assertions.assertThat(actualJson).isEqualTo(expectedJson);
            assertFormattableBufferReset(writer);
        });
    }

    @Test
    void test_writeString_emitter_excessive_string_ending_with_high_surrogate() {
        withLockedWriter(writer -> {
            final int maxStringLength = writer.getMaxStringLength();
            @SuppressWarnings("StringBufferReplaceableByString")
            final String excessiveString = new StringBuilder()
                    .append(Strings.repeat("x", maxStringLength - 1))
                    .append(HI_SURROGATE)
                    .append(LO_SURROGATE)
                    .toString();
            final String expectedJson = "\"" +
                    Strings.repeat("x", maxStringLength - 1) +
                    writer.getTruncatedStringSuffix() +
                    '"';
            final BiConsumer<StringBuilder, String> emitter = StringBuilder::append;
            final String actualJson =
                    writer.use(() -> writer.writeString(emitter, excessiveString));
            Assertions.assertThat(actualJson).isEqualTo(expectedJson);
            assertFormattableBufferReset(writer);
        });
    }

    @Test
    void test_writeString_null_formattable() {
        expectNull(writer -> writer.writeString((StringBuilderFormattable) null));
    }

    @Test
    void test_writeString_formattable() {
        final String expectedJson = "\"foo\\tbar\\tbuzz\"";
        final String actualJson = withLockedWriterReturning(writer ->
                writer.use(() ->
                        writer.writeString(stringBuilder ->
                                stringBuilder.append("foo\tbar\tbuzz"))));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    void test_writeString_formattable_excessive_string() {
        withLockedWriter(writer -> {
            final int maxStringLength = writer.getMaxStringLength();
            final String excessiveString = Strings.repeat("x", maxStringLength) + 'y';
            final String expectedJson = '"' +
                    excessiveString.substring(0, maxStringLength) +
                    writer.getTruncatedStringSuffix() +
                    '"';
            final String actualJson = writer.use(() ->
                    writer.writeString(stringBuilder ->
                            stringBuilder.append(excessiveString)));
            Assertions.assertThat(actualJson).isEqualTo(expectedJson);
            assertFormattableBufferReset(writer);
        });
    }

    @Test
    void test_writeString_formattable_excessive_string_ending_with_high_surrogate() {
        withLockedWriter(writer -> {
            final int maxStringLength = writer.getMaxStringLength();
            @SuppressWarnings("StringBufferReplaceableByString")
            final String excessiveString = new StringBuilder()
                    .append(Strings.repeat("x", maxStringLength - 1))
                    .append(HI_SURROGATE)
                    .append(LO_SURROGATE)
                    .toString();
            final String expectedJson = "\"" +
                    Strings.repeat("x", maxStringLength - 1) +
                    writer.getTruncatedStringSuffix() +
                    '"';
            final String actualJson = writer.use(() ->
                    writer.writeString(stringBuilder ->
                            stringBuilder.append(excessiveString)));
            Assertions.assertThat(actualJson).isEqualTo(expectedJson);
            assertFormattableBufferReset(writer);
        });
    }

    private static void assertFormattableBufferReset(final JsonWriter writer) {
        final StringBuilder formattableBuffer = getFormattableBuffer(writer);
        Assertions
                .assertThat(formattableBuffer.capacity())
                .isEqualTo(writer.getMaxStringLength());
        Assertions
                .assertThat(formattableBuffer.length())
                .isEqualTo(0);
    }

    private static StringBuilder getFormattableBuffer(final JsonWriter writer) {
        try {
            final Field field = JsonWriter.class.getDeclaredField("formattableBuffer");
            field.setAccessible(true);
            return (StringBuilder) field.get(writer);
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }

    @Test
    void test_writeString_null_seq_1() {
        expectNull(writer -> writer.writeString((CharSequence) null));
    }

    @Test
    void test_writeString_null_seq_2() {
        expectNull(writer -> writer.writeString((CharSequence) null, 0, 4));
    }

    @Test
    void test_writeString_seq_negative_offset() {
        withLockedWriter(writer -> Assertions
                .assertThatThrownBy(() ->
                        writer.use(() -> writer.writeString("a", -1, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("offset"));
    }

    @Test
    void test_writeString_seq_negative_length() {
        withLockedWriter(writer -> Assertions
                .assertThatThrownBy(() ->
                        writer.use(() -> writer.writeString("a", 0, -1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("length"));
    }

    @Test
    void test_writeString_excessive_seq() {
        withLockedWriter(writer -> {
            final CharSequence seq = Strings.repeat("x", writer.getMaxStringLength()) + 'y';
            final String expectedJson = "\"" +
                    Strings.repeat("x", writer.getMaxStringLength()) +
                    writer.getTruncatedStringSuffix() +
                    '"';
            final String actualJson = writer.use(() -> writer.writeString(seq));
            Assertions.assertThat(actualJson).isEqualTo(expectedJson);
        });
    }

    @Test
    void test_writeString_excessive_seq_ending_with_high_surrogate() {
        withLockedWriter(writer -> {
            final int maxStringLength = writer.getMaxStringLength();
            @SuppressWarnings("StringBufferReplaceableByString")
            final CharSequence seq = new StringBuilder()
                    .append(Strings.repeat("x", maxStringLength - 1))
                    .append(HI_SURROGATE)
                    .append(LO_SURROGATE)
                    .toString();
            final String expectedJson = "\"" +
                    Strings.repeat("x", maxStringLength - 1) +
                    writer.getTruncatedStringSuffix() +
                    '"';
            final String actualJson = writer.use(() -> writer.writeString(seq));
            Assertions.assertThat(actualJson).isEqualTo(expectedJson);
        });
    }

    @Test
    void test_writeString_seq() throws IOException {
        final char[] surrogates = new char[2];
        testQuoting((final Integer codePoint) -> {
            // noinspection ResultOfMethodCallIgnored
            Character.toChars(codePoint, surrogates, 0);
            final String s = new String(surrogates);
            return withLockedWriterReturning(writer ->
                    writer.use(() -> writer.writeString(s)));
        });
    }

    @Test
    void test_writeString_null_buffer_1() {
        expectNull(writer -> writer.writeString((char[]) null));
    }

    @Test
    void test_writeString_null_buffer_2() {
        expectNull(writer -> writer.writeString((char[]) null, 0, 4));
    }

    @Test
    void test_writeString_buffer_negative_offset() {
        withLockedWriter(writer -> Assertions
                .assertThatThrownBy(() ->
                        writer.use(() -> writer.writeString(new char[]{'a'}, -1, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("offset"));
    }

    @Test
    void test_writeString_buffer_negative_length() {
        withLockedWriter(writer -> Assertions
                .assertThatThrownBy(() ->
                        writer.use(() -> writer.writeString(new char[]{'a'}, 0, -1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("length"));
    }

    @Test
    void test_writeString_excessive_buffer() {
        withLockedWriter(writer -> {
            final char[] buffer =
                    (Strings.repeat("x", writer.getMaxStringLength()) + 'y')
                            .toCharArray();
            final String expectedJson = "\"" +
                    Strings.repeat("x", writer.getMaxStringLength()) +
                    writer.getTruncatedStringSuffix() +
                    '"';
            final String actualJson = writer.use(() -> writer.writeString(buffer));
            Assertions.assertThat(actualJson).isEqualTo(expectedJson);
        });
    }

    @Test
    void test_writerString_excessive_buffer_ending_with_high_surrogate() {
        withLockedWriter(writer -> {
            final int maxStringLength = writer.getMaxStringLength();
            @SuppressWarnings("StringBufferReplaceableByString")
            final char[] buffer = new StringBuilder()
                    .append(Strings.repeat("x", maxStringLength - 1))
                    .append(HI_SURROGATE)
                    .append(LO_SURROGATE)
                    .toString()
                    .toCharArray();
            final String expectedJson = "\"" +
                    Strings.repeat("x", maxStringLength - 1) +
                    writer.getTruncatedStringSuffix() +
                    '"';
            final String actualJson = writer.use(() -> writer.writeString(buffer));
            Assertions.assertThat(actualJson).isEqualTo(expectedJson);
        });
    }

    @Test
    void test_writeString_buffer() throws IOException {
        final char[] buffer = new char[2];
        testQuoting((final Integer codePoint) -> {
            // noinspection ResultOfMethodCallIgnored
            Character.toChars(codePoint, buffer, 0);
            return withLockedWriterReturning(writer ->
                    writer.use(() -> writer.writeString(buffer)));
        });
    }

    private static void testQuoting(
            final Function<Integer, String> quoter) throws IOException {
        final SoftAssertions assertions = new SoftAssertions();
        final char[] surrogates = new char[2];
        final Random random = new Random(0);
        for (int codePoint = Character.MIN_CODE_POINT;
             codePoint <= Character.MAX_CODE_POINT;
             // Incrementing randomly, since incrementing by one takes almost
             // two minutes for this test to finish.
             codePoint += Math.abs(random.nextInt(100))) {
            // noinspection ResultOfMethodCallIgnored
            Character.toChars(codePoint, surrogates, 0);
            final String s = new String(surrogates);
            final String expectedJson = JacksonFixture
                    .getObjectMapper()
                    .writeValueAsString(s);
            final String actualJson = quoter.apply(codePoint);
            assertions
                    .assertThat(actualJson)
                    .as("codePoint='%s' (%d)", s, codePoint)
                    .isEqualTo(expectedJson);
        }
        assertions.assertAll();
    }

    @Test
    void test_writeNumber_null_BigDecimal() {
        expectNull(writer -> writer.writeNumber((BigDecimal) null));
    }

    @Test
    void test_writeNumber_BigDecimal() {
        for (final BigDecimal number : new BigDecimal[]{
                BigDecimal.ZERO,
                BigDecimal.ONE,
                BigDecimal.TEN,
                new BigDecimal("" + Long.MAX_VALUE +
                        "" + Long.MAX_VALUE +
                        '.' + Long.MAX_VALUE +
                        "" + Long.MAX_VALUE)}) {
            final String expectedJson = String.valueOf(number);
            final String actualJson = withLockedWriterReturning(writer ->
                    writer.use(() -> writer.writeNumber(number)));
            Assertions.assertThat(actualJson).isEqualTo(expectedJson);
        }
    }

    @Test
    void test_writeNumber_null_BigInteger() {
        expectNull(writer -> writer.writeNumber((BigInteger) null));
    }

    @Test
    void test_writeNumber_BigInteger() {
        for (final BigInteger number : new BigInteger[]{
                BigInteger.ZERO,
                BigInteger.ONE,
                BigInteger.TEN,
                new BigInteger("" + Long.MAX_VALUE + "" + Long.MAX_VALUE)}) {
            final String expectedJson = String.valueOf(number);
            final String actualJson = withLockedWriterReturning(writer ->
                    writer.use(() -> writer.writeNumber(number)));
            Assertions.assertThat(actualJson).isEqualTo(expectedJson);
        }
    }

    @Test
    void test_writeNumber_float() {
        for (final float number : new float[]{Float.MIN_VALUE, -1.0F, 0F, 1.0F, Float.MAX_VALUE}) {
            final String expectedJson = String.valueOf(number);
            final String actualJson = withLockedWriterReturning(writer ->
                    writer.use(() -> writer.writeNumber(number)));
            Assertions.assertThat(actualJson).isEqualTo(expectedJson);
        }
    }

    @Test
    void test_writeNumber_double() {
        for (final double number : new double[]{Double.MIN_VALUE, -1.0D, 0D, 1.0D, Double.MAX_VALUE}) {
            final String expectedJson = String.valueOf(number);
            final String actualJson = withLockedWriterReturning(writer ->
                    writer.use(() -> writer.writeNumber(number)));
            Assertions.assertThat(actualJson).isEqualTo(expectedJson);
        }
    }

    @Test
    void test_writeNumber_short() {
        for (final short number : new short[]{Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE}) {
            final String expectedJson = String.valueOf(number);
            final String actualJson = withLockedWriterReturning(writer ->
                    writer.use(() -> writer.writeNumber(number)));
            Assertions.assertThat(actualJson).isEqualTo(expectedJson);
        }
    }

    @Test
    void test_writeNumber_int() {
        for (final int number : new int[]{Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE}) {
            final String expectedJson = String.valueOf(number);
            final String actualJson = withLockedWriterReturning(writer ->
                    writer.use(() -> writer.writeNumber(number)));
            Assertions.assertThat(actualJson).isEqualTo(expectedJson);
        }
    }

    @Test
    void test_writeNumber_long() {
        for (final long number : new long[]{Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE}) {
            final String expectedJson = String.valueOf(number);
            final String actualJson = withLockedWriterReturning(writer ->
                    writer.use(() -> writer.writeNumber(number)));
            Assertions.assertThat(actualJson).isEqualTo(expectedJson);
        }
    }

    @Test
    void test_writeNumber_integral_and_negative_fractional() {
        Assertions
                .assertThatThrownBy(() ->
                        withLockedWriter(writer ->
                                writer.use(() -> writer.writeNumber(0, -1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("was expecting a positive fraction: -1");
    }

    @Test
    void test_writeNumber_integral_and_zero_fractional() {
        final String expectedJson = "123";
        final String actualJson = withLockedWriterReturning(writer ->
                writer.use(() -> writer.writeNumber(123L, 0L)));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    void test_writeNumber_integral_and_fractional() {
        final String expectedJson = "123.456";
        final String actualJson = withLockedWriterReturning(writer ->
                writer.use(() -> writer.writeNumber(123L, 456L)));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    void test_writeBoolean_true() {
        final String expectedJson = "true";
        final String actualJson = withLockedWriterReturning(writer ->
                writer.use(() -> writer.writeBoolean(true)));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    void test_writeBoolean_false() {
        final String expectedJson = "false";
        final String actualJson = withLockedWriterReturning(writer ->
                writer.use(() -> writer.writeBoolean(false)));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    void test_writeNull() {
        expectNull(JsonWriter::writeNull);
    }

    private void expectNull(Consumer<JsonWriter> body) {
        final String expectedJson = "null";
        final String actualJson = withLockedWriterReturning(writer ->
                writer.use(() -> body.accept(writer)));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    void test_writeRawString_null_seq() {
        withLockedWriter(writer -> Assertions
                .assertThatThrownBy(() ->
                        writer.use(() ->
                                writer.writeRawString((String) null)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("seq"));
    }

    @Test
    void test_writeRawString_seq_negative_offset() {
        withLockedWriter(writer -> Assertions
                .assertThatThrownBy(() ->
                        writer.use(() ->
                                writer.writeRawString("a", -1, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("offset"));
    }

    @Test
    void test_writeRawString_seq_negative_length() {
        withLockedWriter(writer -> Assertions
                .assertThatThrownBy(() ->
                        writer.use(() ->
                                writer.writeRawString("a", 0, -1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("length"));
    }

    @Test
    void test_writeRawString_seq() {
        final String expectedJson = "this is not a valid JSON string";
        final String actualJson = withLockedWriterReturning(writer ->
                writer.use(() -> writer.writeRawString(expectedJson)));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    void test_writeRawString_null_buffer() {
        withLockedWriter(writer -> Assertions
                .assertThatThrownBy(() ->
                        writer.use(() ->
                                writer.writeRawString((char[]) null)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("buffer"));
    }

    @Test
    void test_writeRawString_buffer_negative_offset() {
        withLockedWriter(writer -> Assertions
                .assertThatThrownBy(() ->
                        writer.use(() ->
                                writer.writeRawString(new char[]{'a'}, -1, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("offset"));
    }

    @Test
    void test_writeRawString_buffer_negative_length() {
        withLockedWriter(writer -> Assertions
                .assertThatThrownBy(() ->
                        writer.use(() ->
                                writer.writeRawString(new char[]{'a'}, 0, -1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("length"));
    }

    @Test
    void test_writeRawString_buffer() {
        final String expectedJson = "this is not a valid JSON string";
        final String actualJson = withLockedWriterReturning(writer ->
                writer.use(() ->
                        writer.writeRawString(expectedJson.toCharArray())));
        Assertions.assertThat(actualJson).isEqualTo(expectedJson);
    }

}
