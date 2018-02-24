/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.appender.db.jpa.converter;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.categories.Appenders;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.*;

@Category(Appenders.Jpa.class)
public class MarkerAttributeConverterTest {
    private MarkerAttributeConverter converter;

    @Before
    public void setUp() {
        this.converter = new MarkerAttributeConverter();
    }

    @Test
    public void testConvert01() {
        final Marker marker = MarkerManager.getMarker("testConvert01");

        final String converted = this.converter.convertToDatabaseColumn(marker);

        assertNotNull("The converted value should not be null.", converted);
        assertEquals("The converted value is not correct.", "testConvert01", converted);

        final Marker reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull("The reversed value should not be null.", reversed);
        assertEquals("The reversed value is not correct.", "testConvert01", marker.getName());
    }

    @Test
    public void testConvert02() {
        Marker marker = MarkerManager.getMarker("anotherConvert02").setParents(MarkerManager.getMarker("finalConvert03"));
        marker = MarkerManager.getMarker("testConvert02").setParents(marker);

        final String converted = this.converter.convertToDatabaseColumn(marker);

        assertNotNull("The converted value should not be null.", converted);
        assertEquals("The converted value is not correct.", "testConvert02[ anotherConvert02[ finalConvert03 ] ]",
                converted);

        final Marker reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull("The reversed value should not be null.", reversed);
        assertEquals("The reversed value is not correct.", "testConvert02", marker.getName());
        final Marker[] parents = marker.getParents();
        assertNotNull("The first parent should not be null.", parents);
        assertNotNull("The first parent should not be null.", parents[0]);
        assertEquals("The first parent is not correct.", "anotherConvert02", parents[0].getName());
        assertNotNull("The second parent should not be null.", parents[0].getParents());
        assertNotNull("The second parent should not be null.", parents[0].getParents()[0]);
        assertEquals("The second parent is not correct.", "finalConvert03", parents[0].getParents()[0].getName());
    }

    @Test
    public void testConvertNullToDatabaseColumn() {
        assertNull("The converted value should be null.", this.converter.convertToDatabaseColumn(null));
    }

    @Test
    public void testConvertNullOrBlankToEntityAttribute() {
        assertNull("The converted attribute should be null (1).", this.converter.convertToEntityAttribute(null));
        assertNull("The converted attribute should be null (2).", this.converter.convertToEntityAttribute(""));
    }
}
