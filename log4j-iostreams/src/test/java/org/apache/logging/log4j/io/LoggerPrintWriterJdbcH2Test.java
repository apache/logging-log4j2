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
package org.apache.logging.log4j.io;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.apache.logging.log4j.util.Strings;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

public class LoggerPrintWriterJdbcH2Test {
    
    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule("log4j2-jdbc-driver-manager.xml");

    private static final String H2_URL = "jdbc:h2:mem:Log4j";

    private static final String PASSWORD = Strings.EMPTY;

    private static final String USER_ID = "sa";

    private ListAppender listAppender;

    private PrintWriter createLoggerPrintWriter() {
        return IoBuilder.forLogger(context.getLogger()).setLevel(Level.ALL).buildPrintWriter();
    }

    private ListAppender getListAppender() {
        return listAppender;
    }

    protected Connection newConnection() throws SQLException {
        return DriverManager.getConnection(H2_URL, USER_ID, PASSWORD);
    }

    private void setListAppender(final ListAppender listAppender) {
        this.listAppender = listAppender;
    }

    @Before
    public void setUp() throws Exception {
        this.setListAppender(context.getListAppender("List").clear());
        Assert.assertEquals(0, this.getListAppender().getMessages().size());
    }

    @Test
    @Ignore("DataSource#setLogWriter() has no effect in H2, it uses its own internal logging and an SLF4J bridge.")
    public void testDataSource_setLogWriter() throws SQLException {
        final JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setUrl(H2_URL);
        dataSource.setUser(USER_ID);
        dataSource.setPassword(PASSWORD);
        dataSource.setLogWriter(createLoggerPrintWriter());
        // dataSource.setLogWriter(new PrintWriter(new OutputStreamWriter(System.out)));
        try (final Connection conn = dataSource.getConnection()) {
            conn.prepareCall("select 1");
        }
        Assert.assertTrue(this.getListAppender().getMessages().size() > 0);
    }

    @Test
    public void testDriverManager_setLogWriter() throws SQLException {
        DriverManager.setLogWriter(createLoggerPrintWriter());
        // DriverManager.setLogWriter(new PrintWriter(new OutputStreamWriter(System.out)));
        try (final Connection conn = this.newConnection()) {
            conn.rollback();
        } finally {
            DriverManager.setLogWriter(null);
        }
        Assert.assertTrue(this.getListAppender().getMessages().size() > 0);
    }
}
