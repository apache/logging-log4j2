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
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.util.datetime.FixedDateFormat.FixedFormat;
import org.junit.Test;

import static org.apache.logging.log4j.core.util.datetime.FixedDateFormat.FixedFormat.*;
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
        assertEquals("yyyy-MM-dd ", DEFAULT.getDatePattern());
    }

    @Test
    public void testFixedFormat_getDatePatternLengthReturnsDatePatternLength() {
        assertEquals("yyyyMMdd".length(), FixedFormat.COMPACT.getDatePatternLength());
        assertEquals("yyyy-MM-dd ".length(), DEFAULT.getDatePatternLength());
    }

    @Test
    public void testFixedFormat_getFastDateFormatNonNullIfDateInPattern() {
        assertNotNull(FixedFormat.COMPACT.getFastDateFormat());
        assertNotNull(DEFAULT.getFastDateFormat());
        assertEquals("yyyyMMdd", FixedFormat.COMPACT.getFastDateFormat().getPattern());
        assertEquals("yyyy-MM-dd ", DEFAULT.getFastDateFormat().getPattern());
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
        assertEquals(DEFAULT.getPattern(), fmt.getFormat());
    }

    @Test
    public void testCreateIfSupported_defaultIfOptionsArrayEmpty() {
        final FixedDateFormat fmt = FixedDateFormat.createIfSupported(new String[0]);
        assertEquals(DEFAULT.getPattern(), fmt.getFormat());
    }

    @Test
    public void testCreateIfSupported_defaultIfOptionsArrayWithSingleNullElement() {
        final FixedDateFormat fmt = FixedDateFormat.createIfSupported(new String[1]);
        assertEquals(DEFAULT.getPattern(), fmt.getFormat());
        assertEquals(TimeZone.getDefault(), fmt.getTimeZone());
    }

    @Test
    public void testCreateIfSupported_defaultTimeZoneIfOptionsArrayWithSecondNullElement() {
        final FixedDateFormat fmt = FixedDateFormat.createIfSupported(new String[] {DEFAULT.getPattern(), null, ""});
        assertEquals(DEFAULT.getPattern(), fmt.getFormat());
        assertEquals(TimeZone.getDefault(), fmt.getTimeZone());
    }

    @Test
    public void testCreateIfSupported_customTimeZoneIfOptionsArrayWithTimeZoneElement() {
        final FixedDateFormat fmt = FixedDateFormat.createIfSupported(new String[] {DEFAULT.getPattern(), "+08:00", ""});
        assertEquals(DEFAULT.getPattern(), fmt.getFormat());
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

    @Test
    public void testDaylightSavingToSummerTime() throws Exception {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").parse("2017-03-12 00:00:00 UTC"));

        final SimpleDateFormat usCentral = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS", Locale.US);
        usCentral.setTimeZone(TimeZone.getTimeZone("US/Central"));

        final SimpleDateFormat utc = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS", Locale.US);
        utc.setTimeZone(TimeZone.getTimeZone("UTC"));

        final FixedDateFormat fixedUsCentral = FixedDateFormat.create(DEFAULT, TimeZone.getTimeZone("US/Central"));
        final FixedDateFormat fixedUtc = FixedDateFormat.create(DEFAULT, TimeZone.getTimeZone("UTC"));

        final String[][] expectedDstAndNoDst = {
                // US/Central, UTC
                { "2017-03-11 18:00:00,000", "2017-03-12 00:00:00,000" }, //
                { "2017-03-11 19:00:00,000", "2017-03-12 01:00:00,000" }, //
                { "2017-03-11 20:00:00,000", "2017-03-12 02:00:00,000" }, //
                { "2017-03-11 21:00:00,000", "2017-03-12 03:00:00,000" }, //
                { "2017-03-11 22:00:00,000", "2017-03-12 04:00:00,000" }, //
                { "2017-03-11 23:00:00,000", "2017-03-12 05:00:00,000" }, //
                { "2017-03-12 00:00:00,000", "2017-03-12 06:00:00,000" }, //
                { "2017-03-12 01:00:00,000", "2017-03-12 07:00:00,000" }, //
                { "2017-03-12 03:00:00,000", "2017-03-12 08:00:00,000" }, //  DST jump at 2am US central time
                { "2017-03-12 04:00:00,000", "2017-03-12 09:00:00,000" }, //
                { "2017-03-12 05:00:00,000", "2017-03-12 10:00:00,000" }, //
                { "2017-03-12 06:00:00,000", "2017-03-12 11:00:00,000" }, //
                { "2017-03-12 07:00:00,000", "2017-03-12 12:00:00,000" }, //
                { "2017-03-12 08:00:00,000", "2017-03-12 13:00:00,000" }, //
                { "2017-03-12 09:00:00,000", "2017-03-12 14:00:00,000" }, //
                { "2017-03-12 10:00:00,000", "2017-03-12 15:00:00,000" }, //
                { "2017-03-12 11:00:00,000", "2017-03-12 16:00:00,000" }, //
                { "2017-03-12 12:00:00,000", "2017-03-12 17:00:00,000" }, //
                { "2017-03-12 13:00:00,000", "2017-03-12 18:00:00,000" }, //
                { "2017-03-12 14:00:00,000", "2017-03-12 19:00:00,000" }, //
                { "2017-03-12 15:00:00,000", "2017-03-12 20:00:00,000" }, //
                { "2017-03-12 16:00:00,000", "2017-03-12 21:00:00,000" }, //
                { "2017-03-12 17:00:00,000", "2017-03-12 22:00:00,000" }, //
                { "2017-03-12 18:00:00,000", "2017-03-12 23:00:00,000" }, // 24
                { "2017-03-12 19:00:00,000", "2017-03-13 00:00:00,000" }, //
                { "2017-03-12 20:00:00,000", "2017-03-13 01:00:00,000" }, //
                { "2017-03-12 21:00:00,000", "2017-03-13 02:00:00,000" }, //
                { "2017-03-12 22:00:00,000", "2017-03-13 03:00:00,000" }, //
                { "2017-03-12 23:00:00,000", "2017-03-13 04:00:00,000" }, //
                { "2017-03-13 00:00:00,000", "2017-03-13 05:00:00,000" }, //
                { "2017-03-13 01:00:00,000", "2017-03-13 06:00:00,000" }, //
                { "2017-03-13 02:00:00,000", "2017-03-13 07:00:00,000" }, //
                { "2017-03-13 03:00:00,000", "2017-03-13 08:00:00,000" }, //
                { "2017-03-13 04:00:00,000", "2017-03-13 09:00:00,000" }, //
                { "2017-03-13 05:00:00,000", "2017-03-13 10:00:00,000" }, //
                { "2017-03-13 06:00:00,000", "2017-03-13 11:00:00,000" }, //
        };

        TimeZone tz = TimeZone.getTimeZone("US/Central");
        for (int i = 0; i < 36; i++) {
            final Date date = calendar.getTime();
            assertEquals("SimpleDateFormat TZ=US Central", expectedDstAndNoDst[i][0], usCentral.format(date));
            assertEquals("SimpleDateFormat TZ=UTC", expectedDstAndNoDst[i][1], utc.format(date));
            assertEquals("FixedDateFormat TZ=US Central", expectedDstAndNoDst[i][0], fixedUsCentral.format(date.getTime()));
            assertEquals("FixedDateFormat TZ=UTC", expectedDstAndNoDst[i][1], fixedUtc.format(date.getTime()));
            calendar.add(Calendar.HOUR_OF_DAY, 1);
        }
    }

    @Test
    public void testDaylightSavingToWinterTime() throws Exception {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").parse("2017-11-05 00:00:00 UTC"));

        final SimpleDateFormat usCentral = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS", Locale.US);
        usCentral.setTimeZone(TimeZone.getTimeZone("US/Central"));

        final SimpleDateFormat utc = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS", Locale.US);
        utc.setTimeZone(TimeZone.getTimeZone("UTC"));

        final FixedDateFormat fixedUsCentral = FixedDateFormat.create(DEFAULT, TimeZone.getTimeZone("US/Central"));
        final FixedDateFormat fixedUtc = FixedDateFormat.create(DEFAULT, TimeZone.getTimeZone("UTC"));

        final String[][] expectedDstAndNoDst = {
                // US/Central, UTC
                { "2017-11-04 19:00:00,000", "2017-11-05 00:00:00,000" }, //
                { "2017-11-04 20:00:00,000", "2017-11-05 01:00:00,000" }, //
                { "2017-11-04 21:00:00,000", "2017-11-05 02:00:00,000" }, //
                { "2017-11-04 22:00:00,000", "2017-11-05 03:00:00,000" }, //
                { "2017-11-04 23:00:00,000", "2017-11-05 04:00:00,000" }, //
                { "2017-11-05 00:00:00,000", "2017-11-05 05:00:00,000" }, //
                { "2017-11-05 01:00:00,000", "2017-11-05 06:00:00,000" }, //  DST jump at 2am US central time
                { "2017-11-05 01:00:00,000", "2017-11-05 07:00:00,000" }, //
                { "2017-11-05 02:00:00,000", "2017-11-05 08:00:00,000" }, //
                { "2017-11-05 03:00:00,000", "2017-11-05 09:00:00,000" }, //
                { "2017-11-05 04:00:00,000", "2017-11-05 10:00:00,000" }, //
                { "2017-11-05 05:00:00,000", "2017-11-05 11:00:00,000" }, //
                { "2017-11-05 06:00:00,000", "2017-11-05 12:00:00,000" }, //
                { "2017-11-05 07:00:00,000", "2017-11-05 13:00:00,000" }, //
                { "2017-11-05 08:00:00,000", "2017-11-05 14:00:00,000" }, //
                { "2017-11-05 09:00:00,000", "2017-11-05 15:00:00,000" }, //
                { "2017-11-05 10:00:00,000", "2017-11-05 16:00:00,000" }, //
                { "2017-11-05 11:00:00,000", "2017-11-05 17:00:00,000" }, //
                { "2017-11-05 12:00:00,000", "2017-11-05 18:00:00,000" }, //
                { "2017-11-05 13:00:00,000", "2017-11-05 19:00:00,000" }, //
                { "2017-11-05 14:00:00,000", "2017-11-05 20:00:00,000" }, //
                { "2017-11-05 15:00:00,000", "2017-11-05 21:00:00,000" }, //
                { "2017-11-05 16:00:00,000", "2017-11-05 22:00:00,000" }, //
                { "2017-11-05 17:00:00,000", "2017-11-05 23:00:00,000" }, // 24
                { "2017-11-05 18:00:00,000", "2017-11-06 00:00:00,000" }, //
                { "2017-11-05 19:00:00,000", "2017-11-06 01:00:00,000" }, //
                { "2017-11-05 20:00:00,000", "2017-11-06 02:00:00,000" }, //
                { "2017-11-05 21:00:00,000", "2017-11-06 03:00:00,000" }, //
                { "2017-11-05 22:00:00,000", "2017-11-06 04:00:00,000" }, //
                { "2017-11-05 23:00:00,000", "2017-11-06 05:00:00,000" }, //
                { "2017-11-06 00:00:00,000", "2017-11-06 06:00:00,000" }, //
                { "2017-11-06 01:00:00,000", "2017-11-06 07:00:00,000" }, //
                { "2017-11-06 02:00:00,000", "2017-11-06 08:00:00,000" }, //
                { "2017-11-06 03:00:00,000", "2017-11-06 09:00:00,000" }, //
                { "2017-11-06 04:00:00,000", "2017-11-06 10:00:00,000" }, //
                { "2017-11-06 05:00:00,000", "2017-11-06 11:00:00,000" }, //
        };

        TimeZone tz = TimeZone.getTimeZone("US/Central");
        for (int i = 0; i < 36; i++) {
            final Date date = calendar.getTime();
            //System.out.println(usCentral.format(date) + ", Fixed: " + fixedUsCentral.format(date.getTime()) + ", utc: " + utc.format(date));
            assertEquals("SimpleDateFormat TZ=US Central", expectedDstAndNoDst[i][0], usCentral.format(date));
            assertEquals("SimpleDateFormat TZ=UTC", expectedDstAndNoDst[i][1], utc.format(date));
            assertEquals("FixedDateFormat TZ=US Central", expectedDstAndNoDst[i][0], fixedUsCentral.format(date.getTime()));
            assertEquals("FixedDateFormat TZ=UTC", expectedDstAndNoDst[i][1], fixedUtc.format(date.getTime()));
            calendar.add(Calendar.HOUR_OF_DAY, 1);
        }
    }
    /**
     * This test case validates date pattern before and after DST
     * Base Date : 12 Mar 2017
     * Daylight Savings started on : 02:00 AM
     */
    @Test
    public void testFormatLong_goingBackInTime_DST() {
        final Calendar instance = Calendar.getInstance(TimeZone.getTimeZone("EST"));
        instance.set(2017, 2, 12, 2, 0);
        final long now = instance.getTimeInMillis();
        final long start = now - TimeUnit.HOURS.toMillis(1);
        final long end = now + TimeUnit.HOURS.toMillis(1);

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
}
