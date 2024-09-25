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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.logging.log4j.core.pattern.ThrowableStackTraceRendererTest.EXCEPTION;
import static org.apache.logging.log4j.core.pattern.ThrowableStackTraceRendererTest.NEWLINE;
import static org.apache.logging.log4j.core.pattern.ThrowableStackTraceRendererTest.assertStackTraceLineMatches;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ThrowableInvertedStackTraceRendererTest {

    static int[] maxLineCounts() {
        return ThrowableStackTraceRendererTest.maxLineCounts();
    }

    @ParameterizedTest
    @MethodSource("maxLineCounts")
    void output_produced_with_maxLineCount_should_match(final int maxLineCount) {
        final String stackTrace = renderStackTrace(emptyList(), maxLineCount);
        assertStackTraceLineMatches(
                stackTrace,
                maxLineCount,
                Arrays.asList(
                        "foo.TestFriendlyException: r_c_c",
                        "	at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "	at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "	... 4 more",
                        "Wrapped by: foo.TestFriendlyException: r_c",
                        "	at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "	at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "	... 3 more",
                        "	Suppressed: foo.TestFriendlyException: r_c_s",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "		... 4 more",
                        "Wrapped by: foo.TestFriendlyException: r",
                        "	at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "	at foo.TestFriendlyException.<clinit>(TestFriendlyException.java:%DIGITS%)",
                        "	at org.apache.logging.log4j.core.pattern.ThrowableStackTraceRendererTest.<clinit>(ThrowableStackTraceRendererTest.java:%DIGITS%)",
                        "	at org.apache.logging.log4j.core.pattern.ThrowableInvertedStackTraceRendererTest.maxLineCounts(ThrowableInvertedStackTraceRendererTest.java:%DIGITS%)",
                        "	Suppressed: foo.TestFriendlyException: r_s_c",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "		... 4 more",
                        "	Wrapped by: foo.TestFriendlyException: r_s",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "		... 3 more",
                        "		Suppressed: foo.TestFriendlyException: r_s_s",
                        "			at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "			at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "			... 4 more"));
    }

    @ParameterizedTest
    @MethodSource("maxLineCounts")
    void output_produced_with_ignoredPackageNames_and_maxLineCount_should_match_1(final int maxLineCount) {
        final String stackTrace = renderStackTrace(singletonList("foo"), maxLineCount);
        assertStackTraceLineMatches(
                stackTrace,
                maxLineCount,
                Arrays.asList(
                        "foo.TestFriendlyException: r_c_c",
                        "	... suppressed 2 lines",
                        "	... 4 more",
                        "Wrapped by: foo.TestFriendlyException: r_c",
                        "	... suppressed 2 lines",
                        "	... 3 more",
                        "	Suppressed: foo.TestFriendlyException: r_c_s",
                        "		... suppressed 2 lines",
                        "		... 4 more",
                        "Wrapped by: foo.TestFriendlyException: r",
                        "	... suppressed 2 lines",
                        "	at org.apache.logging.log4j.core.pattern.ThrowableStackTraceRendererTest.<clinit>(ThrowableStackTraceRendererTest.java:%DIGITS%)",
                        "	at org.apache.logging.log4j.core.pattern.ThrowableInvertedStackTraceRendererTest.maxLineCounts(ThrowableInvertedStackTraceRendererTest.java:%DIGITS%)",
                        "	Suppressed: foo.TestFriendlyException: r_s_c",
                        "		... suppressed 2 lines",
                        "		... 4 more",
                        "	Wrapped by: foo.TestFriendlyException: r_s",
                        "		... suppressed 2 lines",
                        "		... 3 more",
                        "		Suppressed: foo.TestFriendlyException: r_s_s",
                        "			... suppressed 2 lines",
                        "			... 4 more"));
    }

    @ParameterizedTest
    @MethodSource("maxLineCounts")
    void output_produced_with_ignoredPackageNames_and_maxLineCount_should_match_2(final int maxLineCount) {
        final String stackTrace = renderStackTrace(singletonList("org.apache"), maxLineCount);
        assertStackTraceLineMatches(
                stackTrace,
                maxLineCount,
                Arrays.asList(
                        "foo.TestFriendlyException: r_c_c",
                        "	at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "	at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "	... 4 more",
                        "Wrapped by: foo.TestFriendlyException: r_c",
                        "	at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "	at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "	... 3 more",
                        "	Suppressed: foo.TestFriendlyException: r_c_s",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "		... 4 more",
                        "Wrapped by: foo.TestFriendlyException: r",
                        "	at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "	at foo.TestFriendlyException.<clinit>(TestFriendlyException.java:%DIGITS%)",
                        "	... suppressed 2 lines",
                        "	Suppressed: foo.TestFriendlyException: r_s_c",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "		... 4 more",
                        "	Wrapped by: foo.TestFriendlyException: r_s",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "		... 3 more",
                        "		Suppressed: foo.TestFriendlyException: r_s_s",
                        "			at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "			at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%)",
                        "			... 4 more"));
    }

    private static String renderStackTrace(final List<String> ignoredPackageNames, final int maxLineCount) {
        final ThrowableStackTraceRenderer<?> renderer =
                new ThrowableInvertedStackTraceRenderer(ignoredPackageNames, maxLineCount);
        final StringBuilder rendererOutputBuilder = new StringBuilder();
        renderer.renderThrowable(rendererOutputBuilder, EXCEPTION, NEWLINE);
        return rendererOutputBuilder.toString();
    }
}
