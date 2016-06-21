package org.apache.logging.log4j.core.pattern;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.ClassRule;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 *
 */
public class SequenceNumberPatternConverterTest {

    @ClassRule
    public static LoggerContextRule ctx = new LoggerContextRule("SequenceNumberPatternConverterTest.yml");

    @Test
    public void testSequenceIncreases() throws Exception {
        final Logger logger = ctx.getLogger();
        logger.info("Message 1");
        logger.info("Message 2");
        logger.info("Message 3");
        logger.info("Message 4");
        logger.info("Message 5");

        final ListAppender app = ctx.getListAppender("List");
        final List<String> messages = app.getMessages();
        assertThat(messages, contains("1", "2", "3", "4", "5"));
    }
}
