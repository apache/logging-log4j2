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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.io.StringReader;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLTransactionRollbackException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    static final class FactoryData extends AbstractDatabaseManager.AbstractFactoryData {
        private final ConnectionSource connectionSource;
        private final String tableName;
        private final ColumnConfig[] columnConfigs;
        final ColumnMapping[] columnMappings;
        private final boolean immediateFail;
        private final boolean retry;
        private final long reconnectIntervalMillis;
        private final boolean truncateStrings;

        protected FactoryData(
                final int bufferSize,
                final Layout<? extends Serializable> layout,
                final ConnectionSource connectionSource,
                final String tableName,
                final ColumnConfig[] columnConfigs,
                final ColumnMapping[] columnMappings,
                final boolean immediateFail,
                final long reconnectIntervalMillis,
                final boolean truncateStrings) {
            super(bufferSize, layout);
            this.connectionSource = connectionSource;
            this.tableName = tableName;
            this.columnConfigs = columnConfigs;
            this.columnMappings = columnMappings;
            this.immediateFail = immediateFail;
            this.retry = reconnectIntervalMillis > 0;
            this.reconnectIntervalMillis = reconnectIntervalMillis;
            this.truncateStrings = truncateStrings;
        }

        @Override
        public String toString() {
            return String.format(
                    "FactoryData [connectionSource=%s, tableName=%s, columnConfigs=%s, columnMappings=%s, immediateFail=%s, retry=%s, reconnectIntervalMillis=%s, truncateStrings=%s]",
                    connectionSource,
                    tableName,
                    Arrays.toString(columnConfigs),
                    Arrays.toString(columnMappings),
                    immediateFail,
                    retry,
                    reconnectIntervalMillis,
                    truncateStrings);
        }
    }

    /**
     * Creates managers.
     */
    private static final class JdbcDatabaseManagerFactory implements ManagerFactory<JdbcDatabaseManager, FactoryData> {

        private static final char PARAMETER_MARKER = '?';

        @Override
        public JdbcDatabaseManager createManager(final String name, final FactoryData data) {
            final StringBuilder sb =
                    new StringBuilder("insert into ").append(data.tableName).append(" (");
            // so this gets a little more complicated now that there are two ways to configure column mappings, but
            // both mappings follow the same exact pattern for the prepared statement
            appendColumnNames("INSERT", data, sb);
            sb.append(") values (");
            int i = 1;
            if (data.columnMappings != null) {
                for (final ColumnMapping mapping : data.columnMappings) {
                    final String mappingName = mapping.getName();
                    if (Strings.isNotEmpty(mapping.getLiteralValue())) {
                        logger().trace(
                                        "Adding INSERT VALUES literal for ColumnMapping[{}]: {}={} ",
                                        i,
                                        mappingName,
                                        mapping.getLiteralValue());
                        sb.append(mapping.getLiteralValue());
                    } else if (Strings.isNotEmpty(mapping.getParameter())) {
                        logger().trace(
                                        "Adding INSERT VALUES parameter for ColumnMapping[{}]: {}={} ",
                                        i,
                                        mappingName,
                                        mapping.getParameter());
                        sb.append(mapping.getParameter());
                    } else {
                        logger().trace(
                                        "Adding INSERT VALUES parameter marker for ColumnMapping[{}]: {}={} ",
                                        i,
                                        mappingName,
                                        PARAMETER_MARKER);
                        sb.append(PARAMETER_MARKER);
                    }
                    sb.append(',');
                    i++;
                }
            }
            final int columnConfigsLen = data.columnConfigs == null ? 0 : data.columnConfigs.length;
            final List<ColumnConfig> columnConfigs = new ArrayList<>(columnConfigsLen);
            if (data.columnConfigs != null) {
                for (final ColumnConfig config : data.columnConfigs) {
                    if (Strings.isNotEmpty(config.getLiteralValue())) {
                        sb.append(config.getLiteralValue());
                    } else {
                        sb.append(PARAMETER_MARKER);
                        columnConfigs.add(config);
                    }
                    sb.append(',');
                }
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
    private final class Reconnector extends Log4jThread {

        private final CountDownLatch latch = new CountDownLatch(1);
        private volatile boolean shutdown;

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
                    logger().debug(
                                    "Cannot reestablish JDBC connection to {}: {}",
                                    factoryData,
                                    e.getLocalizedMessage(),
                                    e);
                } finally {
                    latch.countDown();
                }
            }
        }

        public void shutdown() {
            shutdown = true;
        }

        @Override
        public String toString() {
            return String.format("Reconnector [latch=%s, shutdown=%s]", latch, shutdown);
        }
    }

    private static final class ResultSetColumnMetaData {

        private final String schemaName;
        private final String catalogName;
        private final String tableName;
        private final String name;
        private final String nameKey;
        private final String label;
        private final int displaySize;
        private final int type;
        private final String typeName;
        private final String className;
        private final int precision;
        private final int scale;
        private final boolean isStringType;

        public ResultSetColumnMetaData(final ResultSetMetaData rsMetaData, final int j) throws SQLException {
            // @formatter:off
            this(
                    rsMetaData.getSchemaName(j),
                    rsMetaData.getCatalogName(j),
                    rsMetaData.getTableName(j),
                    rsMetaData.getColumnName(j),
                    rsMetaData.getColumnLabel(j),
                    rsMetaData.getColumnDisplaySize(j),
                    rsMetaData.getColumnType(j),
                    rsMetaData.getColumnTypeName(j),
                    rsMetaData.getColumnClassName(j),
                    rsMetaData.getPrecision(j),
                    rsMetaData.getScale(j));
            // @formatter:on
        }

        private ResultSetColumnMetaData(
                final String schemaName,
                final String catalogName,
                final String tableName,
                final String name,
                final String label,
                final int displaySize,
                final int type,
                final String typeName,
                final String className,
                final int precision,
                final int scale) {
            this.schemaName = schemaName;
            this.catalogName = catalogName;
            this.tableName = tableName;
            this.name = name;
            this.nameKey = ColumnMapping.toKey(name);
            this.label = label;
            this.displaySize = displaySize;
            this.type = type;
            this.typeName = typeName;
            this.className = className;
            this.precision = precision;
            this.scale = scale;
            // TODO How about also using the className?
            // @formatter:off
            this.isStringType = type == Types.CHAR
                    || type == Types.LONGNVARCHAR
                    || type == Types.LONGVARCHAR
                    || type == Types.NVARCHAR
                    || type == Types.VARCHAR;
            // @formatter:on
        }

        public String getCatalogName() {
            return catalogName;
        }

        public String getClassName() {
            return className;
        }

        public int getDisplaySize() {
            return displaySize;
        }

        public String getLabel() {
            return label;
        }

        public String getName() {
            return name;
        }

        public String getNameKey() {
            return nameKey;
        }

        public int getPrecision() {
            return precision;
        }

        public int getScale() {
            return scale;
        }

        public String getSchemaName() {
            return schemaName;
        }

        public String getTableName() {
            return tableName;
        }

        public int getType() {
            return type;
        }

        public String getTypeName() {
            return typeName;
        }

        public boolean isStringType() {
            return this.isStringType;
        }

        @Override
        public String toString() {
            return String.format(
                    "ColumnMetaData [schemaName=%s, catalogName=%s, tableName=%s, name=%s, nameKey=%s, label=%s, displaySize=%s, type=%s, typeName=%s, className=%s, precision=%s, scale=%s, isStringType=%s]",
                    schemaName,
                    catalogName,
                    tableName,
                    name,
                    nameKey,
                    label,
                    displaySize,
                    type,
                    typeName,
                    className,
                    precision,
                    scale,
                    isStringType);
        }

        public String truncate(final String string) {
            return precision > 0 ? Strings.left(string, precision) : string;
        }
    }

    private static final JdbcDatabaseManagerFactory INSTANCE = new JdbcDatabaseManagerFactory();

    private static void appendColumnName(final int i, final String columnName, final StringBuilder sb) {
        if (i > 1) {
            sb.append(',');
        }
        sb.append(columnName);
    }

    /**
     * Appends column names to the given buffer in the format {@code "A,B,C"}.
     */
    private static void appendColumnNames(final String sqlVerb, final FactoryData data, final StringBuilder sb) {
        // so this gets a little more complicated now that there are two ways to
        // configure column mappings, but
        // both mappings follow the same exact pattern for the prepared statement
        int i = 1;
        final String messagePattern = "Appending {} {}[{}]: {}={} ";
        if (data.columnMappings != null) {
            for (final ColumnMapping colMapping : data.columnMappings) {
                final String columnName = colMapping.getName();
                appendColumnName(i, columnName, sb);
                logger().trace(
                                messagePattern,
                                sqlVerb,
                                colMapping.getClass().getSimpleName(),
                                i,
                                columnName,
                                colMapping);
                i++;
            }
        }
        if (data.columnConfigs != null) {
            for (final ColumnConfig colConfig : data.columnConfigs) {
                final String columnName = colConfig.getColumnName();
                appendColumnName(i, columnName, sb);
                logger().trace(messagePattern, sqlVerb, colConfig.getClass().getSimpleName(), i, columnName, colConfig);
                i++;
            }
        }
    }

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
    public static JdbcDatabaseManager getJDBCDatabaseManager(
            final String name,
            final int bufferSize,
            final ConnectionSource connectionSource,
            final String tableName,
            final ColumnConfig[] columnConfigs) {
        return getManager(
                name,
                new FactoryData(
                        bufferSize,
                        null,
                        connectionSource,
                        tableName,
                        columnConfigs,
                        ColumnMapping.EMPTY_ARRAY,
                        false,
                        AbstractDatabaseAppender.DEFAULT_RECONNECT_INTERVAL_MILLIS,
                        true),
                getFactory());
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
    public static JdbcDatabaseManager getManager(
            final String name,
            final int bufferSize,
            final Layout<? extends Serializable> layout,
            final ConnectionSource connectionSource,
            final String tableName,
            final ColumnConfig[] columnConfigs,
            final ColumnMapping[] columnMappings) {
        return getManager(
                name,
                new FactoryData(
                        bufferSize,
                        layout,
                        connectionSource,
                        tableName,
                        columnConfigs,
                        columnMappings,
                        false,
                        AbstractDatabaseAppender.DEFAULT_RECONNECT_INTERVAL_MILLIS,
                        true),
                getFactory());
    }

    /**
     * Creates a JDBC manager for use within the {@link JdbcAppender}, or returns a suitable one if it already exists.
     *
     * @param name The name of the manager, which should include connection details and hashed passwords where possible.
     * @param bufferSize The size of the log event buffer.
     * @param layout the Appender-level layout
     * @param connectionSource The source for connections to the database.
     * @param tableName The name of the database table to insert log events into.
     * @param columnConfigs Configuration information about the log table columns.
     * @param columnMappings column mapping configuration (including type conversion).
     * @param reconnectIntervalMillis How often to reconnect to the database when a SQL exception is detected.
     * @param immediateFail Whether to fail immediately with a {@link AppenderLoggingException} when connecting
     * to JDBC fails.
     * @return a new or existing JDBC manager as applicable.
     * @deprecated use
     * {@link #getManager(String, int, Layout, ConnectionSource, String, ColumnConfig[], ColumnMapping[], boolean, long)}
     */
    @Deprecated
    public static JdbcDatabaseManager getManager(
            final String name,
            final int bufferSize,
            final Layout<? extends Serializable> layout,
            final ConnectionSource connectionSource,
            final String tableName,
            final ColumnConfig[] columnConfigs,
            final ColumnMapping[] columnMappings,
            final boolean immediateFail,
            final long reconnectIntervalMillis) {
        return getManager(
                name,
                new FactoryData(
                        bufferSize,
                        null,
                        connectionSource,
                        tableName,
                        columnConfigs,
                        columnMappings,
                        false,
                        AbstractDatabaseAppender.DEFAULT_RECONNECT_INTERVAL_MILLIS,
                        true),
                getFactory());
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
     * @param truncateStrings Whether or not to truncate strings to match column metadata.
     * @return a new or existing JDBC manager as applicable.
     */
    public static JdbcDatabaseManager getManager(
            final String name,
            final int bufferSize,
            final Layout<? extends Serializable> layout,
            final ConnectionSource connectionSource,
            final String tableName,
            final ColumnConfig[] columnConfigs,
            final ColumnMapping[] columnMappings,
            final boolean immediateFail,
            final long reconnectIntervalMillis,
            final boolean truncateStrings) {
        return getManager(
                name,
                new FactoryData(
                        bufferSize,
                        layout,
                        connectionSource,
                        tableName,
                        columnConfigs,
                        columnMappings,
                        immediateFail,
                        reconnectIntervalMillis,
                        truncateStrings),
                getFactory());
    }

    // NOTE: prepared statements are prepared in this order: column mappings, then column configs
    private final List<ColumnConfig> columnConfigs;
    private final String sqlStatement;
    // Used in tests
    final FactoryData factoryData;
    private volatile Connection connection;
    private volatile PreparedStatement statement;
    private volatile Reconnector reconnector;
    private volatile boolean isBatchSupported;
    private volatile Map<String, ResultSetColumnMetaData> columnMetaData;

    private JdbcDatabaseManager(
            final String name,
            final String sqlStatement,
            final List<ColumnConfig> columnConfigs,
            final FactoryData factoryData) {
        super(name, factoryData.getBufferSize());
        this.sqlStatement = sqlStatement;
        this.columnConfigs = columnConfigs;
        this.factoryData = factoryData;
    }

    private void checkConnection() {
        boolean connClosed = true;
        try {
            connClosed = isClosed(this.connection);
        } catch (final SQLException e) {
            // Be quiet
        }
        boolean stmtClosed = true;
        try {
            stmtClosed = isClosed(this.statement);
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
                            "Error writing to JDBC Manager '%s': JDBC connection not available [%s]",
                            getName(), fieldsToString());
                }
                if (statement == null) {
                    throw new AppenderLoggingException(
                            "Error writing to JDBC Manager '%s': JDBC statement not available [%s].",
                            getName(), connection, fieldsToString());
                }
            }
        }
    }

    protected void closeResources(final boolean logExceptions) {
        final PreparedStatement tempPreparedStatement = this.statement;
        this.statement = null;
        try {
            // Closing a statement returns it to the pool when using Apache Commons DBCP.
            // Closing an already closed statement has no effect.
            Closer.close(tempPreparedStatement);
        } catch (final Exception e) {
            if (logExceptions) {
                logWarn("Failed to close SQL statement logging event or flushing buffer", e);
            }
        }

        final Connection tempConnection = this.connection;
        this.connection = null;
        try {
            // Closing a connection returns it to the pool when using Apache Commons DBCP.
            // Closing an already closed connection has no effect.
            Closer.close(tempConnection);
        } catch (final Exception e) {
            if (logExceptions) {
                logWarn("Failed to close database connection logging event or flushing buffer", e);
            }
        }
    }

    @Override
    protected boolean commitAndClose() {
        final boolean closed = true;
        try {
            if (this.connection != null && !this.connection.isClosed()) {
                if (isBuffered() && this.isBatchSupported && this.statement != null) {
                    logger().debug("Executing batch PreparedStatement {}", this.statement);
                    int[] result;
                    try {
                        result = this.statement.executeBatch();
                    } catch (SQLTransactionRollbackException e) {
                        logger().debug("{} executing batch PreparedStatement {}, retrying.", e, this.statement);
                        result = this.statement.executeBatch();
                    }
                    logger().debug("Batch result: {}", Arrays.toString(result));
                }
                logger().debug("Committing Connection {}", this.connection);
                this.connection.commit();
            }
        } catch (final SQLException e) {
            throw new DbAppenderLoggingException(
                    e, "Failed to commit transaction logging event or flushing buffer [%s]", fieldsToString());
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
            } catch (final AppenderLoggingException e) {
                // Database connection has likely gone stale.
                final Throwable cause = e.getCause();
                final Throwable actual = cause == null ? e : cause;
                logger().debug(
                                "{} committing and closing connection: {}",
                                actual,
                                actual.getClass().getSimpleName(),
                                e.toString(),
                                e);
            }
        }
        if (factoryData.connectionSource != null) {
            factoryData.connectionSource.stop();
        }
        return true;
    }

    @SuppressFBWarnings(
            value = "SQL_INJECTION_JDBC",
            justification = "The SQL statement is generated based on the configuration file.")
    private void connectAndPrepare() throws SQLException {
        logger().debug("Acquiring JDBC connection from {}", this.getConnectionSource());
        this.connection = getConnectionSource().getConnection();
        logger().debug("Acquired JDBC connection {}", this.connection);
        logger().debug("Getting connection metadata {}", this.connection);
        final DatabaseMetaData databaseMetaData = this.connection.getMetaData();
        logger().debug("Connection metadata {}", databaseMetaData);
        this.isBatchSupported = databaseMetaData.supportsBatchUpdates();
        logger().debug("Connection supportsBatchUpdates: {}", this.isBatchSupported);
        this.connection.setAutoCommit(false);
        logger().debug("Preparing SQL {}", this.sqlStatement);
        this.statement = this.connection.prepareStatement(this.sqlStatement);
        logger().debug("Prepared SQL {}", this.statement);
        if (this.factoryData.truncateStrings) {
            initColumnMetaData();
        }
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

    private String createSqlSelect() {
        final StringBuilder sb = new StringBuilder("select ");
        appendColumnNames("SELECT", this.factoryData, sb);
        sb.append(" from ");
        sb.append(this.factoryData.tableName);
        sb.append(" where 1=0");
        return sb.toString();
    }

    private String fieldsToString() {
        return String.format(
                "columnConfigs=%s, sqlStatement=%s, factoryData=%s, connection=%s, statement=%s, reconnector=%s, isBatchSupported=%s, columnMetaData=%s",
                columnConfigs,
                sqlStatement,
                factoryData,
                connection,
                statement,
                reconnector,
                isBatchSupported,
                columnMetaData);
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

    @SuppressFBWarnings(
            value = "SQL_INJECTION_JDBC",
            justification = "The SQL statement is generated based on the configuration file.")
    private void initColumnMetaData() throws SQLException {
        // Could use:
        // this.connection.getMetaData().getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern);
        // But this returns more data than we need for now, so do a SQL SELECT with 0 result rows instead.
        final String sqlSelect = createSqlSelect();
        logger().debug("Getting SQL metadata for table {}: {}", this.factoryData.tableName, sqlSelect);
        try (final PreparedStatement mdStatement = this.connection.prepareStatement(sqlSelect)) {
            final ResultSetMetaData rsMetaData = mdStatement.getMetaData();
            logger().debug("SQL metadata: {}", rsMetaData);
            if (rsMetaData != null) {
                final int columnCount = rsMetaData.getColumnCount();
                columnMetaData = new HashMap<>(columnCount);
                for (int i = 0, j = 1; i < columnCount; i++, j++) {
                    final ResultSetColumnMetaData value = new ResultSetColumnMetaData(rsMetaData, j);
                    columnMetaData.put(value.getNameKey(), value);
                }
            } else {
                logger().warn(
                                "{}: truncateStrings is true and ResultSetMetaData is null for statement: {}; manager will not perform truncation.",
                                getClass().getSimpleName(),
                                mdStatement);
            }
        }
    }

    /**
     * Checks if a statement is closed. A null statement is considered closed.
     *
     * @param statement The statement to check.
     * @return true if a statement is closed, false if null.
     * @throws SQLException if a database access error occurs
     */
    private boolean isClosed(final Statement statement) throws SQLException {
        return statement == null || statement.isClosed();
    }

    /**
     * Checks if a connection is closed. A null connection is considered closed.
     *
     * @param connection The connection to check.
     * @return true if a connection is closed, false if null.
     * @throws SQLException if a database access error occurs
     */
    private boolean isClosed(final Connection connection) throws SQLException {
        return connection == null || connection.isClosed();
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
                logger().debug(
                                "Cannot reestablish JDBC connection to {}: {}; starting reconnector thread {}",
                                factoryData,
                                reconnectEx,
                                reconnector.getName(),
                                reconnectEx);
                reconnector.start();
                reconnector.latch();
                if (connection == null || statement == null) {
                    throw new AppenderLoggingException(
                            exception, "Error sending to %s for %s [%s]", getName(), factoryData, fieldsToString());
                }
            }
        }
    }

    private void setFields(final MapMessage<?, ?> mapMessage) throws SQLException {
        final IndexedReadOnlyStringMap map = mapMessage.getIndexedReadOnlyStringMap();
        final String simpleName = statement.getClass().getName();
        int j = 1; // JDBC indices start at 1
        if (this.factoryData.columnMappings != null) {
            for (final ColumnMapping mapping : this.factoryData.columnMappings) {
                if (mapping.getLiteralValue() == null) {
                    final String source = mapping.getSource();
                    final String key = Strings.isEmpty(source) ? mapping.getName() : source;
                    final Object value = map.getValue(key);
                    if (logger().isTraceEnabled()) {
                        final String valueStr =
                                value instanceof String ? "\"" + value + "\"" : Objects.toString(value, null);
                        logger().trace(
                                        "{} setObject({}, {}) for key '{}' and mapping '{}'",
                                        simpleName,
                                        j,
                                        valueStr,
                                        key,
                                        mapping.getName());
                    }
                    setStatementObject(j, mapping.getNameKey(), value);
                    j++;
                }
            }
        }
    }

    /**
     * Sets the given Object in the prepared statement. The value is truncated if needed.
     */
    private void setStatementObject(final int j, final String nameKey, final Object value) throws SQLException {
        if (statement == null) {
            throw new AppenderLoggingException("Cannot set a value when the PreparedStatement is null.");
        }
        if (value == null) {
            if (columnMetaData == null) {
                throw new AppenderLoggingException("Cannot set a value when the column metadata is null.");
            }
            // [LOG4J2-2762] [JDBC] MS-SQL Server JDBC driver throws SQLServerException when
            // inserting a null value for a VARBINARY column.
            // Calling setNull() instead of setObject() for null values fixes [LOG4J2-2762].
            this.statement.setNull(j, columnMetaData.get(nameKey).getType());
        } else {
            statement.setObject(j, truncate(nameKey, value));
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

    /**
     * Truncates the value if needed.
     */
    private Object truncate(final String nameKey, Object value) {
        if (value != null && this.factoryData.truncateStrings && columnMetaData != null) {
            final ResultSetColumnMetaData resultSetColumnMetaData = columnMetaData.get(nameKey);
            if (resultSetColumnMetaData != null) {
                if (resultSetColumnMetaData.isStringType()) {
                    value = resultSetColumnMetaData.truncate(value.toString());
                }
            } else {
                logger().error(
                                "Missing ResultSetColumnMetaData for {}, connection={}, statement={}",
                                nameKey,
                                connection,
                                statement);
            }
        }
        return value;
    }

    @Override
    protected void writeInternal(final LogEvent event, final Serializable serializable) {
        StringReader reader = null;
        try {
            if (!this.isRunning() || isClosed(this.connection) || isClosed(this.statement)) {
                throw new AppenderLoggingException(
                        "Cannot write logging event; JDBC manager not connected to the database, running=%s, [%s]).",
                        isRunning(), fieldsToString());
            }
            // Clear in case there are leftovers.
            statement.clearParameters();
            if (serializable instanceof MapMessage) {
                setFields((MapMessage<?, ?>) serializable);
            }
            int j = 1; // JDBC indices start at 1
            if (this.factoryData.columnMappings != null) {
                for (final ColumnMapping mapping : this.factoryData.columnMappings) {
                    if (ThreadContextMap.class.isAssignableFrom(mapping.getType())
                            || ReadOnlyStringMap.class.isAssignableFrom(mapping.getType())) {
                        this.statement.setObject(j++, event.getContextData().toMap());
                    } else if (ThreadContextStack.class.isAssignableFrom(mapping.getType())) {
                        this.statement.setObject(j++, event.getContextStack().asList());
                    } else if (Date.class.isAssignableFrom(mapping.getType())) {
                        this.statement.setObject(
                                j++,
                                DateTypeConverter.fromMillis(
                                        event.getTimeMillis(), mapping.getType().asSubclass(Date.class)));
                    } else {
                        final StringLayout layout = mapping.getLayout();
                        if (layout != null) {
                            if (Clob.class.isAssignableFrom(mapping.getType())) {
                                this.statement.setClob(j++, new StringReader(layout.toSerializable(event)));
                            } else if (NClob.class.isAssignableFrom(mapping.getType())) {
                                this.statement.setNClob(j++, new StringReader(layout.toSerializable(event)));
                            } else {
                                final Object value =
                                        TypeConverters.convert(layout.toSerializable(event), mapping.getType(), null);
                                setStatementObject(j++, mapping.getNameKey(), value);
                            }
                        }
                    }
                }
            }
            for (final ColumnConfig column : this.columnConfigs) {
                if (column.isEventTimestamp()) {
                    this.statement.setTimestamp(j++, new Timestamp(event.getTimeMillis()));
                } else if (column.isClob()) {
                    reader = new StringReader(column.getLayout().toSerializable(event));
                    if (column.isUnicode()) {
                        this.statement.setNClob(j++, reader);
                    } else {
                        this.statement.setClob(j++, reader);
                    }
                } else if (column.isUnicode()) {
                    this.statement.setNString(
                            j++,
                            Objects.toString(
                                    truncate(
                                            column.getColumnNameKey(),
                                            column.getLayout().toSerializable(event)),
                                    null));
                } else {
                    this.statement.setString(
                            j++,
                            Objects.toString(
                                    truncate(
                                            column.getColumnNameKey(),
                                            column.getLayout().toSerializable(event)),
                                    null));
                }
            }

            if (isBuffered() && this.isBatchSupported) {
                logger().debug("addBatch for {}", this.statement);
                this.statement.addBatch();
            } else {
                final int executeUpdate = this.statement.executeUpdate();
                logger().debug("executeUpdate = {} for {}", executeUpdate, this.statement);
                if (executeUpdate == 0) {
                    throw new AppenderLoggingException(
                            "No records inserted in database table for log event in JDBC manager [%s].",
                            fieldsToString());
                }
            }
        } catch (final SQLException e) {
            throw new DbAppenderLoggingException(
                    e, "Failed to insert record for log event in JDBC manager: %s [%s]", e, fieldsToString());
        } finally {
            // Release ASAP
            try {
                // statement can be null when a AppenderLoggingException is thrown at the start of this method
                if (statement != null) {
                    statement.clearParameters();
                }
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
        } catch (final DbAppenderLoggingException e) {
            reconnectOn(e);
            try {
                this.writeInternal(event, serializable);
            } finally {
                this.commitAndClose();
            }
        }
    }
}
