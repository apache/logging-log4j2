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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyStore;
import org.junit.jupiter.api.Test;

public class TrustStoreConfigurationTest {
    @SuppressWarnings("deprecation")
    @Test
    public void loadEmptyConfigurationDeprecated() {
        assertThatThrownBy(() -> new TrustStoreConfiguration(null, TestConstants.NULL_PWD, null, null)).isInstanceOf(StoreConfigurationException.class);
    }

    @Test
    public void loadEmptyConfiguration() {
        assertThatThrownBy(() -> new TrustStoreConfiguration(null, new MemoryPasswordProvider(TestConstants.NULL_PWD), null, null)).isInstanceOf(StoreConfigurationException.class);
    }

    @Test
    public void loadConfigurationDeprecated() throws StoreConfigurationException {
        @SuppressWarnings("deprecation") final TrustStoreConfiguration ksc =
                new TrustStoreConfiguration(TestConstants.TRUSTSTORE_FILE, TestConstants.TRUSTSTORE_PWD(), null, null);
        final KeyStore ks = ksc.getKeyStore();
        assertThat(ks).isNotNull();
    }

    @Test
    public void loadConfiguration() throws StoreConfigurationException {
        final TrustStoreConfiguration ksc = new TrustStoreConfiguration(TestConstants.TRUSTSTORE_FILE, new MemoryPasswordProvider(TestConstants.TRUSTSTORE_PWD()), null, null);
        final KeyStore ks = ksc.getKeyStore();
        assertThat(ks).isNotNull();
    }

    @Test
    public void returnTheSameKeyStoreAfterMultipleLoadsDeprecated() throws StoreConfigurationException {
        @SuppressWarnings("deprecation") final TrustStoreConfiguration ksc =
                new TrustStoreConfiguration(TestConstants.TRUSTSTORE_FILE, TestConstants.TRUSTSTORE_PWD(), null, null);
        final KeyStore ks = ksc.getKeyStore();
        final KeyStore ks2 = ksc.getKeyStore();
        assertThat(ks2).isSameAs(ks);
    }

    @Test
    public void returnTheSameKeyStoreAfterMultipleLoads() throws StoreConfigurationException {
        final TrustStoreConfiguration ksc = new TrustStoreConfiguration(TestConstants.TRUSTSTORE_FILE, new MemoryPasswordProvider(TestConstants.TRUSTSTORE_PWD()), null, null);
        final KeyStore ks = ksc.getKeyStore();
        final KeyStore ks2 = ksc.getKeyStore();
        assertThat(ks2).isSameAs(ks);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void wrongPasswordDeprecated() {
        assertThatThrownBy(() -> new TrustStoreConfiguration(TestConstants.TRUSTSTORE_FILE, "wrongPassword!".toCharArray(), null, null)).isInstanceOf(StoreConfigurationException.class);
    }

    @Test
    public void wrongPassword() {
        assertThatThrownBy(() -> new TrustStoreConfiguration(TestConstants.TRUSTSTORE_FILE, new MemoryPasswordProvider("wrongPassword!".toCharArray()), null, null)).isInstanceOf(StoreConfigurationException.class);
    }
}
