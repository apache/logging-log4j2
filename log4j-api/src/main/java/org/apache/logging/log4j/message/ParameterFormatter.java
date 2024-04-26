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

import java.io.Serializable;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.StringBuilders;

/**
 * Supports parameter formatting as used in ParameterizedMessage and ReusableParameterizedMessage.
 */
final class ParameterFormatter {
    /**
     * Prefix for recursion.
     */
    static final String RECURSION_PREFIX = "[...";
    /**
     * Suffix for recursion.
     */
    static final String RECURSION_SUFFIX = "...]";

    /**
     * Prefix for errors.
     */
    static final String ERROR_PREFIX = "[!!!";
    /**
     * Separator for errors.
     */
    static final String ERROR_SEPARATOR = "=>";
    /**
     * Separator for error messages.
     */
    static final String ERROR_MSG_SEPARATOR = ":";
    /**
     * Suffix for errors.
     */
    static final String ERROR_SUFFIX = "!!!]";

    private static final char DELIM_START = '{';
    private static final char DELIM_STOP = '}';
    private static final char ESCAPE_CHAR = '\\';

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").withZone(ZoneId.systemDefault());

    private static final Logger STATUS_LOGGER = StatusLogger.getLogger();

    private ParameterFormatter() {}

    /**
     * Analyzes – finds argument placeholder (i.e., {@literal "{}"}) occurrences, etc. – the given message pattern.
     * <p>
     * Only {@literal "{}"} strings are treated as argument placeholders.
     * Escaped or incomplete argument placeholders will be ignored.
     * Some invalid argument placeholder examples:
     * </p>
     * <pre>
     * { }
     * foo\{}
     * {bar
     * {buzz}
     * </pre>
     *
     * @param pattern a message pattern to be analyzed
     * @param argCount
     * The number of arguments to be formatted.
     * For instance, for a parametrized message containing 7 placeholders in the pattern and 4 arguments for formatting, analysis will only need to store the index of the first 4 placeholder characters.
     * A negative value indicates no limit.
     * @return the analysis result
     */
    static MessagePatternAnalysis analyzePattern(final String pattern, final int argCount) {
        MessagePatternAnalysis analysis = new MessagePatternAnalysis();
        analyzePattern(pattern, argCount, analysis);
        return analysis;
    }

    /**
     * Analyzes – finds argument placeholder (i.e., {@literal "{}"}) occurrences, etc. – the given message pattern.
     * <p>
     * Only {@literal "{}"} strings are treated as argument placeholders.
     * Escaped or incomplete argument placeholders will be ignored.
     * Some invalid argument placeholder examples:
     * </p>
     * <pre>
     * { }
     * foo\{}
     * {bar
     * {buzz}
     * </pre>
     *
     * @param pattern a message pattern to be analyzed
     * @param argCount
     * The number of arguments to be formatted.
     * For instance, for a parametrized message containing 7 placeholders in the pattern and 4 arguments for formatting, analysis will only need to store the index of the first 4 placeholder characters.
     * A negative value indicates no limit.
     * @param analysis an object to store the results
     */
    static void analyzePattern(final String pattern, final int argCount, final MessagePatternAnalysis analysis) {

        // Short-circuit if there is nothing interesting
        final int l;
        if (pattern == null || (l = pattern.length()) < 2) {
            analysis.placeholderCount = 0;
            return;
        }

        // Count `{}` occurrences that is not escaped, i.e., not `\`-prefixed
        boolean escaped = false;
        analysis.placeholderCount = 0;
        analysis.escapedCharFound = false;
        for (int i = 0; i < (l - 1); i++) {
            final char c = pattern.charAt(i);
            if (c == ESCAPE_CHAR) {
                analysis.escapedCharFound = true;
                escaped = !escaped;
            } else {
                if (escaped) {
                    escaped = false;
                } else if (c == DELIM_START && pattern.charAt(i + 1) == DELIM_STOP) {
                    if (argCount < 0 || analysis.placeholderCount < argCount) {
                        analysis.ensurePlaceholderCharIndicesCapacity(argCount);
                        analysis.placeholderCharIndices[analysis.placeholderCount++] = i++;
                    }
                    // `argCount` is exceeded, skip storing the index
                    else {
                        analysis.placeholderCount++;
                        i++;
                    }
                }
            }
        }
    }

    /**
     *See {@link #analyzePattern(String, int, MessagePatternAnalysis)}.
     *
     */
    static final class MessagePatternAnalysis implements Serializable {

        private static final long serialVersionUID = -5974082575968329887L;

        /**
         * The size of the {@link #placeholderCharIndices} buffer to be allocated if it is found to be null.
         */
        private static final int PLACEHOLDER_CHAR_INDEX_BUFFER_INITIAL_SIZE = 8;

        /**
         * The size {@link #placeholderCharIndices} buffer will be extended with if it has found to be insufficient.
         */
        private static final int PLACEHOLDER_CHAR_INDEX_BUFFER_SIZE_INCREMENT = 8;

        /**
         * The total number of argument placeholder occurrences.
         */
        int placeholderCount;

        /**
         * The array of indices pointing to the first character of the found argument placeholder occurrences.
         */
        int[] placeholderCharIndices;

        /**
         * Flag indicating if an escaped (i.e., `\`-prefixed) character is found.
         */
        boolean escapedCharFound;

        private void ensurePlaceholderCharIndicesCapacity(final int argCount) {

            // Initialize the index buffer, if necessary
            if (placeholderCharIndices == null) {
                final int length = Math.max(argCount, PLACEHOLDER_CHAR_INDEX_BUFFER_INITIAL_SIZE);
                placeholderCharIndices = new int[length];
            }

            // Extend the index buffer, if necessary
            else if (placeholderCount >= placeholderCharIndices.length) {
                final int newLength = argCount > 0
                        ? argCount
                        : Math.addExact(placeholderCharIndices.length, PLACEHOLDER_CHAR_INDEX_BUFFER_SIZE_INCREMENT);
                final int[] newPlaceholderCharIndices = new int[newLength];
                System.arraycopy(placeholderCharIndices, 0, newPlaceholderCharIndices, 0, placeholderCount);
                placeholderCharIndices = newPlaceholderCharIndices;
            }
        }
    }

    /**
     * Format the given pattern using provided arguments.
     *
     * @param pattern a formatting pattern
     * @param args arguments to be formatted
     * @return the formatted message
     * @throws IllegalArgumentException on invalid input
     */
    static String format(final String pattern, final Object[] args, int argCount) {
        final StringBuilder result = new StringBuilder();
        final MessagePatternAnalysis analysis = analyzePattern(pattern, argCount);
        formatMessage(result, pattern, args, argCount, analysis);
        return result.toString();
    }

    /**
     * Format the given pattern using provided arguments into the buffer pointed.
     *
     * @param buffer a buffer the formatted output will be written to
     * @param pattern a formatting pattern
     * @param args arguments to be formatted
     * @throws IllegalArgumentException on invalid input
     */
    static void formatMessage(
            final StringBuilder buffer,
            final String pattern,
            final Object[] args,
            final int argCount,
            final MessagePatternAnalysis analysis) {

        // Short-circuit if there is nothing interesting
        if (pattern == null || args == null || analysis.placeholderCount == 0) {
            buffer.append(pattern);
            return;
        }

        // #2380: check if the count of placeholder is not equal to the count of arguments
        if (analysis.placeholderCount != argCount) {
            final int noThrowableArgCount =
                    argCount < 1 ? 0 : argCount - ((args[argCount - 1] instanceof Throwable) ? 1 : 0);
            if (analysis.placeholderCount != noThrowableArgCount) {
                STATUS_LOGGER.warn(
                        "found {} argument placeholders, but provided {} for pattern `{}`",
                        analysis.placeholderCount,
                        argCount,
                        pattern);
            }
        }

        // Fast-path for patterns containing no escapes
        if (analysis.escapedCharFound) {
            formatMessageContainingEscapes(buffer, pattern, args, argCount, analysis);
        }

        // Slow-path for patterns containing escapes
        else {
            formatMessageContainingNoEscapes(buffer, pattern, args, argCount, analysis);
        }
    }

    private static void formatMessageContainingNoEscapes(
            final StringBuilder buffer,
            final String pattern,
            final Object[] args,
            final int argCount,
            final MessagePatternAnalysis analysis) {

        // Format each argument and the text preceding it
        int precedingTextStartIndex = 0;
        final int argLimit = Math.min(analysis.placeholderCount, argCount);
        for (int argIndex = 0; argIndex < argLimit; argIndex++) {
            final int placeholderCharIndex = analysis.placeholderCharIndices[argIndex];
            buffer.append(pattern, precedingTextStartIndex, placeholderCharIndex);
            recursiveDeepToString(args[argIndex], buffer);
            precedingTextStartIndex = placeholderCharIndex + 2;
        }

        // Format the last trailing text
        buffer.append(pattern, precedingTextStartIndex, pattern.length());
    }

    private static void formatMessageContainingEscapes(
            final StringBuilder buffer,
            final String pattern,
            final Object[] args,
            final int argCount,
            final MessagePatternAnalysis analysis) {

        // Format each argument and the text preceding it
        int precedingTextStartIndex = 0;
        final int argLimit = Math.min(analysis.placeholderCount, argCount);
        for (int argIndex = 0; argIndex < argLimit; argIndex++) {
            final int placeholderCharIndex = analysis.placeholderCharIndices[argIndex];
            copyMessagePatternContainingEscapes(buffer, pattern, precedingTextStartIndex, placeholderCharIndex);
            recursiveDeepToString(args[argIndex], buffer);
            precedingTextStartIndex = placeholderCharIndex + 2;
        }

        // Format the last trailing text
        copyMessagePatternContainingEscapes(buffer, pattern, precedingTextStartIndex, pattern.length());
    }

    private static void copyMessagePatternContainingEscapes(
            final StringBuilder buffer, final String pattern, final int startIndex, final int endIndex) {
        boolean escaped = false;
        int i = startIndex;
        for (; i < endIndex; i++) {
            final char c = pattern.charAt(i);
            if (c == ESCAPE_CHAR) {
                if (escaped) {
                    // Found an escaped `\`, skip appending it
                    escaped = false;
                } else {
                    escaped = true;
                    buffer.append(c);
                }
            } else {
                if (escaped) {
                    if (c == DELIM_START && pattern.charAt(i + 1) == DELIM_STOP) {
                        // Found an escaped placeholder, override the earlier appended `\`
                        buffer.setLength(buffer.length() - 1);
                        buffer.append("{}");
                        i++;
                    } else {
                        buffer.append(c);
                    }
                    escaped = false;
                } else {
                    buffer.append(c);
                }
            }
        }
    }

    /**
     * This method performs a deep toString of the given Object.
     * Primitive arrays are converted using their respective Arrays.toString methods while
     * special handling is implemented for "container types", i.e. Object[], Map and Collection because those could
     * contain themselves.
     * <p>
     * It should be noted that neither AbstractMap.toString() nor AbstractCollection.toString() implement such a
     * behavior. They only check if the container is directly contained in itself, but not if a contained container
     * contains the original one. Because of that, Arrays.toString(Object[]) isn't safe either.
     * Confusing? Just read the last paragraph again and check the respective toString() implementation.
     * </p>
     * <p>
     * This means, in effect, that logging would produce a usable output even if an ordinary System.out.println(o)
     * would produce a relatively hard-to-debug StackOverflowError.
     * </p>
     * @param o The object.
     * @return The String representation.
     */
    static String deepToString(final Object o) {
        if (o == null) {
            return null;
        }
        // Check special types to avoid unnecessary StringBuilder usage
        if (o instanceof String) {
            return (String) o;
        }
        if (o instanceof Integer) {
            return Integer.toString((Integer) o);
        }
        if (o instanceof Long) {
            return Long.toString((Long) o);
        }
        if (o instanceof Double) {
            return Double.toString((Double) o);
        }
        if (o instanceof Boolean) {
            return Boolean.toString((Boolean) o);
        }
        if (o instanceof Character) {
            return Character.toString((Character) o);
        }
        if (o instanceof Short) {
            return Short.toString((Short) o);
        }
        if (o instanceof Float) {
            return Float.toString((Float) o);
        }
        if (o instanceof Byte) {
            return Byte.toString((Byte) o);
        }
        final StringBuilder str = new StringBuilder();
        recursiveDeepToString(o, str);
        return str.toString();
    }

    /**
     * This method performs a deep {@code toString()} of the given {@code Object}.
     * <p>
     * Primitive arrays are converted using their respective {@code Arrays.toString()} methods, while
     * special handling is implemented for <i>container types</i>, i.e. {@code Object[]}, {@code Map} and {@code Collection},
     * because those could contain themselves.
     * <p>
     * It should be noted that neither {@code AbstractMap.toString()} nor {@code AbstractCollection.toString()} implement such a behavior.
     * They only check if the container is directly contained in itself, but not if a contained container contains the original one.
     * Because of that, {@code Arrays.toString(Object[])} isn't safe either.
     * Confusing? Just read the last paragraph again and check the respective {@code toString()} implementation.
     * <p>
     * This means, in effect, that logging would produce a usable output even if an ordinary {@code System.out.println(o)}
     * would produce a relatively hard-to-debug {@code StackOverflowError}.
     *
     * @param o      the {@code Object} to convert into a {@code String}
     * @param str    the {@code StringBuilder} that {@code o} will be appended to
     */
    static void recursiveDeepToString(final Object o, final StringBuilder str) {
        recursiveDeepToString(o, str, null);
    }

    /**
     * This method performs a deep {@code toString()} of the given {@code Object}.
     * <p>
     * Primitive arrays are converted using their respective {@code Arrays.toString()} methods, while
     * special handling is implemented for <i>container types</i>, i.e. {@code Object[]}, {@code Map} and {@code Collection},
     * because those could contain themselves.
     * <p>
     * {@code dejaVu} is used in case of those container types to prevent an endless recursion.
     * <p>
     * It should be noted that neither {@code AbstractMap.toString()} nor {@code AbstractCollection.toString()} implement such a behavior.
     * They only check if the container is directly contained in itself, but not if a contained container contains the original one.
     * Because of that, {@code Arrays.toString(Object[])} isn't safe either.
     * Confusing? Just read the last paragraph again and check the respective {@code toString()} implementation.
     * <p>
     * This means, in effect, that logging would produce a usable output even if an ordinary {@code System.out.println(o)}
     * would produce a relatively hard-to-debug {@code StackOverflowError}.
     *
     * @param o      the {@code Object} to convert into a {@code String}
     * @param str    the {@code StringBuilder} that {@code o} will be appended to
     * @param dejaVu a set of container objects directly or transitively containing {@code o}
     */
    private static void recursiveDeepToString(final Object o, final StringBuilder str, final Set<Object> dejaVu) {
        if (appendSpecialTypes(o, str)) {
            return;
        }
        if (isMaybeRecursive(o)) {
            appendPotentiallyRecursiveValue(o, str, dejaVu);
        } else {
            tryObjectToString(o, str);
        }
    }

    private static boolean appendSpecialTypes(final Object o, final StringBuilder str) {
        return StringBuilders.appendSpecificTypes(str, o) || appendDate(o, str);
    }

    private static boolean appendDate(final Object o, final StringBuilder str) {
        if (!(o instanceof Date)) {
            return false;
        }
        DATE_FORMATTER.formatTo(((Date) o).toInstant(), str);
        return true;
    }

    /**
     * Returns {@code true} if the specified object is an array, a Map or a Collection.
     */
    private static boolean isMaybeRecursive(final Object o) {
        return o.getClass().isArray() || o instanceof Map || o instanceof Collection;
    }

    private static void appendPotentiallyRecursiveValue(
            final Object o, final StringBuilder str, final Set<Object> dejaVu) {
        final Class<?> oClass = o.getClass();
        if (oClass.isArray()) {
            appendArray(o, str, dejaVu, oClass);
        } else if (o instanceof Map) {
            appendMap(o, str, dejaVu);
        } else if (o instanceof Collection) {
            appendCollection(o, str, dejaVu);
        } else {
            throw new IllegalArgumentException("was expecting a container, found " + oClass);
        }
    }

    private static void appendArray(
            final Object o, final StringBuilder str, final Set<Object> dejaVu, final Class<?> oClass) {
        if (oClass == byte[].class) {
            str.append(Arrays.toString((byte[]) o));
        } else if (oClass == short[].class) {
            str.append(Arrays.toString((short[]) o));
        } else if (oClass == int[].class) {
            str.append(Arrays.toString((int[]) o));
        } else if (oClass == long[].class) {
            str.append(Arrays.toString((long[]) o));
        } else if (oClass == float[].class) {
            str.append(Arrays.toString((float[]) o));
        } else if (oClass == double[].class) {
            str.append(Arrays.toString((double[]) o));
        } else if (oClass == boolean[].class) {
            str.append(Arrays.toString((boolean[]) o));
        } else if (oClass == char[].class) {
            str.append(Arrays.toString((char[]) o));
        } else {
            // special handling of container Object[]
            final Set<Object> effectiveDejaVu = getOrCreateDejaVu(dejaVu);
            final boolean seen = !effectiveDejaVu.add(o);
            if (seen) {
                final String id = identityToString(o);
                str.append(RECURSION_PREFIX).append(id).append(RECURSION_SUFFIX);
            } else {
                final Object[] oArray = (Object[]) o;
                str.append('[');
                boolean first = true;
                for (final Object current : oArray) {
                    if (first) {
                        first = false;
                    } else {
                        str.append(", ");
                    }
                    recursiveDeepToString(current, str, cloneDejaVu(effectiveDejaVu));
                }
                str.append(']');
            }
        }
    }

    /**
     * Specialized handler for {@link Map}s.
     */
    private static void appendMap(final Object o, final StringBuilder str, final Set<Object> dejaVu) {
        final Set<Object> effectiveDejaVu = getOrCreateDejaVu(dejaVu);
        final boolean seen = !effectiveDejaVu.add(o);
        if (seen) {
            final String id = identityToString(o);
            str.append(RECURSION_PREFIX).append(id).append(RECURSION_SUFFIX);
        } else {
            final Map<?, ?> oMap = (Map<?, ?>) o;
            str.append('{');
            boolean isFirst = true;
            for (final Map.Entry<?, ?> entry : oMap.entrySet()) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    str.append(", ");
                }
                final Object key = entry.getKey();
                final Object value = entry.getValue();
                recursiveDeepToString(key, str, cloneDejaVu(effectiveDejaVu));
                str.append('=');
                recursiveDeepToString(value, str, cloneDejaVu(effectiveDejaVu));
            }
            str.append('}');
        }
    }

    /**
     * Specialized handler for {@link Collection}s.
     */
    private static void appendCollection(final Object o, final StringBuilder str, final Set<Object> dejaVu) {
        final Set<Object> effectiveDejaVu = getOrCreateDejaVu(dejaVu);
        final boolean seen = !effectiveDejaVu.add(o);
        if (seen) {
            final String id = identityToString(o);
            str.append(RECURSION_PREFIX).append(id).append(RECURSION_SUFFIX);
        } else {
            final Collection<?> oCol = (Collection<?>) o;
            str.append('[');
            boolean isFirst = true;
            for (final Object anOCol : oCol) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    str.append(", ");
                }
                recursiveDeepToString(anOCol, str, cloneDejaVu(effectiveDejaVu));
            }
            str.append(']');
        }
    }

    private static Set<Object> getOrCreateDejaVu(final Set<Object> dejaVu) {
        return dejaVu == null ? createDejaVu() : dejaVu;
    }

    private static Set<Object> createDejaVu() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    private static Set<Object> cloneDejaVu(final Set<Object> dejaVu) {
        final Set<Object> clonedDejaVu = createDejaVu();
        clonedDejaVu.addAll(dejaVu);
        return clonedDejaVu;
    }

    private static void tryObjectToString(final Object o, final StringBuilder str) {
        // it's just some other Object, we can only use toString().
        try {
            str.append(o.toString());
        } catch (final Throwable t) {
            handleErrorInObjectToString(o, str, t);
        }
    }

    private static void handleErrorInObjectToString(final Object o, final StringBuilder str, final Throwable t) {
        str.append(ERROR_PREFIX);
        str.append(identityToString(o));
        str.append(ERROR_SEPARATOR);
        final String msg = t.getMessage();
        final String className = t.getClass().getName();
        str.append(className);
        if (!className.equals(msg)) {
            str.append(ERROR_MSG_SEPARATOR);
            str.append(msg);
        }
        str.append(ERROR_SUFFIX);
    }

    /**
     * This method returns the same as if Object.toString() would not have been
     * overridden in obj.
     * <p>
     * Note that this isn't 100% secure as collisions can always happen with hash codes.
     * </p>
     * <p>
     * Copied from Object.hashCode():
     * </p>
     * <blockquote>
     * As much as is reasonably practical, the hashCode method defined by
     * class {@code Object} does return distinct integers for distinct
     * objects. (This is typically implemented by converting the internal
     * address of the object into an integer, but this implementation
     * technique is not required by the Java&#8482; programming language.)
     * </blockquote>
     *
     * @param obj the Object that is to be converted into an identity string.
     * @return the identity string as also defined in Object.toString()
     */
    static String identityToString(final Object obj) {
        if (obj == null) {
            return null;
        }
        return obj.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(obj));
    }
}
