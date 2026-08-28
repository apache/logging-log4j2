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

import static org.apache.logging.log4j.core.net.SslSocketManager.createSniHostName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.net.ssl.SslKeyStoreConstants;
import org.apache.logging.log4j.core.net.ssl.TrustStoreConfiguration;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junitpioneer.jupiter.Issue;

class SslSocketManagerTest {

    private static final String HOST_NAME = "apache.org";

    private static final int HOST_PORT = 443;

    private static final Layout<?> LAYOUT = PatternLayout.createDefaultLayout();

    @Test
    @Issue("https://github.com/apache/logging-log4j2/issues/3947")
    @UsingStatusListener // Suppresses `StatusLogger` output, unless there is a failure
    void should_not_throw_exception_when_configuration_without_KeyStore() throws Exception {
        final TrustStoreConfiguration trustStoreConfig = new TrustStoreConfiguration(
                SslKeyStoreConstants.TRUSTSTORE_LOCATION,
                SslKeyStoreConstants::TRUSTSTORE_PWD,
                SslKeyStoreConstants.TRUSTSTORE_TYPE,
                null);
        final SslConfiguration sslConfig = SslConfiguration.createSSLConfiguration(null, null, trustStoreConfig);
        assertThatCode(() -> {
                    // noinspection EmptyTryBlock
                    try (final SslSocketManager ignored = createSocketManager(sslConfig)) {
                        // Do nothing
                    }
                })
                .doesNotThrowAnyException();
    }

    @Test
    void host_name_verification_should_take_effect() {
        final SslConfiguration sslConfig = SslConfiguration.createSSLConfiguration(
                null,
                null,
                null,
                // Explicitly enable hostname verification
                true);
        try (final SslSocketManager ssm = createSocketManager(sslConfig)) {
            final SSLSocket sslSocket = (SSLSocket) ssm.getSocket();
            final SSLParameters sslParams = sslSocket.getSSLParameters();
            assertThat(sslParams.getEndpointIdentificationAlgorithm()).isEqualTo("HTTPS");
            assertThat(sslParams.getServerNames()).containsOnly(new SNIHostName(HOST_NAME));
        }
    }

    private static SslSocketManager createSocketManager(final SslConfiguration sslConfig) {
        return SslSocketManager.getSocketManager(
                sslConfig, SslSocketManagerTest.HOST_NAME, HOST_PORT, 0, 0, true, LAYOUT, 8192, null);
    }

    static String[] hostNamesAllowedForSniServerName() {
        return new String[] {
            "apache.org",
            "a.b",
            "sub.domain.co.uk",
            "xn--bcher-kva.example", // valid IDN/punycode
            "my-host-123.example",
            "a1234567890.b1234567890.c1234567890", // numeric/alpha labels
            "EXAMPLE.COM", // case-insensitive
            "service--node.example", // double hyphens allowed
            Strings.repeat("l", 63) + ".com", // 63-char label, valid
            "a.b.c.d.e.f.g.h.i.j", // many labels, short each
        };
    }

    @ParameterizedTest
    @MethodSource("hostNamesAllowedForSniServerName")
    void createSniHostName_should_work_with_allowed_input(String hostName) {
        assertThat(createSniHostName(hostName)).isNotNull();
    }

    static String[] hostNamesNotAllowedForSniServerName() {
        return new String[] {
            // Invalid because IP literals not allowed
            "192.168.1.1", // IPv4 literal
            "2001:db8::1", // IPv6 literal
            "[2001:db8::1]", // IPv6 URL literal form
            // Invalid because illegal characters
            "exa_mple.com", // underscore not allowed
            "host name.com", // space not allowed
            "example!.com", // symbol not allowed
            // Invalid because label syntax violations
            "-example.com", // leading hyphen
            "example-.com", // trailing hyphen
            ".example.com", // leading dot / empty label
            // Invalid because label too long
            Strings.repeat("l", 64) + ".com",
        };
    }

    @ParameterizedTest
    @MethodSource("hostNamesNotAllowedForSniServerName")
    void createSniHostName_should_fail_with_not_allowed_input(String hostName) {
        assertThat(createSniHostName(hostName)).isNull();
    }
}
