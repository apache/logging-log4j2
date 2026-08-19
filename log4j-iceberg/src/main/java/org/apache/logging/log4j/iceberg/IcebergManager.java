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

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.CatalogUtil;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Table;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.Namespace;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.parquet.GenericParquetWriter;
import org.apache.iceberg.io.DataWriter;
import org.apache.iceberg.io.OutputFile;
import org.apache.iceberg.parquet.Parquet;
import org.apache.iceberg.types.Types;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Manages the lifecycle of the Iceberg catalog, table, and periodic flush of
 * buffered log events into Parquet data files committed to the Iceberg table.
 */
class IcebergManager {

    private static final StatusLogger LOGGER = StatusLogger.getLogger();
    static final int MAX_COMMIT_RETRIES = 4;
    private static final long RETRY_BASE_SLEEP_MS = 100;

    static final Schema LOG_SCHEMA = new Schema(
            Types.NestedField.required(1, "timestamp", Types.TimestampType.withZone()),
            Types.NestedField.required(2, "level", Types.StringType.get()),
            Types.NestedField.required(3, "logger_name", Types.StringType.get()),
            Types.NestedField.optional(4, "message", Types.StringType.get()),
            Types.NestedField.optional(5, "thread_name", Types.StringType.get()),
            Types.NestedField.optional(6, "thrown", Types.StringType.get()),
            Types.NestedField.required(7, "event_date", Types.DateType.get()));

    static final PartitionSpec PARTITION_SPEC = PartitionSpec.builderFor(LOG_SCHEMA)
            .day("event_date")
            .build();

    private final String name;
    private final String catalogName;
    private final String catalogImpl;
    private final String catalogUri;
    private final String catalogWarehouse;
    private final String tableNamespace;
    private final String tableName;
    private final int batchSize;
    private final int flushIntervalSeconds;
    private final Map<String, String> extraCatalogProperties;

    private final ReentrantLock lock = new ReentrantLock();
    private List<LogEvent> buffer;

    Catalog catalog;
    Table table;
    ScheduledExecutorService scheduler;
    ScheduledFuture<?> flushTask;
    volatile boolean running;

    IcebergManager(
            final String name,
            final String catalogName,
            final String catalogImpl,
            final String catalogUri,
            final String catalogWarehouse,
            final String tableNamespace,
            final String tableName,
            final int batchSize,
            final int flushIntervalSeconds,
            final Map<String, String> extraCatalogProperties) {
        this.name = name;
        this.catalogName = catalogName;
        this.catalogImpl = catalogImpl;
        this.catalogUri = catalogUri;
        this.catalogWarehouse = catalogWarehouse;
        this.tableNamespace = tableNamespace;
        this.tableName = tableName;
        this.batchSize = batchSize;
        this.flushIntervalSeconds = flushIntervalSeconds;
        this.extraCatalogProperties = extraCatalogProperties != null
                ? Collections.unmodifiableMap(new HashMap<>(extraCatalogProperties))
                : Collections.emptyMap();
        this.buffer = new ArrayList<>(batchSize);
    }

    IcebergManager(
            final String name,
            final String catalogName,
            final String catalogImpl,
            final String catalogUri,
            final String catalogWarehouse,
            final String tableNamespace,
            final String tableName,
            final int batchSize,
            final int flushIntervalSeconds) {
        this(name, catalogName, catalogImpl, catalogUri, catalogWarehouse,
                tableNamespace, tableName, batchSize, flushIntervalSeconds, null);
    }

    void startup() {
        if (running) {
            return;
        }
        LOGGER.info("Starting IcebergManager [{}]", name);

        final Map<String, String> catalogProperties = new HashMap<>();
        catalogProperties.put("type", catalogImpl);
        if (catalogUri != null) {
            catalogProperties.put("uri", catalogUri);
        }
        if (catalogWarehouse != null) {
            catalogProperties.put("warehouse", catalogWarehouse);
        }
        catalogProperties.putAll(extraCatalogProperties);

        catalog = CatalogUtil.buildIcebergCatalog(catalogName, catalogProperties, new Configuration());
        final TableIdentifier tableId = TableIdentifier.of(Namespace.of(tableNamespace), tableName);

        if (!((org.apache.iceberg.catalog.SupportsNamespaces) catalog).namespaceExists(Namespace.of(tableNamespace))) {
            ((org.apache.iceberg.catalog.SupportsNamespaces) catalog).createNamespace(Namespace.of(tableNamespace));
        }

        if (!catalog.tableExists(tableId)) {
            table = catalog.createTable(tableId, LOG_SCHEMA, PARTITION_SPEC);
            LOGGER.info("Created Iceberg table {} partitioned by event_date", tableId);
        } else {
            table = catalog.loadTable(tableId);
            validateSchema(table.schema());
            LOGGER.info("Loaded existing Iceberg table {}", tableId);
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread t = new Thread(r, "log4j-iceberg-flush-" + name);
            t.setDaemon(true);
            return t;
        });
        flushTask = scheduler.scheduleAtFixedRate(
                this::flush, flushIntervalSeconds, flushIntervalSeconds, TimeUnit.SECONDS);

        running = true;
    }

    void validateSchema(final Schema existingSchema) {
        final Set<String> required = new HashSet<>();
        for (final Types.NestedField field : LOG_SCHEMA.columns()) {
            required.add(field.name());
        }
        final Set<String> missing = new HashSet<>();
        for (final String fieldName : required) {
            if (existingSchema.findField(fieldName) == null) {
                missing.add(fieldName);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "Iceberg table " + tableNamespace + "." + tableName
                            + " is missing required columns: " + missing
                            + ". Expected schema columns: " + required);
        }
    }

    void write(final LogEvent event) {
        lock.lock();
        try {
            buffer.add(event);
            if (buffer.size() >= batchSize) {
                flushBuffer();
            }
        } finally {
            lock.unlock();
        }
    }

    void flush() {
        lock.lock();
        try {
            flushBuffer();
        } finally {
            lock.unlock();
        }
    }

    private void flushBuffer() {
        if (buffer.isEmpty()) {
            return;
        }
        final List<LogEvent> events = buffer;
        buffer = new ArrayList<>(batchSize);

        final DataFile dataFile;
        try {
            dataFile = writeParquetFile(events);
        } catch (final Exception e) {
            throw new IcebergCommitException(
                    "Failed to write Parquet file for Iceberg table " + tableNamespace + "." + tableName, e);
        }

        commitWithRetry(dataFile, events.size());
    }

    private void commitWithRetry(final DataFile dataFile, final int eventCount) {
        int attempt = 0;
        while (true) {
            try {
                final AppendFiles append = table.newAppend();
                append.appendFile(dataFile);
                append.commit();
                LOGGER.debug("Committed {} log events to Iceberg table {}.{}", eventCount, tableNamespace, tableName);
                return;
            } catch (final Exception e) {
                if (attempt >= MAX_COMMIT_RETRIES) {
                    throw new IcebergCommitException(
                            "Failed to commit to Iceberg table "
                                    + tableNamespace
                                    + "."
                                    + tableName
                                    + " after "
                                    + (MAX_COMMIT_RETRIES + 1)
                                    + " attempts",
                            e);
                }
                final long sleepMs = RETRY_BASE_SLEEP_MS * (1L << attempt);
                LOGGER.warn(
                        "Commit attempt {} failed for Iceberg table {}.{}, retrying in {} ms",
                        attempt + 1,
                        tableNamespace,
                        tableName,
                        sleepMs,
                        e);
                try {
                    Thread.sleep(sleepMs);
                } catch (final InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IcebergCommitException(
                            "Interrupted while retrying commit to Iceberg table " + tableNamespace + "." + tableName,
                            e);
                }
                table.refresh();
                attempt++;
            }
        }
    }

    private DataFile writeParquetFile(final List<LogEvent> events) throws IOException {
        final String filename = String.format(
                "%s/data/%s-%d-%d.parquet",
                table.location(),
                name,
                System.currentTimeMillis(),
                Thread.currentThread().getId());
        final OutputFile outputFile = table.io().newOutputFile(filename);

        try (DataWriter<GenericRecord> writer = Parquet.writeData(outputFile)
                .schema(LOG_SCHEMA)
                .withSpec(PartitionSpec.unpartitioned())
                .createWriterFunc(GenericParquetWriter::create)
                .overwrite()
                .build()) {
            for (final LogEvent event : events) {
                writer.write(toRecord(event));
            }
        }

        return org.apache.iceberg.DataFiles.builder(PartitionSpec.unpartitioned())
                .withPath(filename)
                .withFileSizeInBytes(outputFile.toInputFile().getLength())
                .withRecordCount(events.size())
                .withFormat(org.apache.iceberg.FileFormat.PARQUET)
                .build();
    }

    private GenericRecord toRecord(final LogEvent event) {
        final GenericRecord record = GenericRecord.create(LOG_SCHEMA);
        final Instant instant = Instant.ofEpochMilli(event.getTimeMillis());
        record.setField("timestamp", instant.atOffset(ZoneOffset.UTC));
        record.setField("level", event.getLevel().name());
        record.setField("logger_name", event.getLoggerName());
        record.setField(
                "message", event.getMessage() != null ? event.getMessage().getFormattedMessage() : null);
        record.setField("thread_name", event.getThreadName());
        record.setField("thrown", event.getThrown() != null ? event.getThrown().toString() : null);
        record.setField("event_date", instant.atOffset(ZoneOffset.UTC).toLocalDate());
        return record;
    }

    boolean stop(final long timeout, final TimeUnit timeUnit) {
        if (!running) {
            return true;
        }
        LOGGER.info("Stopping IcebergManager [{}]", name);
        running = false;

        if (flushTask != null) {
            flushTask.cancel(false);
        }
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                scheduler.awaitTermination(timeout, timeUnit);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        flush();

        if (catalog instanceof java.io.Closeable) {
            try {
                ((java.io.Closeable) catalog).close();
            } catch (final IOException e) {
                LOGGER.warn("Error closing Iceberg catalog", e);
            }
        }
        return true;
    }

    String getName() {
        return name;
    }
}
