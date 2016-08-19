package org.apache.logging.log4j.core.layout;

import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * See (LOG4J2-905) Ability to disable (date) lookup completely, compatibility issues with other libraries like camel.
 * 
 * This shows the behavior this user wants to disable.
 */
public class PatternLayoutLookupDateTest {

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule("log4j-list.xml");

    @Test
    public void testDateLookupInMessage() {
        context.getLogger(PatternLayoutLookupDateTest.class.getName()).info("${date:now:buhu}");
        final ListAppender listAppender = context.getListAppender("List");
        Assert.assertNotEquals("${date:now:buhu}", listAppender.getMessages().get(0));
    }

}
