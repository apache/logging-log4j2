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
package org.apache.logging.log4j.perf.nogc;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.message.Message;

/**
 * Handles messages that consist of a format string containing '{}' to represent each replaceable token, and
 * the parameters.
 * <p>
 * This class was originally written for <a href="http://lilithapp.com/">Lilith</a> by Joern Huxhorn where it is
 * licensed under the LGPL. It has been relicensed here with his permission providing that this attribution remain.
 * </p>
 */
public class ParameterizedMessage implements Message {

    /**
     * Prefix for recursion.
     */
    public static final String RECURSION_PREFIX = "[...";
    /**
     * Suffix for recursion.
     */
    public static final String RECURSION_SUFFIX = "...]";

    /**
     * Prefix for errors.
     */
    public static final String ERROR_PREFIX = "[!!!";
    /**
     * Separator for errors.
     */
    public static final String ERROR_SEPARATOR = "=>";
    /**
     * Separator for error messages.
     */
    public static final String ERROR_MSG_SEPARATOR = ":";
    /**
     * Suffix for errors.
     */
    public static final String ERROR_SUFFIX = "!!!]";

    private static final long serialVersionUID = -665975803997290697L;

    private static final int HASHVAL = 31;

    private static final char DELIM_START = '{';
    private static final char DELIM_STOP = '}';
    private static final char ESCAPE_CHAR = '\\';

    private final String messagePattern;
    private final String[] stringArgs;
    private transient Object[] argArray;
    private transient String formattedMessage;
    private transient Throwable throwable;

    /**
     * Creates a parameterized message.
     * @param messagePattern The message "format" string. This will be a String containing "{}" placeholders
     * where parameters should be substituted.
     * @param stringArgs The arguments for substitution.
     * @param throwable A Throwable.
     */
    public ParameterizedMessage(final String messagePattern, final String[] stringArgs, final Throwable throwable) {
        this.messagePattern = messagePattern;
        this.stringArgs = stringArgs;
        this.throwable = throwable;
    }

    /**
     * Creates a parameterized message.
     * @param messagePattern The message "format" string. This will be a String containing "{}" placeholders
     * where parameters should be substituted.
     * @param objectArgs The arguments for substitution.
     * @param throwable A Throwable.
     */
    public ParameterizedMessage(final String messagePattern, final Object[] objectArgs, final Throwable throwable) {
        this.messagePattern = messagePattern;
        this.throwable = throwable;
        this.stringArgs = argumentsToStrings(objectArgs);
    }

    /**
     * Constructs a ParameterizedMessage which contains the arguments converted to String as well as an optional
     * Throwable.
     *
     * <p>If the last argument is a Throwable and is NOT used up by a placeholder in the message pattern it is returned
     * in {@link #getThrowable()} and won't be contained in the created String[].
     * If it is used up {@link #getThrowable()} will return null even if the last argument was a Throwable!</p>
     *
     * @param messagePattern the message pattern that to be checked for placeholders.
     * @param arguments      the argument array to be converted.
     */
    public ParameterizedMessage(final String messagePattern, final Object[] arguments) {
        this.messagePattern = messagePattern;
        this.stringArgs = argumentsToStrings(arguments);
    }

    /**
     * Constructor with a pattern and a single parameter.
     * @param messagePattern The message pattern.
     * @param arg The parameter.
     */
    public ParameterizedMessage(final String messagePattern, final Object arg) {
        this(messagePattern, new Object[]{arg});
    }

    /**
     * Constructor with a pattern and two parameters.
     * @param messagePattern The message pattern.
     * @param arg1 The first parameter.
     * @param arg2 The second parameter.
     */
    public ParameterizedMessage(final String messagePattern, final Object arg1, final Object arg2) {
        this(messagePattern, new Object[]{arg1, arg2});
    }

    private String[] argumentsToStrings(final Object[] arguments) {
        if (arguments == null) {
            return null;
        }
        final int argsCount = countArgumentPlaceholders(messagePattern);
        int resultArgCount = arguments.length;
        if (argsCount < arguments.length && throwable == null && arguments[arguments.length - 1] instanceof Throwable) {
            throwable = (Throwable) arguments[arguments.length - 1];
            resultArgCount--;
        }
        argArray = new Object[resultArgCount];
        System.arraycopy(arguments, 0, argArray, 0, resultArgCount);

        String[] strArgs;
        if (argsCount == 1 && throwable == null && arguments.length > 1) {
            // special case
            strArgs = new String[1];
            strArgs[0] = deepToString(arguments);
        } else {
            strArgs = new String[resultArgCount];
            for (int i = 0; i < strArgs.length; i++) {
                strArgs[i] = deepToString(arguments[i]);
            }
        }
        return strArgs;
    }

    /**
     * Returns the formatted message.
     * @return the formatted message.
     */
    @Override
    public String getFormattedMessage() {
        if (formattedMessage == null) {
            formattedMessage = formatMessage(messagePattern, stringArgs);
        }
        return formattedMessage;
    }

    /**
     * Returns the message pattern.
     * @return the message pattern.
     */
    @Override
    public String getFormat() {
        return messagePattern;
    }

    /**
     * Returns the message parameters.
     * @return the message parameters.
     */
    @Override
    public Object[] getParameters() {
        if (argArray != null) {
            return argArray;
        }
        return stringArgs;
    }

    /**
     * Returns the Throwable that was given as the last argument, if any.
     * It will not survive serialization. The Throwable exists as part of the message
     * primarily so that it can be extracted from the end of the list of parameters
     * and then be added to the LogEvent. As such, the Throwable in the event should
     * not be used once the LogEvent has been constructed.
     *
     * @return the Throwable, if any.
     */
    @Override
    public Throwable getThrowable() {
        return throwable;
    }

    protected String formatMessage(final String msgPattern, final String[] sArgs) {
        return formatStringArgs(msgPattern, sArgs);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ParameterizedMessage that = (ParameterizedMessage) o;

        if (messagePattern != null ? !messagePattern.equals(that.messagePattern) : that.messagePattern != null) {
            return false;
        }
        if (!Arrays.equals(stringArgs, that.stringArgs)) {
            return false;
        }
        //if (throwable != null ? !throwable.equals(that.throwable) : that.throwable != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = messagePattern != null ? messagePattern.hashCode() : 0;
        result = HASHVAL * result + (stringArgs != null ? Arrays.hashCode(stringArgs) : 0);
        return result;
    }

    /**
     * Replace placeholders in the given messagePattern with arguments.
     *
     * @param messagePattern the message pattern containing placeholders.
     * @param arguments      the arguments to be used to replace placeholders.
     * @return the formatted message.
     */
    public static String format(final String messagePattern, final Object[] arguments) {
        if (messagePattern == null || arguments == null || arguments.length == 0) {
            return messagePattern;
        }
        if (arguments instanceof String[]) {
            return formatStringArgs(messagePattern, (String[]) arguments);
        }
        final String[] stringArgs = new String[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            stringArgs[i] = String.valueOf(arguments[i]);
        }
        return formatStringArgs(messagePattern, stringArgs);
    }

    /**
     * Replace placeholders in the given messagePattern with arguments.
     * <p>
     * Package protected for unit tests.
     *
     * @param messagePattern the message pattern containing placeholders.
     * @param arguments      the arguments to be used to replace placeholders.
     * @return the formatted message.
     */
    // Profiling showed this method is important to log4j performance. Modify with care!
    // 33 bytes (allows immediate JVM inlining: < 35 bytes) LOG4J2-1096
    static String formatStringArgs(final String messagePattern, final String[] arguments) {
        int len = 0;
        if (messagePattern == null || (len = messagePattern.length()) == 0 || arguments == null
                || arguments.length == 0) {
            return messagePattern;
        }

        return formatStringArgs0(messagePattern, len, arguments);
    }

    // Profiling showed this method is important to log4j performance. Modify with care!
    // 157 bytes (will be inlined when hot enough: < 325 bytes)
    private static String formatStringArgs0(final String messagePattern, final int len, final String[] arguments) {
        final char[] result = new char[len + sumStringLengths(arguments)];
        int pos = 0;
        int escapeCounter = 0;
        int currentArgument = 0;
        int i = 0;
        for (; i < len - 1; i++) { // last char is excluded from the loop
            final char curChar = messagePattern.charAt(i);
            if (curChar == ESCAPE_CHAR) {
                escapeCounter++;
            } else {
                if (isDelimPair(curChar, messagePattern, i)) { // looks ahead one char
                    i++;

                    // write escaped escape chars
                    pos = writeEscapedEscapeChars(escapeCounter, result, pos);

                    if (isOdd(escapeCounter)) {
                        // i.e. escaped
                        // write escaped escape chars
                        pos = writeDelimPair(result, pos);
                    } else {
                        // unescaped
                        pos = writeArgOrDelimPair(arguments, currentArgument, result, pos);
                        currentArgument++;
                    }
                } else {
                    pos = handleLiteralChar(result, pos, escapeCounter, curChar);
                }
                escapeCounter = 0;
            }
        }
        pos = handleRemainingCharIfAny(messagePattern, len, result, pos, escapeCounter, i);
        return new String(result, 0, pos);
    }

    /**
     * Returns the sum of the lengths of all Strings in the specified array.
     */
    // Profiling showed this method is important to log4j performance. Modify with care!
    // 30 bytes (allows immediate JVM inlining: < 35 bytes) LOG4J2-1096
    private static int sumStringLengths(final String[] arguments) {
        int result = 0;
        for (int i = 0; i < arguments.length; i++) {
            result += String.valueOf(arguments[i]).length();
        }
        return result;
    }

    /**
     * Returns {@code true} if the specified char and the char at {@code curCharIndex + 1} in the specified message
     * pattern together form a "{}" delimiter pair, returns {@code false} otherwise.
     */
    // Profiling showed this method is important to log4j performance. Modify with care!
    // 22 bytes (allows immediate JVM inlining: < 35 bytes) LOG4J2-1096
    private static boolean isDelimPair(final char curChar, final String messagePattern, final int curCharIndex) {
        return curChar == DELIM_START && messagePattern.charAt(curCharIndex + 1) == DELIM_STOP;
    }

    /**
     * Detects whether the message pattern has been fully processed or if an unprocessed character remains and processes
     * it if necessary, returning the resulting position in the result char array.
     */
    // Profiling showed this method is important to log4j performance. Modify with care!
    // 28 bytes (allows immediate JVM inlining: < 35 bytes) LOG4J2-1096
    private static int handleRemainingCharIfAny(final String messagePattern, final int len, final char[] result,
            int pos, final int escapeCounter, final int i) {
        if (i == len - 1) {
            final char curChar = messagePattern.charAt(i);
            pos = handleLastChar(result, pos, escapeCounter, curChar);
        }
        return pos;
    }

    /**
     * Processes the last unprocessed character and returns the resulting position in the result char array.
     */
    // Profiling showed this method is important to log4j performance. Modify with care!
    // 28 bytes (allows immediate JVM inlining: < 35 bytes) LOG4J2-1096
    private static int handleLastChar(final char[] result, int pos, final int escapeCounter, final char curChar) {
        if (curChar == ESCAPE_CHAR) {
            pos = writeUnescapedEscapeChars(escapeCounter + 1, result, pos);
        } else {
            pos = handleLiteralChar(result, pos, escapeCounter, curChar);
        }
        return pos;
    }

    /**
     * Processes a literal char (neither an '\' escape char nor a "{}" delimiter pair) and returns the resulting
     * position.
     */
    // Profiling showed this method is important to log4j performance. Modify with care!
    // 16 bytes (allows immediate JVM inlining: < 35 bytes) LOG4J2-1096
    private static int handleLiteralChar(final char[] result, int pos, final int escapeCounter, final char curChar) {
        // any other char beside ESCAPE or DELIM_START/STOP-combo
        // write unescaped escape chars
        pos = writeUnescapedEscapeChars(escapeCounter, result, pos);
        result[pos++] = curChar;
        return pos;
    }

    /**
     * Writes "{}" to the specified result array at the specified position and returns the resulting position.
     */
    // Profiling showed this method is important to log4j performance. Modify with care!
    // 18 bytes (allows immediate JVM inlining: < 35 bytes) LOG4J2-1096
    private static int writeDelimPair(final char[] result, int pos) {
        result[pos++] = DELIM_START;
        result[pos++] = DELIM_STOP;
        return pos;
    }

    /**
     * Returns {@code true} if the specified parameter is odd.
     */
    // Profiling showed this method is important to log4j performance. Modify with care!
    // 11 bytes (allows immediate JVM inlining: < 35 bytes) LOG4J2-1096
    private static boolean isOdd(final int number) {
        return (number & 1) == 1;
    }

    /**
     * Writes a '\' char to the specified result array (starting at the specified position) for each <em>pair</em> of
     * '\' escape chars encountered in the message format and returns the resulting position.
     */
    // Profiling showed this method is important to log4j performance. Modify with care!
    // 11 bytes (allows immediate JVM inlining: < 35 bytes) LOG4J2-1096
    private static int writeEscapedEscapeChars(final int escapeCounter, final char[] result, final int pos) {
        final int escapedEscapes = escapeCounter >> 1; // divide by two
        return writeUnescapedEscapeChars(escapedEscapes, result, pos);
    }

    /**
     * Writes the specified number of '\' chars to the specified result array (starting at the specified position) and
     * returns the resulting position.
     */
    // Profiling showed this method is important to log4j performance. Modify with care!
    // 20 bytes (allows immediate JVM inlining: < 35 bytes) LOG4J2-1096
    private static int writeUnescapedEscapeChars(int escapeCounter, final char[] result, int pos) {
        while (escapeCounter > 0) {
            result[pos++] = ESCAPE_CHAR;
            escapeCounter--;
        }
        return pos;
    }

    /**
     * Appends the argument at the specified argument index (or, if no such argument exists, the "{}" delimiter pair) to
     * the specified result char array at the specified position and returns the resulting position.
     */
    // Profiling showed this method is important to log4j performance. Modify with care!
    // 25 bytes (allows immediate JVM inlining: < 35 bytes) LOG4J2-1096
    private static int writeArgOrDelimPair(final String[] arguments, final int currentArgument, final char[] result,
            int pos) {
        if (currentArgument < arguments.length) {
            pos = writeArgAt0(arguments, currentArgument, result, pos);
        } else {
            pos = writeDelimPair(result, pos);
        }
        return pos;
    }

    /**
     * Appends the argument at the specified argument index to the specified result char array at the specified position
     * and returns the resulting position.
     */
    // Profiling showed this method is important to log4j performance. Modify with care!
    // 30 bytes (allows immediate JVM inlining: < 35 bytes) LOG4J2-1096
    private static int writeArgAt0(final String[] arguments, final int currentArgument, final char[] result,
            final int pos) {
        final String arg = String.valueOf(arguments[currentArgument]);
        final int argLen = arg.length();
        arg.getChars(0, argLen, result, pos);
        return pos + argLen;
    }

    /**
     * Counts the number of unescaped placeholders in the given messagePattern.
     *
     * @param messagePattern the message pattern to be analyzed.
     * @return the number of unescaped placeholders.
     */
    public static int countArgumentPlaceholders(final String messagePattern) {
        if (messagePattern == null) {
            return 0;
        }

        final int delim = messagePattern.indexOf(DELIM_START);

        if (delim == -1) {
            // special case, no placeholders at all.
            return 0;
        }
        int result = 0;
        boolean isEscaped = false;
        for (int i = 0; i < messagePattern.length(); i++) {
            final char curChar = messagePattern.charAt(i);
            if (curChar == ESCAPE_CHAR) {
                isEscaped = !isEscaped;
            } else if (curChar == DELIM_START) {
                if (!isEscaped && i < messagePattern.length() - 1 && messagePattern.charAt(i + 1) == DELIM_STOP) {
                    result++;
                    i++;
                }
                isEscaped = false;
            } else {
                isEscaped = false;
            }
        }
        return result;
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
    public static String deepToString(final Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof String) {
            return (String) o;
        }
        final StringBuilder str = new StringBuilder();
        final Set<String> dejaVu = new HashSet<>(); // that's actually a neat name ;)
        recursiveDeepToString(o, str, dejaVu);
        return str.toString();
    }

    /**
     * This method performs a deep toString of the given Object.
     * Primitive arrays are converted using their respective Arrays.toString methods while
     * special handling is implemented for "container types", i.e. Object[], Map and Collection because those could
     * contain themselves.
     * <p>
     * dejaVu is used in case of those container types to prevent an endless recursion.
     * </p>
     * <p>
     * It should be noted that neither AbstractMap.toString() nor AbstractCollection.toString() implement such a
     * behavior.
     * They only check if the container is directly contained in itself, but not if a contained container contains the
     * original one. Because of that, Arrays.toString(Object[]) isn't safe either.
     * Confusing? Just read the last paragraph again and check the respective toString() implementation.
     * </p>
     * <p>
     * This means, in effect, that logging would produce a usable output even if an ordinary System.out.println(o)
     * would produce a relatively hard-to-debug StackOverflowError.
     * </p>
     *
     * @param o      the Object to convert into a String
     * @param str    the StringBuilder that o will be appended to
     * @param dejaVu a list of container identities that were already used.
     */
    private static void recursiveDeepToString(final Object o, final StringBuilder str, final Set<String> dejaVu) {
        if (appendStringDateOrNull(o, str)) {
            return;
        }
        if (isMaybeRecursive(o)) {
            appendPotentiallyRecursiveValue(o, str, dejaVu);
        } else {
            tryObjectToString(o, str);
        }
    }

    private static boolean appendStringDateOrNull(final Object o, final StringBuilder str) {
        if (o == null || o instanceof String) {
            str.append(String.valueOf(o));
            return true;
        }
        return appendDate(o, str);
    }

    private static boolean appendDate(final Object o, final StringBuilder str) {
        if (!(o instanceof Date)) {
            return false;
        }
        final Date date = (Date) o;
        final SimpleDateFormat format = getSimpleDateFormat();
        str.append(format.format(date));
        return true;
    }

    private static SimpleDateFormat getSimpleDateFormat() {
        // I'll leave it like this for the moment... this could probably be optimized using ThreadLocal...
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    }

    /**
     * Returns {@code true} if the specified object is an array, a Map or a Collection.
     */
    private static boolean isMaybeRecursive(final Object o) {
        return o.getClass().isArray() || o instanceof Map || o instanceof Collection;
    }

    private static void appendPotentiallyRecursiveValue(final Object o, final StringBuilder str,
            final Set<String> dejaVu) {
        final Class<?> oClass = o.getClass();
        if (oClass.isArray()) {
            appendArray(o, str, dejaVu, oClass);
        } else if (o instanceof Map) {
            appendMap(o, str, dejaVu);
        } else if (o instanceof Collection) {
            appendCollection(o, str, dejaVu);
        }
    }

    private static void appendArray(final Object o, final StringBuilder str, final Set<String> dejaVu,
            final Class<?> oClass) {
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
            final String id = identityToString(o);
            if (dejaVu.contains(id)) {
                str.append(RECURSION_PREFIX).append(id).append(RECURSION_SUFFIX);
            } else {
                dejaVu.add(id);
                final Object[] oArray = (Object[]) o;
                str.append('[');
                boolean first = true;
                for (final Object current : oArray) {
                    if (first) {
                        first = false;
                    } else {
                        str.append(", ");
                    }
                    recursiveDeepToString(current, str, new HashSet<>(dejaVu));
                }
                str.append(']');
            }
            //str.append(Arrays.deepToString((Object[]) o));
        }
    }

    private static void appendMap(final Object o, final StringBuilder str, final Set<String> dejaVu) {
        // special handling of container Map
        final String id = identityToString(o);
        if (dejaVu.contains(id)) {
            str.append(RECURSION_PREFIX).append(id).append(RECURSION_SUFFIX);
        } else {
            dejaVu.add(id);
            final Map<?, ?> oMap = (Map<?, ?>) o;
            str.append('{');
            boolean isFirst = true;
            for (final Object o1 : oMap.entrySet()) {
                final Map.Entry<?, ?> current = (Map.Entry<?, ?>) o1;
                if (isFirst) {
                    isFirst = false;
                } else {
                    str.append(", ");
                }
                final Object key = current.getKey();
                final Object value = current.getValue();
                recursiveDeepToString(key, str, new HashSet<>(dejaVu));
                str.append('=');
                recursiveDeepToString(value, str, new HashSet<>(dejaVu));
            }
            str.append('}');
        }
    }

    private static void appendCollection(final Object o, final StringBuilder str, final Set<String> dejaVu) {
        // special handling of container Collection
        final String id = identityToString(o);
        if (dejaVu.contains(id)) {
            str.append(RECURSION_PREFIX).append(id).append(RECURSION_SUFFIX);
        } else {
            dejaVu.add(id);
            final Collection<?> oCol = (Collection<?>) o;
            str.append('[');
            boolean isFirst = true;
            for (final Object anOCol : oCol) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    str.append(", ");
                }
                recursiveDeepToString(anOCol, str, new HashSet<>(dejaVu));
            }
            str.append(']');
        }
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
    public static String identityToString(final Object obj) {
        if (obj == null) {
            return null;
        }
        return obj.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(obj));
    }

    @Override
    public String toString() {
        return getFormattedMessage();
    }
}
