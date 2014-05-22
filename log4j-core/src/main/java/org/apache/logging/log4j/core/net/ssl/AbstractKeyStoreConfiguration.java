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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * Configuration of the KeyStore
 */
public class AbstractKeyStoreConfiguration extends StoreConfiguration<KeyStore> {
    private final KeyStore keyStore;
    private final String keyStoreType;

    public AbstractKeyStoreConfiguration(final String location, final String password, final String keyStoreType)
            throws StoreConfigurationException {
        super(location, password);
        this.keyStoreType = keyStoreType == null ? SslConfigurationDefaults.KEYSTORE_TYPE : keyStoreType;
        this.keyStore = this.load();
    }

    @Override
    protected KeyStore load() throws StoreConfigurationException {
        FileInputStream fin = null;

        LOGGER.debug("Loading keystore from file with params(location={})", this.getLocation());
        try {
            if (this.getLocation() == null) {
                throw new IOException("The location is null");
            }
            fin = new FileInputStream(this.getLocation());
            final KeyStore ks = KeyStore.getInstance(this.keyStoreType);
            ks.load(fin, this.getPasswordAsCharArray());
            LOGGER.debug("Keystore successfully loaded with params(location={})", this.getLocation());
            return ks;
        } catch (final CertificateException e) {
            LOGGER.error("No Provider supports a KeyStoreSpi implementation for the specified type {}", this.keyStoreType);
            throw new StoreConfigurationException(e);
        } catch (final NoSuchAlgorithmException e) {
            LOGGER.error("The algorithm used to check the integrity of the keystore cannot be found");
            throw new StoreConfigurationException(e);
        } catch (final KeyStoreException e) {
            LOGGER.error(e);
            throw new StoreConfigurationException(e);
        } catch (final FileNotFoundException e) {
            LOGGER.error("The keystore file({}) is not found", this.getLocation());
            throw new StoreConfigurationException(e);
        } catch (final IOException e) {
            LOGGER.error("Something is wrong with the format of the keystore or the given password");
            throw new StoreConfigurationException(e);
        } finally {
            try {
                if (fin != null) {
                    fin.close();
                }
            } catch (final IOException e) {
                LOGGER.debug(e);
            }
        }
    }

    public KeyStore getKeyStore() {
        return this.keyStore;
    }

}
