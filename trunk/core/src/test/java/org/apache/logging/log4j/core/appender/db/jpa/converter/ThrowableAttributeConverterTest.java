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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ThrowableAttributeConverterTest {
    private ThrowableAttributeConverter converter;

    @Before
    public void setUp() {
        this.converter = new ThrowableAttributeConverter();
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testConvert01() {
        RuntimeException exception = new RuntimeException("My message 01.");

        String stackTrace = getStackTrace(exception);

        String converted = this.converter.convertToDatabaseColumn(exception);

        assertNotNull("The converted value is not correct.", converted);
        assertEquals("The converted value is not correct.", stackTrace, converted);

        Throwable reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull("The reversed value should not be null.", reversed);
        assertEquals("The reversed value is not correct.", stackTrace, getStackTrace(reversed));
    }

    @Test
    public void testConvert02() {
        SQLException cause2 = new SQLException("This is a test cause.");
        Error cause1 = new Error(cause2);
        RuntimeException exception = new RuntimeException("My message 01.", cause1);

        String stackTrace = getStackTrace(exception);

        String converted = this.converter.convertToDatabaseColumn(exception);

        assertNotNull("The converted value is not correct.", converted);
        assertEquals("The converted value is not correct.", stackTrace, converted);

        Throwable reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull("The reversed value should not be null.", reversed);
        assertEquals("The reversed value is not correct.", stackTrace, getStackTrace(reversed));
    }

    private static String getStackTrace(Throwable throwable) {
        String returnValue = throwable.toString() + "\n";

        for (StackTraceElement element : throwable.getStackTrace()) {
            returnValue += "\tat " + element.toString() + "\n";
        }

        if (throwable.getCause() != null) {
            returnValue += "Caused by " + getStackTrace(throwable.getCause());
        }

        return returnValue;
    }
}
