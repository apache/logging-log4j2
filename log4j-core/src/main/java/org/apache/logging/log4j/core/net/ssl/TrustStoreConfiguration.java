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

import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;

/**
 * Configuration of the TrustStore
 */
@Configurable(printObject = true)
@Plugin("TrustStore")
public class TrustStoreConfiguration extends AbstractKeyStoreConfiguration {

    private final String trustManagerFactoryAlgorithm;

    private TrustStoreConfiguration(final String location,
                                    final PasswordProvider passwordProvider,
                                    final String keyStoreType,
                                    final String trustManagerFactoryAlgorithm)
            throws StoreConfigurationException {
        super(location, passwordProvider, keyStoreType);
        this.trustManagerFactoryAlgorithm = trustManagerFactoryAlgorithm == null ? TrustManagerFactory
                .getDefaultAlgorithm() : trustManagerFactoryAlgorithm;
    }

    public TrustManagerFactory initTrustManagerFactory() throws NoSuchAlgorithmException, KeyStoreException {
        final TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(this.trustManagerFactoryAlgorithm);
        tmFactory.init(this.getKeyStore());
        return tmFactory;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((trustManagerFactoryAlgorithm == null) ? 0 : trustManagerFactoryAlgorithm.hashCode());
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
        final TrustStoreConfiguration other = (TrustStoreConfiguration) obj;
        if (trustManagerFactoryAlgorithm == null) {
            if (other.trustManagerFactoryAlgorithm != null) {
                return false;
            }
        } else if (!trustManagerFactoryAlgorithm.equals(other.trustManagerFactoryAlgorithm)) {
            return false;
        }
        return true;
    }

    public String getTrustManagerFactoryAlgorithm() {
        return trustManagerFactoryAlgorithm;
    }

    @PluginFactory
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AbstractKeyStoreConfiguration.Builder<Builder, TrustStoreConfiguration> {
        private String trustManagerFactoryAlgorithm;

        public String getTrustManagerFactoryAlgorithm() {
            return trustManagerFactoryAlgorithm;
        }

        public Builder setTrustManagerFactoryAlgorithm(@PluginAttribute final String trustManagerFactoryAlgorithm) {
            this.trustManagerFactoryAlgorithm = trustManagerFactoryAlgorithm;
            return this;
        }

        @Override
        public TrustStoreConfiguration build() throws StoreConfigurationException {
            return new TrustStoreConfiguration(getLocation(), buildPasswordProvider(), getKeyStoreType(),
                    getTrustManagerFactoryAlgorithm());
        }
    }
}
