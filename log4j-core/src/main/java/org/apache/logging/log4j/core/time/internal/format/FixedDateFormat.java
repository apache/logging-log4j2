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

import org.apache.logging.log4j.core.time.Instant;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Custom time formatter that trades flexibility for performance. This formatter only supports the date patterns defined
 * in {@link FixedFormat}. For any other date patterns use {@link FastDateFormat}.
 * <p>
 * Related benchmarks: /log4j-perf/src/main/java/org/apache/logging/log4j/perf/jmh/TimeFormatBenchmark.java and
 * /log4j-perf/src/main/java/org/apache/logging/log4j/perf/jmh/ThreadsafeDateFormatBenchmark.java
 */
public class FixedDateFormat {

    /**
     * Enumeration over the supported date/time format patterns.
     * <p>
     * Package protected for unit tests.
     */
    public enum FixedFormat {
        /**
         * ABSOLUTE time format: {@code "HH:mm:ss,SSS"}.
         */
        ABSOLUTE("HH:mm:ss,SSS", null, 0, ':', 1, ',', 1, 3),
        /**
         * ABSOLUTE time format with microsecond precision: {@code "HH:mm:ss,nnnnnn"}.
         */
        ABSOLUTE_MICROS("HH:mm:ss,nnnnnn", null, 0, ':', 1, ',', 1, 6),
        /**
         * ABSOLUTE time format with nanosecond precision: {@code "HH:mm:ss,nnnnnnnnn"}.
         */
        ABSOLUTE_NANOS("HH:mm:ss,nnnnnnnnn", null, 0, ':', 1, ',', 1, 9),

        /**
         * ABSOLUTE time format variation with period separator: {@code "HH:mm:ss.SSS"}.
         */
        ABSOLUTE_PERIOD("HH:mm:ss.SSS", null, 0, ':', 1, '.', 1, 3),

        /**
         * COMPACT time format: {@code "yyyyMMddHHmmssSSS"}.
         */
        COMPACT("yyyyMMddHHmmssSSS", "yyyyMMdd", 0, ' ', 0, ' ', 0, 3),

        /**
         * DATE_AND_TIME time format: {@code "dd MMM yyyy HH:mm:ss,SSS"}.
         */
        DATE("dd MMM yyyy HH:mm:ss,SSS", "dd MMM yyyy ", 0, ':', 1, ',', 1, 3),

        /**
         * DATE_AND_TIME time format variation with period separator: {@code "dd MMM yyyy HH:mm:ss.SSS"}.
         */
        DATE_PERIOD("dd MMM yyyy HH:mm:ss.SSS", "dd MMM yyyy ", 0, ':', 1, '.', 1, 3),

        /**
         * DEFAULT time format: {@code "yyyy-MM-dd HH:mm:ss,SSS"}.
         */
        DEFAULT("yyyy-MM-dd HH:mm:ss,SSS", "yyyy-MM-dd ", 0, ':', 1, ',', 1, 3),
        /**
         * DEFAULT time format with microsecond precision: {@code "yyyy-MM-dd HH:mm:ss,nnnnnn"}.
         */
        DEFAULT_MICROS("yyyy-MM-dd HH:mm:ss,nnnnnn", "yyyy-MM-dd ", 0, ':', 1, ',', 1, 6),
        /**
         * DEFAULT time format with nanosecond precision: {@code "yyyy-MM-dd HH:mm:ss,nnnnnnnnn"}.
         */
        DEFAULT_NANOS("yyyy-MM-dd HH:mm:ss,nnnnnnnnn", "yyyy-MM-dd ", 0, ':', 1, ',', 1, 9),

        /**
         * DEFAULT time format variation with period separator: {@code "yyyy-MM-dd HH:mm:ss.SSS"}.
         */
        DEFAULT_PERIOD("yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd ", 0, ':', 1, '.', 1, 3),

        /**
         * ISO8601_BASIC time format: {@code "yyyyMMdd'T'HHmmss,SSS"}.
         */
        ISO8601_BASIC("yyyyMMdd'T'HHmmss,SSS", "yyyyMMdd'T'", 2, ' ', 0, ',', 1, 3),

        /**
         * ISO8601_BASIC time format: {@code "yyyyMMdd'T'HHmmss.SSS"}.
         */
        ISO8601_BASIC_PERIOD("yyyyMMdd'T'HHmmss.SSS", "yyyyMMdd'T'", 2, ' ', 0, '.', 1, 3),

        /**
         * ISO8601 time format: {@code "yyyy-MM-dd'T'HH:mm:ss,SSS"}.
         */
        ISO8601("yyyy-MM-dd'T'HH:mm:ss,SSS", "yyyy-MM-dd'T'", 2, ':', 1, ',', 1, 3),

        /**
         * ISO8601 time format: {@code "yyyy-MM-dd'T'HH:mm:ss.SSS"}.
         */
        ISO8601_PERIOD("yyyy-MM-dd'T'HH:mm:ss.SSS", "yyyy-MM-dd'T'", 2, ':', 1, '.', 1, 3);

        private static final String DEFAULT_SECOND_FRACTION_PATTERN = "SSS";
        private static final int MILLI_FRACTION_DIGITS = DEFAULT_SECOND_FRACTION_PATTERN.length();
        private static final char SECOND_FRACTION_PATTERN = 'n';

        private final String pattern;
        private final String datePattern;
        private final int escapeCount;
        private final char timeSeparatorChar;
        private final int timeSeparatorLength;
        private final char millisSeparatorChar;
        private final int millisSeparatorLength;
        private final int secondFractionDigits;

        FixedFormat(final String pattern, final String datePattern, final int escapeCount, final char timeSeparator,
                    final int timeSepLength, final char millisSeparator, final int millisSepLength,
                    final int secondFractionDigits) {
            this.timeSeparatorChar = timeSeparator;
            this.timeSeparatorLength = timeSepLength;
            this.millisSeparatorChar = millisSeparator;
            this.millisSeparatorLength = millisSepLength;
            this.pattern = Objects.requireNonNull(pattern);
            this.datePattern = datePattern; // may be null
            this.escapeCount = escapeCount;
            this.secondFractionDigits = secondFractionDigits;
        }

        /**
         * Returns the full pattern.
         *
         * @return the full pattern
         */
        public String getPattern() {
            return pattern;
        }

        /**
         * Returns the date part of the pattern.
         *
         * @return the date part of the pattern
         */
        public String getDatePattern() {
            return datePattern;
        }

        /**
         * Returns the FixedFormat with the name or pattern matching the specified string or {@code null} if not found.
         *
         * @param nameOrPattern the name or pattern to find a FixedFormat for
         * @return the FixedFormat with the name or pattern matching the specified string
         */
        public static FixedFormat lookup(final String nameOrPattern) {
            for (final FixedFormat type : FixedFormat.values()) {
                if (type.name().equals(nameOrPattern) || type.getPattern().equals(nameOrPattern)) {
                    return type;
                }
            }
            return null;
        }

        static FixedFormat lookupIgnoringNanos(final String pattern) {
            final int nanoStart = nanoStart(pattern);
            if (nanoStart > 0) {
                final String subPattern = pattern.substring(0, nanoStart) + DEFAULT_SECOND_FRACTION_PATTERN;
                for (final FixedFormat type : FixedFormat.values()) {
                    if (type.getPattern().equals(subPattern)) {
                        return type;
                    }
                }
            }
            return null;
        }

        private static int nanoStart(final String pattern) {
            final int index = pattern.indexOf(SECOND_FRACTION_PATTERN);
            if (index >= 0) {
                for (int i = index + 1; i < pattern.length(); i++) {
                    if (pattern.charAt(i) != SECOND_FRACTION_PATTERN) {
                        return -1;
                    }
                }
            }
            return index;
        }

        /**
         * Returns the length of the resulting formatted date and time strings.
         *
         * @return the length of the resulting formatted date and time strings
         */
        public int getLength() {
            return pattern.length() - escapeCount;
        }

        /**
         * Returns the length of the date part of the resulting formatted string.
         *
         * @return the length of the date part of the resulting formatted string
         */
        public int getDatePatternLength() {
            return getDatePattern() == null ? 0 : getDatePattern().length() - escapeCount;
        }

        /**
         * Returns the {@code FastDateFormat} object for formatting the date part of the pattern or {@code null} if the
         * pattern does not have a date part.
         *
         * @return the {@code FastDateFormat} object for formatting the date part of the pattern or {@code null}
         */
        public FastDateFormat getFastDateFormat() {
            return getFastDateFormat(null);
        }

        /**
         * Returns the {@code FastDateFormat} object for formatting the date part of the pattern or {@code null} if the
         * pattern does not have a date part.
         *
         * @param tz the time zone to use
         * @return the {@code FastDateFormat} object for formatting the date part of the pattern or {@code null}
         */
        public FastDateFormat getFastDateFormat(final TimeZone tz) {
            return getDatePattern() == null ? null : FastDateFormat.getInstance(getDatePattern(), tz);
        }

        /**
         * Returns the number of digits specifying the fraction of the second to show
         * @return 3 for millisecond precision, 6 for microsecond precision or 9 for nanosecond precision
         */
        public int getSecondFractionDigits() {
            return secondFractionDigits;
        }
    }

    private final FixedFormat fixedFormat;
    private final TimeZone timeZone;
    private final int length;
    private final int secondFractionDigits;
    private final FastDateFormat fastDateFormat; // may be null
    private final char timeSeparatorChar;
    private final char millisSeparatorChar;
    private final int timeSeparatorLength;
    private final int millisSeparatorLength;

    private volatile long midnightToday = 0;
    private volatile long midnightTomorrow = 0;
    private final int[] dstOffsets = new int[25];

    // cachedDate does not need to be volatile because
    // there is a write to a volatile field *after* cachedDate is modified,
    // and there is a read from a volatile field *before* cachedDate is read.
    // The Java memory model guarantees that because of the above,
    // changes to cachedDate in one thread are visible to other threads.
    // See http://g.oswego.edu/dl/jmm/cookbook.html
    private char[] cachedDate; // may be null
    private int dateLength;

    /**
     * Constructs a FixedDateFormat for the specified fixed format.
     * <p>
     * Package protected for unit tests.
     *
     * @param fixedFormat the fixed format
     * @param tz time zone
     */
    FixedDateFormat(final FixedFormat fixedFormat, final TimeZone tz) {
        this(fixedFormat, tz, fixedFormat.getSecondFractionDigits());
    }

    /**
     * Constructs a FixedDateFormat for the specified fixed format.
     * <p>
     * Package protected for unit tests.
     *
     * @param fixedFormat the fixed format
     * @param tz time zone
     * @param secondFractionDigits the number of digits specifying the fraction of the second to show
     */
    FixedDateFormat(final FixedFormat fixedFormat, final TimeZone tz, final int secondFractionDigits) {
        this.fixedFormat = Objects.requireNonNull(fixedFormat);
        this.timeZone = Objects.requireNonNull(tz);
        this.timeSeparatorChar = fixedFormat.timeSeparatorChar;
        this.timeSeparatorLength = fixedFormat.timeSeparatorLength;
        this.millisSeparatorChar = fixedFormat.millisSeparatorChar;
        this.millisSeparatorLength = fixedFormat.millisSeparatorLength;
        this.length = fixedFormat.getLength();
        this.secondFractionDigits = Math.max(1, Math.min(9, secondFractionDigits));
        this.fastDateFormat = fixedFormat.getFastDateFormat(tz);
    }

    public static FixedDateFormat createIfSupported(final String... options) {
        if (options == null || options.length == 0 || options[0] == null) {
            return new FixedDateFormat(FixedFormat.DEFAULT, TimeZone.getDefault());
        }
        final TimeZone tz;
        if (options.length > 1) {
            if (options[1] != null) {
                tz = TimeZone.getTimeZone(options[1]);
            } else {
                tz = TimeZone.getDefault();
            }
        } else {
            tz = TimeZone.getDefault();
        }

        final FixedFormat withNanos = FixedFormat.lookupIgnoringNanos(options[0]);
        if (withNanos != null) {
            final int secondFractionDigits = options[0].length() - FixedFormat.nanoStart(options[0]);
            return new FixedDateFormat(withNanos, tz, secondFractionDigits);
        }
        final FixedFormat type = FixedFormat.lookup(options[0]);
        return type == null ? null : new FixedDateFormat(type, tz);
    }

    /**
     * Returns a new {@code FixedDateFormat} object for the specified {@code FixedFormat} and a {@code TimeZone.getDefault()} TimeZone.
     *
     * @param format the format to use
     * @return a new {@code FixedDateFormat} object
     */
    public static FixedDateFormat create(final FixedFormat format) {
        return new FixedDateFormat(format, TimeZone.getDefault());
    }

    /**
     * Returns a new {@code FixedDateFormat} object for the specified {@code FixedFormat} and TimeZone.
     *
     * @param format the format to use
     * @param tz the time zone to use
     * @return a new {@code FixedDateFormat} object
     */
    public static FixedDateFormat create(final FixedFormat format, final TimeZone tz) {
        return new FixedDateFormat(format, tz != null ? tz : TimeZone.getDefault());
    }

    /**
     * Returns the full pattern of the selected fixed format.
     *
     * @return the full date-time pattern
     */
    public String getFormat() {
        return fixedFormat.getPattern();
    }

    /**
     * Returns the time zone.
     *
     * @return the time zone
     */

    public TimeZone getTimeZone() {
        return timeZone;
    }

    /**
     * <p>Returns the number of milliseconds since midnight in the time zone that this {@code FixedDateFormat}
     * was constructed with for the specified currentTime.</p>
     * <p>As a side effect, this method updates the cached formatted date and the cached date demarcation timestamps
     * when the specified current time is outside the previously set demarcation timestamps for the start or end
     * of the current day.</p>
     * @param currentTime the current time in millis since the epoch
     * @return the number of milliseconds since midnight for the specified time
     */
    // Profiling showed this method is important to log4j performance. Modify with care!
    // 30 bytes (allows immediate JVM inlining: <= -XX:MaxInlineSize=35 bytes)
    public long millisSinceMidnight(final long currentTime) {
        if (currentTime >= midnightTomorrow || currentTime < midnightToday) {
            updateMidnightMillis(currentTime);
        }
        return currentTime - midnightToday;
    }

    private void updateMidnightMillis(final long now) {
        if (now >= midnightTomorrow || now < midnightToday) {
            synchronized (this) {
                updateCachedDate(now);
                midnightToday = calcMidnightMillis(now, 0);
                midnightTomorrow = calcMidnightMillis(now, 1);

                updateDaylightSavingTime();
            }
        }
    }

    private long calcMidnightMillis(final long time, final int addDays) {
        final Calendar cal = Calendar.getInstance(timeZone);
        cal.setTimeInMillis(time);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DATE, addDays);
        return cal.getTimeInMillis();
    }

    private void updateDaylightSavingTime() {
        Arrays.fill(dstOffsets, 0);
        final int ONE_HOUR = (int) TimeUnit.HOURS.toMillis(1);
        if (timeZone.getOffset(midnightToday) != timeZone.getOffset(midnightToday + 23 * ONE_HOUR)) {
            for (int i = 0; i < dstOffsets.length; i++) {
                final long time = midnightToday + i * ONE_HOUR;
                dstOffsets[i] = timeZone.getOffset(time) - timeZone.getRawOffset();
            }
            if (dstOffsets[0] > dstOffsets[23]) { // clock is moved backwards.
                // we obtain midnightTonight with Calendar.getInstance(TimeZone), so it already includes raw offset
                for (int i = dstOffsets.length - 1; i >= 0; i--) {
                    dstOffsets[i] -= dstOffsets[0]; //
                }
            }
        }
    }

    private void updateCachedDate(final long now) {
        if (fastDateFormat != null) {
            final StringBuilder result = fastDateFormat.format(now, new StringBuilder());
            cachedDate = result.toString().toCharArray();
            dateLength = result.length();
        }
    }

    public String formatInstant(final Instant instant) {
        final char[] result = new char[length << 1]; // double size for locales with lengthy DateFormatSymbols
        final int written = formatInstant(instant, result, 0);
        return new String(result, 0, written);
    }

    public int formatInstant(final Instant instant, final char[] buffer, final int startPos) {
        int result = format(instant.getEpochMillisecond(), buffer, startPos);
        result -= digitsLessThanThree();
        formatNanoOfMillisecond(instant.getNanoOfMillisecond(), buffer, startPos + result);
        return result + digitsMorePreciseThanMillis();
    }

    private int digitsLessThanThree() { // in case user specified only 1 or 2 'n' format characters
        return Math.max(0, FixedFormat.MILLI_FRACTION_DIGITS - secondFractionDigits);
    }

    private int digitsMorePreciseThanMillis() {
        return Math.max(0, secondFractionDigits - FixedFormat.MILLI_FRACTION_DIGITS);
    }

    // Profiling showed this method is important to log4j performance. Modify with care!
    // 28 bytes (allows immediate JVM inlining: <= -XX:MaxInlineSize=35 bytes)
    public String format(final long epochMillis) {
        final char[] result = new char[length << 1]; // double size for locales with lengthy DateFormatSymbols
        final int written = format(epochMillis, result, 0);
        return new String(result, 0, written);
    }

    // Profiling showed this method is important to log4j performance. Modify with care!
    // 31 bytes (allows immediate JVM inlining: <= -XX:MaxInlineSize=35 bytes)
    public int format(final long epochMillis, final char[] buffer, final int startPos) {
        // Calculate values by getting the ms values first and do then
        // calculate the hour minute and second values divisions.

        // Get daytime in ms: this does fit into an int
        // int ms = (int) (time % 86400000);
        final int ms = (int) (millisSinceMidnight(epochMillis));
        writeDate(buffer, startPos);
        return writeTime(ms, buffer, startPos + dateLength) - startPos;
    }

    // Profiling showed this method is important to log4j performance. Modify with care!
    // 22 bytes (allows immediate JVM inlining: <= -XX:MaxInlineSize=35 bytes)
    private void writeDate(final char[] buffer, final int startPos) {
        if (cachedDate != null) {
            System.arraycopy(cachedDate, 0, buffer, startPos, dateLength);
        }
    }

    // Profiling showed this method is important to log4j performance. Modify with care!
    // 262 bytes (will be inlined when hot enough: <= -XX:FreqInlineSize=325 bytes on Linux)
    private int writeTime(int ms, final char[] buffer, int pos) {
        final int hourOfDay = ms / 3600000;
        final int hours = hourOfDay + daylightSavingTime(hourOfDay) / 3600000;
        ms -= 3600000 * hourOfDay;

        final int minutes = ms / 60000;
        ms -= 60000 * minutes;

        final int seconds = ms / 1000;
        ms -= 1000 * seconds;

        // Hour
        int temp = hours / 10;
        buffer[pos++] = ((char) (temp + '0'));

        // Do subtract to get remainder instead of doing % 10
        buffer[pos++] = ((char) (hours - 10 * temp + '0'));
        buffer[pos] = timeSeparatorChar;
        pos += timeSeparatorLength;

        // Minute
        temp = minutes / 10;
        buffer[pos++] = ((char) (temp + '0'));

        // Do subtract to get remainder instead of doing % 10
        buffer[pos++] = ((char) (minutes - 10 * temp + '0'));
        buffer[pos] = timeSeparatorChar;
        pos += timeSeparatorLength;

        // Second
        temp = seconds / 10;
        buffer[pos++] = ((char) (temp + '0'));
        buffer[pos++] = ((char) (seconds - 10 * temp + '0'));
        buffer[pos] = millisSeparatorChar;
        pos += millisSeparatorLength;

        // Millisecond
        temp = ms / 100;
        buffer[pos++] = ((char) (temp + '0'));

        ms -= 100 * temp;
        temp = ms / 10;
        buffer[pos++] = ((char) (temp + '0'));

        ms -= 10 * temp;
        buffer[pos++] = ((char) (ms + '0'));
        return pos;
    }

    static int[] TABLE = {
            100000, // 0
            10000, // 1
            1000, // 2
            100, // 3
            10, // 4
            1, // 5
    };

    private void formatNanoOfMillisecond(int nanoOfMillisecond, final char[] buffer, int pos) {
        int temp;
        int remain = nanoOfMillisecond;
        for (int i = 0; i < secondFractionDigits - FixedFormat.MILLI_FRACTION_DIGITS; i++) {
            int divisor = TABLE[i];
            temp = remain / divisor;
            buffer[pos++] = ((char) (temp + '0'));
            remain -= divisor * temp; // equivalent of remain % 10
        }
    }

    private int daylightSavingTime(final int hourOfDay) {
        return hourOfDay > 23 ? dstOffsets[23] : dstOffsets[hourOfDay];
    }
}
