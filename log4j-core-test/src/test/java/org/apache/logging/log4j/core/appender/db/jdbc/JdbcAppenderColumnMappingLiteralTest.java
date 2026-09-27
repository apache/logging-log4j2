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
import java.sql.Statement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.test.appender.db.jdbc.JdbcH2TestHelper;
import org.apache.logging.log4j.core.test.junit.JdbcRule;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.h2.util.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@LoggerContextSource("org/apache/logging/log4j/core/appender/db/jdbc/log4j2-dm-column-mapping-literal.xml")
public class JdbcAppenderColumnMappingLiteralTest extends AbstractH2Test {

    @RegisterExtension
    private final JdbcRule jdbcRule = new JdbcRule(
            JdbcH2TestHelper.TEST_CONFIGURATION_SOURCE_TMPDIR,
            "CREATE TABLE dsMappingLogEntry (id INTEGER, level VARCHAR(10), logger VARCHAR(255), message VARCHAR(1024), exception CLOB)",
            "DROP TABLE IF EXISTS dsMappingLogEntry");

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

                assertTrue(resultSet.next(), "There should be at least one row.");

                assertEquals("FATAL", resultSet.getNString("level"), "The level column is not correct (1).");
                assertEquals(logger.getName(), resultSet.getNString("logger"), "The logger column is not correct (1).");
                assertEquals("Hello World!", resultSet.getString("message"), "The message column is not correct (1).");
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
