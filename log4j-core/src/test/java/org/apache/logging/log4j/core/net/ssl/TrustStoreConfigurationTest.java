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

import org.junit.jupiter.api.Test;

import java.security.KeyStore;

import static org.junit.jupiter.api.Assertions.*;

public class TrustStoreConfigurationTest {
    @SuppressWarnings("deprecation")
    @Test
    public void loadEmptyConfigurationDeprecated() {
        assertThrows(StoreConfigurationException.class, () -> new TrustStoreConfiguration(null, TestConstants.NULL_PWD, null, null));
    }

    @Test
    public void loadEmptyConfiguration() {
        assertThrows(StoreConfigurationException.class, () -> new TrustStoreConfiguration(null, new MemoryPasswordProvider(TestConstants.NULL_PWD), null, null));
    }

    @Test
    public void loadConfigurationDeprecated() throws StoreConfigurationException {
        @SuppressWarnings("deprecation") final TrustStoreConfiguration ksc =
                new TrustStoreConfiguration(TestConstants.TRUSTSTORE_FILE, TestConstants.TRUSTSTORE_PWD(), null, null);
        final KeyStore ks = ksc.getKeyStore();
        assertNotNull(ks);
    }

    @Test
    public void loadConfiguration() throws StoreConfigurationException {
        final TrustStoreConfiguration ksc = new TrustStoreConfiguration(TestConstants.TRUSTSTORE_FILE, new MemoryPasswordProvider(TestConstants.TRUSTSTORE_PWD()), null, null);
        final KeyStore ks = ksc.getKeyStore();
        assertNotNull(ks);
    }

    @Test
    public void returnTheSameKeyStoreAfterMultipleLoadsDeprecated() throws StoreConfigurationException {
        @SuppressWarnings("deprecation") final TrustStoreConfiguration ksc =
                new TrustStoreConfiguration(TestConstants.TRUSTSTORE_FILE, TestConstants.TRUSTSTORE_PWD(), null, null);
        final KeyStore ks = ksc.getKeyStore();
        final KeyStore ks2 = ksc.getKeyStore();
        assertSame(ks, ks2);
    }

    @Test
    public void returnTheSameKeyStoreAfterMultipleLoads() throws StoreConfigurationException {
        final TrustStoreConfiguration ksc = new TrustStoreConfiguration(TestConstants.TRUSTSTORE_FILE, new MemoryPasswordProvider(TestConstants.TRUSTSTORE_PWD()), null, null);
        final KeyStore ks = ksc.getKeyStore();
        final KeyStore ks2 = ksc.getKeyStore();
        assertSame(ks, ks2);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void wrongPasswordDeprecated() {
        assertThrows(StoreConfigurationException.class, () -> new TrustStoreConfiguration(TestConstants.TRUSTSTORE_FILE, "wrongPassword!".toCharArray(), null, null));
    }

    @Test
    public void wrongPassword() {
        assertThrows(StoreConfigurationException.class, () -> new TrustStoreConfiguration(TestConstants.TRUSTSTORE_FILE, new MemoryPasswordProvider("wrongPassword!".toCharArray()), null, null));
    }
}
