package org.apache.logging.log4j.core.appender;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Rule;
import org.junit.Test;

// TODO this test requires manual verification
public class HttpAppenderTest {

    private static final String LOG_MESSAGE = "Hello, world!";

    private static Log4jLogEvent createLogEvent() {
        return Log4jLogEvent.newBuilder()
            .setLoggerName(HttpAppenderTest.class.getName())
            .setLoggerFqcn(HttpAppenderTest.class.getName())
            .setLevel(Level.INFO)
            .setMessage(new SimpleMessage(LOG_MESSAGE))
            .build();
    }

    @Rule
    public LoggerContextRule ctx = new LoggerContextRule("HttpAppenderTest.xml");

    @Test
    public void testAppendSuccess() throws Exception {
        final Appender appender = ctx.getRequiredAppender("HttpSuccess");
        appender.append(createLogEvent());
    }

    @Test
    public void testAppendErrorIgnore() throws Exception {
        final Appender appender = ctx.getRequiredAppender("HttpErrorIgnore");
        appender.append(createLogEvent());
    }

    @Test(expected = AppenderLoggingException.class)
    public void testAppendError() throws Exception {
        final Appender appender = ctx.getRequiredAppender("HttpError");
        appender.append(createLogEvent());
    }

    @Test
    public void testAppendSubst() throws Exception {
        final Appender appender = ctx.getRequiredAppender("HttpSubst");
        appender.append(createLogEvent());
    }

}