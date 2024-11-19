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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.KeyStore;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;

// Suppresses `StatusLogger` output, unless there is a failure
@UsingStatusListener
class TrustStoreConfigurationTest {
    @SuppressWarnings("deprecation")
    @Test
    void loadEmptyConfigurationDeprecated() {
        assertThrows(
                StoreConfigurationException.class,
                () -> new TrustStoreConfiguration(null, SslKeyStoreConstants.NULL_PWD, null, null));
    }

    @Test
    void loadEmptyConfiguration() {
        assertThrows(
                StoreConfigurationException.class,
                () -> new TrustStoreConfiguration(
                        null, new MemoryPasswordProvider(SslKeyStoreConstants.NULL_PWD), null, null));
    }

    @Test
    void loadConfigurationDeprecated() throws StoreConfigurationException {
        @SuppressWarnings("deprecation")
        final TrustStoreConfiguration ksc = new TrustStoreConfiguration(
                SslKeyStoreConstants.TRUSTSTORE_LOCATION, SslKeyStoreConstants.TRUSTSTORE_PWD(), null, null);
        final KeyStore ks = ksc.getKeyStore();
        assertNotNull(ks);
    }

    @Test
    void loadConfiguration() throws StoreConfigurationException {
        final TrustStoreConfiguration ksc = new TrustStoreConfiguration(
                SslKeyStoreConstants.TRUSTSTORE_LOCATION,
                new MemoryPasswordProvider(SslKeyStoreConstants.TRUSTSTORE_PWD()),
                null,
                null);
        final KeyStore ks = ksc.getKeyStore();
        assertNotNull(ks);
    }

    @Test
    void returnTheSameKeyStoreAfterMultipleLoadsDeprecated() throws StoreConfigurationException {
        @SuppressWarnings("deprecation")
        final TrustStoreConfiguration ksc = new TrustStoreConfiguration(
                SslKeyStoreConstants.TRUSTSTORE_LOCATION, SslKeyStoreConstants.TRUSTSTORE_PWD(), null, null);
        final KeyStore ks = ksc.getKeyStore();
        final KeyStore ks2 = ksc.getKeyStore();
        assertSame(ks, ks2);
    }

    @Test
    void returnTheSameKeyStoreAfterMultipleLoads() throws StoreConfigurationException {
        final TrustStoreConfiguration ksc = new TrustStoreConfiguration(
                SslKeyStoreConstants.TRUSTSTORE_LOCATION,
                new MemoryPasswordProvider(SslKeyStoreConstants.TRUSTSTORE_PWD()),
                null,
                null);
        final KeyStore ks = ksc.getKeyStore();
        final KeyStore ks2 = ksc.getKeyStore();
        assertSame(ks, ks2);
    }

    @SuppressWarnings("deprecation")
    @Test
    void wrongPasswordDeprecated() {
        assertThrows(
                StoreConfigurationException.class,
                () -> new TrustStoreConfiguration(
                        SslKeyStoreConstants.TRUSTSTORE_LOCATION, "wrongPassword!".toCharArray(), null, null));
    }

    @Test
    void wrongPassword() {
        assertThrows(
                StoreConfigurationException.class,
                () -> new TrustStoreConfiguration(
                        SslKeyStoreConstants.TRUSTSTORE_LOCATION,
                        new MemoryPasswordProvider("wrongPassword!".toCharArray()),
                        null,
                        null));
    }
}
