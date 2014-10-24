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
package org.apache.logging.log4j.message;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
        return format(msgPattern, sArgs);
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

        final StringBuilder result = new StringBuilder();
        int escapeCounter = 0;
        int currentArgument = 0;
        for (int i = 0; i < messagePattern.length(); i++) {
            final char curChar = messagePattern.charAt(i);
            if (curChar == ESCAPE_CHAR) {
                escapeCounter++;
            } else {
                if (curChar == DELIM_START && i < messagePattern.length() - 1
                        && messagePattern.charAt(i + 1) == DELIM_STOP) {
                    // write escaped escape chars
                    final int escapedEscapes = escapeCounter / 2;
                    for (int j = 0; j < escapedEscapes; j++) {
                        result.append(ESCAPE_CHAR);
                    }

                    if (escapeCounter % 2 == 1) {
                        // i.e. escaped
                        // write escaped escape chars
                        result.append(DELIM_START);
                        result.append(DELIM_STOP);
                    } else {
                        // unescaped
                        if (currentArgument < arguments.length) {
                            result.append(arguments[currentArgument]);
                        } else {
                            result.append(DELIM_START).append(DELIM_STOP);
                        }
                        currentArgument++;
                    }
                    i++;
                    escapeCounter = 0;
                    continue;
                }
                // any other char beside ESCAPE or DELIM_START/STOP-combo
                // write unescaped escape chars
                if (escapeCounter > 0) {
                    for (int j = 0; j < escapeCounter; j++) {
                        result.append(ESCAPE_CHAR);
                    }
                    escapeCounter = 0;
                }
                result.append(curChar);
            }
        }
        return result.toString();
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
        final Set<String> dejaVu = new HashSet<String>(); // that's actually a neat name ;)
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
        if (o == null) {
            str.append("null");
            return;
        }
        if (o instanceof String) {
            str.append(o);
            return;
        }

        final Class<?> oClass = o.getClass();
        if (oClass.isArray()) {
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
                        recursiveDeepToString(current, str, new HashSet<String>(dejaVu));
                    }
                    str.append(']');
                }
                //str.append(Arrays.deepToString((Object[]) o));
            }
        } else if (o instanceof Map) {
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
                    recursiveDeepToString(key, str, new HashSet<String>(dejaVu));
                    str.append('=');
                    recursiveDeepToString(value, str, new HashSet<String>(dejaVu));
                }
                str.append('}');
            }
        } else if (o instanceof Collection) {
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
                    recursiveDeepToString(anOCol, str, new HashSet<String>(dejaVu));
                }
                str.append(']');
            }
        } else if (o instanceof Date) {
            final Date date = (Date) o;
            final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            // I'll leave it like this for the moment... this could probably be optimized using ThreadLocal...
            str.append(format.format(date));
        } else {
            // it's just some other Object, we can only use toString().
            try {
                str.append(o.toString());
            } catch (final Throwable t) {
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
        }
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
        return "ParameterizedMessage[messagePattern=" + messagePattern + ", stringArgs=" +
            Arrays.toString(stringArgs) + ", throwable=" + throwable + ']';
    }
}
