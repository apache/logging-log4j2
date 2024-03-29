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
package org.apache.logging.log4j.async.logger;

import java.net.URI;
import java.util.stream.Stream;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.plugins.di.DI;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DefaultIncludeLocationTest {

    private static final String LOGGER_NAME = DefaultIncludeLocationTest.class.getName();

    private static Stream<Arguments> loggerConfigs(final Configuration config) {
        return Stream.of(
                Arguments.of(
                        LoggerConfig.newBuilder()
                                .setConfig(config)
                                .setLoggerName(LOGGER_NAME)
                                .build(),
                        true),
                Arguments.of(
                        LoggerConfig.RootLogger.newRootBuilder()
                                .setConfig(config)
                                .build(),
                        true),
                Arguments.of(
                        AsyncLoggerConfig.newAsyncBuilder()
                                .setConfig(config)
                                .setLoggerName(LOGGER_NAME)
                                .build(),
                        false),
                Arguments.of(
                        AsyncLoggerConfig.RootLogger.newAsyncRootBuilder()
                                .setConfig(config)
                                .build(),
                        false));
    }

    static Stream<Arguments> loggerContextDefaultLocation() {
        final LoggerContext ctx = new LoggerContext("sync", null, (URI) null, DI.createInitializedFactory());
        final NullConfiguration config = new NullConfiguration(ctx);
        ctx.setConfiguration(config);
        return loggerConfigs(config);
    }

    @ParameterizedTest
    @MethodSource
    void loggerContextDefaultLocation(final LoggerConfig loggerConfig, final boolean expected) {
        Assertions.assertThat(loggerConfig.isIncludeLocation())
                .as("Default `includeLocation` value")
                .isEqualTo(expected);
    }

    static Stream<LoggerConfig> asyncLoggerContextDefaultLocation() {
        final AsyncLoggerContext ctx = new AsyncLoggerContext("async", null, null, DI.createInitializedFactory());
        final NullConfiguration config = new NullConfiguration(ctx);
        ctx.setConfiguration(config);
        return loggerConfigs(config).map(args -> (LoggerConfig) args.get()[0]);
    }

    @ParameterizedTest
    @MethodSource
    void asyncLoggerContextDefaultLocation(final LoggerConfig loggerConfig) {
        Assertions.assertThat(loggerConfig.isIncludeLocation())
                .as("Default `includeLocation` value")
                .isFalse();
    }
}
