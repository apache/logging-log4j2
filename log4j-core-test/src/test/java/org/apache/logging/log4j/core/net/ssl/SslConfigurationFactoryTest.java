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

import java.util.Properties;
import java.util.stream.Stream;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

// Suppresses `StatusLogger` output, unless there is a failure
@UsingStatusListener
class SslConfigurationFactoryTest {

    private static final String TRUSTSTORE_LOCATION_PROP_NAME = "log4j2.trustStoreLocation";

    private static final String TRUSTSTORE_PASSWORD_PROP_NAME = "log4j2.trustStorePassword";

    private static final String TRUSTSTORE_TYPE_PROP_NAME = "log4j2.trustStoreKeyStoreType";

    private static final String KEYSTORE_LOCATION_PROP_NAME = "log4j2.keyStoreLocation";

    private static final String KEYSTORE_PASSWORD_PROP_NAME = "log4j2.keyStorePassword";

    private static final String KEYSTORE_TYPE_PROP_NAME = "log4j2.keyStoreType";

    private static void addKeystoreConfiguration(final Properties props) {
        props.setProperty(KEYSTORE_LOCATION_PROP_NAME, SslKeyStoreConstants.KEYSTORE_LOCATION);
        props.setProperty(KEYSTORE_TYPE_PROP_NAME, SslKeyStoreConstants.KEYSTORE_TYPE);
    }

    private static void addTruststoreConfiguration(final Properties props) {
        props.setProperty(TRUSTSTORE_LOCATION_PROP_NAME, SslKeyStoreConstants.TRUSTSTORE_LOCATION);
        props.setProperty(TRUSTSTORE_TYPE_PROP_NAME, SslKeyStoreConstants.TRUSTSTORE_TYPE);
    }

    @Test
    void testStaticConfiguration() {

        // Case 1: Empty configuration
        final Properties props = new Properties();
        final PropertiesUtil util = new PropertiesUtil(props);
        SslConfiguration sslConfiguration = SslConfigurationFactory.createSslConfiguration(util);
        assertNull(sslConfiguration);

        // Case 2: Only key store
        props.clear();
        addKeystoreConfiguration(props);
        sslConfiguration = SslConfigurationFactory.createSslConfiguration(util);
        assertNotNull(sslConfiguration);
        assertNotNull(sslConfiguration.getKeyStoreConfig());
        assertNull(sslConfiguration.getTrustStoreConfig());

        // Case 3: Only trust store
        props.clear();
        addTruststoreConfiguration(props);
        sslConfiguration = SslConfigurationFactory.createSslConfiguration(util);
        assertNotNull(sslConfiguration);
        assertNull(sslConfiguration.getKeyStoreConfig());
        assertNotNull(sslConfiguration.getTrustStoreConfig());

        // Case 4: Both key and trust stores
        props.clear();
        addKeystoreConfiguration(props);
        addTruststoreConfiguration(props);
        sslConfiguration = SslConfigurationFactory.createSslConfiguration(util);
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
    void testPasswordLessStores(final String location, final String password) {

        // Create the configuration
        final Properties props = new Properties();
        props.setProperty(KEYSTORE_TYPE_PROP_NAME, SslKeyStoreConstants.WINDOWS_KEYSTORE_TYPE);
        props.setProperty(TRUSTSTORE_TYPE_PROP_NAME, SslKeyStoreConstants.WINDOWS_TRUSTSTORE_TYPE);
        if (location != null) {
            props.setProperty(KEYSTORE_LOCATION_PROP_NAME, location);
            props.setProperty(TRUSTSTORE_LOCATION_PROP_NAME, location);
        }
        if (password != null) {
            props.setProperty(KEYSTORE_PASSWORD_PROP_NAME, password);
            props.setProperty(TRUSTSTORE_PASSWORD_PROP_NAME, password);
        }
        final PropertiesUtil util = new PropertiesUtil(props);
        final SslConfiguration config = SslConfigurationFactory.createSslConfiguration(util);

        // Verify the configuration
        assertNotNull(config);
        final KeyStoreConfiguration keyStoreConfig = config.getKeyStoreConfig();
        assertNotNull(keyStoreConfig);
        KeyStoreConfigurationTest.checkKeystoreConfiguration(keyStoreConfig);
        final TrustStoreConfiguration trustStoreConfig = config.getTrustStoreConfig();
        assertNotNull(trustStoreConfig);
        KeyStoreConfigurationTest.checkKeystoreConfiguration(trustStoreConfig);
    }
}
