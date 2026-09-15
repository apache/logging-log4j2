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

import static org.apache.logging.log4j.core.pattern.ThrowablePatternConverterTest.LEVEL;
import static org.apache.logging.log4j.core.pattern.ThrowablePatternConverterTest.PATTERN_PARSER;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.Issue;

/**
 * Regression tests for <a href="https://github.com/apache/logging-log4j2/issues/4279">#4279</a>.
 *
 * <p>
 * The {@link ThrowableStackTraceRenderer} walks a {@link Throwable}'s causal chain in two
 * passes: a metadata pre-computation pass, and a render pass. For both passes to find the
 * same {@link Throwable} instance in the {@code IdentityHashMap}-keyed metadata table,
 * the renderer must call {@link Throwable#getCause()} only once and reuse the result.
 * Before this fix, the render pass invoked {@code getCause()} a second time — which
 * returned a different identity for non-idempotent implementations, causing a
 * {@link NullPointerException} when reading the missing {@code Metadata#stackLength}.
 * </p>
 *
 * <p>
 * These tests live in their own class (rather than as cases in
 * {@link ThrowablePatternConverterTest}'s shared {@code AbstractStackTraceTest}) because
 * the existing test classes share a static {@code EXCEPTION} whose stack trace is mutated
 * by a concurrent-mutation test, and adding new test methods to the abstract base shifts
 * surefire's randomized order, which exposes that pre-existing isolation problem. Keeping
 * these tests in a separate file isolates the new behavior from the shared state.
 * </p>
 */
class NonIdempotentGetCauseTest {

    @Test
    @Issue("https://github.com/apache/logging-log4j2/issues/4279")
    void non_idempotent_cause_should_not_cause_failure_for_ex() {
        assertNonIdempotentCauseRendersCorrectly("%ex", "Caused by: ");
    }

    @Test
    @Issue("https://github.com/apache/logging-log4j2/issues/4279")
    void non_idempotent_cause_should_not_cause_failure_for_rEx() {
        assertNonIdempotentCauseRendersCorrectly("%rEx", "Wrapped by: ");
    }

    @Test
    @Issue("https://github.com/apache/logging-log4j2/issues/4279")
    void non_idempotent_cause_should_not_cause_failure_for_xEx() {
        assertNonIdempotentCauseRendersCorrectly("%xEx", "Caused by: ");
    }

    @Test
    @Issue("https://github.com/apache/logging-log4j2/issues/4279")
    void getCause_should_be_invoked_exactly_once_for_ex() {
        assertGetCauseInvocationCount("%ex", 1);
    }

    @Test
    @Issue("https://github.com/apache/logging-log4j2/issues/4279")
    void getCause_should_be_invoked_exactly_once_for_rEx() {
        assertGetCauseInvocationCount("%rEx", 1);
    }

    /**
     * {@link ThrowableExtendedStackTraceRenderer} performs an additional, independent
     * causal-chain walk for JAR/version enrichment
     * (see {@code ThrowableExtendedStackTraceRenderer.ExtendedContext#createClassResourceInfoByName}),
     * so {@link Throwable#getCause()} is legitimately invoked twice for {@code %xEx}.
     */
    @Test
    @Issue("https://github.com/apache/logging-log4j2/issues/4279")
    void getCause_should_be_invoked_twice_for_xEx() {
        assertGetCauseInvocationCount("%xEx", 2);
    }

    private static void assertNonIdempotentCauseRendersCorrectly(final String pattern, final String causeCaption) {
        final Throwable exception = new NonIdempotentRootException("Root");
        final String stackTrace = convert(pattern, exception);
        // The cause is rendered with a "Caused by: " or "Wrapped by: " caption and the
        // cause's message. Match on the caption and the message text rather than the
        // cause's (anonymous) class name to keep the assertion stable.
        assertThat(stackTrace).contains(causeCaption).contains("Dynamic Cause");
    }

    private static void assertGetCauseInvocationCount(final String pattern, final int expectedCount) {
        final AtomicInteger causeInvocationCount = new AtomicInteger();
        final Throwable cause = new Exception("Cause");
        final Throwable root = new Exception("Root") {
            @Override
            public synchronized Throwable getCause() {
                causeInvocationCount.incrementAndGet();
                return cause;
            }
        };
        convert(pattern, root);
        assertThat(causeInvocationCount)
                .as("`getCause()` invocation count for pattern=`%s`", pattern)
                .hasValue(expectedCount);
    }

    private static String convert(final String pattern, final Throwable throwable) {
        final List<PatternFormatter> patternFormatters = PATTERN_PARSER.parse(pattern, false, true, true);
        final LogEvent logEvent =
                Log4jLogEvent.newBuilder().setThrown(throwable).setLevel(LEVEL).build();
        final StringBuilder buffer = new StringBuilder();
        for (final PatternFormatter patternFormatter : patternFormatters) {
            patternFormatter.format(logEvent, buffer);
        }
        return buffer.toString();
    }

    /**
     * Root exception with a {@code getCause()} that returns a brand-new {@link Exception}
     * instance on every invocation. The chain is finite because the cause's own
     * {@code getCause()} returns {@code null}.
     */
    private static final class NonIdempotentRootException extends Exception {

        private static final long serialVersionUID = 1L;

        NonIdempotentRootException(final String message) {
            super(message);
        }

        @Override
        public synchronized Throwable getCause() {
            return new TerminalCause("Dynamic Cause");
        }
    }

    /** Terminal node of the chain: {@code getCause()} returns {@code null} so the walk terminates. */
    private static final class TerminalCause extends Exception {

        private static final long serialVersionUID = 1L;

        TerminalCause(final String message) {
            super(message);
        }

        @Override
        public synchronized Throwable getCause() {
            return null;
        }
    }
}
