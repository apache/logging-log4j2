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
package org.apache.logging.log4j.cassandra;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Permission;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.cassandra.service.CassandraDaemon;
import org.apache.logging.log4j.core.util.Cancellable;
import org.apache.logging.log4j.core.util.Closer;
import org.apache.logging.log4j.core.util.Log4jThreadFactory;
import org.apache.logging.log4j.core.util.Throwables;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver;
import org.opentest4j.TestAbortedException;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

import static org.junit.jupiter.api.Assertions.fail;

public class CassandraExtension extends TypeBasedParameterResolver<Cluster> implements BeforeEachCallback, AfterEachCallback {
    private static final ThreadFactory THREAD_FACTORY = Log4jThreadFactory.createThreadFactory("CassandraFixture");

    @Override
    public void beforeEach(final ExtensionContext context) throws Exception {
        final var cassandraFixture = context.getRequiredTestMethod().getAnnotation(CassandraFixture.class);
        if (cassandraFixture != null) {
            final var latch = new CountDownLatch(1);
            final var errorRef = new AtomicReference<Throwable>();
            final var embeddedCassandra = new EmbeddedCassandra(latch, errorRef);
            final Path root = Files.createTempDirectory("cassandra");
            Files.createDirectories(root.resolve("data"));
            final Path config = root.resolve("cassandra.yml");
            Files.copy(getClass().getResourceAsStream("/cassandra.yaml"), config);
            System.setProperty("cassandra.config", "file:" + config.toString());
            System.setProperty("cassandra.storagedir", root.toString());
            System.setProperty("cassandra-foreground", "true"); // prevents Cassandra from closing stdout/stderr
            THREAD_FACTORY.newThread(embeddedCassandra).start();
            latch.await();
            final Throwable error = errorRef.get();
            if (error instanceof NoClassDefFoundError) {
                throw new TestAbortedException("Unsupported platform", error);
            } else {
                fail(error);
            }
            final var cluster = Cluster.builder().addContactPoints(InetAddress.getLoopbackAddress()).build();
            final var store = context.getStore(
                    ExtensionContext.Namespace.create(CassandraFixture.class, context.getRequiredTestInstance()));
            store.put(Cluster.class, cluster);
            store.put(EmbeddedCassandra.class, embeddedCassandra);
            try (final Session session = cluster.connect()) {
                session.execute("CREATE KEYSPACE " + cassandraFixture.keyspace() + " WITH REPLICATION = " +
                        "{ 'class': 'SimpleStrategy', 'replication_factor': 2 };");
            }
            try (final Session session = cluster.connect(cassandraFixture.keyspace())) {
                for (final String ddl : cassandraFixture.setup()) {
                    session.execute(ddl);
                }
            }
        }

    }

    @Override
    public void afterEach(final ExtensionContext context) throws Exception {
        final var store =
                context.getStore(ExtensionContext.Namespace.create(CassandraFixture.class, context.getRequiredTestInstance()));
        final var cluster = store.get(Cluster.class, Cluster.class);
        final var embeddedCassandra = store.get(EmbeddedCassandra.class, EmbeddedCassandra.class);
        if (embeddedCassandra != null) {
            Closer.closeSilently(cluster);
            embeddedCassandra.cancel();
        }
    }

    @Override
    public Cluster resolveParameter(
            final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        final var store = extensionContext.getStore(
                ExtensionContext.Namespace.create(CassandraFixture.class, extensionContext.getRequiredTestInstance()));
        return store.get(Cluster.class, Cluster.class);
    }

    private static class EmbeddedCassandra implements Cancellable {

        private final CassandraDaemon daemon = new CassandraDaemon();
        private final CountDownLatch latch;
        private final AtomicReference<Throwable> errorRef;

        private EmbeddedCassandra(
                final CountDownLatch latch, final AtomicReference<Throwable> errorRef) {
            this.latch = latch;
            this.errorRef = errorRef;
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
            try {
                daemon.init(null);
                daemon.start();
            } catch (final Exception | LinkageError e) {
                errorRef.set(Throwables.getRootCause(e));
            } finally {
                latch.countDown();
            }
        }
    }
}
