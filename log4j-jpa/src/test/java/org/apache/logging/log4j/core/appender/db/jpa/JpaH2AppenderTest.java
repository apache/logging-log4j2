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
package org.apache.logging.log4j.core.appender.db.jpa;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Tag;

@Tag("Appenders.Jpa")
class JpaH2AppenderTest extends AbstractJpaAppenderTest {
    private static final String USER_ID = "sa";
    private static final String PASSWORD = "123";

    public JpaH2AppenderTest() {
        super("h2");
    }

    @Override
    protected Connection setUpConnection() throws SQLException {
        final Connection connection = DriverManager.getConnection("jdbc:h2:mem:Log4j", USER_ID, PASSWORD);

        try (final Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE jpaBaseLogEntry ( "
                    + "id INTEGER GENERATED ALWAYS AS IDENTITY, eventDate DATETIME, instant NVARCHAR(64), level NVARCHAR(10), "
                    + "logger NVARCHAR(255), message NVARCHAR(1024), exception NVARCHAR(1048576) )");
        }

        try (final Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE jpaBasicLogEntry ( "
                    + "id INTEGER GENERATED ALWAYS AS IDENTITY, timemillis BIGINT, instant NVARCHAR(64), nanoTime BIGINT, "
                    + "level NVARCHAR(10), loggerName NVARCHAR(255), message NVARCHAR(1024), "
                    + "thrown NVARCHAR(1048576), contextMapJson NVARCHAR(1048576), loggerFQCN NVARCHAR(1024), "
                    + "contextStack NVARCHAR(1048576), marker NVARCHAR(255), source NVARCHAR(2048),"
                    + "threadId BIGINT, threadName NVARCHAR(255), threadPriority INTEGER )");
        }

        return connection;
    }
}
