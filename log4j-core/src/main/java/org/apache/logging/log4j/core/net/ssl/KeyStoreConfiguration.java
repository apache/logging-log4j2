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

import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * Configuration of the KeyStore
 */
@Plugin(name = "KeyStore", category = Core.CATEGORY_NAME, printObject = true)
public class KeyStoreConfiguration extends AbstractKeyStoreConfiguration {

    private final String keyManagerFactoryAlgorithm;

    /**
     * 
     * @throws StoreConfigurationException Thrown if this instance cannot load the KeyStore.
     */
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
     *         The standard name of the requested algorithm. See the Java Secure Socket Extension Reference Guide for information about these names.
     * @return a new KeyStoreConfiguration
     * @throws StoreConfigurationException Thrown if this call cannot load the KeyStore.
     */
    @PluginFactory
    public static KeyStoreConfiguration createKeyStoreConfiguration(
            // @formatter:off
            @PluginAttribute("location") final String location,
            @PluginAttribute(value = "password", sensitive = true) final String password,
            @PluginAttribute("type") final String keyStoreType, 
            @PluginAttribute("keyManagerFactoryAlgorithm") final String keyManagerFactoryAlgorithm) throws StoreConfigurationException {
            // @formatter:on
        return new KeyStoreConfiguration(location, password, keyStoreType, keyManagerFactoryAlgorithm);
    }

    public KeyManagerFactory initKeyManagerFactory() throws NoSuchAlgorithmException, UnrecoverableKeyException,
            KeyStoreException {
        final KeyManagerFactory kmFactory = KeyManagerFactory.getInstance(this.keyManagerFactoryAlgorithm);
        kmFactory.init(this.getKeyStore(), this.getPasswordAsCharArray());
        return kmFactory;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((keyManagerFactoryAlgorithm == null) ? 0 : keyManagerFactoryAlgorithm.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final KeyStoreConfiguration other = (KeyStoreConfiguration) obj;
        if (keyManagerFactoryAlgorithm == null) {
            if (other.keyManagerFactoryAlgorithm != null) {
                return false;
            }
        } else if (!keyManagerFactoryAlgorithm.equals(other.keyManagerFactoryAlgorithm)) {
            return false;
        }
        return true;
    }
}
