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

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.categories.PerformanceTests;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;
import org.easymock.IAnswer;
import org.h2.util.IOUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockejb.jndi.MockContextFactory;

import static org.easymock.EasyMock.*;

import static org.junit.Assert.*;

public abstract class AbstractJdbcAppenderTest {
    private final String databaseType;
    private Connection connection;

    public AbstractJdbcAppenderTest(final String databaseType) {
        this.databaseType = databaseType;
    }

    protected abstract Connection newConnection() throws SQLException;

    protected abstract String toCreateTableSqlString(final String tableName);

    protected void setUp(final String tableName, final String configFileName) throws SQLException {
        this.connection = this.newConnection();
        final Statement statement = this.connection.createStatement();
        try {
            statement.executeUpdate(this.toCreateTableSqlString(tableName));
        } finally {
            statement.close();
        }
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY,
                "org/apache/logging/log4j/core/appender/db/jdbc/" + configFileName);
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        if (context.getConfiguration() instanceof DefaultConfiguration) {
            context.reconfigure();
        }
        StatusLogger.getLogger().reset();
    }

    @After
    public void tearDown() throws SQLException {
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        try {
            final Appender appender = context.getConfiguration().getAppender("databaseAppender");
            assertNotNull("The appender should not be null.", appender);
            assertTrue("The appender should be a JdbcAppender.", appender instanceof JdbcAppender);
            ((JdbcAppender) appender).getManager().release();
        } finally {
            System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
            context.reconfigure();
            StatusLogger.getLogger().reset();

            Statement statement = null;
            try {
                statement = this.connection.createStatement();
                statement.execute("SHUTDOWN");
            } finally {
                try {
                    if (statement != null) {
                        statement.close();
                    }
                } catch (final SQLException ignore) {
                    /* */
                }
            }

            this.connection.close();
        }
    }

    @Test
    public void testDataSourceConfig() throws Exception {
        final DataSource dataSource = createStrictMock(DataSource.class);

        expect(dataSource.getConnection()).andAnswer(new IAnswer<Connection>() {
            @Override
            public Connection answer() throws Throwable {
                return newConnection();
            }
        }).atLeastOnce();
        replay(dataSource);

        MockContextFactory.setAsInitial();

        final InitialContext context = new InitialContext();
        context.createSubcontext("java:");
        context.createSubcontext("java:/comp");
        context.createSubcontext("java:/comp/env");
        context.createSubcontext("java:/comp/env/jdbc");

        context.bind("java:/comp/env/jdbc/TestDataSourceAppender", dataSource);

        try {
            this.setUp("dsLogEntry", "log4j2-data-source.xml");

            final Error exception = new Error("Final error massage is fatal!");
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            final PrintWriter writer = new PrintWriter(outputStream);
            exception.printStackTrace(writer);
            writer.close();
            final String stackTrace = outputStream.toString();

            final long millis = System.currentTimeMillis();

            final Logger logger = LogManager.getLogger(this.getClass().getName() + ".testDataSourceConfig");
            logger.trace("Data source logged message 01.");
            logger.fatal("Error from data source 02.", exception);

            final Statement statement = this.connection.createStatement();
            final ResultSet resultSet = statement.executeQuery("SELECT * FROM dsLogEntry ORDER BY id");

            assertTrue("There should be at least one row.", resultSet.next());

            final long date = resultSet.getTimestamp("eventDate").getTime();
            assertTrue("The date should be later than pre-logging (1).", date >= millis);
            assertTrue("The date should be earlier than now (1).", date <= System.currentTimeMillis());
            assertEquals("The literal column is not correct (1).", "Literal Value of Data Source",
                    resultSet.getString("literalColumn"));
            assertEquals("The level column is not correct (1).", "FATAL", resultSet.getNString("level"));
            assertEquals("The logger column is not correct (1).", logger.getName(), resultSet.getNString("logger"));
            assertEquals("The message column is not correct (1).", "Error from data source 02.",
                    resultSet.getString("message"));
            assertEquals("The exception column is not correct (1).", stackTrace,
                    IOUtils.readStringAndClose(resultSet.getNClob("exception").getCharacterStream(), -1));

            assertFalse("There should not be two rows.", resultSet.next());

            verify(dataSource);
        } finally {
            MockContextFactory.revertSetAsInitial();
        }
    }

    @Test
    public void testFactoryMethodConfig() throws Exception {
        this.setUp("fmLogEntry", "log4j2-" + this.databaseType + "-factory-method.xml");

        final SQLException exception = new SQLException("Some other error message!");
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final PrintWriter writer = new PrintWriter(outputStream);
        exception.printStackTrace(writer);
        writer.close();
        final String stackTrace = outputStream.toString();

        final long millis = System.currentTimeMillis();

        final Logger logger = LogManager.getLogger(this.getClass().getName() + ".testFactoryMethodConfig");
        logger.debug("Factory logged message 01.");
        logger.error("Error from factory 02.", exception);

        final Statement statement = this.connection.createStatement();
        final ResultSet resultSet = statement.executeQuery("SELECT * FROM fmLogEntry ORDER BY id");

        assertTrue("There should be at least one row.", resultSet.next());

        long date = resultSet.getTimestamp("eventDate").getTime();
        assertTrue("The date should be later than pre-logging (1).", date >= millis);
        assertTrue("The date should be earlier than now (1).", date <= System.currentTimeMillis());
        assertEquals("The literal column is not correct (1).", "Some Other Literal Value",
                resultSet.getString("literalColumn"));
        assertEquals("The level column is not correct (1).", "DEBUG", resultSet.getNString("level"));
        assertEquals("The logger column is not correct (1).", logger.getName(), resultSet.getNString("logger"));
        assertEquals("The message column is not correct (1).", "Factory logged message 01.",
                resultSet.getString("message"));
        assertEquals("The exception column is not correct (1).", Strings.EMPTY,
                IOUtils.readStringAndClose(resultSet.getNClob("exception").getCharacterStream(), -1));

        assertTrue("There should be two rows.", resultSet.next());

        date = resultSet.getTimestamp("eventDate").getTime();
        assertTrue("The date should be later than pre-logging (2).", date >= millis);
        assertTrue("The date should be earlier than now (2).", date <= System.currentTimeMillis());
        assertEquals("The literal column is not correct (2).", "Some Other Literal Value",
                resultSet.getString("literalColumn"));
        assertEquals("The level column is not correct (2).", "ERROR", resultSet.getNString("level"));
        assertEquals("The logger column is not correct (2).", logger.getName(), resultSet.getNString("logger"));
        assertEquals("The message column is not correct (2).", "Error from factory 02.",
                resultSet.getString("message"));
        assertEquals("The exception column is not correct (2).", stackTrace,
                IOUtils.readStringAndClose(resultSet.getNClob("exception").getCharacterStream(), -1));

        assertFalse("There should not be three rows.", resultSet.next());
    }

    @Test
    @Category(PerformanceTests.class)
    public void testPerformanceOfAppenderWith1000Events() throws Exception {
        this.setUp("fmLogEntry", "log4j2-" + this.databaseType + "-factory-method.xml");

        final RuntimeException exception = new RuntimeException("Hello, world!");

        final Logger logger = LogManager.getLogger(this.getClass().getName() +
                ".testPerformanceOfAppenderWith1000Events");
        logger.info("This is a warm-up message.");

        System.out.println("Starting a performance test for JDBC Appender for " + this.databaseType + '.');

        final long start = System.nanoTime();

        for(int i = 0; i < 1000; i++) {
            if (i % 25 == 0) {
                logger.warn("This is an exception message.", exception);
            } else {
                logger.info("This is an info message.");
            }
        }

        final long elapsed = System.nanoTime() - start;
        final long elapsedMilli = elapsed / 1000000;

        final Statement statement = this.connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);
        final ResultSet resultSet = statement.executeQuery("SELECT * FROM fmLogEntry ORDER BY id");

        resultSet.last();
        assertEquals("The number of records is not correct.", 1001, resultSet.getRow());

        System.out.println("Wrote 1,000 log events in " + elapsed + " nanoseconds (" + elapsedMilli +
                " milliseconds) for " + this.databaseType + '.');
    }
}
