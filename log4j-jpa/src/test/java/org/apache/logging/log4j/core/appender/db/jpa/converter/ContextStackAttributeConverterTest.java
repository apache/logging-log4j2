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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.spi.MutableThreadContextStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("Appenders.Jpa")
class ContextStackAttributeConverterTest {
    private ContextStackAttributeConverter converter;

    @BeforeEach
    void setUp() {
        this.converter = new ContextStackAttributeConverter();
    }

    @Test
    void testConvertToDatabaseColumn01() {
        final ThreadContext.ContextStack stack = new MutableThreadContextStack(Arrays.asList("value1", "another2"));

        assertEquals(
                "value1\nanother2",
                this.converter.convertToDatabaseColumn(stack),
                "The converted value is not correct.");
    }

    @Test
    void testConvertToDatabaseColumn02() {
        final ThreadContext.ContextStack stack = new MutableThreadContextStack(Arrays.asList("key1", "value2", "my3"));

        assertEquals(
                "key1\nvalue2\nmy3",
                this.converter.convertToDatabaseColumn(stack),
                "The converted value is not correct.");
    }

    @Test
    void testConvertNullToDatabaseColumn() {
        assertNull(this.converter.convertToDatabaseColumn(null), "The converted value should be null.");
    }

    @Test
    void testConvertToEntityAttribute() {
        assertThrows(UnsupportedOperationException.class, () -> this.converter.convertToEntityAttribute(null));
    }
}
