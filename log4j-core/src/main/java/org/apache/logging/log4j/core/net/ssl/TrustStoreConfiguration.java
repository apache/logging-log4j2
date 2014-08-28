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

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.TrustManagerFactory;

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * Configuration of the TrustStore
 */
@Plugin(name = "TrustStore", category = "Core", printObject = true)
public class TrustStoreConfiguration extends AbstractKeyStoreConfiguration {

    private final String trustManagerFactoryAlgorithm;

    public TrustStoreConfiguration(final String location, final String password, final String keyStoreType,
            final String trustManagerFactoryAlgorithm) throws StoreConfigurationException {
        super(location, password, keyStoreType);
        this.trustManagerFactoryAlgorithm = trustManagerFactoryAlgorithm == null ? TrustManagerFactory
                .getDefaultAlgorithm() : trustManagerFactoryAlgorithm;
    }

    /**
     * Creates a KeyStoreConfiguration.
     * 
     * @param location
     *        The location of the KeyStore.
     * @param password
     *        The password to access the KeyStore.
     * @param keyStoreType
     *        The KeyStore type, null defaults to {@code "JKS"}.
     * @param trustManagerFactoryAlgorithm
     *        TODO
     * @return a new TrustStoreConfiguration
     * @throws StoreConfigurationException
     */
    @PluginFactory
    public static TrustStoreConfiguration createKeyStoreConfiguration(
            // @formatter:off
            @PluginAttribute("location") final String location,
            @PluginAttribute("password") final String password,
            @PluginAttribute("type") final String keyStoreType, 
            @PluginAttribute("trustManagerFactoryAlgorithm") final String trustManagerFactoryAlgorithm) throws StoreConfigurationException {
            // @formatter:on
        return new TrustStoreConfiguration(location, password, keyStoreType, trustManagerFactoryAlgorithm);
    }

    public TrustManagerFactory initTrustManagerFactory() throws NoSuchAlgorithmException, KeyStoreException {
        final TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(this.trustManagerFactoryAlgorithm);
        tmFactory.init(this.getKeyStore());
        return tmFactory;
    }
}
