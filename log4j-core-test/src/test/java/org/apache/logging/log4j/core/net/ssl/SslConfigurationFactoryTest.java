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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.stream.Stream;
import org.apache.logging.log4j.core.impl.CoreProperties.KeyManagerFactoryProperties;
import org.apache.logging.log4j.core.impl.CoreProperties.KeyStoreProperties;
import org.apache.logging.log4j.core.impl.CoreProperties.TransportSecurityProperties;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@UsingStatusListener // Suppresses `StatusLogger` output, unless there is a failure
class SslConfigurationFactoryTest {

    @Test
    void testStaticConfiguration() {

        // Case 1: Empty configuration
        final TransportSecurityProperties transportSecurity = TransportSecurityProperties.defaultValue();
        SslConfiguration sslConfiguration = SslConfigurationFactory.getSslConfiguration(transportSecurity);
        assertNull(sslConfiguration);

        // Case 2: Only key store
        final KeyStoreProperties keyStore = new KeyStoreProperties(
                new KeyManagerFactoryProperties(null),
                SslKeyStoreConstants.KEYSTORE_LOCATION,
                null,
                null,
                null,
                SslKeyStoreConstants.KEYSTORE_TYPE);
        sslConfiguration = SslConfigurationFactory.getSslConfiguration(transportSecurity.withKeyStore(keyStore));
        assertNotNull(sslConfiguration);
        assertNotNull(sslConfiguration.getKeyStoreConfig());
        assertNull(sslConfiguration.getTrustStoreConfig());

        // Case 3: Only trust store
        final KeyStoreProperties trustStore = new KeyStoreProperties(
                new KeyManagerFactoryProperties(null),
                SslKeyStoreConstants.TRUSTSTORE_LOCATION,
                null,
                null,
                null,
                SslKeyStoreConstants.TRUSTSTORE_TYPE);
        sslConfiguration = SslConfigurationFactory.getSslConfiguration(transportSecurity.withTrustStore(trustStore));
        assertNotNull(sslConfiguration);
        assertNull(sslConfiguration.getKeyStoreConfig());
        assertNotNull(sslConfiguration.getTrustStoreConfig());

        // Case 4: Both key and trust stores
        sslConfiguration = SslConfigurationFactory.getSslConfiguration(
                transportSecurity.withKeyStore(keyStore).withTrustStore(trustStore));
        assertNotNull(sslConfiguration);
        assertNotNull(sslConfiguration.getKeyStoreConfig());
        assertNotNull(sslConfiguration.getTrustStoreConfig());
    }

    static Stream<Arguments> windowsKeystoreConfigs() {
        final String[] emptyOrNull = {"", null};
        final Stream.Builder<Arguments> builder = Stream.builder();
        for (final String location : emptyOrNull) {
            for (final String password : emptyOrNull) {
                builder.add(Arguments.of(location, password));
            }
        }
        return builder.build();
    }

    @EnabledOnOs(OS.WINDOWS)
    @ParameterizedTest
    @MethodSource("windowsKeystoreConfigs")
    public void testPasswordLessStores(String location, String password) {

        // Create the configuration
        final char[] passwordChars = password != null ? password.toCharArray() : null;
        final KeyStoreProperties keyStore = new KeyStoreProperties(
                new KeyManagerFactoryProperties(null),
                location,
                passwordChars,
                null,
                null,
                SslKeyStoreConstants.WINDOWS_KEYSTORE_TYPE);
        final KeyStoreProperties trustStore = new KeyStoreProperties(
                new KeyManagerFactoryProperties(null),
                location,
                passwordChars,
                null,
                null,
                SslKeyStoreConstants.WINDOWS_TRUSTSTORE_TYPE);
        final TransportSecurityProperties transportSecurity = TransportSecurityProperties.defaultValue()
                .withKeyStore(keyStore)
                .withTrustStore(trustStore);

        // Verify the configuration
        final SslConfiguration config = SslConfigurationFactory.getSslConfiguration(transportSecurity);
        assertNotNull(config);
        final KeyStoreConfiguration keyStoreConfig = config.getKeyStoreConfig();
        assertNotNull(keyStoreConfig);
        KeyStoreConfigurationTest.checkKeystoreConfiguration(keyStoreConfig);
        final TrustStoreConfiguration trustStoreConfig = config.getTrustStoreConfig();
        assertNotNull(trustStoreConfig);
        KeyStoreConfigurationTest.checkKeystoreConfiguration(trustStoreConfig);
    }
}
