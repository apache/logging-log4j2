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
package org.apache.logging.log4j.cassandra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.SSLOptions;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.appender.db.ColumnMapping;
import org.apache.logging.log4j.core.net.SocketAddress;
import org.apache.logging.log4j.core.util.ReflectionUtil;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.junit.jupiter.api.Test;

class CassandraManagerSslTest {

    @Test
    void hostNameVerifyingSslOptionsEnableHttpsEndpointIdentification() throws Exception {
        final CassandraManager.HostNameVerifyingSSLOptions options =
                CassandraManager.createHostNameVerifyingSSLOptions();

        assertEquals(
                "HTTPS",
                options.newSSLEngine(null, new InetSocketAddress("example.com", 9042))
                        .getSSLParameters()
                        .getEndpointIdentificationAlgorithm());
    }

    @Test
    @SetTestProperty(key = "log4j2.sslVerifyHostName", value = "true")
    void managerVerifiesHostNameWhenPropertyIsTrue() throws Exception {
        assertInstanceOf(CassandraManager.HostNameVerifyingSSLOptions.class, sslOptionsOfNewManager());
    }

    @Test
    @SetTestProperty(key = "log4j2.sslVerifyHostName", value = "false")
    void managerKeepsDriverDefaultsWhenPropertyIsFalse() throws Exception {
        assertFalse(sslOptionsOfNewManager() instanceof CassandraManager.HostNameVerifyingSSLOptions);
    }

    @Test
    void managerKeepsDriverDefaultsWhenPropertyIsUnset() throws Exception {
        assertFalse(sslOptionsOfNewManager() instanceof CassandraManager.HostNameVerifyingSSLOptions);
    }

    private static SSLOptions sslOptionsOfNewManager() throws Exception {
        final CassandraManager manager = CassandraManager.getManager(
                "CassandraManagerSslTest-" + System.nanoTime(),
                new SocketAddress[] {SocketAddress.getLoopback()},
                new ColumnMapping[0],
                true,
                "test",
                "keyspace",
                "table",
                null,
                null,
                false,
                0,
                false,
                BatchStatement.Type.LOGGED);
        try {
            final Cluster cluster =
                    (Cluster) ReflectionUtil.getFieldValue(CassandraManager.class.getDeclaredField("cluster"), manager);
            return cluster.getConfiguration().getProtocolOptions().getSSLOptions();
        } finally {
            manager.stop(0, TimeUnit.MILLISECONDS);
        }
    }
}
