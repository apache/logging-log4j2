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
package org.apache.logging.log4j.core.net;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.net.TcpSocketManager.HostResolver;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;

/**
 * Tests reconnection support of {@link org.apache.logging.log4j.core.appender.SocketAppender}.
 */
public class SocketAppenderReconnectTest {

    private static final long LOGGER_CONTEXT_STOP_WAIT_PERIOD_MILLIS = 5_000L;

    private static final int EPHEMERAL_PORT = 0;

    /**
     * Tests if failures are propagated when reconnection fails.
     *
     * @see <a href="https://issues.apache.org/jira/browse/LOG4J2-2829">LOG4J2-2829</a>
     */
    @Test
    @UsingStatusListener
    void repeating_reconnect_failures_should_be_propagated() throws Exception {
        final BufferingErrorHandler errorHandler = new BufferingErrorHandler();
        try (final LineReadingTcpServer server = new LineReadingTcpServer()) {

            // Start the server.
            server.start("Main", EPHEMERAL_PORT);
            final int port = server.getServerSocket().getLocalPort();

            // Initialize the logger context.
            final LoggerContext loggerContext = initContext(port, errorHandler);
            final Logger logger = loggerContext.getRootLogger();
            try {

                // Verify the initial working state.
                verifyLoggingSuccess(logger, server, errorHandler);

                // Stop the server, and verify the logging failure.
                server.close();
                verifyLoggingFailure(logger, errorHandler);

                // Start the server again, and verify the logging success.
                server.start("Main", port);
                verifyLoggingSuccess(logger, server, errorHandler);

            }

            // Shutdown the logger context
            finally {
                loggerContext.stop(LOGGER_CONTEXT_STOP_WAIT_PERIOD_MILLIS, TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * Tests if all the {@link InetSocketAddress}es returned by an {@link HostResolver} is used for fallback on reconnect attempts.
     */
    @Test
    @UsingStatusListener
    void reconnect_should_fallback_when_there_are_multiple_resolved_hosts() throws Exception {
        final BufferingErrorHandler errorHandler = new BufferingErrorHandler();
        try (final LineReadingTcpServer primaryServer = new LineReadingTcpServer();
                final LineReadingTcpServer secondaryServer = new LineReadingTcpServer()) {

            // Start servers.
            primaryServer.start("Primary", EPHEMERAL_PORT);
            secondaryServer.start("Secondary", EPHEMERAL_PORT);

            // Mock the host resolver.
            final FixedHostResolver hostResolver = FixedHostResolver.ofServers(primaryServer, secondaryServer);
            TcpSocketManager.setHostResolver(hostResolver);
            try {

                // Initialize the logger context.
                final LoggerContext loggerContext = initContext(
                        // Passing an invalid port, since the resolution is supposed to be performed by the mocked host
                        // resolver anyway.
                        0, errorHandler);
                final Logger logger = loggerContext.getRootLogger();
                try {

                    // Verify the initial working state on the primary server.
                    verifyLoggingSuccess(logger, primaryServer, errorHandler);

                    // Stop the primary server, and verify the logging success due to fallback on to the secondary
                    // server.
                    primaryServer.close();
                    verifyLoggingSuccess(logger, secondaryServer, errorHandler);

                }

                // Shutdown the logger context
                finally {
                    loggerContext.stop(LOGGER_CONTEXT_STOP_WAIT_PERIOD_MILLIS, TimeUnit.MILLISECONDS);
                }

            }

            // Reset the host resolver
            finally {
                TcpSocketManager.setHostResolver(new HostResolver());
            }
        }
    }

    private static LoggerContext initContext(final int port, final BufferingErrorHandler handler) {

        // Create the configuration builder.
        final ConfigurationBuilder<BuiltConfiguration> configBuilder =
                ConfigurationBuilderFactory.newConfigurationBuilder()
                        .setStatusLevel(Level.ERROR)
                        .setConfigurationName(SocketAppenderReconnectTest.class.getSimpleName());

        // Create the configuration
        final String appenderName = "Socket";
        final Configuration config = configBuilder
                .add(configBuilder
                        .newAppender(appenderName, "SOCKET")
                        .addAttribute("host", "localhost")
                        .addAttribute("port", String.valueOf(port))
                        .addAttribute("protocol", Protocol.TCP)
                        .addAttribute("ignoreExceptions", false)
                        .addAttribute("reconnectionDelayMillis", 10)
                        .addAttribute("immediateFlush", true)
                        .add(configBuilder.newLayout("PatternLayout").addAttribute("pattern", "%m%n")))
                .add(configBuilder.newRootLogger(Level.ALL).add(configBuilder.newAppenderRef(appenderName)))
                .setStatusLevel(Level.OFF)
                .build(true);

        // Set the error handler
        config.getAppender("Socket").setHandler(handler);

        // Create the logger context
        return Configurator.initialize(config);
    }

    private static void verifyLoggingSuccess(final Logger logger, final LineReadingTcpServer server, final BufferingErrorHandler errorHandler)
            throws Exception {

        // Create messages to log
        final int messageCount = 100;
        assertThat(messageCount)
                .as("expecting `messageCount > 1` due to LOG4J2-2829")
                .isGreaterThan(1);
        final List<String> expectedMessages = IntStream.range(0, messageCount)
                .mapToObj(messageIndex -> String.format("m%02d", messageIndex))
                .collect(Collectors.toList());

        // Log the 1st message
        // Due to socket initialization, the first `write()` might need some extra effort
        await("first socket append")
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(120, TimeUnit.SECONDS)
                .until(() -> {
                    logger.info(expectedMessages.get(0));
                    return true;
                });

        // Reset the error handler
        errorHandler.clear();

        // Log the rest of the messages
        for (int messageIndex = 1; messageIndex < expectedMessages.size(); messageIndex++) {
            logger.info(expectedMessages.get(messageIndex));
        }

        // Verify the messages received by the server
        final List<String> actualMessages = server.pollLines(messageCount);
        assertThat(actualMessages).containsExactlyElementsOf(expectedMessages);

        // Verify the error handler state
        assertThat(errorHandler.getBuffer()).isEmpty();
    }

    private static void verifyLoggingFailure(final Logger logger, final BufferingErrorHandler errorHandler) {
        final int retryCount = 3;
        assertThat(retryCount)
                .as("expecting `retryCount > 1` due to LOG4J2-2829")
                .isGreaterThan(1);
        for (int i = 0; i < retryCount; i++) {
            try {
                logger.info("should fail #" + i);
                fail("should have failed #" + i);
            } catch (final AppenderLoggingException ignored) {
                assertThat(errorHandler.getBuffer()).hasSize(2 * (i + 1));
            }
        }
    }

    /**
     * {@link TcpSocketManager.HostResolver} implementation always resolving to the given list of {@link #addresses}.
     */
    private static final class FixedHostResolver extends TcpSocketManager.HostResolver {

        private final List<InetSocketAddress> addresses;

        private FixedHostResolver(final List<InetSocketAddress> addresses) {
            this.addresses = addresses;
        }

        private static FixedHostResolver ofServers(final LineReadingTcpServer... servers) {
            final List<InetSocketAddress> addresses = Arrays.stream(servers)
                    .map(server -> (InetSocketAddress) server.getServerSocket().getLocalSocketAddress())
                    .collect(Collectors.toList());
            return new FixedHostResolver(addresses);
        }

        @Override
        public List<InetSocketAddress> resolveHost(final String ignoredHost, final int ignoredPort) {
            return addresses;
        }
    }

}
