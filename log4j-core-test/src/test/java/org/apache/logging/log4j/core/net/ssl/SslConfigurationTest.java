/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.net.ssl;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.logging.log4j.core.test.net.ssl.TestConstants;
import org.apache.logging.log4j.test.junit.StatusLoggerLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@StatusLoggerLevel("OFF")
public class SslConfigurationTest {

    private static final String TLS_TEST_HOST = "login.yahoo.com";
    private static final int TLS_TEST_PORT = 443;

    public static SslConfiguration createTestSslConfigurationResources() throws StoreConfigurationException {
        final KeyStoreConfiguration ksc = KeyStoreConfiguration.builder()
                .setLocation(TestConstants.KEYSTORE_FILE_RESOURCE)
                .setPassword(TestConstants.KEYSTORE_PWD())
                .setKeyStoreType(TestConstants.KEYSTORE_TYPE)
                .build();
        final TrustStoreConfiguration tsc = TrustStoreConfiguration.builder()
                .setLocation(TestConstants.TRUSTSTORE_FILE_RESOURCE)
                .setPassword(TestConstants.TRUSTSTORE_PWD())
                .build();
        return SslConfiguration.createSSLConfiguration(null, ksc, tsc);
    }

    public static SslConfiguration createTestSslConfigurationFiles() throws StoreConfigurationException {
        final KeyStoreConfiguration ksc = KeyStoreConfiguration.builder()
                .setLocation(TestConstants.KEYSTORE_FILE)
                .setPassword(TestConstants.KEYSTORE_PWD())
                .setKeyStoreType(TestConstants.KEYSTORE_TYPE)
                .build();
        final TrustStoreConfiguration tsc = TrustStoreConfiguration.builder()
                .setLocation(TestConstants.TRUSTSTORE_FILE)
                .setPassword(TestConstants.TRUSTSTORE_PWD())
                .build();
        return SslConfiguration.createSSLConfiguration(null, ksc, tsc);
    }

    @Test
    public void testGettersFromScratchFiles() throws StoreConfigurationException {
        assertNotNull(createTestSslConfigurationFiles().getProtocol());
        assertNotNull(createTestSslConfigurationFiles().getKeyStoreConfig());
        assertNotNull(createTestSslConfigurationFiles().getSslContext());
        assertNotNull(createTestSslConfigurationFiles().getSslSocketFactory());
        assertNotNull(createTestSslConfigurationFiles().getTrustStoreConfig());
    }

    @Test
    public void testGettersFromScratchResources() throws StoreConfigurationException {
        assertNotNull(createTestSslConfigurationResources().getProtocol());
        assertNotNull(createTestSslConfigurationResources().getKeyStoreConfig());
        assertNotNull(createTestSslConfigurationResources().getSslContext());
        assertNotNull(createTestSslConfigurationResources().getSslSocketFactory());
        assertNotNull(createTestSslConfigurationResources().getTrustStoreConfig());
    }

    @Test
    public void equals() {
        assertEquals(SslConfiguration.createSSLConfiguration(null, null, null),
                SslConfiguration.createSSLConfiguration(null, null, null));
    }

    @Test
        public void emptyConfigurationDoesntCauseNullSSLSocketFactory() {
        final SslConfiguration sc = SslConfiguration.createSSLConfiguration(null, null, null);
        final SSLSocketFactory factory = sc.getSslSocketFactory();
        assertNotNull(factory);
    }

    @Test
    public void emptyConfigurationHasDefaultTrustStore() throws IOException {
        final SslConfiguration sc = SslConfiguration.createSSLConfiguration(null, null, null);
        final SSLSocketFactory factory = sc.getSslSocketFactory();
        try {
            try (final SSLSocket clientSocket = (SSLSocket) factory.createSocket(TLS_TEST_HOST, TLS_TEST_PORT)) {
                assertNotNull(clientSocket);
            }
        } catch (final UnknownHostException | ConnectException connectionTimeout) {
            // this exception is thrown on Windows when host is behind a proxy that does not allow connection to external network
        }
    }

    @Test
    public void connectionFailsWithoutValidServerCertificate() throws IOException, StoreConfigurationException {
        final TrustStoreConfiguration tsc = TrustStoreConfiguration.builder().setLocation(TestConstants.TRUSTSTORE_FILE).build();
        final SslConfiguration sc = SslConfiguration.createSSLConfiguration(null, null, tsc);
        final SSLSocketFactory factory = sc.getSslSocketFactory();
        try {
            try (final SSLSocket clientSocket = (SSLSocket) factory.createSocket(TLS_TEST_HOST, TLS_TEST_PORT)) {
                try (final OutputStream os = clientSocket.getOutputStream()) {
                    assertThrows(IOException.class, () -> os.write("GET config/login_verify2?".getBytes()));
                }
            }
        } catch (final UnknownHostException | ConnectException connectionTimeout) {
            // this exception is thrown on Windows when host is behind a proxy that does not allow connection to external network
        }
    }

    @Test
    public void loadKeyStoreWithoutPassword() throws StoreConfigurationException {
        final KeyStoreConfiguration ksc = KeyStoreConfiguration.builder().setLocation(TestConstants.KEYSTORE_FILE).build();
        final SslConfiguration sslConf = SslConfiguration.createSSLConfiguration(null, ksc, null);
        final SSLSocketFactory factory = sslConf.getSslSocketFactory();
        assertNotNull(factory);
    }
}
