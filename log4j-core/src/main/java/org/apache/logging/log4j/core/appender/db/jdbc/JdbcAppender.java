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
package org.apache.logging.log4j.core.appender.db.jdbc;

import java.io.Serializable;
import java.nio.charset.Charset;
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

    private final String description;

    private JdbcAppender(final String name, final Filter filter, final boolean ignoreExceptions,
                         final JdbcDatabaseManager manager) {
        super(name, filter, ignoreExceptions, manager);
        this.description = this.getName() + "{ manager=" + this.getManager() + " }";
    }

    @Override
    public String toString() {
        return this.description;
    }

    /**
     * Factory method for creating a JDBC appender within the plugin manager.
     *
     * @see Builder
     * @deprecated use {@link #newBuilder()}
     */
    @Deprecated
    public static <B extends Builder<B>> JdbcAppender createAppender(final String name, final String ignore,
                                                                     final Filter filter,
                                                                     final ConnectionSource connectionSource,
                                                                     final String bufferSize, final String tableName,
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
            .withName(name)
            .withIgnoreExceptions(ignoreExceptions)
            .withFilter(filter)
            .build();
    }

    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    public static class Builder<B extends Builder<B>> extends AbstractAppender.Builder<B>
        implements org.apache.logging.log4j.core.util.Builder<JdbcAppender> {

        @PluginElement("ConnectionSource")
        @Required(message = "No ConnectionSource provided")
        private ConnectionSource connectionSource;

        @PluginBuilderAttribute
        private int bufferSize;

        @PluginBuilderAttribute
        @Required(message = "No table name provided")
        private String tableName;

        @PluginElement("ColumnConfigs")
        private ColumnConfig[] columnConfigs;

        @PluginElement("ColumnMappings")
        private ColumnMapping[] columnMappings;

        /**
         * The connections source from which database connections should be retrieved.
         */
        public B setConnectionSource(final ConnectionSource connectionSource) {
            this.connectionSource = connectionSource;
            return asBuilder();
        }

        /**
         * If an integer greater than 0, this causes the appender to buffer log events and flush whenever the buffer
         * reaches this size.
         */
        public B setBufferSize(final int bufferSize) {
            this.bufferSize = bufferSize;
            return asBuilder();
        }

        /**
         * The name of the database table to insert log events into.
         */
        public B setTableName(final String tableName) {
            this.tableName = tableName;
            return asBuilder();
        }

        /**
         * Information about the columns that log event data should be inserted into and how to insert that data.
         */
        public B setColumnConfigs(final ColumnConfig... columnConfigs) {
            this.columnConfigs = columnConfigs;
            return asBuilder();
        }

        public B setColumnMappings(final ColumnMapping... columnMappings) {
            this.columnMappings = columnMappings;
            return asBuilder();
        }

        @Override
        public JdbcAppender build() {
            if (Assert.isEmpty(columnConfigs) && Assert.isEmpty(columnMappings)) {
                LOGGER.error("Cannot create JdbcAppender without any columns configured.");
                return null;
            }
            final String managerName = "JdbcManager{name=" + getName() + ", bufferSize=" + bufferSize + ", tableName=" +
                tableName + ", columnConfigs=" + Arrays.toString(columnConfigs) + ", columnMappings=" +
                Arrays.toString(columnMappings) + '}';
            final JdbcDatabaseManager manager = JdbcDatabaseManager.getManager(managerName, bufferSize,
                connectionSource, tableName, columnConfigs, columnMappings);
            if (manager == null) {
                return null;
            }
            return new JdbcAppender(getName(), getFilter(), isIgnoreExceptions(), manager);
        }

        @Override
        @Deprecated
        public Layout<? extends Serializable> getLayout() {
            throw new UnsupportedOperationException();
        }

        @Override
        @Deprecated
        public B withLayout(final Layout<? extends Serializable> layout) {
            throw new UnsupportedOperationException();
        }

        @Override
        @Deprecated
        public Layout<? extends Serializable> getOrCreateLayout() {
            throw new UnsupportedOperationException();
        }

        @Override
        @Deprecated
        public Layout<? extends Serializable> getOrCreateLayout(final Charset charset) {
            throw new UnsupportedOperationException();
        }
    }
}
