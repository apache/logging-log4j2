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

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;
import io.netty.channel.socket.ServerSocketChannel;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Permission;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import org.apache.cassandra.service.CassandraDaemon;
import org.apache.cassandra.service.NativeTransportService;
import org.apache.cassandra.transport.Server;
import org.apache.cassandra.transport.Server.ConnectionTracker;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.util.Cancellable;
import org.apache.logging.log4j.core.util.Closer;
import org.apache.logging.log4j.core.util.Log4jThreadFactory;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.junit.rules.ExternalResource;

/**
 * JUnit rule to set up and tear down a Cassandra database instance.
 */
public class CassandraRule extends ExternalResource {

    private static final ThreadFactory THREAD_FACTORY = Log4jThreadFactory.createThreadFactory("Cassandra");

    private final CountDownLatch latch = new CountDownLatch(1);
    private final EmbeddedCassandra embeddedCassandra = new EmbeddedCassandra(latch);
    private final String keyspace;
    private final String tableDdl;
    private Cluster cluster;

    public CassandraRule(final String keyspace, final String tableDdl) {
        this.keyspace = keyspace;
        this.tableDdl = tableDdl;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public Session connect() {
        return cluster.connect(keyspace);
    }

    @Override
    protected void before() throws Throwable {
        final Path root = Files.createTempDirectory("cassandra");
        Files.createDirectories(root.resolve("data"));
        final Path config = root.resolve("cassandra.yml");
        Files.copy(getClass().getResourceAsStream("/cassandra.yaml"), config);
        System.setProperty("cassandra.native_transport_port", "0");
        System.setProperty("cassandra.storage_port", "0");
        System.setProperty("cassandra.config", "file:" + config.toString());
        System.setProperty("cassandra.storagedir", root.toString());
        System.setProperty("cassandra-foreground", "true"); // prevents Cassandra from closing stdout/stderr
        THREAD_FACTORY.newThread(embeddedCassandra).start();
        latch.await();
        final InetSocketAddress nativeSocket = embeddedCassandra.getNativeSocket();
        assertNotNull(nativeSocket);
        System.setProperty("cassandra.native_transport_port", Integer.toString(nativeSocket.getPort()));
        cluster = Cluster.builder()
                .addContactPointsWithPorts(nativeSocket)
                .withSocketOptions(new SocketOptions().setConnectTimeoutMillis(60000))
                .build();

        try (final Session session = cluster.connect()) {
            session.execute("CREATE KEYSPACE " + keyspace + " WITH REPLICATION = "
                    + "{ 'class': 'SimpleStrategy', 'replication_factor': 2 };");
        }
        try (final Session session = connect()) {
            session.execute(tableDdl);
        }
    }

    @Override
    protected void after() {
        Closer.closeSilently(cluster);
        embeddedCassandra.cancel();
    }

    private static final class EmbeddedCassandra implements Cancellable {

        private final CassandraDaemon daemon = CassandraDaemon.getInstanceForTesting();
        private final CountDownLatch latch;

        private EmbeddedCassandra(final CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void cancel() {
            // LOG4J2-1850 Cassandra on Windows calls System.exit in the daemon stop method
            if (PropertiesUtil.getProperties().isOsWindows()) {
                cancelOnWindows();
            } else {
                daemon.stop();
            }
        }

        private void cancelOnWindows() {
            final SecurityManager currentSecurityManager = System.getSecurityManager();
            try {
                final SecurityManager securityManager = new SecurityManager() {
                    @Override
                    public void checkPermission(final Permission permission) {
                        final String permissionName = permission.getName();
                        if (permissionName != null && permissionName.startsWith("exitVM")) {
                            throw new SecurityException("test");
                        }
                    }
                };
                System.setSecurityManager(securityManager);
                daemon.stop();
            } catch (final SecurityException ex) {
                // ignore
            } finally {
                System.setSecurityManager(currentSecurityManager);
            }
        }

        @Override
        public void run() {
            daemon.applyConfig();
            try {
                daemon.init(null);
            } catch (final IOException e) {
                throw new LoggingException("Cannot initialize embedded Cassandra instance", e);
            }
            daemon.start();
            latch.countDown();
        }

        public InetSocketAddress getNativeSocket() {
            try {
                final Field nativeServiceField = CassandraDaemon.class.getDeclaredField("nativeTransportService");
                nativeServiceField.setAccessible(true);
                final NativeTransportService nativeService = (NativeTransportService) nativeServiceField.get(daemon);
                final Field serversField = NativeTransportService.class.getDeclaredField("servers");
                serversField.setAccessible(true);
                @SuppressWarnings("unchecked")
                final Collection<Server> servers = (Collection<Server>) serversField.get(nativeService);
                if (servers.size() > 0) {
                    final Server server = servers.iterator().next();
                    final Field trackerField = Server.class.getDeclaredField("connectionTracker");
                    trackerField.setAccessible(true);
                    final ConnectionTracker connectionTracker = (ConnectionTracker) trackerField.get(server);
                    final ServerSocketChannel serverChannel = connectionTracker.allChannels.stream()
                            .filter(ServerSocketChannel.class::isInstance)
                            .map(ServerSocketChannel.class::cast)
                            .findFirst()
                            .orElse(null);
                    return serverChannel.localAddress();
                }
            } catch (ReflectiveOperationException | ClassCastException e) {
                fail(e);
            }
            return null;
        }
    }
}
