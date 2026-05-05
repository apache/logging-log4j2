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
package org.apache.logging.log4j.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TransformTest {

    static Stream<Arguments> testEscapeHtmlTags() {
        final char replacement = '\uFFFD';
        return Stream.of(
                // Empty
                Arguments.of("", ""),
                // characters that need to be escaped
                Arguments.of("<\"Salt&Peppa'\">", "&lt;&quot;Salt&amp;Peppa&#39;&quot;&gt;"),
                // control character replaced with U+FFFD
                Arguments.of("A" + (char) 0x01 + "B", "A" + replacement + "B"),
                // standalone low surrogate replaced with U+FFFD
                Arguments.of("low" + Character.MIN_SURROGATE + "surrogate", "low" + replacement + "surrogate"),
                Arguments.of(Character.MIN_SURROGATE + "low", replacement + "low"),
                // standalone high surrogate replaced with U+FFFD
                Arguments.of("high" + Character.MAX_SURROGATE + "surrogate", "high" + replacement + "surrogate"),
                Arguments.of(Character.MAX_SURROGATE + "high", replacement + "high"),
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
    void testEscapeHtmlTags(final String input, final String expected) {
        String actual = Transform.escapeHtmlTags(input);
        assertThat(actual).isEqualTo(expected);
    }

    static Stream<Arguments> testAppendEscapingCData() {
        final char replacement = '\uFFFD';
        return Stream.of(
                // Empty
                Arguments.of("", ""),
                // characters that need to be escaped
                Arguments.of("<\"Salt&Peppa'\">", "<\"Salt&Peppa'\">"),
                // control character replaced with U+FFFD
                Arguments.of("A" + (char) 0x01 + "B", "A" + replacement + "B"),
                // standalone low surrogate replaced with U+FFFD
                Arguments.of("low" + Character.MIN_SURROGATE + "surrogate", "low" + replacement + "surrogate"),
                Arguments.of(Character.MIN_SURROGATE + "low", replacement + "low"),
                // standalone high surrogate replaced with U+FFFD
                Arguments.of("high" + Character.MAX_SURROGATE + "surrogate", "high" + replacement + "surrogate"),
                Arguments.of(Character.MAX_SURROGATE + "high", replacement + "high"),
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
    void testAppendEscapingCData(final String input, final String expected) {
        StringBuilder cdata = new StringBuilder();
        Transform.appendEscapingCData(cdata, input);
        assertThat(cdata.toString()).isEqualTo(expected);
    }
}
