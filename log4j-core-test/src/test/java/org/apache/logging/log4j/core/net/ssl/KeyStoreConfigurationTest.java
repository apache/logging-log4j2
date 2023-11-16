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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.KeyStore;
import java.util.Collections;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junitpioneer.jupiter.SetSystemProperty;

@SetSystemProperty(key = "sun.security.mscapi.keyStoreCompatibilityMode", value = "false")
public class KeyStoreConfigurationTest {

    @SuppressWarnings("deprecation")
    @Test
    public void loadEmptyConfigurationDeprecated() {
        assertThrows(
                StoreConfigurationException.class,
                () -> new KeyStoreConfiguration(null, TestConstants.NULL_PWD, null, null));
    }

    @Test
    public void loadEmptyConfiguration() {
        assertThrows(
                StoreConfigurationException.class,
                () -> new KeyStoreConfiguration(null, new MemoryPasswordProvider(TestConstants.NULL_PWD), null, null));
    }

    @Test
    public void loadNotEmptyConfigurationDeprecated() throws StoreConfigurationException {
        @SuppressWarnings("deprecation")
        final KeyStoreConfiguration ksc = new KeyStoreConfiguration(
                TestConstants.KEYSTORE_FILE, TestConstants.KEYSTORE_PWD(), TestConstants.KEYSTORE_TYPE, null);
        final KeyStore ks = ksc.getKeyStore();
        assertNotNull(ks);
        checkKeystoreConfiguration(ksc);
    }

    static Stream<Arguments> configurations() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        builder.add(Arguments.of(
                        TestConstants.KEYSTORE_FILE,
                        (Supplier<char[]>) TestConstants::KEYSTORE_PWD,
                        TestConstants.KEYSTORE_TYPE))
                .add(Arguments.of(
                        TestConstants.KEYSTORE_PKCS12_FILE,
                        (Supplier<char[]>) TestConstants::KEYSTORE_PKCS12_PWD,
                        TestConstants.KEYSTORE_PKCS12_TYPE))
                .add(Arguments.of(
                        TestConstants.KEYSTORE_EMPTYPASS_FILE,
                        (Supplier<char[]>) TestConstants::KEYSTORE_EMPTYPASS_PWD,
                        TestConstants.KEYSTORE_EMPTYPASS_TYPE));
        if (OS.WINDOWS.isCurrentOs()) {
            builder.add(Arguments.of(null, (Supplier<char[]>) () -> null, "Windows-MY"))
                    .add(Arguments.of(null, (Supplier<char[]>) () -> null, "Windows-ROOT"));
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource("configurations")
    public void loadNotEmptyConfiguration(
            final String keystoreFile, final Supplier<char[]> password, final String keystoreType)
            throws StoreConfigurationException {
        final KeyStoreConfiguration ksc =
                new KeyStoreConfiguration(keystoreFile, new MemoryPasswordProvider(password.get()), keystoreType, null);
        final KeyStore ks = ksc.getKeyStore();
        assertNotNull(ks);
        checkKeystoreConfiguration(ksc);
    }

    @Test
    public void returnTheSameKeyStoreAfterMultipleLoadsDeprecated() throws StoreConfigurationException {
        @SuppressWarnings("deprecation")
        final KeyStoreConfiguration ksc = new KeyStoreConfiguration(
                TestConstants.KEYSTORE_FILE, TestConstants.KEYSTORE_PWD(), TestConstants.KEYSTORE_TYPE, null);
        final KeyStore ks = ksc.getKeyStore();
        final KeyStore ks2 = ksc.getKeyStore();
        assertSame(ks, ks2);
    }

    @Test
    public void returnTheSameKeyStoreAfterMultipleLoads() throws StoreConfigurationException {
        final KeyStoreConfiguration ksc = new KeyStoreConfiguration(
                TestConstants.KEYSTORE_FILE,
                new MemoryPasswordProvider(TestConstants.KEYSTORE_PWD()),
                TestConstants.KEYSTORE_TYPE,
                null);
        final KeyStore ks = ksc.getKeyStore();
        final KeyStore ks2 = ksc.getKeyStore();
        assertSame(ks, ks2);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void wrongPasswordDeprecated() {
        assertThrows(
                StoreConfigurationException.class,
                () -> new KeyStoreConfiguration(TestConstants.KEYSTORE_FILE, "wrongPassword!", null, null));
    }

    static Stream<Arguments> wrongConfigurations() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        builder.add(Arguments.of(
                        TestConstants.KEYSTORE_FILE,
                        (Supplier<char[]>) TestConstants::KEYSTORE_EMPTYPASS_PWD,
                        TestConstants.KEYSTORE_TYPE))
                .add(Arguments.of(
                        TestConstants.KEYSTORE_FILE,
                        (Supplier<char[]>) () -> "wrongPassword!".toCharArray(),
                        TestConstants.KEYSTORE_TYPE))
                .add(Arguments.of(
                        TestConstants.KEYSTORE_PKCS12_FILE,
                        (Supplier<char[]>) TestConstants::KEYSTORE_EMPTYPASS_PWD,
                        TestConstants.KEYSTORE_PKCS12_TYPE))
                .add(Arguments.of(
                        TestConstants.KEYSTORE_PKCS12_FILE,
                        (Supplier<char[]>) TestConstants::KEYSTORE_EMPTYPASS_PWD,
                        TestConstants.KEYSTORE_PKCS12_TYPE));
        if (OS.WINDOWS.isCurrentOs()) {
            builder.add(Arguments.of(null, (Supplier<char[]>) () -> new char[0], "Windows-MY"))
                    .add(Arguments.of(null, (Supplier<char[]>) () -> new char[0], "Windows-ROOT"));
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource("wrongConfigurations")
    public void wrongPassword(final String keystoreFile, final Supplier<char[]> password, final String keystoreType) {
        assertThrows(
                StoreConfigurationException.class,
                () -> new KeyStoreConfiguration(
                        keystoreFile, new MemoryPasswordProvider(password.get()), keystoreType, null));
    }

    static void checkKeystoreConfiguration(final AbstractKeyStoreConfiguration config) {
        // Not all keystores throw immediately if the password is wrong
        assertDoesNotThrow(() -> {
            final KeyStore ks = config.load();
            for (final String alias : Collections.list(ks.aliases())) {
                if (ks.isCertificateEntry(alias)) {
                    ks.getCertificate(alias);
                }
                if (ks.isKeyEntry(alias)) {
                    ks.getKey(alias, config.getPasswordAsCharArray());
                }
            }
        });
    }
}
