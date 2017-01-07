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

import org.apache.logging.log4j.junit.JdbcRule;

/**
 *
 */
public class JdbcAppenderHsqlDataSourceTest extends AbstractJdbcAppenderDataSourceTest {
    public JdbcAppenderHsqlDataSourceTest() {
        super(new JdbcRule(
            new ConnectionSource() {
                @Override
                public Connection getConnection() throws SQLException {
                    return DriverManager.getConnection("jdbc:hsqldb:mem:Log4j", "sa", "");
                }
            },
            "CREATE TABLE dsLogEntry (" +
                "id INTEGER IDENTITY, eventDate DATETIME, literalColumn VARCHAR(255), level VARCHAR(10), " +
                "logger VARCHAR(255), message VARCHAR(1024), exception CLOB, anotherDate TIMESTAMP" +
                ")",
            "DROP TABLE dsLogEntry"
        ));
    }
}
