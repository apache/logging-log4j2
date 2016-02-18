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

import org.apache.logging.log4j.message.Message;

import java.util.Arrays;

/**
 * Reusable Message..
 */
public class NoGcMessage implements Message {
    class InternalState {
        private Object[] params = new Object[10];
        private int paramCount;
        private StringBuilder buffer = new StringBuilder(2048);

        public Object[] getParamsCopy() {
            return Arrays.copyOf(params, paramCount);
        }
    }

    private ThreadLocal<InternalState> state = new ThreadLocal<>();

    public NoGcMessage() {
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

    public void set(String message, Object p1, Object p2, Object p3, Object p4) {
        InternalState state = getState();
        state.params[0] = p1;
        state.params[1] = p2;
        state.params[2] = p3;
        state.params[3] = p4;
        state.paramCount = 4;
        int current = 0;
        state.buffer.setLength(0);
        for (int i = 0; i < message.length() - 1; i++) {
            char c = message.charAt(i);
            if (c == '{' && message.charAt(i + 1) == '}') {
                append(state.params[current++], state.buffer);
                i++;
            } else {
                state.buffer.append(c);
            }
        }
        char c = message.charAt(message.length() - 1);
        if (c != '}') {
            state.buffer.append(c);
        }
    }

    private void append(Object param, StringBuilder buffer) {
        if (param instanceof StringBuilder) {
            buffer.append((StringBuilder) param);
        } else {
            buffer.append(param);
        }
    }

    @Override
    public String getFormattedMessage() {
        return getState().buffer.toString(); // not called by NoGcLayout
    }

    @Override
    public String getFormat() {
        return null;
    }

    @Override
    public Object[] getParameters() {
        return getState().getParamsCopy();
    }

    @Override
    public Throwable getThrowable() {
        return null;
    }
}
