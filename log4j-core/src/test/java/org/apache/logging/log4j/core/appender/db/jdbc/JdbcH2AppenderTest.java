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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.logging.log4j.util.Strings;

/**
 * Tests the JDBC appender with the H2 database in memory.
 */
public class JdbcH2AppenderTest extends AbstractJdbcAppenderTest {
    private static final String USER_ID = "sa";
    private static final String PASSWORD = Strings.EMPTY;

    public JdbcH2AppenderTest() {
        super("h2");
    }

    /**
     * Referred from log4j2-h2-factory-method.xml.
     */
    public static Connection getConfigConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:Log4j", USER_ID, PASSWORD);
    }

    @Override
    protected Connection newConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:Log4j", USER_ID, PASSWORD);
    }

    @Override
    protected String toCreateTableSqlString(final String tableName) {
        return "CREATE TABLE " + tableName + " ( " +
                    "id INTEGER IDENTITY, eventDate DATETIME, literalColumn VARCHAR(255), level NVARCHAR(10), " +
                    "logger NVARCHAR(255), message VARCHAR(1024), exception NCLOB" +
                " )";
    }
}
