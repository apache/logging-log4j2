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
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.core.util.datetime.FixedDateFormat;
import org.apache.logging.log4j.core.util.datetime.FixedDateFormat.FixedTimeZoneFormat;
import org.apache.logging.log4j.util.Strings;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class DatePatternConverterTest {

    private class MyLogEvent extends AbstractLogEvent {
        private static final long serialVersionUID = 0;

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

    /**
     * SimpleTimePattern for DEFAULT.
     */
    private static final String DEFAULT_PATTERN = FixedDateFormat.FixedFormat.DEFAULT.getPattern();

    /**
     * ISO8601 string literal.
     */
    private static final String ISO8601 = FixedDateFormat.FixedFormat.ISO8601.name();

    /**
     * ISO8601_OFFSET_DATE_TIME_XX string literal.
     */
    private static final String ISO8601_OFFSET_DATE_TIME_HHMM = FixedDateFormat.FixedFormat.ISO8601_OFFSET_DATE_TIME_HHMM.name();

    /**
     * ISO8601_OFFSET_DATE_TIME_XXX string literal.
     */
    private static final String ISO8601_OFFSET_DATE_TIME_HHCMM = FixedDateFormat.FixedFormat.ISO8601_OFFSET_DATE_TIME_HHCMM.name();

    private static final String[] ISO8601_FORMAT_OPTIONS = { ISO8601 };

    @Parameterized.Parameters(name = "threadLocalEnabled={0}")
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

    private static Date date(final int year, final int month, final int date) {
        final Calendar cal = Calendar.getInstance();
        cal.set(year, month, date, 14, 15, 16);
        cal.set(Calendar.MILLISECOND, 123);
        return cal.getTime();
    }

    private String precisePattern(final String pattern, final int precision) {
        final String search = "SSS";
        final int foundIndex = pattern.indexOf(search);
        final String seconds = pattern.substring(0, foundIndex);
        final String remainder = pattern.substring(foundIndex + search.length());
        return seconds + "nnnnnnnnn".substring(0, precision) + remainder;
    }

    @Test
    public void testFormatDateStringBuilderDefaultPattern() {
        assertDatePattern(null, date(2001, 1, 1), "2001-02-01 14:15:16,123");
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
    public void testFormatDateStringBuilderIso8601BasicWithPeriod() {
        assertDatePattern(FixedDateFormat.FixedFormat.ISO8601_BASIC_PERIOD.name(), date(2001, 1, 1), "20010201T141516.123");
    }

    @Test
    public void testFormatDateStringBuilderIso8601WithPeriod() {
        assertDatePattern(FixedDateFormat.FixedFormat.ISO8601_PERIOD.name(), date(2001, 1, 1), "2001-02-01T14:15:16.123");
    }

    @Test
    public void testFormatDateStringBuilderIso8601WithPeriodMicroseconds() {
        final String[] pattern = {FixedDateFormat.FixedFormat.ISO8601_PERIOD_MICROS.name(), "Z"};
        final DatePatternConverter converter = DatePatternConverter.newInstance(pattern);
        final StringBuilder sb = new StringBuilder();
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochMilli(
                1577225134559L,
                // One microsecond
                1000);
        converter.format(instant, sb);

        final String expected = "2019-12-24T22:05:34.559001";
        assertEquals(expected, sb.toString());
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
    public void testFormatAmericanPatterns() {
        Date date = date(2011, 2, 11);
        assertDatePattern("US_MONTH_DAY_YEAR4_TIME", date, "11/03/2011 14:15:16.123");
        assertDatePattern("US_MONTH_DAY_YEAR2_TIME", date, "11/03/11 14:15:16.123");
        assertDatePattern("dd/MM/yyyy HH:mm:ss.SSS", date, "11/03/2011 14:15:16.123");
        assertDatePattern("dd/MM/yyyy HH:mm:ss.nnnnnn", date, "11/03/2011 14:15:16.123000");
        assertDatePattern("dd/MM/yy HH:mm:ss.SSS", date, "11/03/11 14:15:16.123");
        assertDatePattern("dd/MM/yy HH:mm:ss.nnnnnn", date, "11/03/11 14:15:16.123000");
    }

    private static void assertDatePattern(final String format, final Date date, final String expected) {
        DatePatternConverter converter = DatePatternConverter.newInstance(new String[] {format});
        StringBuilder sb = new StringBuilder();
        converter.format(date, sb);

        assertEquals(expected, sb.toString());
    }

    @Test
    public void testFormatLogEventStringBuilderIso8601TimezoneJST() {
        final LogEvent event = new MyLogEvent();
        final String[] optionsWithTimezone = { ISO8601, "JST" };
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
    public void testFormatLogEventStringBuilderIso8601TimezoneOffsetHHCMM() {
        final LogEvent event = new MyLogEvent();
        final String[] optionsWithTimezone = { ISO8601_OFFSET_DATE_TIME_HHCMM };
        final DatePatternConverter converter = DatePatternConverter.newInstance(optionsWithTimezone);
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);

        final SimpleDateFormat sdf = new SimpleDateFormat(converter.getPattern());
        final String format = sdf.format(new Date(event.getTimeMillis()));
        final String expected = format.endsWith("Z") ? format.substring(0, format.length() - 1) + "+00:00" : format;
        assertEquals(expected, sb.toString());
    }

    @Test
    public void testFormatLogEventStringBuilderIso8601TimezoneOffsetHHMM() {
        final LogEvent event = new MyLogEvent();
        final String[] optionsWithTimezone = { ISO8601_OFFSET_DATE_TIME_HHMM };
        final DatePatternConverter converter = DatePatternConverter.newInstance(optionsWithTimezone);
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);

        final SimpleDateFormat sdf = new SimpleDateFormat(converter.getPattern());
        final String format = sdf.format(new Date(event.getTimeMillis()));
        final String expected = format.endsWith("Z") ? format.substring(0, format.length() - 1) + "+0000" : format;
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
    public void testFormatLogEventStringBuilderIso8601TimezoneZ() {
        final LogEvent event = new MyLogEvent();
        final String[] optionsWithTimezone = { ISO8601, "Z" };
        final DatePatternConverter converter = DatePatternConverter.newInstance(optionsWithTimezone);
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);

        final TimeZone tz = TimeZone.getTimeZone("UTC");
        final SimpleDateFormat sdf = new SimpleDateFormat(converter.getPattern());
        sdf.setTimeZone(tz);
        final long adjusted = event.getTimeMillis() + tz.getDSTSavings();
        final String expected = sdf.format(new Date(adjusted));
        // final String expected = "2011-12-30T17:56:35,987"; // in UTC
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

    @Test
    public void testGetPatternReturnsDefaultForEmptyOptionsArray() {
        assertEquals(DEFAULT_PATTERN, DatePatternConverter.newInstance(Strings.EMPTY_ARRAY).getPattern());
    }

    @Test
    public void testGetPatternReturnsDefaultForInvalidPattern() {
        final String[] invalid = {"ABC I am not a valid date pattern"};
        assertEquals(DEFAULT_PATTERN, DatePatternConverter.newInstance(invalid).getPattern());
    }

    @Test
    public void testGetPatternReturnsDefaultForNullOptions() {
        assertEquals(DEFAULT_PATTERN, DatePatternConverter.newInstance(null).getPattern());
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
    public void testInvalidLongPatternIgnoresExcessiveDigits() {
        final StringBuilder preciseBuilder = new StringBuilder();
        final StringBuilder milliBuilder = new StringBuilder();
        final LogEvent event = new MyLogEvent();

        for (final FixedDateFormat.FixedFormat format : FixedDateFormat.FixedFormat.values()) {
            final String pattern = format.getPattern();
            final String search = "SSS";
            final int foundIndex = pattern.indexOf(search);
            if (pattern.endsWith("n") || pattern.matches(".+n+X*") || pattern.matches(".+n+Z*")) {
                // ignore patterns that already have precise time formats
                // ignore patterns that do not use seconds.
                continue;
            }
            preciseBuilder.setLength(0);
            milliBuilder.setLength(0);

            final DatePatternConverter preciseConverter;
            final String precisePattern;
            if (foundIndex < 0) {
                precisePattern = pattern;
            } else {
                final String subPattern = pattern.substring(0, foundIndex);
                final String remainder = pattern.substring(foundIndex + search.length());
                precisePattern = subPattern + "nnnnnnnnn" + "n" + remainder; // nanos too long
            }
            preciseConverter = DatePatternConverter.newInstance(new String[] { precisePattern });
            preciseConverter.format(event, preciseBuilder);

            final String[] milliOptions = { pattern };
            DatePatternConverter.newInstance(milliOptions).format(event, milliBuilder);
            final FixedTimeZoneFormat timeZoneFormat = format.getFixedTimeZoneFormat();
            final int truncateLen = 3 + (timeZoneFormat != null ? timeZoneFormat.getLength() : 0);
            final String tz = timeZoneFormat != null
                    ? milliBuilder.substring(milliBuilder.length() - timeZoneFormat.getLength(), milliBuilder.length())
                    : Strings.EMPTY;
            milliBuilder.setLength(milliBuilder.length() - truncateLen); // truncate millis
            if (foundIndex >= 0) {
                milliBuilder.append("987123456");
            }
            final String expected = milliBuilder.append(tz).toString();

            assertEquals("format = " + format + ", pattern = " + pattern + ", precisePattern = " + precisePattern,
                    expected, preciseBuilder.toString());
            // System.out.println(preciseOptions[0] + ": " + precise);
        }
    }

    @Test
    public void testNewInstanceAllowsNullParameter() {
        DatePatternConverter.newInstance(null); // no errors
    }

    // test with all formats from one 'n' (100s of millis) to 'nnnnnnnnn' (nanosecond precision)
    @Test
    public void testPredefinedFormatWithAnyValidNanoPrecision() {
        final StringBuilder preciseBuilder = new StringBuilder();
        final StringBuilder milliBuilder = new StringBuilder();
        final LogEvent event = new MyLogEvent();

        for (final String timeZone : new String[]{"PST", null}) { // Pacific Standard Time=UTC-8:00
            for (final FixedDateFormat.FixedFormat format : FixedDateFormat.FixedFormat.values()) {
                for (int i = 1; i <= 9; i++) {
                    final String pattern = format.getPattern();
                    if (pattern.endsWith("n") || pattern.matches(".+n+X*") || pattern.matches(".+n+Z*")
                            || pattern.indexOf("SSS") < 0) {
                        // ignore patterns that already have precise time formats
                        // ignore patterns that do not use seconds.
                        continue;
                    }
                    preciseBuilder.setLength(0);
                    milliBuilder.setLength(0);

                    final String precisePattern = precisePattern(pattern, i);
                    final String[] preciseOptions = { precisePattern, timeZone };
                    final DatePatternConverter preciseConverter = DatePatternConverter.newInstance(preciseOptions);
                    preciseConverter.format(event, preciseBuilder);

                    final String[] milliOptions = { pattern, timeZone };
                    DatePatternConverter.newInstance(milliOptions).format(event, milliBuilder);
                    final FixedTimeZoneFormat timeZoneFormat = format.getFixedTimeZoneFormat();
                    final int truncateLen = 3 + (timeZoneFormat != null ? timeZoneFormat.getLength() : 0);
                    final String tz = timeZoneFormat != null
                            ? milliBuilder.substring(milliBuilder.length() - timeZoneFormat.getLength(),
                                    milliBuilder.length())
                            : Strings.EMPTY;
                    milliBuilder.setLength(milliBuilder.length() - truncateLen); // truncate millis
                    final String expected = milliBuilder.append("987123456".substring(0, i)).append(tz).toString();

                    assertEquals(
                            "format = " + format + ", pattern = " + pattern + ", precisePattern = " + precisePattern,
                            expected, preciseBuilder.toString());
                    // System.out.println(preciseOptions[0] + ": " + precise);
                }
            }
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

    @Test
    public void testPredefinedFormatWithTimezone() {
        for (final FixedDateFormat.FixedFormat format : FixedDateFormat.FixedFormat.values()) {
            final String[] options = {format.name(), "PST"}; // Pacific Standard Time=UTC-8:00
            final DatePatternConverter converter = DatePatternConverter.newInstance(options);
            assertEquals(format.getPattern(), converter.getPattern());
        }
    }

}
