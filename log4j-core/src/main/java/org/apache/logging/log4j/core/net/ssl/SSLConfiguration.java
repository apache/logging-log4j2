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
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;

import javax.net.ssl.*;
import java.security.*;

/**
 *  SSL Configuration
 */
@Plugin(name = "ssl", category = "Core", printObject = true)
public class SSLConfiguration {
    private static final StatusLogger LOGGER = StatusLogger.getLogger();
    private KeyStoreConfiguration keyStoreConfig;
    private TrustStoreConfiguration trustStoreConfig;
    private SSLContext sslContext;

    private SSLConfiguration(KeyStoreConfiguration keyStoreConfig, TrustStoreConfiguration trustStoreConfig) {
        this.keyStoreConfig = keyStoreConfig;
        this.trustStoreConfig = trustStoreConfig;
        this.sslContext = null;
    }

    public SSLSocketFactory getSSLSocketFactory() {
        if (sslContext == null) {
            this.sslContext = createSSLContext();
        }
        return sslContext.getSocketFactory();
    }

    public SSLServerSocketFactory getSSLServerSocketFactory() {
            if (sslContext == null) {
                this.sslContext = createSSLContext();
            }
            return sslContext.getServerSocketFactory();
    }

    private SSLContext createSSLContext() {
        SSLContext context = null;

        try {
            context = createSSLContextBasedOnConfiguration();
            LOGGER.debug("Creating SSLContext with the given parameters");
        }
        catch (TrustStoreConfigurationException e) {
            context = createSSLContextWithTrustStoreFailure();
        }
        catch (KeyStoreConfigurationException e) {
            context = createSSLContextWithKeyStoreFailure();
        }
        return context;
    }

    private SSLContext createSSLContextWithTrustStoreFailure() {
        SSLContext context;

        try {
            context = createSSLContextWithDefaultTrustManagerFactory();
            LOGGER.debug("Creating SSLContext with default truststore");
        }
        catch (KeyStoreConfigurationException e) {
            context = createDefaultSSLContext();
            LOGGER.debug("Creating SSLContext with default configuration");
        }
        return context;
    }

    private SSLContext createSSLContextWithKeyStoreFailure() {
        SSLContext context;

        try {
            context = createSSLContextWithDefaultKeyManagerFactory();
            LOGGER.debug("Creating SSLContext with default keystore");
        }
        catch (TrustStoreConfigurationException e) {
            context = createDefaultSSLContext();
            LOGGER.debug("Creating SSLContext with default configuration");
        }
        return context;
    }

    private SSLContext createSSLContextBasedOnConfiguration() throws KeyStoreConfigurationException, TrustStoreConfigurationException {
        return createSSLContext(false, false);
    }

    private SSLContext createSSLContextWithDefaultKeyManagerFactory() throws TrustStoreConfigurationException {
        try {
            return createSSLContext(true, false);
        } catch (KeyStoreConfigurationException dummy) {
             LOGGER.debug("Exception occured while using default keystore. This should be a BUG");
             return null;
        }
    }

    private SSLContext createSSLContextWithDefaultTrustManagerFactory() throws KeyStoreConfigurationException {
        try {
            return createSSLContext(false, true);
        }
        catch (TrustStoreConfigurationException dummy) {
            LOGGER.debug("Exception occured while using default truststore. This should be a BUG");
            return null;
        }
    }

    private SSLContext createDefaultSSLContext() {
        try {
            return SSLContext.getDefault();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Failed to create an SSLContext with default configuration");
            return null;
        }
    }

    private SSLContext createSSLContext(boolean loadDefaultKeyManagerFactory, boolean loadDefaultTrustManagerFactory)
            throws KeyStoreConfigurationException, TrustStoreConfigurationException {
        try {
            KeyManager[] kManagers = null;
            TrustManager[] tManagers = null;

            SSLContext sslContext = SSLContext.getInstance(SSLConfigurationDefaults.PROTOCOL);
            if (!loadDefaultKeyManagerFactory) {
                KeyManagerFactory kmFactory = loadKeyManagerFactory();
                kManagers = kmFactory.getKeyManagers();
            }
            if (!loadDefaultTrustManagerFactory) {
                TrustManagerFactory tmFactory = loadTrustManagerFactory();
                tManagers = tmFactory.getTrustManagers();
            }

            sslContext.init(kManagers, tManagers, null);
            return sslContext;
        }
        catch (NoSuchAlgorithmException e) {
            LOGGER.error("No Provider supports a TrustManagerFactorySpi implementation for the specified protocol");
            throw new TrustStoreConfigurationException(e);
        }
        catch (KeyManagementException e) {
            LOGGER.error("Failed to initialize the SSLContext");
            throw new KeyStoreConfigurationException(e);
        }
    }

    private TrustManagerFactory loadTrustManagerFactory() throws TrustStoreConfigurationException {
        KeyStore trustStore = null;
        TrustManagerFactory tmFactory = null;

        if (trustStoreConfig == null)
            throw new TrustStoreConfigurationException(new Exception("The trustStoreConfiguration is null"));

        try {
            trustStore = trustStoreConfig.getTrustStore();
            tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmFactory.init(trustStore);
        }
        catch (NoSuchAlgorithmException e) {
            LOGGER.error("The specified algorithm is not available from the specified provider");
            throw new TrustStoreConfigurationException(e);
        } catch (KeyStoreException e) {
            LOGGER.error("Failed to initialize the TrustManagerFactory");
            throw new TrustStoreConfigurationException(e);
        } catch (StoreConfigurationException e) {
            throw new TrustStoreConfigurationException(e);
        }

        return tmFactory;
    }

    private KeyManagerFactory loadKeyManagerFactory() throws KeyStoreConfigurationException {
        KeyStore keyStore = null;
        KeyManagerFactory kmFactory = null;

        if (keyStoreConfig == null)
            throw new KeyStoreConfigurationException(new Exception("The keyStoreConfiguration is null"));

        try {
            keyStore = keyStoreConfig.getKeyStore();
            kmFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmFactory.init(keyStore, keyStoreConfig.getPasswordAsCharArray());
        }
        catch (NoSuchAlgorithmException e) {
            LOGGER.error("The specified algorithm is not available from the specified provider");
            throw new KeyStoreConfigurationException(e);
        } catch (KeyStoreException e) {
            LOGGER.error("Failed to initialize the TrustManagerFactory");
            throw new KeyStoreConfigurationException(e);
        } catch (StoreConfigurationException e) {
            throw new KeyStoreConfigurationException(e);
        } catch (UnrecoverableKeyException e) {
            LOGGER.error("The key cannot be recovered (e.g. the given password is wrong)");
            throw new KeyStoreConfigurationException(e);
        }

        return kmFactory;
    }

    public boolean equals(SSLConfiguration config) {
        if (config == null)
            return false;

        boolean keyStoreEquals = false;
        boolean trustStoreEquals = false;

        if (keyStoreConfig != null)
            keyStoreEquals = keyStoreConfig.equals(config.keyStoreConfig);
        else
            keyStoreEquals = keyStoreConfig == config.keyStoreConfig;

        if (trustStoreConfig != null)
            trustStoreEquals = trustStoreConfig.equals(config.trustStoreConfig);
        else
            trustStoreEquals = trustStoreConfig == config.trustStoreConfig;

        return keyStoreEquals && trustStoreEquals;
    }

    /**
     * Create an SSLConfiguration from a KeyStoreConfiguration and a TrustStoreConfiguration.
     * @param keyStoreConfig The KeyStoreConfiguration.
     * @param trustStoreConfig The TrustStoreConfiguration.
     * @return
     */
    @PluginFactory
    public static SSLConfiguration createSSLConfiguration(
            @PluginElement("keyStore") KeyStoreConfiguration keyStoreConfig,
            @PluginElement("trustStore") TrustStoreConfiguration trustStoreConfig) {
        return new SSLConfiguration(keyStoreConfig, trustStoreConfig);
    }
}
