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

import java.util.Properties;

import org.apache.logging.log4j.core.test.net.ssl.TestConstants;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SslConfigurationFactoryTest {

    private static void addKeystoreConfiguration(Properties props) {
        props.setProperty("log4j2.keyStoreLocation", TestConstants.KEYSTORE_FILE_RESOURCE);
        props.setProperty("log4j2.keyStoreKeyStoreType", TestConstants.KEYSTORE_TYPE);
    }

    private static void addTruststoreConfiguration(Properties props) {
        props.setProperty("log4j2.trustStoreLocation", TestConstants.TRUSTSTORE_FILE_RESOURCE);
        props.setProperty("log4j2.trustStoreKeyStoreType", TestConstants.TRUSTSTORE_TYPE);
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
}
