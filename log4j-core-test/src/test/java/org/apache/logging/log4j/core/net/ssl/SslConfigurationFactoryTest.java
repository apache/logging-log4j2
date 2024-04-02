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

import org.apache.logging.log4j.core.impl.CoreProperties.KeyManagerFactoryProperties;
import org.apache.logging.log4j.core.impl.CoreProperties.KeyStoreProperties;
import org.apache.logging.log4j.core.impl.CoreProperties.TransportSecurityProperties;
import org.apache.logging.log4j.core.test.net.ssl.TestConstants;
import org.junit.jupiter.api.Test;

public class SslConfigurationFactoryTest {

    private static KeyStoreProperties createKeyStoreProps() {
        return new KeyStoreProperties(
                new KeyManagerFactoryProperties(null),
                TestConstants.KEYSTORE_FILE_RESOURCE,
                null,
                null,
                null,
                TestConstants.KEYSTORE_TYPE);
    }

    private static KeyStoreProperties createTrustStoreProps() {
        return new KeyStoreProperties(
                new KeyManagerFactoryProperties(null),
                TestConstants.TRUSTSTORE_FILE_RESOURCE,
                null,
                null,
                null,
                TestConstants.KEYSTORE_TYPE);
    }

    @Test
    public void testStaticConfiguration() {
        final KeyStoreProperties keyStore = createKeyStoreProps();
        final KeyStoreProperties trustStore = createTrustStoreProps();
        final TransportSecurityProperties transportSecurity = TransportSecurityProperties.defaultValue();
        // No keystore and truststore -> no SslConfiguration
        SslConfiguration sslConfiguration = SslConfigurationFactory.getSslConfiguration(transportSecurity);
        assertNull(sslConfiguration);
        // Only keystore
        sslConfiguration = SslConfigurationFactory.getSslConfiguration(transportSecurity.withKeyStore(keyStore));
        assertNotNull(sslConfiguration);
        assertNotNull(sslConfiguration.getKeyStoreConfig());
        assertNull(sslConfiguration.getTrustStoreConfig());
        // Only truststore
        sslConfiguration = SslConfigurationFactory.getSslConfiguration(transportSecurity.withTrustStore(trustStore));
        assertNotNull(sslConfiguration);
        assertNull(sslConfiguration.getKeyStoreConfig());
        assertNotNull(sslConfiguration.getTrustStoreConfig());
        // Both
        sslConfiguration = SslConfigurationFactory.getSslConfiguration(
                transportSecurity.withKeyStore(keyStore).withTrustStore(trustStore));
        assertNotNull(sslConfiguration);
        assertNotNull(sslConfiguration.getKeyStoreConfig());
        assertNotNull(sslConfiguration.getTrustStoreConfig());
    }
}
