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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class Log4jMDCAdapterTest {

    private static final Log4jMDCAdapter MDC_ADAPTER = new Log4jMDCAdapter();
    private static final int SURROGATE_CODE_POINT = 65536;
    private static final String SURROGATES;
    static {
        final char[] surrogatePair = new char[2];
        Character.toChars(SURROGATE_CODE_POINT, surrogatePair, 0);
        SURROGATES = new String(surrogatePair);
    }
    private static final String KEY = "Log4j2";

    private static Deque<String> createDeque(int size) {
        final Deque<String> result = new ArrayDeque<>(size);
        IntStream.range(0, size).mapToObj(Integer::toString).forEach(result::addLast);
        return result;
    }

    static Stream<String> keys() {
        return Stream.of(KEY, "", null);
    }

    @ParameterizedTest
    @MethodSource("keys")
    public void testPushPopByKey(final String key) {
        MDC_ADAPTER.clearDequeByKey(key);

        final Deque<String> expectedValues = createDeque(100);
        expectedValues.forEach(value -> MDC_ADAPTER.pushByKey(key, value));
        final Deque<String> actualValues = MDC_ADAPTER.getCopyOfDequeByKey(key);
        Iterator<String> expectedIterator = expectedValues.descendingIterator();
        final Iterator<String> actualIterator = actualValues.iterator();
        while (expectedIterator.hasNext()) {
            assertTrue(actualIterator.hasNext(), "same length");
            assertEquals(expectedIterator.next(), actualIterator.next(), "same values");
        }
        assertFalse(actualIterator.hasNext(), "same length");

        expectedIterator = expectedValues.descendingIterator();
        while (expectedIterator.hasNext()) {
            assertEquals(expectedIterator.next(), MDC_ADAPTER.popByKey(key));
        }
    }

    static Stream<Arguments> expectedQuoted() {
        return Stream.of(Arguments.of("hello", "\"hello\""),
                Arguments.of("\"hello,\"", "\"\\\"hello,\\\"\""),
                Arguments.of(SURROGATES, "\"" + SURROGATES + "\""),
                Arguments.of("\\hello\"\\, hello\"\\", "\"\\\\hello\\\"\\\\, hello\\\"\\\\\""));
    }

    @ParameterizedTest
    @MethodSource("expectedQuoted")
    public void testQuoteString(final String unquoted, final String quoted) {
        final StringBuilder sb = new StringBuilder();
        Log4jMDCAdapter.quoteString(unquoted, sb);
        assertEquals(quoted, sb.toString());
        sb.setLength(0);
        sb.append("[").append(quoted).append("]");
        final String[] headTail = new String[2];
        Log4jMDCAdapter.splitString(sb.toString().toCharArray(), headTail);
        assertEquals(unquoted, headTail[0]);
        assertNull(headTail[1]);
    }

    static Stream<String> invalidValues() {
        return Stream.of("[\"never ending value", "hello", "[]", "[lala]", "", null);
    }

    /**
     * If the user uses the same key as string and stack value, the implementation
     * should at least not throw any exceptions.
     */
    @ParameterizedTest
    @MethodSource("invalidValues")
    public void testDoesNotThrowOnInvalid(final String value) {
        MDC_ADAPTER.put("Log4j2", value);
        assertDoesNotThrow(() -> MDC_ADAPTER.popByKey(KEY));
        MDC_ADAPTER.put("Log4j2", value);
        assertDoesNotThrow(() -> MDC_ADAPTER.getCopyOfDequeByKey(KEY));
        MDC_ADAPTER.put("Log4j2", value);
        // this actually overrides the value
        assertDoesNotThrow(() -> MDC_ADAPTER.pushByKey(KEY, "anything"));
        assertEquals("anything", MDC_ADAPTER.popByKey(KEY));
    }
}
