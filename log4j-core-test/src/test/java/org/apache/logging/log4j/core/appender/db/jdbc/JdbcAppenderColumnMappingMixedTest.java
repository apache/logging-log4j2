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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.test.RuleChainFactory;
import org.apache.logging.log4j.core.test.appender.db.jdbc.JdbcH2TestHelper;
import org.apache.logging.log4j.core.test.junit.JdbcRule;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.apache.logging.log4j.message.StringMapMessage;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * A source column, a literal timestamp, and pattern columns must keep their JDBC parameter indexes.
 */
public class JdbcAppenderColumnMappingMixedTest extends AbstractH2Test {

    @Rule
    public final RuleChain rules;

    private final JdbcRule jdbcRule;

    private final LoggerContextRule loggerContextRule;

    public JdbcAppenderColumnMappingMixedTest() {
        this(new JdbcRule(
                JdbcH2TestHelper.TEST_CONFIGURATION_SOURCE_MEM,
                "CREATE TABLE dsMixedLogEntry (id INTEGER, stamp TIMESTAMP, level VARCHAR(10), message VARCHAR(255))",
                "DROP TABLE IF EXISTS dsMixedLogEntry"));
    }

    protected JdbcAppenderColumnMappingMixedTest(final JdbcRule jdbcRule) {
        this.loggerContextRule = new LoggerContextRule(
                "org/apache/logging/log4j/core/appender/db/jdbc/log4j2-dm-column-mapping-mixed.xml");
        this.rules = RuleChainFactory.create(jdbcRule, loggerContextRule);
        this.jdbcRule = jdbcRule;
    }

    @Test
    public void testSourceColumnKeepsItsParameter() throws Exception {
        try (final Connection connection = jdbcRule.getConnection()) {
            final Logger logger = loggerContextRule.getLogger(
                    getClass().getName() + ".testSourceColumnKeepsItsParameter");
            final StringMapMessage mapMessage = new StringMapMessage();
            mapMessage.put("id", "7");
            mapMessage.put("message", "from-map");
            logger.error(mapMessage);

            try (final Statement statement = connection.createStatement();
                    final ResultSet resultSet =
                            statement.executeQuery("SELECT id, level, message FROM dsMixedLogEntry")) {
                assertTrue("There should be one row.", resultSet.next());
                assertEquals("The id column is not correct.", 7, resultSet.getInt("id"));
                assertEquals("The level column is not correct.", "ERROR", resultSet.getString("level"));
                assertFalse("The message column should not be the map id.", "7".equals(resultSet.getString("message")));
                assertFalse("There should not be two rows.", resultSet.next());
            }
        }
    }
}
