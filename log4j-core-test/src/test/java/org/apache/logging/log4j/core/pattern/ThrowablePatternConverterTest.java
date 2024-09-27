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
import static org.assertj.core.api.Assertions.assertThat;

import foo.TestFriendlyException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link ThrowablePatternConverter} tests.
 */
public class ThrowablePatternConverterTest {

    private static final String NEWLINE = System.lineSeparator();

    private static final Throwable EXCEPTION = TestFriendlyException.INSTANCE;

    private static final StackTraceElement THROWING_METHOD = EXCEPTION.getStackTrace()[0];

    private static final PatternParser PATTERN_PARSER = PatternLayout.createPatternParser(null);

    private static final Level LEVEL = Level.FATAL;

    static final class SeparatorTestCase {

        final String patternAddendum;

        private final String conversionEnding;

        private SeparatorTestCase(final String patternAddendum, final String conversionEnding) {
            this.patternAddendum = patternAddendum;
            this.conversionEnding = conversionEnding;
        }

        @Override
        public String toString() {
            return String.format("{patternAddendum=`%s`, conversionEnding=`%s`}", patternAddendum, conversionEnding);
        }
    }

    static Stream<SeparatorTestCase> separatorTestCases() {
        final String level = LEVEL.toString();
        return Stream.of(
                // Only separators
                new SeparatorTestCase("{separator()}", ""),
                new SeparatorTestCase("{separator(#)}", "#"),
                // Only suffixes
                new SeparatorTestCase("{suffix()}", NEWLINE),
                new SeparatorTestCase("{suffix(~)}", " ~" + NEWLINE),
                new SeparatorTestCase("{suffix(%level)}", " " + level + NEWLINE),
                new SeparatorTestCase("{suffix(%rEx)}", NEWLINE),
                // Both separators and suffixes
                new SeparatorTestCase("{separator()}{suffix()}", ""),
                new SeparatorTestCase("{separator()}{suffix(~)}", " ~"),
                new SeparatorTestCase("{separator()}{suffix(%level)}", " " + level),
                new SeparatorTestCase("{separator()}{suffix(%rEx)}", ""),
                new SeparatorTestCase("{separator(#)}{suffix()}", "#"),
                new SeparatorTestCase("{separator(#)}{suffix(~)}", " ~#"),
                new SeparatorTestCase("{separator(#)}{suffix(%level)}", " " + level + "#"),
                new SeparatorTestCase("{separator(#)}{suffix(%rEx)}", "#"));
    }

    @Nested
    class PropertyTest extends AbstractPropertyTest {

        PropertyTest() {
            super("%ex");
        }
    }

    abstract static class AbstractPropertyTest {

        private final String patternPrefix;

        AbstractPropertyTest(final String patternPrefix) {
            this.patternPrefix = patternPrefix;
        }

        @ParameterizedTest
        @MethodSource("org.apache.logging.log4j.core.pattern.ThrowablePatternConverterTest#separatorTestCases")
        void message_should_be_rendered(final SeparatorTestCase separatorTestCase) {
            assertConversion(separatorTestCase, "{short.message}", EXCEPTION.getMessage());
        }

        @ParameterizedTest
        @MethodSource("org.apache.logging.log4j.core.pattern.ThrowablePatternConverterTest#separatorTestCases")
        void localizedMessage_should_be_rendered(final SeparatorTestCase separatorTestCase) {
            assertConversion(separatorTestCase, "{short.localizedMessage}", EXCEPTION.getLocalizedMessage());
        }

        @ParameterizedTest
        @MethodSource("org.apache.logging.log4j.core.pattern.ThrowablePatternConverterTest#separatorTestCases")
        void className_should_be_rendered(final SeparatorTestCase separatorTestCase) {
            assertConversion(separatorTestCase, "{short.className}", THROWING_METHOD.getClassName());
        }

        @ParameterizedTest
        @MethodSource("org.apache.logging.log4j.core.pattern.ThrowablePatternConverterTest#separatorTestCases")
        void methodName_should_be_rendered(final SeparatorTestCase separatorTestCase) {
            assertConversion(separatorTestCase, "{short.methodName}", THROWING_METHOD.getMethodName());
        }

        @ParameterizedTest
        @MethodSource("org.apache.logging.log4j.core.pattern.ThrowablePatternConverterTest#separatorTestCases")
        void lineNumber_should_be_rendered(final SeparatorTestCase separatorTestCase) {
            assertConversion(separatorTestCase, "{short.lineNumber}", THROWING_METHOD.getLineNumber() + "");
        }

        @ParameterizedTest
        @MethodSource("org.apache.logging.log4j.core.pattern.ThrowablePatternConverterTest#separatorTestCases")
        void fileName_should_be_rendered(final SeparatorTestCase separatorTestCase) {
            assertConversion(separatorTestCase, "{short.fileName}", THROWING_METHOD.getFileName());
        }

        private void assertConversion(
                final SeparatorTestCase separatorTestCase, final String pattern, final Object expectedOutput) {
            final String effectivePattern = patternPrefix + pattern + separatorTestCase.patternAddendum;
            final String output = convert(effectivePattern);
            final String effectiveExpectedOutput = expectedOutput + separatorTestCase.conversionEnding;
            assertThat(output)
                    .as(
                            "pattern=`%s`, separatorTestCase=%s, expectedOutput=`%s`",
                            pattern, separatorTestCase, expectedOutput)
                    .isEqualTo(effectiveExpectedOutput);
        }
    }

    static final class DepthTestCase {

        final SeparatorTestCase separatorTestCase;

        final int maxLineCount;

        private DepthTestCase(final SeparatorTestCase separatorTestCase, final int maxLineCount) {
            this.separatorTestCase = separatorTestCase;
            this.maxLineCount = maxLineCount;
        }

        @Override
        public String toString() {
            return String.format("{separatorTestCase=%s, maxLineCount=%d}", separatorTestCase, maxLineCount);
        }
    }

    static Stream<DepthTestCase> depthTestCases() {
        return separatorTestCases().flatMap(separatorTestCase -> maxLineCounts()
                .map(maxLineCount -> new DepthTestCase(separatorTestCase, maxLineCount)));
    }

    static Stream<Integer> maxLineCounts() {
        return Stream.of(0, 1, 2, 3, 4, 5, 10, 15, 20, Integer.MAX_VALUE);
    }

    static Stream<String> fullStackTracePatterns() {
        return Stream.of("", "{}", "{full}", "{" + Integer.MAX_VALUE + "}", "{separator(" + NEWLINE + ")}");
    }

    @Nested
    class StackTraceTest extends AbstractStackTraceTest {

        StackTraceTest() {
            super("%ex");
        }

        // This test does not provide `separator` and `suffix` options, since the reference output will be obtained from
        // `Throwable#printStackTrace()`, which doesn't take these into account.
        @ParameterizedTest
        @MethodSource("org.apache.logging.log4j.core.pattern.ThrowablePatternConverterTest#fullStackTracePatterns")
        void full_output_should_match_Throwable_printStackTrace(final String pattern) {
            final String expectedStackTrace = renderStackTraceUsingJava();
            final String effectivePattern = patternPrefix + pattern;
            final String actualStackTrace = convert(effectivePattern);
            assertThat(actualStackTrace).as("pattern=`%s`", effectivePattern).isEqualTo(expectedStackTrace);
        }

        // This test does not provide `separator` and `suffix` options, since the reference output will be obtained from
        // `Throwable#printStackTrace()`, which doesn't take these into account.
        @ParameterizedTest
        @MethodSource("org.apache.logging.log4j.core.pattern.ThrowablePatternConverterTest#maxLineCounts")
        void depth_limited_output_should_match_Throwable_printStackTrace(final int maxLineCount) {
            final String expectedStackTrace = renderStackTraceUsingJava(maxLineCount);
            final String effectivePattern = patternPrefix + '{' + maxLineCount + '}';
            final String actualStackTrace = convert(effectivePattern);
            assertThat(actualStackTrace).as("pattern=`%s`", effectivePattern).isEqualTo(expectedStackTrace);
        }

        private String renderStackTraceUsingJava(final int maxLineCount) {
            if (maxLineCount == 0) {
                return "";
            }
            final String stackTrace = renderStackTraceUsingJava();
            if (maxLineCount == Integer.MAX_VALUE) {
                return stackTrace;
            }
            return limitLines(stackTrace, maxLineCount);
        }

        private String limitLines(final String text, final int maxLineCount) {
            final StringBuilder buffer = new StringBuilder();
            int lineCount = 0;
            int startIndex = 0;
            int newlineIndex;
            while (lineCount < maxLineCount && (newlineIndex = text.indexOf(NEWLINE, startIndex)) != -1) {
                final int endIndex = newlineIndex + NEWLINE.length();
                final String line = text.substring(startIndex, endIndex);
                buffer.append(line);
                lineCount++;
                startIndex = endIndex;
            }
            return buffer.toString();
        }

        private String renderStackTraceUsingJava() {
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

        @ParameterizedTest
        @MethodSource("org.apache.logging.log4j.core.pattern.ThrowablePatternConverterTest#depthTestCases")
        void depth_limited_output_should_match(final DepthTestCase depthTestCase) {
            final String pattern = String.format(
                    "%s{%d}%s",
                    patternPrefix, depthTestCase.maxLineCount, depthTestCase.separatorTestCase.patternAddendum);
            assertStackTraceLines(
                    depthTestCase,
                    pattern,
                    asList(
                            "foo.TestFriendlyException: r [localized]",
                            "	at foo.TestFriendlyException.create(TestFriendlyException.java:0)",
                            "	at foo.TestFriendlyException.<clinit>(TestFriendlyException.java:0)",
                            "	at org.apache.logging.log4j.core.pattern.ThrowablePatternConverterTest.<clinit>(ThrowablePatternConverterTest.java:0)",
                            "	Suppressed: foo.TestFriendlyException: r_s [localized]",
                            "		at foo.TestFriendlyException.create(TestFriendlyException.java:0)",
                            "		at foo.TestFriendlyException.create(TestFriendlyException.java:0)",
                            "		... 2 more",
                            "		Suppressed: foo.TestFriendlyException: r_s_s [localized]",
                            "			at foo.TestFriendlyException.create(TestFriendlyException.java:0)",
                            "			at foo.TestFriendlyException.create(TestFriendlyException.java:0)",
                            "			... 3 more",
                            "	Caused by: foo.TestFriendlyException: r_s_c [localized]",
                            "		at foo.TestFriendlyException.create(TestFriendlyException.java:0)",
                            "		at foo.TestFriendlyException.create(TestFriendlyException.java:0)",
                            "		... 3 more",
                            "Caused by: foo.TestFriendlyException: r_c [localized]",
                            "	at foo.TestFriendlyException.create(TestFriendlyException.java:0)",
                            "	at foo.TestFriendlyException.create(TestFriendlyException.java:0)",
                            "	... 2 more",
                            "	Suppressed: foo.TestFriendlyException: r_c_s [localized]",
                            "		at foo.TestFriendlyException.create(TestFriendlyException.java:0)",
                            "		at foo.TestFriendlyException.create(TestFriendlyException.java:0)",
                            "		... 3 more",
                            "	Caused by: [CIRCULAR REFERENCE: foo.TestFriendlyException: r_c [localized]]",
                            "Caused by: foo.TestFriendlyException: r_c_c [localized]",
                            "	at foo.TestFriendlyException.create(TestFriendlyException.java:0)",
                            "	at foo.TestFriendlyException.create(TestFriendlyException.java:0)",
                            "	... 3 more",
                            "Caused by: [CIRCULAR REFERENCE: foo.TestFriendlyException: r_c [localized]]"));
        }

        @ParameterizedTest
        @MethodSource("org.apache.logging.log4j.core.pattern.ThrowablePatternConverterTest#depthTestCases")
        void depth_and_package_limited_output_should_match_1(final DepthTestCase depthTestCase) {
            final String pattern = String.format(
                    "%s{%d}{filters(foo)}%s",
                    patternPrefix, depthTestCase.maxLineCount, depthTestCase.separatorTestCase.patternAddendum);
            assertStackTraceLines(
                    depthTestCase,
                    pattern,
                    asList(
                            "foo.TestFriendlyException: r [localized]",
                            "	... suppressed 2 lines",
                            "	at org.apache.logging.log4j.core.pattern.ThrowablePatternConverterTest.<clinit>(ThrowablePatternConverterTest.java:0)",
                            "	Suppressed: foo.TestFriendlyException: r_s [localized]",
                            "		... suppressed 2 lines",
                            "		... 2 more",
                            "		Suppressed: foo.TestFriendlyException: r_s_s [localized]",
                            "			... suppressed 2 lines",
                            "			... 3 more",
                            "	Caused by: foo.TestFriendlyException: r_s_c [localized]",
                            "		... suppressed 2 lines",
                            "		... 3 more",
                            "Caused by: foo.TestFriendlyException: r_c [localized]",
                            "	... suppressed 2 lines",
                            "	... 2 more",
                            "	Suppressed: foo.TestFriendlyException: r_c_s [localized]",
                            "		... suppressed 2 lines",
                            "		... 3 more",
                            "	Caused by: [CIRCULAR REFERENCE: foo.TestFriendlyException: r_c [localized]]",
                            "Caused by: foo.TestFriendlyException: r_c_c [localized]",
                            "	... suppressed 2 lines",
                            "	... 3 more",
                            "Caused by: [CIRCULAR REFERENCE: foo.TestFriendlyException: r_c [localized]]"));
        }

        @ParameterizedTest
        @MethodSource("org.apache.logging.log4j.core.pattern.ThrowablePatternConverterTest#depthTestCases")
        void depth_and_package_limited_output_should_match_2(final DepthTestCase depthTestCase) {
            final String pattern = String.format(
                    "%s{%d}{filters(org.apache)}%s",
                    patternPrefix, depthTestCase.maxLineCount, depthTestCase.separatorTestCase.patternAddendum);
            assertStackTraceLines(
                    depthTestCase,
                    pattern,
                    asList(
                            "foo.TestFriendlyException: r [localized]",
                            "	at foo.TestFriendlyException.create(TestFriendlyException.java:0)",
                            "	at foo.TestFriendlyException.<clinit>(TestFriendlyException.java:0)",
                            "	...",
                            "	Suppressed: foo.TestFriendlyException: r_s [localized]",
                            "		at foo.TestFriendlyException.create(TestFriendlyException.java:0)",
                            "		at foo.TestFriendlyException.create(TestFriendlyException.java:0)",
                            "		... 2 more",
                            "		Suppressed: foo.TestFriendlyException: r_s_s [localized]",
                            "			at foo.TestFriendlyException.create(TestFriendlyException.java:0)",
                            "			at foo.TestFriendlyException.create(TestFriendlyException.java:0)",
                            "			... 3 more",
                            "	Caused by: foo.TestFriendlyException: r_s_c [localized]",
                            "		at foo.TestFriendlyException.create(TestFriendlyException.java:0)",
                            "		at foo.TestFriendlyException.create(TestFriendlyException.java:0)",
                            "		... 3 more",
                            "Caused by: foo.TestFriendlyException: r_c [localized]",
                            "	at foo.TestFriendlyException.create(TestFriendlyException.java:0)",
                            "	at foo.TestFriendlyException.create(TestFriendlyException.java:0)",
                            "	... 2 more",
                            "	Suppressed: foo.TestFriendlyException: r_c_s [localized]",
                            "		at foo.TestFriendlyException.create(TestFriendlyException.java:0)",
                            "		at foo.TestFriendlyException.create(TestFriendlyException.java:0)",
                            "		... 3 more",
                            "	Caused by: [CIRCULAR REFERENCE: foo.TestFriendlyException: r_c [localized]]",
                            "Caused by: foo.TestFriendlyException: r_c_c [localized]",
                            "	at foo.TestFriendlyException.create(TestFriendlyException.java:0)",
                            "	at foo.TestFriendlyException.create(TestFriendlyException.java:0)",
                            "	... 3 more",
                            "Caused by: [CIRCULAR REFERENCE: foo.TestFriendlyException: r_c [localized]]"));
        }
    }

    abstract static class AbstractStackTraceTest {

        final String patternPrefix;

        AbstractStackTraceTest(final String patternPrefix) {
            this.patternPrefix = patternPrefix;
        }

        @ParameterizedTest
        @MethodSource("org.apache.logging.log4j.core.pattern.ThrowablePatternConverterTest#separatorTestCases")
        void none_output_should_be_empty(final SeparatorTestCase separatorTestCase) {
            final String effectivePattern = patternPrefix + "{none}" + separatorTestCase.patternAddendum;
            final String stackTrace = convert(effectivePattern);
            assertThat(stackTrace).as("pattern=`%s`", effectivePattern).isEmpty();
        }

        void assertStackTraceLines(
                @Nullable final DepthTestCase depthTestCase,
                final String pattern,
                final List<String> expectedStackTraceLines) {
            final String actualStackTrace = convert(pattern);
            final int maxLineCount;
            final String conversionEnding;
            if (depthTestCase == null) {
                maxLineCount = Integer.MAX_VALUE;
                conversionEnding = NEWLINE;
            } else {
                maxLineCount = depthTestCase.maxLineCount;
                conversionEnding = depthTestCase.separatorTestCase.conversionEnding;
            }
            final String expectedStackTrace = expectedStackTraceLines.stream()
                    .limit(maxLineCount)
                    .map(expectedStackTraceLine -> expectedStackTraceLine + conversionEnding)
                    .collect(Collectors.joining());
            final String truncatedActualStackTrace = truncateStackTraceLineNumbers(actualStackTrace);
            final String truncatedExpectedStackTrace = truncateStackTraceLineNumbers(expectedStackTrace);
            assertThat(truncatedActualStackTrace)
                    .as("depthTestCase=%s, pattern=`%s`", depthTestCase, pattern)
                    .isEqualTo(truncatedExpectedStackTrace);
        }

        private static String truncateStackTraceLineNumbers(final String stackTrace) {
            return stackTrace.replaceAll("\\.java:[0-9]+\\)", ".java:0)");
        }
    }

    private static String convert(final String pattern) {
        final List<PatternFormatter> patternFormatters = PATTERN_PARSER.parse(pattern, false, true, true);
        assertThat(patternFormatters).hasSize(1);
        final PatternFormatter patternFormatter = patternFormatters.get(0);
        final LogEvent logEvent =
                Log4jLogEvent.newBuilder().setThrown(EXCEPTION).setLevel(LEVEL).build();
        final StringBuilder buffer = new StringBuilder();
        patternFormatter.format(logEvent, buffer);
        return buffer.toString();
    }
}
