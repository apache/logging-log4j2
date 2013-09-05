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

import org.junit.Assert;
import org.junit.Test;

import java.security.KeyStore;

public class KeyStoreConfigurationTest {
    @Test(expected = StoreConfigurationException.class)
    public void loadEmptyConfiguration() throws StoreConfigurationException {
        KeyStoreConfiguration ksc = new KeyStoreConfiguration(null, null);
        KeyStore ks = ksc.getKeyStore();
        Assert.assertTrue(ks == null);
    }

    @Test
    public void loadNotEmptyConfiguration() throws StoreConfigurationException {
        KeyStoreConfiguration ksc = new KeyStoreConfiguration(TestConstants.KEYSTORE_FILE, TestConstants.KEYSTORE_PWD);
        KeyStore ks = ksc.getKeyStore();
        Assert.assertTrue(ks != null);
    }

    @Test
    public void returnTheSameKeyStoreAfterMultipleLoads() throws StoreConfigurationException {
        KeyStoreConfiguration ksc = new KeyStoreConfiguration(TestConstants.KEYSTORE_FILE, TestConstants.KEYSTORE_PWD);
        KeyStore ks = ksc.getKeyStore();
        KeyStore ks2 = ksc.getKeyStore();
        Assert.assertTrue(ks == ks2);
    }

    @Test(expected = StoreConfigurationException.class)
    public void wrongPassword() throws StoreConfigurationException {
        KeyStoreConfiguration ksc = new KeyStoreConfiguration(TestConstants.KEYSTORE_FILE, "wrongPassword!");
        KeyStore ks = ksc.getKeyStore();
        Assert.assertTrue(false);
    }
}
