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
package org.apache.logging.log4j.core.pattern;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.TimeZone;

import org.apache.logging.log4j.core.AbstractLogEvent;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.datetime.FixedDateFormat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class DatePatternConverterTest {

    /**
     * SimpleTimePattern for DEFAULT.
     */
    private static final String DEFAULT_PATTERN = FixedDateFormat.FixedFormat.DEFAULT.getPattern();

    /**
     * ISO8601 string literal.
     */
    private static final String ISO8601_FORMAT = FixedDateFormat.FixedFormat.ISO8601.name();

    private static final String[] ISO8601_FORMAT_OPTIONS = {ISO8601_FORMAT};

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{{Boolean.TRUE}, {Boolean.FALSE}});
    }

    public DatePatternConverterTest(final Boolean threadLocalEnabled) throws Exception {
        // Setting the system property does not work: the Constant field has already been initialized...
        //System.setProperty("log4j2.enable.threadlocals", threadLocalEnabled.toString());

        final Field field = Constants.class.getDeclaredField("ENABLE_THREADLOCALS");
        field.setAccessible(true); // make non-private

        final Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL); // make non-final

        field.setBoolean(null, threadLocalEnabled.booleanValue());
    }

    @Test
    public void testNewInstanceAllowsNullParameter() {
        DatePatternConverter.newInstance(null); // no errors
    }

    @Test
    public void testFormatLogEventStringBuilderDefaultPattern() {
        final LogEvent event = new MyLogEvent();
        final DatePatternConverter converter = DatePatternConverter.newInstance(null);
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);

        final String expected = "2011-12-30 10:56:35,987";
        assertEquals(expected, sb.toString());
    }

    @Test
    public void testFormatLogEventStringBuilderIso8601() {
        final LogEvent event = new MyLogEvent();
        final DatePatternConverter converter = DatePatternConverter.newInstance(ISO8601_FORMAT_OPTIONS);
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);

        final String expected = "2011-12-30T10:56:35,987";
        assertEquals(expected, sb.toString());
    }

    @Test
    public void testFormatLogEventStringBuilderIso8601TimezoneUTC() {
        final LogEvent event = new MyLogEvent();
        final DatePatternConverter converter = DatePatternConverter.newInstance(new String[] {"ISO8601", "UTC"});
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);

        final TimeZone tz = TimeZone.getTimeZone("UTC");
        final SimpleDateFormat sdf = new SimpleDateFormat(converter.getPattern());
        sdf.setTimeZone(tz);
        final long adjusted = event.getTimeMillis() + tz.getDSTSavings();
        final String expected = sdf.format(new Date(adjusted));
        // final String expected = "2011-12-30T09:56:35,987";
        assertEquals(expected, sb.toString());
    }

    @Test
    public void testFormatLogEventStringBuilderIso8601TimezoneJST() {
        final LogEvent event = new MyLogEvent();
        final String[] optionsWithTimezone = {ISO8601_FORMAT, "JST"};
        final DatePatternConverter converter = DatePatternConverter.newInstance(optionsWithTimezone);
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);

        // JST=Japan Standard Time: UTC+9:00
        final TimeZone tz = TimeZone.getTimeZone("JST");
        final SimpleDateFormat sdf = new SimpleDateFormat(converter.getPattern());
        sdf.setTimeZone(tz);
        final long adjusted = event.getTimeMillis() + tz.getDSTSavings();
        final String expected = sdf.format(new Date(adjusted));
        // final String expected = "2011-12-30T18:56:35,987"; // in CET (Central Eastern Time: Amsterdam)
        assertEquals(expected, sb.toString());
    }

    @Test
    public void testPredefinedFormatWithTimezone() {
        for (final FixedDateFormat.FixedFormat format : FixedDateFormat.FixedFormat.values()) {
            final String[] options = {format.name(), "PDT"}; // Pacific Daylight Time=UTC-8:00
            final DatePatternConverter converter = DatePatternConverter.newInstance(options);
            assertEquals(format.getPattern(), converter.getPattern());
        }
    }

    @Test
    public void testPredefinedFormatWithoutTimezone() {
        for (final FixedDateFormat.FixedFormat format : FixedDateFormat.FixedFormat.values()) {
            final String[] options = {format.name()};
            final DatePatternConverter converter = DatePatternConverter.newInstance(options);
            assertEquals(format.getPattern(), converter.getPattern());
        }
    }

    private class MyLogEvent extends AbstractLogEvent {
        private static final long serialVersionUID = 0;

        @Override
        public long getTimeMillis() {
            final Calendar cal = Calendar.getInstance();
            cal.set(2011, Calendar.DECEMBER, 30, 10, 56, 35);
            cal.set(Calendar.MILLISECOND, 987);
            return cal.getTimeInMillis();
        }
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
    public void testFormatDateStringBuilderDefaultPattern() {
        final DatePatternConverter converter = DatePatternConverter.newInstance(null);
        final StringBuilder sb = new StringBuilder();
        converter.format(date(2001, 1, 1), sb);

        final String expected = "2001-02-01 14:15:16,123";
        assertEquals(expected, sb.toString());
    }

    @Test
    public void testFormatDateStringBuilderIso8601() {
        final DatePatternConverter converter = DatePatternConverter.newInstance(ISO8601_FORMAT_OPTIONS);
        final StringBuilder sb = new StringBuilder();
        converter.format(date(2001, 1, 1), sb);

        final String expected = "2001-02-01T14:15:16,123";
        assertEquals(expected, sb.toString());
    }

    @Test
    public void testFormatDateStringBuilderIso8601WithPeriod() {
        final String[] pattern = {FixedDateFormat.FixedFormat.ISO8601_PERIOD.name()};
        final DatePatternConverter converter = DatePatternConverter.newInstance(pattern);
        final StringBuilder sb = new StringBuilder();
        converter.format(date(2001, 1, 1), sb);

        final String expected = "2001-02-01T14:15:16.123";
        assertEquals(expected, sb.toString());
    }

    @Test
    public void testFormatDateStringBuilderIso8601BasicWithPeriod() {
        final String[] pattern = {FixedDateFormat.FixedFormat.ISO8601_BASIC_PERIOD.name()};
        final DatePatternConverter converter = DatePatternConverter.newInstance(pattern);
        final StringBuilder sb = new StringBuilder();
        converter.format(date(2001, 1, 1), sb);

        final String expected = "20010201T141516.123";
        assertEquals(expected, sb.toString());
    }

    @Test
    public void testFormatDateStringBuilderOriginalPattern() {
        final String[] pattern = {"yyyy/MM/dd HH-mm-ss.SSS"};
        final DatePatternConverter converter = DatePatternConverter.newInstance(pattern);
        final StringBuilder sb = new StringBuilder();
        converter.format(date(2001, 1, 1), sb);

        final String expected = "2001/02/01 14-15-16.123";
        assertEquals(expected, sb.toString());
    }

    @Test
    public void testFormatStringBuilderObjectArrayDefaultPattern() {
        final DatePatternConverter converter = DatePatternConverter.newInstance(null);
        final StringBuilder sb = new StringBuilder();
        converter.format(sb, date(2001, 1, 1), date(2002, 2, 2), date(2003, 3, 3));

        final String expected = "2001-02-01 14:15:16,123"; // only process first date
        assertEquals(expected, sb.toString());
    }

    @Test
    public void testFormatStringBuilderObjectArrayIso8601() {
        final DatePatternConverter converter = DatePatternConverter.newInstance(ISO8601_FORMAT_OPTIONS);
        final StringBuilder sb = new StringBuilder();
        converter.format(sb, date(2001, 1, 1), date(2002, 2, 2), date(2003, 3, 3));

        final String expected = "2001-02-01T14:15:16,123"; // only process first date
        assertEquals(expected, sb.toString());
    }

    private Date date(final int year, final int month, final int date) {
        final Calendar cal = Calendar.getInstance();
        cal.set(year, month, date, 14, 15, 16);
        cal.set(Calendar.MILLISECOND, 123);
        return cal.getTime();
    }

    @Test
    public void testGetPatternReturnsDefaultForNullOptions() {
        assertEquals(DEFAULT_PATTERN, DatePatternConverter.newInstance(null).getPattern());
    }

    @Test
    public void testGetPatternReturnsDefaultForEmptyOptionsArray() {
        assertEquals(DEFAULT_PATTERN, DatePatternConverter.newInstance(new String[0]).getPattern());
    }

    @Test
    public void testGetPatternReturnsDefaultForSingleNullElementOptionsArray() {
        assertEquals(DEFAULT_PATTERN, DatePatternConverter.newInstance(new String[1]).getPattern());
    }

    @Test
    public void testGetPatternReturnsDefaultForTwoNullElementsOptionsArray() {
        assertEquals(DEFAULT_PATTERN, DatePatternConverter.newInstance(new String[2]).getPattern());
    }

    @Test
    public void testGetPatternReturnsDefaultForInvalidPattern() {
        final String[] invalid = {"ABC I am not a valid date pattern"};
        assertEquals(DEFAULT_PATTERN, DatePatternConverter.newInstance(invalid).getPattern());
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

}
