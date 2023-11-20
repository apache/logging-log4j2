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

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Date;
import java.util.TimeZone;
import org.apache.logging.log4j.core.util.Constants;

/**
 * CachedDateFormat optimizes the performance of a wrapped
 * DateFormat.  The implementation is not thread-safe.
 * If the millisecond pattern is not recognized,
 * the class will only use the cache if the
 * same value is requested.
 */
final class CachedDateFormat extends DateFormat {

    /**
     * Constant used to represent that there was no change
     * observed when changing the millisecond count.
     */
    public static final int NO_MILLISECONDS = -2;

    /**
     * Constant used to represent that there was an
     * observed change, but was an expected change.
     */
    public static final int UNRECOGNIZED_MILLISECONDS = -1;

    private static final long serialVersionUID = -1253877934598423628L;

    /**
     * Supported digit set.  If the wrapped DateFormat uses
     * a different unit set, the millisecond pattern
     * will not be recognized and duplicate requests
     * will use the cache.
     */
    private static final String DIGITS = "0123456789";

    /**
     * First magic number used to detect the millisecond position.
     */
    private static final int MAGIC1 = 654;

    /**
     * Expected representation of first magic number.
     */
    private static final String MAGICSTRING1 = "654";

    /**
     * Second magic number used to detect the millisecond position.
     */
    private static final int MAGIC2 = 987;

    /**
     * Expected representation of second magic number.
     */
    private static final String MAGICSTRING2 = "987";

    /**
     * Expected representation of 0 milliseconds.
     */
    private static final String ZERO_STRING = "000";

    private static final int BUF_SIZE = 50;

    private static final int DEFAULT_VALIDITY = 1000;

    private static final int THREE_DIGITS = 100;

    private static final int TWO_DIGITS = 10;

    private static final long SLOTS = 1000L;

    /**
     * Wrapped formatter.
     */
    private final DateFormat formatter;

    /**
     * Index of initial digit of millisecond pattern or
     * UNRECOGNIZED_MILLISECONDS or NO_MILLISECONDS.
     */
    private int millisecondStart;

    /**
     * Integral second preceding the previous converted Date.
     */
    private long slotBegin;

    /**
     * Cache of previous conversion.
     */
    private final StringBuffer cache = new StringBuffer(BUF_SIZE);

    /**
     * Maximum validity period for the cache.
     * Typically 1, use cache for duplicate requests only, or
     * 1000, use cache for requests within the same integral second.
     */
    private final int expiration;

    /**
     * Date requested in previous conversion.
     */
    private long previousTime;

    /**
     * Scratch date object used to minimize date object creation.
     */
    private final Date tmpDate = new Date(0);

    /**
     * Creates a new CachedDateFormat object.
     *
     * @param dateFormat Date format, may not be null.
     * @param expiration maximum cached range in milliseconds.
     *                   If the dateFormat is known to be incompatible with the
     *                   caching algorithm, use a value of 0 to totally disable
     *                   caching or 1 to only use cache for duplicate requests.
     */
    public CachedDateFormat(final DateFormat dateFormat, final int expiration) {
        if (dateFormat == null) {
            throw new IllegalArgumentException("dateFormat cannot be null");
        }

        if (expiration < 0) {
            throw new IllegalArgumentException("expiration must be non-negative");
        }

        formatter = dateFormat;
        this.expiration = expiration;
        millisecondStart = 0;

        //
        //   set the previousTime so the cache will be invalid
        //        for the next request.
        previousTime = Long.MIN_VALUE;
        slotBegin = Long.MIN_VALUE;
    }

    /**
     * Finds start of millisecond field in formatted time.
     *
     * @param time      long time, must be integral number of seconds
     * @param formatted String corresponding formatted string
     * @param formatter DateFormat date format
     * @return int position in string of first digit of milliseconds,
     *         -1 indicates no millisecond field, -2 indicates unrecognized
     *         field (likely RelativeTimeDateFormat)
     */
    public static int findMillisecondStart(final long time, final String formatted, final DateFormat formatter) {
        long slotBegin = (time / Constants.MILLIS_IN_SECONDS) * Constants.MILLIS_IN_SECONDS;

        if (slotBegin > time) {
            slotBegin -= Constants.MILLIS_IN_SECONDS;
        }

        final int millis = (int) (time - slotBegin);

        int magic = MAGIC1;
        String magicString = MAGICSTRING1;

        if (millis == MAGIC1) {
            magic = MAGIC2;
            magicString = MAGICSTRING2;
        }

        final String plusMagic = formatter.format(new Date(slotBegin + magic));

        /**
         *   If the string lengths differ then
         *      we can't use the cache except for duplicate requests.
         */
        if (plusMagic.length() != formatted.length()) {
            return UNRECOGNIZED_MILLISECONDS;
        }
        // find first difference between values
        for (int i = 0; i < formatted.length(); i++) {
            if (formatted.charAt(i) != plusMagic.charAt(i)) {
                //
                //   determine the expected digits for the base time
                final StringBuffer formattedMillis = new StringBuffer("ABC");
                millisecondFormat(millis, formattedMillis, 0);

                final String plusZero = formatter.format(new Date(slotBegin));

                //   If the next 3 characters match the magic
                //      string and the expected string
                if ((plusZero.length() == formatted.length())
                        && magicString.regionMatches(0, plusMagic, i, magicString.length())
                        && formattedMillis.toString().regionMatches(0, formatted, i, magicString.length())
                        && ZERO_STRING.regionMatches(0, plusZero, i, ZERO_STRING.length())) {
                    return i;
                }
                return UNRECOGNIZED_MILLISECONDS;
            }
        }

        return NO_MILLISECONDS;
    }

    /**
     * Formats a Date into a date/time string.
     *
     * @param date          the date to format.
     * @param sbuf          the string buffer to write to.
     * @param fieldPosition remains untouched.
     * @return the formatted time string.
     */
    @Override
    public StringBuffer format(final Date date, final StringBuffer sbuf, final FieldPosition fieldPosition) {
        format(date.getTime(), sbuf);

        return sbuf;
    }

    /**
     * Formats a millisecond count into a date/time string.
     *
     * @param now Number of milliseconds after midnight 1 Jan 1970 GMT.
     * @param buf the string buffer to write to.
     * @return the formatted time string.
     */
    public StringBuffer format(final long now, final StringBuffer buf) {
        //
        // If the current requested time is identical to the previously
        //     requested time, then append the cache contents.
        //
        if (now == previousTime) {
            buf.append(cache);

            return buf;
        }

        //
        //   If millisecond pattern was not unrecognized
        //     (that is if it was found or milliseconds did not appear)
        //
        if (millisecondStart != UNRECOGNIZED_MILLISECONDS
                &&
                //    Check if the cache is still valid.
                //    If the requested time is within the same integral second
                //       as the last request and a shorter expiration was not requested.
                (now < (slotBegin + expiration))
                && (now >= slotBegin)
                && (now < (slotBegin + SLOTS))) {
            //
            //    if there was a millisecond field then update it
            //
            if (millisecondStart >= 0) {
                millisecondFormat((int) (now - slotBegin), cache, millisecondStart);
            }

            //
            //   update the previously requested time
            //      (the slot begin should be unchanged)
            previousTime = now;
            buf.append(cache);

            return buf;
        }

        //
        //  could not use previous value.
        //    Call underlying formatter to format date.
        cache.setLength(0);
        tmpDate.setTime(now);
        cache.append(formatter.format(tmpDate));
        buf.append(cache);
        previousTime = now;
        slotBegin = (previousTime / Constants.MILLIS_IN_SECONDS) * Constants.MILLIS_IN_SECONDS;

        if (slotBegin > previousTime) {
            slotBegin -= Constants.MILLIS_IN_SECONDS;
        }

        //
        //    if the milliseconds field was previous found
        //       then reevaluate in case it moved.
        //
        if (millisecondStart >= 0) {
            millisecondStart = findMillisecondStart(now, cache.toString(), formatter);
        }

        return buf;
    }

    /**
     * Formats a count of milliseconds (0-999) into a numeric representation.
     *
     * @param millis Millisecond count between 0 and 999.
     * @param buf    String buffer, may not be null.
     * @param offset Starting position in buffer, the length of the
     *               buffer must be at least offset + 3.
     */
    private static void millisecondFormat(final int millis, final StringBuffer buf, final int offset) {
        buf.setCharAt(offset, DIGITS.charAt(millis / THREE_DIGITS));
        buf.setCharAt(offset + 1, DIGITS.charAt((millis / TWO_DIGITS) % TWO_DIGITS));
        buf.setCharAt(offset + 2, DIGITS.charAt(millis % TWO_DIGITS));
    }

    /**
     * Sets the time zone.
     * <p>
     * Setting the time zone using getCalendar().setTimeZone() will likely cause caching to misbehave.
     * </p>
     *
     * @param timeZone
     *        TimeZone new time zone
     */
    @Override
    public void setTimeZone(final TimeZone timeZone) {
        formatter.setTimeZone(timeZone);
        previousTime = Long.MIN_VALUE;
        slotBegin = Long.MIN_VALUE;
    }

    /**
     * This method is delegated to the formatter which most
     * likely returns null.
     *
     * @param s   string representation of date.
     * @param pos field position, unused.
     * @return parsed date, likely null.
     */
    @Override
    public Date parse(final String s, final ParsePosition pos) {
        return formatter.parse(s, pos);
    }

    /**
     * Gets number formatter.
     *
     * @return NumberFormat number formatter
     */
    @Override
    public NumberFormat getNumberFormat() {
        return formatter.getNumberFormat();
    }

    /**
     * Gets maximum cache validity for the specified SimpleDateTime
     * conversion pattern.
     *
     * @param pattern conversion pattern, may not be null.
     * @return Duration in milliseconds from an integral second
     *         that the cache will return consistent results.
     */
    public static int getMaximumCacheValidity(final String pattern) {
        //
        //   If there are more "S" in the pattern than just one "SSS" then
        //      (for example, "HH:mm:ss,SSS SSS"), then set the expiration to
        //      one millisecond which should only perform duplicate request caching.
        //
        final int firstS = pattern.indexOf('S');

        if ((firstS >= 0) && (firstS != pattern.lastIndexOf("SSS"))) {
            return 1;
        }

        return DEFAULT_VALIDITY;
    }
}
