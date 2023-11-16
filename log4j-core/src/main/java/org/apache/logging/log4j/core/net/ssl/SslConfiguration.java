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

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Objects;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;

/**
 *  SSL Configuration
 */
@Plugin(name = "Ssl", category = Core.CATEGORY_NAME, printObject = true)
public class SslConfiguration {
    private static final StatusLogger LOGGER = StatusLogger.getLogger();
    private final KeyStoreConfiguration keyStoreConfig;
    private final TrustStoreConfiguration trustStoreConfig;
    private final SSLContext sslContext;
    private final String protocol;
    private final boolean verifyHostName;

    private SslConfiguration(
            final String protocol,
            final KeyStoreConfiguration keyStoreConfig,
            final TrustStoreConfiguration trustStoreConfig,
            final boolean verifyHostName) {
        this.keyStoreConfig = keyStoreConfig;
        this.trustStoreConfig = trustStoreConfig;
        this.protocol = protocol == null ? SslConfigurationDefaults.PROTOCOL : protocol;
        this.sslContext = this.createSslContext();
        this.verifyHostName = verifyHostName;
    }

    /**
     * Clears the secret fields in this object but still allow it to operate normally.
     */
    public void clearSecrets() {
        if (this.keyStoreConfig != null) {
            this.keyStoreConfig.clearSecrets();
        }
        if (this.trustStoreConfig != null) {
            this.trustStoreConfig.clearSecrets();
        }
    }

    public SSLSocketFactory getSslSocketFactory() {
        return sslContext.getSocketFactory();
    }

    public SSLServerSocketFactory getSslServerSocketFactory() {
        return sslContext.getServerSocketFactory();
    }

    private SSLContext createSslContext() {
        SSLContext context = null;

        try {
            context = createSslContextBasedOnConfiguration();
            LOGGER.debug("Creating SSLContext with the given parameters");
        } catch (final TrustStoreConfigurationException e) {
            context = createSslContextWithTrustStoreFailure();
        } catch (final KeyStoreConfigurationException e) {
            context = createSslContextWithKeyStoreFailure();
        }
        return context;
    }

    private SSLContext createSslContextWithTrustStoreFailure() {
        SSLContext context;

        try {
            context = createSslContextWithDefaultTrustManagerFactory();
            LOGGER.debug("Creating SSLContext with default truststore");
        } catch (final KeyStoreConfigurationException e) {
            context = createDefaultSslContext();
            LOGGER.debug("Creating SSLContext with default configuration");
        }
        return context;
    }

    private SSLContext createSslContextWithKeyStoreFailure() {
        SSLContext context;

        try {
            context = createSslContextWithDefaultKeyManagerFactory();
            LOGGER.debug("Creating SSLContext with default keystore");
        } catch (final TrustStoreConfigurationException e) {
            context = createDefaultSslContext();
            LOGGER.debug("Creating SSLContext with default configuration");
        }
        return context;
    }

    private SSLContext createSslContextBasedOnConfiguration()
            throws KeyStoreConfigurationException, TrustStoreConfigurationException {
        return createSslContext(false, false);
    }

    private SSLContext createSslContextWithDefaultKeyManagerFactory() throws TrustStoreConfigurationException {
        try {
            return createSslContext(true, false);
        } catch (final KeyStoreConfigurationException dummy) {
            LOGGER.debug("Exception occurred while using default keystore. This should be a BUG");
            return null;
        }
    }

    private SSLContext createSslContextWithDefaultTrustManagerFactory() throws KeyStoreConfigurationException {
        try {
            return createSslContext(false, true);
        } catch (final TrustStoreConfigurationException dummy) {
            LOGGER.debug("Exception occurred while using default truststore. This should be a BUG");
            return null;
        }
    }

    private SSLContext createDefaultSslContext() {
        try {
            return SSLContext.getDefault();
        } catch (final NoSuchAlgorithmException e) {
            LOGGER.error("Failed to create an SSLContext with default configuration", e);
            return null;
        }
    }

    private SSLContext createSslContext(
            final boolean loadDefaultKeyManagerFactory, final boolean loadDefaultTrustManagerFactory)
            throws KeyStoreConfigurationException, TrustStoreConfigurationException {
        try {
            KeyManager[] kManagers = null;
            TrustManager[] tManagers = null;

            final SSLContext newSslContext = SSLContext.getInstance(this.protocol);
            if (!loadDefaultKeyManagerFactory) {
                final KeyManagerFactory kmFactory = loadKeyManagerFactory();
                kManagers = kmFactory.getKeyManagers();
            }
            if (!loadDefaultTrustManagerFactory) {
                final TrustManagerFactory tmFactory = loadTrustManagerFactory();
                tManagers = tmFactory.getTrustManagers();
            }

            newSslContext.init(kManagers, tManagers, null);
            return newSslContext;
        } catch (final NoSuchAlgorithmException e) {
            LOGGER.error("No Provider supports a TrustManagerFactorySpi implementation for the specified protocol", e);
            throw new TrustStoreConfigurationException(e);
        } catch (final KeyManagementException e) {
            LOGGER.error("Failed to initialize the SSLContext", e);
            throw new KeyStoreConfigurationException(e);
        }
    }

    private TrustManagerFactory loadTrustManagerFactory() throws TrustStoreConfigurationException {
        if (trustStoreConfig == null) {
            throw new TrustStoreConfigurationException(new Exception("The trustStoreConfiguration is null"));
        }

        try {
            return trustStoreConfig.initTrustManagerFactory();
        } catch (final NoSuchAlgorithmException e) {
            LOGGER.error("The specified algorithm is not available from the specified provider", e);
            throw new TrustStoreConfigurationException(e);
        } catch (final KeyStoreException e) {
            LOGGER.error("Failed to initialize the TrustManagerFactory", e);
            throw new TrustStoreConfigurationException(e);
        }
    }

    private KeyManagerFactory loadKeyManagerFactory() throws KeyStoreConfigurationException {
        if (keyStoreConfig == null) {
            throw new KeyStoreConfigurationException(new Exception("The keyStoreConfiguration is null"));
        }

        try {
            return keyStoreConfig.initKeyManagerFactory();
        } catch (final NoSuchAlgorithmException e) {
            LOGGER.error("The specified algorithm is not available from the specified provider", e);
            throw new KeyStoreConfigurationException(e);
        } catch (final KeyStoreException e) {
            LOGGER.error("Failed to initialize the TrustManagerFactory", e);
            throw new KeyStoreConfigurationException(e);
        } catch (final UnrecoverableKeyException e) {
            LOGGER.error("The key cannot be recovered (e.g. the given password is wrong)", e);
            throw new KeyStoreConfigurationException(e);
        }
    }

    /**
     * Creates an SslConfiguration from a KeyStoreConfiguration and a TrustStoreConfiguration.
     *
     * @param protocol The protocol, see http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#SSLContext
     * @param keyStoreConfig The KeyStoreConfiguration.
     * @param trustStoreConfig The TrustStoreConfiguration.
     * @return a new SslConfiguration
     */
    @PluginFactory
    public static SslConfiguration createSSLConfiguration(
            // @formatter:off
            @PluginAttribute("protocol") final String protocol,
            @PluginElement("KeyStore") final KeyStoreConfiguration keyStoreConfig,
            @PluginElement("TrustStore") final TrustStoreConfiguration trustStoreConfig) {
        // @formatter:on
        return new SslConfiguration(protocol, keyStoreConfig, trustStoreConfig, false);
    }

    /**
     * Creates an SslConfiguration from a KeyStoreConfiguration and a TrustStoreConfiguration.
     *
     * @param protocol The protocol, see http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#SSLContext
     * @param keyStoreConfig The KeyStoreConfiguration.
     * @param trustStoreConfig The TrustStoreConfiguration.
     * @param verifyHostName whether or not to perform host name verification
     * @return a new SslConfiguration
     * @since 2.12
     */
    public static SslConfiguration createSSLConfiguration(
            // @formatter:off
            @PluginAttribute("protocol") final String protocol,
            @PluginElement("KeyStore") final KeyStoreConfiguration keyStoreConfig,
            @PluginElement("TrustStore") final TrustStoreConfiguration trustStoreConfig,
            @PluginAttribute("verifyHostName") final boolean verifyHostName) {
        // @formatter:on
        return new SslConfiguration(protocol, keyStoreConfig, trustStoreConfig, verifyHostName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyStoreConfig, protocol, sslContext, trustStoreConfig);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SslConfiguration other = (SslConfiguration) obj;
        if (!Objects.equals(keyStoreConfig, other.keyStoreConfig)) {
            return false;
        }
        if (!Objects.equals(protocol, other.protocol)) {
            return false;
        }
        if (!Objects.equals(sslContext, other.sslContext)) {
            return false;
        }
        if (!Objects.equals(trustStoreConfig, other.trustStoreConfig)) {
            return false;
        }
        return true;
    }

    public KeyStoreConfiguration getKeyStoreConfig() {
        return keyStoreConfig;
    }

    public TrustStoreConfiguration getTrustStoreConfig() {
        return trustStoreConfig;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    public String getProtocol() {
        return protocol;
    }

    public boolean isVerifyHostName() {
        return verifyHostName;
    }
}
