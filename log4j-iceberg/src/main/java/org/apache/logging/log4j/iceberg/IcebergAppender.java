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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

/**
 * Log4j appender that writes log events as rows in an Apache Iceberg table
 * backed by Parquet data files.
 *
 * <p>The table is partitioned by {@code event_date} (day granularity) for efficient
 * time-range queries and data lifecycle management.</p>
 *
 * <p>The {@code catalogImpl} attribute accepts either a short type name
 * ({@code "hadoop"}, {@code "hive"}, {@code "rest"}) or a fully qualified class name
 * (e.g. {@code "org.apache.iceberg.rest.RESTCatalog"}).</p>
 *
 * <p>Additional catalog properties (e.g. S3 credentials, REST auth headers) can be
 * passed via nested {@code <Property>} elements under a {@code <CatalogProperties>}
 * wrapper.</p>
 *
 * <p>Configuration example:</p>
 * <pre>
 * &lt;Iceberg name="IcebergAppender"
 *          catalogName="my_catalog"
 *          catalogImpl="rest"
 *          catalogUri="http://localhost:8181"
 *          catalogWarehouse="s3://my-bucket/warehouse"
 *          tableNamespace="logs"
 *          tableName="app_logs"
 *          batchSize="1000"
 *          flushIntervalSeconds="30"&gt;
 *   &lt;CatalogProperties&gt;
 *     &lt;Property name="s3.access-key-id"&gt;AKIA...&lt;/Property&gt;
 *     &lt;Property name="s3.secret-access-key"&gt;secret&lt;/Property&gt;
 *   &lt;/CatalogProperties&gt;
 * &lt;/Iceberg&gt;
 * </pre>
 */
@Plugin(name = "Iceberg", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class IcebergAppender extends AbstractAppender {

    private final IcebergManager manager;

    private IcebergAppender(
            final String name,
            final Filter filter,
            final boolean ignoreExceptions,
            final Property[] properties,
            final IcebergManager manager) {
        super(name, filter, null, ignoreExceptions, properties);
        this.manager = manager;
    }

    @Override
    public void append(final LogEvent event) {
        manager.write(event.toImmutable());
    }

    @Override
    public void start() {
        manager.startup();
        super.start();
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        boolean stopped = super.stop(timeout, timeUnit, false);
        stopped &= manager.stop(timeout, timeUnit);
        setStopped();
        return stopped;
    }

    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    public static class Builder<B extends Builder<B>> extends AbstractAppender.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<IcebergAppender> {

        @PluginBuilderAttribute
        private String catalogName = "log4j";

        @PluginBuilderAttribute
        @Required(message = "No catalog implementation provided (use 'hadoop', 'hive', 'rest', or a fully qualified class name)")
        private String catalogImpl;

        @PluginBuilderAttribute
        private String catalogUri;

        @PluginBuilderAttribute
        private String catalogWarehouse;

        @PluginBuilderAttribute
        private String tableNamespace = "default";

        @PluginBuilderAttribute
        @Required(message = "No Iceberg table name provided")
        private String tableName;

        @PluginBuilderAttribute
        private int batchSize = 1000;

        @PluginBuilderAttribute
        private int flushIntervalSeconds = 30;

        @PluginElement("CatalogProperties")
        private Property[] catalogProperties;

        public B setCatalogName(final String catalogName) {
            this.catalogName = catalogName;
            return asBuilder();
        }

        public B setCatalogImpl(final String catalogImpl) {
            this.catalogImpl = catalogImpl;
            return asBuilder();
        }

        public B setCatalogUri(final String catalogUri) {
            this.catalogUri = catalogUri;
            return asBuilder();
        }

        public B setCatalogWarehouse(final String catalogWarehouse) {
            this.catalogWarehouse = catalogWarehouse;
            return asBuilder();
        }

        public B setTableNamespace(final String tableNamespace) {
            this.tableNamespace = tableNamespace;
            return asBuilder();
        }

        public B setTableName(final String tableName) {
            this.tableName = tableName;
            return asBuilder();
        }

        public B setBatchSize(final int batchSize) {
            this.batchSize = batchSize;
            return asBuilder();
        }

        public B setFlushIntervalSeconds(final int flushIntervalSeconds) {
            this.flushIntervalSeconds = flushIntervalSeconds;
            return asBuilder();
        }

        public B setCatalogProperties(final Property[] catalogProperties) {
            this.catalogProperties = catalogProperties;
            return asBuilder();
        }

        @Override
        public IcebergAppender build() {
            final Map<String, String> extraProperties = new HashMap<>();
            if (catalogProperties != null) {
                for (final Property prop : catalogProperties) {
                    extraProperties.put(prop.getName(), prop.getValue());
                }
            }
            final IcebergManager manager = new IcebergManager(
                    getName(),
                    catalogName,
                    catalogImpl,
                    catalogUri,
                    catalogWarehouse,
                    tableNamespace,
                    tableName,
                    batchSize,
                    flushIntervalSeconds,
                    extraProperties);
            return new IcebergAppender(getName(), getFilter(), isIgnoreExceptions(), null, manager);
        }
    }
}
