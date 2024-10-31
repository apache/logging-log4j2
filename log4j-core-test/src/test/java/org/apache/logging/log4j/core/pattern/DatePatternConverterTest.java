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
package org.apache.logging.log4j.core.pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Calendar;
import java.util.Date;
import org.apache.logging.log4j.core.AbstractLogEvent;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Test;

class DatePatternConverterTest {

    private static final class MyLogEvent extends AbstractLogEvent {

        @Override
        public Instant getInstant() {
            final MutableInstant result = new MutableInstant();
            result.initFromEpochMilli(getTimeMillis(), 123456);
            return result;
        }

        @Override
        public long getTimeMillis() {
            final Calendar cal = Calendar.getInstance();
            cal.set(2011, Calendar.DECEMBER, 30, 10, 56, 35);
            cal.set(Calendar.MILLISECOND, 987);
            return cal.getTimeInMillis();
        }
    }

    private static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";

    private static Date date(final int year, final int month, final int date) {
        final Calendar cal = Calendar.getInstance();
        cal.set(year, month, date, 14, 15, 16);
        cal.set(Calendar.MILLISECOND, 123);
        return cal.getTime();
    }

    @Test
    public void testFormatDateStringBuilderDefaultPattern() {
        assertDatePattern(null, date(2001, 1, 1), "2001-02-01 14:15:16.123");
    }

    @Test
    public void testFormatDateStringBuilderOriginalPattern() {
        assertDatePattern("yyyy/MM/dd HH-mm-ss.SSS", date(2001, 1, 1), "2001/02/01 14-15-16.123");
    }

    @Test
    public void testFormatLogEventStringBuilderDefaultPattern() {
        final LogEvent event = new MyLogEvent();
        final DatePatternConverter converter = DatePatternConverter.newInstance(null);
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);

        final String expected = "2011-12-30 10:56:35.987";
        assertEquals(expected, sb.toString());
    }

    @Test
    public void testFormatAmericanPatterns() {
        final Date date = date(2011, 2, 11);
        assertDatePattern("dd/MM/yyyy HH:mm:ss.SSS", date, "11/03/2011 14:15:16.123");
        assertDatePattern("dd/MM/yyyy HH:mm:ss.SSSSSS", date, "11/03/2011 14:15:16.123000");
        assertDatePattern("dd/MM/yy HH:mm:ss.SSS", date, "11/03/11 14:15:16.123");
        assertDatePattern("dd/MM/yy HH:mm:ss.SSSSSS", date, "11/03/11 14:15:16.123000");
    }

    private static void assertDatePattern(final String format, final Date date, final String expected) {
        final DatePatternConverter converter = DatePatternConverter.newInstance(new String[] {format});
        final StringBuilder sb = new StringBuilder();
        converter.format(date, sb);

        assertEquals(expected, sb.toString());
    }

    @Test
    public void testFormatObjectStringBuilderDefaultPattern() {
        final DatePatternConverter converter = DatePatternConverter.newInstance(null);
        final StringBuilder sb = new StringBuilder();
        converter.format("nondate", sb);

        final String expected = ""; // only process dates
        assertEquals(expected, sb.toString());
    }

    @Test
    public void testFormatStringBuilderObjectArrayDefaultPattern() {
        final DatePatternConverter converter = DatePatternConverter.newInstance(null);
        final StringBuilder sb = new StringBuilder();
        converter.format(sb, date(2001, 1, 1), date(2002, 2, 2), date(2003, 3, 3));

        final String expected = "2001-02-01 14:15:16.123"; // only process first date
        assertEquals(expected, sb.toString());
    }

    @Test
    public void testGetPatternReturnsDefaultForEmptyOptionsArray() {
        assertEquals(
                DEFAULT_PATTERN,
                DatePatternConverter.newInstance(Strings.EMPTY_ARRAY).getPattern());
    }

    @Test
    public void testGetPatternReturnsDefaultForInvalidPattern() {
        final String[] invalid = {"A single `V` is not allow by `DateTimeFormatter` and should cause an exception"};
        assertEquals(DEFAULT_PATTERN, DatePatternConverter.newInstance(invalid).getPattern());
    }

    @Test
    public void testGetPatternReturnsDefaultForNullOptions() {
        assertEquals(DEFAULT_PATTERN, DatePatternConverter.newInstance(null).getPattern());
    }

    @Test
    public void testGetPatternReturnsDefaultForSingleNullElementOptionsArray() {
        assertEquals(
                DEFAULT_PATTERN, DatePatternConverter.newInstance(new String[1]).getPattern());
    }

    @Test
    public void testGetPatternReturnsDefaultForTwoNullElementsOptionsArray() {
        assertEquals(
                DEFAULT_PATTERN, DatePatternConverter.newInstance(new String[2]).getPattern());
    }

    @Test
    public void testGetPatternReturnsNullForUnix() {
        final String[] options = {"UNIX"};
        assertNull(DatePatternConverter.newInstance(options).getPattern());
    }

    @Test
    public void testGetPatternReturnsNullForUnixMillis() {
        final String[] options = {"UNIX_MILLIS"};
        assertNull(DatePatternConverter.newInstance(options).getPattern());
    }

    @Test
    public void testNewInstanceAllowsNullParameter() {
        DatePatternConverter.newInstance(null); // no errors
    }
}
