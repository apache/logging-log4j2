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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.Throwables;
import org.apache.logging.log4j.junit.JdbcRule;
import org.apache.logging.log4j.junit.JndiRule;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.message.MapMessage;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * Unit tests {@link MapMessage}s for JdbcAppender using a {@link DataSource} configuration.
 */
public class JdbcAppenderMapMessageDataSourceTest {

    @Rule
    public final RuleChain rules;
    private final JdbcRule jdbcRule;

    public JdbcAppenderMapMessageDataSourceTest() {
        this(new JdbcRule(JdbcH2TestHelper.TEST_CONFIGURATION_SOURCE_MEM,
        // @formatter:off
                "CREATE TABLE dsLogEntry (Id INTEGER IDENTITY, ColumnA VARCHAR(255), ColumnB VARCHAR(255))",
                "DROP TABLE dsLogEntry"));
        // @formatter:on
    }

    protected JdbcAppenderMapMessageDataSourceTest(final JdbcRule jdbcRule) {
        // @formatter:off
        this.rules = RuleChain.emptyRuleChain()
                .around(new JndiRule("java:/comp/env/jdbc/TestDataSourceAppender", createMockDataSource()))
                .around(jdbcRule)
                .around(new LoggerContextRule("org/apache/logging/log4j/core/appender/db/jdbc/log4j2-data-source-map-message.xml"));
        // @formatter:on
        this.jdbcRule = jdbcRule;
    }

    private DataSource createMockDataSource() {
        try {
            final DataSource dataSource = mock(DataSource.class);
            given(dataSource.getConnection()).willAnswer(invocation -> jdbcRule.getConnectionSource().getConnection());
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
            final PrintWriter writer = new PrintWriter(outputStream);
            exception.printStackTrace(writer);
            writer.close();

            final Logger logger = LogManager.getLogger(this.getClass().getName() + ".testDataSourceConfig");
            final MapMessage mapMessage = new MapMessage();
            mapMessage.with("Id", 1);
            mapMessage.with("ColumnA", "ValueA");
            mapMessage.with("ColumnB", "ValueB");
            logger.info(mapMessage);

            try (final Statement statement = connection.createStatement();
                    final ResultSet resultSet = statement
                            .executeQuery("SELECT Id, ColumnA, ColumnB FROM dsLogEntry ORDER BY Id")) {

                assertTrue("There should be at least one row.", resultSet.next());

                Assert.assertEquals(1, resultSet.getInt("Id"));

                assertFalse("There should not be two rows.", resultSet.next());
            }
        }
    }

    @Test
    public void testTruncate() throws SQLException {
        try (final Connection connection = jdbcRule.getConnectionSource().getConnection()) {
            final Logger logger = LogManager.getLogger(this.getClass().getName() + ".testFactoryMethodConfig");
            // Some drivers and database will not allow more data than the column defines.
            // We really need a MySQL databases with a default configuration to test this.
            final MapMessage mapMessage = new MapMessage();
            mapMessage.with("Id", 1);
            mapMessage.with("ColumnA", StringUtils.repeat('A', 1000));
            mapMessage.with("ColumnB", StringUtils.repeat('B', 1000));
            logger.info(mapMessage);
            try (final Statement statement = connection.createStatement();
                    final ResultSet resultSet = statement
                            .executeQuery("SELECT Id, ColumnA, ColumnB FROM dsLogEntry ORDER BY Id")) {

                assertTrue("There should be at least one row.", resultSet.next());

                Assert.assertEquals(1, resultSet.getInt("Id"));

                assertFalse("There should not be two rows.", resultSet.next());
            }
        }
    }
}
