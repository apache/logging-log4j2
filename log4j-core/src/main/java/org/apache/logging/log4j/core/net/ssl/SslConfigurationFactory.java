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

import java.util.Optional;
import java.util.function.Supplier;

import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.util.PropertyResolver;

import static org.apache.logging.log4j.core.impl.Log4jProperties.*;

/**
 * Creates an SSL configuration from Log4j properties.
 */
public class SslConfigurationFactory implements Supplier<SslConfiguration> {

    private final PropertyResolver propertyResolver;

    @Inject
    public SslConfigurationFactory(final PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
    }

    @Override
    public SslConfiguration get() {
        final String trustStoreAlgorithm =
                propertyResolver.getString(TRANSPORT_SECURITY_TRUST_STORE_KEY_MANAGER_FACTORY_ALGORITHM)
                        .orElse(null);
        final TrustStoreConfiguration trustStoreConfiguration = initKeyStoreConfigBuilder(
                TrustStoreConfiguration::builder,
                TRANSPORT_SECURITY_TRUST_STORE_LOCATION,
                TRANSPORT_SECURITY_TRUST_STORE_PASSWORD,
                TRANSPORT_SECURITY_TRUST_STORE_PASSWORD_ENV_VAR,
                TRANSPORT_SECURITY_TRUST_STORE_PASSWORD_FILE,
                TRANSPORT_SECURITY_TRUST_STORE_KEY_STORE_TYPE)
                .map(builder -> builder.setTrustManagerFactoryAlgorithm(trustStoreAlgorithm).get())
                .orElse(null);
        final String keyStoreAlgorithm =
                propertyResolver.getString(TRANSPORT_SECURITY_KEY_STORE_KEY_MANAGER_FACTORY_ALGORITHM)
                        .orElse(null);
        final KeyStoreConfiguration keyStoreConfiguration = initKeyStoreConfigBuilder(
                KeyStoreConfiguration::builder,
                TRANSPORT_SECURITY_KEY_STORE_LOCATION,
                TRANSPORT_SECURITY_KEY_STORE_PASSWORD,
                TRANSPORT_SECURITY_KEY_STORE_PASSWORD_ENV_VAR,
                TRANSPORT_SECURITY_KEY_STORE_PASSWORD_FILE,
                TRANSPORT_SECURITY_KEY_STORE_TYPE)
                .map(builder -> builder.setKeyManagerFactoryAlgorithm(keyStoreAlgorithm).get())
                .orElse(null);
        if (trustStoreConfiguration != null || keyStoreConfiguration != null) {
            final boolean isVerifyHostName = propertyResolver.getBoolean(TRANSPORT_SECURITY_VERIFY_HOST_NAME);
            return SslConfiguration.createSSLConfiguration(null, keyStoreConfiguration, trustStoreConfiguration, isVerifyHostName);
        }
        return null;
    }

    private <B extends AbstractKeyStoreConfiguration.Builder<B, C>, C extends AbstractKeyStoreConfiguration>
    Optional<B> initKeyStoreConfigBuilder(final Supplier<B> builderFactory,
                                          final String locationKey,
                                          final String passwordKey,
                                          final String passwordEnvVarKey,
                                          final String passwordFileKey,
                                          final String keyStoreTypeKey) {
        return propertyResolver.getString(locationKey)
                .map(location -> builderFactory.get()
                        .setLocation(location)
                        .setPassword(propertyResolver.getString(passwordKey).map(String::toCharArray).orElse(null))
                        .setPasswordEnvironmentVariable(propertyResolver.getString(passwordEnvVarKey).orElse(null))
                        .setPasswordFile(propertyResolver.getString(passwordFileKey).orElse(null))
                        .setKeyStoreType(propertyResolver.getString(keyStoreTypeKey).orElse(null)));
    }
}
