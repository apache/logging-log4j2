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
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.Strings;

/**
 * Creates an SSL configuration from Log4j properties.
 */
public class SslConfigurationFactory {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final SslConfiguration sslConfiguration = createSslConfiguration(PropertiesUtil.getProperties());

    private static final String trustStorelocation = "log4j2.trustStoreLocation";
    private static final String trustStorePassword = "log4j2.trustStorePassword";
    private static final String trustStorePasswordFile = "log4j2.trustStorePasswordFile";
    private static final String trustStorePasswordEnvVar = "log4j2.trustStorePasswordEnvironmentVariable";
    private static final String trustStoreKeyStoreType = "log4j2.trustStoreKeyStoreType";
    private static final String trustStoreKeyManagerFactoryAlgorithm = "log4j2.trustStoreKeyManagerFactoryAlgorithm";
    private static final String keyStoreLocation = "log4j2.keyStoreLocation";
    private static final String keyStorePassword = "log4j2.keyStorePassword";
    private static final String keyStorePasswordFile = "log4j2.keyStorePasswordFile";
    private static final String keyStorePasswordEnvVar = "log4j2.keyStorePasswordEnvironmentVariable";
    private static final String keyStoreType = "log4j2.keyStoreType";
    private static final String keyStoreKeyManagerFactoryAlgorithm = "log4j2.keyStoreKeyManagerFactoryAlgorithm";
    private static final String verifyHostName = "log4j2.sslVerifyHostName";

    static SslConfiguration createSslConfiguration(final PropertiesUtil props) {
        KeyStoreConfiguration keyStoreConfiguration = null;
        TrustStoreConfiguration trustStoreConfiguration = null;
        String location = props.getStringProperty(trustStorelocation);
        String storeType = props.getStringProperty(trustStoreKeyStoreType);
        if (Strings.isNotEmpty(location) || storeType != null) {
            final String password = props.getStringProperty(trustStorePassword);
            final char[] passwordChars = getPassword(password, storeType);
            try {
                trustStoreConfiguration = TrustStoreConfiguration.createKeyStoreConfiguration(
                        Strings.trimToNull(location),
                        passwordChars,
                        props.getStringProperty(trustStorePasswordEnvVar),
                        props.getStringProperty(trustStorePasswordFile),
                        storeType,
                        props.getStringProperty(trustStoreKeyManagerFactoryAlgorithm));
            } catch (Exception ex) {
                LOGGER.warn(
                        "Unable to create trust store configuration due to: {} {}",
                        ex.getClass().getName(),
                        ex.getMessage());
            }
        }
        location = props.getStringProperty(keyStoreLocation);
        storeType = props.getStringProperty(keyStoreType);
        if (Strings.isNotEmpty(location) || storeType != null) {
            final String password = props.getStringProperty(keyStorePassword);
            final char[] passwordChars = getPassword(password, storeType);
            try {
                keyStoreConfiguration = KeyStoreConfiguration.createKeyStoreConfiguration(
                        Strings.trimToNull(location),
                        passwordChars,
                        props.getStringProperty(keyStorePasswordEnvVar),
                        props.getStringProperty(keyStorePasswordFile),
                        storeType,
                        props.getStringProperty(keyStoreKeyManagerFactoryAlgorithm));
            } catch (Exception ex) {
                LOGGER.warn(
                        "Unable to create key store configuration due to: {} {}",
                        ex.getClass().getName(),
                        ex.getMessage());
            }
        }
        if (trustStoreConfiguration != null || keyStoreConfiguration != null) {
            final boolean isVerifyHostName = props.getBooleanProperty(verifyHostName, false);
            return SslConfiguration.createSSLConfiguration(
                    null, keyStoreConfiguration, trustStoreConfiguration, isVerifyHostName);
        }
        return null;
    }

    private static char[] getPassword(final String password, final String keyStoreType) {
        // Note from Tomcat's SSLUtiBase#getStore:
        //
        // JKS key stores treat null and "" interchangeably.
        // PKCS12 key stores don't return the cert if null is used.
        // Key stores that do not use passwords expect null
        // Therefore:
        // - generally use null if pass is null or ""
        // - for JKS or PKCS12 only use null if pass is null
        //   (because JKS will auto-switch to PKCS12)
        if (keyStoreType.equals(StoreConfiguration.JKS) || keyStoreType.equals(StoreConfiguration.PKCS12)) {
            return password != null ? password.toCharArray() : null;
        }
        return Strings.isEmpty(password) ? null : password.toCharArray();
    }

    public static SslConfiguration getSslConfiguration() {
        return sslConfiguration;
    }
}
