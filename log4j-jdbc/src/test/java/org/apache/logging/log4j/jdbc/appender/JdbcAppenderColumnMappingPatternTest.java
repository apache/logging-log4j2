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
package org.apache.logging.log4j.jdbc.appender;

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
import org.apache.logging.log4j.core.util.Throwables;
import org.apache.logging.log4j.junit.JndiRule;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.message.MapMessage;
import org.h2.util.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Unit tests {@link ColumnMapping}s for JdbcAppender using a pattern for formatting.
 */
public class JdbcAppenderColumnMappingPatternTest {

  @Rule
  public final RuleChain rules;
  private final JdbcRule jdbcRule;

  public JdbcAppenderColumnMappingPatternTest() {
      this(new JdbcRule(JdbcH2TestHelper.TEST_CONFIGURATION_SOURCE,
          "CREATE TABLE dsMappingLogEntry (id INTEGER IDENTITY, level VARCHAR(10), logger VARCHAR(255), message VARCHAR(1024), exception CLOB)",
          "DROP TABLE dsMappingLogEntry"));
  }

  protected JdbcAppenderColumnMappingPatternTest(final JdbcRule jdbcRule) {
      this.rules = RuleChain.emptyRuleChain()
              .around(new JndiRule("java:/comp/env/jdbc/TestDataSourceAppender", createMockDataSource()))
              .around(jdbcRule)
              .around(new LoggerContextRule("org/apache/logging/log4j/jdbc/appender/log4j2-data-source-column-mapping-pattern.xml"));
      this.jdbcRule = jdbcRule;
  }
  
  private DataSource createMockDataSource() {
      try {
          final DataSource dataSource = mock(DataSource.class);
          given(dataSource.getConnection()).willAnswer(new Answer<Connection>() {
              @Override
              public Connection answer(final InvocationOnMock invocation) throws Throwable {
                  return jdbcRule.getConnectionSource().getConnection();
              }
          });
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

          final Logger logger = LogManager.getLogger(this.getClass().getName() + ".testDataSourceConfig");
          logger.trace("Data source logged message 01.");
          logger.fatal("Error from data source 02.", exception);

          try (final Statement statement = connection.createStatement();
                  final ResultSet resultSet = statement.executeQuery("SELECT * FROM dsMappingLogEntry ORDER BY id")) {

              assertTrue("There should be at least one row.", resultSet.next());

              assertEquals("The level column is not correct (1).", "FATAL", resultSet.getNString("level"));
              assertEquals("The logger column is not correct (1).", logger.getName(), resultSet.getNString("logger"));
              assertEquals("The message column is not correct (1).", "Error from data source 02.",
                      resultSet.getString("message"));
              assertEquals("The exception column is not correct (1).", stackTrace,
                      IOUtils.readStringAndClose(resultSet.getNClob("exception").getCharacterStream(), -1));

              assertFalse("There should not be two rows.", resultSet.next());
          }
			} 
  }
}