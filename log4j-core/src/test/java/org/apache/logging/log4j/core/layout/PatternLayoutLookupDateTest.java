package org.apache.logging.log4j.core.layout;

import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * See (LOG4J2-905) Ability to disable (date) lookup completely, compatibility issues with other libraries like camel.
 * 
 * This shows the behavior this user wants to disable.
 */
public class PatternLayoutLookupDateTest {

    @Rule
    public final LoggerContextRule context = new LoggerContextRule("log4j-list.xml");

    @Test
    public void testDateLookupInMessage() {
        final String template = "${date:YYYY-MM-dd}";
        context.getLogger(PatternLayoutLookupDateTest.class.getName()).info(template);
        final ListAppender listAppender = context.getListAppender("List");
        final String string = listAppender.getMessages().get(0);
        Assert.assertFalse(string, string.contains(template));
    }

}
