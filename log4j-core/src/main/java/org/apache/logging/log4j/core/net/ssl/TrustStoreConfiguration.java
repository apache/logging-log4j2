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

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import javax.net.ssl.TrustManagerFactory;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * Configuration of the TrustStore
 */
@Plugin(name = "TrustStore", category = Core.CATEGORY_NAME, printObject = true)
public class TrustStoreConfiguration extends AbstractKeyStoreConfiguration {

    private final String trustManagerFactoryAlgorithm;

    public TrustStoreConfiguration(
            final String location,
            final PasswordProvider passwordProvider,
            final String keyStoreType,
            final String trustManagerFactoryAlgorithm)
            throws StoreConfigurationException {
        super(location, passwordProvider, keyStoreType);
        this.trustManagerFactoryAlgorithm = trustManagerFactoryAlgorithm == null
                ? TrustManagerFactory.getDefaultAlgorithm()
                : trustManagerFactoryAlgorithm;
    }

    /**
     * @deprecated Use {@link #TrustStoreConfiguration(String, PasswordProvider, String, String)} instead
     */
    @Deprecated
    public TrustStoreConfiguration(
            final String location,
            final char[] password,
            final String keyStoreType,
            final String trustManagerFactoryAlgorithm)
            throws StoreConfigurationException {
        this(location, new MemoryPasswordProvider(password), keyStoreType, trustManagerFactoryAlgorithm);
        if (password != null) {
            Arrays.fill(password, '\0');
        }
    }

    /**
     * @deprecated Use {@link #TrustStoreConfiguration(String, PasswordProvider, String, String)} instead
     */
    @Deprecated
    public TrustStoreConfiguration(
            final String location,
            final String password,
            final String keyStoreType,
            final String trustManagerFactoryAlgorithm)
            throws StoreConfigurationException {
        this(
                location,
                new MemoryPasswordProvider(password == null ? null : password.toCharArray()),
                keyStoreType,
                trustManagerFactoryAlgorithm);
    }

    /**
     * Creates a KeyStoreConfiguration.
     *
     * @param location
     *        The location of the KeyStore, a file path, URL or resource.
     * @param password
     *        The password to access the KeyStore.
     * @param keyStoreType
     *        The KeyStore type, null defaults to {@code "JKS"}.
     * @param trustManagerFactoryAlgorithm
     *        The standard name of the requested trust management algorithm. See the Java Secure Socket Extension Reference Guide for information these names.
     * @return a new TrustStoreConfiguration
     * @throws StoreConfigurationException Thrown if this instance cannot load the KeyStore.
     */
    @PluginFactory
    public static TrustStoreConfiguration createKeyStoreConfiguration(
            // @formatter:off
            @PluginAttribute("location") final String location,
            @PluginAttribute(value = "password", sensitive = true) final char[] password,
            @PluginAttribute("passwordEnvironmentVariable") final String passwordEnvironmentVariable,
            @PluginAttribute("passwordFile") final String passwordFile,
            @PluginAttribute("type") final String keyStoreType,
            @PluginAttribute("trustManagerFactoryAlgorithm") final String trustManagerFactoryAlgorithm)
            throws StoreConfigurationException {
        // @formatter:on

        if (password != null && passwordEnvironmentVariable != null && passwordFile != null) {
            throw new IllegalStateException(
                    "You MUST set only one of 'password', 'passwordEnvironmentVariable' or 'passwordFile'.");
        }
        try {
            // @formatter:off
            final PasswordProvider provider = passwordFile != null
                    ? new FilePasswordProvider(passwordFile)
                    : passwordEnvironmentVariable != null
                            ? new EnvironmentPasswordProvider(passwordEnvironmentVariable)
                            // the default is memory char[] array, which may be null
                            : new MemoryPasswordProvider(password);
            // @formatter:on
            if (password != null) {
                Arrays.fill(password, '\0');
            }
            return new TrustStoreConfiguration(location, provider, keyStoreType, trustManagerFactoryAlgorithm);
        } catch (final Exception ex) {
            throw new StoreConfigurationException("Could not configure TrustStore", ex);
        }
    }

    /**
     * @deprecated Use {@link #createKeyStoreConfiguration(String, char[], String, String, String, String)}
     */
    @Deprecated
    public static TrustStoreConfiguration createKeyStoreConfiguration(
            // @formatter:off
            final String location,
            final char[] password,
            final String keyStoreType,
            final String trustManagerFactoryAlgorithm)
            throws StoreConfigurationException {
        // @formatter:on
        return createKeyStoreConfiguration(location, password, null, null, keyStoreType, trustManagerFactoryAlgorithm);
    }

    /**
     * Creates a KeyStoreConfiguration.
     *
     * @param location The location of the KeyStore, a file path, URL or resource.
     * @param password The password to access the KeyStore.
     * @param keyStoreType The KeyStore type, null defaults to {@code "JKS"}.
     * @param trustManagerFactoryAlgorithm The standard name of the requested trust management algorithm. See the Java
     * Secure Socket Extension Reference Guide for information these names.
     * @return a new TrustStoreConfiguration
     * @throws StoreConfigurationException Thrown if this instance cannot load the KeyStore.
     * @deprecated Use createKeyStoreConfiguration(String, char[], String, String)
     */
    @Deprecated
    public static TrustStoreConfiguration createKeyStoreConfiguration(
            // @formatter:off
            final String location,
            final String password,
            final String keyStoreType,
            final String trustManagerFactoryAlgorithm)
            throws StoreConfigurationException {
        // @formatter:on
        return createKeyStoreConfiguration(
                location,
                (password == null ? null : password.toCharArray()),
                null,
                null,
                keyStoreType,
                trustManagerFactoryAlgorithm);
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
        result =
                prime * result + ((trustManagerFactoryAlgorithm == null) ? 0 : trustManagerFactoryAlgorithm.hashCode());
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
        if (!Objects.equals(trustManagerFactoryAlgorithm, other.trustManagerFactoryAlgorithm)) {
            return false;
        }
        return true;
    }

    public String getTrustManagerFactoryAlgorithm() {
        return trustManagerFactoryAlgorithm;
    }
}
