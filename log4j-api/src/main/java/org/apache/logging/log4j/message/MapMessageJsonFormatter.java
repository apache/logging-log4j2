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
package org.apache.logging.log4j.message;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.util.IndexedStringMap;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.apache.logging.log4j.util.StringBuilders;

/**
 * The default JSON formatter for {@link MapMessage}s.
 * <p>
 * The following types have specific handlers:
 * <p>
 * <ul>
 *     <li>{@link Map}
 *     <li>{@link Collection} ({@link List}, {@link Set}, etc.)
 *     <li>{@link Number} ({@link BigDecimal}, {@link Double}, {@link Long}, {@link Byte}, etc.)
 *     <li>{@link Boolean}
 *     <li>{@link StringBuilderFormattable}
 *     <li><tt>char/boolean/byte/short/int/long/float/double/Object</tt> arrays
 *     <li>{@link String}
 * </ul>
 * <p>
 * It supports nesting up to a maximum depth of 8, which is set by
 * <tt>log4j2.mapMessage.jsonFormatter.maxDepth</tt> property.
 */
enum MapMessageJsonFormatter {
    ;

    public static final int MAX_DEPTH = readMaxDepth();

    private static final char DQUOTE = '"';

    private static final char RBRACE = ']';

    private static final char LBRACE = '[';

    private static final char COMMA = ',';

    private static final char RCURLY = '}';

    private static final char LCURLY = '{';

    private static final char COLON = ':';

    private static int readMaxDepth() {
        final int maxDepth =
                PropertiesUtil.getProperties().getIntegerProperty("log4j2.mapMessage.jsonFormatter.maxDepth", 8);
        if (maxDepth < 0) {
            throw new IllegalArgumentException("was expecting a positive maxDepth, found: " + maxDepth);
        }
        return maxDepth;
    }

    static void format(final StringBuilder sb, final Object object) {
        format(sb, object, 0);
    }

    private static void format(final StringBuilder sb, final Object object, final int depth) {

        if (depth >= MAX_DEPTH) {
            throw new IllegalArgumentException("maxDepth has been exceeded");
        }

        // null
        if (object == null) {
            sb.append("null");
        }

        // map
        else if (object instanceof IndexedStringMap) {
            final IndexedStringMap map = (IndexedStringMap) object;
            formatIndexedStringMap(sb, map, depth);
        } else if (object instanceof Map) {
            @SuppressWarnings("unchecked")
            final Map<Object, Object> map = (Map<Object, Object>) object;
            formatMap(sb, map, depth);
        }

        // list & collection
        else if (object instanceof List) {
            @SuppressWarnings("unchecked")
            final List<Object> list = (List<Object>) object;
            formatList(sb, list, depth);
        } else if (object instanceof Collection) {
            @SuppressWarnings("unchecked")
            final Collection<Object> collection = (Collection<Object>) object;
            formatCollection(sb, collection, depth);
        }

        // number & boolean
        else if (object instanceof Number) {
            final Number number = (Number) object;
            formatNumber(sb, number);
        } else if (object instanceof Boolean) {
            final boolean booleanValue = (boolean) object;
            formatBoolean(sb, booleanValue);
        }

        // formattable
        else if (object instanceof StringBuilderFormattable) {
            final StringBuilderFormattable formattable = (StringBuilderFormattable) object;
            formatFormattable(sb, formattable);
        }

        // arrays
        else if (object instanceof char[]) {
            final char[] charValues = (char[]) object;
            formatCharArray(sb, charValues);
        } else if (object instanceof boolean[]) {
            final boolean[] booleanValues = (boolean[]) object;
            formatBooleanArray(sb, booleanValues);
        } else if (object instanceof byte[]) {
            final byte[] byteValues = (byte[]) object;
            formatByteArray(sb, byteValues);
        } else if (object instanceof short[]) {
            final short[] shortValues = (short[]) object;
            formatShortArray(sb, shortValues);
        } else if (object instanceof int[]) {
            final int[] intValues = (int[]) object;
            formatIntArray(sb, intValues);
        } else if (object instanceof long[]) {
            final long[] longValues = (long[]) object;
            formatLongArray(sb, longValues);
        } else if (object instanceof float[]) {
            final float[] floatValues = (float[]) object;
            formatFloatArray(sb, floatValues);
        } else if (object instanceof double[]) {
            final double[] doubleValues = (double[]) object;
            formatDoubleArray(sb, doubleValues);
        } else if (object instanceof Object[]) {
            final Object[] objectValues = (Object[]) object;
            formatObjectArray(sb, objectValues, depth);
        }

        // string
        else {
            formatString(sb, object);
        }
    }

    private static void formatIndexedStringMap(final StringBuilder sb, final IndexedStringMap map, final int depth) {
        sb.append(LCURLY);
        final int nextDepth = depth + 1;
        for (int entryIndex = 0; entryIndex < map.size(); entryIndex++) {
            final String key = map.getKeyAt(entryIndex);
            final Object value = map.getValueAt(entryIndex);
            if (entryIndex > 0) {
                sb.append(COMMA);
            }
            sb.append(DQUOTE);
            final int keyStartIndex = sb.length();
            sb.append(key);
            StringBuilders.escapeJson(sb, keyStartIndex);
            sb.append(DQUOTE).append(COLON);
            format(sb, value, nextDepth);
        }
        sb.append(RCURLY);
    }

    private static void formatMap(final StringBuilder sb, final Map<Object, Object> map, final int depth) {
        sb.append(LCURLY);
        final int nextDepth = depth + 1;
        final boolean[] firstEntry = {true};
        map.forEach((final Object key, final Object value) -> {
            if (key == null) {
                throw new IllegalArgumentException("null keys are not allowed");
            }
            if (firstEntry[0]) {
                firstEntry[0] = false;
            } else {
                sb.append(COMMA);
            }
            sb.append(DQUOTE);
            final String keyString = String.valueOf(key);
            final int keyStartIndex = sb.length();
            sb.append(keyString);
            StringBuilders.escapeJson(sb, keyStartIndex);
            sb.append(DQUOTE).append(COLON);
            format(sb, value, nextDepth);
        });
        sb.append(RCURLY);
    }

    private static void formatList(final StringBuilder sb, final List<Object> items, final int depth) {
        sb.append(LBRACE);
        final int nextDepth = depth + 1;
        for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
            if (itemIndex > 0) {
                sb.append(COMMA);
            }
            final Object item = items.get(itemIndex);
            format(sb, item, nextDepth);
        }
        sb.append(RBRACE);
    }

    private static void formatCollection(final StringBuilder sb, final Collection<Object> items, final int depth) {
        sb.append(LBRACE);
        final int nextDepth = depth + 1;
        final boolean[] firstItem = {true};
        items.forEach((final Object item) -> {
            if (firstItem[0]) {
                firstItem[0] = false;
            } else {
                sb.append(COMMA);
            }
            format(sb, item, nextDepth);
        });
        sb.append(RBRACE);
    }

    private static void formatNumber(final StringBuilder sb, final Number number) {
        if (number instanceof BigDecimal) {
            final BigDecimal decimalNumber = (BigDecimal) number;
            sb.append(decimalNumber.toString());
        } else if (number instanceof Double) {
            final double doubleNumber = (Double) number;
            sb.append(doubleNumber);
        } else if (number instanceof Float) {
            final float floatNumber = (float) number;
            sb.append(floatNumber);
        } else if (number instanceof Byte
                || number instanceof Short
                || number instanceof Integer
                || number instanceof Long) {
            final long longNumber = number.longValue();
            sb.append(longNumber);
        } else {
            final long longNumber = number.longValue();
            final double doubleValue = number.doubleValue();
            if (Double.compare((double) longNumber, doubleValue) == 0) {
                sb.append(longNumber);
            } else {
                sb.append(doubleValue);
            }
        }
    }

    private static void formatBoolean(final StringBuilder sb, final boolean booleanValue) {
        sb.append(booleanValue);
    }

    private static void formatFormattable(final StringBuilder sb, final StringBuilderFormattable formattable) {
        sb.append(DQUOTE);
        final int startIndex = sb.length();
        formattable.formatTo(sb);
        StringBuilders.escapeJson(sb, startIndex);
        sb.append(DQUOTE);
    }

    private static void formatCharArray(final StringBuilder sb, final char[] items) {
        sb.append(LBRACE);
        for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
            if (itemIndex > 0) {
                sb.append(COMMA);
            }
            final char item = items[itemIndex];
            sb.append(DQUOTE);
            final int startIndex = sb.length();
            sb.append(item);
            StringBuilders.escapeJson(sb, startIndex);
            sb.append(DQUOTE);
        }
        sb.append(RBRACE);
    }

    private static void formatBooleanArray(final StringBuilder sb, final boolean[] items) {
        sb.append(LBRACE);
        for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
            if (itemIndex > 0) {
                sb.append(COMMA);
            }
            final boolean item = items[itemIndex];
            sb.append(item);
        }
        sb.append(RBRACE);
    }

    private static void formatByteArray(final StringBuilder sb, final byte[] items) {
        sb.append(LBRACE);
        for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
            if (itemIndex > 0) {
                sb.append(COMMA);
            }
            final byte item = items[itemIndex];
            sb.append(item);
        }
        sb.append(RBRACE);
    }

    private static void formatShortArray(final StringBuilder sb, final short[] items) {
        sb.append(LBRACE);
        for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
            if (itemIndex > 0) {
                sb.append(COMMA);
            }
            final short item = items[itemIndex];
            sb.append(item);
        }
        sb.append(RBRACE);
    }

    private static void formatIntArray(final StringBuilder sb, final int[] items) {
        sb.append(LBRACE);
        for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
            if (itemIndex > 0) {
                sb.append(COMMA);
            }
            final int item = items[itemIndex];
            sb.append(item);
        }
        sb.append(RBRACE);
    }

    private static void formatLongArray(final StringBuilder sb, final long[] items) {
        sb.append(LBRACE);
        for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
            if (itemIndex > 0) {
                sb.append(COMMA);
            }
            final long item = items[itemIndex];
            sb.append(item);
        }
        sb.append(RBRACE);
    }

    private static void formatFloatArray(final StringBuilder sb, final float[] items) {
        sb.append(LBRACE);
        for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
            if (itemIndex > 0) {
                sb.append(COMMA);
            }
            final float item = items[itemIndex];
            sb.append(item);
        }
        sb.append(RBRACE);
    }

    private static void formatDoubleArray(final StringBuilder sb, final double[] items) {
        sb.append(LBRACE);
        for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
            if (itemIndex > 0) {
                sb.append(COMMA);
            }
            final double item = items[itemIndex];
            sb.append(item);
        }
        sb.append(RBRACE);
    }

    private static void formatObjectArray(final StringBuilder sb, final Object[] items, final int depth) {
        sb.append(LBRACE);
        final int nextDepth = depth + 1;
        for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
            if (itemIndex > 0) {
                sb.append(COMMA);
            }
            final Object item = items[itemIndex];
            format(sb, item, nextDepth);
        }
        sb.append(RBRACE);
    }

    private static void formatString(final StringBuilder sb, final Object value) {
        sb.append(DQUOTE);
        final int startIndex = sb.length();
        final String valueString = String.valueOf(value);
        sb.append(valueString);
        StringBuilders.escapeJson(sb, startIndex);
        sb.append(DQUOTE);
    }
}
