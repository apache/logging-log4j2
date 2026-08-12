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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("Appenders.Jpa")
class StackTraceElementAttributeConverterTest {
    private StackTraceElementAttributeConverter converter;

    @BeforeEach
    void setUp() {
        this.converter = new StackTraceElementAttributeConverter();
    }

    @Test
    void testConvert01() {
        final StackTraceElement element =
                new StackTraceElement("TestNoPackage", "testConvert01", "TestNoPackage.java", 1234);

        final String converted = this.converter.convertToDatabaseColumn(element);

        assertNotNull(converted, "The converted value should not be null.");
        assertEquals(
                "TestNoPackage.testConvert01(TestNoPackage.java:1234)",
                converted,
                "The converted value is not correct.");

        final StackTraceElement reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull(reversed, "The reversed value should not be null.");
        assertEquals("TestNoPackage", reversed.getClassName(), "The class name is not correct.");
        assertEquals("testConvert01", reversed.getMethodName(), "The method name is not correct.");
        assertEquals("TestNoPackage.java", reversed.getFileName(), "The file name is not correct.");
        assertEquals(1234, reversed.getLineNumber(), "The line number is not correct.");
        assertFalse(reversed.isNativeMethod(), "The native flag should be false.");
    }

    @Test
    void testConvert02() {
        final StackTraceElement element = new StackTraceElement(
                "org.apache.logging.TestWithPackage", "testConvert02", "TestWithPackage.java", -1);

        final String converted = this.converter.convertToDatabaseColumn(element);

        assertNotNull(converted, "The converted value should not be null.");
        assertEquals(
                "org.apache.logging.TestWithPackage.testConvert02(TestWithPackage.java)",
                converted,
                "The converted value is not correct.");

        final StackTraceElement reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull(reversed, "The reversed value should not be null.");
        assertEquals("org.apache.logging.TestWithPackage", reversed.getClassName(), "The class name is not correct.");
        assertEquals("testConvert02", reversed.getMethodName(), "The method name is not correct.");
        assertEquals("TestWithPackage.java", reversed.getFileName(), "The file name is not correct.");
        assertEquals(-1, reversed.getLineNumber(), "The line number is not correct.");
        assertFalse(reversed.isNativeMethod(), "The native flag should be false.");
    }

    @Test
    void testConvert03() {
        final StackTraceElement element =
                new StackTraceElement("org.apache.logging.TestNoSource", "testConvert03", null, -1);

        final String converted = this.converter.convertToDatabaseColumn(element);

        assertNotNull(converted, "The converted value should not be null.");
        assertEquals(
                "org.apache.logging.TestNoSource.testConvert03(Unknown Source)",
                converted,
                "The converted value is not correct.");

        final StackTraceElement reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull(reversed, "The reversed value should not be null.");
        assertEquals("org.apache.logging.TestNoSource", reversed.getClassName(), "The class name is not correct.");
        assertEquals("testConvert03", reversed.getMethodName(), "The method name is not correct.");
        assertNull(reversed.getFileName(), "The file name is not correct.");
        assertEquals(-1, reversed.getLineNumber(), "The line number is not correct.");
        assertFalse(reversed.isNativeMethod(), "The native flag should be false.");
    }

    @Test
    void testConvert04() {
        final StackTraceElement element =
                new StackTraceElement("org.apache.logging.TestIsNativeMethod", "testConvert04", null, -2);

        final String converted = this.converter.convertToDatabaseColumn(element);

        assertNotNull(converted, "The converted value should not be null.");
        assertEquals(
                "org.apache.logging.TestIsNativeMethod.testConvert04(Native Method)",
                converted,
                "The converted value is not correct.");

        final StackTraceElement reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull(reversed, "The reversed value should not be null.");
        assertEquals(
                "org.apache.logging.TestIsNativeMethod", reversed.getClassName(), "The class name is not correct.");
        assertEquals("testConvert04", reversed.getMethodName(), "The method name is not correct.");
        assertNull(reversed.getFileName(), "The file name is not correct.");
        assertEquals(-2, reversed.getLineNumber(), "The line number is not correct.");
        assertTrue(reversed.isNativeMethod(), "The native flag should be true.");
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
