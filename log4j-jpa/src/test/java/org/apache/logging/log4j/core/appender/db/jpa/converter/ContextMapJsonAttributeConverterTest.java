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

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.categories.Appenders;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.*;

@Category(Appenders.Jpa.class)
public class ContextMapJsonAttributeConverterTest {
    private ContextMapJsonAttributeConverter converter;

    @Before
    public void setUp() {
        this.converter = new ContextMapJsonAttributeConverter();
    }

    @Test
    public void testConvert01() {
        final Map<String, String> map = new HashMap<>();
        map.put("test1", "another1");
        map.put("key2", "value2");

        final String converted = this.converter.convertToDatabaseColumn(map);

        assertNotNull("The converted value should not be null.", converted);

        final Map<String, String> reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull("The reversed value should not be null.", reversed);
        assertEquals("The reversed value is not correct.", map, reversed);
    }

    @Test
    public void testConvert02() {
        final Map<String, String> map = new HashMap<>();
        map.put("someKey", "coolValue");
        map.put("anotherKey", "testValue");
        map.put("myKey", "yourValue");

        final String converted = this.converter.convertToDatabaseColumn(map);

        assertNotNull("The converted value should not be null.", converted);

        final Map<String, String> reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull("The reversed value should not be null.", reversed);
        assertEquals("The reversed value is not correct.", map, reversed);
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
