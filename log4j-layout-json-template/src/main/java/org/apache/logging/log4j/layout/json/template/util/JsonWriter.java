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

import org.apache.logging.log4j.util.IndexedReadOnlyStringMap;
import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.apache.logging.log4j.util.StringBuilders;
import org.apache.logging.log4j.util.StringMap;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A simple JSON writer with support for common Java data types.
 * <p>
 * The following types have specific handlers:
 * <p>
 * <ul>
 *     <li> <tt>null</tt> input
 *     <li>{@link Map}, {@link IndexedReadOnlyStringMap}, {@link StringMap}
 *     <li>{@link Collection} and {@link List}
 *     <li>{@link Number} ({@link BigDecimal}, {@link BigInteger}, {@link Float},
 *     {@link Double}, {@link Byte}, {@link Short}, {@link Integer}, and
 *     {@link Long})
 *     <li>{@link Boolean}
 *     <li>{@link StringBuilderFormattable}
 *     <li>arrays of primitve types
 *     <tt>char/boolean/byte/short/int/long/float/double</tt> and {@link Object}
 *     <li>{@link CharSequence} and <tt>char[]</tt> with necessary escaping
 * </ul>
 */
public final class JsonWriter implements AutoCloseable, Cloneable {

    private final StringBuilder stringBuilder;

    private final int maxStringLength;

    private final String truncatedStringSuffix;

    private final String escapedTruncatedStringSuffix;

    private JsonWriter(final Builder builder) {
        this.stringBuilder = new StringBuilder();
        this.maxStringLength = builder.maxStringLength;
        this.truncatedStringSuffix = builder.truncatedStringSuffix;
        this.escapedTruncatedStringSuffix = escapeString(builder.truncatedStringSuffix);
    }

    private static String escapeString(final String string) {
        final StringBuilder stringBuilder = new StringBuilder(string);
        StringBuilders.escapeJson(stringBuilder, 0);
        return stringBuilder.toString();
    }

    public String use(Runnable runnable) {
        final int startIndex = stringBuilder.length();
        runnable.run();
        final StringBuilder sliceStringBuilder = new StringBuilder();
        sliceStringBuilder.append(stringBuilder, startIndex, stringBuilder.length());
        stringBuilder.setLength(startIndex);
        return sliceStringBuilder.toString();
    }

    public StringBuilder getStringBuilder() {
        return stringBuilder;
    }

    public int getMaxStringLength() {
        return maxStringLength;
    }

    public String getTruncatedStringSuffix() {
        return truncatedStringSuffix;
    }

    public void writeValue(final Object value) {

        // null
        if (value == null) {
            writeNull();
        }

        // map
        else if (value instanceof IndexedReadOnlyStringMap) {
            final IndexedReadOnlyStringMap map = (IndexedReadOnlyStringMap) value;
            writeObject(map);
        } else if (value instanceof StringMap) {
            final StringMap map = (StringMap) value;
            writeObject(map);
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) value;
            writeObject(map);
        }

        // list & collection
        else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            final List<Object> list = (List<Object>) value;
            writeArray(list);
        } else if (value instanceof Collection) {
            @SuppressWarnings("unchecked")
            final Collection<Object> collection = (Collection<Object>) value;
            writeArray(collection);
        }

        // number & boolean
        else if (value instanceof Number) {
            final Number number = (Number) value;
            writeNumber(number);
        } else if (value instanceof Boolean) {
            final boolean booleanValue = (boolean) value;
            writeBoolean(booleanValue);
        }

        // formattable
        else if (value instanceof StringBuilderFormattable) {
            final StringBuilderFormattable formattable = (StringBuilderFormattable) value;
            writeString(formattable);
        }

        // arrays
        else if (value instanceof char[]) {
            final char[] charValues = (char[]) value;
            writeArray(charValues);
        } else if (value instanceof boolean[]) {
            final boolean[] booleanValues = (boolean[]) value;
            writeArray(booleanValues);
        } else if (value instanceof byte[]) {
            final byte[] byteValues = (byte[]) value;
            writeArray(byteValues);
        } else if (value instanceof short[]) {
            final short[] shortValues = (short[]) value;
            writeArray(shortValues);
        } else if (value instanceof int[]) {
            final int[] intValues = (int[]) value;
            writeArray(intValues);
        } else if (value instanceof long[]) {
            final long[] longValues = (long[]) value;
            writeArray(longValues);
        } else if (value instanceof float[]) {
            final float[] floatValues = (float[]) value;
            writeArray(floatValues);
        } else if (value instanceof double[]) {
            final double[] doubleValues = (double[]) value;
            writeArray(doubleValues);
        } else if (value instanceof Object[]) {
            final Object[] values = (Object[]) value;
            writeArray(values);
        }

        // string
        else {
            final String stringValue = value instanceof String
                    ? (String) value
                    : String.valueOf(value);
            writeString(stringValue);
        }

    }

    public void writeObject(final StringMap map) {
        if (map == null) {
            writeNull();
        } else {
            writeObjectStart();
            final boolean[] firstEntry = {true};
            map.forEach((final String key, final Object value) -> {
                if (key == null) {
                    throw new IllegalArgumentException("null keys are not allowed");
                }
                if (firstEntry[0]) {
                    firstEntry[0] = false;
                } else {
                    writeSeparator();
                }
                writeObjectKey(key);
                writeValue(value);
            });
            writeObjectEnd();
        }
    }

    public void writeObject(final IndexedReadOnlyStringMap map) {
        if (map == null) {
            writeNull();
        } else {
            writeObjectStart();
            for (int entryIndex = 0; entryIndex < map.size(); entryIndex++) {
                final String key = map.getKeyAt(entryIndex);
                final Object value = map.getValueAt(entryIndex);
                if (entryIndex > 0) {
                    writeSeparator();
                }
                writeObjectKey(key);
                writeValue(value);
            }
            writeObjectEnd();
        }
    }

    public void writeObject(final Map<String, Object> map) {
        if (map == null) {
            writeNull();
        } else {
            writeObjectStart();
            final boolean[] firstEntry = {true};
            map.forEach((final String key, final Object value) -> {
                if (key == null) {
                    throw new IllegalArgumentException("null keys are not allowed");
                }
                if (firstEntry[0]) {
                    firstEntry[0] = false;
                } else {
                    writeSeparator();
                }
                writeObjectKey(key);
                writeValue(value);
            });
            writeObjectEnd();
        }
    }

    public void writeObjectStart() {
        stringBuilder.append('{');
    }

    public void writeObjectEnd() {
        stringBuilder.append('}');
    }

    public void writeObjectKey(final String key) {
        writeString(key);
        stringBuilder.append(':');
    }

    public void writeArray(final List<Object> items) {
        if (items == null) {
            writeNull();
        } else {
            writeArrayStart();
            for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
                if (itemIndex > 0) {
                    writeSeparator();
                }
                final Object item = items.get(itemIndex);
                writeValue(item);
            }
            writeArrayEnd();
        }
    }

    public void writeArray(final Collection<Object> items) {
        if (items == null) {
            writeNull();
        } else {
            writeArrayStart();
            final boolean[] firstItem = {true};
            items.forEach((final Object item) -> {
                if (firstItem[0]) {
                    firstItem[0] = false;
                } else {
                    writeSeparator();
                }
                writeValue(item);
            });
            writeArrayEnd();
        }
    }

    public void writeArray(final char[] items) {
        if (items == null) {
            writeNull();
        } else {
            writeArrayStart();
            for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
                if (itemIndex > 0) {
                    writeSeparator();
                }
                final char item = items[itemIndex];
                stringBuilder.append('"');
                final int startIndex = stringBuilder.length();
                stringBuilder.append(item);
                StringBuilders.escapeJson(stringBuilder, startIndex);
                stringBuilder.append('"');
            }
            writeArrayEnd();
        }
    }

    public void writeArray(final boolean[] items) {
        if (items == null) {
            writeNull();
        } else {
            writeArrayStart();
            for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
                if (itemIndex > 0) {
                    writeSeparator();
                }
                final boolean item = items[itemIndex];
                writeBoolean(item);
            }
            writeArrayEnd();
        }
    }

    public void writeArray(final byte[] items) {
        if (items == null) {
            writeNull();
        } else {
            writeArrayStart();
            for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
                if (itemIndex > 0) {
                    writeSeparator();
                }
                final byte item = items[itemIndex];
                writeNumber(item);
            }
            writeArrayEnd();
        }
    }

    public void writeArray(final short[] items) {
        if (items == null) {
            writeNull();
        } else {
            writeArrayStart();
            for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
                if (itemIndex > 0) {
                    writeSeparator();
                }
                final short item = items[itemIndex];
                writeNumber(item);
            }
            writeArrayEnd();
        }
    }

    public void writeArray(final int[] items) {
        if (items == null) {
            writeNull();
        } else {
            writeArrayStart();
            for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
                if (itemIndex > 0) {
                    writeSeparator();
                }
                final int item = items[itemIndex];
                writeNumber(item);
            }
            writeArrayEnd();
        }
    }

    public void writeArray(final long[] items) {
        if (items == null) {
            writeNull();
        } else {
            writeArrayStart();
            for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
                if (itemIndex > 0) {
                    writeSeparator();
                }
                final long item = items[itemIndex];
                writeNumber(item);
            }
            writeArrayEnd();
        }
    }

    public void writeArray(final float[] items) {
        if (items == null) {
            writeNull();
        } else {
            writeArrayStart();
            for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
                if (itemIndex > 0) {
                    writeSeparator();
                }
                final float item = items[itemIndex];
                writeNumber(item);
            }
            writeArrayEnd();
        }
    }

    public void writeArray(final double[] items) {
        if (items == null) {
            writeNull();
        } else {
            writeArrayStart();
            for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
                if (itemIndex > 0) {
                    writeSeparator();
                }
                final double item = items[itemIndex];
                writeNumber(item);
            }
            writeArrayEnd();
        }
    }

    public void writeArray(final Object[] items) {
        if (items == null) {
            writeNull();
        } else {
            writeArrayStart();
            for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
                if (itemIndex > 0) {
                    writeSeparator();
                }
                final Object item = items[itemIndex];
                writeValue(item);
            }
            writeArrayEnd();
        }
    }

    public void writeArrayStart() {
        stringBuilder.append('[');
    }

    public void writeArrayEnd() {
        stringBuilder.append(']');
    }

    public void writeSeparator() {
        stringBuilder.append(',');
    }

    public void writeString(final StringBuilderFormattable formattable) {
        if (formattable == null) {
            writeNull();
        } else {
            stringBuilder.append('"');
            final int startIndex = stringBuilder.length();
            formattable.formatTo(stringBuilder);
            final int length = stringBuilder.length() - startIndex;
            if (length > 0) {
                if (length > maxStringLength) {
                    stringBuilder.setLength(startIndex + maxStringLength);
                    StringBuilders.escapeJson(stringBuilder, startIndex);
                    stringBuilder.append(escapedTruncatedStringSuffix);
                } else {
                    StringBuilders.escapeJson(stringBuilder, startIndex);
                }
            }
            stringBuilder.append('"');
        }
    }

    public void writeString(final CharSequence seq) {
        if (seq == null) {
            writeNull();
        } else {
            writeString(seq, 0, seq.length());
        }
    }

    public void writeString(
            final CharSequence seq,
            final int offset,
            final int length) {

        // Handle null input.
        if (seq == null) {
            writeNull();
            return;
        }

        // Check arguments.
        if (offset < 0) {
            throw new IllegalArgumentException(
                    "was expecting a positive offset: " + offset);
        }
        if (length < 0) {
            throw new IllegalArgumentException(
                    "was expecting a positive length: " + length);
        }

        stringBuilder.append('"');
        // Handle max. string length complying input.
        if (maxStringLength <= 0 || length <= maxStringLength) {
            final int limit = offset + length;
            final int escapeOffset = stringBuilder.length();
            stringBuilder.append(seq, offset, limit);
            StringBuilders.escapeJson(stringBuilder, escapeOffset);
        }
        // Handle max. string length violating input.
        else {
            final int limit = offset + maxStringLength;
            final int escapeOffset = stringBuilder.length();
            stringBuilder.append(seq, offset, limit);
            StringBuilders.escapeJson(stringBuilder, escapeOffset);
            stringBuilder.append(escapedTruncatedStringSuffix);
        }
        stringBuilder.append('"');

    }

    public void writeString(final char[] buffer) {
        if (buffer == null) {
            writeNull();
        } else {
            writeString(buffer, 0, buffer.length);
        }
    }

    public void writeString(
            final char[] buffer,
            final int offset,
            final int length) {

        // Handle null input.
        if (buffer == null) {
            writeNull();
            return;
        }

        // Check arguments.
        if (offset < 0) {
            throw new IllegalArgumentException(
                    "was expecting a positive offset: " + offset);
        }
        if (length < 0) {
            throw new IllegalArgumentException(
                    "was expecting a positive length: " + length);
        }

        stringBuilder.append('"');
        // Handle max. string length complying input.
        if (maxStringLength <= 0 || length <= maxStringLength) {
            final int limit = offset + length;
            final int escapeOffset = stringBuilder.length();
            stringBuilder.append(buffer, offset, limit);
            StringBuilders.escapeJson(stringBuilder, escapeOffset);
        }
        // Handle max. string length violating input.
        else {
            final int limit = offset + maxStringLength;
            final int escapeOffset = stringBuilder.length();
            stringBuilder.append(buffer, offset, limit);
            StringBuilders.escapeJson(stringBuilder, escapeOffset);
            stringBuilder.append(escapedTruncatedStringSuffix);
        }
        stringBuilder.append('"');

    }

    private void writeNumber(final Number number) {
        if (number instanceof BigDecimal) {
            final BigDecimal decimalNumber = (BigDecimal) number;
            writeNumber(decimalNumber);
        } else if (number instanceof BigInteger) {
            final BigInteger integerNumber = (BigInteger) number;
            writeNumber(integerNumber);
        } else if (number instanceof Double) {
            final double doubleNumber = (Double) number;
            writeNumber(doubleNumber);
        } else if (number instanceof Float) {
            final float floatNumber = (float) number;
            writeNumber(floatNumber);
        } else if (number instanceof Byte ||
                number instanceof Short ||
                number instanceof Integer ||
                number instanceof Long) {
            final long longNumber = number.longValue();
            writeNumber(longNumber);
        } else {
            final long longNumber = number.longValue();
            final double doubleValue = number.doubleValue();
            if (Double.compare(longNumber, doubleValue) == 0) {
                writeNumber(longNumber);
            } else {
                writeNumber(doubleValue);
            }
        }
    }

    public void writeNumber(final BigDecimal number) {
        if (number == null) {
            writeNull();
        } else {
            stringBuilder.append(number);
        }
    }

    public void writeNumber(final BigInteger number) {
        if (number == null) {
            writeNull();
        } else {
            stringBuilder.append(number);
        }
    }

    public void writeNumber(final float number) {
        stringBuilder.append(number);
    }

    public void writeNumber(final double number) {
        stringBuilder.append(number);
    }

    public void writeNumber(final short number) {
        stringBuilder.append(number);
    }

    public void writeNumber(final int number) {
        stringBuilder.append(number);
    }

    public void writeNumber(final long number) {
        stringBuilder.append(number);
    }

    public void writeNumber(final long integralPart, final long fractionalPart) {
        if (fractionalPart < 0) {
            throw new IllegalArgumentException(
                    "was expecting a positive fraction: " + fractionalPart);
        }
        stringBuilder.append(integralPart);
        if (fractionalPart != 0) {
            stringBuilder.append('.');
            stringBuilder.append(fractionalPart);
        }
    }

    public void writeBoolean(final boolean value) {
        writeRawString(value ? "true" : "false");
    }

    public void writeNull() {
        writeRawString("null");
    }

    public void writeRawString(final CharSequence seq) {
        Objects.requireNonNull(seq, "seq");
        writeRawString(seq, 0, seq.length());
    }

    public void writeRawString(
            final CharSequence seq,
            final int offset,
            final int length) {

        // Check arguments.
        Objects.requireNonNull(seq, "seq");
        if (offset < 0) {
            throw new IllegalArgumentException(
                    "was expecting a positive offset: " + offset);
        }
        if (length < 0) {
            throw new IllegalArgumentException(
                    "was expecting a positive length: " + length);
        }

        // Write characters.
        final int limit = offset + length;
        stringBuilder.append(seq, offset, limit);

    }

    public void writeRawString(final char[] buffer) {
        Objects.requireNonNull(buffer, "buffer");
        writeRawString(buffer, 0, buffer.length);
    }

    public void writeRawString(
            final char[] buffer,
            final int offset,
            final int length) {

        // Check arguments.
        Objects.requireNonNull(buffer, "buffer");
        if (offset < 0) {
            throw new IllegalArgumentException(
                    "was expecting a positive offset: " + offset);
        }
        if (length < 0) {
            throw new IllegalArgumentException(
                    "was expecting a positive length: " + length);
        }

        // Write characters.
        stringBuilder.append(buffer, offset, length);

    }

    @Override
    public void close() {
        stringBuilder.setLength(0);
    }

    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public JsonWriter clone() {
        final JsonWriter jsonWriter = newBuilder()
                .setMaxStringLength(maxStringLength)
                .setTruncatedStringSuffix(truncatedStringSuffix)
                .build();
        jsonWriter.stringBuilder.append(stringBuilder);
        return jsonWriter;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {

        private int maxStringLength;

        private String truncatedStringSuffix;

        public int getMaxStringLength() {
            return maxStringLength;
        }

        public Builder setMaxStringLength(final int maxStringLength) {
            this.maxStringLength = maxStringLength;
            return this;
        }

        public String getTruncatedStringSuffix() {
            return truncatedStringSuffix;
        }

        public Builder setTruncatedStringSuffix(final String truncatedStringSuffix) {
            this.truncatedStringSuffix = truncatedStringSuffix;
            return this;
        }

        public JsonWriter build() {
            validate();
            return new JsonWriter(this);
        }

        private void validate() {
            Objects.requireNonNull(truncatedStringSuffix, "truncatedStringSuffix");
        }

    }

}
