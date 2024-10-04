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

import java.io.PrintWriter;
import java.util.Collections;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.LoggerTest;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.util.StringBuilderWriter;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@code throwable}, {@code rThrowable} and {@code xThrowable} pattern.
 */
public class ThrowableTest {
    static Stream<Arguments> testConverter_dataSource() {
        final String filters = "org.junit,org.apache.maven,sun.reflect,java.lang.reflect";
        final Integer depth = 5;
        return Stream.of(
                // Throwable
                Arguments.of("%ex", filters, null, null, null),
                Arguments.of("%ex", null, depth, null, null),
                Arguments.of("%ex", null, null, "I am suffix", "#"),
                // RootThrowable
                Arguments.of("%rEx", filters, null, null, null),
                Arguments.of("%rEx", null, depth, null, null),
                Arguments.of("%rEx", null, null, "I am suffix", "#"),
                // ExtendedThrowable
                Arguments.of("%xEx", filters, null, null, null),
                Arguments.of("%xEx", null, depth, null, null),
                Arguments.of("%xEx", null, null, "I am suffix", "#"));
    }

    @ParameterizedTest
    @MethodSource("testConverter_dataSource")
    void testConverter(String exceptionPattern, String filters, Integer depth, String suffix, String lineSeparator) {
        final String pattern = buildPattern(exceptionPattern, filters, depth, suffix, lineSeparator);
        final ConfigurationBuilder<BuiltConfiguration> configBuilder =
                ConfigurationBuilderFactory.newConfigurationBuilder();

        final String appenderName = "LIST";
        final Configuration config = configBuilder
                .add(configBuilder
                        .newAppender(appenderName, "List")
                        .add(configBuilder.newLayout("PatternLayout").addAttribute("pattern", pattern)))
                .add(configBuilder.newRootLogger(Level.ALL).add(configBuilder.newAppenderRef(appenderName)))
                .build(false);

        try (final LoggerContext loggerContext = Configurator.initialize(config)) {
            // Restart logger context after first test run
            if (loggerContext.isStopped()) {
                loggerContext.start();
                loggerContext.reconfigure(config);
            }
            final Throwable r = createException("r", 1, 3);

            final Logger logger = loggerContext.getLogger(LoggerTest.class);
            final ListAppender appender = loggerContext.getConfiguration().getAppender(appenderName);
            logger.error("Exception", r);

            assertThat(appender.getMessages()).hasSize(1);
            final String message = appender.getMessages().get(0);
            assertThat(message).isNotNull();
            verifyFilters(message, filters);
            verifyDepth(message, depth);
            verifySuffix(message, suffix, lineSeparator);
        }
    }

    static Stream<Arguments> renderers_dataSource() {
        return Stream.of(
                Arguments.of(new ThrowableStackTraceRenderer<>(Collections.emptyList(), Integer.MAX_VALUE)),
                Arguments.of(new ThrowableInvertedStackTraceRenderer(Collections.emptyList(), Integer.MAX_VALUE)),
                Arguments.of(new ThrowableExtendedStackTraceRenderer(Collections.emptyList(), Integer.MAX_VALUE)));
    }

    @ParameterizedTest
    @MethodSource("renderers_dataSource")
    void testCircularSuppressedExceptions(final ThrowableStackTraceRenderer<?> renderer) {
        final Exception e1 = new Exception();
        final Exception e2 = new Exception();
        e2.addSuppressed(e1);
        e1.addSuppressed(e2);

        render(renderer, e1);
    }

    @ParameterizedTest
    @MethodSource("renderers_dataSource")
    void testCircularSuppressedNestedException(final ThrowableStackTraceRenderer<?> renderer) {
        final Exception e1 = new Exception();
        final Exception e2 = new Exception(e1);
        e2.addSuppressed(e1);
        e1.addSuppressed(e2);

        render(renderer, e1);
    }

    @ParameterizedTest
    @MethodSource("renderers_dataSource")
    void testCircularCauseExceptions(final ThrowableStackTraceRenderer<?> renderer) {
        final Exception e1 = new Exception();
        final Exception e2 = new Exception(e1);
        e1.initCause(e2);
        render(renderer, e1);
    }

    /**
     * Default setting ThrowableRenderer render output should equal to throwable.printStackTrace().
     */
    @Test
    public void testThrowableRenderer() {
        final Throwable throwable = createException("r", 1, 3);
        final ThrowableStackTraceRenderer<?> renderer =
                new ThrowableStackTraceRenderer<>(Collections.emptyList(), Integer.MAX_VALUE);
        String actual = render(renderer, throwable);
        assertThat(actual).isEqualTo(getStandardThrowableStackTrace(throwable));
    }

    private static String render(final ThrowableStackTraceRenderer<?> renderer, final Throwable throwable) {
        final StringBuilder stringBuilder = new StringBuilder();
        renderer.renderThrowable(stringBuilder, throwable, System.lineSeparator());
        return stringBuilder.toString();
    }

    private static String getStandardThrowableStackTrace(final Throwable throwable) {
        final StringBuilder buffer = new StringBuilder();
        final PrintWriter printWriter = new PrintWriter(new StringBuilderWriter(buffer));
        throwable.printStackTrace(printWriter);
        return buffer.toString();
    }

    private static String buildPattern(
            final String exceptionPattern,
            final String filters,
            final Integer depth,
            final String suffix,
            final String lineSeparator) {
        final StringBuilder buffer = new StringBuilder("%m");
        buffer.append(exceptionPattern);
        if (filters != null) {
            buffer.append("{filters(");
            buffer.append(filters);
            buffer.append(")}");
        }

        if (depth != null) {
            buffer.append("{");
            buffer.append(depth);
            buffer.append("}");
        }

        if (suffix != null) {
            buffer.append("{suffix(");
            buffer.append(suffix);
            buffer.append(")}");
        }

        if (lineSeparator != null) {
            buffer.append("{separator(");
            buffer.append(lineSeparator);
            buffer.append(")}");
        }
        return buffer.toString();
    }

    private static void verifyFilters(final String message, final String filters) {
        if (filters != null) {
            assertThat(message).contains("suppressed");
            final String[] filterArray = filters.split(",");
            for (final String filter : filterArray) {
                assertThat(message).doesNotContain(filter);
            }
        } else {
            assertThat(message).doesNotContain("suppressed");
        }
    }

    private static void verifyDepth(final String message, final Integer depth) {
        if (depth != null) {
            assertThat(message).hasLineCount(depth);
        }
    }

    private static void verifySuffix(final String message, final String suffix, final String lineSeparator) {
        if (suffix != null && lineSeparator != null) {
            for (String line : message.split(lineSeparator)) {
                assertThat(line).endsWith(suffix);
            }
        }
    }

    private static Throwable createException(final String name, int depth, int maxDepth) {
        Exception r = new Exception(name);
        if (depth < maxDepth) {
            r.initCause(createException(name + "_c", depth + 1, maxDepth));
            r.addSuppressed(createException(name + "_s", depth + 1, maxDepth));
        }
        return r;
    }
}
