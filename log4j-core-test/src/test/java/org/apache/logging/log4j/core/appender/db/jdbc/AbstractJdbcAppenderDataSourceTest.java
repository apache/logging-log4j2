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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.test.junit.JdbcRule;
import org.apache.logging.log4j.core.test.junit.JndiExtension;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.util.Throwables;
import org.h2.util.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Abstract unit test for JdbcAppender using a {@link DataSource} configuration.
 */
public abstract class AbstractJdbcAppenderDataSourceTest extends AbstractJdbcDataSourceTest {

    @RegisterExtension
    public JndiExtension ext = new JndiExtension(createBindings());

    @RegisterExtension
    private final JdbcRule jdbcRule;

    protected AbstractJdbcAppenderDataSourceTest(final JdbcRule jdbcRule) {
        this.jdbcRule = jdbcRule;
    }

    private Map<String, Object> createBindings() {
        final Map<String, Object> map = new HashMap<>();
        map.put("java:/comp/env/jdbc/TestDataSourceAppender", createMockDataSource());
        return map;
    }

    private DataSource createMockDataSource() {
        try {
            final DataSource dataSource = mock(DataSource.class);
            given(dataSource.getConnection())
                    .willAnswer(invocation -> jdbcRule.getConnectionSource().getConnection());
            return dataSource;
        } catch (final SQLException e) {
            Throwables.rethrow(e);
            throw new InternalError("unreachable");
        }
    }

    @Test
    @LoggerContextSource("org/apache/logging/log4j/core/appender/db/jdbc/log4j2-data-source.xml")
    public void testDataSourceConfig() throws Exception {
        try (final Connection connection = jdbcRule.getConnectionSource().getConnection()) {
            final Error exception = new Error("Final error massage is fatal!");
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (final PrintWriter writer = new PrintWriter(outputStream)) {
                exception.printStackTrace(writer);
            }
            final String stackTrace = outputStream.toString();

            final long millis = System.currentTimeMillis();

            final Logger logger = LogManager.getLogger(this.getClass().getName() + ".testDataSourceConfig");
            logger.trace("Data source logged message 01.");
            logger.fatal("Error from data source 02.", exception);

            try (final Statement statement = connection.createStatement();
                    final ResultSet resultSet = statement.executeQuery("SELECT * FROM dsLogEntry ORDER BY id")) {

                assertTrue(resultSet.next(), "There should be at least one row.");

                final long date = resultSet.getTimestamp("eventDate").getTime();
                assertTrue(date >= millis, "The date should be later than pre-logging (1).");
                assertTrue(date <= System.currentTimeMillis(), "The date should be earlier than now (1).");
                assertEquals(
                        "Literal Value of Data Source",
                        resultSet.getString("literalColumn"),
                        "The literal column is not correct (1).");
                assertEquals("FATAL", resultSet.getNString("level"), "The level column is not correct (1).");
                assertEquals(logger.getName(), resultSet.getNString("logger"), "The logger column is not correct (1).");
                assertEquals(
                        "Error from data source 02.",
                        resultSet.getString("message"),
                        "The message column is not correct (1).");
                assertEquals(
                        stackTrace,
                        IOUtils.readStringAndClose(
                                resultSet.getNClob("exception").getCharacterStream(), -1),
                        "The exception column is not correct (1).");

                assertFalse(resultSet.next(), "There should not be two rows.");
            }
        }
    }
}
