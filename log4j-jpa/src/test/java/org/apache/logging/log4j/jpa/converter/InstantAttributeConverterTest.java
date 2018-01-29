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
package org.apache.logging.log4j.jpa.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.logging.log4j.categories.Appenders;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(Appenders.Jpa.class)
public class InstantAttributeConverterTest {

    private InstantAttributeConverter converter;

    @Before
    public void setUp() {
        this.converter = new InstantAttributeConverter();
    }

    @Test
    public void testConvert01() {
        final MutableInstant instant = new MutableInstant();
        instant.initFromEpochSecond(1234567, 89012);

        final String converted = this.converter.convertToDatabaseColumn(instant);

        assertNotNull("The converted value should not be null.", converted);
        assertEquals("The converted value is not correct.", "1234567,89012", converted);

        final Instant reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull("The reversed value should not be null.", reversed);
        assertEquals("epoch sec", 1234567, reversed.getEpochSecond());
        assertEquals("nanoOfSecond", 89012, reversed.getNanoOfSecond());
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
