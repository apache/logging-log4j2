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
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
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

class IcebergScheduledFlushTest {

    private Path warehouseDir;
    private IcebergManager manager;

    @BeforeEach
    void setUp() throws IOException {
        warehouseDir = Files.createTempDirectory("iceberg-scheduled-flush-test");
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
    void scheduledFlushCommitsWithoutManualTrigger() throws Exception {
        // flushIntervalSeconds=1 so the timer fires every second
        // batchSize=10000 so batch-triggered flush won't happen with just 5 events
        manager = new IcebergManager(
                "scheduled",
                "test_catalog",
                "hadoop",
                null,
                warehouseDir.toAbsolutePath().toString(),
                "test_ns",
                "scheduled_table",
                10000,
                1);
        manager.startup();

        for (int i = 0; i < 5; i++) {
            final LogEvent event = Log4jLogEvent.newBuilder()
                    .setLoggerName("com.example.Scheduled")
                    .setLevel(Level.INFO)
                    .setMessage(new SimpleMessage("scheduled-msg-" + i))
                    .setThreadName("main")
                    .setTimeMillis(System.currentTimeMillis())
                    .build();
            manager.write(event.toImmutable());
        }

        // Wait for the scheduled flush to fire (interval is 1s, wait up to 3s)
        long deadline = System.currentTimeMillis() + 3000;
        int recordCount = 0;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(200);
            manager.table.refresh();
            recordCount = 0;
            try (CloseableIterable<Record> records = IcebergGenerics.read(manager.table).build()) {
                for (final Record ignored : records) {
                    recordCount++;
                }
            }
            if (recordCount == 5) {
                break;
            }
        }

        assertThat(recordCount).isEqualTo(5);
    }
}
