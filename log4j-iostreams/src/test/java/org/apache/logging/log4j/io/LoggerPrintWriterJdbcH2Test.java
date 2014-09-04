package org.apache.logging.log4j.io;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.junit.InitialLoggerContext;
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
    public static InitialLoggerContext context = new InitialLoggerContext("log4j2-jdbc-driver-manager.xml");

    private static final String H2_URL = "jdbc:h2:mem:Log4j";

    private static final String PASSWORD = Strings.EMPTY;

    private static final String USER_ID = "sa";

    private ListAppender listAppender;

    private PrintWriter createLoggerPrintWriter() {
        return LoggerStreams.forLogger(context.getLogger()).setLevel(Level.ALL).buildPrintWriter();
    }

    private ListAppender getListAppender() {
        return listAppender;
    }

    protected Connection newConnection() throws SQLException {
        return DriverManager.getConnection(H2_URL, USER_ID, PASSWORD);
    }

    private void setListAppender(ListAppender listAppender) {
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
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setUrl(H2_URL);
        dataSource.setUser(USER_ID);
        dataSource.setPassword(PASSWORD);
        dataSource.setLogWriter(createLoggerPrintWriter());
        // dataSource.setLogWriter(new PrintWriter(new OutputStreamWriter(System.out)));
        Connection conn = dataSource.getConnection();
        try {
            conn.prepareCall("select 1");
        } finally {
            conn.close();
        }
        Assert.assertTrue(this.getListAppender().getMessages().size() > 0);
    }

    @Test
    public void testDriverManager_setLogWriter() throws SQLException {
        DriverManager.setLogWriter(createLoggerPrintWriter());
        // DriverManager.setLogWriter(new PrintWriter(new OutputStreamWriter(System.out)));
        try {
            Connection conn = this.newConnection();
            try {
                conn.rollback();
            } finally {
                conn.close();
            }
        } finally {
            DriverManager.setLogWriter(null);
        }
        Assert.assertTrue(this.getListAppender().getMessages().size() > 0);
    }
}
