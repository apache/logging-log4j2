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

import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class JdbcAppenderStringSubstitutionTest {

    private static final String VALUE = "MyTableName";
    private static final String KEY = "Test.TableName";

    public JdbcAppenderStringSubstitutionTest() {
        System.setProperty(KEY, VALUE);
    }

    @AfterClass
    public static void afterClass() {
        System.getProperties().remove(KEY);
    }

    @Rule
	public final LoggerContextRule rule = new LoggerContextRule("org/apache/logging/log4j/core/appender/db/jdbc/log4j2-jdbc-string-substitution.xml");

    @Test
    public void test() throws Exception {
        final JdbcAppender appender = rule.getAppender("databaseAppender", JdbcAppender.class);
        Assert.assertNotNull(appender);
        final JdbcDatabaseManager manager = appender.getManager();
        Assert.assertNotNull(manager);
        final String sqlStatement = manager.getSqlStatement();
        Assert.assertFalse(sqlStatement, sqlStatement.contains(KEY));
        Assert.assertTrue(sqlStatement, sqlStatement.contains(VALUE));
    }

}
