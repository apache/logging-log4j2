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
package org.apache.logging.log4j.core.pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.presentation.HexadecimalRepresentation.HEXA_REPRESENTATION;

import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class JAnsiTextRendererTest {

    public static Stream<Arguments> testRendering() {
        return Stream.of(
                // Use style names
                Arguments.of(
                        "KeyStyle=white ValueStyle=cyan,bold",
                        "@|KeyStyle key|@ = @|ValueStyle some value|@",
                        "\u001b[37mkey\u001b[m = \u001b[36;1msome value\u001b[m"),
                // Use AnsiEscape codes directly
                Arguments.of(
                        "",
                        "@|white key|@ = @|cyan,bold some value|@",
                        "\u001b[37mkey\u001b[m = \u001b[36;1msome value\u001b[m"),
                // Return broken escapes as is
                Arguments.of("", "Hello @|crazy|@ world!", "Hello @|crazy|@ world!"),
                Arguments.of("", "Hello @|world!", "Hello @|world!"));
    }

    @ParameterizedTest
    @MethodSource
    void testRendering(final String format, final String text, final String expected) {
        final JAnsiTextRenderer renderer = new JAnsiTextRenderer(new String[] {"ansi", format}, Collections.emptyMap());
        final StringBuilder actual = new StringBuilder();
        renderer.render(new StringBuilder(text), actual);
        assertThat(actual.toString())
                .as("Rendering text '%s'", text)
                .withRepresentation(HEXA_REPRESENTATION)
                .isEqualTo(expected);
    }
}
