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

import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("Appenders.Jpa")
class JpaHsqldbAppenderTest extends AbstractJpaAppenderTest {
    private static final String USER_ID = "sa";
    private static final String PASSWORD = "123";

    public JpaHsqldbAppenderTest() {
        super("hsqldb");
    }

    @Override
    protected Connection setUpConnection() throws SQLException {
        final Connection connection = DriverManager.getConnection("jdbc:hsqldb:mem:Log4j", USER_ID, PASSWORD);

        try (final Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE jpaBaseLogEntry ( "
                    + "id INTEGER IDENTITY, eventDate DATETIME, instant NVARCHAR(64), level VARCHAR(10), "
                    + "logger VARCHAR(255), message VARCHAR(1024), exception VARCHAR(1048576) )");
        }

        try (final Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE jpaBasicLogEntry ( "
                    + "id INTEGER IDENTITY, timemillis BIGINT, instant NVARCHAR(64), nanoTime BIGINT, "
                    + "level VARCHAR(10), loggerName VARCHAR(255), message VARCHAR(1024), thrown VARCHAR(1048576), "
                    + "contextMapJson VARCHAR(1048576), loggerFQCN VARCHAR(1024), "
                    + "contextStack VARCHAR(1048576), marker VARCHAR(255), source VARCHAR(2048),"
                    + "threadId BIGINT, threadName NVARCHAR(255), threadPriority INTEGER )");
        }

        return connection;
    }

    @Test
    void testNoEntityClassName() {
        final JpaAppender appender = JpaAppender.createAppender("name", null, null, null, null, "jpaAppenderTestUnit");

        assertNull(appender, "The appender should be null.");
    }

    @Test
    void testNoPersistenceUnitName() {
        final JpaAppender appender =
                JpaAppender.createAppender("name", null, null, null, TestBaseEntity.class.getName(), null);

        assertNull(appender, "The appender should be null.");
    }

    @Test
    void testBadEntityClassName() {
        final JpaAppender appender =
                JpaAppender.createAppender("name", null, null, null, "com.foo.Bar", "jpaAppenderTestUnit");

        assertNull(appender, "The appender should be null.");
    }

    @Test
    void testNonLogEventEntity() {
        final JpaAppender appender =
                JpaAppender.createAppender("name", null, null, null, Object.class.getName(), "jpaAppenderTestUnit");

        assertNull(appender, "The appender should be null.");
    }

    @Test
    void testBadConstructorEntity01() {
        final JpaAppender appender = JpaAppender.createAppender(
                "name", null, null, null, BadConstructorEntity1.class.getName(), "jpaAppenderTestUnit");

        assertNull(appender, "The appender should be null.");
    }

    @Test
    void testBadConstructorEntity02() {
        final JpaAppender appender = JpaAppender.createAppender(
                "name", null, null, null, BadConstructorEntity2.class.getName(), "jpaAppenderTestUnit");

        assertNull(appender, "The appender should be null.");
    }

    @SuppressWarnings("unused")
    public static class BadConstructorEntity1 extends TestBaseEntity {
        private static final long serialVersionUID = 1L;

        public BadConstructorEntity1(final LogEvent wrappedEvent) {
            super(wrappedEvent);
        }
    }

    @SuppressWarnings("unused")
    public static class BadConstructorEntity2 extends TestBaseEntity {
        private static final long serialVersionUID = 1L;

        public BadConstructorEntity2() {
            super(null);
        }

        public BadConstructorEntity2(final LogEvent wrappedEvent, final String badParameter) {
            super(wrappedEvent);
        }
    }
}
