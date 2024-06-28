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


import java.util.stream.Stream;
import org.apache.logging.log4j.core.util.internal.StringBuilders;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class StringBuildersTest {

    static Stream<Arguments> testTruncateLines_happyCases() {
        return Stream.of(
                // maxOccurrenceCount < lines count
                Arguments.of("abc\ndef\nghi\njkl\n", "\n", 2, "abc\ndef\n"),
                // maxOccurrenceCount == lines count
                Arguments.of("abc|def|ghi|jkl|", "|", 4, "abc|def|ghi|jkl|"),
                // maxOccurrenceCount > lines count
                Arguments.of("abc|def|ghi|jkl|", "|", 10, "abc|def|ghi|jkl|"),
                // maxOccurrenceCount ==  Integer.MAX_VALUE
                Arguments.of("abc|def|ghi|jkl|", "|", Integer.MAX_VALUE, "abc|def|ghi|jkl|"),
                // maxOccurrenceCount ==  0
                Arguments.of("abc|def|ghi|jkl|", "|", 0, ""),
                // empty buffer
                Arguments.of("", "|", 2, ""),
                // empty delimiter
                Arguments.of("abc|def|ghi|jkl|", "", 2, "abc|def|ghi|jkl|"),
                // delimiter |
                Arguments.of("|", "|", 10, "|"),
                Arguments.of("||", "|", 10, "||"),
                Arguments.of("a|", "|", 10, "a|"),
                Arguments.of("|a", "|", 10, "|"),
                // delimiter ||
                Arguments.of("||", "||", 10, "||"),
                Arguments.of("|||", "||", 10, "||"),
                Arguments.of("||||", "||", 10, "||||"),
                Arguments.of("a|", "||", 10, ""),
                Arguments.of("a||", "||", 10, "a||"),
                Arguments.of("a|||", "||", 10, "a||"),
                Arguments.of("a||||", "||", 10, "a||||"),
                Arguments.of("|a", "||", 10, ""),
                Arguments.of("||a", "||", 10, "||"),
                Arguments.of("|||a", "||", 10, "||"),
                Arguments.of("||||a", "||", 10, "||||")
        );
    }

    @ParameterizedTest
    @MethodSource("testTruncateLines_happyCases")
    void testTruncateLinesHappyCases(String input, String delimiter, int maxOccurrenceCount, String expected) {
        final StringBuilder buffer = new StringBuilder(input);
        StringBuilders.truncateAfterDelimiter(buffer, delimiter, maxOccurrenceCount);
        assertThat(buffer.toString()).isEqualTo(expected);
    }

    static Stream<Arguments> testTruncateLines_failCases() {
        return Stream.of(
                // negative maxOccurrenceCount
                Arguments.of("abc\ndef\nghi\njkl\n", "\n", -1, IllegalArgumentException.class),
                // null buffer
                Arguments.of(null, "|", 10, NullPointerException.class),
                // null delimiter
                Arguments.of("abc|def|ghi|jkl|", null, 10, NullPointerException.class)
        );
    }

    @ParameterizedTest
    @MethodSource("testTruncateLines_failCases")
    void testTruncateLinesFailCases(String input, String delimiter, int maxOccurrenceCount, Class<Throwable> expected) {
        final StringBuilder buffer = input == null ? null : new StringBuilder(input);
        assertThatThrownBy(() -> StringBuilders.truncateAfterDelimiter(buffer, delimiter, maxOccurrenceCount)).isInstanceOf(expected);
    }
}
