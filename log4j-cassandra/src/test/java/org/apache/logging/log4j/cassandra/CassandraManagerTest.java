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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.EndPoint;
import com.datastax.driver.core.ExtendedRemoteEndpointAwareSslOptions;
import com.datastax.driver.core.SSLOptions;
import io.netty.handler.ssl.SslHandler;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.net.SocketAddress;
import org.junit.Test;

public class CassandraManagerTest {

    private static final String VERIFY_HOST_NAME_PROPERTY = "log4j2.sslVerifyHostName";

    @Test
    public void shouldVerifyHostNameWhenPropertyIsEnabled() throws Exception {
        withSystemProperty(VERIFY_HOST_NAME_PROPERTY, "true", () -> {
            final CassandraManager manager = createManager(true);
            try {
                final Cluster cluster = getCluster(manager);
                final SSLOptions sslOptions =
                        cluster.getConfiguration().getProtocolOptions().getSSLOptions();
                assertTrue(sslOptions instanceof ExtendedRemoteEndpointAwareSslOptions);

                final InetSocketAddress remoteAddress = new InetSocketAddress("cassandra.example", 9042);
                final EndPoint endpoint = () -> remoteAddress;
                final SslHandler sslHandler =
                        ((ExtendedRemoteEndpointAwareSslOptions) sslOptions).newSSLHandler(null, endpoint);
                assertEquals("HTTPS", sslHandler.engine().getSSLParameters().getEndpointIdentificationAlgorithm());
                assertEquals("cassandra.example", sslHandler.engine().getPeerHost());
            } finally {
                manager.stop(0, TimeUnit.MILLISECONDS);
            }
        });
    }

    @Test
    public void shouldKeepUsingDriverDefaultsWhenHostNameVerificationIsDisabled() throws Exception {
        withSystemProperty(VERIFY_HOST_NAME_PROPERTY, "false", () -> {
            final CassandraManager manager = createManager(true);
            try {
                final Cluster cluster = getCluster(manager);
                final SSLOptions sslOptions =
                        cluster.getConfiguration().getProtocolOptions().getSSLOptions();
                assertNotNull(sslOptions);
                assertTrue(sslOptions instanceof ExtendedRemoteEndpointAwareSslOptions);

                final InetSocketAddress remoteAddress = new InetSocketAddress("cassandra.example", 9042);
                final EndPoint endpoint = () -> remoteAddress;
                final SslHandler sslHandler =
                        ((ExtendedRemoteEndpointAwareSslOptions) sslOptions).newSSLHandler(null, endpoint);
                assertNull(sslHandler.engine().getSSLParameters().getEndpointIdentificationAlgorithm());
            } finally {
                manager.stop(0, TimeUnit.MILLISECONDS);
            }
        });
    }

    private static CassandraManager createManager(final boolean useTls) {
        return CassandraManager.getManager(
                "CassandraManagerTest-" + System.nanoTime(),
                new SocketAddress[] {SocketAddress.getLoopback()},
                new org.apache.logging.log4j.core.appender.db.ColumnMapping[0],
                useTls,
                "test",
                "keyspace",
                "table",
                null,
                null,
                false,
                0,
                false,
                BatchStatement.Type.LOGGED);
    }

    private static Cluster getCluster(final CassandraManager manager) throws Exception {
        final Field clusterField = CassandraManager.class.getDeclaredField("cluster");
        clusterField.setAccessible(true);
        return (Cluster) clusterField.get(manager);
    }

    private static void withSystemProperty(final String name, final String value, final ThrowingRunnable action)
            throws Exception {
        final String previousValue = System.getProperty(name);
        System.setProperty(name, value);
        try {
            action.run();
        } finally {
            if (previousValue == null) {
                System.clearProperty(name);
            } else {
                System.setProperty(name, previousValue);
            }
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
