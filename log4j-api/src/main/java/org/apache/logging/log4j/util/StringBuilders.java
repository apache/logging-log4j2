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

import java.util.Map.Entry;

import static java.lang.Character.toLowerCase;

/**
 * <em>Consider this class private.</em>
 */
public final class StringBuilders {
    private StringBuilders() {
    }

    /**
     * Appends in the following format: double quoted value.
     *
     * @param sb a string builder
     * @param value a value
     * @return {@code "value"}
     */
    public static StringBuilder appendDqValue(final StringBuilder sb, final Object value) {
        return sb.append(Chars.DQUOTE).append(value).append(Chars.DQUOTE);
    }

    /**
     * Appends in the following format: key=double quoted value.
     *
     * @param sb a string builder
     * @param entry a map entry
     * @return {@code key="value"}
     */
    public static StringBuilder appendKeyDqValue(final StringBuilder sb, final Entry<String, String> entry) {
        return appendKeyDqValue(sb, entry.getKey(), entry.getValue());
    }

    /**
     * Appends in the following format: key=double quoted value.
     *
     * @param sb a string builder
     * @param key a key
     * @param value a value
     * @return the specified StringBuilder
     */
    public static StringBuilder appendKeyDqValue(final StringBuilder sb, final String key, final Object value) {
        return sb.append(key).append(Chars.EQ).append(Chars.DQUOTE).append(value).append(Chars.DQUOTE);
    }

    /**
     * Appends a text representation of the specified object to the specified StringBuilder,
     * if possible without allocating temporary objects.
     *
     * @param stringBuilder the StringBuilder to append the value to
     * @param obj the object whose text representation to append to the StringBuilder
     */
    public static void appendValue(final StringBuilder stringBuilder, final Object obj) {
        if (obj == null || obj instanceof String) {
            stringBuilder.append((String) obj);
        } else if (obj instanceof StringBuilderFormattable) {
            ((StringBuilderFormattable) obj).formatTo(stringBuilder);
        } else if (obj instanceof CharSequence) {
            stringBuilder.append((CharSequence) obj);
        } else if (obj instanceof Integer) { // LOG4J2-1437 unbox auto-boxed primitives to avoid calling toString()
            stringBuilder.append(((Integer) obj).intValue());
        } else if (obj instanceof Long) {
            stringBuilder.append(((Long) obj).longValue());
        } else if (obj instanceof Double) {
            stringBuilder.append(((Double) obj).doubleValue());
        } else if (obj instanceof Boolean) {
            stringBuilder.append(((Boolean) obj).booleanValue());
        } else if (obj instanceof Character) {
            stringBuilder.append(((Character) obj).charValue());
        } else if (obj instanceof Short) {
            stringBuilder.append(((Short) obj).shortValue());
        } else if (obj instanceof Float) {
            stringBuilder.append(((Float) obj).floatValue());
        } else {
            stringBuilder.append(obj);
        }
    }

    /**
     * Returns true if the specified section of the left CharSequence equals the specified section of the right
     * CharSequence.
     *
     * @param left the left CharSequence
     * @param leftOffset start index in the left CharSequence
     * @param leftLength length of the section in the left CharSequence
     * @param right the right CharSequence to compare a section of
     * @param rightOffset start index in the right CharSequence
     * @param rightLength length of the section in the right CharSequence
     * @return true if equal, false otherwise
     */
    public static boolean equals(final CharSequence left, final int leftOffset, final int leftLength,
                                    final CharSequence right, final int rightOffset, final int rightLength) {
        if (leftLength == rightLength) {
            for (int i = 0; i < rightLength; i++) {
                if (left.charAt(i + leftOffset) != right.charAt(i + rightOffset)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Returns true if the specified section of the left CharSequence equals, ignoring case, the specified section of
     * the right CharSequence.
     *
     * @param left the left CharSequence
     * @param leftOffset start index in the left CharSequence
     * @param leftLength length of the section in the left CharSequence
     * @param right the right CharSequence to compare a section of
     * @param rightOffset start index in the right CharSequence
     * @param rightLength length of the section in the right CharSequence
     * @return true if equal ignoring case, false otherwise
     */
    public static boolean equalsIgnoreCase(final CharSequence left, final int leftOffset, final int leftLength,
                                              final CharSequence right, final int rightOffset, final int rightLength) {
        if (leftLength == rightLength) {
            for (int i = 0; i < rightLength; i++) {
                if (toLowerCase(left.charAt(i + leftOffset)) != toLowerCase(right.charAt(i + rightOffset))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
