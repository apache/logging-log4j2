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

import java.io.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * The TrustStoreConfiguration.
 */
@Plugin(name = "trustStore", category = "Core", printObject = true)
public class TrustStoreConfiguration extends StoreConfiguration {
    private KeyStore trustStore;
    private String trustStoreType;

    public TrustStoreConfiguration(String location, String password) {
        super(location, password);
        trustStoreType = SSLConfigurationDefaults.KEYSTORE_TYPE;
        trustStore = null;
    }

    @Override
    protected void load() throws StoreConfigurationException {
        KeyStore ts = null;
        InputStream in = null;

        LOGGER.debug("Loading truststore from file with params(location={})", getLocation());
        try {
            if (getLocation() == null) {
                throw new IOException("The location is null");
            }
            ts = KeyStore.getInstance(trustStoreType);
            in = new FileInputStream(getLocation());
            ts.load(in, getPasswordAsCharArray());
        }
        catch (CertificateException e) {
            LOGGER.error("No Provider supports a KeyStoreSpi implementation for the specified type {}", trustStoreType);
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
            LOGGER.error("Something is wrong with the format of the truststore or the given password: {}", e.getMessage());
            throw new StoreConfigurationException(e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            }
            catch (Exception e) {
                LOGGER.warn("Error closing {}", getLocation(), e);
            }
        }
        trustStore = ts;
        LOGGER.debug("Truststore successfully loaded with params(location={})", getLocation());
    }

    public KeyStore getTrustStore() throws StoreConfigurationException {
        if (trustStore == null) {
            load();
        }
        return trustStore;
    }

    /**
     * Create a TrustStoreConfiguration.
     * @param location The location of the TrustStore.
     * @param password The password required to access the TrustStore.
     * @return
     */
    @PluginFactory
    public static TrustStoreConfiguration createTrustStoreConfiguration(@PluginAttribute("location") String location,
                                                                        @PluginAttribute("password") String password){
        return new TrustStoreConfiguration(location, password);
    }
}
