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

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;

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
@Plugin(name = "keyStore", category = "Core", printObject = true)
public class KeyStoreConfiguration extends StoreConfiguration {
    private KeyStore keyStore;
    private String keyStoreType;


    public KeyStoreConfiguration(String location, String password) {
        super(location, password);
        this.keyStoreType = SSLConfigurationDefaults.KEYSTORE_TYPE;
        this.keyStore = null;
    }

    @Override
    protected void load() throws StoreConfigurationException {
        FileInputStream fin = null;

        LOGGER.debug("Loading keystore from file with params(location={})", getLocation());
        try {
            if (getLocation() == null) {
                throw new IOException("The location is null");
            }
            fin = new FileInputStream(getLocation());
            KeyStore ks = KeyStore.getInstance(keyStoreType);
            ks.load(fin, getPasswordAsCharArray());
            keyStore = ks;
        }
         catch (CertificateException e) {
            LOGGER.error("No Provider supports a KeyStoreSpi implementation for the specified type {}", keyStoreType);
            throw new StoreConfigurationException(e);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("The algorithm used to check the integrity of the keystore cannot be found");
            throw new StoreConfigurationException(e);
        } catch (KeyStoreException e) {
            LOGGER.error(e);
            throw new StoreConfigurationException(e);
        } catch (FileNotFoundException e) {
            LOGGER.error("The keystore file({}) is not found", getLocation());
            throw new StoreConfigurationException(e);
        } catch (IOException e) {
            LOGGER.error("Something is wrong with the format of the keystore or the given password");
            throw new StoreConfigurationException(e);
        }
        finally {
            try {
                if (fin != null)
                    fin.close();
            } catch (IOException e) {
            }
        }
        LOGGER.debug("Keystore successfully loaded with params(location={})", getLocation());
    }

    public KeyStore getKeyStore() throws StoreConfigurationException {
        if (keyStore == null) {
            load();
        }
        return keyStore;
    }

    /**
     * Create a KeyStoreConfiguration.
     * @param location The location of the KeyStore.
     * @param password The password to access the KeyStore.
     * @return
     */
    @PluginFactory
    public static KeyStoreConfiguration createKeyStoreConfiguration(
            @PluginAttribute("location") String location,
            @PluginAttribute("password") String password) {
        return new KeyStoreConfiguration(location, password);
    }
}
