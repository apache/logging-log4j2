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
package org.apache.logging.log4j.core.appender.db.jdbc;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.Objects;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.db.AbstractDatabaseAppender;
import org.apache.logging.log4j.core.appender.db.ColumnMapping;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.convert.TypeConverter;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.util.Assert;
import org.apache.logging.log4j.core.util.Booleans;

/**
 * This Appender writes logging events to a relational database using standard JDBC mechanisms. It takes a list of
 * {@link ColumnConfig}s and/or {@link ColumnMapping}s with which it determines how to save the event data into the
 * appropriate columns in the table. ColumnMapping is new as of Log4j 2.8 and supports
 * {@linkplain TypeConverter type conversion} and persistence using {@link PreparedStatement#setObject(int, Object)}.
 * A {@link ConnectionSource} plugin instance instructs the appender (and {@link JdbcDatabaseManager}) how to connect to
 * the database. This appender can be reconfigured at run time.
 *
 * @see ColumnConfig
 * @see ColumnMapping
 * @see ConnectionSource
 */
@Plugin(name = "JDBC", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public final class JdbcAppender extends AbstractDatabaseAppender<JdbcDatabaseManager> {

    public static class Builder<B extends Builder<B>> extends AbstractDatabaseAppender.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<JdbcAppender> {

        @PluginElement("ConnectionSource")
        @Required(message = "No ConnectionSource provided")
        private ConnectionSource connectionSource;

        @PluginBuilderAttribute
        private boolean immediateFail;

        @PluginBuilderAttribute
        private int bufferSize;

        @PluginBuilderAttribute
        @Required(message = "No table name provided")
        private String tableName;

        @PluginElement("ColumnConfigs")
        private ColumnConfig[] columnConfigs;

        @PluginElement("ColumnMappings")
        private ColumnMapping[] columnMappings;

        @PluginBuilderAttribute
        private boolean truncateStrings = true;

        // TODO Consider moving up to AbstractDatabaseAppender.Builder.
        @PluginBuilderAttribute
        private long reconnectIntervalMillis = DEFAULT_RECONNECT_INTERVAL_MILLIS;

        @Override
        public JdbcAppender build() {
            if (Assert.isEmpty(columnConfigs) && Assert.isEmpty(columnMappings)) {
                LOGGER.error("Cannot create JdbcAppender without any columns.");
                return null;
            }
            final String managerName = "JdbcManager{name=" + getName() + ", bufferSize=" + bufferSize + ", tableName="
                    + tableName + ", columnConfigs=" + Arrays.toString(columnConfigs) + ", columnMappings="
                    + Arrays.toString(columnMappings) + '}';
            final JdbcDatabaseManager manager = JdbcDatabaseManager.getManager(
                    managerName,
                    bufferSize,
                    getLayout(),
                    connectionSource,
                    tableName,
                    columnConfigs,
                    columnMappings,
                    immediateFail,
                    reconnectIntervalMillis,
                    truncateStrings);
            if (manager == null) {
                return null;
            }
            return new JdbcAppender(
                    getName(), getFilter(), getLayout(), isIgnoreExceptions(), getPropertyArray(), manager);
        }

        public long getReconnectIntervalMillis() {
            return reconnectIntervalMillis;
        }

        public boolean isImmediateFail() {
            return immediateFail;
        }

        /**
         * If an integer greater than 0, this causes the appender to buffer log events and flush whenever the buffer
         * reaches this size.
         *
         * @param bufferSize buffer size.
         *
         * @return this
         */
        public B setBufferSize(final int bufferSize) {
            this.bufferSize = bufferSize;
            return asBuilder();
        }

        /**
         * Information about the columns that log event data should be inserted into and how to insert that data.
         *
         * @param columnConfigs Column configurations.
         *
         * @return this
         */
        public B setColumnConfigs(final ColumnConfig... columnConfigs) {
            this.columnConfigs = columnConfigs;
            return asBuilder();
        }

        public B setColumnMappings(final ColumnMapping... columnMappings) {
            this.columnMappings = columnMappings;
            return asBuilder();
        }

        /**
         * The connections source from which database connections should be retrieved.
         *
         * @param connectionSource The connections source.
         *
         * @return this
         */
        public B setConnectionSource(final ConnectionSource connectionSource) {
            this.connectionSource = connectionSource;
            return asBuilder();
        }

        public void setImmediateFail(final boolean immediateFail) {
            this.immediateFail = immediateFail;
        }

        public void setReconnectIntervalMillis(final long reconnectIntervalMillis) {
            this.reconnectIntervalMillis = reconnectIntervalMillis;
        }

        /**
         * The name of the database table to insert log events into.
         *
         * @param tableName The database table name.
         *
         * @return this
         */
        public B setTableName(final String tableName) {
            this.tableName = tableName;
            return asBuilder();
        }

        public B setTruncateStrings(final boolean truncateStrings) {
            this.truncateStrings = truncateStrings;
            return asBuilder();
        }
    }

    /**
     * Factory method for creating a JDBC appender within the plugin manager.
     *
     * @see Builder
     * @deprecated use {@link #newBuilder()}
     */
    @Deprecated
    public static <B extends Builder<B>> JdbcAppender createAppender(
            final String name,
            final String ignore,
            final Filter filter,
            final ConnectionSource connectionSource,
            final String bufferSize,
            final String tableName,
            final ColumnConfig[] columnConfigs) {
        Assert.requireNonEmpty(name, "Name cannot be empty");
        Objects.requireNonNull(connectionSource, "ConnectionSource cannot be null");
        Assert.requireNonEmpty(tableName, "Table name cannot be empty");
        Assert.requireNonEmpty(columnConfigs, "ColumnConfigs cannot be empty");

        final int bufferSizeInt = AbstractAppender.parseInt(bufferSize, 0);
        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);

        return JdbcAppender.<B>newBuilder()
                .setBufferSize(bufferSizeInt)
                .setColumnConfigs(columnConfigs)
                .setConnectionSource(connectionSource)
                .setTableName(tableName)
                .setName(name)
                .setIgnoreExceptions(ignoreExceptions)
                .setFilter(filter)
                .build();
    }

    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    private final String description;

    private JdbcAppender(
            final String name,
            final Filter filter,
            final Layout<? extends Serializable> layout,
            final boolean ignoreExceptions,
            final Property[] properties,
            final JdbcDatabaseManager manager) {
        super(name, filter, layout, ignoreExceptions, properties, manager);
        this.description = this.getName() + "{ manager=" + this.getManager() + " }";
    }

    @Override
    public String toString() {
        return this.description;
    }
}
