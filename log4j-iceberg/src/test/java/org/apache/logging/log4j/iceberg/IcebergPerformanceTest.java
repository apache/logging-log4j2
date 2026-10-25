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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.data.IcebergGenerics;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IcebergPerformanceTest {

    private static final int TOTAL_RECORDS = 1_000_000;
    private static final int RECORDS_PER_COMMIT = 100_000;

    private Path tempDir;
    private IcebergManager manager;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("iceberg-perf-test");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (manager != null) {
            manager.stop(30, TimeUnit.SECONDS);
        }
        Files.walk(tempDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }

    private static final Level[] LEVELS = {Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR};

    private LogEvent buildEvent(final int i) {
        return Log4jLogEvent.newBuilder()
                .setLoggerName("com.example.perf.Logger" + (i % 100))
                .setLevel(LEVELS[i % LEVELS.length])
                .setMessage(new SimpleMessage("perf-message-" + i))
                .setThreadName("perf-thread-" + (i % 10))
                .setTimeMillis(1700000000000L + i)
                .build();
    }

    @Test
    void writeOneMillionRecordsWithPeriodicCommits() throws IOException {

        // ============================================================
        // Phase 1: File Appender benchmark
        // ============================================================
        final Path logFile = tempDir.resolve("perf-test.log");
        final PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("%d %-5level [%t] %logger - %msg%n")
                .build();
        final FileAppender fileAppender = FileAppender.newBuilder()
                .setName("filePerf")
                .withFileName(logFile.toString())
                .withImmediateFlush(false)
                .withBufferedIo(true)
                .withBufferSize(8192)
                .setLayout(layout)
                .build();
        fileAppender.start();

        final long fileWriteStart = System.nanoTime();
        for (int i = 0; i < TOTAL_RECORDS; i++) {
            fileAppender.append(buildEvent(i));
        }
        fileAppender.stop(30, TimeUnit.SECONDS);
        final long fileWriteMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - fileWriteStart);
        final double fileWriteThroughput = (double) TOTAL_RECORDS / fileWriteMs * 1000.0;

        int fileLineCount = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile.toFile()))) {
            while (reader.readLine() != null) {
                fileLineCount++;
            }
        }
        assertThat(fileLineCount).isEqualTo(TOTAL_RECORDS);
        final long fileSizeBytes = Files.size(logFile);

        // ============================================================
        // Phase 2: Iceberg Appender benchmark
        // ============================================================
        final Path warehouseDir = tempDir.resolve("warehouse");
        Files.createDirectories(warehouseDir);
        manager = new IcebergManager(
                "perf_test",
                "perf_catalog",
                "hadoop",
                null,
                warehouseDir.toAbsolutePath().toString(),
                "perf_ns",
                "perf_table",
                RECORDS_PER_COMMIT,
                3600);
        manager.startup();

        final long icebergWriteStart = System.nanoTime();
        for (int i = 0; i < TOTAL_RECORDS; i++) {
            manager.write(buildEvent(i).toImmutable());
        }
        manager.flush();
        final long icebergWriteMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - icebergWriteStart);
        final double icebergWriteThroughput = (double) TOTAL_RECORDS / icebergWriteMs * 1000.0;

        manager.table.refresh();
        int snapshotCount = 0;
        for (final Snapshot ignored : manager.table.snapshots()) {
            snapshotCount++;
        }
        final int expectedCommits = (TOTAL_RECORDS + RECORDS_PER_COMMIT - 1) / RECORDS_PER_COMMIT;
        assertThat(snapshotCount).isEqualTo(expectedCommits);

        // Iceberg read-back
        final long icebergReadStart = System.nanoTime();
        final Map<String, Integer> levelCounts = new HashMap<>();
        final Set<String> seenMessages = new HashSet<>();
        int totalRead = 0;

        try (CloseableIterable<Record> records =
                IcebergGenerics.read(manager.table).build()) {
            for (final Record record : records) {
                final String level = (String) record.getField("level");
                final String message = (String) record.getField("message");

                assertThat(level).isNotNull();
                assertThat(message).startsWith("perf-message-");
                assertThat(record.getField("logger_name").toString()).startsWith("com.example.perf.Logger");
                assertThat(record.getField("thread_name").toString()).startsWith("perf-thread-");
                assertThat(record.getField("timestamp")).isNotNull();
                assertThat(record.getField("event_date")).isNotNull();

                levelCounts.merge(level, 1, Integer::sum);
                seenMessages.add(message);
                totalRead++;
            }
        }
        final long icebergReadMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - icebergReadStart);
        final double icebergReadThroughput = (double) totalRead / icebergReadMs * 1000.0;

        assertThat(totalRead).isEqualTo(TOTAL_RECORDS);
        assertThat(seenMessages).hasSize(TOTAL_RECORDS);
        final int expectedPerLevel = TOTAL_RECORDS / LEVELS.length;
        for (final Level level : LEVELS) {
            assertThat(levelCounts.get(level.name()))
                    .as("Count for level %s", level.name())
                    .isEqualTo(expectedPerLevel);
        }

        long icebergSizeBytes = 0;
        try (java.util.stream.Stream<Path> paths = Files.walk(warehouseDir)) {
            icebergSizeBytes = paths.filter(Files::isRegularFile)
                    .mapToLong(p -> p.toFile().length())
                    .sum();
        }

        // ============================================================
        // Results
        // ============================================================
        System.out.println();
        System.out.println("========== Performance Comparison (1M records) ==========");
        System.out.println();
        System.out.printf("%-25s %15s %15s%n", "", "File Appender", "Iceberg");
        System.out.printf("%-25s %15s %15s%n", "-------------------------", "---------------", "---------------");
        System.out.printf("%-25s %12d ms %12d ms%n", "Write time", fileWriteMs, icebergWriteMs);
        System.out.printf("%-25s %12.0f/s %12.0f/s%n", "Write throughput", fileWriteThroughput, icebergWriteThroughput);
        System.out.printf("%-25s %15s %12d ms%n", "Read time", "n/a", icebergReadMs);
        System.out.printf("%-25s %15s %12.0f/s%n", "Read throughput", "n/a", icebergReadThroughput);
        System.out.printf(
                "%-25s %12.1f MB %12.1f MB%n",
                "Disk size", fileSizeBytes / (1024.0 * 1024.0), icebergSizeBytes / (1024.0 * 1024.0));
        System.out.printf("%-25s %15d %15d%n", "Commits/flushes", 1, snapshotCount);
        System.out.printf("%-25s %12.1fx %15s%n", "Iceberg overhead", (double) icebergWriteMs / fileWriteMs, "");
        System.out.printf("%-25s %12.1fx %15s%n", "Iceberg compression", (double) fileSizeBytes / icebergSizeBytes, "");
        System.out.println();
        System.out.println("Level distribution: " + levelCounts);
        System.out.println("All " + TOTAL_RECORDS + " records verified correct in both appenders.");
        System.out.println("=========================================================");
    }
}
