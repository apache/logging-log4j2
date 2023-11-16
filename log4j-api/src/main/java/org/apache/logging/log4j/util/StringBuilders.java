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
package org.apache.logging.log4j.util;

import static java.lang.Character.toLowerCase;

import java.util.Map.Entry;

/**
 * <em>Consider this class private.</em>
 */
@InternalApi
public final class StringBuilders {

    private static final Class<?> timeClass;
    private static final Class<?> dateClass;

    static {
        Class<?> clazz;
        try {
            clazz = Class.forName("java.sql.Time");
        } catch (ClassNotFoundException ex) {
            clazz = null;
        }
        timeClass = clazz;

        try {
            clazz = Class.forName("java.sql.Date");
        } catch (ClassNotFoundException ex) {
            clazz = null;
        }
        dateClass = clazz;
    }

    private StringBuilders() {}

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
        return sb.append(key)
                .append(Chars.EQ)
                .append(Chars.DQUOTE)
                .append(value)
                .append(Chars.DQUOTE);
    }

    /**
     * Appends a text representation of the specified object to the specified StringBuilder,
     * if possible without allocating temporary objects.
     *
     * @param stringBuilder the StringBuilder to append the value to
     * @param obj the object whose text representation to append to the StringBuilder
     */
    public static void appendValue(final StringBuilder stringBuilder, final Object obj) {
        if (!appendSpecificTypes(stringBuilder, obj)) {
            stringBuilder.append(obj);
        }
    }

    public static boolean appendSpecificTypes(final StringBuilder stringBuilder, final Object obj) {
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
        } else if (obj instanceof Byte) {
            stringBuilder.append(((Byte) obj).byteValue());
        } else if (isTime(obj) || isDate(obj) || obj instanceof java.time.temporal.Temporal) {
            stringBuilder.append(obj);
        } else {
            return false;
        }
        return true;
    }

    /*
       Check to see if obj is an instance of java.sql.Time without requiring the java.sql module.
    */
    private static boolean isTime(final Object obj) {
        return timeClass != null && timeClass.isAssignableFrom(obj.getClass());
    }

    /*
        Check to see if obj is an instance of java.sql.Date without requiring the java.sql module.
    */
    private static boolean isDate(final Object obj) {
        return dateClass != null && dateClass.isAssignableFrom(obj.getClass());
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
    public static boolean equals(
            final CharSequence left,
            final int leftOffset,
            final int leftLength,
            final CharSequence right,
            final int rightOffset,
            final int rightLength) {
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
    public static boolean equalsIgnoreCase(
            final CharSequence left,
            final int leftOffset,
            final int leftLength,
            final CharSequence right,
            final int rightOffset,
            final int rightLength) {
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

    /**
     * Ensures that the char[] array of the specified StringBuilder does not exceed the specified number of characters.
     * This method is useful to ensure that excessively long char[] arrays are not kept in memory forever.
     *
     * @param stringBuilder the StringBuilder to check
     * @param maxSize the maximum number of characters the StringBuilder is allowed to have
     * @since 2.9
     */
    public static void trimToMaxSize(final StringBuilder stringBuilder, final int maxSize) {
        if (stringBuilder != null && stringBuilder.capacity() > maxSize) {
            stringBuilder.setLength(maxSize);
            stringBuilder.trimToSize();
        }
    }

    public static void escapeJson(final StringBuilder toAppendTo, final int start) {
        int escapeCount = 0;
        for (int i = start; i < toAppendTo.length(); i++) {
            final char c = toAppendTo.charAt(i);
            switch (c) {
                case '\b':
                case '\t':
                case '\f':
                case '\n':
                case '\r':
                case '"':
                case '\\':
                    escapeCount++;
                    break;
                default:
                    if (Character.isISOControl(c)) {
                        escapeCount += 5;
                    }
            }
        }

        final int lastChar = toAppendTo.length() - 1;
        toAppendTo.setLength(toAppendTo.length() + escapeCount);
        int lastPos = toAppendTo.length() - 1;

        for (int i = lastChar; lastPos > i; i--) {
            final char c = toAppendTo.charAt(i);
            switch (c) {
                case '\b':
                    lastPos = escapeAndDecrement(toAppendTo, lastPos, 'b');
                    break;

                case '\t':
                    lastPos = escapeAndDecrement(toAppendTo, lastPos, 't');
                    break;

                case '\f':
                    lastPos = escapeAndDecrement(toAppendTo, lastPos, 'f');
                    break;

                case '\n':
                    lastPos = escapeAndDecrement(toAppendTo, lastPos, 'n');
                    break;

                case '\r':
                    lastPos = escapeAndDecrement(toAppendTo, lastPos, 'r');
                    break;

                case '"':
                case '\\':
                    lastPos = escapeAndDecrement(toAppendTo, lastPos, c);
                    break;

                default:
                    if (Character.isISOControl(c)) {
                        // all iso control characters are in U+00xx, JSON output format is "\\u00XX"
                        toAppendTo.setCharAt(lastPos--, Chars.getUpperCaseHex(c & 0xF));
                        toAppendTo.setCharAt(lastPos--, Chars.getUpperCaseHex((c & 0xF0) >> 4));
                        toAppendTo.setCharAt(lastPos--, '0');
                        toAppendTo.setCharAt(lastPos--, '0');
                        toAppendTo.setCharAt(lastPos--, 'u');
                        toAppendTo.setCharAt(lastPos--, '\\');
                    } else {
                        toAppendTo.setCharAt(lastPos, c);
                        lastPos--;
                    }
            }
        }
    }

    private static int escapeAndDecrement(final StringBuilder toAppendTo, int lastPos, final char c) {
        toAppendTo.setCharAt(lastPos--, c);
        toAppendTo.setCharAt(lastPos--, '\\');
        return lastPos;
    }

    public static void escapeXml(final StringBuilder toAppendTo, final int start) {
        int escapeCount = 0;
        for (int i = start; i < toAppendTo.length(); i++) {
            final char c = toAppendTo.charAt(i);
            switch (c) {
                case '&':
                    escapeCount += 4;
                    break;
                case '<':
                case '>':
                    escapeCount += 3;
                    break;
                case '"':
                case '\'':
                    escapeCount += 5;
            }
        }

        final int lastChar = toAppendTo.length() - 1;
        toAppendTo.setLength(toAppendTo.length() + escapeCount);
        int lastPos = toAppendTo.length() - 1;

        for (int i = lastChar; lastPos > i; i--) {
            final char c = toAppendTo.charAt(i);
            switch (c) {
                case '&':
                    toAppendTo.setCharAt(lastPos--, ';');
                    toAppendTo.setCharAt(lastPos--, 'p');
                    toAppendTo.setCharAt(lastPos--, 'm');
                    toAppendTo.setCharAt(lastPos--, 'a');
                    toAppendTo.setCharAt(lastPos--, '&');
                    break;
                case '<':
                    toAppendTo.setCharAt(lastPos--, ';');
                    toAppendTo.setCharAt(lastPos--, 't');
                    toAppendTo.setCharAt(lastPos--, 'l');
                    toAppendTo.setCharAt(lastPos--, '&');
                    break;
                case '>':
                    toAppendTo.setCharAt(lastPos--, ';');
                    toAppendTo.setCharAt(lastPos--, 't');
                    toAppendTo.setCharAt(lastPos--, 'g');
                    toAppendTo.setCharAt(lastPos--, '&');
                    break;
                case '"':
                    toAppendTo.setCharAt(lastPos--, ';');
                    toAppendTo.setCharAt(lastPos--, 't');
                    toAppendTo.setCharAt(lastPos--, 'o');
                    toAppendTo.setCharAt(lastPos--, 'u');
                    toAppendTo.setCharAt(lastPos--, 'q');
                    toAppendTo.setCharAt(lastPos--, '&');
                    break;
                case '\'':
                    toAppendTo.setCharAt(lastPos--, ';');
                    toAppendTo.setCharAt(lastPos--, 's');
                    toAppendTo.setCharAt(lastPos--, 'o');
                    toAppendTo.setCharAt(lastPos--, 'p');
                    toAppendTo.setCharAt(lastPos--, 'a');
                    toAppendTo.setCharAt(lastPos--, '&');
                    break;
                default:
                    toAppendTo.setCharAt(lastPos--, c);
            }
        }
    }
}
