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
import java.util.Arrays;
import javax.net.ssl.KeyManagerFactory;

import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;

/**
 * Configuration of the KeyStore
 */
@Configurable(printObject = true)
@Plugin("KeyStore")
public class KeyStoreConfiguration extends AbstractKeyStoreConfiguration {

    private final String keyManagerFactoryAlgorithm;

    /**
     *
     * @throws StoreConfigurationException Thrown if this instance cannot load the KeyStore.
     */
    private KeyStoreConfiguration(final String location,
                                  final PasswordProvider passwordProvider,
                                  final String keyStoreType,
                                  final String keyManagerFactoryAlgorithm)
            throws StoreConfigurationException {
        super(location, passwordProvider, keyStoreType);
        this.keyManagerFactoryAlgorithm = keyManagerFactoryAlgorithm == null ? KeyManagerFactory.getDefaultAlgorithm()
                : keyManagerFactoryAlgorithm;
    }

    public KeyManagerFactory initKeyManagerFactory() throws NoSuchAlgorithmException, UnrecoverableKeyException,
            KeyStoreException {
        final KeyManagerFactory kmFactory = KeyManagerFactory.getInstance(this.keyManagerFactoryAlgorithm);
        final char[] password = this.getPassword();
        try {
            kmFactory.init(this.getKeyStore(), password != null ? password : DEFAULT_PASSWORD);
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
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

    public String getKeyManagerFactoryAlgorithm() {
        return keyManagerFactoryAlgorithm;
    }

    @PluginFactory
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AbstractKeyStoreConfiguration.Builder<Builder, KeyStoreConfiguration> {
        private String keyManagerFactoryAlgorithm;

        public String getKeyManagerFactoryAlgorithm() {
            return keyManagerFactoryAlgorithm;
        }

        public Builder setKeyManagerFactoryAlgorithm(@PluginAttribute final String keyManagerFactoryAlgorithm) {
            this.keyManagerFactoryAlgorithm = keyManagerFactoryAlgorithm;
            return this;
        }

        @Override
        public KeyStoreConfiguration build() throws StoreConfigurationException {
            return new KeyStoreConfiguration(getLocation(), buildPasswordProvider(), getKeyStoreType(),
                    getKeyManagerFactoryAlgorithm());
        }
    }
}
