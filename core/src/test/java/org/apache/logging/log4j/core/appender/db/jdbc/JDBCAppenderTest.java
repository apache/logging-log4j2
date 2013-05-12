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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.status.StatusLogger;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Test;
import org.mockejb.jndi.MockContextFactory;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class JDBCAppenderTest {
    private Connection connection;

    public void setUp(final String tableName, final String configFileName) throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:hsqldb:mem:Log4j", "sa", "");

        final Statement statement = this.connection.createStatement();
        statement.executeUpdate("CREATE TABLE " + tableName + " ( " +
                    "id INTEGER IDENTITY, eventDate DATETIME, literalColumn VARCHAR(255), level VARCHAR(10), "  +
                    "logger VARCHAR(255), message VARCHAR(1024), exception VARCHAR(1048576)" +
                " )");
        statement.close();

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
            final Map<String, Appender<?>> list = context.getConfiguration().getAppenders();
            final Appender<?> appender = list.get("databaseAppender");
            assertNotNull("The appender should not be null.", appender);
            assertTrue("The appender should be a JDBCAppender.", appender instanceof JDBCAppender);
            ((JDBCAppender) appender).getManager().release();
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
                } catch (final SQLException ignore) { /* */
                }
            }

            this.connection.close();
        }
    }

    @Test
    public void testDriverManagerConfig() throws SQLException {
        this.setUp("dmLogEntry", "log4j2-driver-manager.xml");

        final RuntimeException exception = new RuntimeException("Hello, world!");
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final PrintWriter writer = new PrintWriter(outputStream);
        exception.printStackTrace(writer);
        writer.close();
        final String stackTrace = outputStream.toString();

        final long millis = System.currentTimeMillis();

        final Logger logger = LogManager.getLogger(this.getClass().getName() + ".testDriverManagerConfig");
        logger.info("Test my message 01.");
        logger.warn("This is another message 02.", exception);

        final Statement statement = this.connection.createStatement();
        final ResultSet resultSet = statement.executeQuery("SELECT * FROM dmLogEntry ORDER BY id");

        assertTrue("There should be at least one row.", resultSet.next());

        long date = resultSet.getTimestamp("eventDate").getTime();
        assertTrue("The date should be later than pre-logging (1).", date >= millis);
        assertTrue("The date should be earlier than now (1).", date <= System.currentTimeMillis());
        assertEquals("The literal column is not correct (1).", "Literal Value Test String",
                resultSet.getString("literalColumn"));
        assertEquals("The level column is not correct (1).", "INFO", resultSet.getString("level"));
        assertEquals("The logger column is not correct (1).", logger.getName(), resultSet.getString("logger"));
        assertEquals("The message column is not correct (1).", "Test my message 01.", resultSet.getString("message"));
        assertEquals("The exception column is not correct (1).", "", resultSet.getString("exception"));

        assertTrue("There should be two rows.", resultSet.next());

        date = resultSet.getTimestamp("eventDate").getTime();
        assertTrue("The date should be later than pre-logging (2).", date >= millis);
        assertTrue("The date should be earlier than now (2).", date <= System.currentTimeMillis());
        assertEquals("The literal column is not correct (2).", "Literal Value Test String",
                resultSet.getString("literalColumn"));
        assertEquals("The level column is not correct (2).", "WARN", resultSet.getString("level"));
        assertEquals("The logger column is not correct (2).", logger.getName(), resultSet.getString("logger"));
        assertEquals("The message column is not correct (2).", "This is another message 02.",
                resultSet.getString("message"));
        assertEquals("The exception column is not correct (2).", stackTrace, resultSet.getString("exception"));

        assertFalse("There should not be three rows.", resultSet.next());
    }

    @Test
    public void testFactoryMethodConfig() throws SQLException {
        this.setUp("fmLogEntry", "log4j2-factory-method.xml");

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
        assertEquals("The level column is not correct (1).", "DEBUG", resultSet.getString("level"));
        assertEquals("The logger column is not correct (1).", logger.getName(), resultSet.getString("logger"));
        assertEquals("The message column is not correct (1).", "Factory logged message 01.",
                resultSet.getString("message"));
        assertEquals("The exception column is not correct (1).", "", resultSet.getString("exception"));

        assertTrue("There should be two rows.", resultSet.next());

        date = resultSet.getTimestamp("eventDate").getTime();
        assertTrue("The date should be later than pre-logging (2).", date >= millis);
        assertTrue("The date should be earlier than now (2).", date <= System.currentTimeMillis());
        assertEquals("The literal column is not correct (2).", "Some Other Literal Value",
                resultSet.getString("literalColumn"));
        assertEquals("The level column is not correct (2).", "ERROR", resultSet.getString("level"));
        assertEquals("The logger column is not correct (2).", logger.getName(), resultSet.getString("logger"));
        assertEquals("The message column is not correct (2).", "Error from factory 02.", resultSet.getString("message"));
        assertEquals("The exception column is not correct (2).", stackTrace, resultSet.getString("exception"));

        assertFalse("There should not be three rows.", resultSet.next());
    }

    @Test
    public void testDataSourceConfig() throws Exception {
        System.out.println("Before creating mock data source.");
        DataSource dataSource = createStrictMock(DataSource.class);

        expect(dataSource.getConnection()).andAnswer(new IAnswer<Connection>() {
            @Override
            public Connection answer() throws Throwable {
                return DriverManager.getConnection("jdbc:hsqldb:mem:Log4j", "sa", "");
            }
        }).atLeastOnce();
        replay(dataSource);

        System.out.println("Before creating mock context.");
        MockContextFactory.setAsInitial();

        System.out.println("Before instantiating context.");
        InitialContext context = new InitialContext();
        context.createSubcontext("java:");
        context.createSubcontext("java:/comp");
        context.createSubcontext("java:/comp/env");
        context.createSubcontext("java:/comp/env/jdbc");

        System.out.println("Before binding data source.");
        context.bind("java:/comp/env/jdbc/TestDataSourceAppender", dataSource);

        try {
            System.out.println("Before setting up.");
            this.setUp("dsLogEntry", "log4j2-data-source.xml");

            System.out.println("After setting up.");
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

            long date = resultSet.getTimestamp("eventDate").getTime();
            assertTrue("The date should be later than pre-logging (1).", date >= millis);
            assertTrue("The date should be earlier than now (1).", date <= System.currentTimeMillis());
            assertEquals("The literal column is not correct (1).", "Literal Value of Data Source",
                    resultSet.getString("literalColumn"));
            assertEquals("The level column is not correct (1).", "FATAL", resultSet.getString("level"));
            assertEquals("The logger column is not correct (1).", logger.getName(), resultSet.getString("logger"));
            assertEquals("The message column is not correct (1).", "Error from data source 02.",
                    resultSet.getString("message"));
            assertEquals("The exception column is not correct (1).", stackTrace, resultSet.getString("exception"));

            assertFalse("There should not be two rows.", resultSet.next());

            verify(dataSource);
        } finally {
            MockContextFactory.revertSetAsInitial();
        }
    }

    @SuppressWarnings("unused")
    public static Connection testFactoryMethodConfigMethod() throws SQLException {
        return DriverManager.getConnection("jdbc:hsqldb:mem:Log4j;ifexists=true", "sa", "");
    }
}
