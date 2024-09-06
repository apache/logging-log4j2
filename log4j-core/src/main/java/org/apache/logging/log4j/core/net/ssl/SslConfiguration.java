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

import java.security.NoSuchAlgorithmException;
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
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

/**
 *  SSL Configuration
 */
@NullMarked
@Plugin(name = "Ssl", category = Core.CATEGORY_NAME, printObject = true)
public class SslConfiguration {

    private static final StatusLogger LOGGER = StatusLogger.getLogger();

    private final String protocol;

    private final boolean verifyHostName;

    @Nullable
    private final KeyStoreConfiguration keyStoreConfig;

    @Nullable
    private final TrustStoreConfiguration trustStoreConfig;

    private final transient SSLContext sslContext;

    private SslConfiguration(
            @Nullable final String protocol,
            final boolean verifyHostName,
            @Nullable final KeyStoreConfiguration keyStoreConfig,
            @Nullable final TrustStoreConfiguration trustStoreConfig) {
        this.keyStoreConfig = keyStoreConfig;
        this.trustStoreConfig = trustStoreConfig;
        final String effectiveProtocol = protocol == null ? SslConfigurationDefaults.PROTOCOL : protocol;
        this.protocol = effectiveProtocol;
        this.verifyHostName = verifyHostName;
        this.sslContext = createSslContext(effectiveProtocol, keyStoreConfig, trustStoreConfig);
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

    private static SSLContext createDefaultSslContext(final String protocol) {
        try {
            return SSLContext.getDefault();
        } catch (final NoSuchAlgorithmException defaultContextError) {
            LOGGER.error(
                    "Failed to create an `SSLContext` using the default configuration, falling back to creating an empty one",
                    defaultContextError);
            try {
                final SSLContext emptyContext = SSLContext.getInstance(protocol);
                emptyContext.init(new KeyManager[0], new TrustManager[0], null);
                return emptyContext;
            } catch (final Exception emptyContextError) {
                LOGGER.error("Failed to create an empty `SSLContext`", emptyContextError);
                return null;
            }
        }
    }

    private static SSLContext createSslContext(
            final String protocol,
            @Nullable final KeyStoreConfiguration keyStoreConfig,
            @Nullable final TrustStoreConfiguration trustStoreConfig) {
        try {
            final SSLContext sslContext = SSLContext.getInstance(protocol);
            final KeyManager[] keyManagers = loadKeyManagers(keyStoreConfig);
            final TrustManager[] trustManagers = loadTrustManagers(trustStoreConfig);
            sslContext.init(keyManagers, trustManagers, null);
            return sslContext;
        } catch (final Exception error) {
            LOGGER.error(
                    "Failed to create an `SSLContext` using the provided configuration, falling back to a default instance",
                    error);
            return createDefaultSslContext(protocol);
        }
    }

    private static KeyManager[] loadKeyManagers(@Nullable final KeyStoreConfiguration config) throws Exception {
        if (config == null) {
            return new KeyManager[0];
        }
        final KeyManagerFactory factory = KeyManagerFactory.getInstance(config.getKeyManagerFactoryAlgorithm());
        final char[] password = config.getPasswordAsCharArray();
        try {
            factory.init(config.getKeyStore(), password);
        } finally {
            config.clearSecrets();
        }
        return factory.getKeyManagers();
    }

    private static TrustManager[] loadTrustManagers(@Nullable final TrustStoreConfiguration config) throws Exception {
        if (config == null) {
            return new TrustManager[0];
        }
        final TrustManagerFactory factory = TrustManagerFactory.getInstance(config.getTrustManagerFactoryAlgorithm());
        factory.init(config.getKeyStore());
        return factory.getTrustManagers();
    }

    /**
     * Creates an SslConfiguration from a KeyStoreConfiguration and a TrustStoreConfiguration.
     *
     * @param protocol         The protocol, see <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#SSLContext">SSLContext Algorithms</a>
     * @param keyStoreConfig   The KeyStoreConfiguration.
     * @param trustStoreConfig The TrustStoreConfiguration.
     * @return a new SslConfiguration
     */
    @NullUnmarked
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
    @NullUnmarked
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
