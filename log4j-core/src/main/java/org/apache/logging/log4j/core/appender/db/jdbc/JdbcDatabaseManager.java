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
import java.io.StringReader;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.appender.db.AbstractDatabaseAppender;
import org.apache.logging.log4j.core.appender.db.AbstractDatabaseManager;
import org.apache.logging.log4j.core.appender.db.ColumnMapping;
import org.apache.logging.log4j.core.appender.db.DbAppenderLoggingException;
import org.apache.logging.log4j.core.config.plugins.convert.DateTypeConverter;
import org.apache.logging.log4j.core.config.plugins.convert.TypeConverters;
import org.apache.logging.log4j.core.util.Closer;
import org.apache.logging.log4j.core.util.Log4jThread;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.spi.ThreadContextMap;
import org.apache.logging.log4j.spi.ThreadContextStack;
import org.apache.logging.log4j.util.IndexedReadOnlyStringMap;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.Strings;

/**
 * An {@link AbstractDatabaseManager} implementation for relational databases accessed via JDBC.
 */
public final class JdbcDatabaseManager extends AbstractDatabaseManager {

    /**
     * Encapsulates data that {@link JdbcDatabaseManagerFactory} uses to create managers.
     */
    private static final class FactoryData extends AbstractDatabaseManager.AbstractFactoryData {
        private final ConnectionSource connectionSource;
        private final String tableName;
        private final ColumnConfig[] columnConfigs;
        private final ColumnMapping[] columnMappings;
        private final boolean immediateFail;
        private final boolean retry;
        private final long reconnectIntervalMillis;

        protected FactoryData(final int bufferSize, final Layout<? extends Serializable> layout,
                final ConnectionSource connectionSource, final String tableName, final ColumnConfig[] columnConfigs,
                final ColumnMapping[] columnMappings, final boolean immediateFail, final long reconnectIntervalMillis) {
            super(bufferSize, layout);
            this.connectionSource = connectionSource;
            this.tableName = tableName;
            this.columnConfigs = columnConfigs;
            this.columnMappings = columnMappings;
            this.immediateFail = immediateFail;
            this.retry = reconnectIntervalMillis > 0;
            this.reconnectIntervalMillis = reconnectIntervalMillis;
        }

        @Override
        public String toString() {
            return String.format(
                    "FactoryData [connectionSource=%s, tableName=%s, columnConfigs=%s, columnMappings=%s, immediateFail=%s, retry=%s, reconnectIntervalMillis=%,d]",
                    connectionSource, tableName, Arrays.toString(columnConfigs), Arrays.toString(columnMappings),
                    immediateFail, retry, reconnectIntervalMillis);
        }
    }

    /**
     * Creates managers.
     */
    private static final class JdbcDatabaseManagerFactory implements ManagerFactory<JdbcDatabaseManager, FactoryData> {

        private static final char PARAMETER_MARKER = '?';

        @Override
        public JdbcDatabaseManager createManager(final String name, final FactoryData data) {
            final StringBuilder sb = new StringBuilder("INSERT INTO ").append(data.tableName).append(" (");
            // so this gets a little more complicated now that there are two ways to configure column mappings, but
            // both mappings follow the same exact pattern for the prepared statement
            int i = 1;
            for (final ColumnMapping mapping : data.columnMappings) {
                final String mappingName = mapping.getName();
                logger().trace("Adding INSERT ColumnMapping[{}]: {}={} ", i++, mappingName, mapping);
                sb.append(mappingName).append(',');
            }
            for (final ColumnConfig config : data.columnConfigs) {
                sb.append(config.getColumnName()).append(',');
            }
            // at least one of those arrays is guaranteed to be non-empty
            sb.setCharAt(sb.length() - 1, ')');
            sb.append(" VALUES (");
            i = 1;
            final List<ColumnMapping> columnMappings = new ArrayList<>(data.columnMappings.length);
            for (final ColumnMapping mapping : data.columnMappings) {
                final String mappingName = mapping.getName();
                if (Strings.isNotEmpty(mapping.getLiteralValue())) {
                    logger().trace("Adding INSERT VALUES literal for ColumnMapping[{}]: {}={} ", i, mappingName,
                            mapping.getLiteralValue());
                    sb.append(mapping.getLiteralValue());
                } else if (Strings.isNotEmpty(mapping.getParameter())) {
                    logger().trace("Adding INSERT VALUES parameter for ColumnMapping[{}]: {}={} ", i, mappingName,
                            mapping.getParameter());
                    sb.append(mapping.getParameter());
                    columnMappings.add(mapping);
                } else {
                    logger().trace("Adding INSERT VALUES parameter marker for ColumnMapping[{}]: {}={} ", i,
                            mappingName, PARAMETER_MARKER);
                    sb.append(PARAMETER_MARKER);
                    columnMappings.add(mapping);
                }
                sb.append(',');
                i++;
            }
            final List<ColumnConfig> columnConfigs = new ArrayList<>(data.columnConfigs.length);
            for (final ColumnConfig config : data.columnConfigs) {
                if (Strings.isNotEmpty(config.getLiteralValue())) {
                    sb.append(config.getLiteralValue());
                } else {
                    sb.append(PARAMETER_MARKER);
                    columnConfigs.add(config);
                }
                sb.append(',');
            }
            // at least one of those arrays is guaranteed to be non-empty
            sb.setCharAt(sb.length() - 1, ')');
            final String sqlStatement = sb.toString();

            return new JdbcDatabaseManager(name, sqlStatement, columnConfigs, data);
        }
    }

    /**
     * Handles reconnecting to JDBC once on a Thread.
     */
    private class Reconnector extends Log4jThread {

        private final CountDownLatch latch = new CountDownLatch(1);
        private volatile boolean shutdown = false;

        private Reconnector() {
            super("JdbcDatabaseManager-Reconnector");
        }

        public void latch() {
            try {
                latch.await();
            } catch (final InterruptedException ex) {
                // Ignore the exception.
            }
        }

        void reconnect() throws SQLException {
            closeResources(false);
            connectAndPrepare();
            reconnector = null;
            shutdown = true;
            logger().debug("Connection reestablished to {}", factoryData);
        }

        @Override
        public void run() {
            while (!shutdown) {
                try {
                    sleep(factoryData.reconnectIntervalMillis);
                    reconnect();
                } catch (final InterruptedException | SQLException e) {
                    logger().debug("Cannot reestablish JDBC connection to {}: {}", factoryData, e.getLocalizedMessage(),
                            e);
                } finally {
                    latch.countDown();
                }
            }
        }

        public void shutdown() {
            shutdown = true;
        }

    }

    private static final JdbcDatabaseManagerFactory INSTANCE = new JdbcDatabaseManagerFactory();

    private static JdbcDatabaseManagerFactory getFactory() {
        return INSTANCE;
    }

    /**
     * Creates a JDBC manager for use within the {@link JdbcAppender}, or returns a suitable one if it already exists.
     *
     * @param name The name of the manager, which should include connection details and hashed passwords where possible.
     * @param bufferSize The size of the log event buffer.
     * @param connectionSource The source for connections to the database.
     * @param tableName The name of the database table to insert log events into.
     * @param columnConfigs Configuration information about the log table columns.
     * @return a new or existing JDBC manager as applicable.
     * @deprecated use
     * {@link #getManager(String, int, Layout, ConnectionSource, String, ColumnConfig[], ColumnMapping[], boolean, long)}
     */
    @Deprecated
    public static JdbcDatabaseManager getJDBCDatabaseManager(final String name, final int bufferSize,
            final ConnectionSource connectionSource, final String tableName, final ColumnConfig[] columnConfigs) {
        return getManager(
                name, new FactoryData(bufferSize, null, connectionSource, tableName, columnConfigs,
                        new ColumnMapping[0], false, AbstractDatabaseAppender.DEFAULT_RECONNECT_INTERVAL_MILLIS),
                getFactory());
    }

    /**
     * Creates a JDBC manager for use within the {@link JdbcAppender}, or returns a suitable one if it already exists.
     *
     * @param name The name of the manager, which should include connection details and hashed passwords where possible.
     * @param bufferSize The size of the log event buffer.
     * @param connectionSource The source for connections to the database.
     * @param tableName The name of the database table to insert log events into.
     * @param columnConfigs Configuration information about the log table columns.
     * @param columnMappings column mapping configuration (including type conversion).
     * @return a new or existing JDBC manager as applicable.
     * @deprecated use
     * {@link #getManager(String, int, Layout, ConnectionSource, String, ColumnConfig[], ColumnMapping[], boolean, long)}
     */
    @Deprecated
    public static JdbcDatabaseManager getManager(final String name, final int bufferSize,
            final ConnectionSource connectionSource, final String tableName, final ColumnConfig[] columnConfigs,
            final ColumnMapping[] columnMappings) {
        return getManager(name, new FactoryData(bufferSize, null, connectionSource, tableName, columnConfigs,
                columnMappings, false, AbstractDatabaseAppender.DEFAULT_RECONNECT_INTERVAL_MILLIS), getFactory());
    }

    /**
     * Creates a JDBC manager for use within the {@link JdbcAppender}, or returns a suitable one if it already exists.
     *
     * @param name The name of the manager, which should include connection details and hashed passwords where possible.
     * @param bufferSize The size of the log event buffer.
     * @param layout The Appender-level layout
     * @param connectionSource The source for connections to the database.
     * @param tableName The name of the database table to insert log events into.
     * @param columnConfigs Configuration information about the log table columns.
     * @param columnMappings column mapping configuration (including type conversion).
     * @return a new or existing JDBC manager as applicable.
     */
    @Deprecated
    public static JdbcDatabaseManager getManager(final String name, final int bufferSize,
            final Layout<? extends Serializable> layout, final ConnectionSource connectionSource,
            final String tableName, final ColumnConfig[] columnConfigs, final ColumnMapping[] columnMappings) {
        return getManager(name, new FactoryData(bufferSize, layout, connectionSource, tableName, columnConfigs,
                columnMappings, false, AbstractDatabaseAppender.DEFAULT_RECONNECT_INTERVAL_MILLIS), getFactory());
    }

    /**
     * Creates a JDBC manager for use within the {@link JdbcAppender}, or returns a suitable one if it already exists.
     *
     * @param name The name of the manager, which should include connection details and hashed passwords where possible.
     * @param bufferSize The size of the log event buffer.
     * @param layout The Appender-level layout
     * @param connectionSource The source for connections to the database.
     * @param tableName The name of the database table to insert log events into.
     * @param columnConfigs Configuration information about the log table columns.
     * @param columnMappings column mapping configuration (including type conversion).
     * @param immediateFail Whether or not to fail immediately with a {@link AppenderLoggingException} when connecting
     * to JDBC fails.
     * @param reconnectIntervalMillis How often to reconnect to the database when a SQL exception is detected.
     * @return a new or existing JDBC manager as applicable.
     */
    public static JdbcDatabaseManager getManager(final String name, final int bufferSize,
            final Layout<? extends Serializable> layout, final ConnectionSource connectionSource,
            final String tableName, final ColumnConfig[] columnConfigs, final ColumnMapping[] columnMappings,
            final boolean immediateFail, final long reconnectIntervalMillis) {
        return getManager(name, new FactoryData(bufferSize, layout, connectionSource, tableName, columnConfigs,
                columnMappings, immediateFail, reconnectIntervalMillis), getFactory());
    }

    // NOTE: prepared statements are prepared in this order: column mappings, then column configs
    private final List<ColumnConfig> columnConfigs;
    private final String sqlStatement;
    private final FactoryData factoryData;
    private volatile Connection connection;
    private volatile PreparedStatement statement;
    private volatile Reconnector reconnector;
    private volatile boolean isBatchSupported;

    private JdbcDatabaseManager(final String name, final String sqlStatement, final List<ColumnConfig> columnConfigs,
            final FactoryData factoryData) {
        super(name, factoryData.getBufferSize());
        this.sqlStatement = sqlStatement;
        this.columnConfigs = columnConfigs;
        this.factoryData = factoryData;
    }

    private void checkConnection() {
        boolean connClosed = true;
        try {
            connClosed = this.connection == null || this.connection.isClosed();
        } catch (final SQLException e) {
            // Be quiet
        }
        boolean stmtClosed = true;
        try {
            stmtClosed = this.statement == null || this.statement.isClosed();
        } catch (final SQLException e) {
            // Be quiet
        }
        if (!this.isRunning() || connClosed || stmtClosed) {
            // If anything is closed, close it all down before we reconnect
            closeResources(false);
            // Reconnect
            if (reconnector != null && !factoryData.immediateFail) {
                reconnector.latch();
                if (connection == null) {
                    throw new AppenderLoggingException(
                            "Error writing to JDBC Manager '" + getName() + "': JDBC connection not available.");
                }
                if (statement == null) {
                    throw new AppenderLoggingException(
                            "Error writing to JDBC Manager '" + getName() + "': JDBC statement not available.");
                }
            }
        }
    }

    protected void closeResources(boolean logExceptions) {
        try {
            // Closing a statement returns it to the pool when using Apache Commons DBCP.
            // Closing an already closed statement has no effect.
            Closer.close(this.statement);
        } catch (final Exception e) {
            if (logExceptions) {
                logWarn("Failed to close SQL statement logging event or flushing buffer", e);
            }
        } finally {
            this.statement = null;
        }

        try {
            // Closing a connection returns it to the pool when using Apache Commons DBCP.
            // Closing an already closed connection has no effect.
            Closer.close(this.connection);
        } catch (final Exception e) {
            if (logExceptions) {
                logWarn("Failed to close database connection logging event or flushing buffer", e);
            }
        } finally {
            this.connection = null;
        }
    }

    @Override
    protected boolean commitAndClose() {
        boolean closed = true;
        try {
            if (this.connection != null && !this.connection.isClosed()) {
                if (this.isBatchSupported && this.statement != null) {
                    logger().debug("Executing batch PreparedStatement {}", this.statement);
                    final int[] result = this.statement.executeBatch();
                    logger().debug("Batch result: {}", Arrays.toString(result));
                }
                logger().debug("Committing Connection {}", this.connection);
                this.connection.commit();
            }
        } catch (final SQLException e) {
            throw new DbAppenderLoggingException("Failed to commit transaction logging event or flushing buffer.", e);
        } finally {
            closeResources(true);
        }
        return closed;
    }

    private boolean commitAndCloseAll() {
        if (this.connection != null || this.statement != null) {
            try {
                this.commitAndClose();
                return true;
            } catch (AppenderLoggingException e) {
                // Database connection has likely gone stale.
                final Throwable cause = e.getCause();
                final Throwable actual = cause == null ? e : cause;
                logger().debug("{} committing and closing connection: {}", actual, actual.getClass().getSimpleName(),
                        e.toString(), e);
            }
        }
        if (factoryData.connectionSource != null) {
            factoryData.connectionSource.stop();
        }
        return true;
    }

    private void connectAndPrepare() throws SQLException {
        logger().debug("Acquiring JDBC connection from {}", this.getConnectionSource());
        this.connection = getConnectionSource().getConnection();
        logger().debug("Acquired JDBC connection {}", this.connection);
        logger().debug("Getting connection metadata {}", this.connection);
        final DatabaseMetaData metaData = this.connection.getMetaData();
        logger().debug("Connection metadata {}", metaData);
        this.isBatchSupported = metaData.supportsBatchUpdates();
        logger().debug("Connection supportsBatchUpdates: {}", this.isBatchSupported);
        this.connection.setAutoCommit(false);
        logger().debug("Preparing SQL {}", this.sqlStatement);
        this.statement = this.connection.prepareStatement(this.sqlStatement);
        logger().debug("Prepared SQL {}", this.statement);
    }

    @Override
    protected void connectAndStart() {
        checkConnection();
        synchronized (this) {
            try {
                connectAndPrepare();
            } catch (final SQLException e) {
                reconnectOn(e);
            }
        }
    }

    private Reconnector createReconnector() {
        final Reconnector recon = new Reconnector();
        recon.setDaemon(true);
        recon.setPriority(Thread.MIN_PRIORITY);
        return recon;
    }

    public ConnectionSource getConnectionSource() {
        return factoryData.connectionSource;
    }

    public String getSqlStatement() {
        return sqlStatement;
    }

    public String getTableName() {
        return factoryData.tableName;
    }

    private void reconnectOn(final Exception exception) {
        if (!factoryData.retry) {
            throw new AppenderLoggingException("Cannot connect and prepare", exception);
        }
        if (reconnector == null) {
            reconnector = createReconnector();
            try {
                reconnector.reconnect();
            } catch (final SQLException reconnectEx) {
                logger().debug("Cannot reestablish JDBC connection to {}: {}; starting reconnector thread {}",
                        factoryData, reconnectEx, reconnector.getName(), reconnectEx);
                reconnector.start();
                reconnector.latch();
                if (connection == null || statement == null) {
                    throw new AppenderLoggingException(
                            String.format("Error sending to %s for %s", getName(), factoryData), exception);
                }
            }
        }
    }

    private void setFields(final MapMessage<?, ?> mapMessage) throws SQLException {
        final IndexedReadOnlyStringMap map = mapMessage.getIndexedReadOnlyStringMap();
        final String simpleName = statement.getClass().getName();
        int i = 1; // JDBC indices start at 1
        for (final ColumnMapping mapping : this.factoryData.columnMappings) {
            if (mapping.getLiteralValue() == null) {
                final String source = mapping.getSource();
                final String key = Strings.isEmpty(source) ? mapping.getName() : source;
                final Object value = map.getValue(key);
                if (logger().isTraceEnabled()) {
                    final String valueStr = value instanceof String ? "\"" + value + "\""
                            : Objects.toString(value, null);
                    logger().trace("{} setObject({}, {}) for key '{}' and mapping '{}'", simpleName, i, valueStr, key,
                            mapping.getName());
                }
                statement.setObject(i++, value);
            }
        }
    }

    @Override
    protected boolean shutdownInternal() {
        if (reconnector != null) {
            reconnector.shutdown();
            reconnector.interrupt();
            reconnector = null;
        }
        return commitAndCloseAll();
    }

    @Override
    protected void startupInternal() throws Exception {
        // empty
    }

    @Override
    protected void writeInternal(final LogEvent event, final Serializable serializable) {
        StringReader reader = null;
        try {
            if (!this.isRunning() || this.connection == null || this.connection.isClosed() || this.statement == null
                    || this.statement.isClosed()) {
                throw new AppenderLoggingException(
                        "Cannot write logging event; JDBC manager not connected to the database.");
            }
            // Clear in case there are leftovers.
            statement.clearParameters();
            if (serializable instanceof MapMessage) {
                setFields((MapMessage<?, ?>) serializable);
            }
            int i = 1; // JDBC indices start at 1
            for (final ColumnMapping mapping : this.factoryData.columnMappings) {
                if (ThreadContextMap.class.isAssignableFrom(mapping.getType())
                        || ReadOnlyStringMap.class.isAssignableFrom(mapping.getType())) {
                    this.statement.setObject(i++, event.getContextData().toMap());
                } else if (ThreadContextStack.class.isAssignableFrom(mapping.getType())) {
                    this.statement.setObject(i++, event.getContextStack().asList());
                } else if (Date.class.isAssignableFrom(mapping.getType())) {
                    this.statement.setObject(i++, DateTypeConverter.fromMillis(event.getTimeMillis(),
                            mapping.getType().asSubclass(Date.class)));
                } else {
                    final StringLayout layout = mapping.getLayout();
                    if (layout != null) {
                        if (Clob.class.isAssignableFrom(mapping.getType())) {
                            this.statement.setClob(i++, new StringReader(layout.toSerializable(event)));
                        } else if (NClob.class.isAssignableFrom(mapping.getType())) {
                            this.statement.setNClob(i++, new StringReader(layout.toSerializable(event)));
                        } else {
                            final Object value = TypeConverters.convert(layout.toSerializable(event), mapping.getType(),
                                    null);
                            if (value == null) {
                                this.statement.setNull(i++, Types.NULL);
                            } else {
                                this.statement.setObject(i++, value);
                            }
                        }
                    }
                }
            }
            for (final ColumnConfig column : this.columnConfigs) {
                if (column.isEventTimestamp()) {
                    this.statement.setTimestamp(i++, new Timestamp(event.getTimeMillis()));
                } else if (column.isClob()) {
                    reader = new StringReader(column.getLayout().toSerializable(event));
                    if (column.isUnicode()) {
                        this.statement.setNClob(i++, reader);
                    } else {
                        this.statement.setClob(i++, reader);
                    }
                } else if (column.isUnicode()) {
                    this.statement.setNString(i++, column.getLayout().toSerializable(event));
                } else {
                    this.statement.setString(i++, column.getLayout().toSerializable(event));
                }
            }

            if (this.isBatchSupported) {
                this.statement.addBatch();
            } else if (this.statement.executeUpdate() == 0) {
                throw new AppenderLoggingException(
                        "No records inserted in database table for log event in JDBC manager.");
            }
        } catch (final SQLException e) {
            throw new DbAppenderLoggingException(
                    "Failed to insert record for log event in JDBC manager: " + e.getMessage(), e);
        } finally {
            // Release ASAP
            try {
                statement.clearParameters();
            } catch (final SQLException e) {
                // Ignore
            }
            Closer.closeSilently(reader);
        }
    }

    @Override
    protected void writeThrough(final LogEvent event, final Serializable serializable) {
        this.connectAndStart();
        try {
            try {
                this.writeInternal(event, serializable);
            } finally {
                this.commitAndClose();
            }
        } catch (DbAppenderLoggingException e) {
            reconnectOn(e);
            try {
                this.writeInternal(event, serializable);
            } finally {
                this.commitAndClose();
            }
        }
    }

}
