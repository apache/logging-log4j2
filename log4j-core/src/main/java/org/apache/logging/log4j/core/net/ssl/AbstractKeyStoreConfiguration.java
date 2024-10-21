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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Objects;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.util.NetUtils;

/**
 * Configuration of the KeyStore
 */
public class AbstractKeyStoreConfiguration extends StoreConfiguration<KeyStore> {

    private final KeyStore keyStore;
    private final String keyStoreType;

    public AbstractKeyStoreConfiguration(
            final String location, final PasswordProvider passwordProvider, final String keyStoreType)
            throws StoreConfigurationException {
        super(location, passwordProvider);
        this.keyStoreType = keyStoreType == null ? SslConfigurationDefaults.KEYSTORE_TYPE : keyStoreType;
        this.keyStore = this.load();
    }

    /**
     * @deprecated Use {@link #AbstractKeyStoreConfiguration(String, PasswordProvider, String)} instead
     */
    @Deprecated
    public AbstractKeyStoreConfiguration(final String location, final char[] password, final String keyStoreType)
            throws StoreConfigurationException {
        this(location, new MemoryPasswordProvider(password), keyStoreType);
    }

    /**
     * @deprecated Use {@link #AbstractKeyStoreConfiguration(String, PasswordProvider, String)} instead
     */
    @Deprecated
    public AbstractKeyStoreConfiguration(final String location, final String password, final String keyStoreType)
            throws StoreConfigurationException {
        this(location, new MemoryPasswordProvider(password == null ? null : password.toCharArray()), keyStoreType);
    }

    @Override
    protected KeyStore load() throws StoreConfigurationException {
        final String loadLocation = this.getLocation();
        final char[] password = this.getPasswordAsCharArray();
        LOGGER.debug("Loading keystore from location {}", loadLocation);
        try {
            final KeyStore ks = KeyStore.getInstance(this.keyStoreType);
            if (loadLocation == null) {
                if (keyStoreType.equalsIgnoreCase(JKS) || keyStoreType.equalsIgnoreCase(PKCS12)) {
                    throw new IOException("The location is null");
                }
                ks.load(null, password);
                LOGGER.debug("KeyStore successfully loaded");
                return ks;
            }
            try (final InputStream fin = openInputStream(loadLocation)) {
                ks.load(fin, password);
                LOGGER.debug("KeyStore successfully loaded from location {}", loadLocation);
                return ks;
            }
        } catch (final CertificateException e) {
            LOGGER.error(
                    "No Provider supports a KeyStoreSpi implementation for the specified type {} for location {}",
                    this.keyStoreType,
                    loadLocation,
                    e);
            throw new StoreConfigurationException(loadLocation, e);
        } catch (final NoSuchAlgorithmException e) {
            LOGGER.error(
                    "The algorithm used to check the integrity of the keystore cannot be found for location {}",
                    loadLocation,
                    e);
            throw new StoreConfigurationException(loadLocation, e);
        } catch (final KeyStoreException e) {
            LOGGER.error("KeyStoreException for location {}", loadLocation, e);
            throw new StoreConfigurationException(loadLocation, e);
        } catch (final FileNotFoundException e) {
            LOGGER.error("The keystore file {} is not found", loadLocation, e);
            throw new StoreConfigurationException(loadLocation, e);
        } catch (final IOException e) {
            LOGGER.error(
                    "Something is wrong with the format of the keystore or the given password for location {}",
                    loadLocation,
                    e);
            throw new StoreConfigurationException(loadLocation, e);
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }

    private InputStream openInputStream(final String filePathOrUri) {
        return ConfigurationSource.fromUri(NetUtils.toURI(filePathOrUri)).getInputStream();
    }

    public KeyStore getKeyStore() {
        return this.keyStore;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((keyStore == null) ? 0 : keyStore.hashCode());
        result = prime * result + ((keyStoreType == null) ? 0 : keyStoreType.hashCode());
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
        final AbstractKeyStoreConfiguration other = (AbstractKeyStoreConfiguration) obj;
        if (!Objects.equals(keyStore, other.keyStore)) {
            return false;
        }
        if (!Objects.equals(keyStoreType, other.keyStoreType)) {
            return false;
        }
        return true;
    }

    public String getKeyStoreType() {
        return keyStoreType;
    }
}
