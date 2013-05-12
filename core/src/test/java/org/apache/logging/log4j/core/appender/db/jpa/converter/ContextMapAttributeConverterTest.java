package org.apache.logging.log4j.core.appender.db.jpa.converter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ContextMapAttributeConverterTest {
    private ContextMapAttributeConverter converter;

    @Before
    public void setUp() {
        this.converter = new ContextMapAttributeConverter();
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testConvertToDatabaseColumn01() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("test1", "another1");
        map.put("key2", "value2");

        assertEquals("The converted value is not correct.", map.toString(),
                this.converter.convertToDatabaseColumn(map));
    }

    @Test
    public void testConvertToDatabaseColumn02() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("someKey", "coolValue");
        map.put("anotherKey", "testValue");
        map.put("myKey", "yourValue");

        assertEquals("The converted value is not correct.", map.toString(),
                this.converter.convertToDatabaseColumn(map));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testConvertToEntityAttribute() {
        this.converter.convertToEntityAttribute(null);
    }
}
