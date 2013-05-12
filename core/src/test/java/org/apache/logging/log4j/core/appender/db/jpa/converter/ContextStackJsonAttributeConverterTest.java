package org.apache.logging.log4j.core.appender.db.jpa.converter;

import org.apache.logging.log4j.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class ContextStackJsonAttributeConverterTest {
    private ContextStackJsonAttributeConverter converter;

    @Before
    public void setUp() {
        this.converter = new ContextStackJsonAttributeConverter();
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testConvert01() {
        ThreadContext.ContextStack stack = new ThreadContext.ImmutableStack(Arrays.asList("value1", "another2"));

        String converted = this.converter.convertToDatabaseColumn(stack);

        assertNotNull("The converted value should not be null.", converted);

        ThreadContext.ContextStack reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull("The reversed value should not be null.", reversed);
        assertEquals("The reversed value is not correct.", stack.asList(), reversed.asList());
    }

    @Test
    public void testConvert02() {
        ThreadContext.ContextStack stack = new ThreadContext.ImmutableStack(Arrays.asList("key1", "value2", "my3"));

        String converted = this.converter.convertToDatabaseColumn(stack);

        assertNotNull("The converted value should not be null.", converted);

        ThreadContext.ContextStack reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull("The reversed value should not be null.", reversed);
        assertEquals("The reversed value is not correct.", stack.asList(), reversed.asList());
    }
}
