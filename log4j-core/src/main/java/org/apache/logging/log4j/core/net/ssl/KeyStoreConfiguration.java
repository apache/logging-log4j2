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
import java.security.UnrecoverableKeyException;
import java.util.Arrays;
import java.util.Objects;
import javax.net.ssl.KeyManagerFactory;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * Configuration of the KeyStore
 */
@Plugin(name = "KeyStore", category = Core.CATEGORY_NAME, printObject = true)
public class KeyStoreConfiguration extends AbstractKeyStoreConfiguration {

    private final String keyManagerFactoryAlgorithm;

    /**
     *
     * @throws StoreConfigurationException Thrown if this instance cannot load the KeyStore.
     */
    public KeyStoreConfiguration(
            final String location,
            final PasswordProvider passwordProvider,
            final String keyStoreType,
            final String keyManagerFactoryAlgorithm)
            throws StoreConfigurationException {
        super(location, passwordProvider, keyStoreType);
        this.keyManagerFactoryAlgorithm = keyManagerFactoryAlgorithm == null
                ? KeyManagerFactory.getDefaultAlgorithm()
                : keyManagerFactoryAlgorithm;
    }

    /**
     *
     * @throws StoreConfigurationException Thrown if this instance cannot load the KeyStore.
     * @deprecated use {@link #KeyStoreConfiguration(String, PasswordProvider, String, String)} instead
     */
    @Deprecated
    public KeyStoreConfiguration(
            final String location,
            final char[] password,
            final String keyStoreType,
            final String keyManagerFactoryAlgorithm)
            throws StoreConfigurationException {
        this(location, new MemoryPasswordProvider(password), keyStoreType, keyManagerFactoryAlgorithm);
        if (password != null) {
            Arrays.fill(password, '\0');
        }
    }

    /**
     *
     * @throws StoreConfigurationException Thrown if this instance cannot load the KeyStore.
     * @deprecated Use {@link #KeyStoreConfiguration(String, PasswordProvider, String, String)} instead
     */
    @Deprecated
    public KeyStoreConfiguration(
            final String location,
            final String password,
            final String keyStoreType,
            final String keyManagerFactoryAlgorithm)
            throws StoreConfigurationException {
        this(
                location,
                new MemoryPasswordProvider(password == null ? null : password.toCharArray()),
                keyStoreType,
                keyManagerFactoryAlgorithm);
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
     * @param keyManagerFactoryAlgorithm
     *         The standard name of the requested algorithm. See the Java Secure Socket Extension Reference Guide for information about these names.
     * @return a new KeyStoreConfiguration
     * @throws StoreConfigurationException Thrown if this call cannot load the KeyStore.
     */
    @PluginFactory
    public static KeyStoreConfiguration createKeyStoreConfiguration(
            // @formatter:off
            @PluginAttribute("location") final String location,
            @PluginAttribute(value = "password", sensitive = true) final char[] password,
            @PluginAttribute("passwordEnvironmentVariable") final String passwordEnvironmentVariable,
            @PluginAttribute("passwordFile") final String passwordFile,
            @PluginAttribute("type") final String keyStoreType,
            @PluginAttribute("keyManagerFactoryAlgorithm") final String keyManagerFactoryAlgorithm)
            throws StoreConfigurationException {
        // @formatter:on

        if (password != null && passwordEnvironmentVariable != null && passwordFile != null) {
            throw new StoreConfigurationException(
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
            return new KeyStoreConfiguration(location, provider, keyStoreType, keyManagerFactoryAlgorithm);
        } catch (final Exception ex) {
            throw new StoreConfigurationException("Could not configure KeyStore", ex);
        }
    }

    /**
     * @deprecated use {@link #createKeyStoreConfiguration(String, char[], String, String, String, String)}
     */
    @Deprecated
    public static KeyStoreConfiguration createKeyStoreConfiguration(
            // @formatter:off
            final String location,
            final char[] password,
            final String keyStoreType,
            final String keyManagerFactoryAlgorithm)
            throws StoreConfigurationException {
        // @formatter:on
        return createKeyStoreConfiguration(location, password, null, null, keyStoreType, keyManagerFactoryAlgorithm);
    }

    /**
     * Creates a KeyStoreConfiguration.
     *
     * @param location The location of the KeyStore, a file path, URL or resource.
     * @param password The password to access the KeyStore.
     * @param keyStoreType The KeyStore type, null defaults to {@code "JKS"}.
     * @param keyManagerFactoryAlgorithm The standard name of the requested algorithm. See the Java Secure Socket
     * Extension Reference Guide for information about these names.
     * @return a new KeyStoreConfiguration
     * @throws StoreConfigurationException Thrown if this call cannot load the KeyStore.
     * @deprecated Use createKeyStoreConfiguration(String, char[], String, String)
     */
    @Deprecated
    public static KeyStoreConfiguration createKeyStoreConfiguration(
            // @formatter:off
            final String location,
            final String password,
            final String keyStoreType,
            final String keyManagerFactoryAlgorithm)
            throws StoreConfigurationException {
        // @formatter:on
        return createKeyStoreConfiguration(
                location, (password == null ? null : password.toCharArray()), keyStoreType, keyManagerFactoryAlgorithm);
    }

    public KeyManagerFactory initKeyManagerFactory()
            throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
        final KeyManagerFactory kmFactory = KeyManagerFactory.getInstance(this.keyManagerFactoryAlgorithm);
        final char[] password = this.getPasswordAsCharArray();
        try {
            kmFactory.init(this.getKeyStore(), password);
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
        return kmFactory;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((keyManagerFactoryAlgorithm == null) ? 0 : keyManagerFactoryAlgorithm.hashCode());
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
        final KeyStoreConfiguration other = (KeyStoreConfiguration) obj;
        if (!Objects.equals(keyManagerFactoryAlgorithm, other.keyManagerFactoryAlgorithm)) {
            return false;
        }
        return true;
    }

    public String getKeyManagerFactoryAlgorithm() {
        return keyManagerFactoryAlgorithm;
    }
}
