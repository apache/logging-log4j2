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

import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * Reusable parameterized message. This message is mutable and is not safe to be accessed or modified by multiple
 * threads concurrently.
 *
 * @see ParameterizedMessage
 * @since 2.6
 */
@PerformanceSensitive("allocation")
public class ReusableParameterizedMessage implements ReusableMessage {

    private static final long serialVersionUID = 7800075879295123856L;
    private static ThreadLocal<StringBuilder> buffer = new ThreadLocal<>();

    private String messagePattern;
    private int argCount;
    private int usedCount;
    private final int[] indices = new int[256];
    private transient Object[] varargs;
    private transient Object[] params = new Object[10];
    private transient Throwable throwable;

    /**
     * Creates a reusable message.
     */
    public ReusableParameterizedMessage() {
    }

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
            if (emptyReplacement.length >= 10) {
                params = emptyReplacement;
            } else {
                // Bad replacement! Too small, may blow up future 10-arg messages.
                if (argCount <= emptyReplacement.length) {
                    // copy params into the specified replacement array and return that
                    System.arraycopy(params, 0, emptyReplacement, 0, argCount);
                    result = emptyReplacement;
                } else {
                    // replacement array is too small for current content and future content: discard it
                    params = new Object[10];
                }
            }
        } else {
            // The returned array will be reused by the caller in future swapParameter() calls.
            // Therefore we want to avoid returning arrays with less than 10 elements.
            // If the vararg array is less than 10 params we just copy its content into the specified array
            // and return it. This helps the caller to retain a reusable array of at least 10 elements.
            if (argCount <= emptyReplacement.length) {
                // copy params into the specified replacement array and return that
                System.arraycopy(varargs, 0, emptyReplacement, 0, argCount);
                result = emptyReplacement;
            } else {
                result = varargs;
                varargs = emptyReplacement;
            }
        }
        return result;
    }

    // see interface javadoc
    @Override
    public short getParameterCount() {
        return (short) argCount;
    }

    @Override
    public Message memento() {
        return new ParameterizedMessage(messagePattern, getTrimmedParams());
    }

    private void init(final String messagePattern, final int argCount, final Object[] paramArray) {
        this.varargs = null;
        this.messagePattern = messagePattern;
        this.argCount = argCount;
        int placeholderCount = count(messagePattern, indices);
        initThrowable(paramArray, argCount, placeholderCount);
        this.usedCount = Math.min(placeholderCount, argCount);
    }

    private static int count(final String messagePattern, final int[] indices) {
        try {
            // try the fast path first
            return ParameterFormatter.countArgumentPlaceholders2(messagePattern, indices);
        } catch (final Exception ex) { // fallback if more than int[] length (256) parameter placeholders
            return ParameterFormatter.countArgumentPlaceholders(messagePattern);
        }
    }

    private void initThrowable(final Object[] params, final int argCount, final int usedParams) {
        if (usedParams < argCount && this.throwable == null && params[argCount - 1] instanceof Throwable) {
            this.throwable = (Throwable) params[argCount - 1];
        }
    }

    ReusableParameterizedMessage set(String messagePattern, Object... arguments) {
        init(messagePattern, arguments == null ? 0 : arguments.length, arguments);
        varargs = arguments;
        return this;
    }

    ReusableParameterizedMessage set(String messagePattern, Object p0) {
        params[0] = p0;
        init(messagePattern, 1, params);
        return this;
    }

    ReusableParameterizedMessage set(String messagePattern, Object p0, Object p1) {
        params[0] = p0;
        params[1] = p1;
        init(messagePattern, 2, params);
        return this;
    }

    ReusableParameterizedMessage set(String messagePattern, Object p0, Object p1, Object p2) {
        params[0] = p0;
        params[1] = p1;
        params[2] = p2;
        init(messagePattern, 3, params);
        return this;
    }

    ReusableParameterizedMessage set(String messagePattern, Object p0, Object p1, Object p2, Object p3) {
        params[0] = p0;
        params[1] = p1;
        params[2] = p2;
        params[3] = p3;
        init(messagePattern, 4, params);
        return this;
    }

    ReusableParameterizedMessage set(String messagePattern, Object p0, Object p1, Object p2, Object p3, Object p4) {
        params[0] = p0;
        params[1] = p1;
        params[2] = p2;
        params[3] = p3;
        params[4] = p4;
        init(messagePattern, 5, params);
        return this;
    }

    ReusableParameterizedMessage set(String messagePattern, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
        params[0] = p0;
        params[1] = p1;
        params[2] = p2;
        params[3] = p3;
        params[4] = p4;
        params[5] = p5;
        init(messagePattern, 6, params);
        return this;
    }

    ReusableParameterizedMessage set(String messagePattern, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
            Object p6) {
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

    ReusableParameterizedMessage set(String messagePattern, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
            Object p6, Object p7) {
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

    ReusableParameterizedMessage set(String messagePattern, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
            Object p6, Object p7, Object p8) {
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

    ReusableParameterizedMessage set(String messagePattern, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
            Object p6, Object p7, Object p8, Object p9) {
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
        final StringBuilder sb = getBuffer();
        formatTo(sb);
        return sb.toString();
    }

    private StringBuilder getBuffer() {
        StringBuilder result = buffer.get();
        if (result == null) {
            final int currentPatternLength = messagePattern == null ? 0 : messagePattern.length();
            result = new StringBuilder(Math.min(512, currentPatternLength * 2));
            buffer.set(result);
        }
        result.setLength(0);
        return result;
    }

    @Override
    public void formatTo(final StringBuilder buffer) {
        if (indices[0] < 0) {
            ParameterFormatter.formatMessage(buffer, messagePattern, getParams(), argCount);
        } else {
            ParameterFormatter.formatMessage2(buffer, messagePattern, getParams(), usedCount, indices);
        }
    }


    @Override
    public String toString() {
        return "ReusableParameterizedMessage[messagePattern=" + getFormat() + ", stringArgs=" +
                Arrays.toString(getParameters()) + ", throwable=" + getThrowable() + ']';
    }
}
