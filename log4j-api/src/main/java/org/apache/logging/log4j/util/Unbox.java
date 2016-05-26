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
package org.apache.logging.log4j.util;

/**
 * Utility for preventing primitive parameter values from being auto-boxed. Auto-boxing creates temporary objects
 * which contribute to pressure on the garbage collector. With this utility users can convert primitive values directly
 * into text without allocating temporary objects.
 * <p>
 * Example usage:
 * </p><pre>
 * import static org.apache.logging.log4j.util.Unbox.box;
 * ...
 * long longValue = 123456L;
 * double doubleValue = 3.14;
 * // prevent primitive values from being auto-boxed
 * logger.debug("Long value={}, double value={}", box(longValue), box(doubleValue));
 * </pre>
 */
@PerformanceSensitive("allocation")
public class Unbox {
    private static final int MASK = 16 - 1;

    private static class State {
        private final StringBuilder[] ringBuffer = new StringBuilder[16];
        private int current;
        State() {
            for (int i = 0; i < ringBuffer.length; i++) {
                ringBuffer[i] = new StringBuilder(21);
            }
        }

        public StringBuilder getStringBuilder() {
            final StringBuilder result = ringBuffer[MASK & current++];
            result.setLength(0);
            return result;
        }

        public boolean isBoxedPrimitive(final StringBuilder text) {
            for (int i = 0; i < ringBuffer.length; i++) {
                if (text == ringBuffer[i]) {
                    return true;
                }
            }
            return false;
        }
    }
    private static ThreadLocal<State> threadLocalState = new ThreadLocal<>();

    /**
     * Returns a {@code StringBuilder} containing the text representation of the specified primitive value.
     * This method will not allocate temporary objects.
     *
     * @param value the value whose text representation to return
     * @return a {@code StringBuilder} containing the text representation of the specified primitive value
     */
    @PerformanceSensitive("allocation")
    public static StringBuilder box(float value) {
        return getSB().append(value);
    }

    /**
     * Returns a {@code StringBuilder} containing the text representation of the specified primitive value.
     * This method will not allocate temporary objects.
     *
     * @param value the value whose text representation to return
     * @return a {@code StringBuilder} containing the text representation of the specified primitive value
     */
    @PerformanceSensitive("allocation")
    public static StringBuilder box(double value) {
        return getSB().append(value);
    }

    /**
     * Returns a {@code StringBuilder} containing the text representation of the specified primitive value.
     * This method will not allocate temporary objects.
     *
     * @param value the value whose text representation to return
     * @return a {@code StringBuilder} containing the text representation of the specified primitive value
     */
    @PerformanceSensitive("allocation")
    public static StringBuilder box(short value) {
        return getSB().append(value);
    }

    /**
     * Returns a {@code StringBuilder} containing the text representation of the specified primitive value.
     * This method will not allocate temporary objects.
     *
     * @param value the value whose text representation to return
     * @return a {@code StringBuilder} containing the text representation of the specified primitive value
     */
    @PerformanceSensitive("allocation")
    public static StringBuilder box(int value) {
        return getSB().append(value);
    }

    /**
     * Returns a {@code StringBuilder} containing the text representation of the specified primitive value.
     * This method will not allocate temporary objects.
     *
     * @param value the value whose text representation to return
     * @return a {@code StringBuilder} containing the text representation of the specified primitive value
     */
    @PerformanceSensitive("allocation")
    public static StringBuilder box(char value) {
        return getSB().append(value);
    }

    /**
     * Returns a {@code StringBuilder} containing the text representation of the specified primitive value.
     * This method will not allocate temporary objects.
     *
     * @param value the value whose text representation to return
     * @return a {@code StringBuilder} containing the text representation of the specified primitive value
     */
    @PerformanceSensitive("allocation")
    public static StringBuilder box(long value) {
        return getSB().append(value);
    }

    /**
     * Returns a {@code StringBuilder} containing the text representation of the specified primitive value.
     * This method will not allocate temporary objects.
     *
     * @param value the value whose text representation to return
     * @return a {@code StringBuilder} containing the text representation of the specified primitive value
     */
    @PerformanceSensitive("allocation")
    public static StringBuilder box(byte value) {
        return getSB().append(value);
    }

    /**
     * Returns a {@code StringBuilder} containing the text representation of the specified primitive value.
     * This method will not allocate temporary objects.
     *
     * @param value the value whose text representation to return
     * @return a {@code StringBuilder} containing the text representation of the specified primitive value
     */
    @PerformanceSensitive("allocation")
    public static StringBuilder box(boolean value) {
        return getSB().append(value);
    }

    public static boolean isBoxedPrimitive(final StringBuilder text) {
        return getState().isBoxedPrimitive(text);
    }

    private static State getState() {
        State state = threadLocalState.get();
        if (state == null) {
            state = new State();
            threadLocalState.set(state);
        }
        return state;
    }

    private static StringBuilder getSB() {
        return getState().getStringBuilder();
    }
}
