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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.test.junit.JdbcRule;
import org.apache.logging.log4j.util.Strings;
import org.h2.util.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Abstract unit test for JdbcAppender using a {@link FactoryMethodConnectionSource} configuration.
 */
public abstract class AbstractJdbcAppenderFactoryMethodTest {

    @RegisterExtension
    private final JdbcRule jdbcRule;

    protected AbstractJdbcAppenderFactoryMethodTest(final JdbcRule jdbcRule) {
        this.jdbcRule = jdbcRule;
    }

    @Test
    public void testFactoryMethodConfig() throws Exception {
        try (final Connection connection = jdbcRule.getConnectionSource().getConnection()) {
            final SQLException exception = new SQLException("Some other error message!");
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (final PrintWriter writer = new PrintWriter(outputStream)) {
                exception.printStackTrace(writer);
            }
            final String stackTrace = outputStream.toString();

            final long millis = System.currentTimeMillis();

            ThreadContext.put("some_int", "42");
            final Logger logger = LogManager.getLogger(this.getClass().getName() + ".testFactoryMethodConfig");
            logger.debug("Factory logged message 01.");
            logger.error("Error from factory 02.", exception);

            try (final Statement statement = connection.createStatement();
                    final ResultSet resultSet = statement.executeQuery("SELECT * FROM fmLogEntry ORDER BY id")) {

                assertTrue(resultSet.next(), "There should be at least one row.");

                long date = resultSet.getTimestamp("eventDate").getTime();
                long anotherDate = resultSet.getTimestamp("anotherDate").getTime();
                assertEquals(date, anotherDate);
                assertTrue(date >= millis, "The date should be later than pre-logging (1).");
                assertTrue(date <= System.currentTimeMillis(), "The date should be earlier than now (1).");
                assertEquals(
                        "Some Other Literal Value",
                        resultSet.getString("literalColumn"),
                        "The literal column is not correct (1).");
                assertEquals("DEBUG", resultSet.getNString("level"), "The level column is not correct (1).");
                assertEquals(logger.getName(), resultSet.getNString("logger"), "The logger column is not correct (1).");
                assertEquals(
                        "Factory logged message 01.",
                        resultSet.getString("message"),
                        "The message column is not correct (1).");
                assertEquals(
                        Strings.EMPTY,
                        IOUtils.readStringAndClose(
                                resultSet.getNClob("exception").getCharacterStream(), -1),
                        "The exception column is not correct (1).");

                assertTrue(resultSet.next(), "There should be two rows.");

                date = resultSet.getTimestamp("eventDate").getTime();
                anotherDate = resultSet.getTimestamp("anotherDate").getTime();
                assertEquals(date, anotherDate);
                assertTrue(date >= millis, "The date should be later than pre-logging (2).");
                assertTrue(date <= System.currentTimeMillis(), "The date should be earlier than now (2).");
                assertEquals(
                        "Some Other Literal Value",
                        resultSet.getString("literalColumn"),
                        "The literal column is not correct (2).");
                assertEquals("ERROR", resultSet.getNString("level"), "The level column is not correct (2).");
                assertEquals(logger.getName(), resultSet.getNString("logger"), "The logger column is not correct (2).");
                assertEquals(
                        "Error from factory 02.",
                        resultSet.getString("message"),
                        "The message column is not correct (2).");
                assertEquals(
                        stackTrace,
                        IOUtils.readStringAndClose(
                                resultSet.getNClob("exception").getCharacterStream(), -1),
                        "The exception column is not correct (2).");

                assertFalse(resultSet.next(), "There should not be three rows.");
            }
        }
    }

    @Test
    public void testTruncate() {
        final Logger logger = LogManager.getLogger(this.getClass().getName() + ".testFactoryMethodConfig");
        // Some drivers and database will not allow more data than the column defines.
        // We really need a MySQL databases with a default configuration to test this.
        logger.debug(StringUtils.repeat('A', 1000));
    }
}
