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

import static org.apache.logging.log4j.message.ParameterFormatter.analyzePattern;
import static org.apache.logging.log4j.util.StringBuilders.trimToMaxSize;

import com.google.errorprone.annotations.InlineMe;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import org.apache.logging.log4j.message.ParameterFormatter.MessagePatternAnalysis;
import org.apache.logging.log4j.util.Constants;
import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.apache.logging.log4j.util.internal.SerializationUtil;

/**
 * A {@link Message} accepting argument placeholders in the formatting pattern.
 * <p>
 * Only {@literal "{}"} strings are treated as argument placeholders.
 * Escaped (i.e., {@code "\"}-prefixed) or incomplete argument placeholders will be ignored.
 * Examples of argument placeholders that will be discarded and rendered intact:
 * </p>
 * <pre>
 * { }
 * foo\{}
 * {bar
 * {buzz}
 * </pre>
 */
public class ParameterizedMessage implements Message, StringBuilderFormattable {

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

    private static final ThreadLocal<FormatBufferHolder> FORMAT_BUFFER_HOLDER_REF =
            Constants.ENABLE_THREADLOCALS ? ThreadLocal.withInitial(FormatBufferHolder::new) : null;

    private static final class FormatBufferHolder {

        private final StringBuilder buffer = new StringBuilder(Constants.MAX_REUSABLE_MESSAGE_SIZE);

        private boolean used = false;
    }

    private final String pattern;

    private transient Object[] args;

    private final transient Throwable throwable;

    private final MessagePatternAnalysis patternAnalysis;

    private String formattedMessage;

    /**
     * Constructs an instance.
     * <p>
     * The {@link Throwable} associated with the message (and returned in {@link #getThrowable()}) will be determined as follows:
     * </p>
     * <ol>
     * <li>If a {@code throwable} argument is provided</li>
     * <li>If the last argument is a {@link Throwable} and is <b>not</b> referred to by any placeholder in the pattern</li>
     * </ol>
     *
     * @param pattern a formatting pattern
     * @param args arguments to be formatted
     * @param throwable a {@link Throwable}
     * @deprecated Use {@link #ParameterizedMessage(String, Object[], Throwable)} instead
     */
    @InlineMe(
            replacement = "this(pattern, Arrays.stream(args).toArray(Object[]::new), throwable)",
            imports = "java.util.Arrays")
    @Deprecated
    public ParameterizedMessage(final String pattern, final String[] args, final Throwable throwable) {
        this(pattern, Arrays.stream(args).toArray(Object[]::new), throwable);
    }

    /**
     * Constructs an instance.
     * <p>
     * The {@link Throwable} associated with the message (and returned in {@link #getThrowable()}) will be determined as follows:
     * </p>
     * <ol>
     * <li>If a {@code throwable} argument is provided</li>
     * <li>If the last argument is a {@link Throwable} and is <b>not</b> referred to by any placeholder in the pattern</li>
     * </ol>
     *
     * @param pattern a formatting pattern
     * @param args arguments to be formatted
     * @param throwable a {@link Throwable}
     */
    public ParameterizedMessage(final String pattern, final Object[] args, final Throwable throwable) {
        this.args = args;
        this.pattern = pattern;
        this.patternAnalysis = analyzePattern(pattern, args != null ? args.length : 0);
        this.throwable = determineThrowable(throwable, this.args, patternAnalysis);
    }

    private static Throwable determineThrowable(
            final Throwable throwable, final Object[] args, final MessagePatternAnalysis analysis) {

        // Short-circuit if an explicit `Throwable` is provided
        if (throwable != null) {
            return throwable;
        }

        // If the last `Throwable` argument is not consumed in the pattern, use that
        if (args != null && args.length > analysis.placeholderCount) {
            Object lastArg = args[args.length - 1];
            if (lastArg instanceof Throwable) {
                return (Throwable) lastArg;
            }
        }

        // No `Throwable`s available
        return null;
    }

    /**
     * Constructor with a pattern and multiple arguments.
     * <p>
     * If the last argument is a {@link Throwable} and is <b>not</b> referred to by any placeholder in the pattern, it is returned in {@link #getThrowable()}.
     * </p>
     *
     * @param pattern a formatting pattern
     * @param args arguments to be formatted
     */
    public ParameterizedMessage(final String pattern, final Object... args) {
        this(pattern, args, null);
    }

    /**
     * Constructor with a pattern and a single argument.
     * <p>
     * If the argument is a {@link Throwable} and is <b>not</b> referred to by any placeholder in the pattern, it is returned in {@link #getThrowable()}.
     * </p>
     *
     * @param pattern a formatting pattern
     * @param arg an argument
     */
    public ParameterizedMessage(final String pattern, final Object arg) {
        this(pattern, new Object[] {arg});
    }

    /**
     * Constructor with a pattern and two arguments.
     * <p>
     * If the last argument is a {@link Throwable} and is <b>not</b> referred to by any placeholder in the pattern, it is returned in {@link #getThrowable()} and won't be contained in the formatted message.
     * </p>
     *
     * @param pattern a formatting pattern
     * @param arg0 the first argument
     * @param arg1 the second argument
     */
    public ParameterizedMessage(final String pattern, final Object arg0, final Object arg1) {
        this(pattern, new Object[] {arg0, arg1});
    }

    /**
     * @return the message formatting pattern
     */
    @Override
    public String getFormat() {
        return pattern;
    }

    /**
     * @return the message arguments
     */
    @Override
    public Object[] getParameters() {
        return args;
    }

    /**
     * The {@link Throwable} provided along with the message by one of the following means:
     * <ol>
     * <li>explicitly in the constructor</li>
     * <li>as the last message argument that is <b>not</b> referred to by any placeholder in the formatting pattern</li>
     * </ol>
     *
     * @return the {@link Throwable} provided along with the message
     */
    @Override
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * Returns the formatted message.
     * <p>
     * If possible, the result will be cached for subsequent invocations.
     * </p>
     *
     * @return the formatted message
     */
    @Override
    public String getFormattedMessage() {
        if (formattedMessage == null) {
            final FormatBufferHolder bufferHolder;
            // If there isn't a format buffer to reuse
            if (FORMAT_BUFFER_HOLDER_REF == null || (bufferHolder = FORMAT_BUFFER_HOLDER_REF.get()).used) {
                final StringBuilder buffer = new StringBuilder(Constants.MAX_REUSABLE_MESSAGE_SIZE);
                formatTo(buffer);
                formattedMessage = buffer.toString();
            }
            // If there is a format buffer to reuse
            else {
                bufferHolder.used = true;
                final StringBuilder buffer = bufferHolder.buffer;
                try {
                    formatTo(buffer);
                    formattedMessage = buffer.toString();
                } finally {
                    trimToMaxSize(buffer, Constants.MAX_REUSABLE_MESSAGE_SIZE);
                    buffer.setLength(0);
                    bufferHolder.used = false;
                }
            }
        }
        return formattedMessage;
    }

    @Override
    public void formatTo(final StringBuilder buffer) {
        if (formattedMessage != null) {
            buffer.append(formattedMessage);
        } else {
            final int argCount = args != null ? args.length : 0;
            ParameterFormatter.formatMessage(buffer, pattern, args, argCount, patternAnalysis);
        }
    }

    /**
     * Returns the formatted message.
     * @param pattern a message pattern containing argument placeholders
     * @param args arguments to be used to replace placeholders
     */
    public static String format(final String pattern, final Object[] args) {
        final int argCount = args != null ? args.length : 0;
        return ParameterFormatter.format(pattern, args, argCount);
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ParameterizedMessage)) {
            return false;
        }
        final ParameterizedMessage that = (ParameterizedMessage) object;
        return Objects.equals(pattern, that.pattern) && Arrays.equals(args, that.args);
    }

    @Override
    public int hashCode() {
        int result = pattern != null ? pattern.hashCode() : 0;
        result = 31 * result + (args != null ? Arrays.hashCode(args) : 0);
        return result;
    }

    /**
     * Returns the number of argument placeholders.
     * @param pattern the message pattern to be analyzed
     */
    public static int countArgumentPlaceholders(final String pattern) {
        if (pattern == null) {
            return 0;
        }
        return analyzePattern(pattern, -1).placeholderCount;
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
        // Avoid formatting arguments!
        // It can cause recursion, which can become pretty unpleasant while troubleshooting.
        return "ParameterizedMessage[messagePattern=" + pattern + ", argCount=" + args.length + ", throwableProvided="
                + (throwable != null) + ']';
    }

    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(args.length);
        for (final Object arg : args) {
            final Serializable serializableArg = arg instanceof Serializable ? (Serializable) arg : String.valueOf(arg);
            SerializationUtil.writeWrappedObject(serializableArg, out);
        }
    }

    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        SerializationUtil.assertFiltered(in);
        in.defaultReadObject();
        final int argCount = in.readInt();
        args = new Object[argCount];
        for (int argIndex = 0; argIndex < args.length; argIndex++) {
            args[argIndex] = SerializationUtil.readWrappedObject(in);
        }
    }
}
