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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.impl.Log4jProperties;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.PropertyEnvironment;

/**
 * Creates an SSL configuration from Log4j properties.
 */
public class SslConfigurationFactory {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final Lazy<SslConfiguration> SSL_CONFIGURATION = Lazy.lazy(() -> {
        final PropertyEnvironment props = PropertiesUtil.getProperties();
        return createSslConfiguration(props);
    });

    static SslConfiguration createSslConfiguration(final PropertyEnvironment props) {
        KeyStoreConfiguration keyStoreConfiguration = null;
        TrustStoreConfiguration trustStoreConfiguration = null;
        String location = props.getStringProperty(Log4jProperties.TRANSPORT_SECURITY_TRUST_STORE_LOCATION);
        if (location != null) {
            final String password = props.getStringProperty(Log4jProperties.TRANSPORT_SECURITY_TRUST_STORE_PASSWORD);
            char[] passwordChars = null;
            if (password != null) {
                passwordChars = password.toCharArray();
            }
            try {
                trustStoreConfiguration = TrustStoreConfiguration.createKeyStoreConfiguration(location, passwordChars,
                        props.getStringProperty(Log4jProperties.TRANSPORT_SECURITY_TRUST_STORE_PASSWORD_ENV_VAR),
                        props.getStringProperty(Log4jProperties.TRANSPORT_SECURITY_TRUST_STORE_PASSWORD_FILE),
                        props.getStringProperty(Log4jProperties.TRANSPORT_SECURITY_TRUST_STORE_KEY_STORE_TYPE),
                        props.getStringProperty(Log4jProperties.TRANSPORT_SECURITY_TRUST_STORE_KEY_MANAGER_FACTORY_ALGORITHM));
            } catch (final Exception ex) {
                LOGGER.warn("Unable to create trust store configuration due to: {} {}", ex.getClass().getName(),
                        ex.getMessage());
            }
        }
        location = props.getStringProperty(Log4jProperties.TRANSPORT_SECURITY_KEY_STORE_LOCATION);
        if (location != null) {
            final String password = props.getStringProperty(Log4jProperties.TRANSPORT_SECURITY_KEY_STORE_PASSWORD);
            char[] passwordChars = null;
            if (password != null) {
                passwordChars = password.toCharArray();
            }
            try {
                keyStoreConfiguration = KeyStoreConfiguration.createKeyStoreConfiguration(location, passwordChars,
                        props.getStringProperty(Log4jProperties.TRANSPORT_SECURITY_KEY_STORE_PASSWORD_ENV_VAR),
                        props.getStringProperty(Log4jProperties.TRANSPORT_SECURITY_KEY_STORE_PASSWORD_FILE),
                        props.getStringProperty(Log4jProperties.TRANSPORT_SECURITY_KEY_STORE_TYPE),
                        props.getStringProperty(Log4jProperties.TRANSPORT_SECURITY_KEY_STORE_KEY_MANAGER_FACTORY_ALGORITHM));
            } catch (final Exception ex) {
                LOGGER.warn("Unable to create key store configuration due to: {} {}", ex.getClass().getName(),
                        ex.getMessage());
            }
        }
        if (trustStoreConfiguration != null || keyStoreConfiguration != null) {
            final boolean isVerifyHostName = props.getBooleanProperty(Log4jProperties.TRANSPORT_SECURITY_VERIFY_HOST_NAME, false);
            return SslConfiguration.createSSLConfiguration(null, keyStoreConfiguration,
                    trustStoreConfiguration, isVerifyHostName);
        }
        return null;
    }

    public static SslConfiguration getSslConfiguration() {
        return SSL_CONFIGURATION.value();
    }
}
