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
import static org.apache.logging.log4j.util.Strings.LINE_SEPARATOR;
import static org.assertj.core.api.Assertions.assertThat;

import foo.TestFriendlyException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junitpioneer.jupiter.Issue;

/**
 * {@link ThrowablePatternConverter} tests.
 */
public class ThrowablePatternConverterTest {

    static final Throwable EXCEPTION = TestFriendlyException.INSTANCE;

    static final StackTraceElement THROWING_METHOD = EXCEPTION.getStackTrace()[0];

    private static final PatternParser PATTERN_PARSER = PatternLayout.createPatternParser(null);

    static final Level LEVEL = Level.FATAL;

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

    /**
     * Test exception whose {@link #toString()} is intentionally overridden to return a fixed value.
     */
    private static final class ToStringOverridingException extends RuntimeException {

        private static final ToStringOverridingException INSTANCE = new ToStringOverridingException();

        private ToStringOverridingException() {
            super(EXCEPTION);
        }

        @Override
        public String toString() {
            return "foo";
        }
    }

    static Stream<SeparatorTestCase> separatorTestCases() {
        final String level = LEVEL.toString();
        return Stream.of(
                // Only separators
                new SeparatorTestCase("{separator()}", ""),
                new SeparatorTestCase("{separator(#)}", "#"),
                // Only suffixes
                new SeparatorTestCase("{suffix()}", LINE_SEPARATOR),
                new SeparatorTestCase("{suffix(~)}", " ~" + LINE_SEPARATOR),
                new SeparatorTestCase("{suffix(%level)}", " " + level + LINE_SEPARATOR),
                new SeparatorTestCase("{suffix(%rEx)}", LINE_SEPARATOR),
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
            super("%ex", THROWING_METHOD);
        }
    }

    abstract static class AbstractPropertyTest {

        private final String patternPrefix;

        private final StackTraceElement throwingMethod;

        AbstractPropertyTest(final String patternPrefix, final StackTraceElement throwingMethod) {
            this.patternPrefix = patternPrefix;
            this.throwingMethod = throwingMethod;
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
            assertConversion(separatorTestCase, "{short.className}", throwingMethod.getClassName());
        }

        @ParameterizedTest
        @MethodSource("org.apache.logging.log4j.core.pattern.ThrowablePatternConverterTest#separatorTestCases")
        void methodName_should_be_rendered(final SeparatorTestCase separatorTestCase) {
            assertConversion(separatorTestCase, "{short.methodName}", throwingMethod.getMethodName());
        }

        @ParameterizedTest
        @MethodSource("org.apache.logging.log4j.core.pattern.ThrowablePatternConverterTest#separatorTestCases")
        void lineNumber_should_be_rendered(final SeparatorTestCase separatorTestCase) {
            assertConversion(separatorTestCase, "{short.lineNumber}", throwingMethod.getLineNumber() + "");
        }

        @ParameterizedTest
        @MethodSource("org.apache.logging.log4j.core.pattern.ThrowablePatternConverterTest#separatorTestCases")
        void fileName_should_be_rendered(final SeparatorTestCase separatorTestCase) {
            assertConversion(separatorTestCase, "{short.fileName}", throwingMethod.getFileName());
        }

        private void assertConversion(
                final SeparatorTestCase separatorTestCase, final String pattern, final Object expectedOutput) {
            final String effectivePattern = patternPrefix + pattern + separatorTestCase.patternAddendum;
            final String output = convert(effectivePattern);
            assertThat(output)
                    .as("pattern=`%s`, separatorTestCase=%s", pattern, separatorTestCase)
                    .isEqualTo(expectedOutput);
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
        return Stream.of("", "{}", "{full}", "{" + Integer.MAX_VALUE + "}", "{separator(" + LINE_SEPARATOR + ")}");
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

        @Test
        void full_output_should_use_custom_toString() {
            final Throwable exception = ToStringOverridingException.INSTANCE;
            final String expectedStackTrace = renderStackTraceUsingJava(exception);
            final String actualStackTrace = convert(patternPrefix, exception);
            assertThat(actualStackTrace).isEqualTo(expectedStackTrace);
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
            while (lineCount < maxLineCount && (newlineIndex = text.indexOf(LINE_SEPARATOR, startIndex)) != -1) {
                final int endIndex = newlineIndex + LINE_SEPARATOR.length();
                final String line = text.substring(startIndex, endIndex);
                buffer.append(line);
                lineCount++;
                startIndex = endIndex;
            }
            return buffer.toString();
        }

        private String renderStackTraceUsingJava() {
            return renderStackTraceUsingJava(EXCEPTION);
        }

        private String renderStackTraceUsingJava(final Throwable throwable) {
            final Charset charset = StandardCharsets.UTF_8;
            try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    final PrintStream printStream = new PrintStream(outputStream, false, charset.name())) {
                throwable.printStackTrace(printStream);
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
                            "	at " + TestFriendlyException.NAMED_MODULE_STACK_TRACE_ELEMENT,
                            "	at foo.TestFriendlyException.create(TestFriendlyException.java:0)",
                            "	at foo.TestFriendlyException.<clinit>(TestFriendlyException.java:0)",
                            "	at " + TestFriendlyException.ORG_APACHE_REPLACEMENT_STACK_TRACE_ELEMENT,
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
                            "	at " + TestFriendlyException.NAMED_MODULE_STACK_TRACE_ELEMENT,
                            "	... suppressed 2 lines",
                            "	at " + TestFriendlyException.ORG_APACHE_REPLACEMENT_STACK_TRACE_ELEMENT,
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
                    "%s{%d}{filters(bar)}%s",
                    patternPrefix, depthTestCase.maxLineCount, depthTestCase.separatorTestCase.patternAddendum);
            assertStackTraceLines(
                    depthTestCase,
                    pattern,
                    asList(
                            "foo.TestFriendlyException: r [localized]",
                            "	at " + TestFriendlyException.NAMED_MODULE_STACK_TRACE_ELEMENT,
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

        @Test
        @Issue("https://github.com/apache/logging-log4j2/issues/3873")
        void output_should_not_be_newline_prefixed() {
            final String pattern = "%p" + patternPrefix;
            final String stackTrace = convert(pattern);
            final String expectedStart =
                    String.format("%s%s", LEVEL, EXCEPTION.getClass().getCanonicalName());
            assertThat(stackTrace).as("pattern=`%s`", pattern).startsWith(expectedStart);
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
                conversionEnding = LINE_SEPARATOR;
            } else {
                maxLineCount = depthTestCase.maxLineCount;
                conversionEnding = depthTestCase.separatorTestCase.conversionEnding;
            }
            final String expectedStackTrace = expectedStackTraceLines.stream()
                    .limit(maxLineCount)
                    .map(expectedStackTraceLine -> expectedStackTraceLine + conversionEnding)
                    .collect(Collectors.joining());
            final String truncatedActualStackTrace = normalizeStackTrace(actualStackTrace, conversionEnding);
            final String truncatedExpectedStackTrace = normalizeStackTrace(expectedStackTrace, conversionEnding);
            assertThat(truncatedActualStackTrace)
                    .as("depthTestCase=%s, pattern=`%s`", depthTestCase, pattern)
                    .isEqualTo(truncatedExpectedStackTrace);
        }

        private static String normalizeStackTrace(final String stackTrace, final String conversionEnding) {
            return stackTrace
                    // Normalize line numbers
                    .replaceAll("\\.java:[0-9]+\\)", ".java:0)")
                    // Normalize extended stack trace resource information for Java Standard library classes.
                    // We replace the `~[?:1.8.0_422]` suffix of such classes with `~[?:0]`.
                    .replaceAll(" ~\\[\\?:[^]]+](\\Q" + conversionEnding + "\\E|$)", " ~[?:0]$1");
        }

        @RepeatedTest(10)
        @Issue("https://github.com/apache/logging-log4j2/issues/3940")
        void concurrent_stack_trace_mutation_should_not_cause_failure() throws Exception {
            final int stackTracePerThreadCount = 100;
            formatThrowableWhileMutatingConcurrently(threadIndex -> {
                final List<StackTraceElement[]> stackTraces = createExceptionsOfDifferentDepths().stream()
                        .map(Throwable::getStackTrace)
                        .collect(Collectors.toList());
                return exception -> {
                    for (int stackTraceIndex = 0; stackTraceIndex < stackTracePerThreadCount; stackTraceIndex++) {
                        exception.setStackTrace(stackTraces.get(stackTraceIndex));
                        // Give some time slack to increase randomness
                        LockSupport.parkNanos(1);
                    }
                };
            });
        }

        @RepeatedTest(10)
        @Issue("https://github.com/apache/logging-log4j2/issues/3929")
        void concurrent_suppressed_mutation_should_not_cause_failure() throws Exception {
            formatThrowableWhileMutatingConcurrently(threadIndex -> {
                final List<Exception> exceptions = createExceptionsOfDifferentDepths();
                return exception -> exceptions.forEach(suppressed -> {
                    exception.addSuppressed(suppressed);
                    // Give some time slack to increase randomness
                    LockSupport.parkNanos(1);
                });
            });
        }

        private void formatThrowableWhileMutatingConcurrently(
                Function<Integer, Consumer<Throwable>> throwableMutatorFactory) throws Exception {

            // Test constants
            final int threadCount = Math.max(8, Runtime.getRuntime().availableProcessors());
            final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            try {
                final Exception exception = new Exception();
                final CountDownLatch startLatch =
                        new CountDownLatch(threadCount + /* the main thread invoking the rendering: */ 1);
                final AtomicInteger runningThreadCountRef = new AtomicInteger(threadCount);

                // Schedule threads that will start mutating the exception with the start signal
                for (int threadIndex = 0; threadIndex < threadCount; threadIndex++) {
                    final Consumer<Throwable> exceptionMutator = throwableMutatorFactory.apply(threadIndex);
                    executor.submit(() -> {
                        try {
                            startLatch.countDown();
                            startLatch.await();
                            exceptionMutator.accept(exception);
                        } catch (InterruptedException ignored) {
                            // Restore the interrupt
                            Thread.currentThread().interrupt();
                        } finally {
                            runningThreadCountRef.decrementAndGet();
                        }
                    });
                }

                // Create the formatter
                final List<PatternFormatter> patternFormatters = PATTERN_PARSER.parse(patternPrefix, false, true, true);
                assertThat(patternFormatters).hasSize(1);
                final PatternFormatter patternFormatter = patternFormatters.get(0);

                // Create the log event and the layout buffer
                final LogEvent logEvent = Log4jLogEvent.newBuilder()
                        .setThrown(exception)
                        .setLevel(LEVEL)
                        .build();
                final StringBuilder buffer = new StringBuilder(16384);

                // Trigger the start latch and format the exception
                startLatch.countDown();
                startLatch.await();
                while (runningThreadCountRef.get() > 0) {
                    // Give some time slack to increase randomness
                    LockSupport.parkNanos(1);
                    patternFormatter.format(logEvent, buffer);
                    buffer.setLength(0);
                }
            } finally {
                executor.shutdownNow();
            }
        }

        private static List<Exception> createExceptionsOfDifferentDepths() {
            final StackTraceElement[] stackTrace = new Exception().getStackTrace();
            return IntStream.range(0, stackTrace.length)
                    .mapToObj(depth -> {
                        final Exception exception = new Exception();
                        exception.setStackTrace(Arrays.copyOfRange(stackTrace, 0, depth));
                        return exception;
                    })
                    .collect(Collectors.toList());
        }
    }

    static String convert(final String pattern) {
        return convert(pattern, EXCEPTION);
    }

    static String convert(final String pattern, final Throwable throwable) {
        final List<PatternFormatter> patternFormatters = PATTERN_PARSER.parse(pattern, false, true, true);
        final LogEvent logEvent =
                Log4jLogEvent.newBuilder().setThrown(throwable).setLevel(LEVEL).build();
        final StringBuilder buffer = new StringBuilder();
        for (final PatternFormatter patternFormatter : patternFormatters) {
            patternFormatter.format(logEvent, buffer);
        }
        return buffer.toString();
    }
}
