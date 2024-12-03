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

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("Appenders.Jpa")
class ContextMapJsonAttributeConverterTest {
    private ContextMapJsonAttributeConverter converter;

    @BeforeEach
    void setUp() {
        this.converter = new ContextMapJsonAttributeConverter();
    }

    @Test
    void testConvert01() {
        final Map<String, String> map = new HashMap<>();
        map.put("test1", "another1");
        map.put("key2", "value2");

        final String converted = this.converter.convertToDatabaseColumn(map);

        assertNotNull(converted, "The converted value should not be null.");

        final Map<String, String> reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull(reversed, "The reversed value should not be null.");
        assertEquals(map, reversed, "The reversed value is not correct.");
    }

    @Test
    void testConvert02() {
        final Map<String, String> map = new HashMap<>();
        map.put("someKey", "coolValue");
        map.put("anotherKey", "testValue");
        map.put("myKey", "yourValue");

        final String converted = this.converter.convertToDatabaseColumn(map);

        assertNotNull(converted, "The converted value should not be null.");

        final Map<String, String> reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull(reversed, "The reversed value should not be null.");
        assertEquals(map, reversed, "The reversed value is not correct.");
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
