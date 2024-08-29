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

    private final String protocol;

    private final boolean verifyHostName;

    private final KeyStoreConfiguration keyStoreConfig;

    private final TrustStoreConfiguration trustStoreConfig;

    private final transient SSLContext sslContext;

    private SslConfiguration(
            final String protocol,
            final boolean verifyHostName,
            final KeyStoreConfiguration keyStoreConfig,
            final TrustStoreConfiguration trustStoreConfig) {
        this.keyStoreConfig = keyStoreConfig;
        this.trustStoreConfig = trustStoreConfig;
        this.protocol = protocol == null ? SslConfigurationDefaults.PROTOCOL : protocol;
        this.verifyHostName = verifyHostName;
        this.sslContext = createSslContextWithFallbacks(protocol, keyStoreConfig, trustStoreConfig);
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

    /**
     * Gets the SSL socket factory of the configured SSL context.
     *
     * @return the SSL socket factory of the configured SSL context
     * @deprecated Use {@link SSLContext#getSocketFactory()} on {@link #getSslContext()}
     */
    @Deprecated
    public SSLSocketFactory getSslSocketFactory() {
        return sslContext.getSocketFactory();
    }

    /**
     * Gets the SSL server socket factory of the configured SSL context.
     *
     * @return the SSL server socket factory of the configured SSL context
     * @deprecated Use {@link SSLContext#getServerSocketFactory()} on {@link #getSslContext()}
     */
    @Deprecated
    public SSLServerSocketFactory getSslServerSocketFactory() {
        return sslContext.getServerSocketFactory();
    }

    private static SSLContext createSslContextWithFallbacks(
            final String protocol,
            final KeyStoreConfiguration keyStoreConfig,
            final TrustStoreConfiguration trustStoreConfig) {
        SSLContext context;
        try {
            context = createSslContext(protocol, keyStoreConfig, trustStoreConfig);
            LOGGER.debug("Creating SSLContext with the given parameters");
        } catch (final TrustStoreConfigurationException e) {
            context = createSslContextWithTrustStoreFailure(protocol, trustStoreConfig);
        } catch (final KeyStoreConfigurationException e) {
            context = createSslContextWithKeyStoreFailure(protocol, keyStoreConfig);
        }
        return context;
    }

    private static SSLContext createSslContextWithTrustStoreFailure(
            final String protocol, final TrustStoreConfiguration trustStoreConfig) {
        SSLContext context;
        try {
            context = createSslContextWithDefaultTrustManagerFactory(protocol, trustStoreConfig);
            LOGGER.debug("Creating SSLContext with default truststore");
        } catch (final KeyStoreConfigurationException e) {
            context = createDefaultSslContext();
            LOGGER.debug("Creating SSLContext with default configuration");
        }
        return context;
    }

    private static SSLContext createSslContextWithKeyStoreFailure(
            final String protocol, final KeyStoreConfiguration keyStoreConfig) {
        SSLContext context;
        try {
            context = createSslContextWithDefaultKeyManagerFactory(protocol, keyStoreConfig);
            LOGGER.debug("Creating SSLContext with default keystore");
        } catch (final TrustStoreConfigurationException e) {
            context = createDefaultSslContext();
            LOGGER.debug("Creating SSLContext with default configuration");
        }
        return context;
    }

    private static SSLContext createSslContextWithDefaultKeyManagerFactory(
            final String protocol, final KeyStoreConfiguration keyStoreConfig) throws TrustStoreConfigurationException {
        try {
            return createSslContext(protocol, keyStoreConfig, null);
        } catch (final KeyStoreConfigurationException dummy) {
            LOGGER.debug("Exception occurred while using default keystore. This should be a BUG");
            return null;
        }
    }

    private static SSLContext createSslContextWithDefaultTrustManagerFactory(
            final String protocol, final TrustStoreConfiguration trustStoreConfig)
            throws KeyStoreConfigurationException {
        try {
            return createSslContext(protocol, null, trustStoreConfig);
        } catch (final TrustStoreConfigurationException dummy) {
            LOGGER.debug("Exception occurred while using default truststore. This should be a BUG");
            return null;
        }
    }

    private static SSLContext createDefaultSslContext() {
        try {
            return SSLContext.getDefault();
        } catch (final NoSuchAlgorithmException e) {
            LOGGER.error("Failed to create an SSLContext with default configuration", e);
            return null;
        }
    }

    private static SSLContext createSslContext(
            final String protocol,
            final KeyStoreConfiguration keyStoreConfig,
            final TrustStoreConfiguration trustStoreConfig)
            throws KeyStoreConfigurationException, TrustStoreConfigurationException {
        try {
            KeyManager[] kManagers = null;
            TrustManager[] tManagers = null;

            final SSLContext newSslContext = SSLContext.getInstance(protocol);
            if (keyStoreConfig != null) {
                final KeyManagerFactory kmFactory = loadKeyManagerFactory(keyStoreConfig);
                kManagers = kmFactory.getKeyManagers();
            }
            if (trustStoreConfig != null) {
                final TrustManagerFactory tmFactory = loadTrustManagerFactory(trustStoreConfig);
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

    private static TrustManagerFactory loadTrustManagerFactory(final TrustStoreConfiguration trustStoreConfig)
            throws TrustStoreConfigurationException {
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

    private static KeyManagerFactory loadKeyManagerFactory(final KeyStoreConfiguration keyStoreConfig)
            throws KeyStoreConfigurationException {
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
     * @param protocol The protocol, see <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#SSLContext">SSLContext Algorithms</a>
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
        return new SslConfiguration(protocol, false, keyStoreConfig, trustStoreConfig);
    }

    /**
     * Creates an SslConfiguration from a KeyStoreConfiguration and a TrustStoreConfiguration.
     *
     * @param protocol The protocol, see <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#SSLContext">SSLContext Algorithms</a>
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
        return new SslConfiguration(protocol, verifyHostName, keyStoreConfig, trustStoreConfig);
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
        if (!Objects.equals(protocol, other.protocol)) {
            return false;
        }
        if (!Objects.equals(verifyHostName, other.verifyHostName)) {
            return false;
        }
        if (!Objects.equals(keyStoreConfig, other.keyStoreConfig)) {
            return false;
        }
        if (!Objects.equals(trustStoreConfig, other.trustStoreConfig)) {
            return false;
        }
        return true;
    }

    public String getProtocol() {
        return protocol;
    }

    public boolean isVerifyHostName() {
        return verifyHostName;
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
}
