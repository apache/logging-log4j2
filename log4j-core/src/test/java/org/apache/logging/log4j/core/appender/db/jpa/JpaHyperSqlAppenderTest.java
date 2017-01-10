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
package org.apache.logging.log4j.core.appender.db.jpa;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.categories.Appenders;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.util.Strings;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.*;

@Category(Appenders.Jpa.class)
public class JpaHyperSqlAppenderTest extends AbstractJpaAppenderTest {
    private static final String USER_ID = "sa";
    private static final String PASSWORD = Strings.EMPTY;

    public JpaHyperSqlAppenderTest() {
        super("hsqldb");
    }

    @Override
    protected Connection setUpConnection() throws SQLException {
        final Connection connection = DriverManager.getConnection("jdbc:hsqldb:mem:Log4j", USER_ID, PASSWORD);

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE jpaBaseLogEntry ( "
                    + "id INTEGER IDENTITY, eventDate DATETIME, level VARCHAR(10), logger VARCHAR(255), "
                    + "message VARCHAR(1024), exception VARCHAR(1048576)" + " )");
        }

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE jpaBasicLogEntry ( "
                    + "id INTEGER IDENTITY, timemillis BIGINT, nanoTime BIGINT, level VARCHAR(10), loggerName VARCHAR(255), "
                    + "message VARCHAR(1024), thrown VARCHAR(1048576), contextMapJson VARCHAR(1048576),"
                    + "loggerFQCN VARCHAR(1024), contextStack VARCHAR(1048576), marker VARCHAR(255), source VARCHAR(2048),"
                    + "threadId BIGINT, threadName NVARCHAR(255), threadPriority INTEGER" + " )");
        }

        return connection;
    }

    @Test
    public void testNoEntityClassName() {
        final JpaAppender appender = JpaAppender.createAppender("name", null, null, null, null, "jpaAppenderTestUnit");

        assertNull("The appender should be null.", appender);
    }

    @Test
    public void testNoPersistenceUnitName() {
        final JpaAppender appender = JpaAppender.createAppender("name", null, null, null, TestBaseEntity.class.getName(),
                null);

        assertNull("The appender should be null.", appender);
    }

    @Test
    public void testBadEntityClassName() {
        final JpaAppender appender = JpaAppender.createAppender("name", null, null, null, "com.foo.Bar",
                "jpaAppenderTestUnit");

        assertNull("The appender should be null.", appender);
    }

    @Test
    public void testNonLogEventEntity() {
        final JpaAppender appender = JpaAppender.createAppender("name", null, null, null, Object.class.getName(),
                "jpaAppenderTestUnit");

        assertNull("The appender should be null.", appender);
    }

    @Test
    public void testBadConstructorEntity01() {
        final JpaAppender appender = JpaAppender.createAppender("name", null, null, null,
                BadConstructorEntity1.class.getName(), "jpaAppenderTestUnit");

        assertNull("The appender should be null.", appender);
    }

    @Test
    public void testBadConstructorEntity02() {
        final JpaAppender appender = JpaAppender.createAppender("name", null, null, null,
                BadConstructorEntity2.class.getName(), "jpaAppenderTestUnit");

        assertNull("The appender should be null.", appender);
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
