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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.test.junit.JdbcRule;
import org.apache.logging.log4j.core.test.junit.JndiRule;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.apache.logging.log4j.core.util.Throwables;
import org.h2.util.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * Abstract unit test for JdbcAppender using a {@link DataSource} configuration.
 */
public abstract class AbstractJdbcAppenderDataSourceTest extends AbstractJdbcDataSourceTest {

    @Rule
    public final RuleChain rules;

    private final JdbcRule jdbcRule;

    protected AbstractJdbcAppenderDataSourceTest(final JdbcRule jdbcRule) {
        this.rules = RuleChain.emptyRuleChain()
                .around(new JndiRule("java:/comp/env/jdbc/TestDataSourceAppender", createMockDataSource()))
                .around(jdbcRule)
                .around(new LoggerContextRule("org/apache/logging/log4j/core/appender/db/jdbc/log4j2-data-source.xml"));
        this.jdbcRule = jdbcRule;
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

                assertTrue("There should be at least one row.", resultSet.next());

                final long date = resultSet.getTimestamp("eventDate").getTime();
                assertTrue("The date should be later than pre-logging (1).", date >= millis);
                assertTrue("The date should be earlier than now (1).", date <= System.currentTimeMillis());
                assertEquals(
                        "The literal column is not correct (1).",
                        "Literal Value of Data Source",
                        resultSet.getString("literalColumn"));
                assertEquals("The level column is not correct (1).", "FATAL", resultSet.getNString("level"));
                assertEquals("The logger column is not correct (1).", logger.getName(), resultSet.getNString("logger"));
                assertEquals(
                        "The message column is not correct (1).",
                        "Error from data source 02.",
                        resultSet.getString("message"));
                assertEquals(
                        "The exception column is not correct (1).",
                        stackTrace,
                        IOUtils.readStringAndClose(
                                resultSet.getNClob("exception").getCharacterStream(), -1));

                assertFalse("There should not be two rows.", resultSet.next());
            }
        }
    }
}
