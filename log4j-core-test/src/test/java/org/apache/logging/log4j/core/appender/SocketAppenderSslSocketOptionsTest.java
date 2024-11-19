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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.net.ssl.SSLContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.net.Rfc1349TrafficClass;
import org.apache.logging.log4j.core.net.SocketOptions;
import org.apache.logging.log4j.core.net.SocketPerformancePreferences;
import org.apache.logging.log4j.core.net.SslSocketManager;
import org.apache.logging.log4j.core.net.ssl.SslKeyStoreConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

class SocketAppenderSslSocketOptionsTest {

    private static final String APPENDER_NAME = "TestSocket";

    // Socket performance preferences

    private static final int SOCKET_PERFORMANCE_PREFERENCE_BANDWIDTH = 100;

    private static final int SOCKET_PERFORMANCE_PREFERENCE_CONNECTION_TIME = 101;

    private static final int SOCKET_PERFORMANCE_PREFERENCE_LATENCY = 102;

    // Socket options

    private static final boolean SOCKET_OPTION_KEEP_ALIVE = false;

    private static final int SOCKET_OPTION_RECEIVE_BUFFER_SIZE = 10_000;

    private static final boolean SOCKET_OPTION_REUSE_ADDRESS = false;

    private static final Rfc1349TrafficClass SOCKET_OPTION_RFC1349_TRAFFIC_CLASS = Rfc1349TrafficClass.IPTOS_LOWCOST;

    private static final int SOCKET_OPTION_SEND_BUFFER_SIZE = 8_000;

    private static final int SOCKET_OPTION_LINGER = 12_345;

    private static final int SOCKET_OPTION_TIMEOUT = 54_321;

    private static final boolean SOCKET_OPTION_TCP_NO_DELAY = false;

    // Key & Trust store

    private static final String KEYSTORE_TYPE = SslKeyStoreConstants.KEYSTORE_TYPE;

    private static final String KEYSTORE_LOCATION = SslKeyStoreConstants.KEYSTORE_LOCATION;

    private static final char[] KEYSTORE_PWD = SslKeyStoreConstants.KEYSTORE_PWD();

    private static final String TRUSTSTORE_TYPE = SslKeyStoreConstants.TRUSTSTORE_TYPE;

    private static final String TRUSTSTORE_LOCATION = SslKeyStoreConstants.TRUSTSTORE_LOCATION;

    private static final char[] TRUSTSTORE_PWD = SslKeyStoreConstants.TRUSTSTORE_PWD();

    @Test
    void socket_options_should_be_injected(@TempDir(cleanup = CleanupMode.ON_SUCCESS) final Path tempDir)
            throws Exception {

        // Create the SSL context
        final SSLContext sslContext = SslContexts.createSslContext(
                KEYSTORE_TYPE, KEYSTORE_LOCATION, KEYSTORE_PWD, TRUSTSTORE_TYPE, TRUSTSTORE_LOCATION, TRUSTSTORE_PWD);

        // Create the server
        try (final LineReadingTcpServer server = new LineReadingTcpServer(sslContext.getServerSocketFactory())) {

            // Start the server
            server.start("1st", 0);
            final int port = server.getServerSocket().getLocalPort();

            // Create the logger context
            final Configuration config = createConfiguration(tempDir, port);
            try (final LoggerContext loggerContext = Configurator.initialize(config)) {

                // Extract the socket manager
                final SocketAppender appender = loggerContext.getConfiguration().getAppender(APPENDER_NAME);
                final SslSocketManager manager = (SslSocketManager) appender.getManager();

                // Verify socket options
                final SocketOptions socketOptions = manager.getSocketOptions();
                assertThat(socketOptions.isKeepAlive()).isEqualTo(SOCKET_OPTION_KEEP_ALIVE);
                assertThat(socketOptions.isOobInline()).isNull();
                assertThat(socketOptions.getReceiveBufferSize()).isEqualTo(SOCKET_OPTION_RECEIVE_BUFFER_SIZE);
                assertThat(socketOptions.isReuseAddress()).isEqualTo(SOCKET_OPTION_REUSE_ADDRESS);
                assertThat(socketOptions.getRfc1349TrafficClass()).isEqualTo(SOCKET_OPTION_RFC1349_TRAFFIC_CLASS);
                assertThat(socketOptions.getActualTrafficClass())
                        .isEqualTo(SOCKET_OPTION_RFC1349_TRAFFIC_CLASS.value());
                assertThat(socketOptions.getSendBufferSize()).isEqualTo(SOCKET_OPTION_SEND_BUFFER_SIZE);
                assertThat(socketOptions.getSoLinger()).isEqualTo(SOCKET_OPTION_LINGER);
                assertThat(socketOptions.getSoTimeout()).isEqualTo(SOCKET_OPTION_TIMEOUT);
                assertThat(socketOptions.isTcpNoDelay()).isEqualTo(SOCKET_OPTION_TCP_NO_DELAY);

                // Verify socket performance preferences
                final SocketPerformancePreferences performancePreferences = socketOptions.getPerformancePreferences();
                assertThat(performancePreferences.getBandwidth()).isEqualTo(SOCKET_PERFORMANCE_PREFERENCE_BANDWIDTH);
                assertThat(performancePreferences.getConnectionTime())
                        .isEqualTo(SOCKET_PERFORMANCE_PREFERENCE_CONNECTION_TIME);
                assertThat(performancePreferences.getLatency()).isEqualTo(SOCKET_PERFORMANCE_PREFERENCE_LATENCY);
            }
        }
    }

    private static Configuration createConfiguration(final Path tempDir, final int port) throws Exception {

        // Create the configuration builder
        final ConfigurationBuilder<BuiltConfiguration> configBuilder =
                ConfigurationBuilderFactory.newConfigurationBuilder()
                        .setStatusLevel(Level.ERROR)
                        .setConfigurationName(SocketAppenderReconnectTest.class.getSimpleName());

        // Stage the key store password file
        final Path keyStorePasswordFilePath = tempDir.resolve("keyStorePassword");
        Files.write(keyStorePasswordFilePath, new String(KEYSTORE_PWD).getBytes(StandardCharsets.UTF_8));

        // Stage the trust store password file
        final Path trustStorePasswordFilePath = tempDir.resolve("trustStorePassword");
        Files.write(trustStorePasswordFilePath, new String(TRUSTSTORE_PWD).getBytes(StandardCharsets.UTF_8));

        // Create the `SocketOptions` element
        final ComponentBuilder<?> socketPerformancePreferencesComponentBuilder = configBuilder
                .newComponent("SocketPerformancePreferences")
                .addAttribute("bandwidth", SOCKET_PERFORMANCE_PREFERENCE_BANDWIDTH)
                .addAttribute("connectionTime", SOCKET_PERFORMANCE_PREFERENCE_CONNECTION_TIME)
                .addAttribute("latency", SOCKET_PERFORMANCE_PREFERENCE_LATENCY);
        final ComponentBuilder<?> socketOptionsComponentBuilder = configBuilder
                .newComponent("SocketOptions")
                .addAttribute("keepAlive", SOCKET_OPTION_KEEP_ALIVE)
                .addAttribute("receiveBufferSize", SOCKET_OPTION_RECEIVE_BUFFER_SIZE)
                .addAttribute("reuseAddress", SOCKET_OPTION_REUSE_ADDRESS)
                .addAttribute("rfc1349TrafficClass", SOCKET_OPTION_RFC1349_TRAFFIC_CLASS)
                .addAttribute("sendBufferSize", SOCKET_OPTION_SEND_BUFFER_SIZE)
                .addAttribute("soLinger", SOCKET_OPTION_LINGER)
                .addAttribute("soTimeout", SOCKET_OPTION_TIMEOUT)
                .addAttribute("tcpNoDelay", SOCKET_OPTION_TCP_NO_DELAY)
                .addComponent(socketPerformancePreferencesComponentBuilder);

        // Create the `Ssl` element
        final ComponentBuilder<?> keyStoreComponentBuilder = configBuilder
                .newComponent("KeyStore")
                .addAttribute("type", KEYSTORE_TYPE)
                .addAttribute("location", KEYSTORE_LOCATION)
                .addAttribute("passwordFile", keyStorePasswordFilePath);
        final ComponentBuilder<?> trustStoreComponentBuilder = configBuilder
                .newComponent("TrustStore")
                .addAttribute("type", TRUSTSTORE_TYPE)
                .addAttribute("location", TRUSTSTORE_LOCATION)
                .addAttribute("passwordFile", trustStorePasswordFilePath);
        final ComponentBuilder<?> sslComponentBuilder = configBuilder
                .newComponent("Ssl")
                .addAttribute("protocol", "TLS")
                .addComponent(keyStoreComponentBuilder)
                .addComponent(trustStoreComponentBuilder);

        // Create the `Socket` element
        final AppenderComponentBuilder appenderComponentBuilder = configBuilder
                .newAppender(APPENDER_NAME, "Socket")
                .addAttribute("host", "localhost")
                .addAttribute("port", port)
                .addAttribute("ignoreExceptions", false)
                .addAttribute("reconnectionDelayMillis", 10)
                .addAttribute("immediateFlush", true)
                .add(configBuilder.newLayout("PatternLayout").addAttribute("pattern", "%m%n"))
                .addComponent(socketOptionsComponentBuilder)
                .addComponent(sslComponentBuilder);

        // Create the configuration
        return configBuilder
                .add(appenderComponentBuilder)
                .add(configBuilder.newRootLogger(Level.ALL).add(configBuilder.newAppenderRef(APPENDER_NAME)))
                .build(false);
    }
}
