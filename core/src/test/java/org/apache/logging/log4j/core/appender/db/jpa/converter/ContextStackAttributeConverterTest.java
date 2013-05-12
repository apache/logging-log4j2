package org.apache.logging.log4j.core.appender.db.jpa.converter;

import org.apache.logging.log4j.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class ContextStackAttributeConverterTest {
    private ContextStackAttributeConverter converter;

    @Before
    public void setUp() {
        this.converter = new ContextStackAttributeConverter();
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testConvertToDatabaseColumn01() {
        ThreadContext.ContextStack stack = new ThreadContext.ImmutableStack(Arrays.asList("value1", "another2"));

        assertEquals("The converted value is not correct.", "value1\nanother2",
                this.converter.convertToDatabaseColumn(stack));
    }

    @Test
    public void testConvertToDatabaseColumn02() {
        ThreadContext.ContextStack stack = new ThreadContext.ImmutableStack(Arrays.asList("key1", "value2", "my3"));

        assertEquals("The converted value is not correct.", "key1\nvalue2\nmy3",
                this.converter.convertToDatabaseColumn(stack));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testConvertToEntityAttribute() {
        this.converter.convertToEntityAttribute(null);
    }
}
