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

import org.apache.logging.log4j.categories.Appenders;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.*;

@Category(Appenders.Jpa.class)
public class StackTraceElementAttributeConverterTest {
    private StackTraceElementAttributeConverter converter;

    @Before
    public void setUp() {
        this.converter = new StackTraceElementAttributeConverter();
    }

    @Test
    public void testConvert01() {
        final StackTraceElement element = new StackTraceElement("TestNoPackage", "testConvert01", "TestNoPackage.java", 1234);

        final String converted = this.converter.convertToDatabaseColumn(element);

        assertNotNull("The converted value should not be null.", converted);
        assertEquals("The converted value is not correct.", "TestNoPackage.testConvert01(TestNoPackage.java:1234)",
                converted);

        final StackTraceElement reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull("The reversed value should not be null.", reversed);
        assertEquals("The class name is not correct.", "TestNoPackage", reversed.getClassName());
        assertEquals("The method name is not correct.", "testConvert01", reversed.getMethodName());
        assertEquals("The file name is not correct.", "TestNoPackage.java", reversed.getFileName());
        assertEquals("The line number is not correct.", 1234, reversed.getLineNumber());
        assertFalse("The native flag should be false.", reversed.isNativeMethod());
    }

    @Test
    public void testConvert02() {
        final StackTraceElement element = new StackTraceElement("org.apache.logging.TestWithPackage",
                "testConvert02", "TestWithPackage.java", -1);

        final String converted = this.converter.convertToDatabaseColumn(element);

        assertNotNull("The converted value should not be null.", converted);
        assertEquals("The converted value is not correct.",
                "org.apache.logging.TestWithPackage.testConvert02(TestWithPackage.java)",
                converted);

        final StackTraceElement reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull("The reversed value should not be null.", reversed);
        assertEquals("The class name is not correct.", "org.apache.logging.TestWithPackage", reversed.getClassName());
        assertEquals("The method name is not correct.", "testConvert02", reversed.getMethodName());
        assertEquals("The file name is not correct.", "TestWithPackage.java", reversed.getFileName());
        assertEquals("The line number is not correct.", -1, reversed.getLineNumber());
        assertFalse("The native flag should be false.", reversed.isNativeMethod());
    }

    @Test
    public void testConvert03() {
        final StackTraceElement element = new StackTraceElement("org.apache.logging.TestNoSource",
                "testConvert03", null, -1);

        final String converted = this.converter.convertToDatabaseColumn(element);

        assertNotNull("The converted value should not be null.", converted);
        assertEquals("The converted value is not correct.",
                "org.apache.logging.TestNoSource.testConvert03(Unknown Source)",
                converted);

        final StackTraceElement reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull("The reversed value should not be null.", reversed);
        assertEquals("The class name is not correct.", "org.apache.logging.TestNoSource", reversed.getClassName());
        assertEquals("The method name is not correct.", "testConvert03", reversed.getMethodName());
        assertEquals("The file name is not correct.", null, reversed.getFileName());
        assertEquals("The line number is not correct.", -1, reversed.getLineNumber());
        assertFalse("The native flag should be false.", reversed.isNativeMethod());
    }

    @Test
    public void testConvert04() {
        final StackTraceElement element = new StackTraceElement("org.apache.logging.TestIsNativeMethod",
                "testConvert04", null, -2);

        final String converted = this.converter.convertToDatabaseColumn(element);

        assertNotNull("The converted value should not be null.", converted);
        assertEquals("The converted value is not correct.",
                "org.apache.logging.TestIsNativeMethod.testConvert04(Native Method)",
                converted);

        final StackTraceElement reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull("The reversed value should not be null.", reversed);
        assertEquals("The class name is not correct.", "org.apache.logging.TestIsNativeMethod",
                reversed.getClassName());
        assertEquals("The method name is not correct.", "testConvert04", reversed.getMethodName());
        assertEquals("The file name is not correct.", null, reversed.getFileName());
        assertEquals("The line number is not correct.", -2, reversed.getLineNumber());
        assertTrue("The native flag should be true.", reversed.isNativeMethod());
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
