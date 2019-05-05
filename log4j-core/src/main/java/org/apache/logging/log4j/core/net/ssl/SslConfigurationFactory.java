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
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * Creates an SSL configuration from Log4j properties.
 */
public class SslConfigurationFactory {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static SslConfiguration sslConfiguration = null;

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

    static {
        PropertiesUtil props = PropertiesUtil.getProperties();
        KeyStoreConfiguration keyStoreConfiguration = null;
        TrustStoreConfiguration trustStoreConfiguration = null;
        String location = props.getStringProperty(trustStorelocation);
        if (location != null) {
            String password = props.getStringProperty(trustStorePassword);
            char[] passwordChars = null;
            if (password != null) {
                passwordChars = password.toCharArray();
            }
            try {
                trustStoreConfiguration = TrustStoreConfiguration.createKeyStoreConfiguration(location, passwordChars,
                    props.getStringProperty(trustStorePasswordEnvVar), props.getStringProperty(trustStorePasswordFile),
                    props.getStringProperty(trustStoreKeyStoreType), props.getStringProperty(trustStoreKeyManagerFactoryAlgorithm));
            } catch (Exception ex) {
                LOGGER.warn("Unable to create trust store configuration due to: {} {}", ex.getClass().getName(),
                    ex.getMessage());
            }
        }
        location = props.getStringProperty(keyStoreLocation);
        if (location != null) {
            String password = props.getStringProperty(keyStorePassword);
            char[] passwordChars = null;
            if (password != null) {
                passwordChars = password.toCharArray();
            }
            try {
                keyStoreConfiguration = KeyStoreConfiguration.createKeyStoreConfiguration(location, passwordChars,
                    props.getStringProperty(keyStorePasswordEnvVar), props.getStringProperty(keyStorePasswordFile),
                    props.getStringProperty(keyStoreType), props.getStringProperty(keyStoreKeyManagerFactoryAlgorithm));
            } catch (Exception ex) {
                LOGGER.warn("Unable to create key store configuration due to: {} {}", ex.getClass().getName(),
                    ex.getMessage());
            }
        }
        if (trustStoreConfiguration != null || keyStoreConfiguration != null) {
            boolean isVerifyHostName = props.getBooleanProperty(verifyHostName, false);
            sslConfiguration = SslConfiguration.createSSLConfiguration("https", keyStoreConfiguration,
                trustStoreConfiguration, isVerifyHostName);
        }
    }

    public static SslConfiguration getSslConfiguration() {
        return sslConfiguration;
    }
}
