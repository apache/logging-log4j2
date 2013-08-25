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

import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.appender.db.AbstractDatabaseManager;
import org.apache.logging.log4j.core.helpers.Closer;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * An {@link AbstractDatabaseManager} implementation for relational databases accessed via JDBC.
 */
public final class JDBCDatabaseManager extends AbstractDatabaseManager {
    private static final JDBCDatabaseManagerFactory FACTORY = new JDBCDatabaseManagerFactory();

    private final List<Column> columns;
    private final ConnectionSource connectionSource;
    private final String sqlStatement;

    private Connection connection;
    private PreparedStatement statement;

    private JDBCDatabaseManager(final String name, final int bufferSize, final ConnectionSource connectionSource,
                                final String sqlStatement, final List<Column> columns) {
        super(name, bufferSize);
        this.connectionSource = connectionSource;
        this.sqlStatement = sqlStatement;
        this.columns = columns;
    }

    @Override
    protected void connectInternal() throws SQLException {
        this.connection = this.connectionSource.getConnection();
        this.statement = this.connection.prepareStatement(this.sqlStatement);
    }

    @Override
    protected void disconnectInternal() throws SQLException {
        try {
            Closer.close(this.statement);
        } finally {
            Closer.close(this.connection);
        }
    }

    @Override
    protected void writeInternal(final LogEvent event) {
        StringReader reader = null;
        try {
            if (!this.isConnected() || this.connection == null || this.connection.isClosed()) {
                throw new AppenderLoggingException(
                        "Cannot write logging event; JDBC manager not connected to the database.");
            }

            int i = 1;
            for (final Column column : this.columns) {
                if (column.isEventTimestamp) {
                    this.statement.setTimestamp(i++, new Timestamp(event.getMillis()));
                } else {
                    if (column.isClob) {
                        reader = new StringReader(column.layout.toSerializable(event));
                        if (column.isUnicode) {
                            this.statement.setNClob(i++, reader);
                        } else {
                            this.statement.setClob(i++, reader);
                        }
                    } else {
                        if (column.isUnicode) {
                            this.statement.setNString(i++, column.layout.toSerializable(event));
                        } else {
                            this.statement.setString(i++, column.layout.toSerializable(event));
                        }
                    }
                }
            }

            if (this.statement.executeUpdate() == 0) {
                throw new AppenderLoggingException(
                        "No records inserted in database table for log event in JDBC manager.");
            }
        } catch (final SQLException e) {
            throw new AppenderLoggingException("Failed to insert record for log event in JDBC manager: " +
                    e.getMessage(), e);
        } finally {
            Closer.closeSilent(reader);
        }
    }

    /**
     * Creates a JDBC manager for use within the {@link JDBCAppender}, or returns a suitable one if it already exists.
     *
     * @param name The name of the manager, which should include connection details and hashed passwords where possible.
     * @param bufferSize The size of the log event buffer.
     * @param connectionSource The source for connections to the database.
     * @param tableName The name of the database table to insert log events into.
     * @param columnConfigs Configuration information about the log table columns.
     * @return a new or existing JDBC manager as applicable.
     */
    public static JDBCDatabaseManager getJDBCDatabaseManager(final String name, final int bufferSize,
                                                             final ConnectionSource connectionSource,
                                                             final String tableName,
                                                             final ColumnConfig[] columnConfigs) {

        return AbstractDatabaseManager.getManager(
                name, new FactoryData(bufferSize, connectionSource, tableName, columnConfigs), FACTORY
        );
    }

    /**
     * Encapsulates data that {@link JDBCDatabaseManagerFactory} uses to create managers.
     */
    private static final class FactoryData extends AbstractDatabaseManager.AbstractFactoryData {
        private final ColumnConfig[] columnConfigs;
        private final ConnectionSource connectionSource;
        private final String tableName;

        protected FactoryData(final int bufferSize, final ConnectionSource connectionSource, final String tableName,
                              final ColumnConfig[] columnConfigs) {
            super(bufferSize);
            this.connectionSource = connectionSource;
            this.tableName = tableName;
            this.columnConfigs = columnConfigs;
        }
    }

    /**
     * Creates managers.
     */
    private static final class JDBCDatabaseManagerFactory implements ManagerFactory<JDBCDatabaseManager, FactoryData> {
        @Override
        public JDBCDatabaseManager createManager(final String name, final FactoryData data) {
            final StringBuilder columnPart = new StringBuilder();
            final StringBuilder valuePart = new StringBuilder();
            final List<Column> columns = new ArrayList<Column>();
            int i = 0;
            for (final ColumnConfig config : data.columnConfigs) {
                if (i++ > 0) {
                    columnPart.append(',');
                    valuePart.append(',');
                }

                columnPart.append(config.getColumnName());

                if (config.getLiteralValue() != null) {
                    valuePart.append(config.getLiteralValue());
                } else {
                    columns.add(new Column(
                            config.getLayout(), config.isEventTimestamp(), config.isUnicode(), config.isClob()
                    ));
                    valuePart.append('?');
                }
            }

            final String sqlStatement = "INSERT INTO " + data.tableName + " (" + columnPart + ") VALUES (" +
                    valuePart + ")";

            return new JDBCDatabaseManager(name, data.getBufferSize(), data.connectionSource, sqlStatement, columns);
        }
    }

    /**
     * Encapsulates information about a database column and how to persist data to it.
     */
    private static final class Column {
        private final PatternLayout layout;
        private final boolean isEventTimestamp;
        private final boolean isUnicode;
        private final boolean isClob;

        private Column(final PatternLayout layout, final boolean isEventDate, final boolean isUnicode,
                       final boolean isClob) {
            this.layout = layout;
            this.isEventTimestamp = isEventDate;
            this.isUnicode = isUnicode;
            this.isClob = isClob;
        }
    }
}
