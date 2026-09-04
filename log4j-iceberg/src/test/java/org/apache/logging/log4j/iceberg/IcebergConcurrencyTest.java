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
package org.apache.logging.log4j.iceberg;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.iceberg.data.IcebergGenerics;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IcebergConcurrencyTest {

    private Path warehouseDir;
    private IcebergManager manager;

    @BeforeEach
    void setUp() throws IOException {
        warehouseDir = Files.createTempDirectory("iceberg-concurrency-test");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (manager != null) {
            manager.stop(10, TimeUnit.SECONDS);
        }
        Files.walk(warehouseDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    void multipleThreadsWriteConcurrently() throws Exception {
        final int threadCount = 4;
        final int eventsPerThread = 1000;
        final int totalEvents = threadCount * eventsPerThread;

        manager = new IcebergManager(
                "concurrency",
                "test_catalog",
                "hadoop",
                null,
                warehouseDir.toAbsolutePath().toString(),
                "test_ns",
                "concurrent_table",
                100,
                3600);
        manager.startup();

        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(threadCount);
        final AtomicInteger errorCount = new AtomicInteger(0);

        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < eventsPerThread; i++) {
                        final LogEvent event = Log4jLogEvent.newBuilder()
                                .setLoggerName("com.example.Thread" + threadId)
                                .setLevel(Level.INFO)
                                .setMessage(new SimpleMessage("thread-" + threadId + "-msg-" + i))
                                .setThreadName("writer-" + threadId)
                                .setTimeMillis(System.currentTimeMillis())
                                .build();
                        manager.write(event.toImmutable());
                    }
                } catch (final Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertThat(doneLatch.await(30, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        assertThat(errorCount.get()).isZero();

        manager.flush();
        manager.table.refresh();

        final List<Record> records = new ArrayList<>();
        try (CloseableIterable<Record> iterable = IcebergGenerics.read(manager.table).build()) {
            for (final Record record : iterable) {
                records.add(record);
            }
        }

        assertThat(records).hasSize(totalEvents);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            long count = records.stream()
                    .filter(r -> r.getField("logger_name").equals("com.example.Thread" + threadId))
                    .count();
            assertThat(count).as("Events from thread %d", threadId).isEqualTo(eventsPerThread);
        }
    }

    @Test
    void concurrentWritesAndFlushes() throws Exception {
        final int writerCount = 3;
        final int eventsPerWriter = 500;
        final int totalEvents = writerCount * eventsPerWriter;

        manager = new IcebergManager(
                "flush_concurrency",
                "test_catalog",
                "hadoop",
                null,
                warehouseDir.toAbsolutePath().toString(),
                "test_ns",
                "flush_concurrent_table",
                50,
                3600);
        manager.startup();

        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(writerCount + 1);
        final AtomicInteger errorCount = new AtomicInteger(0);

        final ExecutorService executor = Executors.newFixedThreadPool(writerCount + 1);

        for (int t = 0; t < writerCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < eventsPerWriter; i++) {
                        final LogEvent event = Log4jLogEvent.newBuilder()
                                .setLoggerName("com.example.Writer" + threadId)
                                .setLevel(Level.DEBUG)
                                .setMessage(new SimpleMessage("concurrent-" + threadId + "-" + i))
                                .setThreadName("writer-" + threadId)
                                .setTimeMillis(System.currentTimeMillis())
                                .build();
                        manager.write(event.toImmutable());
                    }
                } catch (final Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Explicit flusher thread competing with batch-triggered flushes
        executor.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < 20; i++) {
                    Thread.sleep(10);
                    manager.flush();
                }
            } catch (final Exception e) {
                errorCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        assertThat(doneLatch.await(30, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        assertThat(errorCount.get()).isZero();

        manager.flush();
        manager.table.refresh();

        final List<Record> records = new ArrayList<>();
        try (CloseableIterable<Record> iterable = IcebergGenerics.read(manager.table).build()) {
            for (final Record record : iterable) {
                records.add(record);
            }
        }

        assertThat(records).hasSize(totalEvents);
    }
}
