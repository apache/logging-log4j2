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
package org.apache.logging.log4j.core.appender.db.jpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.categories.PerformanceTests;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public abstract class AbstractJpaAppenderTest {
    private final String databaseType;
    private Connection connection;

    public AbstractJpaAppenderTest(final String databaseType) {
        this.databaseType = databaseType;
    }

    protected abstract Connection setUpConnection() throws SQLException;

    public void setUp(final String configFileName) throws SQLException {
        this.connection = this.setUpConnection();

        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY,
                "org/apache/logging/log4j/core/appender/db/jpa/" + configFileName);
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        if (context.getConfiguration() instanceof DefaultConfiguration) {
            context.reconfigure();
        }
        StatusLogger.getLogger().reset();
    }

    public void tearDown() throws SQLException {
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        try {
            final Map<String, Appender> list = context.getConfiguration().getAppenders();
            final Appender appender = list.get("databaseAppender");
            assertNotNull("The appender should not be null.", appender);
            assertTrue("The appender should be a JPAAppender.", appender instanceof JPAAppender);
            ((JPAAppender) appender).getManager().release();
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
    public void testBaseJpaEntityAppender() throws SQLException {
        try {
            this.setUp("log4j2-" + this.databaseType + "-jpa-base.xml");

            final RuntimeException exception = new RuntimeException("Hello, world!");
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            final PrintWriter writer = new PrintWriter(outputStream);
            exception.printStackTrace(writer);
            writer.close();
            final String stackTrace = outputStream.toString().replace("\r\n", "\n");

            final long millis = System.currentTimeMillis();

            final Logger logger1 = LogManager.getLogger(this.getClass().getName() + ".testBaseJpaEntityAppender");
            final Logger logger2 = LogManager.getLogger(this.getClass().getName() + ".testBaseJpaEntityAppenderAgain");
            logger1.info("Test my message 01.");
            logger1.error("This is another message 02.", exception);
            logger2.warn("A final warning has been issued.");

            final Statement statement = this.connection.createStatement();
            final ResultSet resultSet = statement.executeQuery("SELECT * FROM jpaBaseLogEntry ORDER BY id");

            assertTrue("There should be at least one row.", resultSet.next());

            long date = resultSet.getTimestamp("eventDate").getTime();
            assertTrue("The date should be later than pre-logging (1).", date >= millis);
            assertTrue("The date should be earlier than now (1).", date <= System.currentTimeMillis());
            assertEquals("The level column is not correct (1).", "INFO", resultSet.getString("level"));
            assertEquals("The logger column is not correct (1).", logger1.getName(), resultSet.getString("logger"));
            assertEquals("The message column is not correct (1).", "Test my message 01.",
                    resultSet.getString("message"));
            assertNull("The exception column is not correct (1).", resultSet.getString("exception"));

            assertTrue("There should be at least two rows.", resultSet.next());

            date = resultSet.getTimestamp("eventDate").getTime();
            assertTrue("The date should be later than pre-logging (2).", date >= millis);
            assertTrue("The date should be earlier than now (2).", date <= System.currentTimeMillis());
            assertEquals("The level column is not correct (2).", "ERROR", resultSet.getString("level"));
            assertEquals("The logger column is not correct (2).", logger1.getName(), resultSet.getString("logger"));
            assertEquals("The message column is not correct (2).", "This is another message 02.",
                    resultSet.getString("message"));
            assertEquals("The exception column is not correct (2).", stackTrace, resultSet.getString("exception"));

            assertTrue("There should be three rows.", resultSet.next());

            date = resultSet.getTimestamp("eventDate").getTime();
            assertTrue("The date should be later than pre-logging (3).", date >= millis);
            assertTrue("The date should be earlier than now (3).", date <= System.currentTimeMillis());
            assertEquals("The level column is not correct (3).", "WARN", resultSet.getString("level"));
            assertEquals("The logger column is not correct (3).", logger2.getName(), resultSet.getString("logger"));
            assertEquals("The message column is not correct (3).", "A final warning has been issued.",
                    resultSet.getString("message"));
            assertNull("The exception column is not correct (3).", resultSet.getString("exception"));

            assertFalse("There should not be four rows.", resultSet.next());
        } finally {
            this.tearDown();
        }
    }

    @Test
    public void testBasicJpaEntityAppender() throws SQLException {
        try {
            this.setUp("log4j2-" + this.databaseType + "-jpa-basic.xml");

            final Error exception = new Error("Goodbye, cruel world!");
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            final PrintWriter writer = new PrintWriter(outputStream);
            exception.printStackTrace(writer);
            writer.close();
            final String stackTrace = outputStream.toString().replace("\r\n", "\n");

            final long millis = System.currentTimeMillis();

            final Logger logger1 = LogManager.getLogger(this.getClass().getName() + ".testBasicJpaEntityAppender");
            final Logger logger2 = LogManager.getLogger(this.getClass().getName() + ".testBasicJpaEntityAppenderAgain");
            logger1.debug("Test my debug 01.");
            logger1.warn("This is another warning 02.", exception);
            logger2.fatal("A fatal warning has been issued.");

            final Statement statement = this.connection.createStatement();
            final ResultSet resultSet = statement.executeQuery("SELECT * FROM jpaBasicLogEntry ORDER BY id");

            assertTrue("There should be at least one row.", resultSet.next());

            long date = resultSet.getLong("timemillis");
            assertTrue("The date should be later than pre-logging (1).", date >= millis);
            assertTrue("The date should be earlier than now (1).", date <= System.currentTimeMillis());
            assertEquals("The level column is not correct (1).", "DEBUG", resultSet.getString("level"));
            assertEquals("The logger column is not correct (1).", logger1.getName(), resultSet.getString("loggerName"));
            assertEquals("The message column is not correct (1).", "Test my debug 01.",
                    resultSet.getString("message"));
            assertNull("The exception column is not correct (1).", resultSet.getString("thrown"));

            assertTrue("There should be at least two rows.", resultSet.next());

            date = resultSet.getLong("timemillis");
            assertTrue("The date should be later than pre-logging (2).", date >= millis);
            assertTrue("The date should be earlier than now (2).", date <= System.currentTimeMillis());
            assertEquals("The level column is not correct (2).", "WARN", resultSet.getString("level"));
            assertEquals("The logger column is not correct (2).", logger1.getName(), resultSet.getString("loggerName"));
            assertEquals("The message column is not correct (2).", "This is another warning 02.",
                    resultSet.getString("message"));
            assertEquals("The exception column is not correct (2).", stackTrace, resultSet.getString("thrown"));

            assertTrue("There should be three rows.", resultSet.next());

            date = resultSet.getLong("timemillis");
            assertTrue("The date should be later than pre-logging (3).", date >= millis);
            assertTrue("The date should be earlier than now (3).", date <= System.currentTimeMillis());
            assertEquals("The level column is not correct (3).", "FATAL", resultSet.getString("level"));
            assertEquals("The logger column is not correct (3).", logger2.getName(), resultSet.getString("loggerName"));
            assertEquals("The message column is not correct (3).", "A fatal warning has been issued.",
                    resultSet.getString("message"));
            assertNull("The exception column is not correct (3).", resultSet.getString("thrown"));

            assertFalse("There should not be four rows.", resultSet.next());
        } finally {
            this.tearDown();
        }
    }

    @Test
    @Category(PerformanceTests.class)
    public void testPerformanceOfAppenderWith10000EventsUsingBasicEntity() throws SQLException {
        try {
            this.setUp("log4j2-" + this.databaseType + "-jpa-basic.xml");

            final Error exception = new Error("Goodbye, cruel world!");

            final Logger logger = LogManager.getLogger(this.getClass().getName() +
                    ".testPerformanceOfAppenderWith10000EventsUsingBasicEntity");
            logger.info("This is a warm-up message.");

            System.out.println("Starting a performance test for JPA Appender for " + this.databaseType + '.');

            long start = System.nanoTime();

            for(int i = 0; i < 10000; i++) {
                if (i % 25 == 0) {
                    logger.warn("This is an exception message.", exception);
                } else {
                    logger.info("This is an info message.");
                }
            }

            long elapsed = System.nanoTime() - start;
            long elapsedMilli = elapsed / 1000000;

            final Statement statement = this.connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
            final ResultSet resultSet = statement.executeQuery("SELECT * FROM jpaBasicLogEntry ORDER BY id");

            resultSet.last();
            assertEquals("The number of records is not correct.", 10001, resultSet.getRow());

            System.out.println("Wrote 10,000 log events in " + elapsed + " nanoseconds (" + elapsedMilli +
                    " milliseconds) for " + this.databaseType + '.');
        } finally {
            this.tearDown();
        }
    }
}
