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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.net.UnknownHostException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
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
}
