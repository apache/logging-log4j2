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
package org.apache.logging.log4j.nosql.appender.cassandra;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.appender.db.AbstractDatabaseManager;
import org.apache.logging.log4j.core.appender.db.ColumnMapping;
import org.apache.logging.log4j.core.config.plugins.convert.DateTypeConverter;
import org.apache.logging.log4j.core.config.plugins.convert.TypeConverters;
import org.apache.logging.log4j.core.net.SocketAddress;
import org.apache.logging.log4j.spi.ThreadContextMap;
import org.apache.logging.log4j.spi.ThreadContextStack;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.Strings;

/**
 * Manager for a Cassandra appender instance.
 */
public class CassandraManager extends AbstractDatabaseManager {

    private static final int DEFAULT_PORT = 9042;

    private final Cluster cluster;
    private final String keyspace;
    private final String insertQueryTemplate;
    private final List<ColumnMapping> columnMappings;
    private final BatchStatement batchStatement;
    // re-usable argument binding array
    private final Object[] values;

    private Session session;
    private PreparedStatement preparedStatement;

    private CassandraManager(final String name, final int bufferSize, final Cluster cluster,
                             final String keyspace, final String insertQueryTemplate,
                             final List<ColumnMapping> columnMappings, final BatchStatement batchStatement) {
        super(name, bufferSize);
        this.cluster = cluster;
        this.keyspace = keyspace;
        this.insertQueryTemplate = insertQueryTemplate;
        this.columnMappings = columnMappings;
        this.batchStatement = batchStatement;
        this.values = new Object[columnMappings.size()];
    }

    @Override
    protected void startupInternal() throws Exception {
        session = cluster.connect(keyspace);
        preparedStatement = session.prepare(insertQueryTemplate);
    }

    @Override
    protected boolean shutdownInternal() throws Exception {
        session.close();
        cluster.close();
        return true;
    }

    @Override
    protected void connectAndStart() {
        // a Session automatically manages connections for us
    }

    @Override
    protected void writeInternal(final LogEvent event) {
        for (int i = 0; i < columnMappings.size(); i++) {
            final ColumnMapping columnMapping = columnMappings.get(i);
            if (ThreadContextMap.class.isAssignableFrom(columnMapping.getType())
                || ReadOnlyStringMap.class.isAssignableFrom(columnMapping.getType())) {
                values[i] = event.getContextData().toMap();
            } else if (ThreadContextStack.class.isAssignableFrom(columnMapping.getType())) {
                values[i] = event.getContextStack().asList();
            } else if (Date.class.isAssignableFrom(columnMapping.getType())) {
                values[i] = DateTypeConverter.fromMillis(event.getTimeMillis(), columnMapping.getType().asSubclass(Date.class));
            } else {
                values[i] = TypeConverters.convert(columnMapping.getLayout().toSerializable(event),
                    columnMapping.getType(), null);
            }
        }
        final BoundStatement boundStatement = preparedStatement.bind(values);
        if (batchStatement == null) {
            session.execute(boundStatement);
        } else {
            batchStatement.add(boundStatement);
        }
    }

    @Override
    protected boolean commitAndClose() {
        if (batchStatement != null) {
            session.execute(batchStatement);
        }
        return true;
    }

    public static CassandraManager getManager(final String name, final SocketAddress[] contactPoints,
                                              final ColumnMapping[] columns, final boolean useTls,
                                              final String clusterName, final String keyspace, final String table,
                                              final String username, final String password,
                                              final boolean useClockForTimestampGenerator, final int bufferSize,
                                              final boolean batched, final BatchStatement.Type batchType) {
        return getManager(name,
            new FactoryData(contactPoints, columns, useTls, clusterName, keyspace, table, username, password,
                useClockForTimestampGenerator, bufferSize, batched, batchType), CassandraManagerFactory.INSTANCE);
    }

    private static class CassandraManagerFactory implements ManagerFactory<CassandraManager, FactoryData> {

        private static final CassandraManagerFactory INSTANCE = new CassandraManagerFactory();

        @Override
        public CassandraManager createManager(final String name, final FactoryData data) {
            final Cluster.Builder builder = Cluster.builder()
                .addContactPointsWithPorts(data.contactPoints)
                .withClusterName(data.clusterName);
            if (data.useTls) {
                builder.withSSL();
            }
            if (Strings.isNotBlank(data.username)) {
                builder.withCredentials(data.username, data.password);
            }
            if (data.useClockForTimestampGenerator) {
                builder.withTimestampGenerator(new ClockTimestampGenerator());
            }
            final Cluster cluster = builder.build();

            final StringBuilder sb = new StringBuilder("INSERT INTO ").append(data.table).append(" (");
            for (final ColumnMapping column : data.columns) {
                sb.append(column.getName()).append(',');
            }
            sb.setCharAt(sb.length() - 1, ')');
            sb.append(" VALUES (");
            final List<ColumnMapping> columnMappings = new ArrayList<>(data.columns.length);
            for (final ColumnMapping column : data.columns) {
                if (Strings.isNotEmpty(column.getLiteralValue())) {
                    sb.append(column.getLiteralValue());
                } else {
                    sb.append('?');
                    columnMappings.add(column);
                }
                sb.append(',');
            }
            sb.setCharAt(sb.length() - 1, ')');
            final String insertQueryTemplate = sb.toString();
            LOGGER.debug("Using CQL for appender {}: {}", name, insertQueryTemplate);
            return new CassandraManager(name, data.getBufferSize(), cluster, data.keyspace, insertQueryTemplate,
                columnMappings, data.batched ? new BatchStatement(data.batchType) : null);
        }
    }

    private static class FactoryData extends AbstractFactoryData {
        private final InetSocketAddress[] contactPoints;
        private final ColumnMapping[] columns;
        private final boolean useTls;
        private final String clusterName;
        private final String keyspace;
        private final String table;
        private final String username;
        private final String password;
        private final boolean useClockForTimestampGenerator;
        private final boolean batched;
        private final BatchStatement.Type batchType;

        private FactoryData(final SocketAddress[] contactPoints, final ColumnMapping[] columns, final boolean useTls,
                            final String clusterName, final String keyspace, final String table, final String username,
                            final String password, final boolean useClockForTimestampGenerator, final int bufferSize,
                            final boolean batched, final BatchStatement.Type batchType) {
            super(bufferSize);
            this.contactPoints = convertAndAddDefaultPorts(contactPoints);
            this.columns = columns;
            this.useTls = useTls;
            this.clusterName = clusterName;
            this.keyspace = keyspace;
            this.table = table;
            this.username = username;
            this.password = password;
            this.useClockForTimestampGenerator = useClockForTimestampGenerator;
            this.batched = batched;
            this.batchType = batchType;
        }

        private static InetSocketAddress[] convertAndAddDefaultPorts(final SocketAddress... socketAddresses) {
            final InetSocketAddress[] inetSocketAddresses = new InetSocketAddress[socketAddresses.length];
            for (int i = 0; i < inetSocketAddresses.length; i++) {
                final SocketAddress socketAddress = socketAddresses[i];
                inetSocketAddresses[i] = socketAddress.getPort() == 0
                    ? new InetSocketAddress(socketAddress.getAddress(), DEFAULT_PORT)
                    : socketAddress.getSocketAddress();
            }
            return inetSocketAddresses;
        }
    }
}
