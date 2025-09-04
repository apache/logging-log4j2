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
package org.apache.logging.log4j.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.util.Strings;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@LoggerContextSource("log4j2-jdbc-driver-manager.xml")
public class LoggerPrintWriterJdbcH2Test {

    private LoggerContext context = null;

    LoggerPrintWriterJdbcH2Test(LoggerContext context) {
        this.context = context;
    }

    private static final String H2_URL = "jdbc:h2:mem:Log4j";

    private static final String PASSWORD = Strings.EMPTY;

    private static final String USER_ID = "sa";

    private ListAppender listAppender;

    private PrintWriter createLoggerPrintWriter() {
        return IoBuilder.forLogger(context.getLogger("LoggerPrintWriterJdbcH2Test"))
                .setLevel(Level.ALL)
                .buildPrintWriter();
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

    @BeforeEach
    void setUp() {
        ListAppender listApp = context.getConfiguration().getAppender("List");
        listApp.clear();
        this.setListAppender(listApp);
        assertEquals(0, this.getListAppender().getMessages().size());
    }

    @Test
    @Disabled("DataSource#setLogWriter() has no effect in H2, it uses its own internal logging and an SLF4J bridge.")
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
        assertTrue(!this.getListAppender().getMessages().isEmpty());
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
        assertTrue(!this.getListAppender().getMessages().isEmpty());
    }
}
