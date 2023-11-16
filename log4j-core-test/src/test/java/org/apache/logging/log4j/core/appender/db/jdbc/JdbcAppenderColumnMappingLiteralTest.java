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

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.test.RuleChainFactory;
import org.apache.logging.log4j.core.test.appender.db.jdbc.JdbcH2TestHelper;
import org.apache.logging.log4j.core.test.junit.JdbcRule;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.h2.util.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class JdbcAppenderColumnMappingLiteralTest extends AbstractH2Test {

    @Rule
    public final RuleChain rules;

    private final JdbcRule jdbcRule;

    public JdbcAppenderColumnMappingLiteralTest() {
        this(new JdbcRule(
                JdbcH2TestHelper.TEST_CONFIGURATION_SOURCE_TMPDIR,
                "CREATE TABLE dsMappingLogEntry (id INTEGER, level VARCHAR(10), logger VARCHAR(255), message VARCHAR(1024), exception CLOB)",
                "DROP TABLE IF EXISTS dsMappingLogEntry"));
    }

    protected JdbcAppenderColumnMappingLiteralTest(final JdbcRule jdbcRule) {
        this.rules = RuleChainFactory.create(
                jdbcRule,
                new LoggerContextRule(
                        "org/apache/logging/log4j/core/appender/db/jdbc/log4j2-dm-column-mapping-literal.xml"));
        this.jdbcRule = jdbcRule;
    }

    @Test
    public void test() throws Exception {
        try (final Connection connection = jdbcRule.getConnection()) {
            final Error exception = new Error("This is a test.");
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (final PrintWriter writer = new PrintWriter(outputStream)) {
                exception.printStackTrace(writer);
            }
            final String stackTrace = outputStream.toString();

            final Logger logger = LogManager.getLogger(this.getClass().getName() + ".testDataSourceConfig");
            logger.trace("Data source logged message 01.");
            logger.fatal("Error from data source 02.", exception);
            Thread.sleep(1000);
            try (final Statement statement = connection.createStatement();
                    final ResultSet resultSet = statement.executeQuery("SELECT * FROM dsMappingLogEntry ORDER BY id")) {

                assertTrue("There should be at least one row.", resultSet.next());

                assertEquals("The level column is not correct (1).", "FATAL", resultSet.getNString("level"));
                assertEquals("The logger column is not correct (1).", logger.getName(), resultSet.getNString("logger"));
                assertEquals("The message column is not correct (1).", "Hello World!", resultSet.getString("message"));
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
