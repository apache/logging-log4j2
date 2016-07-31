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
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.util.datetime.FixedDateFormat.FixedFormat;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests {@link FixedDateFormat}.
 */
public class FixedDateFormatTest {

    @Test
    public void testFixedFormat_getDatePatternNullIfNoDateInPattern() {
        assertNull(FixedFormat.ABSOLUTE.getDatePattern());
        assertNull(FixedFormat.ABSOLUTE_PERIOD.getDatePattern());
    }

    @Test
    public void testFixedFormat_getDatePatternLengthZeroIfNoDateInPattern() {
        assertEquals(0, FixedFormat.ABSOLUTE.getDatePatternLength());
        assertEquals(0, FixedFormat.ABSOLUTE_PERIOD.getDatePatternLength());
    }

    @Test
    public void testFixedFormat_getFastDateFormatNullIfNoDateInPattern() {
        assertNull(FixedFormat.ABSOLUTE.getFastDateFormat());
        assertNull(FixedFormat.ABSOLUTE_PERIOD.getFastDateFormat());
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
        for (final FixedDateFormat.FixedFormat format : FixedDateFormat.FixedFormat.values()) {
            final String[] options = {format.name()};
            assertNotNull(format.name(), FixedDateFormat.createIfSupported(options));
        }
    }

    @Test
    public void testCreateIfSupported_nonNullIfPatternMatches() {
        for (final FixedDateFormat.FixedFormat format : FixedDateFormat.FixedFormat.values()) {
            final String[] options = {format.getPattern()};
            assertNotNull(format.name(), FixedDateFormat.createIfSupported(options));
        }
    }

    @Test
    public void testCreateIfSupported_nullIfNameDoesNotMatch() {
        final String[] options = {"DEFAULT3"};
        assertNull("DEFAULT3", FixedDateFormat.createIfSupported(options));
    }

    @Test
    public void testCreateIfSupported_nullIfPatternDoesNotMatch() {
        final String[] options = {"y M d H m s"};
        assertNull("y M d H m s", FixedDateFormat.createIfSupported(options));
    }

    @Test
    public void testCreateIfSupported_defaultIfOptionsArrayNull() {
        final FixedDateFormat fmt = FixedDateFormat.createIfSupported((String[]) null);
        assertEquals(FixedFormat.DEFAULT.getPattern(), fmt.getFormat());
    }

    @Test
    public void testCreateIfSupported_defaultIfOptionsArrayEmpty() {
        final FixedDateFormat fmt = FixedDateFormat.createIfSupported(new String[0]);
        assertEquals(FixedFormat.DEFAULT.getPattern(), fmt.getFormat());
    }

    @Test
    public void testCreateIfSupported_defaultIfOptionsArrayWithSingleNullElement() {
        final FixedDateFormat fmt = FixedDateFormat.createIfSupported(new String[1]);
        assertEquals(FixedFormat.DEFAULT.getPattern(), fmt.getFormat());
        assertEquals(TimeZone.getDefault(), fmt.getTimeZone());
    }

    @Test
    public void testCreateIfSupported_defaultTimeZoneIfOptionsArrayWithSecondNullElement() {
        final FixedDateFormat fmt = FixedDateFormat.createIfSupported(new String[] {FixedFormat.DEFAULT.getPattern(), null, ""});
        assertEquals(FixedFormat.DEFAULT.getPattern(), fmt.getFormat());
        assertEquals(TimeZone.getDefault(), fmt.getTimeZone());
    }

    @Test
    public void testCreateIfSupported_customTimeZoneIfOptionsArrayWithTimeZoneElement() {
        final FixedDateFormat fmt = FixedDateFormat.createIfSupported(new String[] {FixedFormat.DEFAULT.getPattern(), "+08:00", ""});
        assertEquals(FixedFormat.DEFAULT.getPattern(), fmt.getFormat());
        assertEquals(TimeZone.getTimeZone("+08:00"), fmt.getTimeZone());
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorDisallowsNullFormat() {
        new FixedDateFormat(null, TimeZone.getDefault());
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorDisallowsNullTimeZone() {
        new FixedDateFormat(FixedFormat.ABSOLUTE, null);
    }

    @Test
    public void testGetFormatReturnsConstructorFixedFormatPattern() {
        final FixedDateFormat format = new FixedDateFormat(FixedDateFormat.FixedFormat.ABSOLUTE, TimeZone.getDefault());
        assertSame(FixedDateFormat.FixedFormat.ABSOLUTE.getPattern(), format.getFormat());
    }

    @Test
    public void testFormatLong() {
        final long now = System.currentTimeMillis();
        final long start = now - TimeUnit.HOURS.toMillis(25);
        final long end = now + TimeUnit.HOURS.toMillis(25);
        for (final FixedFormat format : FixedFormat.values()) {
            final SimpleDateFormat simpleDF = new SimpleDateFormat(format.getPattern(), Locale.getDefault());
            final FixedDateFormat customTF = new FixedDateFormat(format, TimeZone.getDefault());
            for (long time = start; time < end; time += 12345) {
                final String actual = customTF.format(time);
                final String expected = simpleDF.format(new Date(time));
                assertEquals(format + "(" + format.getPattern() + ")" + "/" + time, expected, actual);
            }
        }
    }

    @Test
    public void testFormatLong_goingBackInTime() {
        final long now = System.currentTimeMillis();
        final long start = now - TimeUnit.HOURS.toMillis(25);
        final long end = now + TimeUnit.HOURS.toMillis(25);
        for (final FixedFormat format : FixedFormat.values()) {
            final SimpleDateFormat simpleDF = new SimpleDateFormat(format.getPattern(), Locale.getDefault());
            final FixedDateFormat customTF = new FixedDateFormat(format, TimeZone.getDefault());
            for (long time = end; time > start; time -= 12345) {
                final String actual = customTF.format(time);
                final String expected = simpleDF.format(new Date(time));
                assertEquals(format + "(" + format.getPattern() + ")" + "/" + time, expected, actual);
            }
        }
    }

    @Test
    public void testFormatLongCharArrayInt() {
        final long now = System.currentTimeMillis();
        final long start = now - TimeUnit.HOURS.toMillis(25);
        final long end = now + TimeUnit.HOURS.toMillis(25);
        final char[] buffer = new char[128];
        for (final FixedFormat format : FixedFormat.values()) {
            final SimpleDateFormat simpleDF = new SimpleDateFormat(format.getPattern(), Locale.getDefault());
            final FixedDateFormat customTF = new FixedDateFormat(format, TimeZone.getDefault());
            for (long time = start; time < end; time += 12345) {
                final int length = customTF.format(time, buffer, 23);
                final String actual = new String(buffer, 23, length);
                final String expected = simpleDF.format(new Date(time));
                assertEquals(format + "(" + format.getPattern() + ")" + "/" + time, expected, actual);
            }
        }
    }

    @Test
    public void testFormatLongCharArrayInt_goingBackInTime() {
        final long now = System.currentTimeMillis();
        final long start = now - TimeUnit.HOURS.toMillis(25);
        final long end = now + TimeUnit.HOURS.toMillis(25);
        final char[] buffer = new char[128];
        for (final FixedFormat format : FixedFormat.values()) {
            final SimpleDateFormat simpleDF = new SimpleDateFormat(format.getPattern(), Locale.getDefault());
            final FixedDateFormat customTF = new FixedDateFormat(format, TimeZone.getDefault());
            for (long time = end; time > start; time -= 12345) {
                final int length = customTF.format(time, buffer, 23);
                final String actual = new String(buffer, 23, length);
                final String expected = simpleDF.format(new Date(time));
                assertEquals(format + "(" + format.getPattern() + ")" + "/" + time, expected, actual);
            }
        }
    }

}
