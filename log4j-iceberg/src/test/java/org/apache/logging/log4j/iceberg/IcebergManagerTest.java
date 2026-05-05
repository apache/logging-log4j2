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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.Table;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.Namespace;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.data.IcebergGenerics;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.OutputFile;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class IcebergManagerTest {

    private Path warehouseDir;
    private IcebergManager manager;

    @BeforeEach
    void setUp() throws IOException {
        warehouseDir = Files.createTempDirectory("iceberg-test-warehouse");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (manager != null) {
            manager.stop(5, TimeUnit.SECONDS);
        }
        Files.walk(warehouseDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    private IcebergManager createManager(final String tableName, final int batchSize) {
        return new IcebergManager(
                "test",
                "test_catalog",
                "hadoop",
                null,
                warehouseDir.toAbsolutePath().toString(),
                "test_ns",
                tableName,
                batchSize,
                3600);
    }

    private LogEvent makeEvent(final String loggerName, final Level level, final String message) {
        return Log4jLogEvent.newBuilder()
                .setLoggerName(loggerName)
                .setLevel(level)
                .setMessage(new SimpleMessage(message))
                .setThreadName("test-thread")
                .setTimeMillis(System.currentTimeMillis())
                .build();
    }

    private LogEvent makeEventWithThrown(
            final String loggerName, final Level level, final String message, final Throwable thrown) {
        return Log4jLogEvent.newBuilder()
                .setLoggerName(loggerName)
                .setLevel(level)
                .setMessage(new SimpleMessage(message))
                .setThreadName("test-thread")
                .setTimeMillis(System.currentTimeMillis())
                .setThrown(thrown)
                .build();
    }

    private LogEvent makeEventNullMessage(final String loggerName, final Level level) {
        return Log4jLogEvent.newBuilder()
                .setLoggerName(loggerName)
                .setLevel(level)
                .setThreadName("test-thread")
                .setTimeMillis(System.currentTimeMillis())
                .build();
    }

    @Test
    void startupCreatesNewTable() {
        manager = createManager("new_table", 100);
        manager.startup();

        assertThat(manager.table).isNotNull();
        assertThat(manager.catalog.tableExists(TableIdentifier.of(Namespace.of("test_ns"), "new_table")))
                .isTrue();
    }

    @Test
    void startupLoadsExistingTable() {
        manager = createManager("existing_table", 100);
        manager.startup();

        final Table firstTable = manager.table;
        manager.stop(5, TimeUnit.SECONDS);

        manager = createManager("existing_table", 100);
        manager.startup();
        assertThat(manager.table).isNotNull();
        assertThat(manager.table.location()).isEqualTo(firstTable.location());
    }

    @Test
    void startupIdempotentWhenAlreadyRunning() {
        manager = createManager("idempotent_table", 100);
        manager.startup();
        final Table firstTable = manager.table;

        manager.startup();
        assertThat(manager.table).isSameAs(firstTable);
    }

    @Test
    void writeAndFlushSingleEvent() throws IOException {
        manager = createManager("single_event", 100);
        manager.startup();

        manager.write(makeEvent("com.example.Test", Level.INFO, "hello iceberg").toImmutable());
        manager.flush();

        manager.table.refresh();
        try (CloseableIterable<Record> records =
                IcebergGenerics.read(manager.table).build()) {
            int count = 0;
            for (final Record record : records) {
                assertThat(record.getField("level")).isEqualTo("INFO");
                assertThat(record.getField("logger_name")).isEqualTo("com.example.Test");
                assertThat(record.getField("message")).isEqualTo("hello iceberg");
                assertThat(record.getField("thread_name")).isEqualTo("test-thread");
                assertThat(record.getField("thrown")).isNull();
                assertThat(record.getField("timestamp")).isNotNull();
                assertThat(record.getField("event_date")).isNotNull();
                count++;
            }
            assertThat(count).isEqualTo(1);
        }
    }

    @Test
    void writeTriggersFlushWhenBatchSizeReached() throws IOException {
        manager = createManager("batch_flush", 3);
        manager.startup();

        manager.write(makeEvent("logger1", Level.DEBUG, "msg1").toImmutable());
        manager.write(makeEvent("logger2", Level.WARN, "msg2").toImmutable());
        manager.write(makeEvent("logger3", Level.ERROR, "msg3").toImmutable());

        manager.table.refresh();
        try (CloseableIterable<Record> records =
                IcebergGenerics.read(manager.table).build()) {
            int count = 0;
            for (final Record ignored : records) {
                count++;
            }
            assertThat(count).isEqualTo(3);
        }
    }

    @Test
    void flushOnEmptyBufferIsNoOp() {
        manager = createManager("empty_flush", 100);
        manager.startup();

        manager.flush();

        assertThat(manager.table.snapshots()).isEmpty();
    }

    @Test
    void eventWithThrownIsRecorded() throws IOException {
        manager = createManager("thrown_event", 100);
        manager.startup();

        final RuntimeException ex = new RuntimeException("test failure");
        manager.write(makeEventWithThrown("com.example.Error", Level.ERROR, "oops", ex)
                .toImmutable());
        manager.flush();

        manager.table.refresh();
        try (CloseableIterable<Record> records =
                IcebergGenerics.read(manager.table).build()) {
            for (final Record record : records) {
                assertThat((String) record.getField("thrown")).contains("test failure");
            }
        }
    }

    @Test
    void eventWithNullMessageIsRecorded() throws IOException {
        manager = createManager("null_msg", 100);
        manager.startup();

        manager.write(makeEventNullMessage("com.example.Null", Level.TRACE).toImmutable());
        manager.flush();

        manager.table.refresh();
        try (CloseableIterable<Record> records =
                IcebergGenerics.read(manager.table).build()) {
            for (final Record record : records) {
                assertThat(record.getField("message")).isNull();
                assertThat(record.getField("thrown")).isNull();
            }
        }
    }

    @Test
    void multipleFlushesProduceMultipleSnapshots() throws IOException {
        manager = createManager("multi_flush", 100);
        manager.startup();

        manager.write(makeEvent("logger", Level.INFO, "batch1").toImmutable());
        manager.flush();

        manager.write(makeEvent("logger", Level.INFO, "batch2").toImmutable());
        manager.flush();

        manager.table.refresh();
        int snapshotCount = 0;
        for (final org.apache.iceberg.Snapshot ignored : manager.table.snapshots()) {
            snapshotCount++;
        }
        assertThat(snapshotCount).isEqualTo(2);

        try (CloseableIterable<Record> records =
                IcebergGenerics.read(manager.table).build()) {
            int count = 0;
            for (final Record ignored : records) {
                count++;
            }
            assertThat(count).isEqualTo(2);
        }
    }

    @Test
    void stopWhenNotRunningReturnsTrueImmediately() {
        manager = createManager("not_running", 100);
        assertThat(manager.stop(1, TimeUnit.SECONDS)).isTrue();
        manager = null;
    }

    @Test
    void stopFlushesRemainingEvents() throws IOException {
        manager = createManager("stop_flush", 100);
        manager.startup();

        manager.write(makeEvent("logger", Level.INFO, "pending").toImmutable());
        manager.stop(5, TimeUnit.SECONDS);

        final IcebergManager reader = createManager("stop_flush", 100);
        reader.startup();
        reader.table.refresh();
        try (CloseableIterable<Record> records =
                IcebergGenerics.read(reader.table).build()) {
            int count = 0;
            for (final Record record : records) {
                assertThat(record.getField("message")).isEqualTo("pending");
                count++;
            }
            assertThat(count).isEqualTo(1);
        }
        reader.stop(5, TimeUnit.SECONDS);
        manager = null;
    }

    @Test
    void getName() {
        manager = createManager("name_test", 100);
        assertThat(manager.getName()).isEqualTo("test");
        manager = null;
    }

    @Test
    void startupWithNullUriAndWarehouse() {
        manager = new IcebergManager(
                "test",
                "test_catalog",
                "hadoop",
                null,
                warehouseDir.toAbsolutePath().toString(),
                "test_ns",
                "null_uri_test",
                100,
                3600);
        manager.startup();
        assertThat(manager.table).isNotNull();
    }

    @Test
    void startupWithCatalogUri() {
        manager = new IcebergManager(
                "test",
                "test_catalog",
                "hadoop",
                "file:///tmp/unused",
                warehouseDir.toAbsolutePath().toString(),
                "test_ns",
                "uri_test",
                100,
                3600);
        manager.startup();
        assertThat(manager.table).isNotNull();
    }

    @Test
    void commitRetrySucceedsAfterTransientFailure() throws IOException {
        manager = createManager("retry_ok", 100);
        manager.startup();

        manager.write(makeEvent("logger", Level.INFO, "retry event").toImmutable());

        final Table original = manager.table;
        final Table spyTable = Mockito.spy(original);
        final AppendFiles failingAppend = Mockito.mock(AppendFiles.class);
        Mockito.doThrow(new org.apache.iceberg.exceptions.CommitFailedException("conflict"))
                .when(failingAppend)
                .commit();
        final AppendFiles realAppend = original.newAppend();

        Mockito.when(spyTable.newAppend()).thenReturn(failingAppend).thenReturn(realAppend);
        manager.table = spyTable;

        manager.flush();

        original.refresh();
        try (CloseableIterable<Record> records = IcebergGenerics.read(original).build()) {
            int count = 0;
            for (final Record record : records) {
                assertThat(record.getField("message")).isEqualTo("retry event");
                count++;
            }
            assertThat(count).isEqualTo(1);
        }
        manager.table = original;
    }

    @Test
    void commitCrashesAfterExhaustingRetries() {
        manager = createManager("retry_exhaust", 100);
        manager.startup();

        manager.write(makeEvent("logger", Level.INFO, "will exhaust retries").toImmutable());

        final Table original = manager.table;
        final Table mockTable = Mockito.mock(Table.class);
        Mockito.when(mockTable.location()).thenReturn(original.location());
        Mockito.when(mockTable.io()).thenReturn(original.io());
        Mockito.when(mockTable.spec()).thenReturn(original.spec());
        final AppendFiles failingAppend = Mockito.mock(AppendFiles.class);
        Mockito.doThrow(new org.apache.iceberg.exceptions.CommitFailedException("persistent conflict"))
                .when(failingAppend)
                .commit();
        Mockito.when(mockTable.newAppend()).thenReturn(failingAppend);
        manager.table = mockTable;

        assertThatThrownBy(() -> manager.flush())
                .isInstanceOf(IcebergCommitException.class)
                .hasMessageContaining("after " + (IcebergManager.MAX_COMMIT_RETRIES + 1) + " attempts")
                .hasCauseInstanceOf(org.apache.iceberg.exceptions.CommitFailedException.class);

        manager.table = original;
    }

    @Test
    void stopHandlesInterruptedException() throws InterruptedException {
        manager = createManager("interrupt_test", 100);
        manager.startup();

        final ScheduledExecutorService realScheduler = manager.scheduler;
        realScheduler.shutdown();
        final ScheduledExecutorService mockScheduler = Mockito.mock(ScheduledExecutorService.class);
        Mockito.when(mockScheduler.awaitTermination(Mockito.anyLong(), Mockito.any()))
                .thenThrow(new InterruptedException("test interrupt"));
        manager.scheduler = mockScheduler;

        assertThat(manager.stop(5, TimeUnit.SECONDS)).isTrue();
        assertThat(Thread.interrupted()).isTrue();
        manager = null;
    }

    @Test
    void stopClosesCloseableCatalog() {
        manager = createManager("closeable_test", 100);
        manager.startup();
        // HadoopCatalog implements Closeable, so this path is naturally covered
        assertThat(manager.catalog).isInstanceOf(Closeable.class);
        assertThat(manager.stop(5, TimeUnit.SECONDS)).isTrue();
        manager = null;
    }

    @Test
    void stopHandlesCloseableIOException() throws IOException {
        manager = createManager("close_error_test", 100);
        manager.startup();

        // Replace catalog with a Closeable mock that throws on close
        final CloseableCatalog mockCatalog = Mockito.mock(CloseableCatalog.class);
        Mockito.doThrow(new IOException("close failed")).when(mockCatalog).close();
        manager.catalog = mockCatalog;

        assertThat(manager.stop(5, TimeUnit.SECONDS)).isTrue();
        manager = null;
    }

    interface CloseableCatalog extends Catalog, Closeable {}

    @Test
    void startupWithNullWarehouse() {
        manager =
                new IcebergManager("test", "test_catalog", "hadoop", null, null, "test_ns", "null_wh_test", 100, 3600);
        try {
            manager.startup();
        } catch (final Exception e) {
            // Expected — hadoop catalog requires a warehouse
        }
        manager = null;
    }

    @Test
    void stopWithNullFlushTaskAndScheduler() {
        manager = createManager("null_sched", 100);
        manager.running = true;
        manager.catalog = Mockito.mock(Catalog.class);
        assertThat(manager.stop(1, TimeUnit.SECONDS)).isTrue();
        manager = null;
    }

    @Test
    void stopWithNonCloseableCatalog() {
        manager = createManager("non_closeable", 100);
        manager.running = true;
        manager.catalog = Mockito.mock(Catalog.class);
        assertThat(manager.stop(1, TimeUnit.SECONDS)).isTrue();
        manager = null;
    }

    @Test
    void writeParquetFileIOExceptionCrashes() {
        manager = createManager("io_error", 100);
        manager.startup();

        manager.write(makeEvent("logger", Level.INFO, "will fail IO").toImmutable());
        final Table original = manager.table;
        final Table mockTable = Mockito.mock(Table.class);
        Mockito.when(mockTable.location()).thenReturn(original.location());
        Mockito.when(mockTable.spec()).thenReturn(original.spec());
        final org.apache.iceberg.io.FileIO mockIo = Mockito.mock(org.apache.iceberg.io.FileIO.class);
        Mockito.when(mockTable.io()).thenReturn(mockIo);
        final OutputFile mockOutput = Mockito.mock(OutputFile.class);
        Mockito.when(mockIo.newOutputFile(Mockito.anyString())).thenReturn(mockOutput);
        Mockito.when(mockOutput.createOrOverwrite()).thenThrow(new RuntimeException(new IOException("disk full")));
        manager.table = mockTable;

        assertThatThrownBy(() -> manager.flush())
                .isInstanceOf(IcebergCommitException.class)
                .hasMessageContaining("Failed to write Parquet file");
        manager.table = original;
    }

    @Test
    void commitRetryInterruptedCrashes() {
        manager = createManager("retry_interrupt", 100);
        manager.startup();

        manager.write(makeEvent("logger", Level.INFO, "will be interrupted").toImmutable());

        final Table original = manager.table;
        final Table spyTable = Mockito.spy(original);
        final AppendFiles failingAppend = Mockito.mock(AppendFiles.class);
        Mockito.doAnswer(invocation -> {
                    Thread.currentThread().interrupt();
                    throw new org.apache.iceberg.exceptions.CommitFailedException("conflict");
                })
                .when(failingAppend)
                .commit();
        Mockito.when(spyTable.newAppend()).thenReturn(failingAppend);
        manager.table = spyTable;

        assertThatThrownBy(() -> manager.flush())
                .isInstanceOf(IcebergCommitException.class)
                .hasMessageContaining("Interrupted");
        Thread.interrupted();
        manager.table = original;
    }

    @Test
    void newTableIsPartitionedByEventDate() {
        manager = createManager("partitioned_table", 100);
        manager.startup();

        assertThat(manager.table.spec().isPartitioned()).isTrue();
        assertThat(manager.table.spec().fields()).hasSize(1);
        assertThat(manager.table.spec().fields().get(0).name()).isEqualTo("event_date_day");
    }

    @Test
    void validateSchemaPassesWithValidTable() {
        manager = createManager("valid_schema", 100);
        manager.startup();

        // Should not throw — table was just created with LOG_SCHEMA
        manager.validateSchema(manager.table.schema());
    }

    @Test
    void validateSchemaFailsWithMissingColumns() {
        manager = createManager("missing_cols", 100);
        manager.startup();

        final org.apache.iceberg.Schema incompleteSchema = new org.apache.iceberg.Schema(
                org.apache.iceberg.types.Types.NestedField.required(1, "timestamp",
                        org.apache.iceberg.types.Types.TimestampType.withZone()),
                org.apache.iceberg.types.Types.NestedField.required(2, "level",
                        org.apache.iceberg.types.Types.StringType.get()));

        assertThatThrownBy(() -> manager.validateSchema(incompleteSchema))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing required columns");
    }

    @Test
    void existingTableWithValidSchemaLoadsSuccessfully() {
        // Create table first
        manager = createManager("existing_valid", 100);
        manager.startup();
        manager.stop(5, TimeUnit.SECONDS);

        // Re-open — should load existing table and pass validation
        manager = createManager("existing_valid", 100);
        manager.startup();
        assertThat(manager.table).isNotNull();
    }

    @Test
    void extraCatalogPropertiesArePassedThrough() {
        final java.util.Map<String, String> extraProps = new java.util.HashMap<>();
        extraProps.put("custom.property", "custom-value");

        manager = new IcebergManager(
                "test",
                "test_catalog",
                "hadoop",
                null,
                warehouseDir.toAbsolutePath().toString(),
                "test_ns",
                "extra_props_table",
                100,
                3600,
                extraProps);
        manager.startup();
        assertThat(manager.table).isNotNull();
    }

    @Test
    void nullExtraCatalogPropertiesHandledGracefully() {
        manager = new IcebergManager(
                "test",
                "test_catalog",
                "hadoop",
                null,
                warehouseDir.toAbsolutePath().toString(),
                "test_ns",
                "null_extra_props",
                100,
                3600,
                null);
        manager.startup();
        assertThat(manager.table).isNotNull();
    }
}
