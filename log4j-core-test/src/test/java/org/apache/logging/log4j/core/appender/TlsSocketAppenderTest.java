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

import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.stream.Stream;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.net.SslSocketManager;
import org.apache.logging.log4j.test.TestProperties;
import org.apache.logging.log4j.test.junit.UsingTestProperties;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@UsingTestProperties
class TlsSocketAppenderTest {

    // Test DNS names and IP addresses
    private static final String TARGET_HOSTNAME = "log4j.localhost";
    private static final String TARGET_IP = "::1";
    private static final String ATTACKER_HOSTNAME = "not-log4j.localhost";
    private static final String ATTACKER_IP = "127.0.0.1";

    // Test PKI material
    private static final KeyPair CA_KEY_PAIR = X509Certificates.generateKeyPair();
    private static final KeyPair SERVER_KEY_PAIR = X509Certificates.generateKeyPair();
    private static final KeyPair CLIENT_KEY_PAIR = X509Certificates.generateKeyPair();

    private static final X509Certificate CA_CERT;

    private static final X509Certificate TARGET_CERT1;
    private static final X509Certificate TARGET_CERT2;
    private static final X509Certificate TARGET_CERT3;

    private static final X509Certificate ATTACKER_CERT1;
    private static final X509Certificate ATTACKER_CERT2;
    private static final X509Certificate ATTACKER_CERT3;

    /** Client certificate used for mutual TLS (mTLS) scenarios. */
    private static final X509Certificate CLIENT_CERT;

    static {
        try {
            CA_CERT = X509Certificates.generateCACertificate(CA_KEY_PAIR);
            PrivateKey caPrivateKey = CA_KEY_PAIR.getPrivate();

            // Certificates with CN only
            TARGET_CERT1 = X509Certificates.generateServerCertificate(
                    SERVER_KEY_PAIR, caPrivateKey, "CN=" + TARGET_HOSTNAME, null, null);
            ATTACKER_CERT1 = X509Certificates.generateServerCertificate(
                    SERVER_KEY_PAIR, caPrivateKey, "CN=" + ATTACKER_HOSTNAME, null, null);

            // Certificates with SAN (DNS)
            TARGET_CERT2 = X509Certificates.generateServerCertificate(
                    SERVER_KEY_PAIR, caPrivateKey, "CN=Test Server", TARGET_HOSTNAME, null);
            ATTACKER_CERT2 = X509Certificates.generateServerCertificate(
                    SERVER_KEY_PAIR, caPrivateKey, "CN=Test Attacker Server", ATTACKER_HOSTNAME, null);

            // Certificates with SAN (IP)
            TARGET_CERT3 = X509Certificates.generateServerCertificate(
                    SERVER_KEY_PAIR, caPrivateKey, "CN=Test Server", null, TARGET_IP);
            ATTACKER_CERT3 = X509Certificates.generateServerCertificate(
                    SERVER_KEY_PAIR, caPrivateKey, "CN=Test Attacker Server", null, ATTACKER_IP);

            CLIENT_CERT = X509Certificates.generateClientCertificate(CLIENT_KEY_PAIR, caPrivateKey, "CN=Test Client");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Store parameters

    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final char[] KEYSTORE_PWD = "aKeyStoreSecret".toCharArray();
    private static final String TRUSTSTORE_TYPE = "PKCS12";
    private static final char[] TRUSTSTORE_PWD = "aTrustStoreSecret".toCharArray();

    @TempDir
    private static Path certPath;

    static Stream<Arguments> connectionAlwaysSucceedsWithoutHostnameVerification() {
        return Stream.of(
                Arguments.of(TARGET_HOSTNAME, ATTACKER_CERT1),
                Arguments.of(TARGET_HOSTNAME, ATTACKER_CERT2),
                Arguments.of(TARGET_IP, ATTACKER_CERT3));
    }

    static Stream<Arguments> connectionSucceedsOnHostNameMatch() {
        return Stream.of(
                // No client certificate
                Arguments.of(TARGET_HOSTNAME, TARGET_CERT1, null),
                Arguments.of(TARGET_HOSTNAME, TARGET_CERT2, null),
                Arguments.of(TARGET_IP, TARGET_CERT3, null),

                // These tests ensure that connections to the attacher fail because of hostname mismatch,
                // not because of other TLS issues.
                Arguments.of(ATTACKER_HOSTNAME, ATTACKER_CERT1, null),
                Arguments.of(ATTACKER_HOSTNAME, ATTACKER_CERT2, null),
                Arguments.of(ATTACKER_IP, ATTACKER_CERT3, null),

                // Mutual TLS
                Arguments.of(TARGET_HOSTNAME, TARGET_CERT1, CLIENT_CERT),
                Arguments.of(TARGET_HOSTNAME, TARGET_CERT2, CLIENT_CERT),
                Arguments.of(TARGET_IP, TARGET_CERT3, CLIENT_CERT));
    }

    static Stream<Arguments> connectionFailsOnHostNameMismatch() {
        return Stream.of(
                Arguments.of(TARGET_HOSTNAME, ATTACKER_CERT1),
                Arguments.of(TARGET_HOSTNAME, ATTACKER_CERT2),
                Arguments.of(TARGET_IP, ATTACKER_CERT3));
    }

    @ParameterizedTest
    @MethodSource
    void connectionAlwaysSucceedsWithoutHostnameVerification(
            String hostName, X509Certificate serverCertificate, TestProperties props) throws Exception {

        TestTlsMaterial tls = createTlsMaterial(hostName, serverCertificate, null);
        applyClientTlsProperties(props, tls);
        props.setProperty("ssl.verifyHostname", "false");

        try (LineReadingTcpServer server = createTlsServer(hostName, tls.serverSslContext, false)) {
            props.setProperty("server.host", hostName);
            props.setProperty("server.port", server.getServerSocket().getLocalPort());

            try (LoggerContext ctx = createLoggerContext()) {
                Logger logger = ctx.getLogger(TlsSocketAppenderTest.class);

                String expected = "Test message for host " + hostName;
                logger.info(expected);

                assertThat(server.pollLines(1)).containsExactly(expected);
            }
        }
    }

    @ParameterizedTest
    @MethodSource
    void connectionSucceedsOnHostNameMatch(
            String hostName,
            X509Certificate serverCertificate,
            @Nullable X509Certificate clientCertificate,
            TestProperties props)
            throws Exception {

        TestTlsMaterial tls = createTlsMaterial(hostName, serverCertificate, clientCertificate);
        applyClientTlsProperties(props, tls);

        try (LineReadingTcpServer server = createTlsServer(hostName, tls.serverSslContext, clientCertificate != null)) {
            props.setProperty("server.host", hostName);
            props.setProperty("server.port", server.getServerSocket().getLocalPort());

            try (LoggerContext ctx = createLoggerContext()) {
                Logger logger = ctx.getLogger(TlsSocketAppenderTest.class);

                String expected = "Test message for host " + hostName;
                logger.info(expected);

                assertThat(server.pollLines(1)).containsExactly(expected);
            }
        }
    }

    @ParameterizedTest
    @MethodSource
    void connectionFailsOnHostNameMismatch(String hostName, X509Certificate serverCertificate, TestProperties props)
            throws Exception {

        // No mTLS needed; we only care about hostname verification failure.
        TestTlsMaterial tls = createTlsMaterial(hostName, serverCertificate, null);
        applyClientTlsProperties(props, tls);

        try (LineReadingTcpServer server = createTlsServer(hostName, tls.serverSslContext, false)) {
            props.setProperty("server.host", hostName);
            props.setProperty("server.port", server.getServerSocket().getLocalPort());

            try (LoggerContext ctx = createLoggerContext()) {
                assertSocketAppenderNotConnected(ctx, hostName);
            }
        }
    }

    private static TestTlsMaterial createTlsMaterial(
            String hostName, X509Certificate serverCertificate, @Nullable X509Certificate clientCertificate)
            throws Exception {

        // Client keystore: only populated when we test mutual TLS.
        String clientKeystore = generateKeystore(
                hostName + "-client", clientCertificate, clientCertificate != null ? CLIENT_KEY_PAIR : null);

        String serverKeystore = generateKeystore(hostName + "-server", serverCertificate, SERVER_KEY_PAIR);

        String truststore = generateTruststore(hostName);

        SSLContext serverSslContext = SslContexts.createSslContext(
                KEYSTORE_TYPE, serverKeystore, KEYSTORE_PWD, TRUSTSTORE_TYPE, truststore, TRUSTSTORE_PWD);

        return new TestTlsMaterial(clientKeystore, truststore, serverSslContext);
    }

    private static void applyClientTlsProperties(TestProperties props, TestTlsMaterial tls) {
        props.setProperty("keystore.location", tls.clientKeystoreLocation);
        props.setProperty("keystore.password", new String(KEYSTORE_PWD));
        props.setProperty("keystore.type", KEYSTORE_TYPE);

        props.setProperty("truststore.location", tls.truststoreLocation);
        props.setProperty("truststore.password", new String(TRUSTSTORE_PWD));
        props.setProperty("truststore.type", TRUSTSTORE_TYPE);
    }

    private static LineReadingTcpServer createTlsServer(String hostName, SSLContext sslContext, boolean needClientAuth)
            throws Exception {

        LineReadingTcpServer server = new LineReadingTcpServer(sslContext.getServerSocketFactory());

        // Bind to all interfaces to allow testing with different host names.
        server.setBindAddress(null);

        server.start("TlsSocketAppenderTest-" + hostName, 0);

        SSLServerSocket socket = (SSLServerSocket) server.getServerSocket();
        socket.setNeedClientAuth(needClientAuth);

        return server;
    }

    private static LoggerContext createLoggerContext() throws Exception {
        URL configLocation = TlsSocketAppenderTest.class.getResource("/TlsSocketAppenderTest/log4j2.xml");
        assertThat(configLocation).isNotNull();

        LoggerContext ctx = new LoggerContext("TlsSocketAppenderTest", null, configLocation.toURI());
        ctx.start();
        return ctx;
    }

    private static void assertSocketAppenderNotConnected(LoggerContext ctx, String hostName) {
        SocketAppender appender = ctx.getConfiguration().getAppender("SOCKET-" + hostName);
        assertThat(appender).isNotNull();
        assertThat(appender.getManager()).isInstanceOf(SslSocketManager.class);

        SslSocketManager manager = (SslSocketManager) appender.getManager();
        Socket socket = manager.getSocket();

        if (socket != null) {
            assertThat(socket.isConnected()).isFalse();
        }
    }

    private static String generateTruststore(String alias) throws Exception {
        KeyStore trustStore = KeyStore.getInstance(TRUSTSTORE_TYPE);
        trustStore.load(null, null);
        trustStore.setCertificateEntry(alias, CA_CERT);

        Path file = certPath.resolve(sanitizePath(alias) + "-truststore.p12");
        try (OutputStream out = Files.newOutputStream(file)) {
            trustStore.store(out, TRUSTSTORE_PWD);
        }
        return file.toAbsolutePath().toString();
    }

    private static String generateKeystore(
            String alias, @Nullable X509Certificate certificate, @Nullable KeyPair keyPair) throws Exception {

        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
        keyStore.load(null, null);

        if (certificate != null && keyPair != null) {
            keyStore.setKeyEntry(
                    alias, keyPair.getPrivate(), KEYSTORE_PWD, new X509Certificate[] {certificate, CA_CERT});
        }

        Path file = certPath.resolve(sanitizePath(alias) + "-keystore.p12");
        try (OutputStream out = Files.newOutputStream(file)) {
            keyStore.store(out, KEYSTORE_PWD);
        }
        return file.toAbsolutePath().toString();
    }

    private static String sanitizePath(String alias) {
        return alias.replace(':', '_');
    }

    private static class TestTlsMaterial {

        private final @Nullable String clientKeystoreLocation;
        private final String truststoreLocation;
        private final SSLContext serverSslContext;

        private TestTlsMaterial(String clientKeystoreLocation, String truststoreLocation, SSLContext serverSslContext) {
            this.clientKeystoreLocation = clientKeystoreLocation;
            this.truststoreLocation = truststoreLocation;
            this.serverSslContext = serverSslContext;
        }
    }
}
