package org.apache.logging.log4j.core.appender.db.jpa.converter;

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class MessageAttributeConverterTest {
    private static final StatusLogger log = StatusLogger.getLogger();

    private MessageAttributeConverter converter;

    @Before
    public void setUp() {
        this.converter = new MessageAttributeConverter();
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testConvert01() {
        Message message = log.getMessageFactory().newMessage("Message #{} said [{}].", 3, "Hello");

        String converted = this.converter.convertToDatabaseColumn(message);

        assertNotNull("The converted value should not be null.", converted);
        assertEquals("The converted value is not correct.", "Message #3 said [Hello].", converted);

        Message reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull("The reversed value should not be null.", reversed);
        assertEquals("The reversed value is not correct.", "Message #3 said [Hello].", reversed.getFormattedMessage());
    }
}
