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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Stream;
import org.apache.logging.log4j.core.AbstractLogEvent;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Test;

abstract class DatePatternConverterTestBase {
    // Mid-month instant to keep month-name assertions stable across all time zones.
    private static final long LOCALE_TEST_EPOCH_MILLIS = 1705276800000L;

    private static final class MyLogEvent extends AbstractLogEvent {
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

    private static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss,SSS";

    private static final String ISO8601 = "ISO8601";

    private static final String ISO8601_OFFSET_DATE_TIME_HHMM = "ISO8601_OFFSET_DATE_TIME_HHMM";

    private static final String ISO8601_OFFSET_DATE_TIME_HHCMM = "ISO8601_OFFSET_DATE_TIME_HHCMM";

    private static final String[] ISO8601_FORMAT_OPTIONS = {ISO8601};

    private final boolean threadLocalsEnabled;

    DatePatternConverterTestBase(final boolean threadLocalsEnabled) {
        this.threadLocalsEnabled = threadLocalsEnabled;
    }

    private static Date date(final int year, final int month, final int date) {
        final Calendar cal = Calendar.getInstance();
        cal.set(year, month, date, 14, 15, 16);
        cal.set(Calendar.MILLISECOND, 123);
        return cal.getTime();
    }

    @Test
    void testThreadLocalsConstant() {
        assertEquals(Constants.ENABLE_THREADLOCALS, threadLocalsEnabled);
    }

    @Test
    void testFormatDateStringBuilderDefaultPattern() {
        assertDatePattern(null, date(2001, 1, 1), "2001-02-01 14:15:16,123");
    }

    @SuppressWarnings("deprecation")
    @Test
    void testFormatDateStringBuilderIso8601() {
        final DatePatternConverter converter = DatePatternConverter.newInstance(ISO8601_FORMAT_OPTIONS);
        final StringBuilder sb = new StringBuilder();
        converter.format(date(2001, 1, 1), sb);

        final String expected = "2001-02-01T14:15:16,123";
        assertEquals(expected, sb.toString());
    }

    @Test
    void testFormatDateStringBuilderIso8601BasicWithPeriod() {
        assertDatePattern("ISO8601_BASIC_PERIOD", date(2001, 1, 1), "20010201T141516.123");
    }

    @Test
    void testFormatDateStringBuilderIso8601WithPeriod() {
        assertDatePattern("ISO8601_PERIOD", date(2001, 1, 1), "2001-02-01T14:15:16.123");
    }

    @SuppressWarnings("deprecation")
    @Test
    void testFormatDateStringBuilderIso8601WithPeriodMicroseconds() {
        final String[] pattern = {"ISO8601_PERIOD_MICROS", "Z"};
        final DatePatternConverter converter = DatePatternConverter.newInstance(pattern);
        final StringBuilder sb = new StringBuilder();
        final MutableInstant instant = new MutableInstant();
        instant.initFromEpochMilli(
                1577225134559L,
                // One microsecond
                1000);
        converter.format(instant, sb);

        final String expected = "2019-12-24T22:05:34.559001";
        assertEquals(expected, sb.toString());
    }

    @Test
    void testFormatDateStringBuilderOriginalPattern() {
        assertDatePattern("yyyy/MM/dd HH-mm-ss.SSS", date(2001, 1, 1), "2001/02/01 14-15-16.123");
    }

    @Test
    void testFormatLogEventStringBuilderDefaultPattern() {
        final LogEvent event = new MyLogEvent();
        final DatePatternConverter converter = DatePatternConverter.newInstance(null);
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);

        final String expected = "2011-12-30 10:56:35,987";
        assertEquals(expected, sb.toString());
    }

    @Test
    void testFormatLogEventStringBuilderIso8601() {
        final LogEvent event = new MyLogEvent();
        final DatePatternConverter converter = DatePatternConverter.newInstance(ISO8601_FORMAT_OPTIONS);
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);

        final String expected = "2011-12-30T10:56:35,987";
        assertEquals(expected, sb.toString());
    }

    @Test
    void testFormatAmericanPatterns() {
        final Date date = date(2011, 2, 11);
        assertDatePattern("US_MONTH_DAY_YEAR4_TIME", date, "11/03/2011 14:15:16.123");
        assertDatePattern("US_MONTH_DAY_YEAR2_TIME", date, "11/03/11 14:15:16.123");
        assertDatePattern("dd/MM/yyyy HH:mm:ss.SSS", date, "11/03/2011 14:15:16.123");
        assertDatePattern("dd/MM/yyyy HH:mm:ss.SSSSSS", date, "11/03/2011 14:15:16.123000");
        assertDatePattern("dd/MM/yy HH:mm:ss.SSS", date, "11/03/11 14:15:16.123");
        assertDatePattern("dd/MM/yy HH:mm:ss.SSSSSS", date, "11/03/11 14:15:16.123000");
    }

    @SuppressWarnings("deprecation")
    private static void assertDatePattern(final String format, final Date date, final String expected) {
        final DatePatternConverter converter = DatePatternConverter.newInstance(new String[] {format});
        final StringBuilder sb = new StringBuilder();
        converter.format(date, sb);

        assertEquals(expected, sb.toString());
    }

    @Test
    void testFormatLogEventStringBuilderIso8601TimezoneJST() {
        final LogEvent event = new MyLogEvent();
        final String[] optionsWithTimezone = {ISO8601, "JST"};
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
    void testFormatLogEventStringBuilderIso8601TimezoneOffsetHHCMM() {
        final LogEvent event = new MyLogEvent();
        final String[] optionsWithTimezone = {ISO8601_OFFSET_DATE_TIME_HHCMM};
        final DatePatternConverter converter = DatePatternConverter.newInstance(optionsWithTimezone);
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);

        final String expected = DateTimeFormatter.ofPattern(converter.getPattern())
                .withZone(ZoneId.systemDefault())
                .format((TemporalAccessor) event.getInstant());
        assertEquals(expected, sb.toString());
    }

    @Test
    void testFormatLogEventStringBuilderIso8601TimezoneOffsetHHMM() {
        final LogEvent event = new MyLogEvent();
        final String[] optionsWithTimezone = {ISO8601_OFFSET_DATE_TIME_HHMM};
        final DatePatternConverter converter = DatePatternConverter.newInstance(optionsWithTimezone);
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);

        final String expected = DateTimeFormatter.ofPattern(converter.getPattern())
                .withZone(ZoneId.systemDefault())
                .format((TemporalAccessor) event.getInstant());
        assertEquals(expected, sb.toString());
    }

    @Test
    void testFormatLogEventStringBuilderIso8601TimezoneUTC() {
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
    void testFormatLogEventStringBuilderIso8601TimezoneZ() {
        final LogEvent event = new MyLogEvent();
        final String[] optionsWithTimezone = {ISO8601, "Z"};
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
    void testFormatObjectStringBuilderDefaultPattern() {
        final DatePatternConverter converter = DatePatternConverter.newInstance(null);
        final StringBuilder sb = new StringBuilder();
        converter.format("nondate", sb);

        final String expected = ""; // only process dates
        assertEquals(expected, sb.toString());
    }

    @Test
    void testFormatStringBuilderObjectArrayDefaultPattern() {
        final DatePatternConverter converter = DatePatternConverter.newInstance(null);
        final StringBuilder sb = new StringBuilder();
        converter.format(sb, date(2001, 1, 1), date(2002, 2, 2), date(2003, 3, 3));

        final String expected = "2001-02-01 14:15:16,123"; // only process first date
        assertEquals(expected, sb.toString());
    }

    @Test
    void testFormatStringBuilderObjectArrayIso8601() {
        final DatePatternConverter converter = DatePatternConverter.newInstance(ISO8601_FORMAT_OPTIONS);
        final StringBuilder sb = new StringBuilder();
        converter.format(sb, date(2001, 1, 1), date(2002, 2, 2), date(2003, 3, 3));

        final String expected = "2001-02-01T14:15:16,123"; // only process first date
        assertEquals(expected, sb.toString());
    }

    @Test
    void testGetPatternReturnsDefaultForEmptyOptionsArray() {
        assertEquals(
                DEFAULT_PATTERN,
                DatePatternConverter.newInstance(Strings.EMPTY_ARRAY).getPattern());
    }

    @Test
    void testGetPatternReturnsDefaultForInvalidPattern() {
        final String[] invalid = {"A single `V` is not allow by `DateTimeFormatter` and should cause an exception"};
        assertEquals(DEFAULT_PATTERN, DatePatternConverter.newInstance(invalid).getPattern());
    }

    @Test
    void testGetPatternReturnsDefaultForNullOptions() {
        assertEquals(DEFAULT_PATTERN, DatePatternConverter.newInstance(null).getPattern());
    }

    @Test
    void testGetPatternReturnsDefaultForSingleNullElementOptionsArray() {
        assertEquals(
                DEFAULT_PATTERN, DatePatternConverter.newInstance(new String[1]).getPattern());
    }

    @Test
    void testGetPatternReturnsDefaultForTwoNullElementsOptionsArray() {
        assertEquals(
                DEFAULT_PATTERN, DatePatternConverter.newInstance(new String[2]).getPattern());
    }

    @Test
    void testGetPatternReturnsNullForUnix() {
        final String[] options = {"UNIX"};
        assertNull(DatePatternConverter.newInstance(options).getPattern());
    }

    @Test
    void testGetPatternReturnsNullForUnixMillis() {
        final String[] options = {"UNIX_MILLIS"};
        assertNull(DatePatternConverter.newInstance(options).getPattern());
    }

    @Test
    void testNewInstanceAllowsNullParameter() {
        DatePatternConverter.newInstance(null); // no errors
    }

    private static final String[] PATTERN_NAMES =
            Stream.of(NamedInstantPattern.values()).map(Enum::name).toArray(String[]::new);

    @Test
    void testPredefinedFormatWithoutTimezone() {
        for (final String patternName : PATTERN_NAMES) {
            final String[] options = {patternName};
            final DatePatternConverter converter = DatePatternConverter.newInstance(options);
            final String expectedPattern = DatePatternConverter.decodeNamedPattern(patternName);
            assertEquals(expectedPattern, converter.getPattern());
        }
    }

    @Test
    void testPredefinedFormatWithTimezone() {
        for (final String patternName : PATTERN_NAMES) {
            final String[] options = {patternName, "PST"}; // Pacific Standard Time=UTC-8:00
            final DatePatternConverter converter = DatePatternConverter.newInstance(options);
            final String expectedPattern = DatePatternConverter.decodeNamedPattern(patternName);
            assertEquals(expectedPattern, converter.getPattern());
        }
    }

    /**
     * Helper: format a fixed UTC instant through the given options.
     * The epoch is 2024-01-01T00:00:00Z: January / Januar depending on locale.
     */
    private static String formatLocaleInstant(final String... options) {
        final MutableInstant instant = new MutableInstant();
        instant.initFromEpochMilli(LOCALE_TEST_EPOCH_MILLIS, 0);
        final DatePatternConverter converter = DatePatternConverter.newInstance(options);
        final StringBuilder sb = new StringBuilder();
        converter.format(instant, sb);
        return sb.toString();
    }

    @Test
    void testIsLocaleOptionDetectsLocaleWithRegion() {
        assertTrue(DatePatternConverter.isLocaleOption("de-DE"));
        assertTrue(DatePatternConverter.isLocaleOption("fr-FR"));
        assertTrue(DatePatternConverter.isLocaleOption("zh-Hans-CN"));
        assertTrue(DatePatternConverter.isLocaleOption("en-US"));
    }

    @Test
    void testIsLocaleOptionDetectsLanguageOnlyLocale() {
        assertTrue(DatePatternConverter.isLocaleOption("de"));
        assertTrue(DatePatternConverter.isLocaleOption("fr"));
        assertTrue(DatePatternConverter.isLocaleOption("zh"));
    }

    @Test
    void testIsLocaleOptionRejectsTimezones() {
        assertFalse(DatePatternConverter.isLocaleOption("UTC"));
        assertFalse(DatePatternConverter.isLocaleOption("GMT"));
        assertFalse(DatePatternConverter.isLocaleOption("JST"));
        assertFalse(DatePatternConverter.isLocaleOption("PST"));
        assertFalse(DatePatternConverter.isLocaleOption("utc"));
        assertFalse(DatePatternConverter.isLocaleOption("gmt"));
        assertFalse(DatePatternConverter.isLocaleOption("pst"));
        assertFalse(DatePatternConverter.isLocaleOption("America/New_York"));
        assertFalse(DatePatternConverter.isLocaleOption("Etc/GMT+5"));
        assertFalse(DatePatternConverter.isLocaleOption("GB-Eire"));
        assertFalse(DatePatternConverter.isLocaleOption("gb-eire"));
        assertFalse(DatePatternConverter.isLocaleOption("NZ-CHAT"));
        assertFalse(DatePatternConverter.isLocaleOption(null));
    }

    @Test
    void testHyphenatedTimezonesAreNotTreatedAsLocales() {
        assertEquals(
                TimeZone.getTimeZone("GB-Eire"),
                DatePatternConverter.newInstance(new String[] {"ISO8601", "GB-Eire"})
                        .getTimeZone());
        assertEquals(
                TimeZone.getTimeZone("NZ-CHAT"),
                DatePatternConverter.newInstance(new String[] {"ISO8601", "NZ-CHAT"})
                        .getTimeZone());
    }

    /**
     * %d{dd-MMMM-yyyy}{GMT}{de-DE}: three args, always worked.
     * Verify baseline still produces German month name.
     */
    @Test
    void testLocaleAppliedWhenTimezoneAndLocaleBothProvided() {
        final String result = formatLocaleInstant("dd-MMMM-yyyy", "GMT", "de-DE");
        assertTrue(
                result.contains("Januar"),
                () -> "Expected German month 'Januar' with [pattern][GMT][de-DE], got: " + result);
    }

    /**
     * Locale-only argument should work generally across multiple locales.
     */
    @Test
    void testLocaleAppliedWhenTimezoneOmitted_monthName_multipleLocales() {
        final String[][] localeCases = {
            {"de-DE", "januar"},
            {"fr-FR", "janvier"},
            {"es-ES", "enero"},
            {"it-IT", "gennaio"},
            {"pt-BR", "janeiro"}
        };
        for (final String[] localeCase : localeCases) {
            final String localeTag = localeCase[0];
            final String expectedMonth = localeCase[1];
            final String result = formatLocaleInstant("dd-MMMM-yyyy", localeTag);
            final String lowerCaseResult = result.toLowerCase(Locale.ROOT);
            assertTrue(
                    lowerCaseResult.contains(expectedMonth),
                    () -> "Expected month '" + expectedMonth + "' with [pattern][" + localeTag
                            + "] (no timezone), got: " + result);
            assertFalse(
                    lowerCaseResult.contains("january"),
                    () -> "locale '" + localeTag + "' must not produce English 'January', got: " + result);
        }
    }

    @Test
    void testLocaleAppliedWhenTimezoneOmitted_languageOnlyTag() {
        final String result = formatLocaleInstant("dd-MMMM-yyyy", "de");
        assertTrue(
                result.contains("Januar"),
                () -> "Expected German month 'Januar' with [pattern][de] (no timezone), got: " + result);
        assertFalse(result.contains("January"), () -> "locale 'de' must not produce English 'January', got: " + result);
    }

    /**
     * %d{EEEE, dd. MMMM yyyy}{de-DE}: full date with day-of-week and locale only.
     * Regression: locale was silently ignored.
     * Fix: German day-of-week and month must be used.
     * See: https://github.com/apache/logging-log4j2/issues/4129
     */
    @Test
    void testLocaleAppliedWhenTimezoneOmitted_fullDate() {
        final String result = formatLocaleInstant("EEEE, dd. MMMM yyyy", "de-DE");
        assertTrue(
                result.contains("Januar"),
                () -> "Expected German month 'Januar' with full-date pattern + de-DE locale, got: " + result);
        assertTrue(
                result.contains("Montag"),
                () -> "Expected German day-of-week 'Montag' with full-date pattern + de-DE locale, got: " + result);
    }

    /**
     * When second arg is a locale tag, the effective timezone must remain the JVM default:
     * NOT be interpreted as the bogus GMT that {@code TimeZone.getTimeZone("de-DE")} returns.
     */
    @Test
    void testDefaultTimezoneUsedWhenOnlyLocaleProvided() {
        final DatePatternConverter converter = DatePatternConverter.newInstance(new String[] {"ISO8601", "de-DE"});
        assertEquals(TimeZone.getDefault(), converter.getTimeZone());
    }
}
