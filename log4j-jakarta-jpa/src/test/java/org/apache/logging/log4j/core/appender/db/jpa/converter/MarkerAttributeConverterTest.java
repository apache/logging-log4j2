/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.appender.db.jpa.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("Appenders.Jpa")
class MarkerAttributeConverterTest {
    private MarkerAttributeConverter converter;

    @BeforeEach
    void setUp() {
        this.converter = new MarkerAttributeConverter();
    }

    @Test
    void testConvert01() {
        final Marker marker = MarkerManager.getMarker("testConvert01");

        final String converted = this.converter.convertToDatabaseColumn(marker);

        assertNotNull(converted, "The converted value should not be null.");
        assertEquals("testConvert01", converted, "The converted value is not correct.");

        final Marker reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull(reversed, "The reversed value should not be null.");
        assertEquals("testConvert01", marker.getName(), "The reversed value is not correct.");
    }

    @Test
    void testConvert02() {
        Marker marker =
                MarkerManager.getMarker("anotherConvert02").setParents(MarkerManager.getMarker("finalConvert03"));
        marker = MarkerManager.getMarker("testConvert02").setParents(marker);

        final String converted = this.converter.convertToDatabaseColumn(marker);

        assertNotNull(converted, "The converted value should not be null.");
        assertEquals(
                "testConvert02[ anotherConvert02[ finalConvert03 ] ]",
                converted,
                "The converted value is not correct.");

        final Marker reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull(reversed, "The reversed value should not be null.");
        assertEquals("testConvert02", marker.getName(), "The reversed value is not correct.");
        final Marker[] parents = marker.getParents();
        assertNotNull(parents, "The first parent should not be null.");
        assertNotNull(parents[0], "The first parent should not be null.");
        assertEquals("anotherConvert02", parents[0].getName(), "The first parent is not correct.");
        assertNotNull(parents[0].getParents(), "The second parent should not be null.");
        assertNotNull(parents[0].getParents()[0], "The second parent should not be null.");
        assertEquals("finalConvert03", parents[0].getParents()[0].getName(), "The second parent is not correct.");
    }

    @Test
    void testConvertNullToDatabaseColumn() {
        assertNull(this.converter.convertToDatabaseColumn(null), "The converted value should be null.");
    }

    @Test
    void testConvertNullOrBlankToEntityAttribute() {
        assertNull(this.converter.convertToEntityAttribute(null), "The converted attribute should be null (1).");
        assertNull(this.converter.convertToEntityAttribute(""), "The converted attribute should be null (2).");
    }
}
