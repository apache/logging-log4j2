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
package org.apache.logging.log4j.core.appender;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

final class SslContexts {

    static SSLContext createSslContext(
            final String keyStoreType,
            final String keyStoreLocation,
            final char[] keyStorePassword,
            final String trustStoreType,
            final String trustStoreLocation,
            final char[] trustStorePassword)
            throws Exception {

        // Create the `KeyManagerFactory`
        final KeyStore keyStore = loadKeyStore(keyStoreType, keyStoreLocation, keyStorePassword);
        final String keyAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
        final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(keyAlgorithm);
        keyManagerFactory.init(keyStore, keyStorePassword);

        // Create the `TrustManagerFactory`
        final KeyStore trustStore = loadKeyStore(trustStoreType, trustStoreLocation, trustStorePassword);
        final String trustAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(trustAlgorithm);
        trustManagerFactory.init(trustStore);

        // Create the `SSLContext`
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
        return sslContext;
    }

    private static KeyStore loadKeyStore(final String storeType, final String storeLocation, final char[] storePassword)
            throws Exception {
        final KeyStore keyStore = KeyStore.getInstance(storeType);
        final ByteArrayInputStream storeByteStream =
                new ByteArrayInputStream(Files.readAllBytes(Paths.get(storeLocation)));
        keyStore.load(storeByteStream, storePassword);
        return keyStore;
    }
}
