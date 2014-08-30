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
import java.security.UnrecoverableKeyException;

import javax.net.ssl.KeyManagerFactory;

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * Configuration of the KeyStore
 */
@Plugin(name = "KeyStore", category = "Core", printObject = true)
public class KeyStoreConfiguration extends AbstractKeyStoreConfiguration {

    private final String keyManagerFactoryAlgorithm;

    public KeyStoreConfiguration(final String location, final String password, final String keyStoreType,
            final String keyManagerFactoryAlgorithm) throws StoreConfigurationException {
        super(location, password, keyStoreType);
        this.keyManagerFactoryAlgorithm = keyManagerFactoryAlgorithm == null ? KeyManagerFactory.getDefaultAlgorithm()
                : keyManagerFactoryAlgorithm;
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
     * @param keyManagerFactoryAlgorithm
     *        TODO
     * @return a new KeyStoreConfiguration
     * @throws StoreConfigurationException
     */
    @PluginFactory
    public static KeyStoreConfiguration createKeyStoreConfiguration(
            // @formatter:off
            @PluginAttribute("location") final String location,
            @PluginAttribute("password") final String password,
            @PluginAttribute("type") final String keyStoreType, 
            @PluginAttribute("keyManagerFactoryAlgorithm") final String keyManagerFactoryAlgorithm) throws StoreConfigurationException {
            // @formatter:on
        return new KeyStoreConfiguration(location, password, keyStoreType, null);
    }

    public KeyManagerFactory initKeyManagerFactory() throws NoSuchAlgorithmException, UnrecoverableKeyException,
            KeyStoreException {
        final KeyManagerFactory kmFactory = KeyManagerFactory.getInstance(this.keyManagerFactoryAlgorithm);
        kmFactory.init(this.getKeyStore(), this.getPasswordAsCharArray());
        return kmFactory;
    }
}
