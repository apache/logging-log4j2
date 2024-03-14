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

import static org.apache.logging.log4j.util.StringBuilders.trimToMaxSize;

import java.util.Arrays;
import org.apache.logging.log4j.message.ParameterFormatter.MessagePatternAnalysis;
import org.apache.logging.log4j.util.Constants;
import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * Reusable parameterized message. This message is mutable and is not safe to be accessed or modified by multiple
 * threads concurrently.
 *
 * @see ParameterizedMessage
 * @since 2.6
 */
@PerformanceSensitive("allocation")
public class ReusableParameterizedMessage implements ReusableMessage, ParameterVisitable, Clearable {

    private static final int MAX_PARAMS = 10;
    private static final long serialVersionUID = 7800075879295123856L;

    private String messagePattern;
    private final MessagePatternAnalysis patternAnalysis = new MessagePatternAnalysis();
    private final StringBuilder formatBuffer = new StringBuilder(Constants.MAX_REUSABLE_MESSAGE_SIZE);
    private int argCount;
    private transient Object[] varargs;
    private transient Object[] params = new Object[MAX_PARAMS];
    private transient Throwable throwable;
    transient boolean reserved = false; // LOG4J2-1583 prevent scrambled logs with nested logging calls

    /**
     * Creates a reusable message.
     */
    public ReusableParameterizedMessage() {}

    private Object[] getTrimmedParams() {
        return varargs == null ? Arrays.copyOf(params, argCount) : varargs;
    }

    private Object[] getParams() {
        return varargs == null ? params : varargs;
    }

    // see interface javadoc
    @Override
    public Object[] swapParameters(final Object[] emptyReplacement) {
        Object[] result;
        if (varargs == null) {
            result = params;
            if (emptyReplacement.length >= MAX_PARAMS) {
                params = emptyReplacement;
            } else if (argCount <= emptyReplacement.length) {
                // Bad replacement! Too small, may blow up future 10-arg messages.
                // copy params into the specified replacement array and return that
                System.arraycopy(params, 0, emptyReplacement, 0, argCount);
                // Do not retain references to objects in the reusable params array.
                for (int i = 0; i < argCount; i++) {
                    params[i] = null;
                }
                result = emptyReplacement;
            } else {
                // replacement array is too small for current content and future content: discard it
                params = new Object[MAX_PARAMS];
            }
        } else {
            // The returned array will be reused by the caller in future swapParameter() calls.
            // Therefore we want to avoid returning arrays with less than 10 elements.
            // If the vararg array is less than 10 params we just copy its content into the specified array
            // and return it. This helps the caller to retain a reusable array of at least 10 elements.
            // NOTE: LOG4J2-1688 unearthed the use case that an application array (not a varargs array) is passed
            // as the argument array. This array should not be modified, so it cannot be passed to the caller
            // who will at some point null out the elements in the array).
            if (argCount <= emptyReplacement.length) {
                result = emptyReplacement;
            } else {
                result = new Object[argCount]; // LOG4J2-1688
            }
            // copy params into the specified replacement array and return that
            System.arraycopy(varargs, 0, result, 0, argCount);
        }
        return result;
    }

    // see interface javadoc
    @Override
    public short getParameterCount() {
        return (short) argCount;
    }

    @Override
    public <S> void forEachParameter(final ParameterConsumer<S> action, final S state) {
        final Object[] parameters = getParams();
        for (short i = 0; i < argCount; i++) {
            action.accept(parameters[i], i, state);
        }
    }

    @Override
    public Message memento() {
        return new ParameterizedMessage(messagePattern, getTrimmedParams());
    }

    private void init(final String messagePattern, final int argCount, final Object[] args) {
        this.varargs = null;
        this.messagePattern = messagePattern;
        this.argCount = argCount;
        ParameterFormatter.analyzePattern(messagePattern, argCount, patternAnalysis);
        this.throwable = determineThrowable(args, argCount, patternAnalysis.placeholderCount);
    }

    private static Throwable determineThrowable(final Object[] args, final int argCount, final int placeholderCount) {
        if (placeholderCount < argCount) {
            final Object lastArg = args[argCount - 1];
            if (lastArg instanceof Throwable) {
                return (Throwable) lastArg;
            }
        }
        return null;
    }

    public ReusableParameterizedMessage set(final String messagePattern, final Object... arguments) {
        init(messagePattern, arguments == null ? 0 : arguments.length, arguments);
        varargs = arguments;
        return this;
    }

    public ReusableParameterizedMessage set(final String messagePattern, final Object p0) {
        params[0] = p0;
        init(messagePattern, 1, params);
        return this;
    }

    public ReusableParameterizedMessage set(final String messagePattern, final Object p0, final Object p1) {
        params[0] = p0;
        params[1] = p1;
        init(messagePattern, 2, params);
        return this;
    }

    public ReusableParameterizedMessage set(
            final String messagePattern, final Object p0, final Object p1, final Object p2) {
        params[0] = p0;
        params[1] = p1;
        params[2] = p2;
        init(messagePattern, 3, params);
        return this;
    }

    public ReusableParameterizedMessage set(
            final String messagePattern, final Object p0, final Object p1, final Object p2, final Object p3) {
        params[0] = p0;
        params[1] = p1;
        params[2] = p2;
        params[3] = p3;
        init(messagePattern, 4, params);
        return this;
    }

    public ReusableParameterizedMessage set(
            final String messagePattern,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4) {
        params[0] = p0;
        params[1] = p1;
        params[2] = p2;
        params[3] = p3;
        params[4] = p4;
        init(messagePattern, 5, params);
        return this;
    }

    public ReusableParameterizedMessage set(
            final String messagePattern,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        params[0] = p0;
        params[1] = p1;
        params[2] = p2;
        params[3] = p3;
        params[4] = p4;
        params[5] = p5;
        init(messagePattern, 6, params);
        return this;
    }

    public ReusableParameterizedMessage set(
            final String messagePattern,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        params[0] = p0;
        params[1] = p1;
        params[2] = p2;
        params[3] = p3;
        params[4] = p4;
        params[5] = p5;
        params[6] = p6;
        init(messagePattern, 7, params);
        return this;
    }

    public ReusableParameterizedMessage set(
            final String messagePattern,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        params[0] = p0;
        params[1] = p1;
        params[2] = p2;
        params[3] = p3;
        params[4] = p4;
        params[5] = p5;
        params[6] = p6;
        params[7] = p7;
        init(messagePattern, 8, params);
        return this;
    }

    public ReusableParameterizedMessage set(
            final String messagePattern,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        params[0] = p0;
        params[1] = p1;
        params[2] = p2;
        params[3] = p3;
        params[4] = p4;
        params[5] = p5;
        params[6] = p6;
        params[7] = p7;
        params[8] = p8;
        init(messagePattern, 9, params);
        return this;
    }

    public ReusableParameterizedMessage set(
            final String messagePattern,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9) {
        params[0] = p0;
        params[1] = p1;
        params[2] = p2;
        params[3] = p3;
        params[4] = p4;
        params[5] = p5;
        params[6] = p6;
        params[7] = p7;
        params[8] = p8;
        params[9] = p9;
        init(messagePattern, 10, params);
        return this;
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
        return getTrimmedParams();
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
        try {
            formatTo(formatBuffer);
            return formatBuffer.toString();
        } finally {
            trimToMaxSize(formatBuffer, Constants.MAX_REUSABLE_MESSAGE_SIZE);
            formatBuffer.setLength(0);
        }
    }

    @Override
    public void formatTo(final StringBuilder builder) {
        ParameterFormatter.formatMessage(builder, messagePattern, getParams(), argCount, patternAnalysis);
    }

    /**
     * Sets the reserved flag to true and returns this object.
     * @return this object
     * @since 2.7
     */
    ReusableParameterizedMessage reserve() {
        reserved = true;
        return this;
    }

    @Override
    public String toString() {
        // Avoid formatting arguments!
        // It can cause recursion, which can become pretty unpleasant while troubleshooting.
        return "ReusableParameterizedMessage[messagePattern=" + getFormat() + ", argCount=" + getParameterCount()
                + ", throwableProvided=" + (getThrowable() != null) + ']';
    }

    @Override
    public void clear() { // LOG4J2-1583
        // This method does not clear parameter values, those are expected to be swapped to a
        // reusable message, which is responsible for clearing references.
        reserved = false;
        varargs = null;
        messagePattern = null;
        throwable = null;
        // Cut down on the memory usage after an analysis with an excessive argument count
        final int placeholderCharIndicesMaxLength = 16;
        if (patternAnalysis.placeholderCharIndices != null
                && patternAnalysis.placeholderCharIndices.length > placeholderCharIndicesMaxLength) {
            patternAnalysis.placeholderCharIndices = new int[placeholderCharIndicesMaxLength];
        }
    }

    private Object writeReplace() {
        return memento();
    }
}
