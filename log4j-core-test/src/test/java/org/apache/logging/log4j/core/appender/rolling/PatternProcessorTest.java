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
package org.apache.logging.log4j.core.appender.rolling;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

/**
 * Tests the PatternProcessor class.
 */
public class PatternProcessorTest {

    private static Instant parseLocalDateTime(final String text) {
        return LocalDateTime.parse(text).atZone(ZoneId.systemDefault()).toInstant();
    }

    @Test
    @ResourceLock(value = Resources.LOCALE, mode = ResourceAccessMode.READ)
    public void testDontInterpretBackslashAsEscape() {
        final PatternProcessor pp = new PatternProcessor("c:\\test\\new/app-%d{HH-mm-ss}.log");
        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 16);
        cal.set(Calendar.MINUTE, 2);
        cal.set(Calendar.SECOND, 15);

        final StringBuilder buf = new StringBuilder();
        pp.formatFileName(buf, cal.getTime(), 23);
        assertEquals("c:\\test\\new/app-16-02-15.log", buf.toString());
    }

    @Test
    @ResourceLock(value = Resources.LOCALE, mode = ResourceAccessMode.READ)
    public void testGetNextTimeHourlyReturnsFirstMinuteOfNextHour() {
        final PatternProcessor pp = new PatternProcessor("logs/app-%d{yyyy-MM-dd-HH}.log.gz");
        final Instant initial = parseLocalDateTime("2014-03-04T10:31:59");
        final long actual = pp.getNextTime(initial.toEpochMilli(), 1, false);

        final Instant expected = parseLocalDateTime("2014-03-04T11:00:00");
        assertEquals(expected, Instant.ofEpochMilli(actual));
    }

    @Test
    @ResourceLock(value = Resources.LOCALE, mode = ResourceAccessMode.READ)
    public void testGetNextTimeHourlyReturnsFirstMinuteOfNextHour2() {
        final PatternProcessor pp = new PatternProcessor("logs/app-%d{yyyy-MM-dd-HH}.log.gz");
        final Instant initial = parseLocalDateTime("2014-03-04T23:31:59");
        final long actual = pp.getNextTime(initial.toEpochMilli(), 1, false);

        final Instant expected = parseLocalDateTime("2014-03-05T00:00:00");
        assertEquals(expected, Instant.ofEpochMilli(actual));
    }

    @Test
    @ResourceLock(value = Resources.LOCALE, mode = ResourceAccessMode.READ)
    public void testGetNextTimeHourlyReturnsFirstMinuteOfNextHourDstStart() {
        // America/Chicago 2014 - DST start - Mar 9 02:00
        // during winter GMT-6
        // during summer GMT-5
        final PatternProcessor pp = new PatternProcessor("logs/app-%d{yyyy-MM-dd-HH}{America/Chicago}.log.gz");
        final Instant initial =
                OffsetDateTime.parse("2014-03-09T01:31:59-06:00").toInstant();
        final long actual = pp.getNextTime(initial.toEpochMilli(), 1, false);

        final Instant expected =
                OffsetDateTime.parse("2014-03-09T02:00:00-06:00").toInstant();
        assertEquals(expected, Instant.ofEpochMilli(actual));
    }

    @Test
    @ResourceLock(value = Resources.LOCALE, mode = ResourceAccessMode.READ)
    public void testGetNextTimeHourlyReturnsFirstMinuteOfHourAfterNextHourDstEnd() {
        // America/Chicago 2014 - DST end - Nov 2 02:00
        // during summer GMT-5
        // during winter GMT-6
        final PatternProcessor pp = new PatternProcessor("logs/app-%d{yyyy-MM-dd-HH}{America/Chicago}.log.gz");
        final Instant initial =
                OffsetDateTime.parse("2014-11-02T01:31:59-05:00").toInstant();
        final long actual = pp.getNextTime(initial.toEpochMilli(), 1, false);

        // expect 1h 29min since initial
        final Instant expected =
                OffsetDateTime.parse("2014-11-02T03:00:00-05:00").toInstant();
        assertEquals(expected, Instant.ofEpochMilli(actual));
    }

    @Test
    @ResourceLock(value = Resources.LOCALE, mode = ResourceAccessMode.READ)
    public void testGetNextTimeHourlyReturnsFirstMinuteOfNextYear() {
        final PatternProcessor pp = new PatternProcessor("logs/app-%d{yyyy-MM-dd-HH}.log.gz");
        final Instant initial = parseLocalDateTime("2015-12-31T23:31:59");
        final long actual = pp.getNextTime(initial.toEpochMilli(), 1, false);

        final Instant expected = parseLocalDateTime("2016-01-01T00:00:00");
        assertEquals(expected, Instant.ofEpochMilli(actual));
    }

    @Test
    @ResourceLock(value = Resources.LOCALE, mode = ResourceAccessMode.READ)
    public void testGetNextTimeMillisecondlyReturnsNextMillisec() {
        final PatternProcessor pp = new PatternProcessor("logs/app-%d{yyyy-MM-dd-HH-mm-ss.SSS}.log.gz");
        final Instant initial = parseLocalDateTime("2014-03-04T10:31:53.123");
        final long actual = pp.getNextTime(initial.toEpochMilli(), 1, false);

        final Instant expected = parseLocalDateTime("2014-03-04T10:31:53.124");
        assertEquals(expected, Instant.ofEpochMilli(actual));
    }

    @Test
    @ResourceLock(value = Resources.LOCALE, mode = ResourceAccessMode.READ)
    public void testGetNextTimeMinutelyReturnsFirstSecondOfNextMinute() {
        final PatternProcessor pp = new PatternProcessor("logs/app-%d{yyyy-MM-dd-HH-mm}.log.gz");
        final Instant initial = parseLocalDateTime("2014-03-04T10:31:59");
        final long actual = pp.getNextTime(initial.toEpochMilli(), 1, false);

        final Instant expected = parseLocalDateTime("2014-03-04T10:32:00");
        assertEquals(expected, Instant.ofEpochMilli(actual));
    }

    @Test
    @ResourceLock(value = Resources.LOCALE, mode = ResourceAccessMode.READ)
    public void testGetNextTimeMonthlyReturnsFirstDayOfNextMonth() {
        final PatternProcessor pp = new PatternProcessor("logs/app-%d{yyyy-MM}.log.gz");
        final Instant initial = parseLocalDateTime("2014-10-15T10:31:59");
        final long actual = pp.getNextTime(initial.toEpochMilli(), 1, false);

        final Instant expected = parseLocalDateTime("2014-11-01T00:00:00");
        assertEquals(expected, Instant.ofEpochMilli(actual));
    }

    @Test
    @ResourceLock(value = Resources.LOCALE, mode = ResourceAccessMode.READ)
    public void testGetNextTimeMonthlyReturnsFirstDayOfNextMonth2() {
        final PatternProcessor pp = new PatternProcessor("logs/app-%d{yyyy-MM}.log.gz");
        final Instant initial = parseLocalDateTime("2014-01-31T10:31:59");
        final long actual = pp.getNextTime(initial.toEpochMilli(), 1, false);

        // Expect 1st of next month: 2014 Feb 1st
        final Instant expected = parseLocalDateTime("2014-02-01T00:00:00");
        assertEquals(expected, Instant.ofEpochMilli(actual));
    }

    @Test
    @ResourceLock(value = Resources.LOCALE, mode = ResourceAccessMode.READ)
    public void testGetNextTimeMonthlyReturnsFirstDayOfNextMonth3() {
        final PatternProcessor pp = new PatternProcessor("logs/app-%d{yyyy-MM}.log.gz");
        final Instant initial = parseLocalDateTime("2014-12-31T10:31:59");
        final long actual = pp.getNextTime(initial.toEpochMilli(), 1, false);

        // Expect 1st of next month: 2015 Jan 1st
        final Instant expected = parseLocalDateTime("2015-01-01T00:00:00");
        assertEquals(expected, Instant.ofEpochMilli(actual));
    }

    @Test
    @ResourceLock(value = Resources.LOCALE, mode = ResourceAccessMode.READ)
    public void testGetNextTimeMonthlyReturnsFirstDayOfNextYear() {
        final PatternProcessor pp = new PatternProcessor("logs/app-%d{yyyy-MM}.log.gz");
        final Instant initial = parseLocalDateTime("2015-12-28T00:00:00");
        final long actual = pp.getNextTime(initial.toEpochMilli(), 1, false);

        // We expect 1st day of next month
        final Instant expected = parseLocalDateTime("2016-01-01T00:00:00");
        assertEquals(expected, Instant.ofEpochMilli(actual));
    }

    @Test
    @ResourceLock(value = Resources.LOCALE, mode = ResourceAccessMode.READ)
    public void testGetNextTimeSecondlyReturnsFirstMillisecOfNextSecond() {
        final PatternProcessor pp = new PatternProcessor("logs/app-%d{yyyy-MM-dd-HH-mm-ss}.log.gz");
        final Instant initial = parseLocalDateTime("2014-03-04T10:31:53.123");
        final long actual = pp.getNextTime(initial.toEpochMilli(), 1, false);

        final Instant expected = parseLocalDateTime("2014-03-04T10:31:54");
        assertEquals(expected, Instant.ofEpochMilli(actual));
    }

    @Test
    @ResourceLock(Resources.LOCALE)
    public void testGetNextTimeWeeklyReturnsFirstDayOfNextWeek_FRANCE() {
        final Locale old = Locale.getDefault();
        Locale.setDefault(Locale.FRANCE); // force 1st day of the week to be Monday

        try {
            final PatternProcessor pp = new PatternProcessor("logs/app-%d{yyyy-MM-W}.log.gz");
            final Instant initial = parseLocalDateTime("2014-03-04T10:31:59");
            final long actual = pp.getNextTime(initial.toEpochMilli(), 1, false);

            final Instant expected = parseLocalDateTime("2014-03-10T00:00:00");
            assertEquals(expected, Instant.ofEpochMilli(actual));
        } finally {
            Locale.setDefault(old);
        }
    }

    @Test
    @ResourceLock(Resources.LOCALE)
    public void testGetNextTimeWeeklyReturnsFirstDayOfNextWeek_US() {
        final Locale old = Locale.getDefault();
        Locale.setDefault(Locale.US); // force 1st day of the week to be Sunday

        try {
            final PatternProcessor pp = new PatternProcessor("logs/app-%d{yyyy-MM-W}.log.gz");
            final Instant initial = parseLocalDateTime("2014-03-04T10:31:59");
            final long actual = pp.getNextTime(initial.toEpochMilli(), 1, false);

            final Instant expected = parseLocalDateTime("2014-03-09T00:00:00");
            assertEquals(expected, Instant.ofEpochMilli(actual));
        } finally {
            Locale.setDefault(old);
        }
    }

    /**
     * Tests https://issues.apache.org/jira/browse/LOG4J2-1232
     */
    @Test
    @ResourceLock(Resources.LOCALE)
    public void testGetNextTimeWeeklyReturnsFirstWeekInYear_US() {
        final Locale old = Locale.getDefault();
        Locale.setDefault(Locale.US); // force 1st day of the week to be Sunday
        try {
            final PatternProcessor pp = new PatternProcessor("logs/market_data_msg.log-%d{yyyy-MM-'W'W}");
            final Instant initial = parseLocalDateTime("2015-12-28T00:00:00");
            final long actual = pp.getNextTime(initial.toEpochMilli(), 1, false);

            final Instant expected = parseLocalDateTime("2016-01-03T00:00:00");
            assertEquals(expected, Instant.ofEpochMilli(actual));
        } finally {
            Locale.setDefault(old);
        }
    }

    @Test
    @ResourceLock(value = Resources.LOCALE, mode = ResourceAccessMode.READ)
    public void testGetNextTimeDailyReturnsFirstHourOfNextDay() {
        final PatternProcessor pp = new PatternProcessor("logs/app-%d{yyyy-MM-dd}.log.gz");
        final Instant initial = parseLocalDateTime("2014-03-04T02:31:59");
        final long actual = pp.getNextTime(initial.toEpochMilli(), 1, false);

        final Instant expected = parseLocalDateTime("2014-03-05T00:00:00");
        assertEquals(expected, Instant.ofEpochMilli(actual));
    }

    @Test
    @ResourceLock(value = Resources.LOCALE, mode = ResourceAccessMode.READ)
    public void testGetNextTimeDailyReturnsFirstHourOfNextDayHonoringTimeZoneOption1() {
        final PatternProcessor pp = new PatternProcessor("logs/app-%d{yyyy-MM-dd}{GMT-6}.log.gz");
        final Instant initial =
                OffsetDateTime.parse("2014-03-04T02:31:59-06:00").toInstant();
        final long actual = pp.getNextTime(initial.toEpochMilli(), 1, false);

        final Instant expected =
                OffsetDateTime.parse("2014-03-05T00:00:00-06:00").toInstant();
        assertEquals(expected, Instant.ofEpochMilli(actual));
    }

    @Test
    @ResourceLock(value = Resources.LOCALE, mode = ResourceAccessMode.READ)
    @ResourceLock(value = Resources.TIME_ZONE)
    public void testGetNextTimeDailyReturnsFirstHourOfNextDayHonoringTimeZoneOption2() {
        final TimeZone old = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+10")); // default is ignored if pattern contains timezone
        try {
            final PatternProcessor pp = new PatternProcessor("logs/app-%d{yyyy-MM-dd}{GMT-6}.log.gz");
            final Instant initial =
                    OffsetDateTime.parse("2014-03-04T02:31:59-06:00").toInstant();
            final long actual = pp.getNextTime(initial.toEpochMilli(), 1, false);

            final Instant expected =
                    OffsetDateTime.parse("2014-03-05T00:00:00-06:00").toInstant();
            assertEquals(expected, Instant.ofEpochMilli(actual));
        } finally {
            TimeZone.setDefault(old);
        }
    }

    @Test
    @ResourceLock(value = Resources.LOCALE, mode = ResourceAccessMode.READ)
    @ResourceLock(value = Resources.TIME_ZONE)
    public void testGetNextTimeDailyReturnsFirstHourOfNextDayHonoringTimeZoneOption3() {
        final TimeZone old = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT-10")); // default is ignored if pattern contains timezone
        try {
            final PatternProcessor pp = new PatternProcessor("logs/app-%d{yyyy-MM-dd}{GMT-6}.log.gz");
            final Instant initial =
                    OffsetDateTime.parse("2014-03-04T02:31:59-06:00").toInstant();
            final long actual = pp.getNextTime(initial.toEpochMilli(), 1, false);

            final Instant expected =
                    OffsetDateTime.parse("2014-03-05T00:00:00-06:00").toInstant();
            assertEquals(expected, Instant.ofEpochMilli(actual));
        } finally {
            TimeZone.setDefault(old);
        }
    }

    @Test
    @ResourceLock(value = Resources.LOCALE, mode = ResourceAccessMode.READ)
    public void testGetNextTimeDailyReturnsFirstHourOfNextDayDstJan() {
        final PatternProcessor pp = new PatternProcessor("logs/app-%d{yyyy-MM-dd}{America/Chicago}.log.gz");
        final Instant initial =
                OffsetDateTime.parse("2014-01-04T00:31:59-06:00").toInstant();
        final long actual = pp.getNextTime(initial.toEpochMilli(), 1, false);

        final Instant expected =
                OffsetDateTime.parse("2014-01-05T00:00:00-06:00").toInstant();
        assertEquals(expected, Instant.ofEpochMilli(actual));
    }

    @Test
    @ResourceLock(value = Resources.LOCALE, mode = ResourceAccessMode.READ)
    public void testGetNextTimeDailyReturnsFirstHourOfNextDayDstJun() {
        final PatternProcessor pp = new PatternProcessor("logs/app-%d{yyyy-MM-dd}{America/Chicago}.log.gz");
        final Instant initial =
                OffsetDateTime.parse("2014-06-04T00:31:59-05:00").toInstant();
        final long actual = pp.getNextTime(initial.toEpochMilli(), 1, false);

        final Instant expected =
                OffsetDateTime.parse("2014-06-05T00:00:00-05:00").toInstant();
        assertEquals(expected, Instant.ofEpochMilli(actual));
    }

    @Test
    @ResourceLock(value = Resources.LOCALE, mode = ResourceAccessMode.READ)
    public void testGetNextTimeDailyReturnsFirstHourOfNextDayDstStart() {
        // America/Chicago 2014 - DST start - Mar 9 02:00
        // during winter GMT-6
        // during summer GMT-5
        final PatternProcessor pp = new PatternProcessor("logs/app-%d{yyyy-MM-dd}{America/Chicago}.log.gz");
        final Instant initial =
                OffsetDateTime.parse("2014-03-09T00:31:59-06:00").toInstant();
        final long actual = pp.getNextTime(initial.toEpochMilli(), 1, false);

        final Instant expected =
                OffsetDateTime.parse("2014-03-10T00:00:00-05:00").toInstant();
        assertEquals(expected, Instant.ofEpochMilli(actual));
    }

    @Test
    @ResourceLock(value = Resources.LOCALE, mode = ResourceAccessMode.READ)
    public void testGetNextTimeDailyReturnsFirstHourOfNextDayDstEnd() {
        // America/Chicago 2014 - DST end - Nov 2 02:00
        // during summer GMT-5
        // during winter GMT-6
        final PatternProcessor pp = new PatternProcessor("logs/app-%d{yyyy-MM-dd}{America/Chicago}.log.gz");
        final Instant initial =
                OffsetDateTime.parse("2014-11-02T00:31:59-05:00").toInstant();
        final long actual = pp.getNextTime(initial.toEpochMilli(), 1, false);

        final Instant expected =
                OffsetDateTime.parse("2014-11-03T00:00:00-06:00").toInstant();
        assertEquals(expected, Instant.ofEpochMilli(actual));
    }

    @Test
    @ResourceLock(value = Resources.LOCALE, mode = ResourceAccessMode.READ)
    @ResourceLock(value = Resources.TIME_ZONE)
    public void testGetNextTimeDailyReturnsFirstHourOfNextDayInGmtIfZoneIsInvalid() {
        final TimeZone old = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT-10")); // default is ignored even if timezone option invalid
        try {
            final PatternProcessor pp = new PatternProcessor("logs/app-%d{yyyy-MM-dd}{NOTVALID}.log.gz");
            final Instant initial = Instant.parse("2014-03-04T02:31:59Z");
            final long actual = pp.getNextTime(initial.toEpochMilli(), 1, false);

            final Instant expected = Instant.parse("2014-03-05T00:00:00Z");
            assertEquals(expected, Instant.ofEpochMilli(actual));
        } finally {
            TimeZone.setDefault(old);
        }
    }
}
