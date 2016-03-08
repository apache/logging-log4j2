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
 * Reusable parameterized message.
 * @see ParameterizedMessage
 * @since 2.6
 */
@PerformanceSensitive("allocation")
public class ReusableParameterizedMessage implements ReusableMessage {

    private static final long serialVersionUID = 7800075879295123856L;

    static class InternalState {
        private final StringBuilder buffer = new StringBuilder(2048);
        private String messagePattern;
        private int argCount;
        private transient Object[] varargs;
        private transient Object[] params = new Object[10];
        private transient Throwable throwable;

        private Object[] getTrimmedParams() {
            return varargs == null ? Arrays.copyOf(params, argCount) : varargs;
        }

        private Object[] getParams() {
            return varargs == null ? params : varargs;
        }

        private void init(String messagePattern, int argCount, Object[] paramArray) {
            this.varargs = null;
            this.buffer.setLength(0);

            this.messagePattern = messagePattern;
            this.argCount= argCount;
            //this.formattedMessage = null;
            int usedCount = ParameterFormatter.countArgumentPlaceholders(messagePattern);
            initThrowable(paramArray, usedCount);
        }

        private void initThrowable(final Object[] params, final int usedParams) {
            if (usedParams < argCount && this.throwable == null && params[argCount - 1] instanceof Throwable) {
                this.throwable = (Throwable) params[argCount - 1];
                argCount--;
            }
        }
    }

    // storing non-JDK classes in ThreadLocals causes memory leaks in web apps, do not use in web apps!
    private static ThreadLocal<InternalState> state = new ThreadLocal<>();

    /**
     * Creates a reusable message.
     */
    public ReusableParameterizedMessage() {
    }

    private InternalState getState() {
        InternalState result = state.get();
        if (result == null) {
            result = new InternalState();
            state.set(result);
        }
        return result;
    }

    public StringBuilder get() {
        return getState().buffer;
    }

    ReusableParameterizedMessage set(String messagePattern, Object... arguments) {
        InternalState state = getState();
        state.init(messagePattern, arguments == null ? 0 : arguments.length, arguments);
        state.varargs = arguments;
        return this;
    }

    ReusableParameterizedMessage set(String messagePattern, Object p0) {
        InternalState state = getState();
        state.params[0] = p0;
        state.init(messagePattern, 1, state.params);
        return this;
    }

    ReusableParameterizedMessage set(String messagePattern, Object p0, Object p1) {
        InternalState state = getState();
        state.params[0] = p0;
        state.params[1] = p1;
        state.init(messagePattern, 2, state.params);
        return this;
    }

    ReusableParameterizedMessage set(String messagePattern, Object p0, Object p1, Object p2) {
        InternalState state = getState();
        state.params[0] = p0;
        state.params[1] = p1;
        state.params[2] = p2;
        state.init(messagePattern, 3, state.params);
        return this;
    }

    ReusableParameterizedMessage set(String messagePattern, Object p0, Object p1, Object p2, Object p3) {
        InternalState state = getState();
        state.params[0] = p0;
        state.params[1] = p1;
        state.params[2] = p2;
        state.params[3] = p3;
        state.init(messagePattern, 4, state.params);
        return this;
    }

    ReusableParameterizedMessage set(String messagePattern, Object p0, Object p1, Object p2, Object p3, Object p4) {
        InternalState state = getState();
        state.params[0] = p0;
        state.params[1] = p1;
        state.params[2] = p2;
        state.params[3] = p3;
        state.params[4] = p4;
        state.init(messagePattern, 5, state.params);
        return this;
    }

    ReusableParameterizedMessage set(String messagePattern, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
        InternalState state = getState();
        state.params[0] = p0;
        state.params[1] = p1;
        state.params[2] = p2;
        state.params[3] = p3;
        state.params[4] = p4;
        state.params[5] = p5;
        state.init(messagePattern, 6, state.params);
        return this;
    }

    ReusableParameterizedMessage set(String messagePattern, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
            Object p6) {
        InternalState state = getState();
        state.params[0] = p0;
        state.params[1] = p1;
        state.params[2] = p2;
        state.params[3] = p3;
        state.params[4] = p4;
        state.params[5] = p5;
        state.params[6] = p6;
        state.init(messagePattern, 7, state.params);
        return this;
    }

    ReusableParameterizedMessage set(String messagePattern, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
            Object p6, Object p7) {
        InternalState state = getState();
        state.params[0] = p0;
        state.params[1] = p1;
        state.params[2] = p2;
        state.params[3] = p3;
        state.params[4] = p4;
        state.params[5] = p5;
        state.params[6] = p6;
        state.params[7] = p7;
        state.init(messagePattern, 8, state.params);
        return this;
    }

    ReusableParameterizedMessage set(String messagePattern, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
            Object p6, Object p7, Object p8) {
        InternalState state = getState();
        state.params[0] = p0;
        state.params[1] = p1;
        state.params[2] = p2;
        state.params[3] = p3;
        state.params[4] = p4;
        state.params[5] = p5;
        state.params[6] = p6;
        state.params[7] = p7;
        state.params[8] = p8;
        state.init(messagePattern, 9, state.params);
        return this;
    }

    ReusableParameterizedMessage set(String messagePattern, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
            Object p6, Object p7, Object p8, Object p9) {
        InternalState state = getState();
        state.params[0] = p0;
        state.params[1] = p1;
        state.params[2] = p2;
        state.params[3] = p3;
        state.params[4] = p4;
        state.params[5] = p5;
        state.params[6] = p6;
        state.params[7] = p7;
        state.params[8] = p8;
        state.params[9] = p9;
        state.init(messagePattern, 10, state.params);
        return this;
    }

    /**
     * Returns the message pattern.
     * @return the message pattern.
     */
    @Override
    public String getFormat() {
        return getState().messagePattern;
    }

    /**
     * Returns the message parameters.
     * @return the message parameters.
     */
    @Override
    public Object[] getParameters() {
        return getState().getTrimmedParams();
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
        return getState().throwable;
    }

    /**
     * Returns the formatted message.
     * @return the formatted message.
     */
    @Override
    public String getFormattedMessage() {
        final InternalState state = getState();
        final StringBuilder buffer = state.buffer;
        formatTo(buffer);
        return buffer.toString();
    }

    @Override
    public void formatTo(final StringBuilder buffer) {
        InternalState state = getState();
        ParameterFormatter.formatMessage(buffer, state.messagePattern, state.getParams(), state.argCount);
    }


    @Override
    public String toString() {
        return "ReusableParameterizedMessage[messagePattern=" + getFormat() + ", stringArgs=" +
                Arrays.toString(getParameters()) + ", throwable=" + getThrowable() + ']';
    }
}
