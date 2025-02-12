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
package org.apache.logging.log4j.core.appender;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

public class MultipleSocketAppenderTest {

    private static final String CLASS_NAME = MultipleSocketAppenderTest.class.getSimpleName();

    private static final int EPHEMERAL_PORT = 0;

    private static final String APPENDER_NAME = "TestMultipleSocket";

    private static final AtomicInteger LOGGER_CONTEXT_COUNTER = new AtomicInteger();

    @Test
    @UsingStatusListener
    void test1() throws Exception {
        try (final LineReadingTcpServer server = new LineReadingTcpServer()) {
            server.start("Main", EPHEMERAL_PORT);
            final String serverHost = server.getServerSocket().getInetAddress().getHostAddress();
            final int serverPort = server.getServerSocket().getLocalPort();
            final Configuration config = createConfiguration(serverHost + ":" + serverPort);
            try (final LoggerContext loggerContext = createStartedLoggerContext(config)) {
                final BufferingErrorHandler errorHandler = new BufferingErrorHandler();
                loggerContext.getConfiguration().getAppender(APPENDER_NAME).setHandler(errorHandler);

                StatusLogger.getLogger().info("verifyLoggingSuccess...");
                verifyLoggingSuccess(loggerContext, server, errorHandler);

                server.close();

                StatusLogger.getLogger().info("verifyLoggingFailure...");
                verifyLoggingFailure(loggerContext, errorHandler);

                server.start("Main", serverPort);
                // reconnecting
                Thread.sleep(10000);

                StatusLogger.getLogger().info("verifyLoggingSuccess...");
                verifyLoggingSuccess(loggerContext, server, errorHandler);
            }
        }
    }

    @Test
    @UsingStatusListener
    void test2() throws Exception {
        try (final LineReadingTcpServer server = new LineReadingTcpServer()) {
            server.start("Main", EPHEMERAL_PORT);
            final String serverHost = server.getServerSocket().getInetAddress().getHostAddress();
            final int serverPort = server.getServerSocket().getLocalPort();
            final Configuration config =
                    createConfiguration(serverHost + ":" + serverPort + "," + serverHost + ":" + serverPort);
            try (final LoggerContext loggerContext = createStartedLoggerContext(config)) {
                final BufferingErrorHandler errorHandler = new BufferingErrorHandler();
                loggerContext.getConfiguration().getAppender(APPENDER_NAME).setHandler(errorHandler);

                StatusLogger.getLogger().info("verifyLoggingSuccess...");
                verifyLoggingSuccess(loggerContext, server, errorHandler);

                server.close();

                StatusLogger.getLogger().info("verifyLoggingFailure...");
                verifyLoggingFailure(loggerContext, errorHandler);

                server.start("Main", serverPort);
                // reconnecting
                Thread.sleep(10000);

                StatusLogger.getLogger().info("verifyLoggingSuccess...");
                verifyLoggingSuccess(loggerContext, server, errorHandler);
            }
        }
    }

    private static Configuration createConfiguration(String serverList) {
        // Create the configuration builder
        final ConfigurationBuilder<BuiltConfiguration> configBuilder =
                ConfigurationBuilderFactory.newConfigurationBuilder()
                        .setStatusLevel(Level.INFO)
                        .setConfigurationName(MultipleSocketAppenderTest.class.getSimpleName());
        // Create the appender configuration
        final AppenderComponentBuilder appenderComponentBuilder = configBuilder
                .newAppender(APPENDER_NAME, "MultipleSocket")
                .addAttribute("serverList", serverList)
                .addAttribute("ignoreExceptions", false)
                .addAttribute("reconnectionDelayMillis", 5000)
                .addAttribute("immediateFlush", true)
                .add(configBuilder.newLayout("PatternLayout").addAttribute("pattern", "%m%n"));
        // Create the configuration
        return configBuilder
                .add(appenderComponentBuilder)
                .add(configBuilder.newRootLogger(Level.ALL).add(configBuilder.newAppenderRef(APPENDER_NAME)))
                .build(false);
    }

    private static LoggerContext createStartedLoggerContext(final Configuration configuration) {
        final String name = String.format(
                "%s-%02d", MultipleSocketAppenderTest.class.getSimpleName(), LOGGER_CONTEXT_COUNTER.getAndIncrement());
        final LoggerContext loggerContext = new LoggerContext(name, null, (String) null, DI.createInitializedFactory());
        loggerContext.start(configuration);
        return loggerContext;
    }

    private static void verifyLoggingSuccess(
            final LoggerContext loggerContext,
            final LineReadingTcpServer server,
            final BufferingErrorHandler errorHandler)
            throws Exception {
        // Report status
        StatusLogger.getLogger().debug("[{}] verifying logging success", CLASS_NAME);
        // Create messages to log
        final int messageCount = 2;
        final List<String> expectedMessages = IntStream.range(0, messageCount)
                .mapToObj(messageIndex -> String.format("m%02d", messageIndex))
                .collect(Collectors.toList());
        // Log the 1st message
        final Logger logger = loggerContext.getRootLogger();
        Awaitility.await("first socket append")
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(120, TimeUnit.SECONDS)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    final String message = expectedMessages.get(0);
                    logger.info(message);
                });
        // Reset the error handler
        errorHandler.clear();
        // Log the rest of the messages
        for (int messageIndex = 1; messageIndex < expectedMessages.size(); messageIndex++) {
            final String message = expectedMessages.get(messageIndex);
            logger.info(message);
        }
        // Verify the messages received by the server
        final List<String> actualMessages = server.pollLines(messageCount);
        Assertions.assertThat(actualMessages).containsExactlyElementsOf(expectedMessages);
        // Verify the error handler state
        Assertions.assertThat(errorHandler.getBuffer()).isEmpty();
    }

    private static void verifyLoggingFailure(
            final LoggerContext loggerContext, final BufferingErrorHandler errorHandler) {
        StatusLogger.getLogger().debug("[{}] verifying logging failure", CLASS_NAME);
        final Logger logger = loggerContext.getRootLogger();
        final int retryCount = 3;
        for (int i = 0; i < retryCount; i++) {
            try {
                logger.info("should fail #" + i);
                Assertions.fail("should have failed #" + i);
            } catch (final AppenderLoggingException ignored) {
                Assertions.assertThat(errorHandler.getBuffer()).hasSize(2 * (i + 1));
            }
        }
    }
}
