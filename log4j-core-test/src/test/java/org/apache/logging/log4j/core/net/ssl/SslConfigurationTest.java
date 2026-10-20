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
package org.apache.logging.log4j.core.net.ssl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.util.PluginBuilder;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;

class SslConfigurationTest {

    private static final String TLS_TEST_HOST = "apache.org";
    private static final int TLS_TEST_PORT = 443;

    private static SslConfiguration createTestSslConfigurationResources() throws StoreConfigurationException {
        final KeyStoreConfiguration ksc = new KeyStoreConfiguration(
                SslKeyStoreConstants.KEYSTORE_LOCATION,
                new MemoryPasswordProvider(SslKeyStoreConstants.KEYSTORE_PWD()),
                SslKeyStoreConstants.KEYSTORE_TYPE,
                null);
        final TrustStoreConfiguration tsc = new TrustStoreConfiguration(
                SslKeyStoreConstants.TRUSTSTORE_LOCATION,
                new MemoryPasswordProvider(SslKeyStoreConstants.TRUSTSTORE_PWD()),
                null,
                null);
        return SslConfiguration.createSSLConfiguration(null, ksc, tsc);
    }

    private static SslConfiguration createTestSslConfigurationFiles() throws StoreConfigurationException {
        final KeyStoreConfiguration ksc = new KeyStoreConfiguration(
                SslKeyStoreConstants.KEYSTORE_LOCATION,
                new MemoryPasswordProvider(SslKeyStoreConstants.KEYSTORE_PWD()),
                SslKeyStoreConstants.KEYSTORE_TYPE,
                null);
        final TrustStoreConfiguration tsc = new TrustStoreConfiguration(
                SslKeyStoreConstants.TRUSTSTORE_LOCATION,
                new MemoryPasswordProvider(SslKeyStoreConstants.TRUSTSTORE_PWD()),
                SslKeyStoreConstants.TRUSTSTORE_TYPE,
                null);
        return SslConfiguration.createSSLConfiguration(null, ksc, tsc);
    }

    @Test
    void testGettersFromScratchFiles() throws StoreConfigurationException {
        assertNotNull(createTestSslConfigurationFiles().getProtocol());
        assertNotNull(createTestSslConfigurationFiles().getKeyStoreConfig());
        assertNotNull(createTestSslConfigurationFiles().getSslContext());
        assertNotNull(createTestSslConfigurationFiles().getSslContext().getSocketFactory());
        assertNotNull(createTestSslConfigurationFiles().getTrustStoreConfig());
    }

    @Test
    void testGettersFromScratchResources() throws StoreConfigurationException {
        assertNotNull(createTestSslConfigurationResources().getProtocol());
        assertNotNull(createTestSslConfigurationResources().getKeyStoreConfig());
        assertNotNull(createTestSslConfigurationResources().getSslContext());
        assertNotNull(createTestSslConfigurationResources().getSslContext().getSocketFactory());
        assertNotNull(createTestSslConfigurationResources().getTrustStoreConfig());
    }

    @Test
    void testEquals() {
        assertEquals(
                SslConfiguration.createSSLConfiguration(null, null, null),
                SslConfiguration.createSSLConfiguration(null, null, null));
    }

    @Test
    void emptyConfigurationDoesNotCauseNullSSLSocketFactory() {
        final SslConfiguration sc = SslConfiguration.createSSLConfiguration(null, null, null);
        final SSLSocketFactory factory = sc.getSslContext().getSocketFactory();
        assertNotNull(factory);
    }

    @Test
    void emptyConfigurationHasDefaultTrustStore() throws IOException {
        final SslConfiguration sc = SslConfiguration.createSSLConfiguration(null, null, null);
        final SSLSocketFactory factory = sc.getSslContext().getSocketFactory();
        try {
            try (final SSLSocket clientSocket = (SSLSocket) factory.createSocket(TLS_TEST_HOST, TLS_TEST_PORT)) {
                assertNotNull(clientSocket);
            }
        } catch (final UnknownHostException offline) {
            // this exception is thrown on Windows when offline
        }
    }

    @Test
    void connectionFailsWithoutValidServerCertificate() throws IOException, StoreConfigurationException {
        final TrustStoreConfiguration tsc = new TrustStoreConfiguration(
                SslKeyStoreConstants.TRUSTSTORE_LOCATION,
                new MemoryPasswordProvider(SslKeyStoreConstants.NULL_PWD),
                null,
                null);
        final SslConfiguration sc = SslConfiguration.createSSLConfiguration(null, null, tsc);
        final SSLSocketFactory factory = sc.getSslContext().getSocketFactory();
        try {
            try (final SSLSocket clientSocket = (SSLSocket) factory.createSocket(TLS_TEST_HOST, TLS_TEST_PORT)) {
                try (final OutputStream os = clientSocket.getOutputStream()) {
                    assertThrows(IOException.class, () -> os.write("GET config/login_verify2?".getBytes()));
                }
            }
        } catch (final UnknownHostException offline) {
            // this exception is thrown on Windows when offline
        }
    }

    @Test
    @UsingStatusListener // Suppresses `StatusLogger` output, unless there is a failure
    void loadKeyStoreWithoutPassword() throws StoreConfigurationException {
        final KeyStoreConfiguration ksc = new KeyStoreConfiguration(
                SslKeyStoreConstants.KEYSTORE_P12_NOPASS_LOCATION,
                new MemoryPasswordProvider(SslKeyStoreConstants.NULL_PWD),
                SslKeyStoreConstants.KEYSTORE_P12_NOPASS_TYPE,
                null);
        final SslConfiguration sslConf = SslConfiguration.createSSLConfiguration(null, ksc, null);
        final SSLSocketFactory factory = sslConf.getSslContext().getSocketFactory();
        assertNotNull(factory);
    }

    @Test
    void verifyHostNameFromXml() {
        PluginManager pluginManager = new PluginManager(Node.CATEGORY);
        pluginManager.collectPlugins();
        PluginType<?> pluginType = pluginManager.getPluginType("Ssl");
        assertThat(pluginType).isNotNull();
        Node ssl = new Node(null, pluginType.getElementName(), pluginType);
        ssl.getAttributes().put("verifyHostName", "true");
        PluginBuilder builder = new PluginBuilder(pluginType);
        SslConfiguration sslConfiguration = (SslConfiguration) builder.withConfigurationNode(ssl)
                .withConfiguration(new DefaultConfiguration())
                .build();
        assertThat(sslConfiguration.isVerifyHostName()).isTrue();
    }

    static void handshake(final SslConfiguration clientConfiguration) throws Exception {
        final SslConfiguration serverConfiguration = createTestSslConfigurationFiles();
        try (final SSLServerSocket server = (SSLServerSocket) serverConfiguration
                .getSslContext()
                .getServerSocketFactory()
                .createServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            server.setSoTimeout(10_000);
            final CompletableFuture<Void> serverHandshake = CompletableFuture.runAsync(() -> {
                try (final SSLSocket socket = (SSLSocket) server.accept()) {
                    socket.startHandshake();
                } catch (final IOException ignored) {
                }
            });
            try (final SSLSocket client = (SSLSocket) clientConfiguration
                    .getSslContext()
                    .getSocketFactory()
                    .createSocket(server.getInetAddress(), server.getLocalPort())) {
                client.startHandshake();
            } finally {
                serverHandshake.join();
            }
        }
    }

    private static SslConfiguration buildSslElement(
            final Map<String, String> keyStoreAttributes, final Map<String, String> trustStoreAttributes) {
        final PluginManager pluginManager = new PluginManager(Node.CATEGORY);
        pluginManager.collectPlugins();
        final Configuration configuration = new DefaultConfiguration();
        final PluginType<?> sslType = pluginManager.getPluginType("Ssl");
        final Node ssl = new Node(null, sslType.getElementName(), sslType);
        addStoreElement(pluginManager, configuration, ssl, "KeyStore", keyStoreAttributes);
        addStoreElement(pluginManager, configuration, ssl, "TrustStore", trustStoreAttributes);
        return (SslConfiguration) new PluginBuilder(sslType)
                .withConfigurationNode(ssl)
                .withConfiguration(configuration)
                .build();
    }

    private static void addStoreElement(
            final PluginManager pluginManager,
            final Configuration configuration,
            final Node ssl,
            final String elementName,
            final Map<String, String> attributes) {
        final PluginType<?> type = pluginManager.getPluginType(elementName);
        final Node store = new Node(ssl, type.getElementName(), type);
        store.getAttributes().putAll(attributes);
        store.setObject(new PluginBuilder(type)
                .withConfigurationNode(store)
                .withConfiguration(configuration)
                .build());
        ssl.getChildren().add(store);
    }

    private static Map<String, String> attributes(final String... namesAndValues) {
        final Map<String, String> attributes = new HashMap<>();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            attributes.put(namesAndValues[i], namesAndValues[i + 1]);
        }
        return attributes;
    }

    private static Map<String, String> validKeyStore() {
        return attributes(
                "location",
                SslKeyStoreConstants.KEYSTORE_LOCATION,
                "password",
                new String(SslKeyStoreConstants.KEYSTORE_PWD()),
                "type",
                SslKeyStoreConstants.KEYSTORE_TYPE);
    }

    private static Map<String, String> validTrustStore() {
        return attributes(
                "location",
                SslKeyStoreConstants.TRUSTSTORE_LOCATION,
                "password",
                new String(SslKeyStoreConstants.TRUSTSTORE_PWD()),
                "type",
                SslKeyStoreConstants.TRUSTSTORE_TYPE);
    }

    @Test
    void validSslElementCompletesHandshake() {
        final SslConfiguration sslConfiguration = buildSslElement(validKeyStore(), validTrustStore());
        assertThatCode(() -> handshake(sslConfiguration)).doesNotThrowAnyException();
    }

    @Test
    @UsingStatusListener
    void sslElementWithMissingTrustStoreRejectsHandshake() {
        final SslConfiguration sslConfiguration = buildSslElement(
                validKeyStore(),
                attributes(
                        "location",
                        SslKeyStoreConstants.TRUSTSTORE_LOCATION + ".missing",
                        "type",
                        SslKeyStoreConstants.TRUSTSTORE_TYPE));
        assertThat(sslConfiguration).isNotNull();
        assertThat(sslConfiguration.getTrustStoreConfig()).isNotNull();
        assertThatThrownBy(() -> handshake(sslConfiguration)).isInstanceOf(SSLHandshakeException.class);
    }

    @Test
    @UsingStatusListener
    void sslElementWithWrongKeyStorePasswordRejectsHandshake() {
        final SslConfiguration sslConfiguration = buildSslElement(
                attributes(
                        "location",
                        SslKeyStoreConstants.KEYSTORE_LOCATION,
                        "password",
                        "wrong",
                        "type",
                        SslKeyStoreConstants.KEYSTORE_TYPE),
                validTrustStore());
        assertThat(sslConfiguration).isNotNull();
        assertThat(sslConfiguration.getKeyStoreConfig()).isNotNull();
        assertThatThrownBy(() -> handshake(sslConfiguration)).isInstanceOf(SSLHandshakeException.class);
    }

    @Test
    @UsingStatusListener
    void sslContextInitializationFailureDoesNotFallBackToDefault() throws Exception {
        final KeyStoreConfiguration ksc = new KeyStoreConfiguration(
                SslKeyStoreConstants.KEYSTORE_LOCATION,
                new MemoryPasswordProvider(SslKeyStoreConstants.KEYSTORE_PWD()),
                SslKeyStoreConstants.KEYSTORE_TYPE,
                "NoSuchKeyManagerFactoryAlgorithm");
        final TrustStoreConfiguration tsc = new TrustStoreConfiguration(
                SslKeyStoreConstants.TRUSTSTORE_LOCATION,
                new MemoryPasswordProvider(SslKeyStoreConstants.TRUSTSTORE_PWD()),
                SslKeyStoreConstants.TRUSTSTORE_TYPE,
                null);
        final SslConfiguration sslConfiguration = SslConfiguration.createSSLConfiguration(null, ksc, tsc);
        assertThat(sslConfiguration.getSslContext()).isNotSameAs(SSLContext.getDefault());
        assertThatThrownBy(() -> handshake(sslConfiguration)).isInstanceOf(SSLHandshakeException.class);
    }

    @Test
    void rejectingTrustManagerRejectsEveryPeer() {
        final X509TrustManager trustManager = SslConfiguration.RejectingTrustManager.INSTANCE;
        assertThatThrownBy(() -> trustManager.checkServerTrusted(new X509Certificate[0], "RSA"))
                .isInstanceOf(CertificateException.class);
        assertThatThrownBy(() -> trustManager.checkClientTrusted(new X509Certificate[0], "RSA"))
                .isInstanceOf(CertificateException.class);
        assertThat(trustManager.getAcceptedIssuers()).isEmpty();
    }
}
