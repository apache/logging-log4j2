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
import org.apache.logging.log4j.util.PropertiesUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SslConfigurationFactoryTest {

    private static final String trustStorelocation = "log4j2.trustStoreLocation";
    private static final String trustStorePassword = "log4j2.trustStorePassword";
    private static final String trustStoreKeyStoreType = "log4j2.trustStoreKeyStoreType";
    private static final String keyStoreLocation = "log4j2.keyStoreLocation";
    private static final String keyStorePassword = "log4j2.keyStorePassword";
    private static final String keyStoreType = "log4j2.keyStoreType";

    private static void addKeystoreConfiguration(final Properties props) {
        props.setProperty(keyStoreLocation, TestConstants.KEYSTORE_FILE_RESOURCE);
        props.setProperty(keyStoreType, TestConstants.KEYSTORE_TYPE);
    }

    private static void addTruststoreConfiguration(final Properties props) {
        props.setProperty(trustStorelocation, TestConstants.TRUSTSTORE_FILE_RESOURCE);
        props.setProperty(trustStoreKeyStoreType, TestConstants.TRUSTSTORE_TYPE);
    }

    @Test
    public void testStaticConfiguration() {
        final Properties props = new Properties();
        final PropertiesUtil util = new PropertiesUtil(props);
        // No keystore and truststore -> no SslConfiguration
        SslConfiguration sslConfiguration = SslConfigurationFactory.createSslConfiguration(util);
        assertNull(sslConfiguration);
        // Only keystore
        props.clear();
        addKeystoreConfiguration(props);
        util.reload();
        sslConfiguration = SslConfigurationFactory.createSslConfiguration(util);
        assertNotNull(sslConfiguration);
        assertNotNull(sslConfiguration.getKeyStoreConfig());
        assertNull(sslConfiguration.getTrustStoreConfig());
        // Only truststore
        props.clear();
        addTruststoreConfiguration(props);
        util.reload();
        sslConfiguration = SslConfigurationFactory.createSslConfiguration(util);
        assertNotNull(sslConfiguration);
        assertNull(sslConfiguration.getKeyStoreConfig());
        assertNotNull(sslConfiguration.getTrustStoreConfig());
        // Both
        props.clear();
        addKeystoreConfiguration(props);
        addTruststoreConfiguration(props);
        util.reload();
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
    public void testPasswordLessStores(final String location, final String password) {
        final Properties props = new Properties();
        props.setProperty(keyStoreType, "Windows-MY");
        props.setProperty(trustStoreKeyStoreType, "Windows-ROOT");
        if (location != null) {
            props.setProperty(keyStoreLocation, location);
            props.setProperty(trustStorelocation, location);
        }
        if (password != null) {
            props.setProperty(keyStorePassword, password);
            props.setProperty(trustStorePassword, password);
        }
        final PropertiesUtil util = new PropertiesUtil(props);
        final SslConfiguration config = SslConfigurationFactory.createSslConfiguration(util);
        assertNotNull(config);
        final KeyStoreConfiguration keyStoreConfig = config.getKeyStoreConfig();
        assertNotNull(keyStoreConfig);
        KeyStoreConfigurationTest.checkKeystoreConfiguration(keyStoreConfig);
        final TrustStoreConfiguration trustStoreConfig = config.getTrustStoreConfig();
        assertNotNull(trustStoreConfig);
        KeyStoreConfigurationTest.checkKeystoreConfiguration(trustStoreConfig);
    }
}
