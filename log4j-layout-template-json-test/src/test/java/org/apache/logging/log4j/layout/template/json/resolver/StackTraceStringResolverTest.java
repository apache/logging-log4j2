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
package org.apache.logging.log4j.layout.template.json.resolver;

import static org.apache.logging.log4j.layout.template.json.TestHelpers.CONFIGURATION;
import static org.apache.logging.log4j.layout.template.json.TestHelpers.JAVA_BASE_PREFIX;
import static org.apache.logging.log4j.layout.template.json.TestHelpers.asMap;
import static org.apache.logging.log4j.layout.template.json.TestHelpers.usingSerializedLogEventAccessor;
import static org.apache.logging.log4j.layout.template.json.TestHelpers.writeJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayoutDefaults;
import org.apache.logging.log4j.util.Constants;
import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class StackTraceStringResolverTest {

    ////////////////////////////////////////////////////////////////////////////
    // exceptions //////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    // Below we create arbitrary exceptions containing stack entries from non-Log4j packages.
    // Non-Log4j package origin is needed to avoid the truncation (e.g., `... 58 more`) done by
    // `Throwable#printStackTrace()`.

    private static final String EXCEPTION_REGEX_FLAGS = "(?s)"; // DOTALL

    private static final String TRUNCATION_SUFFIX = "<truncated>";

    @SuppressWarnings({"BigDecimalMethodWithoutRoundingCalled", "ResultOfMethodCallIgnored"})
    private static Throwable exception1() {
        return catchException(() -> BigDecimal.ONE.divide(BigDecimal.ZERO));
    }

    private static String exception1Regex(final boolean truncated) {
        final String truncationCorrectionRegex = truncationSuffixRegexOr(truncated, ".divide\\(");
        return "java.lang.ArithmeticException: Division by zero\r?\n" + "\t+at " + JAVA_BASE_PREFIX
                + "java.math.BigDecimal" + truncationCorrectionRegex + ".*";
    }

    @SuppressWarnings("ConstantConditions")
    private static Throwable exception2() {
        return catchException(() -> Collections.emptyList().add(0));
    }

    private static String exception2Regex(final boolean truncated) {
        final String truncationCorrectionRegex = truncationSuffixRegexOr(truncated, ".add\\(");
        return "java.lang.UnsupportedOperationException\r?\n" + "\t+at " + JAVA_BASE_PREFIX + "java.util.AbstractList"
                + truncationCorrectionRegex + ".*";
    }

    private static Throwable exception3() {
        return catchException(() -> new ServerSocket(-1));
    }

    private static String exception3Regex(final boolean truncated) {
        final String truncationCorrectionRegex = truncationSuffixRegexOr(truncated, ".<init>");
        return "java.lang.IllegalArgumentException: Port value out of range: -1\r?\n" + "\t+at " + JAVA_BASE_PREFIX
                + "java.net.ServerSocket" + truncationCorrectionRegex + ".*";
    }

    private static String truncationSuffixRegexOr(final boolean truncated, final String fallback) {
        return truncated ? ("\r?\n" + TRUNCATION_SUFFIX) : fallback;
    }

    private static Throwable catchException(final ThrowingRunnable runnable) {
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
        assertThat(stackTrace).matches(EXCEPTION_REGEX_FLAGS + regex);
    }

    @Test
    void exception2_regex_should_match() {
        final Throwable error = exception2();
        final String stackTrace = stackTrace(error);
        final String regex = exception2Regex(false);
        assertThat(stackTrace).matches(EXCEPTION_REGEX_FLAGS + regex);
    }

    @Test
    void exception3_regex_should_match() {
        final Throwable error = exception3();
        final String stackTrace = stackTrace(error);
        final String regex = exception3Regex(false);
        assertThat(stackTrace).matches(EXCEPTION_REGEX_FLAGS + regex);
    }

    private static String stackTrace(final Throwable throwable) {
        final String encoding = "UTF-8";
        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                final PrintStream printStream = new PrintStream(outputStream, false, encoding)) {
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

    private abstract static class AbstractTestCases {

        private final boolean truncated;

        AbstractTestCases(final boolean truncated) {
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
            final String serializedExceptionRegex =
                    EXCEPTION_REGEX_FLAGS + exception1Regex() + "\nCaused by: " + exception2Regex();
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
            final String serializedExceptionRegex = EXCEPTION_REGEX_FLAGS + exception1Regex()
                    + "\nCaused by: "
                    + exception2Regex() + "\nCaused by: "
                    + exception3Regex();
            assertSerializedException(exception, serializedExceptionRegex);
        }

        @Test
        void exception_with_suppressed_should_be_resolved() {

            // Create the exception.
            final Throwable exception = exception1();
            final Throwable suppressed = exception2();
            exception.addSuppressed(suppressed);

            // Check the serialized exception.
            final String serializedExceptionRegex =
                    EXCEPTION_REGEX_FLAGS + exception1Regex() + "\n\tSuppressed: " + exception2Regex();
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
            final String serializedExceptionRegex = EXCEPTION_REGEX_FLAGS + exception1Regex()
                    + "\n\tSuppressed: "
                    + exception2Regex() + "\n\tSuppressed: "
                    + exception3Regex();
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
            final String serializedExceptionRegex = EXCEPTION_REGEX_FLAGS + exception1Regex()
                    + "\n\tSuppressed: "
                    + exception2Regex() + "\nCaused by: "
                    + exception3Regex();
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
            final String serializedExceptionRegex = EXCEPTION_REGEX_FLAGS + exception1Regex()
                    + "\nCaused by: "
                    + exception2Regex() + "\n\tSuppressed: "
                    + exception3Regex();
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
            final String serializedExceptionRegex = EXCEPTION_REGEX_FLAGS + exception1Regex()
                    + "\n\tSuppressed: "
                    + exception2Regex() + "\n\tCaused by: "
                    + exception3Regex();
            assertSerializedException(exception, serializedExceptionRegex);
        }

        abstract void assertSerializedException(final Throwable exception, final String regex);

        private static void assertSerializedException(
                final Map<String, ?> exceptionResolverTemplate,
                final Throwable exception,
                final Consumer<AbstractStringAssert<?>> serializedExceptionAsserter) {

            // Create the event template.
            final String eventTemplate = writeJson(asMap("output", exceptionResolverTemplate));

            // Create the layout.
            final JsonTemplateLayout layout = JsonTemplateLayout.newBuilder()
                    .setConfiguration(CONFIGURATION)
                    .setEventTemplate(eventTemplate)
                    .build();

            // Create the log event.
            final LogEvent logEvent =
                    Log4jLogEvent.newBuilder().setThrown(exception).build();

            // Check the serialized event.
            usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
                final AbstractStringAssert<?> serializedExceptionAssert = assertThat(accessor.getString("output"));
                serializedExceptionAsserter.accept(serializedExceptionAssert);
            });
        }
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

        private void assertSerializedExceptionWithoutTruncation(final Throwable exception, final String regex) {

            // Create the event template.
            final Map<String, ?> exceptionResolverTemplate = asMap(
                    "$resolver", "exception",
                    "field", "stackTrace",
                    "stackTrace", asMap("stringified", true));

            // Check the serialized event.
            AbstractTestCases.assertSerializedException(
                    exceptionResolverTemplate,
                    exception,
                    serializedExceptionAssert -> serializedExceptionAssert.matches(regex));
        }

        @Test
        void JsonWriter_maxStringLength_should_work() {

            // Create the event template.
            final String eventTemplate = writeJson(asMap(
                    "ex",
                    asMap(
                            "$resolver", "exception",
                            "field", "stackTrace",
                            "stringified", true)));

            // Create the layout.
            final int maxStringLength = eventTemplate.length();
            final JsonTemplateLayout layout = JsonTemplateLayout.newBuilder()
                    .setConfiguration(CONFIGURATION)
                    .setEventTemplate(eventTemplate)
                    .setMaxStringLength(maxStringLength)
                    .setStackTraceEnabled(true)
                    .build();

            // Create the log event.
            final Throwable exception = exception1();
            final LogEvent logEvent =
                    Log4jLogEvent.newBuilder().setThrown(exception).build();

            // Check the serialized event.
            usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
                final int expectedLength = maxStringLength
                        + JsonTemplateLayoutDefaults.getTruncatedStringSuffix().length();
                assertThat(accessor.getString("ex").length()).isEqualTo(expectedLength);
            });
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // tests with `truncationPointMatcherStrings` //////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    @Nested
    class WithTruncation extends AbstractTestCases {

        WithTruncation() {
            super(true);
        }

        @Override
        void assertSerializedException(final Throwable exception, final String regex) {
            assertSerializedExceptionWithStringTruncation(exception, regex);
        }

        private void assertSerializedExceptionWithStringTruncation(final Throwable exception, final String regex) {

            // Create the event template.
            final List<String> pointMatcherStrings = pointMatcherStrings();
            final Map<String, ?> exceptionResolverTemplate = asMap(
                    "$resolver", "exception",
                    "field", "stackTrace",
                    "stackTrace",
                            asMap(
                                    "stringified",
                                    asMap(
                                            "truncation",
                                            asMap(
                                                    "suffix", TRUNCATION_SUFFIX,
                                                    "pointMatcherStrings", pointMatcherStrings))));

            // Check the serialized event.
            AbstractTestCases.assertSerializedException(
                    exceptionResolverTemplate,
                    exception,
                    serializedExceptionAssert -> serializedExceptionAssert.matches(regex));
        }

        private List<String> pointMatcherStrings() {
            final Throwable exception1 = exception1();
            final Throwable exception2 = exception2();
            final Throwable exception3 = exception3();
            return Stream.of(exception1, exception2, exception3)
                    .map(this::pointMatcherString)
                    .collect(Collectors.toList());
        }

        @Test
        void point_matchers_should_work() {

            // Create the exception to be logged.
            final Throwable parentError = exception1();
            final Throwable childError = exception3();
            parentError.initCause(childError);

            // Create the event template.
            final String eventTemplate = writeJson(asMap(

                    // Raw exception
                    "ex",
                            asMap(
                                    "$resolver", "exception",
                                    "field", "stackTrace",
                                    "stackTrace", asMap("stringified", true)),

                    // Exception matcher using strings
                    "stringMatchedEx",
                            asMap(
                                    "$resolver", "exception",
                                    "field", "stackTrace",
                                    "stackTrace",
                                            asMap(
                                                    "stringified",
                                                    asMap(
                                                            "truncation",
                                                            asMap(
                                                                    "suffix",
                                                                    TRUNCATION_SUFFIX,
                                                                    "pointMatcherStrings",
                                                                    Arrays.asList(
                                                                            "this string shouldn't match with anything",
                                                                            pointMatcherString(parentError)))))),

                    // Exception matcher using regexes
                    "regexMatchedEx",
                            asMap(
                                    "$resolver", "exception",
                                    "field", "stackTrace",
                                    "stackTrace",
                                            asMap(
                                                    "stringified",
                                                    asMap(
                                                            "truncation",
                                                            asMap(
                                                                    "suffix",
                                                                    TRUNCATION_SUFFIX,
                                                                    "pointMatcherRegexes",
                                                                    Arrays.asList(
                                                                            "this string shouldn't match with anything",
                                                                            pointMatcherRegex(parentError)))))),

                    // Raw exception root cause
                    "rootEx",
                            asMap(
                                    "$resolver", "exceptionRootCause",
                                    "field", "stackTrace",
                                    "stackTrace", asMap("stringified", true)),

                    // Exception root cause matcher using strings
                    "stringMatchedRootEx",
                            asMap(
                                    "$resolver", "exceptionRootCause",
                                    "field", "stackTrace",
                                    "stackTrace",
                                            asMap(
                                                    "stringified",
                                                    asMap(
                                                            "truncation",
                                                            asMap(
                                                                    "suffix",
                                                                    TRUNCATION_SUFFIX,
                                                                    "pointMatcherStrings",
                                                                    Arrays.asList(
                                                                            "this string shouldn't match with anything",
                                                                            pointMatcherString(childError)))))),

                    // Exception root cause matcher using regexes
                    "regexMatchedRootEx",
                            asMap(
                                    "$resolver", "exceptionRootCause",
                                    "field", "stackTrace",
                                    "stackTrace",
                                            asMap(
                                                    "stringified",
                                                    asMap(
                                                            "truncation",
                                                            asMap(
                                                                    "suffix",
                                                                    TRUNCATION_SUFFIX,
                                                                    "pointMatcherRegexes",
                                                                    Arrays.asList(
                                                                            "this string shouldn't match with anything",
                                                                            pointMatcherRegex(childError))))))));

            // Create the layout.
            final JsonTemplateLayout layout = JsonTemplateLayout.newBuilder()
                    .setConfiguration(CONFIGURATION)
                    .setEventTemplate(eventTemplate)
                    .build();

            // Create the log event.
            final LogEvent logEvent =
                    Log4jLogEvent.newBuilder().setThrown(parentError).build();

            // Check the serialized event.
            usingSerializedLogEventAccessor(layout, logEvent, accessor -> {

                // Check the raw parent exception.
                final String exPattern =
                        EXCEPTION_REGEX_FLAGS + exception1Regex(false) + "\nCaused by: " + exception3Regex(false);
                assertThat(accessor.getString("ex")).matches(exPattern);

                // Check the matcher usage on parent exception.
                final String matchedExPattern =
                        EXCEPTION_REGEX_FLAGS + exception1Regex(true) + "\nCaused by: " + exception3Regex(false);
                assertThat(accessor.getString("stringMatchedEx")).matches(matchedExPattern);
                assertThat(accessor.getString("regexMatchedEx")).matches(matchedExPattern);

                // Check the raw child exception.
                final String rootExPattern = EXCEPTION_REGEX_FLAGS + exception3Regex(false);
                assertThat(accessor.getString("rootEx")).matches(rootExPattern);

                // Check the matcher usage on child exception.
                final String matchedRootExPattern = EXCEPTION_REGEX_FLAGS + exception3Regex(true);
                assertThat(accessor.getString("stringMatchedRootEx")).matches(matchedRootExPattern);
                assertThat(accessor.getString("regexMatchedRootEx")).matches(matchedRootExPattern);
            });
        }

        private String pointMatcherString(final Throwable exception) {
            final StackTraceElement stackTraceElement = exception.getStackTrace()[0];
            final String className = stackTraceElement.getClassName();
            final String moduleName;
            if (Constants.JAVA_MAJOR_VERSION > 8) {
                moduleName = assertDoesNotThrow(() -> (String) StackTraceElement.class
                        .getDeclaredMethod("getModuleName")
                        .invoke(stackTraceElement));
            } else {
                moduleName = null;
            }
            return moduleName != null ? "at " + moduleName + "/" + className : "at " + className;
        }

        private String pointMatcherRegex(final Throwable exception) {
            final String string = pointMatcherString(exception);
            return matchingRegex(string);
        }

        /**
         * @return a regex matching the given input
         */
        private String matchingRegex(final String string) {
            return "[" + string.charAt(0) + "]" + Pattern.quote(string.substring(1));
        }
    }

    @Test
    void nonAscii_utf8_method_name_should_get_serialized() {

        // Create the log event.
        final LogEvent logEvent = Log4jLogEvent.newBuilder()
                .setThrown(NonAsciiUtf8MethodNameContainingException.INSTANCE)
                .build();

        // Create the event template.
        final String eventTemplate = writeJson(asMap(
                "ex_stacktrace",
                asMap(
                        "$resolver", "exception",
                        "field", "stackTrace",
                        "stringified", true)));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout.newBuilder()
                .setConfiguration(CONFIGURATION)
                .setStackTraceEnabled(true)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> assertThat(accessor.getString("ex_stacktrace"))
                .contains(NonAsciiUtf8MethodNameContainingException.NON_ASCII_UTF8_TEXT));
    }

    private static final class NonAsciiUtf8MethodNameContainingException extends RuntimeException {

        public static final long serialVersionUID = 0;

        private static final String NON_ASCII_UTF8_TEXT = "அஆஇฬ๘";

        private static final NonAsciiUtf8MethodNameContainingException INSTANCE = createInstance();

        @SuppressWarnings("UnicodeInCode")
        private static NonAsciiUtf8MethodNameContainingException createInstance() {
            try {
                throwException_அஆஇฬ๘();
                throw new IllegalStateException("should not have reached here");
            } catch (final NonAsciiUtf8MethodNameContainingException exception) {
                return exception;
            }
        }

        @SuppressWarnings({"NonAsciiCharacters", "UnicodeInCode"})
        private static void throwException_அஆஇฬ๘() {
            throw new NonAsciiUtf8MethodNameContainingException("exception with non-ASCII UTF-8 method name");
        }

        private NonAsciiUtf8MethodNameContainingException(final String message) {
            super(message);
        }
    }
}
