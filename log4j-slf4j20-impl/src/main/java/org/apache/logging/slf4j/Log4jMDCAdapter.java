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
package org.apache.logging.slf4j;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.spi.MDCAdapter;

/**
 *
 */
public class Log4jMDCAdapter implements MDCAdapter {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final char QUOTE_CHAR = '\"';
    private static final char ESCAPE_CHAR = '\\';
    private static final char ARRAY_START = '[';
    private static final char ARRAY_SEP = ',';
    private static final char ARRAY_END = ']';
    private static final String EMPTY_ARRAY = "[]";

    /**
     * Quotes a string using a simplified JSON quoting mechanism.
     */
    static void quoteString(final CharSequence value, final StringBuilder output) {
        output.append(QUOTE_CHAR);
        int i = 0;
        while (i < value.length()) {
            final char c = value.charAt(i);
            switch (c) {
                case ESCAPE_CHAR:
                case QUOTE_CHAR:
                    output.append(ESCAPE_CHAR);
                    // intentional fall through
                default:
                    output.append(c);
            }
            i++;
        }
        output.append(QUOTE_CHAR);
    }

    /**
     * Splits a valid and non empty JSON array into a head and tail part. Unicode
     * escapes are not supported.
     */
    static void splitString(final char[] value, final String[] output) throws ArrayIndexOutOfBoundsException {
        final StringBuilder head = new StringBuilder();
        if (value[0] != ARRAY_START) {
            throw new ArrayIndexOutOfBoundsException(0);
        }
        if (value[1] != QUOTE_CHAR) {
            throw new ArrayIndexOutOfBoundsException(1);
        }
        int i = 2;
        while (true) {
            final char c = value[i];
            switch (c) {
                case ESCAPE_CHAR:
                    head.append(value[++i]);
                    break;
                case QUOTE_CHAR:
                    output[0] = head.toString();
                    if (value[++i] == ARRAY_SEP) {
                        value[i] = ARRAY_START;
                        output[1] = new String(value, i, value.length - i);
                    } else {
                        output[1] = null;
                    }
                    return;
                default:
                    head.append(value[i]);
            }
            i++;
        }
    }

    @Override
    public void put(final String key, final String val) {
        ThreadContext.put(key, val);
    }

    @Override
    public String get(final String key) {
        return ThreadContext.get(key);
    }

    @Override
    public void remove(final String key) {
        ThreadContext.remove(key);
    }

    @Override
    public void clear() {
        ThreadContext.clearMap();
    }

    @Override
    public Map<String, String> getCopyOfContextMap() {
        return ThreadContext.getContext();
    }

    @Override
    @SuppressWarnings("unchecked") // nothing we can do about this, restricted by SLF4J API
    public void setContextMap(@SuppressWarnings("rawtypes") final Map map) {
        ThreadContext.clearMap();
        ThreadContext.putAll(map);
    }

    @Override
    public void pushByKey(String key, String value) {
        Objects.requireNonNull(value);
        if (Strings.isEmpty(key)) {
            ThreadContext.push(value);
            return;
        }
        final String oldValue = ThreadContext.get(key);
        final StringBuilder output = new StringBuilder().append(ARRAY_START);
        quoteString(value, output);
        if (Strings.isEmpty(oldValue)) {
            output.append(ARRAY_END);
        } else {
            output.append(ARRAY_SEP).append(oldValue, 1, oldValue.length());
        }
        ThreadContext.put(key, output.toString());
    }

    @Override
    public String popByKey(String key) {
        if (Strings.isEmpty(key)) {
            return ThreadContext.pop();
        }
        final String value = ThreadContext.get(key);
        if (value == null || value.equals(EMPTY_ARRAY)) {
            return null;
        }
        try {
            final String[] headTail = new String[2];
            splitString(value.toCharArray(), headTail);
            if (headTail[1] == null) {
                ThreadContext.remove(key);
            } else {
                ThreadContext.put(key, headTail[1]);
            }
            return headTail[0];
        } catch (ArrayIndexOutOfBoundsException e) {
            // invalid value
            LOGGER.warn("Log4jMDCAdapter: invalid stack value {} for key {}.", value, key);
            return null;
        }
    }

    @Override
    public Deque<String> getCopyOfDequeByKey(String key) {
        final Deque<String> deque = new ArrayDeque<>();
        if (Strings.isEmpty(key)) {
            ThreadContext.getImmutableStack().asList().forEach(deque::push);
            return deque;
        }
        final String value = ThreadContext.get(key);
        if (value != null && !value.equals(EMPTY_ARRAY)) {
            try {
                final String[] headTail = new String[2];
                headTail[1] = value;
                while (headTail[1] != null) {
                    splitString(headTail[1].toCharArray(), headTail);
                    deque.add(headTail[0]);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // invalid value
                LOGGER.warn("Log4jMDCAdapter: invalid stack value {} for key {}.", value, key);
            }
        }
        return deque;
    }

    @Override
    public void clearDequeByKey(String key) {
        if (Strings.isEmpty(key)) {
            ThreadContext.clearStack();
        } else {
            ThreadContext.remove(key);
        }
    }
}
