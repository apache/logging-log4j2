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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests the StringBuilders class.
 */
class StringBuildersTest {
    @Test
    void trimToMaxSize() {
        final StringBuilder sb = new StringBuilder();
        final char[] value = new char[4 * 1024];
        sb.append(value);

        assertTrue(sb.length() > Constants.MAX_REUSABLE_MESSAGE_SIZE, "needs trimming");
        StringBuilders.trimToMaxSize(sb, Constants.MAX_REUSABLE_MESSAGE_SIZE);
        assertTrue(sb.length() <= Constants.MAX_REUSABLE_MESSAGE_SIZE, "trimmed OK");
    }

    @Test
    void trimToMaxSizeWithLargeCapacity() {
        final StringBuilder sb = new StringBuilder();
        final char[] value = new char[4 * 1024];
        sb.append(value);
        sb.setLength(0);

        assertTrue(sb.capacity() > Constants.MAX_REUSABLE_MESSAGE_SIZE, "needs trimming");
        StringBuilders.trimToMaxSize(sb, Constants.MAX_REUSABLE_MESSAGE_SIZE);
        assertTrue(sb.capacity() <= Constants.MAX_REUSABLE_MESSAGE_SIZE, "trimmed OK");
    }

    @Test
    void escapeJsonCharactersCorrectly() {
        final String jsonValueNotEscaped = "{\"field\n1\":\"value_1\"}";
        final String jsonValueEscaped = "{\\\"field\\n1\\\":\\\"value_1\\\"}";

        StringBuilder sb = new StringBuilder();
        sb.append(jsonValueNotEscaped);
        assertEquals(jsonValueNotEscaped, sb.toString());
        StringBuilders.escapeJson(sb, 0);
        assertEquals(jsonValueEscaped, sb.toString());

        sb = new StringBuilder();
        final String jsonValuePartiallyEscaped = "{\"field\n1\":\\\"value_1\\\"}";
        sb.append(jsonValueNotEscaped);
        assertEquals(jsonValueNotEscaped, sb.toString());
        StringBuilders.escapeJson(sb, 10);
        assertEquals(jsonValuePartiallyEscaped, sb.toString());
    }

    @Test
    void escapeJsonCharactersISOControl() {
        final String jsonValueNotEscaped = "{\"field\n1\":\"value" + (char) 0x8F + "_1\"}";
        final String jsonValueEscaped = "{\\\"field\\n1\\\":\\\"value\\u008F_1\\\"}";

        final StringBuilder sb = new StringBuilder();
        sb.append(jsonValueNotEscaped);
        assertEquals(jsonValueNotEscaped, sb.toString());
        StringBuilders.escapeJson(sb, 0);
        assertEquals(jsonValueEscaped, sb.toString());
    }

    static Stream<Arguments> escapeXmlCharactersCorrectly() {
        final char replacement = '\uFFFD';
        return Stream.of(
                // Empty
                Arguments.of("", ""),
                // characters that need to be escaped
                Arguments.of("<\"Salt&Peppa'\">", "&lt;&quot;Salt&amp;Peppa&apos;&quot;&gt;"),
                // control character replaced with U+FFFD
                Arguments.of("A" + (char) 0x01 + "B", "A" + replacement + "B"),
                // standalone low surrogate replaced with U+FFFD
                Arguments.of("low" + Character.MIN_LOW_SURROGATE + "surrogate", "low" + replacement + "surrogate"),
                Arguments.of(Character.MIN_LOW_SURROGATE + "low", replacement + "low"),
                // standalone high surrogate replaced with U+FFFD
                Arguments.of("high" + Character.MIN_HIGH_SURROGATE + "surrogate", "high" + replacement + "surrogate"),
                Arguments.of(Character.MIN_HIGH_SURROGATE + "high", replacement + "high"),
                // FFFE and FFFF
                Arguments.of("invalid\uFFFEchars", "invalid" + replacement + "chars"),
                Arguments.of("invalid\uFFFFchars", "invalid" + replacement + "chars"),
                // whitespace characters are preserved
                Arguments.of("tab\tnewline\ncr\r", "tab\tnewline\ncr\r"),
                // character beyond BMP (emoji) preserved as surrogate pair
                Arguments.of("emoji " + "\uD83D\uDE00" + " end", "emoji " + "\uD83D\uDE00" + " end"));
    }

    @ParameterizedTest
    @MethodSource
    void escapeXmlCharactersCorrectly(final String input, final String expected) {
        final StringBuilder sb = new StringBuilder();
        sb.append(input);
        assertThat(sb.toString()).isEqualTo(input);
        StringBuilders.escapeXml(sb, 0);
        assertThat(sb.toString()).isEqualTo(expected);
    }
}
