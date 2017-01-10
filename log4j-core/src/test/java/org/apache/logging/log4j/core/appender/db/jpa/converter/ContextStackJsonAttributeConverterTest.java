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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.categories.Appenders;
import org.apache.logging.log4j.junit.ThreadContextStackRule;
import org.apache.logging.log4j.spi.MutableThreadContextStack;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(Appenders.Jpa.class)
public class ContextStackJsonAttributeConverterTest {
    private ContextStackJsonAttributeConverter converter;

    @Rule
    public final ThreadContextStackRule threadContextRule = new ThreadContextStackRule(); 

    @Before
    public void setUp() {
        this.converter = new ContextStackJsonAttributeConverter();
    }

    @Test
    public void testConvert01() {
        final ThreadContext.ContextStack stack = new MutableThreadContextStack(
                Arrays.asList("value1", "another2"));

        final String converted = this.converter.convertToDatabaseColumn(stack);

        assertNotNull("The converted value should not be null.", converted);

        final ThreadContext.ContextStack reversed = this.converter
                .convertToEntityAttribute(converted);

        assertNotNull("The reversed value should not be null.", reversed);
        assertEquals("The reversed value is not correct.", stack.asList(),
                reversed.asList());
    }

    @Test
    public void testConvert02() {
        final ThreadContext.ContextStack stack = new MutableThreadContextStack(
                Arrays.asList("key1", "value2", "my3"));

        final String converted = this.converter.convertToDatabaseColumn(stack);

        assertNotNull("The converted value should not be null.", converted);

        final ThreadContext.ContextStack reversed = this.converter
                .convertToEntityAttribute(converted);

        assertNotNull("The reversed value should not be null.", reversed);
        assertEquals("The reversed value is not correct.", stack.asList(),
                reversed.asList());
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
