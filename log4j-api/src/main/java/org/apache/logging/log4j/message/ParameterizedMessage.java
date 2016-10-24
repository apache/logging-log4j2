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

import java.util.Arrays;

import org.apache.logging.log4j.util.StringBuilderFormattable;

/**
 * Handles messages that consist of a format string containing '{}' to represent each replaceable token, and
 * the parameters.
 * <p>
 * This class was originally written for <a href="http://lilithapp.com/">Lilith</a> by Joern Huxhorn where it is
 * licensed under the LGPL. It has been relicensed here with his permission providing that this attribution remain.
 * </p>
 */
public class ParameterizedMessage implements Message, StringBuilderFormattable {

    // Should this be configurable?
    private static final int DEFAULT_STRING_BUILDER_SIZE = 255;

    /**
     * Prefix for recursion.
     */
    public static final String RECURSION_PREFIX = ParameterFormatter.RECURSION_PREFIX;
    /**
     * Suffix for recursion.
     */
    public static final String RECURSION_SUFFIX = ParameterFormatter.RECURSION_SUFFIX;

    /**
     * Prefix for errors.
     */
    public static final String ERROR_PREFIX = ParameterFormatter.ERROR_PREFIX;

    /**
     * Separator for errors.
     */
    public static final String ERROR_SEPARATOR = ParameterFormatter.ERROR_SEPARATOR;

    /**
     * Separator for error messages.
     */
    public static final String ERROR_MSG_SEPARATOR = ParameterFormatter.ERROR_MSG_SEPARATOR;

    /**
     * Suffix for errors.
     */
    public static final String ERROR_SUFFIX = ParameterFormatter.ERROR_SUFFIX;

    private static final long serialVersionUID = -665975803997290697L;

    private static final int HASHVAL = 31;

    // storing JDK classes in ThreadLocals does not cause memory leaks in web apps, so this is okay
    private static ThreadLocal<StringBuilder> threadLocalStringBuilder = new ThreadLocal<>();

    private String messagePattern;
    private transient Object[] argArray;

    private String formattedMessage;
    private transient Throwable throwable;
    private int[] indices;
    private int usedCount;

    /**
     * Creates a parameterized message.
     * @param messagePattern The message "format" string. This will be a String containing "{}" placeholders
     * where parameters should be substituted.
     * @param arguments The arguments for substitution.
     * @param throwable A Throwable.
     * @deprecated Use constructor ParameterizedMessage(String, Object[], Throwable) instead
     */
    @Deprecated
    public ParameterizedMessage(final String messagePattern, final String[] arguments, final Throwable throwable) {
        this.argArray = arguments;
        this.throwable = throwable;
        init(messagePattern);
    }

    /**
     * Creates a parameterized message.
     * @param messagePattern The message "format" string. This will be a String containing "{}" placeholders
     * where parameters should be substituted.
     * @param arguments The arguments for substitution.
     * @param throwable A Throwable.
     */
    public ParameterizedMessage(final String messagePattern, final Object[] arguments, final Throwable throwable) {
        this.argArray = arguments;
        this.throwable = throwable;
        init(messagePattern);
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
    public ParameterizedMessage(final String messagePattern, final Object... arguments) {
        this.argArray = arguments;
        init(messagePattern);
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
     * @param arg0 The first parameter.
     * @param arg1 The second parameter.
     */
    public ParameterizedMessage(final String messagePattern, final Object arg0, final Object arg1) {
        this(messagePattern, new Object[]{arg0, arg1});
    }

    private void init(final String messagePattern) {
        this.messagePattern = messagePattern;
        final int len = Math.max(1, messagePattern == null ? 0 : messagePattern.length() >> 1); // divide by 2
        this.indices = new int[len]; // LOG4J2-1542 ensure non-zero array length
        final int placeholders = ParameterFormatter.countArgumentPlaceholders2(messagePattern, indices);
        initThrowable(argArray, placeholders);
        this.usedCount = Math.min(placeholders, argArray == null ? 0 : argArray.length);
    }

    private void initThrowable(final Object[] params, final int usedParams) {
        if (params != null) {
            final int argCount = params.length;
            if (usedParams < argCount && this.throwable == null && params[argCount - 1] instanceof Throwable) {
                this.throwable = (Throwable) params[argCount - 1];
            }
        }
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
        return argArray;
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

    /**
     * Returns the formatted message.
     * @return the formatted message.
     */
    @Override
    public String getFormattedMessage() {
        if (formattedMessage == null) {
            final StringBuilder buffer = getThreadLocalStringBuilder();
            formatTo(buffer);
            formattedMessage = buffer.toString();
        }
        return formattedMessage;
    }

    private static StringBuilder getThreadLocalStringBuilder() {
        StringBuilder buffer = threadLocalStringBuilder.get();
        if (buffer == null) {
            buffer = new StringBuilder(DEFAULT_STRING_BUILDER_SIZE);
            threadLocalStringBuilder.set(buffer);
        }
        buffer.setLength(0);
        return buffer;
    }

    @Override
    public void formatTo(final StringBuilder buffer) {
        if (formattedMessage != null) {
            buffer.append(formattedMessage);
        } else {
            if (indices[0] < 0) {
                ParameterFormatter.formatMessage(buffer, messagePattern, argArray, usedCount);
            } else {
                ParameterFormatter.formatMessage2(buffer, messagePattern, argArray, usedCount, indices);
            }
        }
    }

    /**
     * Replace placeholders in the given messagePattern with arguments.
     *
     * @param messagePattern the message pattern containing placeholders.
     * @param arguments      the arguments to be used to replace placeholders.
     * @return the formatted message.
     */
    public static String format(final String messagePattern, final Object[] arguments) {
        return ParameterFormatter.format(messagePattern, arguments);
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
        if (!Arrays.equals(this.argArray, that.argArray)) {
            return false;
        }
        //if (throwable != null ? !throwable.equals(that.throwable) : that.throwable != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = messagePattern != null ? messagePattern.hashCode() : 0;
        result = HASHVAL * result + (argArray != null ? Arrays.hashCode(argArray) : 0);
        return result;
    }

    /**
     * Counts the number of unescaped placeholders in the given messagePattern.
     *
     * @param messagePattern the message pattern to be analyzed.
     * @return the number of unescaped placeholders.
     */
    public static int countArgumentPlaceholders(final String messagePattern) {
        return ParameterFormatter.countArgumentPlaceholders(messagePattern);
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
        return ParameterFormatter.deepToString(o);
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
        return ParameterFormatter.identityToString(obj);
    }

    @Override
    public String toString() {
        return "ParameterizedMessage[messagePattern=" + messagePattern + ", stringArgs=" +
                Arrays.toString(argArray) + ", throwable=" + throwable + ']';
    }
}
