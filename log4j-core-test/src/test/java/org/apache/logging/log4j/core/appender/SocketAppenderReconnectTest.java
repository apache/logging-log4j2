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

import static org.apache.logging.log4j.core.appender.SslContexts.createSslContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.fail;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.net.ssl.SSLContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.net.TcpSocketManager;
import org.apache.logging.log4j.core.net.TcpSocketManager.HostResolver;
import org.apache.logging.log4j.core.net.ssl.SslKeyStoreConstants;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests reconnection support of {@link org.apache.logging.log4j.core.appender.SocketAppender}.
 */
class SocketAppenderReconnectTest {

    private static final String CLASS_NAME = SocketAppenderReconnectTest.class.getSimpleName();

    private static final int EPHEMERAL_PORT = 0;

    private static final String APPENDER_NAME = "TestSocket";

    /**
     * Tests if failures are propagated when reconnection fails.
     *
     * @see <a href="https://issues.apache.org/jira/browse/LOG4J2-2829">LOG4J2-2829</a>
     */
    @Test
    @UsingStatusListener // Suppresses `StatusLogger` output, unless there is a failure
    void repeating_reconnect_failures_should_be_propagated() throws Exception {
        try (final LineReadingTcpServer server = new LineReadingTcpServer()) {

            // Start the server.
            server.start("Main", EPHEMERAL_PORT);
            final String serverHost = server.getServerSocket().getInetAddress().getHostAddress();
            final int serverPort = server.getServerSocket().getLocalPort();

            // Initialize the logger context
            final Configuration config = createConfiguration(serverHost, serverPort, null);
            try (final LoggerContext loggerContext = createStartedLoggerContext(config)) {

                // Configure the error handler
                final BufferingErrorHandler errorHandler = new BufferingErrorHandler();
                loggerContext.getConfiguration().getAppender(APPENDER_NAME).setHandler(errorHandler);

                // Verify the initial working state.
                verifyLoggingSuccess(loggerContext, server, errorHandler);

                // Stop the server, and verify the logging failure.
                server.close();
                verifyLoggingFailure(loggerContext, errorHandler);

                // Start the server again, and verify the logging success.
                server.start("Main", serverPort);
                verifyLoggingSuccess(loggerContext, server, errorHandler);
            }
        }
    }

    /**
     * Tests if all the {@link InetSocketAddress}es returned by an {@link HostResolver} is used for fallback on reconnect attempts.
     */
    @Test
    @UsingStatusListener // Suppresses `StatusLogger` output, unless there is a failure
    void reconnect_should_fallback_when_there_are_multiple_resolved_hosts() throws Exception {
        try (final LineReadingTcpServer primaryServer = new LineReadingTcpServer();
                final LineReadingTcpServer secondaryServer = new LineReadingTcpServer()) {

            // Start servers.
            primaryServer.start("Primary", EPHEMERAL_PORT);
            secondaryServer.start("Secondary", EPHEMERAL_PORT);

            // Mock the host resolver.
            final FixedHostResolver hostResolver = FixedHostResolver.ofServers(primaryServer, secondaryServer);
            TcpSocketManager.setHostResolver(hostResolver);
            try {

                // Initialize the logger context
                final Configuration config = createConfiguration(
                        // Passing dummy host & port, since the resolution is supposed to be performed by the mocked
                        // host resolver anyway.
                        "localhost", 0, null);
                try (final LoggerContext loggerContext = createStartedLoggerContext(config)) {

                    // Configure the error handler
                    final BufferingErrorHandler errorHandler = new BufferingErrorHandler();
                    loggerContext.getConfiguration().getAppender(APPENDER_NAME).setHandler(errorHandler);

                    // Verify the initial working state on the primary server.
                    verifyLoggingSuccess(loggerContext, primaryServer, errorHandler);

                    // Stop the primary server, and verify the logging success due to fallback on to the secondary
                    // server.
                    primaryServer.close();
                    verifyLoggingSuccess(loggerContext, secondaryServer, errorHandler);
                }

            }

            // Reset the host resolver
            finally {
                TcpSocketManager.setHostResolver(new HostResolver());
            }
        }
    }

    /**
     * Triggers a reconfiguration such that the {@code <KeyStore>} and {@code <TrustStore>} configuration will be unchanged, but the content they refer to will be updated.
     *
     * @see <a href="https://issues.apache.org/jira/browse/LOG4J2-2988">LOG4J2-2988</a>
     * @see <a href="https://github.com/apache/logging-log4j2/pull/2767">#2767</a>
     */
    @Test
    void key_store_changes_should_be_detected_at_reconfigure(
            @TempDir(cleanup = CleanupMode.ON_SUCCESS) final Path tempDir) throws Exception {

        // Create the 1st `SSLContext`
        final String keyStore1Type = SslKeyStoreConstants.KEYSTORE_TYPE;
        final String keyStore1Location = SslKeyStoreConstants.KEYSTORE_LOCATION;
        final char[] keyStore1Password = SslKeyStoreConstants.KEYSTORE_PWD();
        final String trustStore1Type = SslKeyStoreConstants.TRUSTSTORE_TYPE;
        final String trustStore1Location = SslKeyStoreConstants.TRUSTSTORE_LOCATION;
        final char[] trustStore1Password = SslKeyStoreConstants.TRUSTSTORE_PWD();
        final SSLContext sslContext1 = createSslContext(
                keyStore1Type,
                keyStore1Location,
                keyStore1Password,
                trustStore1Type,
                trustStore1Location,
                trustStore1Password);

        // Create the 2nd `SSLContext`
        final String keyStore2Type = SslKeyStoreConstants.KEYSTORE2_TYPE;
        final String keyStore2Location = SslKeyStoreConstants.KEYSTORE2_LOCATION;
        final char[] keyStore2Password = SslKeyStoreConstants.KEYSTORE2_PWD();
        final String trustStore2Type = SslKeyStoreConstants.TRUSTSTORE2_TYPE;
        final String trustStore2Location = SslKeyStoreConstants.TRUSTSTORE2_LOCATION;
        final char[] trustStore2Password = SslKeyStoreConstants.TRUSTSTORE2_PWD();
        final SSLContext sslContext2 = createSslContext(
                keyStore2Type,
                keyStore2Location,
                keyStore2Password,
                trustStore2Type,
                trustStore2Location,
                trustStore2Password);

        // Ensure that store types are identical.
        // We will use these in the `*StoreConfiguration`.
        // They need to be same, so that the encapsulating `SslConfiguration` will be unchanged during reconfiguration.
        assertThat(keyStore1Type).isEqualTo(keyStore2Type);

        // Stage the key store files using the 1st `SSLContext`
        @SuppressWarnings("UnnecessaryLocalVariable")
        final String keyStoreType = keyStore1Type;
        final Path keyStoreFilePath = tempDir.resolve("keyStore");
        Files.write(keyStoreFilePath, Files.readAllBytes(Paths.get(keyStore1Location)));
        final Path keyStorePasswordFilePath = tempDir.resolve("keyStorePassword");
        Files.write(keyStorePasswordFilePath, new String(keyStore1Password).getBytes(StandardCharsets.UTF_8));

        // Stage the trust store files using the 1st `SSLContext`
        @SuppressWarnings("UnnecessaryLocalVariable")
        final String trustStoreType = trustStore1Type;
        final Path trustStoreFilePath = tempDir.resolve("trustStore");
        Files.write(trustStoreFilePath, Files.readAllBytes(Paths.get(trustStore1Location)));
        final Path trustStorePasswordFilePath = tempDir.resolve("trustStorePassword");
        Files.write(trustStorePasswordFilePath, new String(trustStore1Password).getBytes(StandardCharsets.UTF_8));

        // Create servers
        try (final LineReadingTcpServer server1 = new LineReadingTcpServer(sslContext1.getServerSocketFactory());
                final LineReadingTcpServer server2 = new LineReadingTcpServer(sslContext2.getServerSocketFactory())) {

            // Start the 1st server
            server1.start("1st", EPHEMERAL_PORT);
            final String server1Host =
                    server1.getServerSocket().getInetAddress().getHostAddress();
            final int server1Port = server1.getServerSocket().getLocalPort();

            // Create the configuration transformer to add the `<Ssl>`, `<KeyStore>`, and `<TrustStore>` elements
            final BiFunction<
                            ConfigurationBuilder<BuiltConfiguration>,
                            AppenderComponentBuilder,
                            AppenderComponentBuilder>
                    appenderComponentBuilderTransformer = (configBuilder, appenderComponentBuilder) -> {
                        final ComponentBuilder<?> keyStoreComponentBuilder = configBuilder
                                .newComponent("KeyStore")
                                .addAttribute("type", keyStoreType)
                                .addAttribute("location", keyStoreFilePath.toString())
                                .addAttribute("passwordFile", keyStorePasswordFilePath);
                        final ComponentBuilder<?> trustStoreComponentBuilder = configBuilder
                                .newComponent("TrustStore")
                                .addAttribute("type", trustStoreType)
                                .addAttribute("location", trustStoreFilePath.toString())
                                .addAttribute("passwordFile", trustStorePasswordFilePath);
                        return appenderComponentBuilder.addComponent(configBuilder
                                .newComponent("Ssl")
                                .addAttribute("protocol", "TLS")
                                .addComponent(keyStoreComponentBuilder)
                                .addComponent(trustStoreComponentBuilder));
                    };

            // Initialize the logger context
            final Configuration config1 =
                    createConfiguration(server1Host, server1Port, appenderComponentBuilderTransformer);
            try (final LoggerContext loggerContext = createStartedLoggerContext(config1)) {

                // Configure the error handler
                final BufferingErrorHandler errorHandler = new BufferingErrorHandler();
                loggerContext.getConfiguration().getAppender(APPENDER_NAME).setHandler(errorHandler);

                // Verify the initial working state on the 1st server
                verifyLoggingSuccess(loggerContext, server1, errorHandler);

                // Stop the 1st server and start the 2nd one (using different SSL configuration!) on the same port
                server1.close();
                server2.start("2nd", server1Port);

                // Stage the key store files using the 2nd `SSLContext`
                Files.write(keyStoreFilePath, Files.readAllBytes(Paths.get(keyStore2Location)));
                Files.write(keyStorePasswordFilePath, new String(keyStore2Password).getBytes(StandardCharsets.UTF_8));

                // Stage the trust store files using the 2nd `SSLContext`
                Files.write(trustStoreFilePath, Files.readAllBytes(Paths.get(trustStore2Location)));
                Files.write(
                        trustStorePasswordFilePath, new String(trustStore2Password).getBytes(StandardCharsets.UTF_8));

                // Reconfigure the logger context
                //
                // You might be thinking:
                //
                //     Why don't we simply call `loggerContext.reconfigure()`?
                //     Why do we need to create the very same configuration twice?
                //
                // We need to reconfigure the logger context using the very same configuration to test if
                // `SslSocketManager` will be able to pick up the key and trust store changes even though the
                // `SslConfiguration` is untouched – the whole point of this test and the issue reported in LOG4J2-2988.
                //
                // `loggerContext.reconfigure()` stops the active configuration (i.e., the programmatically built
                // configuration we provided to it), and starts a fresh scan for `log4j2.xml` et al. in the classpath.
                // This effectively discards the initially provided configuration.
                //
                // `loggerContext.reconfigure(loggerContext.getConfiguration())` doesn't work either, since it tries to
                // start the configuration it has just stopped and a programmatically built `Configuration` is not
                // `Reconfigurable` – yes, this needs to be fixed.
                //
                // Hence, the only way is to programmatically build the very same configuration, twice, and use the 1st
                // one for initialization, and the 2nd one for reconfiguration.
                final Configuration config2 =
                        createConfiguration(server1Host, server1Port, appenderComponentBuilderTransformer);
                loggerContext.reconfigure(config2);

                // Verify the working state on the 2nd server
                verifyLoggingSuccess(loggerContext, server2, errorHandler);
            }
        }
    }

    private static Configuration createConfiguration(
            final String host,
            final int port,
            @Nullable
                    final BiFunction<
                                    ConfigurationBuilder<BuiltConfiguration>,
                                    AppenderComponentBuilder,
                                    AppenderComponentBuilder>
                            appenderComponentBuilderTransformer) {

        // Create the configuration builder
        final ConfigurationBuilder<BuiltConfiguration> configBuilder =
                ConfigurationBuilderFactory.newConfigurationBuilder()
                        .setStatusLevel(Level.ERROR)
                        .setConfigurationName(SocketAppenderReconnectTest.class.getSimpleName());

        // Create the appender configuration
        final AppenderComponentBuilder appenderComponentBuilder = configBuilder
                .newAppender(APPENDER_NAME, "Socket")
                .addAttribute("host", host)
                .addAttribute("port", port)
                .addAttribute("ignoreExceptions", false)
                .addAttribute("reconnectionDelayMillis", 10)
                .addAttribute("immediateFlush", true)
                .add(configBuilder.newLayout("PatternLayout").addAttribute("pattern", "%m%n"));
        final AppenderComponentBuilder transformedAppenderComponentBuilder = appenderComponentBuilderTransformer != null
                ? appenderComponentBuilderTransformer.apply(configBuilder, appenderComponentBuilder)
                : appenderComponentBuilder;

        // Create the configuration
        return configBuilder
                .add(transformedAppenderComponentBuilder)
                .add(configBuilder.newRootLogger(Level.ALL).add(configBuilder.newAppenderRef(APPENDER_NAME)))
                .build(false);
    }

    private static final AtomicInteger LOGGER_CONTEXT_COUNTER = new AtomicInteger();

    private static LoggerContext createStartedLoggerContext(final Configuration configuration) {
        final String name = String.format(
                "%s-%02d", SocketAppenderReconnectTest.class.getSimpleName(), LOGGER_CONTEXT_COUNTER.getAndIncrement());
        final LoggerContext loggerContext = new LoggerContext(name);
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
        assertThat(messageCount)
                .as("expecting `messageCount > 1` due to LOG4J2-2829")
                .isGreaterThan(1);
        final List<String> expectedMessages = IntStream.range(0, messageCount)
                .mapToObj(messageIndex -> String.format("m%02d", messageIndex))
                .collect(Collectors.toList());

        // Log the 1st message
        // Due to socket initialization, the first `write()` might need some extra effort
        final Logger logger = loggerContext.getRootLogger();
        await("first socket append")
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
        assertThat(actualMessages).containsExactlyElementsOf(expectedMessages);

        // Verify the error handler state
        assertThat(errorHandler.getBuffer()).isEmpty();
    }

    private static void verifyLoggingFailure(
            final LoggerContext loggerContext, final BufferingErrorHandler errorHandler) {

        // Report status
        StatusLogger.getLogger().debug("[{}] verifying logging failure", CLASS_NAME);

        // Verify the configuration
        final Logger logger = loggerContext.getRootLogger();
        final int retryCount = 3;
        assertThat(retryCount)
                .as("expecting `retryCount > 1` due to LOG4J2-2829")
                .isGreaterThan(1);

        // Verify the failure
        for (int i = 0; i < retryCount; i++) {
            try {
                logger.info("should fail #" + i);
                fail("should have failed #" + i);
            } catch (final AppenderLoggingException ignored) {
                assertThat(errorHandler.getBuffer()).hasSize(2 * (i + 1));
            }
        }
    }
}
