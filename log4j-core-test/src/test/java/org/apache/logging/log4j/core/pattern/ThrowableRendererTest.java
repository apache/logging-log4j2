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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import foo.TestFriendlyException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ThrowableRendererTest {

    static final String NEWLINE = System.lineSeparator();

    static final Exception EXCEPTION = TestFriendlyException.INSTANCE;

    @Test
    void output_should_match_Throwable_printStackTrace() {
        final String log4jOutput = renderStackTraceUsingLog4j(emptyList(), Integer.MAX_VALUE);
        final String javaOutput = renderStackTraceUsingJava();
        assertThat(log4jOutput).isEqualTo(javaOutput);
    }

    static int[] maxLineCounts() {
        return new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, Integer.MAX_VALUE};
    }

    @ParameterizedTest
    @MethodSource("maxLineCounts")
    void output_produced_with_maxLineCount_should_match_Throwable_printStackTrace(final int maxLineCount) {
        final String log4jOutput = renderStackTraceUsingLog4j(emptyList(), maxLineCount);
        final String javaOutput = renderStackTraceUsingJava(maxLineCount);
        assertThat(log4jOutput).isEqualTo(javaOutput);
    }

    @ParameterizedTest
    @MethodSource("maxLineCounts")
    void output_produced_with_ignoredPackageNames_and_maxLineCount_should_match_1(final int maxLineCount) {
        final String stackTrace = renderStackTraceUsingLog4j(singletonList("foo"), maxLineCount);
        assertStackTraceLineMatches(
                stackTrace,
                maxLineCount,
                asList(
                        "foo.TestFriendlyException: r",
                        "	... suppressed 2 lines",
                        "	at org.apache.logging.log4j.core.pattern.ThrowableRendererTest.<clinit>(ThrowableRendererTest.java:%DIGITS%)",
                        "	Suppressed: foo.TestFriendlyException: r_s",
                        "		... suppressed 2 lines",
                        "		... 2 more",
                        "		Suppressed: foo.TestFriendlyException: r_s_s",
                        "			... suppressed 2 lines",
                        "			... 3 more",
                        "	Caused by: foo.TestFriendlyException: r_s_c",
                        "		... suppressed 2 lines",
                        "		... 3 more",
                        "Caused by: foo.TestFriendlyException: r_c",
                        "	... suppressed 2 lines",
                        "	... 2 more",
                        "	Suppressed: foo.TestFriendlyException: r_c_s",
                        "		... suppressed 2 lines",
                        "		... 3 more",
                        "Caused by: foo.TestFriendlyException: r_c_c",
                        "	... suppressed 2 lines",
                        "	... 3 more"));
    }

    @ParameterizedTest
    @MethodSource("maxLineCounts")
    void output_produced_with_ignoredPackageNames_and_maxLineCount_should_match_2(final int maxLineCount) {
        final String stackTrace = renderStackTraceUsingLog4j(singletonList("org.apache"), maxLineCount);
        assertStackTraceLineMatches(
                stackTrace,
                maxLineCount,
                asList(
                        "foo.TestFriendlyException: r",
                        "	at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "	at foo.TestFriendlyException.<clinit>(TestFriendlyException.java:%DIGITS%)",
                        "	... ",
                        "	Suppressed: foo.TestFriendlyException: r_s",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "		... 2 more",
                        "		Suppressed: foo.TestFriendlyException: r_s_s",
                        "			at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "			at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "			... 3 more",
                        "	Caused by: foo.TestFriendlyException: r_s_c",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "		... 3 more",
                        "Caused by: foo.TestFriendlyException: r_c",
                        "	at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "	at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "	... 2 more",
                        "	Suppressed: foo.TestFriendlyException: r_c_s",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "		... 3 more",
                        "Caused by: foo.TestFriendlyException: r_c_c",
                        "	at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "	at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "	... 3 more"));
    }

    private static String renderStackTraceUsingLog4j(final List<String> ignoredPackageNames, final int maxLineCount) {
        final ThrowableRenderer<ThrowableRenderer.Context> renderer =
                new ThrowableRenderer<>(ignoredPackageNames, maxLineCount);
        final StringBuilder rendererOutputBuilder = new StringBuilder();
        renderer.renderThrowable(rendererOutputBuilder, EXCEPTION, NEWLINE);
        return rendererOutputBuilder.toString();
    }

    private static String renderStackTraceUsingJava(final int maxLineCount) {
        final String stackTrace = renderStackTraceUsingJava();
        if (maxLineCount == Integer.MAX_VALUE) {
            return stackTrace;
        }
        return splitLines(stackTrace).stream().limit(maxLineCount).collect(Collectors.joining());
    }

    @SuppressWarnings("SameParameterValue")
    private static String renderStackTraceUsingJava() {
        final Charset charset = StandardCharsets.UTF_8;
        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                final PrintStream printStream = new PrintStream(outputStream, false, charset.name())) {
            EXCEPTION.printStackTrace(printStream);
            printStream.flush();
            return new String(outputStream.toByteArray(), charset);
        } catch (final Exception error) {
            throw new RuntimeException(error);
        }
    }

    static void assertStackTraceLineMatches(
            final String actualStackTrace, final int maxLineCount, final List<String> expectedStackTraceLineRegexes) {
        final List<String> actualStackTraceLines = splitLines(actualStackTrace);
        final int expectedLineCount = Math.min(maxLineCount, expectedStackTraceLineRegexes.size());
        assertThat(actualStackTraceLines).hasSize(expectedLineCount);
        for (int lineIndex = 0; lineIndex < expectedLineCount; lineIndex++) {
            final String actualStackTraceLine = actualStackTraceLines.get(lineIndex);
            final String expectedStackTraceLineRegex = expectedStackTraceLineRegexes.get(lineIndex);
            final String interpolatedExpectedStackTraceLineRegex =
                    expectedStackTraceLineRegex.replaceAll("%DIGITS%", "\\\\E\\\\d+\\\\Q");
            assertThat(actualStackTraceLine)
                    .as("line at index %d of stack trace:%n%s", lineIndex, actualStackTrace)
                    .matches("^\\Q" + interpolatedExpectedStackTraceLineRegex + NEWLINE + "\\E$");
        }
    }

    private static List<String> splitLines(final String text) {
        final List<String> lines = new ArrayList<>();
        int startIndex = 0;
        int lfIndex;
        while ((lfIndex = text.indexOf('\n', startIndex)) != -1) {
            final String line = text.substring(startIndex, lfIndex + 1);
            lines.add(line);
            startIndex = lfIndex + 1;
        }
        return lines;
    }
}
