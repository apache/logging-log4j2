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
package org.apache.logging.log4j.util;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

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
 * <p>
 * This class manages a small thread-local ring buffer of StringBuilders.
 * Each time one of the {@code box()} methods is called, the next slot in the ring buffer is used, until the ring
 * buffer is full and the first slot is reused. By default the Unbox ring buffer has 32 slots, so user code can
 * have up to 32 boxed primitives in a single logger call.
 * </p>
 * <p>
 * If more slots are required, set system property {@code log4j.unbox.ringbuffer.size} to the desired ring buffer size.
 * Note that the specified number will be rounded up to the nearest power of 2.
 * </p>
 * @since 2.6
 */
@PerformanceSensitive("allocation")
public class Unbox {
    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final int BITS_PER_INT = 32;
    private static final int RINGBUFFER_MIN_SIZE = 32;
    private static final int RINGBUFFER_SIZE = calculateRingBufferSize("log4j.unbox.ringbuffer.size");
    private static final int MASK = RINGBUFFER_SIZE - 1;

    /**
     * State implementation that only puts JDK classes in ThreadLocals, so this is safe to be used from
     * web applications. Web application containers have thread pools that may hold on to ThreadLocal objects
     * after the application was stopped. This may prevent the classes of the application from being unloaded,
     * causing memory leaks.
     * <p>
     * Such memory leaks will not occur if only JDK classes are stored in ThreadLocals.
     * </p>
     */
    /*
     * https://errorprone.info/bugpattern/ThreadLocalUsage
     * Instance thread locals are not a problem here, since this class is a singleton.
     */
    @SuppressWarnings("ThreadLocalUsage")
    private static class WebSafeState {
        private final ThreadLocal<StringBuilder[]> ringBuffer = new ThreadLocal<>();
        private final ThreadLocal<int[]> current = new ThreadLocal<>();

        public StringBuilder getStringBuilder() {
            StringBuilder[] array = ringBuffer.get();
            if (array == null) {
                array = new StringBuilder[RINGBUFFER_SIZE];
                for (int i = 0; i < array.length; i++) {
                    array[i] = new StringBuilder(21);
                }
                ringBuffer.set(array);
                current.set(new int[1]);
            }
            final int[] index = current.get();
            final StringBuilder result = array[MASK & index[0]++];
            result.setLength(0);
            return result;
        }
    }

    private static class State {
        private final StringBuilder[] ringBuffer = new StringBuilder[RINGBUFFER_SIZE];
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
    }

    private static ThreadLocal<State> threadLocalState = new ThreadLocal<>();
    private static WebSafeState webSafeState = new WebSafeState();

    private Unbox() {
        // this is a utility
    }

    private static int calculateRingBufferSize(final String propertyName) {
        final String userPreferredRBSize =
                PropertiesUtil.getProperties().getStringProperty(propertyName, String.valueOf(RINGBUFFER_MIN_SIZE));
        try {
            int size = Integer.parseInt(userPreferredRBSize.trim());
            if (size < RINGBUFFER_MIN_SIZE) {
                size = RINGBUFFER_MIN_SIZE;
                LOGGER.warn(
                        "Invalid {} {}, using minimum size {}.",
                        propertyName,
                        userPreferredRBSize,
                        RINGBUFFER_MIN_SIZE);
            }
            return ceilingNextPowerOfTwo(size);
        } catch (final Exception ex) {
            LOGGER.warn(
                    "Invalid {} {}, using default size {}.", propertyName, userPreferredRBSize, RINGBUFFER_MIN_SIZE);
            return RINGBUFFER_MIN_SIZE;
        }
    }

    /**
     * Calculate the next power of 2, greater than or equal to x.
     * <p>
     * From Hacker's Delight, Chapter 3, Harry S. Warren Jr.
     *
     * @param x Value to round up
     * @return The next power of 2 from x inclusive
     */
    private static int ceilingNextPowerOfTwo(final int x) {
        return 1 << (BITS_PER_INT - Integer.numberOfLeadingZeros(x - 1));
    }

    /**
     * Returns a {@code StringBuilder} containing the text representation of the specified primitive value.
     * This method will not allocate temporary objects.
     *
     * @param value the value whose text representation to return
     * @return a {@code StringBuilder} containing the text representation of the specified primitive value
     */
    @PerformanceSensitive("allocation")
    public static StringBuilder box(final float value) {
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
    public static StringBuilder box(final double value) {
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
    public static StringBuilder box(final short value) {
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
    public static StringBuilder box(final int value) {
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
    public static StringBuilder box(final char value) {
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
    public static StringBuilder box(final long value) {
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
    public static StringBuilder box(final byte value) {
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
    public static StringBuilder box(final boolean value) {
        return getSB().append(value);
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
        return Constants.ENABLE_THREADLOCALS ? getState().getStringBuilder() : webSafeState.getStringBuilder();
    }

    /** For testing. */
    static int getRingbufferSize() {
        return RINGBUFFER_SIZE;
    }
}
