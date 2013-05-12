package org.apache.logging.log4j.core.appender.db.jpa.converter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.*;

public class ThrowableAttributeConverterTest {
    private ThrowableAttributeConverter converter;

    @Before
    public void setUp() {
        this.converter = new ThrowableAttributeConverter();
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testConvert01() {
        RuntimeException exception = new RuntimeException("My message 01.");

        String stackTrace = getStackTrace(exception);

        String converted = this.converter.convertToDatabaseColumn(exception);

        assertNotNull("The converted value is not correct.", converted);
        assertEquals("The converted value is not correct.", stackTrace, converted);

        Throwable reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull("The reversed value should not be null.", reversed);
        assertEquals("The reversed value is not correct.", stackTrace, getStackTrace(reversed));
    }

    @Test
    public void testConvert02() {
        SQLException cause2 = new SQLException("This is a test cause.");
        Error cause1 = new Error(cause2);
        RuntimeException exception = new RuntimeException("My message 01.", cause1);

        String stackTrace = getStackTrace(exception);

        String converted = this.converter.convertToDatabaseColumn(exception);

        assertNotNull("The converted value is not correct.", converted);
        assertEquals("The converted value is not correct.", stackTrace, converted);

        Throwable reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull("The reversed value should not be null.", reversed);
        assertEquals("The reversed value is not correct.", stackTrace, getStackTrace(reversed));
    }

    private static String getStackTrace(Throwable throwable) {
        String returnValue = throwable.toString() + "\n";

        for (StackTraceElement element : throwable.getStackTrace()) {
            returnValue += "\tat " + element.toString() + "\n";
        }

        if (throwable.getCause() != null) {
            returnValue += "Caused by " + getStackTrace(throwable.getCause());
        }

        return returnValue;
    }
}
