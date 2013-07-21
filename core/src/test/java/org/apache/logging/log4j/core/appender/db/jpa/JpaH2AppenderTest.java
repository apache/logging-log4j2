package org.apache.logging.log4j.core.appender.db.jpa;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class JpaH2AppenderTest extends AbstractJpaAppenderTest {
    private static final String USER_ID = "sa";
    private static final String PASSWORD = "";

    public JpaH2AppenderTest() {
        super("h2");
    }

    @Override
    protected Connection setUpConnection() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:h2:mem:Log4j", USER_ID, PASSWORD);

        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE jpaBaseLogEntry ( " +
                "id INTEGER IDENTITY, eventDate DATETIME, level NVARCHAR(10), logger NVARCHAR(255), " +
                "message NVARCHAR(1024), exception NVARCHAR(1048576)" +
                " )");
        statement.close();

        statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE jpaBasicLogEntry ( " +
                "id INTEGER IDENTITY, millis BIGINT, level NVARCHAR(10), loggerName NVARCHAR(255), " +
                "message NVARCHAR(1024), thrown NVARCHAR(1048576), contextMapJson NVARCHAR(1048576)," +
                "fqcn NVARCHAR(1024), contextStack NVARCHAR(1048576), marker NVARCHAR(255), source NVARCHAR(2048)," +
                "threadName NVARCHAR(255)" +
                " )");
        statement.close();

        return connection;
    }
}
