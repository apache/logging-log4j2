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
import static org.apache.logging.log4j.core.pattern.ThrowableRendererTest.assertStackTraceLineMatches;

import foo.TestFriendlyException;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ExtendedThrowableRendererTest {

    static final String NEWLINE = System.lineSeparator();

    static final Exception EXCEPTION = TestFriendlyException.INSTANCE;

    static int[] maxLineCounts() {
        return ThrowableRendererTest.maxLineCounts();
    }

    @ParameterizedTest
    @MethodSource("maxLineCounts")
    void output_produced_with_maxLineCount_should_match(final int maxLineCount) {
        final String stackTrace = renderStackTraceUsingLog4j(emptyList(), maxLineCount);
        assertStackTraceLineMatches(
                stackTrace,
                maxLineCount,
                asList(
                        "foo.TestFriendlyException: r",
                        "	at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%) ~[test-classes/:?]",
                        "	at foo.TestFriendlyException.<clinit>(TestFriendlyException.java:%DIGITS%) ~[test-classes/:?]",
                        "	at org.apache.logging.log4j.core.pattern.ExtendedThrowableRendererTest.<clinit>(ExtendedThrowableRendererTest.java:%DIGITS%) [test-classes/:?]",
                        "	Suppressed: foo.TestFriendlyException: r_s",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%) ~[test-classes/:?]",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%) ~[test-classes/:?]",
                        "		... 2 more",
                        "		Suppressed: foo.TestFriendlyException: r_s_s",
                        "			at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%) ~[test-classes/:?]",
                        "			at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%) ~[test-classes/:?]",
                        "			... 3 more",
                        "	Caused by: foo.TestFriendlyException: r_s_c",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%) ~[test-classes/:?]",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%) ~[test-classes/:?]",
                        "		... 3 more",
                        "Caused by: foo.TestFriendlyException: r_c",
                        "	at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%) ~[test-classes/:?]",
                        "	at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%) ~[test-classes/:?]",
                        "	... 2 more",
                        "	Suppressed: foo.TestFriendlyException: r_c_s",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%) ~[test-classes/:?]",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%) ~[test-classes/:?]",
                        "		... 3 more",
                        "Caused by: foo.TestFriendlyException: r_c_c",
                        "	at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%) ~[test-classes/:?]",
                        "	at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%) ~[test-classes/:?]",
                        "	... 3 more"));
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
                        "	at org.apache.logging.log4j.core.pattern.ExtendedThrowableRendererTest.<clinit>(ExtendedThrowableRendererTest.java:%DIGITS%) [test-classes/:?]",
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
                        "	at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%) ~[test-classes/:?]",
                        "	at foo.TestFriendlyException.<clinit>(TestFriendlyException.java:%DIGITS%) ~[test-classes/:?]",
                        "	... ",
                        "	Suppressed: foo.TestFriendlyException: r_s",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%) ~[test-classes/:?]",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%) ~[test-classes/:?]",
                        "		... 2 more",
                        "		Suppressed: foo.TestFriendlyException: r_s_s",
                        "			at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%) ~[test-classes/:?]",
                        "			at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%) ~[test-classes/:?]",
                        "			... 3 more",
                        "	Caused by: foo.TestFriendlyException: r_s_c",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%) ~[test-classes/:?]",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%) ~[test-classes/:?]",
                        "		... 3 more",
                        "Caused by: foo.TestFriendlyException: r_c",
                        "	at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%) ~[test-classes/:?]",
                        "	at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%) ~[test-classes/:?]",
                        "	... 2 more",
                        "	Suppressed: foo.TestFriendlyException: r_c_s",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%) ~[test-classes/:?]",
                        "		at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%) ~[test-classes/:?]",
                        "		... 3 more",
                        "Caused by: foo.TestFriendlyException: r_c_c",
                        "	at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%) ~[test-classes/:?]",
                        "	at foo.TestFriendlyException.create(TestFriendlyException.java:%DIGITS%) ~[test-classes/:?]",
                        "	... 3 more"));
    }

    private static String renderStackTraceUsingLog4j(final List<String> ignoredPackageNames, final int maxLineCount) {
        final ThrowableRenderer<?> renderer = new ExtendedThrowableRenderer(ignoredPackageNames, maxLineCount);
        final StringBuilder rendererOutputBuilder = new StringBuilder();
        renderer.renderThrowable(rendererOutputBuilder, EXCEPTION, NEWLINE);
        return rendererOutputBuilder.toString();
    }
}
