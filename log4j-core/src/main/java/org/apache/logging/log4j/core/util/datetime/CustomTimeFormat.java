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

import java.util.Calendar;
import java.util.Objects;

/**
 * Custom time formatter that trades flexibility for performance.
 */
public class CustomTimeFormat {
    /**
     * Enumeration over the supported date/time format patterns.
     * <p>
     * Package protected for unit tests.
     */
    static enum FixedFormat {
        /**
         * ABSOLUTE time format: {@code "HH:mm:ss,SSS"}.
         */
        ABSOLUTE("HH:mm:ss,SSS", null, 0, ':', 1, ',', 1),

        /**
         * ABSOLUTE time format variation with period separator: {@code "HH:mm:ss.SSS"}.
         */
        ABSOLUTE2("HH:mm:ss.SSS", null, 0, ':', 1, '.', 1),

        /**
         * COMPACT time format: {@code "yyyyMMddHHmmssSSS"}.
         */
        COMPACT("yyyyMMddHHmmssSSS", "yyyyMMdd", 0, ' ', 0, ' ', 0),

        /**
         * DATE time format: {@code "dd MMM yyyy HH:mm:ss,SSS"}.
         */
        DATE("dd MMM yyyy HH:mm:ss,SSS", "dd MMM yyyy ", 0, ':', 1, ',', 1),

        /**
         * DATE time format variation with period separator: {@code "dd MMM yyyy HH:mm:ss.SSS"}.
         */
        DATE2("dd MMM yyyy HH:mm:ss.SSS", "dd MMM yyyy ", 0, ':', 1, '.', 1),

        /**
         * DEFAULT time format: {@code "yyyy-MM-dd HH:mm:ss,SSS"}.
         */
        DEFAULT("yyyy-MM-dd HH:mm:ss,SSS", "yyyy-MM-dd ", 0, ':', 1, ',', 1),

        /**
         * DEFAULT time format variation with period separator: {@code "yyyy-MM-dd HH:mm:ss.SSS"}.
         */
        DEFAULT2("yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd ", 0, ':', 1, '.', 1),

        /**
         * ISO8601_BASIC time format: {@code "yyyyMMdd'T'HHmmss,SSS"}.
         */
        ISO8601_BASIC("yyyyMMdd'T'HHmmss,SSS", "yyyyMMdd'T'", 2, ' ', 0, ',', 1),

        /**
         * ISO8601 time format: {@code "yyyy-MM-dd'T'HH:mm:ss,SSS"}.
         */
        ISO8601("yyyy-MM-dd'T'HH:mm:ss,SSS", "yyyy-MM-dd'T'", 2, ':', 1, ',', 1), ;

        private final String pattern;
        private final String datePattern;
        private final int escapeCount;
        private final char timeSeparatorChar;
        private final int timeSeparatorLength;
        private final char millisSeparatorChar;
        private final int millisSeparatorLength;

        private FixedFormat(final String pattern, final String datePattern, final int escapeCount,
                final char timeSeparator, final int timeSepLength, final char millisSeparator, final int millisSepLength) {
            this.timeSeparatorChar = timeSeparator;
            this.timeSeparatorLength = timeSepLength;
            this.millisSeparatorChar = millisSeparator;
            this.millisSeparatorLength = millisSepLength;
            this.pattern = Objects.requireNonNull(pattern);
            this.datePattern = datePattern; // may be null
            this.escapeCount = escapeCount;
        }

        public String getPattern() {
            return pattern;
        }

        public String getDatePattern() {
            return datePattern;
        }

        /**
         * Returns the FixedFormat with the name or pattern matching the specified string or {@code null} if not found.
         */
        public static FixedFormat lookup(final String nameOrPattern) {
            for (final FixedFormat type : FixedFormat.values()) {
                if (type.name().equals(nameOrPattern) || type.getPattern().equals(nameOrPattern)) {
                    return type;
                }
            }
            return null;
        }

        public int getLength() {
            return pattern.length() - escapeCount;
        }

        public int getDatePatternLength() {
            return getDatePattern() == null ? 0 : getDatePattern().length() - escapeCount;
        }

        public FastDateFormat getFastDateFormat() {
            return getDatePattern() == null ? null : FastDateFormat.getInstance(getDatePattern());
        }
    }

    public static CustomTimeFormat createIfSupported(final String... options) {
        if (options == null || options.length == 0 || options.length > 1) {
            return null; // time zone not supported
        }
        final FixedFormat type = FixedFormat.lookup(options[0]);
        return type == null ? null : new CustomTimeFormat(type);
    }

    private final FixedFormat type;
    private final int length;
    private final int dateLength;
    private final FastDateFormat fastDateFormat; // may be null
    private final char timeSeparatorChar;
    private final char millisSeparatorChar;
    private final int timeSeparatorLength;
    private final int millisSeparatorLength;

    private volatile long midnightToday = 0;
    private volatile long midnightTomorrow = 0;
    // cachedDate does not need to be volatile because
    // there is a write to a volatile field *after* cachedDate is modified,
    // and there is a read from a volatile field *before* cachedDate is read.
    // The Java memory model guarantees that because of the above,
    // changes to cachedDate in one thread are visible to other threads.
    // See http://g.oswego.edu/dl/jmm/cookbook.html
    private char[] cachedDate; // may be null

    /**
     * Constructs a CustomTimeFormat for the specified fixed format.
     * <p>
     * Package protected for unit tests.
     * 
     * @param type the fixed format
     */
    CustomTimeFormat(final FixedFormat type) {
        this.type = Objects.requireNonNull(type);
        this.timeSeparatorChar = type.timeSeparatorChar;
        this.timeSeparatorLength = type.timeSeparatorLength;
        this.millisSeparatorChar = type.millisSeparatorChar;
        this.millisSeparatorLength = type.millisSeparatorLength;
        this.length = type.getLength();
        this.dateLength = type.getDatePatternLength();
        this.fastDateFormat = type.getFastDateFormat();
    }

    public String getFormat() {
        return type.getPattern();
    }

    // Profiling showed this method is important to log4j performance. Modify with care!
    // 21 bytes (allows immediate JVM inlining: <= -XX:MaxInlineSize=35 bytes)
    private long millisSinceMidnight(final long now) {
        if (now >= midnightTomorrow) {
            updateMidnightMillis(now);
        }
        return now - midnightToday;
    }

    private void updateMidnightMillis(final long now) {

        updateCachedDate(now);

        midnightToday = calcMidnightMillis(now, 0);
        midnightTomorrow = calcMidnightMillis(now, 1);
    }

    static long calcMidnightMillis(final long time, final int addDays) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DATE, addDays);
        return cal.getTimeInMillis();
    }

    private void updateCachedDate(final long now) {
        if (fastDateFormat != null) {
            final StringBuilder result = fastDateFormat.format(now, new StringBuilder());
            cachedDate = result.toString().toCharArray();
        }
    }

    // Profiling showed this method is important to log4j performance. Modify with care!
    // 28 bytes (allows immediate JVM inlining: <= -XX:MaxInlineSize=35 bytes)
    public String format(final long time) {
        final char[] result = new char[length];
        int written = format(time, result, 0);
        return new String(result, 0, written);
    }

    // Profiling showed this method is important to log4j performance. Modify with care!
    // 31 bytes (allows immediate JVM inlining: <= -XX:MaxInlineSize=35 bytes)
    public int format(final long time, final char[] buffer, final int startPos) {
        // Calculate values by getting the ms values first and do then
        // calculate the hour minute and second values divisions.

        // Get daytime in ms: this does fit into an int
        // int ms = (int) (time % 86400000);
        final int ms = (int) (millisSinceMidnight(time));
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
        final int hours = ms / 3600000;
        ms -= 3600000 * hours;

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
}
