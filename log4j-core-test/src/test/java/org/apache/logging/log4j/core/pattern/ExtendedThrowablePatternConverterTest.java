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
import static org.apache.logging.log4j.core.pattern.ThrowablePatternConverterTest.THROWING_METHOD;

import foo.TestFriendlyException;
import java.util.List;
import org.apache.logging.log4j.core.pattern.ThrowablePatternConverterTest.AbstractPropertyTest;
import org.apache.logging.log4j.core.pattern.ThrowablePatternConverterTest.AbstractStackTraceTest;
import org.apache.logging.log4j.core.pattern.ThrowablePatternConverterTest.DepthTestCase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link ExtendedThrowablePatternConverter} tests.
 */
class ExtendedThrowablePatternConverterTest {

    @Nested
    class PropertyTest extends AbstractPropertyTest {

        PropertyTest() {
            super("%xEx", THROWING_METHOD);
        }
    }

    private static final List<String> EXPECTED_FULL_STACK_TRACE_LINES = asList(
            "foo.TestFriendlyException: r [localized]",
            "	at " + TestFriendlyException.NAMED_MODULE_STACK_TRACE_ELEMENT + " ~[?:?]",
            "	at foo.TestFriendlyException.create(TestFriendlyException.java:0) ~[test-classes/:?]",
            "	at foo.TestFriendlyException.<clinit>(TestFriendlyException.java:0) ~[test-classes/:?]",
            "	at " + TestFriendlyException.ORG_APACHE_REPLACEMENT_STACK_TRACE_ELEMENT + " ~[?:0]",
            "	Suppressed: foo.TestFriendlyException: r_s [localized]",
            "		at foo.TestFriendlyException.create(TestFriendlyException.java:0) ~[test-classes/:?]",
            "		at foo.TestFriendlyException.create(TestFriendlyException.java:0) ~[test-classes/:?]",
            "		... 2 more",
            "		Suppressed: foo.TestFriendlyException: r_s_s [localized]",
            "			at foo.TestFriendlyException.create(TestFriendlyException.java:0) ~[test-classes/:?]",
            "			at foo.TestFriendlyException.create(TestFriendlyException.java:0) ~[test-classes/:?]",
            "			... 3 more",
            "	Caused by: foo.TestFriendlyException: r_s_c [localized]",
            "		at foo.TestFriendlyException.create(TestFriendlyException.java:0) ~[test-classes/:?]",
            "		at foo.TestFriendlyException.create(TestFriendlyException.java:0) ~[test-classes/:?]",
            "		... 3 more",
            "Caused by: foo.TestFriendlyException: r_c [localized]",
            "	at foo.TestFriendlyException.create(TestFriendlyException.java:0) ~[test-classes/:?]",
            "	at foo.TestFriendlyException.create(TestFriendlyException.java:0) ~[test-classes/:?]",
            "	... 2 more",
            "	Suppressed: foo.TestFriendlyException: r_c_s [localized]",
            "		at foo.TestFriendlyException.create(TestFriendlyException.java:0) ~[test-classes/:?]",
            "		at foo.TestFriendlyException.create(TestFriendlyException.java:0) ~[test-classes/:?]",
            "		... 3 more",
            "	Caused by: [CIRCULAR REFERENCE: foo.TestFriendlyException: r_c [localized]]",
            "Caused by: foo.TestFriendlyException: r_c_c [localized]",
            "	at foo.TestFriendlyException.create(TestFriendlyException.java:0) ~[test-classes/:?]",
            "	at foo.TestFriendlyException.create(TestFriendlyException.java:0) ~[test-classes/:?]",
            "	... 3 more",
            "Caused by: [CIRCULAR REFERENCE: foo.TestFriendlyException: r_c [localized]]");

    @Nested
    class StackTraceTest extends AbstractStackTraceTest {

        StackTraceTest() {
            super("%xEx");
        }

        @ParameterizedTest
        @MethodSource("org.apache.logging.log4j.core.pattern.ThrowablePatternConverterTest#fullStackTracePatterns")
        void full_output_should_match(final String pattern) {
            final String effectivePattern = patternPrefix + pattern;
            assertStackTraceLines(null, effectivePattern, EXPECTED_FULL_STACK_TRACE_LINES);
        }

        @ParameterizedTest
        @MethodSource("org.apache.logging.log4j.core.pattern.ThrowablePatternConverterTest#depthTestCases")
        void depth_limited_output_should_match(final DepthTestCase depthTestCase) {
            final String pattern = String.format(
                    "%s{%d}%s",
                    patternPrefix, depthTestCase.maxLineCount, depthTestCase.separatorTestCase.patternAddendum);
            assertStackTraceLines(depthTestCase, pattern, EXPECTED_FULL_STACK_TRACE_LINES);
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
                            "	at " + TestFriendlyException.NAMED_MODULE_STACK_TRACE_ELEMENT + " ~[?:?]",
                            "	... suppressed 2 lines",
                            "	at " + TestFriendlyException.ORG_APACHE_REPLACEMENT_STACK_TRACE_ELEMENT + " ~[?:0]",
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
                            "	at " + TestFriendlyException.NAMED_MODULE_STACK_TRACE_ELEMENT + " ~[?:?]",
                            "	at foo.TestFriendlyException.create(TestFriendlyException.java:0) ~[test-classes/:?]",
                            "	at foo.TestFriendlyException.<clinit>(TestFriendlyException.java:0) ~[test-classes/:?]",
                            "	...",
                            "	Suppressed: foo.TestFriendlyException: r_s [localized]",
                            "		at foo.TestFriendlyException.create(TestFriendlyException.java:0) ~[test-classes/:?]",
                            "		at foo.TestFriendlyException.create(TestFriendlyException.java:0) ~[test-classes/:?]",
                            "		... 2 more",
                            "		Suppressed: foo.TestFriendlyException: r_s_s [localized]",
                            "			at foo.TestFriendlyException.create(TestFriendlyException.java:0) ~[test-classes/:?]",
                            "			at foo.TestFriendlyException.create(TestFriendlyException.java:0) ~[test-classes/:?]",
                            "			... 3 more",
                            "	Caused by: foo.TestFriendlyException: r_s_c [localized]",
                            "		at foo.TestFriendlyException.create(TestFriendlyException.java:0) ~[test-classes/:?]",
                            "		at foo.TestFriendlyException.create(TestFriendlyException.java:0) ~[test-classes/:?]",
                            "		... 3 more",
                            "Caused by: foo.TestFriendlyException: r_c [localized]",
                            "	at foo.TestFriendlyException.create(TestFriendlyException.java:0) ~[test-classes/:?]",
                            "	at foo.TestFriendlyException.create(TestFriendlyException.java:0) ~[test-classes/:?]",
                            "	... 2 more",
                            "	Suppressed: foo.TestFriendlyException: r_c_s [localized]",
                            "		at foo.TestFriendlyException.create(TestFriendlyException.java:0) ~[test-classes/:?]",
                            "		at foo.TestFriendlyException.create(TestFriendlyException.java:0) ~[test-classes/:?]",
                            "		... 3 more",
                            "	Caused by: [CIRCULAR REFERENCE: foo.TestFriendlyException: r_c [localized]]",
                            "Caused by: foo.TestFriendlyException: r_c_c [localized]",
                            "	at foo.TestFriendlyException.create(TestFriendlyException.java:0) ~[test-classes/:?]",
                            "	at foo.TestFriendlyException.create(TestFriendlyException.java:0) ~[test-classes/:?]",
                            "	... 3 more",
                            "Caused by: [CIRCULAR REFERENCE: foo.TestFriendlyException: r_c [localized]]"));
        }
    }
}
