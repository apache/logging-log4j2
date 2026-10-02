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

import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Objects;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
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

    @Nullable
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
    @Nullable
    public SSLSocketFactory getSslSocketFactory() {
        return sslContext != null ? sslContext.getSocketFactory() : null;
    }

    /**
     * Gets the SSL server socket factory of the configured SSL context.
     *
     * @return the SSL server socket factory of the configured SSL context
     * @deprecated Use {@link SSLContext#getServerSocketFactory()} on {@link #getSslContext()}
     */
    @Deprecated
    @Nullable
    public SSLServerSocketFactory getSslServerSocketFactory() {
        return sslContext != null ? sslContext.getServerSocketFactory() : null;
    }

    @Nullable
    private static SSLContext createSslContext(
            final String protocol,
            @Nullable final KeyStoreConfiguration keyStoreConfig,
            @Nullable final TrustStoreConfiguration trustStoreConfig) {
        if (hasLoadFailure(keyStoreConfig) || hasLoadFailure(trustStoreConfig)) {
            return createRejectingSslContext();
        }
        try {
            final SSLContext sslContext = SSLContext.getInstance(protocol);
            @Nullable final KeyManager[] keyManagers = loadKeyManagers(keyStoreConfig);
            @Nullable final TrustManager[] trustManagers = loadTrustManagers(trustStoreConfig);
            sslContext.init(keyManagers, trustManagers, null);
            return sslContext;
        } catch (final Exception error) {
            LOGGER.error(
                    "Failed to create an `SSLContext` using the provided configuration, all TLS connections using this configuration will be rejected",
                    error);
            return createRejectingSslContext();
        }
    }

    private static boolean hasLoadFailure(@Nullable final AbstractKeyStoreConfiguration storeConfig) {
        if (storeConfig == null || storeConfig.getLoadFailure() == null) {
            return false;
        }
        LOGGER.error(
                "Failed to load the store located at `{}`, all TLS connections using this configuration will be rejected",
                storeConfig.getLocation(),
                storeConfig.getLoadFailure());
        return true;
    }

    @Nullable
    private static SSLContext createRejectingSslContext() {
        try {
            final SSLContext sslContext = SSLContext.getInstance(SslConfigurationDefaults.PROTOCOL);
            sslContext.init(new KeyManager[0], new TrustManager[] {RejectingTrustManager.INSTANCE}, null);
            return sslContext;
        } catch (final GeneralSecurityException error) {
            LOGGER.error("Failed to create an `SSLContext` rejecting all connections", error);
            return null;
        }
    }

    @Nullable
    @NullUnmarked
    private static KeyManager[] loadKeyManagers(@Nullable final KeyStoreConfiguration config) throws Exception {
        if (config == null) {
            return null;
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

    @Nullable
    @NullUnmarked
    private static TrustManager[] loadTrustManagers(@Nullable final TrustStoreConfiguration config) throws Exception {
        if (config == null) {
            return null;
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
    public static SslConfiguration createSSLConfiguration(
            final String protocol,
            final KeyStoreConfiguration keyStoreConfig,
            final TrustStoreConfiguration trustStoreConfig) {
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
    @PluginFactory
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

    @Nullable
    public KeyStoreConfiguration getKeyStoreConfig() {
        return keyStoreConfig;
    }

    @Nullable
    public TrustStoreConfiguration getTrustStoreConfig() {
        return trustStoreConfig;
    }

    @Nullable
    public SSLContext getSslContext() {
        return sslContext;
    }

    static final class RejectingTrustManager implements X509TrustManager {

        static final RejectingTrustManager INSTANCE = new RejectingTrustManager();

        private static final String MESSAGE = "The `Ssl` configuration is invalid, see the status logger for the cause";

        @Override
        public void checkClientTrusted(final X509Certificate[] chain, final String authType)
                throws CertificateException {
            throw new CertificateException(MESSAGE);
        }

        @Override
        public void checkServerTrusted(final X509Certificate[] chain, final String authType)
                throws CertificateException {
            throw new CertificateException(MESSAGE);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
