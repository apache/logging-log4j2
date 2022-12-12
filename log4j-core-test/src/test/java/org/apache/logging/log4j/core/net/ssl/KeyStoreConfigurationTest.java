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

import java.security.KeyStore;

import org.apache.logging.log4j.core.test.net.ssl.TestConstants;
import org.apache.logging.log4j.test.junit.StatusLoggerLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@StatusLoggerLevel("OFF")
public class KeyStoreConfigurationTest {
    @Test
    public void loadEmptyConfiguration() {
        assertThrows(StoreConfigurationException.class,
                () -> KeyStoreConfiguration.builder().build());
    }

    @Test
    public void loadNotEmptyConfiguration() throws StoreConfigurationException {
        final KeyStoreConfiguration ksc = KeyStoreConfiguration.builder()
                .setLocation(TestConstants.KEYSTORE_FILE)
                .setPassword(TestConstants.KEYSTORE_PWD())
                .setKeyStoreType(TestConstants.KEYSTORE_TYPE)
                .build();
        final KeyStore ks = ksc.getKeyStore();
        assertNotNull(ks);
    }

    @Test
    public void returnTheSameKeyStoreAfterMultipleLoads() throws StoreConfigurationException {
        final KeyStoreConfiguration ksc = KeyStoreConfiguration.builder()
                .setLocation(TestConstants.KEYSTORE_FILE)
                .setPassword(TestConstants.KEYSTORE_PWD())
                .setKeyStoreType(TestConstants.KEYSTORE_TYPE)
                .build();
        final KeyStore ks = ksc.getKeyStore();
        final KeyStore ks2 = ksc.getKeyStore();
        assertSame(ks, ks2);
    }

    @Test
    public void wrongPassword() {
        assertThrows(StoreConfigurationException.class, () ->
                KeyStoreConfiguration.builder()
                        .setLocation(TestConstants.KEYSTORE_FILE)
                        .setPassword("wrongPassword!".toCharArray())
                        .build());
    }
}
