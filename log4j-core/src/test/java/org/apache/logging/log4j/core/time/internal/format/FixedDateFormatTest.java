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

package org.apache.logging.log4j.core.time.internal.format;

import static org.apache.logging.log4j.core.time.internal.format.FixedDateFormat.FixedFormat.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.time.internal.format.FixedDateFormat;
import org.apache.logging.log4j.core.time.internal.format.FixedDateFormat.FixedFormat;
import org.junit.Test;

/**
 * Tests {@link FixedDateFormat}.
 */
public class FixedDateFormatTest {

    private boolean containsNanos(FixedFormat fixedFormat) {
        final String pattern = fixedFormat.getPattern();
        return pattern.endsWith("n") || pattern.matches(".+n+X*") || pattern.matches(".+n+Z*");
    }

    @Test
    public void testFixedFormat_getDatePatternNullIfNoDateInPattern() {
        assertThat(FixedFormat.ABSOLUTE.getDatePattern()).isNull();
        assertThat(FixedFormat.ABSOLUTE_PERIOD.getDatePattern()).isNull();
    }

    @Test
    public void testFixedFormat_getDatePatternLengthZeroIfNoDateInPattern() {
        assertThat(FixedFormat.ABSOLUTE.getDatePatternLength()).isEqualTo(0);
        assertThat(FixedFormat.ABSOLUTE_PERIOD.getDatePatternLength()).isEqualTo(0);
    }

    @Test
    public void testFixedFormat_getFastDateFormatNullIfNoDateInPattern() {
        assertThat(FixedFormat.ABSOLUTE.getFastDateFormat()).isNull();
        assertThat(FixedFormat.ABSOLUTE_PERIOD.getFastDateFormat()).isNull();
    }

    @Test
    public void testFixedFormat_getDatePatternReturnsDatePatternIfExists() {
        assertThat(FixedFormat.COMPACT.getDatePattern()).isEqualTo("yyyyMMdd");
        assertThat(DEFAULT.getDatePattern()).isEqualTo("yyyy-MM-dd ");
    }

    @Test
    public void testFixedFormat_getDatePatternLengthReturnsDatePatternLength() {
        assertThat(FixedFormat.COMPACT.getDatePatternLength()).isEqualTo("yyyyMMdd".length());
        assertThat(DEFAULT.getDatePatternLength()).isEqualTo("yyyy-MM-dd ".length());
    }

    @Test
    public void testFixedFormat_getFastDateFormatNonNullIfDateInPattern() {
        assertThat(FixedFormat.COMPACT.getFastDateFormat()).isNotNull();
        assertThat(DEFAULT.getFastDateFormat()).isNotNull();
        assertThat(FixedFormat.COMPACT.getFastDateFormat().getPattern()).isEqualTo("yyyyMMdd");
        assertThat(DEFAULT.getFastDateFormat().getPattern()).isEqualTo("yyyy-MM-dd ");
    }

    @Test
    public void testCreateIfSupported_nonNullIfNameMatches() {
        for (final FixedDateFormat.FixedFormat format : FixedDateFormat.FixedFormat.values()) {
            final String[] options = {format.name()};
            assertThat(FixedDateFormat.createIfSupported(options)).describedAs(format.name()).isNotNull();
        }
    }

    @Test
    public void testCreateIfSupported_nonNullIfPatternMatches() {
        for (final FixedDateFormat.FixedFormat format : FixedDateFormat.FixedFormat.values()) {
            final String[] options = {format.getPattern()};
            assertThat(FixedDateFormat.createIfSupported(options)).describedAs(format.name()).isNotNull();
        }
    }

    @Test
    public void testCreateIfSupported_nullIfNameDoesNotMatch() {
        final String[] options = {"DEFAULT3"};
        assertThat(FixedDateFormat.createIfSupported(options)).describedAs("DEFAULT3").isNull();
    }

    @Test
    public void testCreateIfSupported_nullIfPatternDoesNotMatch() {
        final String[] options = {"y M d H m s"};
        assertThat(FixedDateFormat.createIfSupported(options)).describedAs("y M d H m s").isNull();
    }

    @Test
    public void testCreateIfSupported_defaultIfOptionsArrayNull() {
        final FixedDateFormat fmt = FixedDateFormat.createIfSupported((String[]) null);
        assertThat(fmt.getFormat()).isEqualTo(DEFAULT.getPattern());
    }

    @Test
    public void testCreateIfSupported_defaultIfOptionsArrayEmpty() {
        final FixedDateFormat fmt = FixedDateFormat.createIfSupported(new String[0]);
        assertThat(fmt.getFormat()).isEqualTo(DEFAULT.getPattern());
    }

    @Test
    public void testCreateIfSupported_defaultIfOptionsArrayWithSingleNullElement() {
        final FixedDateFormat fmt = FixedDateFormat.createIfSupported(new String[1]);
        assertThat(fmt.getFormat()).isEqualTo(DEFAULT.getPattern());
        assertThat(fmt.getTimeZone()).isEqualTo(TimeZone.getDefault());
    }

    @Test
    public void testCreateIfSupported_defaultTimeZoneIfOptionsArrayWithSecondNullElement() {
        final FixedDateFormat fmt = FixedDateFormat.createIfSupported(new String[] {DEFAULT.getPattern(), null, ""});
        assertThat(fmt.getFormat()).isEqualTo(DEFAULT.getPattern());
        assertThat(fmt.getTimeZone()).isEqualTo(TimeZone.getDefault());
    }

    @Test
    public void testCreateIfSupported_customTimeZoneIfOptionsArrayWithTimeZoneElement() {
        final FixedDateFormat fmt = FixedDateFormat.createIfSupported(new String[] {DEFAULT.getPattern(), "+08:00", ""});
        assertThat(fmt.getFormat()).isEqualTo(DEFAULT.getPattern());
        assertThat(fmt.getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+08:00"));
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
        assertThat(format.getFormat()).isSameAs(FixedDateFormat.FixedFormat.ABSOLUTE.getPattern());
    }

    @Test
    public void testFormatLong() {
        final long now = System.currentTimeMillis();
        final long start = now - TimeUnit.HOURS.toMillis(25);
        final long end = now + TimeUnit.HOURS.toMillis(25);
        for (final FixedFormat format : FixedFormat.values()) {
            String pattern = format.getPattern();
            if (containsNanos(format) || format.getTimeZoneFormat() != null) {
                 continue; // cannot compile precise timestamp formats with SimpleDateFormat
            }
            final SimpleDateFormat simpleDF = new SimpleDateFormat(pattern, Locale.getDefault());
            final FixedDateFormat customTF = new FixedDateFormat(format, TimeZone.getDefault());
            for (long time = start; time < end; time += 12345) {
                final String actual = customTF.format(time);
                final String expected = simpleDF.format(new Date(time));
                assertThat(actual).describedAs(format + "(" + pattern + ")" + "/" + time).isEqualTo(expected);
            }
        }
    }

    @Test
    public void testFormatLong_goingBackInTime() {
        final long now = System.currentTimeMillis();
        final long start = now - TimeUnit.HOURS.toMillis(25);
        final long end = now + TimeUnit.HOURS.toMillis(25);
        for (final FixedFormat format : FixedFormat.values()) {
            String pattern = format.getPattern();
            if (containsNanos(format) || format.getTimeZoneFormat() != null) {
                 continue; // cannot compile precise timestamp formats with SimpleDateFormat
            }
            final SimpleDateFormat simpleDF = new SimpleDateFormat(pattern, Locale.getDefault());
            final FixedDateFormat customTF = new FixedDateFormat(format, TimeZone.getDefault());
            for (long time = end; time > start; time -= 12345) {
                final String actual = customTF.format(time);
                final String expected = simpleDF.format(new Date(time));
                assertThat(actual).describedAs(format + "(" + pattern + ")" + "/" + time).isEqualTo(expected);
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
            String pattern = format.getPattern();
            if (containsNanos(format) || format.getTimeZoneFormat() != null) {
                 continue; // cannot compile precise timestamp formats with SimpleDateFormat
            }
            final SimpleDateFormat simpleDF = new SimpleDateFormat(pattern, Locale.getDefault());
            final FixedDateFormat customTF = new FixedDateFormat(format, TimeZone.getDefault());
            for (long time = start; time < end; time += 12345) {
                final int length = customTF.format(time, buffer, 23);
                final String actual = new String(buffer, 23, length);
                final String expected = simpleDF.format(new Date(time));
                assertThat(actual).describedAs(format + "(" + pattern + ")" + "/" + time).isEqualTo(expected);
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
            String pattern = format.getPattern();
            if (containsNanos(format) || format.getTimeZoneFormat() != null) {
                 continue; // cannot compile precise timestamp formats with SimpleDateFormat
            }
            final SimpleDateFormat simpleDF = new SimpleDateFormat(pattern, Locale.getDefault());
            final FixedDateFormat customTF = new FixedDateFormat(format, TimeZone.getDefault());
            for (long time = end; time > start; time -= 12345) {
                final int length = customTF.format(time, buffer, 23);
                final String actual = new String(buffer, 23, length);
                final String expected = simpleDF.format(new Date(time));
                assertThat(actual).describedAs(format + "(" + pattern + ")" + "/" + time).isEqualTo(expected);
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

        final TimeZone tz = TimeZone.getTimeZone("US/Central");
        for (int i = 0; i < 36; i++) {
            final Date date = calendar.getTime();
            assertThat(usCentral.format(date)).describedAs("SimpleDateFormat TZ=US Central").isEqualTo(expectedDstAndNoDst[i][0]);
            assertThat(utc.format(date)).describedAs("SimpleDateFormat TZ=UTC").isEqualTo(expectedDstAndNoDst[i][1]);
            assertThat(fixedUsCentral.format(date.getTime())).describedAs("FixedDateFormat TZ=US Central").isEqualTo(expectedDstAndNoDst[i][0]);
            assertThat(fixedUtc.format(date.getTime())).describedAs("FixedDateFormat TZ=UTC").isEqualTo(expectedDstAndNoDst[i][1]);
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

        final TimeZone tz = TimeZone.getTimeZone("US/Central");
        for (int i = 0; i < 36; i++) {
            final Date date = calendar.getTime();
            //System.out.println(usCentral.format(date) + ", Fixed: " + fixedUsCentral.format(date.getTime()) + ", utc: " + utc.format(date));
            assertThat(usCentral.format(date)).describedAs("SimpleDateFormat TZ=US Central").isEqualTo(expectedDstAndNoDst[i][0]);
            assertThat(utc.format(date)).describedAs("SimpleDateFormat TZ=UTC").isEqualTo(expectedDstAndNoDst[i][1]);
            assertThat(fixedUsCentral.format(date.getTime())).describedAs("FixedDateFormat TZ=US Central").isEqualTo(expectedDstAndNoDst[i][0]);
            assertThat(fixedUtc.format(date.getTime())).describedAs("FixedDateFormat TZ=UTC").isEqualTo(expectedDstAndNoDst[i][1]);
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
            String pattern = format.getPattern();
            if (containsNanos(format) || format.getTimeZoneFormat() != null) {
                 continue; // cannot compile precise timestamp formats with SimpleDateFormat
            }
            final SimpleDateFormat simpleDF = new SimpleDateFormat(pattern, Locale.getDefault());
            final FixedDateFormat customTF = new FixedDateFormat(format, TimeZone.getDefault());
            for (long time = end; time > start; time -= 12345) {
                final String actual = customTF.format(time);
                final String expected = simpleDF.format(new Date(time));
                assertThat(actual).describedAs(format + "(" + pattern + ")" + "/" + time).isEqualTo(expected);
            }
        }
    }
}
