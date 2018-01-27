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

import java.sql.SQLException;

import org.apache.logging.log4j.categories.Appenders;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.*;

@Category(Appenders.Jpa.class)
public class ThrowableAttributeConverterTest {
    private ThrowableAttributeConverter converter;

    @Before
    public void setUp() {
        this.converter = new ThrowableAttributeConverter();
    }

    @Test
    public void testConvert01() {
        final RuntimeException exception = new RuntimeException("My message 01.");

        final String stackTrace = getStackTrace(exception);

        final String converted = this.converter.convertToDatabaseColumn(exception);

        assertNotNull("The converted value is not correct.", converted);
        assertEquals("The converted value is not correct.", stackTrace, converted);

        final Throwable reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull("The reversed value should not be null.", reversed);
        assertEquals("The reversed value is not correct.", stackTrace, getStackTrace(reversed));
    }

    @Test
    public void testConvert02() {
        final SQLException cause2 = new SQLException("This is a test cause.");
        final Error cause1 = new Error(cause2);
        final RuntimeException exception = new RuntimeException("My message 01.", cause1);

        final String stackTrace = getStackTrace(exception);

        final String converted = this.converter.convertToDatabaseColumn(exception);

        assertNotNull("The converted value is not correct.", converted);
        assertEquals("The converted value is not correct.", stackTrace, converted);

        final Throwable reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull("The reversed value should not be null.", reversed);
        assertEquals("The reversed value is not correct.", stackTrace, getStackTrace(reversed));
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

    private static String getStackTrace(final Throwable throwable) {
        String returnValue = throwable.toString() + '\n';

        for (final StackTraceElement element : throwable.getStackTrace()) {
            returnValue += "\tat " + element.toString() + '\n';
        }

        if (throwable.getCause() != null) {
            returnValue += "Caused by " + getStackTrace(throwable.getCause());
        }

        return returnValue;
    }
}
