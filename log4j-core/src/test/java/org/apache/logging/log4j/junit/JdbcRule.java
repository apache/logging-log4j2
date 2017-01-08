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
package org.apache.logging.log4j.junit;

import java.sql.Connection;
import java.sql.Statement;

import org.apache.logging.log4j.core.appender.db.jdbc.ConnectionSource;
import org.junit.rules.TestRule;
import org.junit.runner.Description;

/**
 * JUnit rule to set up a database. This will create a table using the configure creation statement on startup, run its
 * wrapped test(s), then execute a drop table statement on shutdown, and finally will attempt execute a {@code SHUTDOWN}
 * command afterward (e.g., for H2, HSQLDB). When used in integration tests, this rule should be the outer rule in a
 * chain with {@link LoggerContextRule}.
 *
 * @since 2.8
 */
public class JdbcRule implements TestRule {

    private final ConnectionSource connectionSource;
    private final String createTableStatement;
    private final String dropTableStatement;

    /**
     * Creates a JdbcRule using a {@link ConnectionSource} and a table creation statement.
     *
     * @param connectionSource     a source for obtaining a Connection
     * @param createTableStatement a SQL DDL statement to set up a table for use in a JUnit test
     * @param dropTableStatement   a SQL DDL statement to drop the created table
     */
    public JdbcRule(final ConnectionSource connectionSource, final String createTableStatement,
                    final String dropTableStatement) {
        this.dropTableStatement = dropTableStatement;
        this.connectionSource = connectionSource;
        this.createTableStatement = createTableStatement;
    }

    public ConnectionSource getConnectionSource() {
        return connectionSource;
    }

    @Override
    public org.junit.runners.model.Statement apply(final org.junit.runners.model.Statement base,
                                                   final Description description) {
        return new org.junit.runners.model.Statement() {
            @Override
            public void evaluate() throws Throwable {
                try (final Connection connection = connectionSource.getConnection()) {
                    try (final Statement statement = connection.createStatement()) {
                        statement.executeUpdate(createTableStatement);
                    }
                    base.evaluate();
                    try (final Statement statement = connection.createStatement()) {
                        statement.executeUpdate(dropTableStatement);
                        statement.execute("SHUTDOWN");
                    }
                }
            }
        };
    }
}
