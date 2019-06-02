package org.apache.logging.log4j.groovy.declarative;

import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class DeclarativeConfigurationTest {

    @ClassRule
    public static LoggerContextRule rule = new LoggerContextRule();

    @Test
    public void smokeTests() {
        ListAppender appender = ListAppender.getListAppender("Messages");
        rule.getLogger().info("Hello, world!");
        List<String> messages = appender.getMessages();
        assertEquals(1, messages.size());
        assertEquals("Hello, world!\n", messages.get(0));
    }
}