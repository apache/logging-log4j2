package org.apache.logging.log4j.io;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.junit.InitialLoggerContext;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.apache.logging.log4j.util.Strings;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class LoggerPrintWriterJdbcH2Test {
    @ClassRule
    public static InitialLoggerContext context = new InitialLoggerContext("log4j2-jdbc-driver-manager.xml");

    private static final String PASSWORD = Strings.EMPTY;

    private static final String USER_ID = "sa";

    private ListAppender listAppender;

    private ListAppender getListAppender() {
        return listAppender;
    }

    protected Connection newConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:Log4j", USER_ID, PASSWORD);
    }

    private void setListAppender(ListAppender listAppender) {
        this.listAppender = listAppender;
    }

    public void setLogWriter(PrintWriter printWriter) {
        DriverManager.setLogWriter(printWriter);
    }

    @Before
    public void setUp() throws Exception {
        this.setListAppender(context.getListAppender("List").clear());
    }

    @Test
    public void testDriverManager_setLogWriter() throws SQLException {
        Assert.assertEquals(0, this.getListAppender().getMessages().size());
        this.setLogWriter(new LoggerPrintWriter((ExtendedLogger) LogManager.getLogger(), Level.ALL));
        try {
            this.newConnection().close();
        } finally {
            this.setLogWriter(null);
        }
        Assert.assertTrue(this.getListAppender().getMessages().size() > 0);
    }
}
