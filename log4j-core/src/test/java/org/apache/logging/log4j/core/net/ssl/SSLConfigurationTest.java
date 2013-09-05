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

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.OutputStream;

public class SSLConfigurationTest {
    private static final String TLS_TEST_HOST = "login.yahoo.com";
    private static final int TLS_TEST_PORT = 443;

    @Test
    public void emptyConfigurationDoesntCauseNullSSLSocketFactory() throws SSLConfigurationException {
        SSLConfiguration sc = SSLConfiguration.createSSLConfiguration(null, null);
        SSLSocketFactory factory = sc.getSSLSocketFactory();
        Assert.assertTrue(factory != null);
    }

    @Test
    public void emptyConfigurationHasDefaultTrustStore() throws SSLConfigurationException, IOException {
        SSLConfiguration sc = SSLConfiguration.createSSLConfiguration(null, null);
        SSLSocketFactory factory = sc.getSSLSocketFactory();
        SSLSocket clientSocket = (SSLSocket) factory.createSocket(TLS_TEST_HOST, TLS_TEST_PORT);
        Assert.assertTrue(true);
    }

    @Test(expected = IOException.class)
    public void connectionFailsWithoutValidServerCertificate() throws SSLConfigurationException, IOException {
        TrustStoreConfiguration tsc = new TrustStoreConfiguration(TestConstants.TRUSTSTORE_FILE, null);
        SSLConfiguration sc = SSLConfiguration.createSSLConfiguration(null, tsc);
        SSLSocketFactory factory = sc.getSSLSocketFactory();
        SSLSocket clientSocket = (SSLSocket) factory.createSocket(TLS_TEST_HOST, TLS_TEST_PORT);
        OutputStream os = clientSocket.getOutputStream();
        os.write("GET config/login_verify2?".getBytes());
        Assert.assertTrue(false);
    }

    @Test
    public void loadKeyStoreWithoutPassword() throws StoreConfigurationException, SSLConfigurationException {
        KeyStoreConfiguration ksc = new KeyStoreConfiguration(TestConstants.KEYSTORE_FILE, null);
        SSLConfiguration sslConf = SSLConfiguration.createSSLConfiguration(ksc, null);
        SSLSocketFactory factory = sslConf.getSSLSocketFactory();
        Assert.assertTrue(true);
    }
}
