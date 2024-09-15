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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.impl.CoreProperties.KeyStoreProperties;
import org.apache.logging.log4j.core.impl.CoreProperties.TransportSecurityProperties;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Creates an SSL configuration from Log4j properties.
 */
public final class SslConfigurationFactory {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private SslConfigurationFactory() {}

    public static SslConfiguration getSslConfiguration(final PropertyEnvironment props) {
        return getSslConfiguration(props.getProperty(TransportSecurityProperties.class));
    }

    static SslConfiguration getSslConfiguration(final TransportSecurityProperties config) {
        KeyStoreConfiguration keyStoreConfiguration = null;
        TrustStoreConfiguration trustStoreConfiguration = null;
        if (config != null) {
            final KeyStoreProperties trustStoreProperties = config.trustStore();
            if (trustStoreProperties.location() != null || trustStoreProperties.type() != null) {
                try {
                    trustStoreConfiguration = TrustStoreConfiguration.createKeyStoreConfiguration(trustStoreProperties);
                } catch (final Exception error) {
                    LOGGER.error("Failed to create the trust store configuration", error);
                }
            }
            final KeyStoreProperties keyStoreProperties = config.keyStore();
            if (keyStoreProperties.location() != null || keyStoreProperties.type() != null) {
                try {
                    keyStoreConfiguration = KeyStoreConfiguration.createKeyStoreConfiguration(keyStoreProperties);
                } catch (final Exception error) {
                    LOGGER.error("Failed to create the key store configuration", error);
                }
            }
        }
        return trustStoreConfiguration != null || keyStoreConfiguration != null
                ? SslConfiguration.createSSLConfiguration(
                        null, keyStoreConfiguration, trustStoreConfiguration, config.verifyHostName())
                : null;
    }
}
