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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.apache.logging.log4j.core.test.junit.JdbcRule;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;

/**
 *
 */
@LoggerContextSource("org/apache/logging/log4j/core/appender/db/jdbc/log4j2-hsqldb-factory-method.xml")
public class JdbcAppenderHsqldbFactoryMethodTest extends AbstractJdbcAppenderFactoryMethodTest {

    public JdbcAppenderHsqldbFactoryMethodTest() {
        super(new JdbcRule(
                new AbstractConnectionSource() {
                    @Override
                    public Connection getConnection() throws SQLException {
                        return JdbcAppenderHsqldbFactoryMethodTest.getConnection();
                    }
                },
                "CREATE TABLE fmLogEntry ("
                        + "id INTEGER IDENTITY, eventDate DATETIME, literalColumn VARCHAR(255), level VARCHAR(10), "
                        + "logger VARCHAR(255), message VARCHAR(1024), exception CLOB, anotherDate TIMESTAMP"
                        + ")",
                "DROP TABLE IF EXISTS fmLogEntry"));
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:hsqldb:mem:Log4j", "sa", "");
    }
}
