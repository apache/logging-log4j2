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

import java.util.Arrays;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.categories.Appenders;
import org.apache.logging.log4j.spi.MutableThreadContextStack;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.*;

@Category(Appenders.Jpa.class)
public class ContextStackAttributeConverterTest {
    private ContextStackAttributeConverter converter;

    @Before
    public void setUp() {
        this.converter = new ContextStackAttributeConverter();
    }

    @Test
    public void testConvertToDatabaseColumn01() {
        final ThreadContext.ContextStack stack = new MutableThreadContextStack(
                Arrays.asList("value1", "another2"));

        assertEquals("The converted value is not correct.", "value1\nanother2",
                this.converter.convertToDatabaseColumn(stack));
    }

    @Test
    public void testConvertToDatabaseColumn02() {
        final ThreadContext.ContextStack stack = new MutableThreadContextStack(
                Arrays.asList("key1", "value2", "my3"));

        assertEquals("The converted value is not correct.",
                "key1\nvalue2\nmy3",
                this.converter.convertToDatabaseColumn(stack));
    }

    @Test
    public void testConvertNullToDatabaseColumn() {
        assertNull("The converted value should be null.", this.converter.convertToDatabaseColumn(null));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testConvertToEntityAttribute() {
        this.converter.convertToEntityAttribute(null);
    }
}
