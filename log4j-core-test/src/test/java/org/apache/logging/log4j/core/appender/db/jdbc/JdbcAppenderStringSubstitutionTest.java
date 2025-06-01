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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

public class JdbcAppenderStringSubstitutionTest {

    private static final String VALUE = "MyTableName";
    private static final String KEY = "Test.TableName";

    public JdbcAppenderStringSubstitutionTest() {
        System.setProperty(KEY, VALUE);
    }

    @AfterAll
    public static void afterClass() {
        System.getProperties().remove(KEY);
    }

    @Test
    @LoggerContextSource("org/apache/logging/log4j/core/appender/db/jdbc/log4j2-jdbc-string-substitution.xml")
    public void test(@Named("databaseAppender") JdbcAppender appender) {
        assertNotNull(appender);
        final JdbcDatabaseManager manager = appender.getManager();
        assertNotNull(manager);
        final String sqlStatement = manager.getSqlStatement();
        assertFalse(sqlStatement.contains(KEY), sqlStatement);
        assertTrue(sqlStatement.contains(VALUE), sqlStatement);
    }
}
