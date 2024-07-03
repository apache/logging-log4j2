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
                Arguments.of("%ex", filters, null),
                Arguments.of("%ex", null, depth),
                // RootThrowable
                Arguments.of("%rEx", filters, null),
                Arguments.of("%rEx", null, depth),
                // ExtendedThrowable
                Arguments.of("%xEx", filters, null),
                Arguments.of("%xEx", null, depth));
    }

    @ParameterizedTest
    @MethodSource("testConverter_dataSource")
    void testConverter(String exceptionPattern, String filters, Integer depth) {
        final String pattern = buildPattern(exceptionPattern, filters, depth);
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
            final Throwable cause = new NullPointerException("null pointer");
            final Throwable parent = new IllegalArgumentException("IllegalArgument", cause);

            final Logger logger = loggerContext.getLogger(LoggerTest.class);
            final ListAppender appender = loggerContext.getConfiguration().getAppender(appenderName);
            logger.error("Exception", parent);

            assertThat(appender.getMessages()).hasSize(1);
            final String message = appender.getMessages().get(0);
            assertThat(message).isNotNull();
            verifyFilters(message, filters);
            verifyDepth(message, depth);
        }
    }

    private static String buildPattern(String exceptionPattern, String filters, Integer depth) {
        final StringBuilder buffer = new StringBuilder("%m");
        buffer.append(exceptionPattern);
        if (filters != null) {
            buffer.append("{filters(");
            buffer.append(filters);
            buffer.append(")})");
        }

        if (depth != null) {
            buffer.append("{");
            buffer.append(depth);
            buffer.append("}");
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
}
