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

package org.apache.logging.log4j.perf.jmh;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.db.jdbc.JdbcAppender;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.util.Closer;
import org.apache.logging.log4j.status.StatusLogger;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Tests the overhead of a number of JDBC Appenders.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
//
// java -jar log4j-perf/target/benchmarks.jar ".*JdbcAppenderBenchmark.*" -f 1 -wi 5 -i 5
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//
@State(Scope.Thread)
public class JdbcAppenderBenchmark {
    private Logger loggerH2;
    private Logger loggerHSQLDB;
    private Connection connectionHSQLDB;
    private Connection connectionH2;
    private final RuntimeException exception = new RuntimeException("Hello, world!");

    @Setup
    public void setup() throws Exception {
        connectionHSQLDB = getConnectionHSQLDB();
        connectionH2 = getConnectionH2();
        createTable(connectionHSQLDB, toCreateTableSqlStringHQLDB("fmLogEntry"));
        createTable(connectionH2, toCreateTableSqlStringH2("fmLogEntry"));

        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, "log4j2-jdbc-appender.xml");
        final LoggerContext context = LoggerContext.getContext(false);
        if (context.getConfiguration() instanceof DefaultConfiguration) {
            context.reconfigure();
        }
        StatusLogger.getLogger().reset();
        loggerH2 = LogManager.getLogger("H2Logger");
        loggerHSQLDB = LogManager.getLogger("HSQLDBLogger");
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void testThroughputH2Message(final Blackhole bh) {
        loggerH2.info("Test message");
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void testThroughputH2Exception(final Blackhole bh) {
        loggerH2.warn("Test message", exception);
    }

    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Benchmark
    public void testResponseTimeH2Message(final Blackhole bh) {
        loggerH2.info("Test message");
    }

    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Benchmark
    public void testResponseTimeH2Exception(final Blackhole bh) {
        loggerH2.warn("Test message", exception);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void testThroughputHSQLDBMessage(final Blackhole bh) {
        loggerHSQLDB.info("Test message");
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void testThroughputHSQLDBException(final Blackhole bh) {
        loggerHSQLDB.warn("Test message", exception);
    }

    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Benchmark
    public void testResponseTimeHSQLDBMessage(final Blackhole bh) {
        loggerHSQLDB.info("Test message");
    }

    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Benchmark
    public void testResponseTimeHSQLDBException(final Blackhole bh) {
        loggerHSQLDB.warn("Test message", exception);
    }

    @TearDown
    public void tearDown() throws SQLException {
        final LoggerContext context = LoggerContext.getContext(false);
        try {
            ((JdbcAppender) context.getConfiguration().getAppender("H2Appender")).getManager().close();
            ((JdbcAppender) context.getConfiguration().getAppender("HSQLDBAppender")).getManager().close();
        } finally {
            System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
            // context.reconfigure();
            // StatusLogger.getLogger().reset();

            Statement statement = null;
            try {
                statement = connectionHSQLDB.createStatement();
                statement.execute("SHUTDOWN");
            } catch (final SQLException ignore) {
                // ignore
            } finally {
                Closer.closeSilently(statement);
                Closer.closeSilently(connectionHSQLDB);
            }
            try {
                statement = connectionH2.createStatement();
                statement.execute("SHUTDOWN");
            } catch (final SQLException ignore) {
                // ignore
            } finally {
                Closer.closeSilently(statement);
                Closer.closeSilently(connectionH2);
            }
        }
    }

    private void createTable(final Connection connection, final String createSQL) throws SQLException {
        try (final Statement statement = connection.createStatement()) {
            statement.executeUpdate(createSQL);
        }
    }

    private String toCreateTableSqlStringH2(final String tableName) {
        return "CREATE TABLE " + tableName + " ( "
                + "id INTEGER IDENTITY, eventDate DATETIME, literalColumn VARCHAR(255), level NVARCHAR(10), "
                + "logger NVARCHAR(255), message VARCHAR(1024), exception NCLOB" + " )";
    }

    private String toCreateTableSqlStringHQLDB(final String tableName) {
        return "CREATE TABLE " + tableName + " ( "
                + "id INTEGER IDENTITY, eventDate DATETIME, literalColumn VARCHAR(255), level VARCHAR(10), "
                + "logger VARCHAR(255), message VARCHAR(1024), exception CLOB" + " )";
    }

    /**
     * Referred from log4j2-jdbc-appender.xml.
     */
    public static Connection getConnectionH2() throws Exception {
        Class.forName("org.h2.Driver");
        return DriverManager.getConnection("jdbc:h2:mem:Log4j", "sa", "");
    }

    /**
     * Referred from log4j2-jdbc-appender.xml.
     */
    public static Connection getConnectionHSQLDB() throws Exception {
        Class.forName("org.hsqldb.jdbcDriver");
        return DriverManager.getConnection("jdbc:hsqldb:mem:Log4j", "sa", "");
    }
}
