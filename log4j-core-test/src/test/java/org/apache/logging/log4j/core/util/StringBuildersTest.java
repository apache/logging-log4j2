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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.apache.logging.log4j.core.util.internal.StringBuilders;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class StringBuildersTest {

    static Stream<Arguments> testTruncateLines_dataSource() {
        return Stream.of(
                Arguments.of("abc | def | ghi | jkl | ", " | ", 2, "abc | def | "),
                Arguments.of("abc\ndef\nghi\njkl\n", "\n", 2, "abc\ndef\n"),
                Arguments.of("abc | def | ghi | jkl | ", " | ", 4, "abc | def | ghi | jkl | "),
                Arguments.of("abc | def | ghi | jkl | ", " | ", null, "abc | def | ghi | jkl | "),
                Arguments.of("abc | def | ghi | jkl | ", " | ", Integer.MAX_VALUE, "abc | def | ghi | jkl | "),
                Arguments.of("abc | def | ghi | jkl | ", " | ", 10, "abc | def | ghi | jkl | "),
                Arguments.of("abc | def | ghi | jkl | ", "", 2, "abc | def | ghi | jkl | "));
    }

    @ParameterizedTest
    @MethodSource("testTruncateLines_dataSource")
    public void testTruncateLines(String original, String lineSeparator, Integer maxLine, String expected) {
        final StringBuilder sb = new StringBuilder(original);
        StringBuilders.truncateLines(sb, lineSeparator, maxLine);
        assertEquals(expected, sb.toString());
    }
}
