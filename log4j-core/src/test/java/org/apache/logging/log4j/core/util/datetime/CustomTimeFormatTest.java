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

package org.apache.logging.log4j.core.util.datetime;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.util.datetime.CustomTimeFormat.FixedFormat;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the CustomTimeFormat class.
 */
public class CustomTimeFormatTest {

    @Test
    public void testFixedFormat_getDatePatternNullIfNoDateInPattern() {
        assertNull(FixedFormat.ABSOLUTE.getDatePattern());
        assertNull(FixedFormat.ABSOLUTE2.getDatePattern());
    }

    @Test
    public void testFixedFormat_getDatePatternLengthZeroIfNoDateInPattern() {
        assertEquals(0, FixedFormat.ABSOLUTE.getDatePatternLength());
        assertEquals(0, FixedFormat.ABSOLUTE2.getDatePatternLength());
    }

    @Test
    public void testFixedFormat_getFastDateFormatNullIfNoDateInPattern() {
        assertNull(FixedFormat.ABSOLUTE.getFastDateFormat());
        assertNull(FixedFormat.ABSOLUTE2.getFastDateFormat());
    }

    @Test
    public void testFixedFormat_getDatePatternReturnsDatePatternIfExists() {
        assertEquals("yyyyMMdd", FixedFormat.COMPACT.getDatePattern());
        assertEquals("yyyy-MM-dd ", FixedFormat.DEFAULT.getDatePattern());
    }

    @Test
    public void testFixedFormat_getDatePatternLengthReturnsDatePatternLength() {
        assertEquals("yyyyMMdd".length(), FixedFormat.COMPACT.getDatePatternLength());
        assertEquals("yyyy-MM-dd ".length(), FixedFormat.DEFAULT.getDatePatternLength());
    }

    @Test
    public void testFixedFormat_getFastDateFormatNonNullIfDateInPattern() {
        assertNotNull(FixedFormat.COMPACT.getFastDateFormat());
        assertNotNull(FixedFormat.DEFAULT.getFastDateFormat());
        assertEquals("yyyyMMdd", FixedFormat.COMPACT.getFastDateFormat().getPattern());
        assertEquals("yyyy-MM-dd ", FixedFormat.DEFAULT.getFastDateFormat().getPattern());
    }
    
    @Test
    public void testCreateIfSupported_nonNullIfNameMatches() {
        for (final CustomTimeFormat.FixedFormat format : CustomTimeFormat.FixedFormat.values()) {
            final String[] options = {format.name()};
            assertNotNull(format.name(), CustomTimeFormat.createIfSupported(options));
        }
    }

    @Test
    public void testCreateIfSupported_nonNullIfPatternMatches() {
        for (final CustomTimeFormat.FixedFormat format : CustomTimeFormat.FixedFormat.values()) {
            final String[] options = {format.getPattern()};
            assertNotNull(format.name(), CustomTimeFormat.createIfSupported(options));
        }
    }

    @Test
    public void testCreateIfSupported_nullIfNameDoesNotMatch() {
        final String[] options = {"DEFAULT3"};
        assertNull("DEFAULT3", CustomTimeFormat.createIfSupported(options));
    }

    @Test
    public void testCreateIfSupported_nullIfPatternDoesNotMatch() {
        final String[] options = {"y M d H m s"};
        assertNull("y M d H m s", CustomTimeFormat.createIfSupported(options));
    }

    @Test
    public void testCreateIfSupported_nullIfOptionsArrayNull() {
        assertNull("null", CustomTimeFormat.createIfSupported(null));
    }

    @Test
    public void testCreateIfSupported_nullIfOptionsArrayHasTwoElements() {
        final String[] options = {CustomTimeFormat.FixedFormat.ABSOLUTE.getPattern(), "+08:00"};
        assertNull("timezone", CustomTimeFormat.createIfSupported(options));
    }

    @Test(expected=NullPointerException.class)
    public void testConstructorDisallowsNull() {
        new CustomTimeFormat(null);
    }

    @Test
    public void testGetFormatReturnsConstructorFixedFormatPattern() {
        final CustomTimeFormat format = new CustomTimeFormat(CustomTimeFormat.FixedFormat.ABSOLUTE);
        assertSame(CustomTimeFormat.FixedFormat.ABSOLUTE.getPattern(), format.getFormat());
    }

    @Test
    public void testFormatLong() {
        final long start = System.currentTimeMillis();
        final long end = start + TimeUnit.HOURS.toMillis(25);
        for (final FixedFormat format : FixedFormat.values()) {
            final SimpleDateFormat simpleDF = new SimpleDateFormat(format.getPattern());
            final CustomTimeFormat customTF = new CustomTimeFormat(format);
            for (long time = start; time < end; time += 12345) {
                final String actual = customTF.format(time);
                final String expected = simpleDF.format(new Date(time));
                assertEquals(format + "/" + time, expected, actual);
            }
        }
    }

    @Test
    public void testFormatLongCharArrayInt() {
        final long start = System.currentTimeMillis();
        final long end = start + TimeUnit.HOURS.toMillis(25);
        final char[] buffer = new char[128];
        for (final FixedFormat format : FixedFormat.values()) {
            final SimpleDateFormat simpleDF = new SimpleDateFormat(format.getPattern());
            final CustomTimeFormat customTF = new CustomTimeFormat(format);
            for (long time = start; time < end; time += 12345) {
                final int length = customTF.format(time, buffer, 23);
                final String actual = new String(buffer, 23, length);
                final String expected = simpleDF.format(new Date(time));
                assertEquals(format + "/" + time, expected, actual);
            }
        }
    }

}
