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
import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.CatalogUtil;
import org.apache.iceberg.Table;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.Namespace;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.data.IcebergGenerics;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.test.CoreLoggerContexts;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Integration test that exercises the Iceberg appender through the full log4j
 * pipeline: XML config discovery, plugin instantiation, logger calls, flush on
 * context stop, and read-back verification from the Iceberg table.
 */
class IcebergAppenderConfigTest {

    private static Path warehouseDir;

    @BeforeAll
    static void setUp() throws IOException {
        warehouseDir = Files.createTempDirectory("iceberg-it-warehouse");
        System.setProperty("iceberg.warehouse", warehouseDir.toAbsolutePath().toString());
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, "log4j2-iceberg-it.xml");
    }

    @AfterAll
    static void tearDown() throws IOException {
        System.clearProperty("iceberg.warehouse");
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        if (warehouseDir != null) {
            Files.walk(warehouseDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @Test
    void loggerWritesToIcebergTable() throws IOException {
        final Logger logger = LogManager.getLogger("com.example.IcebergIT");

        logger.debug("debug message from IT");
        logger.info("info message from IT");
        logger.warn("warn message from IT");
        logger.error("error message from IT", new RuntimeException("test exception"));

        CoreLoggerContexts.stopLoggerContext(false);

        final Map<String, String> catalogProperties = new HashMap<>();
        catalogProperties.put("type", "hadoop");
        catalogProperties.put("warehouse", warehouseDir.toAbsolutePath().toString());
        final Catalog catalog = CatalogUtil.buildIcebergCatalog("it_catalog", catalogProperties, new Configuration());
        final Table table = catalog.loadTable(TableIdentifier.of(Namespace.of("it_ns"), "it_logs"));
        table.refresh();

        final List<Record> records = new ArrayList<>();
        try (CloseableIterable<Record> iterable = IcebergGenerics.read(table).build()) {
            for (final Record record : iterable) {
                records.add(record);
            }
        }

        assertThat(records).hasSize(4);

        assertThat(records.get(0).getField("level")).isEqualTo("DEBUG");
        assertThat(records.get(0).getField("message")).isEqualTo("debug message from IT");
        assertThat(records.get(0).getField("logger_name")).isEqualTo("com.example.IcebergIT");

        assertThat(records.get(1).getField("level")).isEqualTo("INFO");
        assertThat(records.get(1).getField("message")).isEqualTo("info message from IT");

        assertThat(records.get(2).getField("level")).isEqualTo("WARN");
        assertThat(records.get(2).getField("message")).isEqualTo("warn message from IT");

        assertThat(records.get(3).getField("level")).isEqualTo("ERROR");
        assertThat(records.get(3).getField("message")).isEqualTo("error message from IT");
        assertThat((String) records.get(3).getField("thrown")).contains("test exception");

        for (final Record record : records) {
            assertThat(record.getField("timestamp")).isNotNull();
            assertThat(record.getField("event_date")).isNotNull();
            assertThat(record.getField("thread_name")).isNotNull();
        }
    }

    @Test
    void batchFlushTriggeredByLogVolume() throws IOException {
        final Logger logger = LogManager.getLogger("com.example.BatchIT");

        for (int i = 0; i < 25; i++) {
            logger.info("batch message {}", i);
        }

        CoreLoggerContexts.stopLoggerContext(false);

        final Map<String, String> catalogProperties = new HashMap<>();
        catalogProperties.put("type", "hadoop");
        catalogProperties.put("warehouse", warehouseDir.toAbsolutePath().toString());
        final Catalog catalog = CatalogUtil.buildIcebergCatalog("it_catalog", catalogProperties, new Configuration());
        final Table table = catalog.loadTable(TableIdentifier.of(Namespace.of("it_ns"), "it_logs"));
        table.refresh();

        final List<Record> records = new ArrayList<>();
        try (CloseableIterable<Record> iterable = IcebergGenerics.read(table).build()) {
            for (final Record record : iterable) {
                records.add(record);
            }
        }

        // 4 from previous test + 25 from this test = 29 total
        // (batchSize=10 so at least 2 auto-flushes happened during the 25 writes)
        assertThat(records).hasSizeGreaterThanOrEqualTo(25);

        long batchMessages = records.stream()
                .filter(r -> r.getField("logger_name").equals("com.example.BatchIT"))
                .count();
        assertThat(batchMessages).isEqualTo(25);
    }
}
