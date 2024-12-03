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
package org.apache.logging.log4j.core.appender.db.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("Appenders.Jpa")
public abstract class AbstractJpaAppenderTest {
    private final String databaseType;
    private Connection connection;

    public AbstractJpaAppenderTest(final String databaseType) {
        this.databaseType = databaseType;
    }

    protected abstract Connection setUpConnection() throws SQLException;

    public void setUp(final String configFileName) throws SQLException {
        this.connection = this.setUpConnection();

        System.setProperty(
                ConfigurationFactory.CONFIGURATION_FILE_PROPERTY,
                "org/apache/logging/log4j/core/appender/db/jpa/" + configFileName);
        final LoggerContext context = LoggerContext.getContext(false);
        if (context.getConfiguration() instanceof DefaultConfiguration) {
            context.reconfigure();
        }
        StatusLogger.getLogger().reset();
    }

    public void tearDown() throws SQLException {
        final LoggerContext context = LoggerContext.getContext(false);
        try {
            final String appenderName = "databaseAppender";
            final Appender appender = context.getConfiguration().getAppender(appenderName);
            assertNotNull(appender, "The appender '" + appenderName + "' should not be null.");
            assertInstanceOf(JpaAppender.class, appender, "The appender should be a JpaAppender.");
            ((JpaAppender) appender).getManager().close();
        } finally {
            System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
            context.reconfigure();
            StatusLogger.getLogger().reset();

            if (this.connection != null) {
                try (final Statement statement = this.connection.createStatement()) {
                    statement.execute("SHUTDOWN");
                }
                this.connection.close();
            }
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

            assertTrue(resultSet.next(), "There should be at least one row.");

            long date = resultSet.getTimestamp("eventDate").getTime();
            assertTrue(date >= millis, "The date should be later than pre-logging (1).");
            assertTrue(date <= System.currentTimeMillis(), "The date should be earlier than now (1).");
            assertEquals("INFO", resultSet.getString("level"), "The level column is not correct (1).");
            assertEquals(logger1.getName(), resultSet.getString("logger"), "The logger column is not correct (1).");
            assertEquals(
                    "Test my message 01.", resultSet.getString("message"), "The message column is not correct (1).");
            assertNull(resultSet.getString("exception"), "The exception column is not correct (1).");

            assertTrue(resultSet.next(), "There should be at least two rows.");

            date = resultSet.getTimestamp("eventDate").getTime();
            assertTrue(date >= millis, "The date should be later than pre-logging (2).");
            assertTrue(date <= System.currentTimeMillis(), "The date should be earlier than now (2).");
            assertEquals("ERROR", resultSet.getString("level"), "The level column is not correct (2).");
            assertEquals(logger1.getName(), resultSet.getString("logger"), "The logger column is not correct (2).");
            assertEquals(
                    "This is another message 02.",
                    resultSet.getString("message"),
                    "The message column is not correct (2).");
            assertEquals(stackTrace, resultSet.getString("exception"), "The exception column is not correct (2).");

            assertTrue(resultSet.next(), "There should be three rows.");

            date = resultSet.getTimestamp("eventDate").getTime();
            assertTrue(date >= millis, "The date should be later than pre-logging (3).");
            assertTrue(date <= System.currentTimeMillis(), "The date should be earlier than now (3).");
            assertEquals("WARN", resultSet.getString("level"), "The level column is not correct (3).");
            assertEquals(logger2.getName(), resultSet.getString("logger"), "The logger column is not correct (3).");
            assertEquals(
                    "A final warning has been issued.",
                    resultSet.getString("message"),
                    "The message column is not correct (3).");
            assertNull(resultSet.getString("exception"), "The exception column is not correct (3).");

            assertFalse(resultSet.next(), "There should not be four rows.");
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

            assertTrue(resultSet.next(), "There should be at least one row.");

            long date = resultSet.getLong("timemillis");
            assertTrue(date >= millis, "The date should be later than pre-logging (1).");
            assertTrue(date <= System.currentTimeMillis(), "The date should be earlier than now (1).");
            assertEquals("DEBUG", resultSet.getString("level"), "The level column is not correct (1).");
            assertEquals(logger1.getName(), resultSet.getString("loggerName"), "The logger column is not correct (1).");
            assertEquals("Test my debug 01.", resultSet.getString("message"), "The message column is not correct (1).");
            assertNull(resultSet.getString("thrown"), "The exception column is not correct (1).");

            assertTrue(resultSet.next(), "There should be at least two rows.");

            date = resultSet.getLong("timemillis");
            assertTrue(date >= millis, "The date should be later than pre-logging (2).");
            assertTrue(date <= System.currentTimeMillis(), "The date should be earlier than now (2).");
            assertEquals("WARN", resultSet.getString("level"), "The level column is not correct (2).");
            assertEquals(logger1.getName(), resultSet.getString("loggerName"), "The logger column is not correct (2).");
            assertEquals(
                    "This is another warning 02.",
                    resultSet.getString("message"),
                    "The message column is not correct (2).");
            assertEquals(stackTrace, resultSet.getString("thrown"), "The exception column is not correct (2).");

            assertTrue(resultSet.next(), "There should be three rows.");

            date = resultSet.getLong("timemillis");
            assertTrue(date >= millis, "The date should be later than pre-logging (3).");
            assertTrue(date <= System.currentTimeMillis(), "The date should be earlier than now (3).");
            assertEquals("FATAL", resultSet.getString("level"), "The level column is not correct (3).");
            assertEquals(logger2.getName(), resultSet.getString("loggerName"), "The logger column is not correct (3).");
            assertEquals(
                    "A fatal warning has been issued.",
                    resultSet.getString("message"),
                    "The message column is not correct (3).");
            assertNull(resultSet.getString("thrown"), "The exception column is not correct (3).");

            assertFalse(resultSet.next(), "There should not be four rows.");
        } finally {
            this.tearDown();
        }
    }
}
