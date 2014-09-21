package org.apache.logging.log4j.io;

import java.io.PrintStream;
import java.util.List;

import org.apache.logging.log4j.junit.InitialLoggerContext;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.*;

public class IoBuilderTest {

    @Rule
    public InitialLoggerContext context = new InitialLoggerContext("log4j2-streams-calling-info.xml");

    @Test
    public void testNoArgBuilderCallerClassInfo() throws Exception {
        final PrintStream ps = IoBuilder.forLogger().buildPrintStream();
        ps.println("discarded");
        final ListAppender app = context.getListAppender("IoBuilderTest");
        final List<String> messages = app.getMessages();
        assertThat(messages, not(empty()));
        assertThat(messages, hasSize(1));
        final String message = messages.get(0);
        assertThat(message, startsWith(getClass().getName() + ".testNoArgBuilderCallerClassInfo"));
        app.clear();
    }
}
