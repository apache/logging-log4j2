package org.apache.logging.log4j.core.appender.db.jpa.converter;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class MarkerAttributeConverterTest {
    private MarkerAttributeConverter converter;

    @Before
    public void setUp() {
        this.converter = new MarkerAttributeConverter();
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testConvert01() {
        Marker marker = MarkerManager.getMarker("testConvert01");

        String converted = this.converter.convertToDatabaseColumn(marker);

        assertNotNull("The converted value should not be null.", converted);
        assertEquals("The converted value is not correct.", "testConvert01", converted);

        Marker reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull("The reversed value should not be null.", reversed);
        assertEquals("The reversed value is not correct.", "testConvert01", marker.getName());
    }

    @Test
    public void testConvert02() {
        Marker marker = MarkerManager.getMarker("testConvert02",
                MarkerManager.getMarker("anotherConvert02",
                        MarkerManager.getMarker("finalConvert03")
                )
        );

        String converted = this.converter.convertToDatabaseColumn(marker);

        assertNotNull("The converted value should not be null.", converted);
        assertEquals("The converted value is not correct.", "testConvert02[ anotherConvert02[ finalConvert03 ] ] ]",
                converted);

        Marker reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull("The reversed value should not be null.", reversed);
        assertEquals("The reversed value is not correct.", "testConvert02", marker.getName());
        assertNotNull("The first parent should not be null.", marker.getParent());
        assertEquals("The first parent is not correct.", "anotherConvert02", marker.getParent().getName());
        assertNotNull("The second parent should not be null.", marker.getParent().getParent());
        assertEquals("The second parent is not correct.", "finalConvert03", marker.getParent().getParent().getName());
    }
}
