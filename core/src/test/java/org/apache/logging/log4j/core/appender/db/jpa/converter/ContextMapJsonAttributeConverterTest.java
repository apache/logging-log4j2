package org.apache.logging.log4j.core.appender.db.jpa.converter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ContextMapJsonAttributeConverterTest {
    private ContextMapJsonAttributeConverter converter;

    @Before
    public void setUp() {
        this.converter = new ContextMapJsonAttributeConverter();
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testConvert01() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("test1", "another1");
        map.put("key2", "value2");

        String converted = this.converter.convertToDatabaseColumn(map);

        assertNotNull("The converted value should not be null.", converted);

        Map<String, String> reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull("The reversed value should not be null.", reversed);
        assertEquals("The reversed value is not correct.", map, reversed);
    }

    @Test
    public void testConvert02() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("someKey", "coolValue");
        map.put("anotherKey", "testValue");
        map.put("myKey", "yourValue");

        String converted = this.converter.convertToDatabaseColumn(map);

        assertNotNull("The converted value should not be null.", converted);

        Map<String, String> reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull("The reversed value should not be null.", reversed);
        assertEquals("The reversed value is not correct.", map, reversed);
    }
}
