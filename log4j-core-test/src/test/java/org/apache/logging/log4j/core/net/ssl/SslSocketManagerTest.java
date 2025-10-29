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

import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.net.SslSocketManager;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.Issue;

class SslSocketManagerTest {
    @Issue("https://github.com/apache/logging-log4j2/issues/3947")
    @Test
    void shouldNotThrowExceptionWhenConfiguringTrustStore() {
        final TrustStoreConfiguration trustStoreConfiguration = assertDoesNotThrow(() -> new TrustStoreConfiguration(
                SslKeyStoreConstants.TRUSTSTORE_LOCATION,
                SslKeyStoreConstants::TRUSTSTORE_PWD,
                SslKeyStoreConstants.TRUSTSTORE_TYPE,
                null));
        final SslConfiguration sslConfiguration =
                SslConfiguration.createSSLConfiguration(null, null, trustStoreConfiguration);
        assertDoesNotThrow(() -> {
            // noinspection EmptyTryBlock (try-with-resources to close `SslSocketManager`, even on failure
            try (final SslSocketManager ignored = SslSocketManager.getSocketManager(
                    sslConfiguration, "localhost", 0, 0, 0, true, PatternLayout.createDefaultLayout(), 8192, null)) {
                // Do nothing
            }
        });
    }
}
