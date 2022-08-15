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
package org.apache.logging.log4j.layout.template.json.resolver;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.logging.log4j.layout.template.json.TestHelpers.*;
import static org.assertj.core.api.Assertions.assertThat;

class StackTraceStringResolverTest {

    ////////////////////////////////////////////////////////////////////////////
    // exceptions //////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    // Below we create arbitrary exceptions containing stack entries from non-Log4j packages.
    // Non-Log4j package origin is needed to avoid the truncation (e.g., `... 58 more`) done by `Throwable#printStackTrace()`.

    private static final String EXCEPTION_REGEX_FLAGS = "(?m)(?s)";     // MULTILINE | DOTALL

    private static final String TRUNCATION_SUFFIX = "<truncated>";

    @SuppressWarnings({"BigDecimalMethodWithoutRoundingCalled", "ResultOfMethodCallIgnored"})
    private static Throwable exception1() {
        return catchException(() -> BigDecimal.ONE.divide(BigDecimal.ZERO));
    }

    private static String exception1Regex(final boolean truncated) {
        final String truncationCorrectionRegex = truncationSuffixRegexOr(truncated, ".divide\\(");
        return "java.lang.ArithmeticException: Division by zero\r?\n" +
                "\t+at java.math.BigDecimal" + truncationCorrectionRegex + ".*";
    }

    @SuppressWarnings("ConstantConditions")
    private static Throwable exception2() {
        return catchException(() -> Collections.emptyList().add(0));
    }

    private static String exception2Regex(final boolean truncated) {
        final String truncationCorrectionRegex = truncationSuffixRegexOr(truncated, ".add\\(");
        return "java.lang.UnsupportedOperationException\r?\n" +
                "\t+at java.util.AbstractList" + truncationCorrectionRegex + ".*";
    }

    private static Throwable exception3() {
        return catchException(() -> new ServerSocket(-1));
    }

    private static String exception3Regex(final boolean truncated) {
        final String truncationCorrectionRegex = truncationSuffixRegexOr(truncated, ".<init>");
        return "java.lang.IllegalArgumentException: Port value out of range: -1\r?\n" +
                "\t+at java.net.ServerSocket" + truncationCorrectionRegex + ".*";
    }

    private static String truncationSuffixRegexOr(final boolean truncated, final String fallback) {
        return truncated
                ? ("\r?\n" + TRUNCATION_SUFFIX)
                : fallback;
    }

    private static Throwable catchException(ThrowingRunnable runnable) {
        try {
            runnable.run();
            throw new AssertionError("should not have reached here");
        } catch (Throwable error) {
            return error;
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {

        void run() throws Throwable;

    }

    @Test
    void exception1_regex_should_match() {
        final Throwable error = exception1();
        final String stackTrace = stackTrace(error);
        final String regex = exception1Regex(false);
        Assertions
                .assertThat(stackTrace)
                .matches(EXCEPTION_REGEX_FLAGS + regex);
    }

    @Test
    void exception2_regex_should_match() {
        final Throwable error = exception2();
        final String stackTrace = stackTrace(error);
        final String regex = exception2Regex(false);
        Assertions
                .assertThat(stackTrace)
                .matches(EXCEPTION_REGEX_FLAGS + regex);
    }

    @Test
    void exception3_regex_should_match() {
        final Throwable error = exception3();
        final String stackTrace = stackTrace(error);
        final String regex = exception3Regex(false);
        Assertions
                .assertThat(stackTrace)
                .matches(EXCEPTION_REGEX_FLAGS + regex);
    }

    private static String stackTrace(final Throwable throwable) {
        final String encoding = "UTF-8";
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             PrintStream printStream = new PrintStream(outputStream, false, encoding)) {
            throwable.printStackTrace(printStream);
            printStream.flush();
            return outputStream.toString(encoding);
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // abstract tests //////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    private static abstract class AbstractTestCases {

        private final boolean truncated;

        AbstractTestCases(boolean truncated) {
            this.truncated = truncated;
        }

        private String exception1Regex() {
            return StackTraceStringResolverTest.exception1Regex(truncated);
        }

        private String exception2Regex() {
            return StackTraceStringResolverTest.exception2Regex(truncated);
        }

        private String exception3Regex() {
            return StackTraceStringResolverTest.exception3Regex(truncated);
        }

        @Test
        void exception_should_be_resolved() {
            final Throwable exception = exception1();
            final String serializedExceptionRegex = EXCEPTION_REGEX_FLAGS + exception1Regex();
            assertSerializedException(exception, serializedExceptionRegex);
        }

        @Test
        void exception_with_cause_should_be_resolved() {

            // Create the exception.
            final Throwable exception = exception1();
            final Throwable cause = exception2();
            exception.initCause(cause);

            // Check the serialized exception.
            final String serializedExceptionRegex = EXCEPTION_REGEX_FLAGS +
                    exception1Regex() +
                    "\nCaused by: " + exception2Regex();
            assertSerializedException(exception, serializedExceptionRegex);

        }

        @Test
        void exception_with_causes_should_be_resolved() {

            // Create the exception.
            final Throwable exception = exception1();
            final Throwable cause1 = exception2();
            final Throwable cause2 = exception3();
            exception.initCause(cause1);
            cause1.initCause(cause2);

            // Check the serialized exception.
            final String serializedExceptionRegex = EXCEPTION_REGEX_FLAGS +
                    exception1Regex() +
                    "\nCaused by: " + exception2Regex() +
                    "\nCaused by: " + exception3Regex();
            assertSerializedException(exception, serializedExceptionRegex);

        }

        @Test
        void exception_with_suppressed_should_be_resolved() {

            // Create the exception.
            final Throwable exception = exception1();
            final Throwable suppressed = exception2();
            exception.addSuppressed(suppressed);

            // Check the serialized exception.
            final String serializedExceptionRegex = EXCEPTION_REGEX_FLAGS +
                    exception1Regex() +
                    "\n\tSuppressed: " + exception2Regex();
            assertSerializedException(exception, serializedExceptionRegex);

        }

        @Test
        void exception_with_suppresseds_should_be_resolved() {

            // Create the exception.
            final Throwable exception = exception1();
            final Throwable suppressed1 = exception2();
            final Throwable suppressed2 = exception3();
            exception.addSuppressed(suppressed1);
            exception.addSuppressed(suppressed2);

            // Check the serialized exception.
            final String serializedExceptionRegex = EXCEPTION_REGEX_FLAGS +
                    exception1Regex() +
                    "\n\tSuppressed: " + exception2Regex() +
                    "\n\tSuppressed: " + exception3Regex();
            assertSerializedException(exception, serializedExceptionRegex);

        }

        @Test
        void exception_with_cause_and_suppressed_should_be_resolved() {

            // Create the exception.
            final Throwable exception = exception1();
            final Throwable suppressed = exception2();
            final Throwable cause = exception3();
            exception.addSuppressed(suppressed);
            exception.initCause(cause);

            // Check the serialized exception.
            final String serializedExceptionRegex = EXCEPTION_REGEX_FLAGS +
                    exception1Regex() +
                    "\n\tSuppressed: " + exception2Regex() +
                    "\nCaused by: " + exception3Regex();
            assertSerializedException(exception, serializedExceptionRegex);

        }

        @Test
        void exception_with_cause_with_suppressed_should_be_resolved() {

            // Create the exception.
            final Throwable exception = exception1();
            final Throwable cause = exception2();
            final Throwable suppressed = exception3();
            exception.initCause(cause);
            cause.addSuppressed(suppressed);

            // Check the serialized exception.
            final String serializedExceptionRegex = EXCEPTION_REGEX_FLAGS +
                    exception1Regex() +
                    "\nCaused by: " + exception2Regex() +
                    "\n\tSuppressed: " + exception3Regex();
            assertSerializedException(exception, serializedExceptionRegex);

        }

        @Test
        void exception_with_suppressed_with_cause_should_be_resolved() {

            // Create the exception.
            final Throwable exception = exception1();
            final Throwable suppressed = exception2();
            final Throwable cause = exception3();
            exception.addSuppressed(suppressed);
            suppressed.initCause(cause);

            // Check the serialized exception.
            final String serializedExceptionRegex = EXCEPTION_REGEX_FLAGS +
                    exception1Regex() +
                    "\n\tSuppressed: " + exception2Regex() +
                    "\n\tCaused by: " + exception3Regex();
            assertSerializedException(exception, serializedExceptionRegex);

        }

        abstract void assertSerializedException(
                final Throwable exception,
                final String regex);

    }

    ////////////////////////////////////////////////////////////////////////////
    // tests without truncation ////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    @Nested
    class WithoutTruncation extends AbstractTestCases {

        WithoutTruncation() {
            super(false);
        }

        @Override
        void assertSerializedException(final Throwable exception, final String regex) {
            assertSerializedExceptionWithoutTruncation(exception, regex);
        }

    }

    private static void assertSerializedExceptionWithoutTruncation(
            final Throwable exception,
            final String regex) {

        // Create the event template.
        final Map<String, ?> exceptionResolverTemplate = asMap(
                "$resolver", "exception",
                "field", "stackTrace",
                "stackTrace", asMap("stringified", true));

        // Check the serialized event.
        assertSerializedException(
                exceptionResolverTemplate,
                exception,
                serializedExceptionAssert -> serializedExceptionAssert.matches(regex));

    }

    ////////////////////////////////////////////////////////////////////////////
    // tests with `truncationPointMatcherStrings` //////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    @Nested
    class WithStringTruncation extends AbstractTestCases {

        WithStringTruncation() {
            super(true);
        }

        @Override
        void assertSerializedException(final Throwable exception, final String regex) {
            assertSerializedExceptionWithStringTruncation(exception, regex);
        }
        
    }

    private static void assertSerializedExceptionWithStringTruncation(
            final Throwable exception,
            final String regex) {

        // Create the event template.
        final List<String> pointMatcherStrings = pointMatcherStrings();
        final Map<String, ?> exceptionResolverTemplate = asMap(
                "$resolver", "exception",
                "field", "stackTrace",
                "stackTrace", asMap("stringified", asMap(
                        "truncation", asMap(
                                "suffix", TRUNCATION_SUFFIX,
                                "pointMatcherStrings", pointMatcherStrings))));

        // Check the serialized event.
        assertSerializedException(
                exceptionResolverTemplate,
                exception,
                serializedExceptionAssert -> serializedExceptionAssert.matches(regex));

    }

    private static List<String> pointMatcherStrings() {
        final Throwable exception1 = exception1();
        final Throwable exception2 = exception2();
        final Throwable exception3 = exception3();
        return Stream
                .of(exception1, exception2, exception3)
                .map(exception -> {
                    final StackTraceElement stackTraceElement = exception.getStackTrace()[0];
                    final String className = stackTraceElement.getClassName();
                    return "at " + className;
                })
                .collect(Collectors.toList());
    }

    ////////////////////////////////////////////////////////////////////////////
    // utilities ///////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    private static void assertSerializedException(
            final Map<String, ?> exceptionResolverTemplate,
            final Throwable exception,
            final Consumer<AbstractStringAssert<?>> serializedExceptionAsserter) {

        // Create the event template.
        final String eventTemplate = writeJson(asMap("output", exceptionResolverTemplate));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate(eventTemplate)
                .build();

        // Create the log event.
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setThrown(exception)
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            AbstractStringAssert<?> serializedExceptionAssert = assertThat(accessor.getString("output"));
            serializedExceptionAsserter.accept(serializedExceptionAssert);
        });

    }

}
