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

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.junit.JdbcRule;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.util.Strings;
import org.h2.util.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Abstract unit test for JdbcAppender using a {@link FactoryMethodConnectionSource} configuration.
 */
public abstract class AbstractJdbcAppenderFactoryMethodTest {

    @Rule
    public final RuleChain rules;
    private final JdbcRule jdbcRule;

    protected AbstractJdbcAppenderFactoryMethodTest(final JdbcRule jdbcRule, final String databaseType) {
        this.rules = RuleChain.emptyRuleChain().around(jdbcRule).around(new LoggerContextRule(
            "org/apache/logging/log4j/core/appender/db/jdbc/log4j2-" + databaseType + "-factory-method.xml"));
        this.jdbcRule = jdbcRule;
    }

    @Test
    public void testFactoryMethodConfig() throws Exception {
        try (final Connection connection = jdbcRule.getConnectionSource().getConnection()) {
            final SQLException exception = new SQLException("Some other error message!");
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            final PrintWriter writer = new PrintWriter(outputStream);
            exception.printStackTrace(writer);
            writer.close();
            final String stackTrace = outputStream.toString();

            final long millis = System.currentTimeMillis();

            ThreadContext.put("some_int", "42");
            final Logger logger = LogManager.getLogger(this.getClass().getName() + ".testFactoryMethodConfig");
            logger.debug("Factory logged message 01.");
            logger.error("Error from factory 02.", exception);

            final Statement statement = connection.createStatement();
            final ResultSet resultSet = statement.executeQuery("SELECT * FROM fmLogEntry ORDER BY id");

            assertTrue("There should be at least one row.", resultSet.next());

            long date = resultSet.getTimestamp("eventDate").getTime();
            long anotherDate = resultSet.getTimestamp("anotherDate").getTime();
            assertEquals(date, anotherDate);
            assertTrue("The date should be later than pre-logging (1).", date >= millis);
            assertTrue("The date should be earlier than now (1).", date <= System.currentTimeMillis());
            assertEquals("The literal column is not correct (1).", "Some Other Literal Value",
                resultSet.getString("literalColumn"));
            assertEquals("The level column is not correct (1).", "DEBUG", resultSet.getNString("level"));
            assertEquals("The logger column is not correct (1).", logger.getName(), resultSet.getNString("logger"));
            assertEquals("The message column is not correct (1).", "Factory logged message 01.",
                resultSet.getString("message"));
            assertEquals("The exception column is not correct (1).", Strings.EMPTY,
                IOUtils.readStringAndClose(resultSet.getNClob("exception").getCharacterStream(), -1));

            assertTrue("There should be two rows.", resultSet.next());

            date = resultSet.getTimestamp("eventDate").getTime();
            anotherDate = resultSet.getTimestamp("anotherDate").getTime();
            assertEquals(date, anotherDate);
            assertTrue("The date should be later than pre-logging (2).", date >= millis);
            assertTrue("The date should be earlier than now (2).", date <= System.currentTimeMillis());
            assertEquals("The literal column is not correct (2).", "Some Other Literal Value",
                resultSet.getString("literalColumn"));
            assertEquals("The level column is not correct (2).", "ERROR", resultSet.getNString("level"));
            assertEquals("The logger column is not correct (2).", logger.getName(), resultSet.getNString("logger"));
            assertEquals("The message column is not correct (2).", "Error from factory 02.",
                resultSet.getString("message"));
            assertEquals("The exception column is not correct (2).", stackTrace,
                IOUtils.readStringAndClose(resultSet.getNClob("exception").getCharacterStream(), -1));

            assertFalse("There should not be three rows.", resultSet.next());
        }
    }

}
