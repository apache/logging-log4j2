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
package org.apache.logging.log4j.core.appender.rolling.action;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simplified implementation of the <a href="https://en.wikipedia.org/wiki/ISO_8601#Durations">ISO-8601 Durations</a>
 * standard. The supported format is {@code PnDTnHnMnS}, with 'P' and 'T' optional. Days are considered to be exactly 24
 * hours.
 * <p>
 * Similarly to the {@code java.time.Duration} class, this class does not support year or month sections in the format.
 * This implementation does not support fractions or negative values.
 *
 * @see #parse(CharSequence)
 */
public class Duration implements Serializable, Comparable<Duration> {
    private static final long serialVersionUID = -3756810052716342061L;

    /**
     * Constant for a duration of zero.
     */
    public static final Duration ZERO = new Duration(0);

    /**
     * Hours per day.
     */
    private static final int HOURS_PER_DAY = 24;
    /**
     * Minutes per hour.
     */
    private static final int MINUTES_PER_HOUR = 60;
    /**
     * Seconds per minute.
     */
    private static final int SECONDS_PER_MINUTE = 60;
    /**
     * Seconds per hour.
     */
    private static final int SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR;
    /**
     * Seconds per day.
     */
    private static final int SECONDS_PER_DAY = SECONDS_PER_HOUR * HOURS_PER_DAY;

    /**
     * The pattern for parsing.
     */
    private static final Pattern PATTERN = Pattern.compile(
            "P?(?:([0-9]+)D)?" + "(T?(?:([0-9]+)H)?(?:([0-9]+)M)?(?:([0-9]+)?S)?)?", Pattern.CASE_INSENSITIVE);

    /**
     * The number of seconds in the duration.
     */
    private final long seconds;

    /**
     * Constructs an instance of {@code Duration} using seconds.
     *
     * @param seconds the length of the duration in seconds, positive or negative
     */
    private Duration(final long seconds) {
        this.seconds = seconds;
    }

    /**
     * Obtains a {@code Duration} from a text string such as {@code PnDTnHnMnS}.
     * <p>
     * This will parse a textual representation of a duration, including the string produced by {@code toString()}. The
     * formats accepted are based on the ISO-8601 duration format {@code PnDTnHnMnS} with days considered to be exactly
     * 24 hours.
     * <p>
     * This implementation does not support negative numbers or fractions (so the smallest non-zero value a Duration can
     * have is one second).
     * <p>
     * The string optionally starts with the ASCII letter "P" in upper or lower case. There are then four sections, each
     * consisting of a number and a suffix. The sections have suffixes in ASCII of "D", "H", "M" and "S" for days,
     * hours, minutes and seconds, accepted in upper or lower case. The suffixes must occur in order. The ASCII letter
     * "T" may occur before the first occurrence, if any, of an hour, minute or second section. At least one of the four
     * sections must be present, and if "T" is present there must be at least one section after the "T". The number part
     * of each section must consist of one or more ASCII digits. The number may not be prefixed by the ASCII negative or
     * positive symbol. The number of days, hours, minutes and seconds must parse to a {@code long}.
     * <p>
     * Examples:
     *
     * <pre>
     *    "PT20S" -- parses as "20 seconds"
     *    "PT15M"     -- parses as "15 minutes" (where a minute is 60 seconds)
     *    "PT10H"     -- parses as "10 hours" (where an hour is 3600 seconds)
     *    "P2D"       -- parses as "2 days" (where a day is 24 hours or 86400 seconds)
     *    "P2DT3H4M"  -- parses as "2 days, 3 hours and 4 minutes"
     * </pre>
     *
     * @param text the text to parse, not null
     * @return the parsed duration, not null
     * @throws IllegalArgumentException if the text cannot be parsed to a duration
     */
    public static Duration parse(final CharSequence text) {
        Objects.requireNonNull(text, "text");
        final Matcher matcher = PATTERN.matcher(text);
        if (matcher.matches()) {
            // check for letter T but no time sections
            if ("T".equals(matcher.group(2)) == false) {
                final String dayMatch = matcher.group(1);
                final String hourMatch = matcher.group(3);
                final String minuteMatch = matcher.group(4);
                final String secondMatch = matcher.group(5);
                if (dayMatch != null || hourMatch != null || minuteMatch != null || secondMatch != null) {
                    final long daysAsSecs = parseNumber(text, dayMatch, SECONDS_PER_DAY, "days");
                    final long hoursAsSecs = parseNumber(text, hourMatch, SECONDS_PER_HOUR, "hours");
                    final long minsAsSecs = parseNumber(text, minuteMatch, SECONDS_PER_MINUTE, "minutes");
                    final long seconds = parseNumber(text, secondMatch, 1, "seconds");
                    try {
                        return create(daysAsSecs, hoursAsSecs, minsAsSecs, seconds);
                    } catch (final ArithmeticException ex) {
                        throw new IllegalArgumentException(
                                "Text cannot be parsed to a Duration (overflow) " + text, ex);
                    }
                }
            }
        }
        throw new IllegalArgumentException("Text cannot be parsed to a Duration: " + text);
    }

    private static long parseNumber(
            final CharSequence text, final String parsed, final int multiplier, final String errorText) {
        // regex limits to [0-9]+
        if (parsed == null) {
            return 0;
        }
        try {
            final long val = Long.parseLong(parsed);
            return val * multiplier;
        } catch (final Exception ex) {
            throw new IllegalArgumentException(
                    "Text cannot be parsed to a Duration: " + errorText + " (in " + text + ")", ex);
        }
    }

    private static Duration create(
            final long daysAsSecs, final long hoursAsSecs, final long minsAsSecs, final long secs) {
        return create(daysAsSecs + hoursAsSecs + minsAsSecs + secs);
    }

    /**
     * Obtains an instance of {@code Duration} using seconds.
     *
     * @param seconds the length of the duration in seconds, positive only
     */
    private static Duration create(final long seconds) {
        if ((seconds) == 0) {
            return ZERO;
        }
        return new Duration(seconds);
    }

    /**
     * Converts this duration to the total length in milliseconds.
     *
     * @return the total length of the duration in milliseconds
     */
    public long toMillis() {
        return seconds * 1000L;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Duration)) {
            return false;
        }
        final Duration other = (Duration) obj;
        return other.seconds == this.seconds;
    }

    @Override
    public int hashCode() {
        return (int) (seconds ^ (seconds >>> 32));
    }

    /**
     * A string representation of this duration using ISO-8601 seconds based representation, such as {@code PT8H6M12S}.
     * <p>
     * The format of the returned string will be {@code PnDTnHnMnS}, where n is the relevant days, hours, minutes or
     * seconds part of the duration. If a section has a zero value, it is omitted. The hours, minutes and seconds are
     * all positive.
     * <p>
     * Examples:
     *
     * <pre>
     *    "20 seconds"                     -- "PT20S
     *    "15 minutes" (15 * 60 seconds)   -- "PT15M"
     *    "10 hours" (10 * 3600 seconds)   -- "PT10H"
     *    "2 days" (2 * 86400 seconds)     -- "P2D"
     * </pre>
     *
     * @return an ISO-8601 representation of this duration, not null
     */
    @Override
    public String toString() {
        if (this == ZERO) {
            return "PT0S";
        }
        final long days = seconds / SECONDS_PER_DAY;
        final long hours = (seconds % SECONDS_PER_DAY) / SECONDS_PER_HOUR;
        final int minutes = (int) ((seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE);
        final int secs = (int) (seconds % SECONDS_PER_MINUTE);
        final StringBuilder buf = new StringBuilder(24);
        buf.append("P");
        if (days != 0) {
            buf.append(days).append('D');
        }
        if ((hours | minutes | secs) != 0) {
            buf.append('T');
        }
        if (hours != 0) {
            buf.append(hours).append('H');
        }
        if (minutes != 0) {
            buf.append(minutes).append('M');
        }
        if (secs == 0 && buf.length() > 0) {
            return buf.toString();
        }
        buf.append(secs).append('S');
        return buf.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final Duration other) {
        return Long.signum(toMillis() - other.toMillis());
    }
}
