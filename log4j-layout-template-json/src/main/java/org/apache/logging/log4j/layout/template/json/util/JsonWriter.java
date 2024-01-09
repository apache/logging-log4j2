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
package org.apache.logging.log4j.layout.template.json.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import org.apache.logging.log4j.util.IndexedReadOnlyStringMap;
import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.apache.logging.log4j.util.StringMap;

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
 *     <li>arrays of primitive types
 *     <tt>char/boolean/byte/short/int/long/float/double</tt> and {@link Object}
 *     <li>{@link CharSequence} and <tt>char[]</tt> with necessary escaping
 * </ul>
 * <p>
 * JSON standard quoting routines are borrowed from
 * <a href="https://github.com/FasterXML/jackson-core">Jackson</a>.
 * <p>
 * Note that this class provides no protection against recursive collections,
 * e.g., an array where one or more elements reference to the array itself.
 */
public final class JsonWriter implements AutoCloseable, Cloneable {

    private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

    /**
     * Lookup table used for determining which output characters in 7-bit ASCII
     * range (i.e., first 128 Unicode code points, single-byte UTF-8 characters)
     * need to be quoted.
     * <p>
     * Value of 0 means "no escaping"; other positive values, that value is
     * character to use after backslash; and negative values, that generic
     * (backslash - u) escaping is to be used.
     */
    private static final int[] ESC_CODES;

    static {
        final int[] table = new int[128];
        // Control chars need generic escape sequence
        for (int i = 0; i < 32; ++i) {
            // 04-Mar-2011, tatu: Used to use "-(i + 1)", replaced with constant
            table[i] = -1;
        }
        // Others (and some within that range too) have explicit shorter sequences
        table['"'] = '"';
        table['\\'] = '\\';
        // Escaping of slash is optional, so let's not add it
        table[0x08] = 'b';
        table[0x09] = 't';
        table[0x0C] = 'f';
        table[0x0A] = 'n';
        table[0x0D] = 'r';
        ESC_CODES = table;
    }

    private final char[] quoteBuffer;

    private final StringBuilder stringBuilder;

    private final StringBuilder formattableBuffer;

    private final int maxStringLength;

    private final String truncatedStringSuffix;

    private final String quotedTruncatedStringSuffix;

    private JsonWriter(final Builder builder) {
        this.quoteBuffer = new char[] {'\\', '-', '0', '0', '-', '-'};
        this.stringBuilder = new StringBuilder(builder.maxStringLength);
        this.formattableBuffer = new StringBuilder(builder.maxStringLength);
        this.maxStringLength = builder.maxStringLength;
        this.truncatedStringSuffix = builder.truncatedStringSuffix;
        this.quotedTruncatedStringSuffix = quoteString(builder.truncatedStringSuffix);
    }

    private String quoteString(final String string) {
        final int startIndex = stringBuilder.length();
        quoteString(string, 0, string.length());
        final StringBuilder quotedStringBuilder = new StringBuilder();
        quotedStringBuilder.append(stringBuilder, startIndex, stringBuilder.length());
        final String quotedString = quotedStringBuilder.toString();
        stringBuilder.setLength(startIndex);
        return quotedString;
    }

    public String use(final Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        final int startIndex = stringBuilder.length();
        try {
            runnable.run();
            final StringBuilder sliceStringBuilder = new StringBuilder();
            sliceStringBuilder.append(stringBuilder, startIndex, stringBuilder.length());
            return sliceStringBuilder.toString();
        } finally {
            trimStringBuilder(stringBuilder, startIndex);
        }
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
            final String stringValue = value instanceof String ? (String) value : String.valueOf(value);
            writeString(stringValue);
        }
    }

    public void writeObject(final StringMap map) {
        if (map == null) {
            writeNull();
        } else {
            writeObjectStart();
            final boolean[] firstEntry = {true};
            map.forEach(this::writeStringMap, firstEntry);
            writeObjectEnd();
        }
    }

    private void writeStringMap(final String key, final Object value, final boolean[] firstEntry) {
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

    public void writeObjectKey(final CharSequence key) {
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
                stringBuilder.append('"');
                quoteString(items, itemIndex, 1);
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

    public <S> void writeString(final BiConsumer<StringBuilder, S> emitter, final S state) {
        Objects.requireNonNull(emitter, "emitter");
        stringBuilder.append('"');
        try {
            emitter.accept(formattableBuffer, state);
            final int length = formattableBuffer.length();
            // Handle max. string length complying input.
            if (length <= maxStringLength) {
                quoteString(formattableBuffer, 0, length);
            }
            // Handle max. string length violating input.
            else {
                quoteString(formattableBuffer, 0, maxStringLength);
                stringBuilder.append(quotedTruncatedStringSuffix);
            }
            stringBuilder.append('"');
        } finally {
            trimStringBuilder(formattableBuffer, 0);
        }
    }

    public void writeString(final StringBuilderFormattable formattable) {
        if (formattable == null) {
            writeNull();
        } else {
            stringBuilder.append('"');
            try {
                formattable.formatTo(formattableBuffer);
                final int length = formattableBuffer.length();
                // Handle max. string length complying input.
                if (length <= maxStringLength) {
                    quoteString(formattableBuffer, 0, length);
                }
                // Handle max. string length violating input.
                else {
                    quoteString(formattableBuffer, 0, maxStringLength);
                    stringBuilder.append(quotedTruncatedStringSuffix);
                }
                stringBuilder.append('"');
            } finally {
                trimStringBuilder(formattableBuffer, 0);
            }
        }
    }

    public void writeString(final CharSequence seq) {
        if (seq == null) {
            writeNull();
        } else {
            writeString(seq, 0, seq.length());
        }
    }

    public void writeString(final CharSequence seq, final int offset, final int length) {

        // Handle null input.
        if (seq == null) {
            writeNull();
            return;
        }

        // Check arguments.
        if (offset < 0) {
            throw new IllegalArgumentException("was expecting a positive offset: " + offset);
        }
        if (length < 0) {
            throw new IllegalArgumentException("was expecting a positive length: " + length);
        }

        stringBuilder.append('"');
        // Handle max. string length complying input.
        if (length <= maxStringLength) {
            quoteString(seq, offset, length);
        }
        // Handle max. string length violating input.
        else {
            quoteString(seq, offset, maxStringLength);
            stringBuilder.append(quotedTruncatedStringSuffix);
        }
        stringBuilder.append('"');
    }

    /**
     * Quote text contents using JSON standard quoting.
     */
    private void quoteString(final CharSequence seq, final int offset, final int length) {
        final int surrogateCorrection =
                length > 0 && Character.isHighSurrogate(seq.charAt(offset + length - 1)) ? -1 : 0;
        final int limit = offset + length + surrogateCorrection;
        int i = offset;
        outer:
        while (i < limit) {
            while (true) {
                final char c = seq.charAt(i);
                if (c < ESC_CODES.length && ESC_CODES[c] != 0) {
                    break;
                }
                stringBuilder.append(c);
                if (++i >= limit) {
                    break outer;
                }
            }
            final char d = seq.charAt(i++);
            final int escCode = ESC_CODES[d];
            final int quoteBufferLength = escCode < 0 ? quoteNumeric(d) : quoteNamed(escCode);
            stringBuilder.append(quoteBuffer, 0, quoteBufferLength);
        }
    }

    public void writeString(final char[] buffer) {
        if (buffer == null) {
            writeNull();
        } else {
            writeString(buffer, 0, buffer.length);
        }
    }

    public void writeString(final char[] buffer, final int offset, final int length) {

        // Handle null input.
        if (buffer == null) {
            writeNull();
            return;
        }

        // Check arguments.
        if (offset < 0) {
            throw new IllegalArgumentException("was expecting a positive offset: " + offset);
        }
        if (length < 0) {
            throw new IllegalArgumentException("was expecting a positive length: " + length);
        }

        stringBuilder.append('"');
        // Handle max. string length complying input.
        if (length <= maxStringLength) {
            quoteString(buffer, offset, length);
        }
        // Handle max. string length violating input.
        else {
            quoteString(buffer, offset, maxStringLength);
            stringBuilder.append(quotedTruncatedStringSuffix);
        }
        stringBuilder.append('"');
    }

    /**
     * Quote text contents using JSON standard quoting.
     */
    private void quoteString(final char[] buffer, final int offset, final int length) {
        final int surrogateCorrection = length > 0 && Character.isHighSurrogate(buffer[offset + length - 1]) ? -1 : 0;
        final int limit = offset + length + surrogateCorrection;
        int i = offset;
        outer:
        while (i < limit) {
            while (true) {
                final char c = buffer[i];
                if (c < ESC_CODES.length && ESC_CODES[c] != 0) {
                    break;
                }
                stringBuilder.append(c);
                if (++i >= limit) {
                    break outer;
                }
            }
            final char d = buffer[i++];
            final int escCode = ESC_CODES[d];
            final int quoteBufferLength = escCode < 0 ? quoteNumeric(d) : quoteNamed(escCode);
            stringBuilder.append(quoteBuffer, 0, quoteBufferLength);
        }
    }

    private int quoteNumeric(final int value) {
        quoteBuffer[1] = 'u';
        // We know it's a control char, so only the last 2 chars are non-0
        quoteBuffer[4] = HEX_CHARS[value >> 4];
        quoteBuffer[5] = HEX_CHARS[value & 0xF];
        return 6;
    }

    private int quoteNamed(final int esc) {
        quoteBuffer[1] = (char) esc;
        return 2;
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
        } else if (number instanceof Byte
                || number instanceof Short
                || number instanceof Integer
                || number instanceof Long) {
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
            throw new IllegalArgumentException("was expecting a positive fraction: " + fractionalPart);
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

    public void writeRawString(final CharSequence seq, final int offset, final int length) {

        // Check arguments.
        Objects.requireNonNull(seq, "seq");
        if (offset < 0) {
            throw new IllegalArgumentException("was expecting a positive offset: " + offset);
        }
        if (length < 0) {
            throw new IllegalArgumentException("was expecting a positive length: " + length);
        }

        // Write characters.
        final int limit = offset + length;
        stringBuilder.append(seq, offset, limit);
    }

    public void writeRawString(final char[] buffer) {
        Objects.requireNonNull(buffer, "buffer");
        writeRawString(buffer, 0, buffer.length);
    }

    public void writeRawString(final char[] buffer, final int offset, final int length) {

        // Check arguments.
        Objects.requireNonNull(buffer, "buffer");
        if (offset < 0) {
            throw new IllegalArgumentException("was expecting a positive offset: " + offset);
        }
        if (length < 0) {
            throw new IllegalArgumentException("was expecting a positive length: " + length);
        }

        // Write characters.
        stringBuilder.append(buffer, offset, length);
    }

    @Override
    public void close() {
        trimStringBuilder(stringBuilder, 0);
    }

    private void trimStringBuilder(final StringBuilder stringBuilder, final int length) {
        final int trimLength = Math.max(maxStringLength, length);
        if (stringBuilder.capacity() > trimLength) {
            stringBuilder.setLength(trimLength);
            stringBuilder.trimToSize();
        }
        stringBuilder.setLength(length);
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
            if (maxStringLength <= 0) {
                throw new IllegalArgumentException("was expecting maxStringLength > 0: " + maxStringLength);
            }
            Objects.requireNonNull(truncatedStringSuffix, "truncatedStringSuffix");
        }
    }
}
