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

class IcebergAppenderTest {

    private Path warehouseDir;
    private IcebergAppender appender;

    @BeforeEach
    void setUp() throws IOException {
        warehouseDir = Files.createTempDirectory("iceberg-appender-test");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (appender != null && appender.isStarted()) {
            appender.stop(5, TimeUnit.SECONDS);
        }
        Files.walk(warehouseDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @SuppressWarnings("unchecked")
    private IcebergAppender buildAppender(final String tableName) {
        final IcebergAppender.Builder<? extends IcebergAppender.Builder<?>> builder = IcebergAppender.newBuilder();
        builder.setName("testAppender");
        builder.setCatalogName("test_catalog");
        builder.setCatalogImpl("hadoop");
        builder.setCatalogWarehouse(warehouseDir.toAbsolutePath().toString());
        builder.setTableNamespace("test_ns");
        builder.setTableName(tableName);
        builder.setBatchSize(100);
        builder.setFlushIntervalSeconds(3600);
        return builder.build();
    }

    @Test
    void builderCreatesAppender() {
        appender = buildAppender("builder_test");
        assertThat(appender).isNotNull();
        assertThat(appender.getName()).isEqualTo("testAppender");
    }

    @Test
    void startAndStop() {
        appender = buildAppender("start_stop");
        appender.start();
        assertThat(appender.isStarted()).isTrue();

        assertThat(appender.stop(5, TimeUnit.SECONDS)).isTrue();
        assertThat(appender.isStopped()).isTrue();
    }

    @Test
    void appendWritesEvents() throws IOException {
        appender = buildAppender("append_test");
        appender.start();

        final LogEvent event = Log4jLogEvent.newBuilder()
                .setLoggerName("com.example.AppenderTest")
                .setLevel(Level.WARN)
                .setMessage(new SimpleMessage("appender test message"))
                .setThreadName("test-thread")
                .setTimeMillis(System.currentTimeMillis())
                .build();

        appender.append(event);
        appender.stop(5, TimeUnit.SECONDS);

        final IcebergManager reader = new IcebergManager(
                "reader",
                "test_catalog",
                "hadoop",
                null,
                warehouseDir.toAbsolutePath().toString(),
                "test_ns",
                "append_test",
                100,
                3600);
        reader.startup();
        reader.table.refresh();
        try (CloseableIterable<Record> records =
                IcebergGenerics.read(reader.table).build()) {
            int count = 0;
            for (final Record record : records) {
                assertThat(record.getField("level")).isEqualTo("WARN");
                assertThat(record.getField("message")).isEqualTo("appender test message");
                count++;
            }
            assertThat(count).isEqualTo(1);
        }
        reader.stop(5, TimeUnit.SECONDS);
    }

    @Test
    void newBuilderFactory() {
        final IcebergAppender.Builder<?> builder = IcebergAppender.newBuilder();
        assertThat(builder).isNotNull();
    }

    @Test
    void builderSettersReturnBuilder() {
        final IcebergAppender.Builder<?> builder = IcebergAppender.newBuilder();
        assertThat(builder.setCatalogName("c")).isSameAs(builder);
        assertThat(builder.setCatalogImpl("i")).isSameAs(builder);
        assertThat(builder.setCatalogUri("u")).isSameAs(builder);
        assertThat(builder.setCatalogWarehouse("w")).isSameAs(builder);
        assertThat(builder.setTableNamespace("n")).isSameAs(builder);
        assertThat(builder.setTableName("t")).isSameAs(builder);
        assertThat(builder.setBatchSize(10)).isSameAs(builder);
        assertThat(builder.setFlushIntervalSeconds(5)).isSameAs(builder);
    }
}
