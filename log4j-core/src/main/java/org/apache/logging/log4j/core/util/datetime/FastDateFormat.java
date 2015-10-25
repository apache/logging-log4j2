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

import java.io.Serializable;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * This is a copy of Commons Lang's Fast Date Formatter.
 */
public class FastDateFormat extends Format implements DatePrinter, DateParser, Serializable {

    /**
     * FULL locale dependent date or time style.
     */
    public static final int FULL = DateFormat.FULL;
    /**
     * LONG locale dependent date or time style.
     */
    public static final int LONG = DateFormat.LONG;
    /**
     * MEDIUM locale dependent date or time style.
     */
    public static final int MEDIUM = DateFormat.MEDIUM;
    /**
     * SHORT locale dependent date or time style.
     */
    public static final int SHORT = DateFormat.SHORT;

    /**
     * Required for serialization support.
     *
     * @see java.io.Serializable
     */
    private static final long serialVersionUID = 2L;

    private static final FormatCache<FastDateFormat> CACHE = new FormatCache<FastDateFormat>() {
        @Override
        protected FastDateFormat createInstance(final String pattern, final TimeZone timeZone, final Locale locale) {
            return new FastDateFormat(pattern, timeZone, locale);
        }
    };

    private final FastDatePrinter printer;
    private final FastDateParser parser;

    // Constructor
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Constructs a new FastDateFormat.
     * </p>
     *
     * @param pattern {@link java.text.SimpleDateFormat} compatible pattern
     * @param timeZone non-null time zone to use
     * @param locale non-null locale to use
     * @throws NullPointerException if pattern, timeZone, or locale is null.
     */
    protected FastDateFormat(final String pattern, final TimeZone timeZone, final Locale locale) {
        this(pattern, timeZone, locale, null);
    }

    // Constructor
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Constructs a new FastDateFormat.
     * </p>
     *
     * @param pattern {@link java.text.SimpleDateFormat} compatible pattern
     * @param timeZone non-null time zone to use
     * @param locale non-null locale to use
     * @param centuryStart The start of the 100 year period to use as the "default century" for 2 digit year parsing.
     *            If centuryStart is null, defaults to now - 80 years
     * @throws NullPointerException if pattern, timeZone, or locale is null.
     */
    protected FastDateFormat(final String pattern, final TimeZone timeZone, final Locale locale,
            final Date centuryStart) {
        printer = new FastDatePrinter(pattern, timeZone, locale);
        parser = new FastDateParser(pattern, timeZone, locale, centuryStart);
    }

    // -----------------------------------------------------------------------
    /**
     * <p>
     * Gets a formatter instance using the default pattern in the default locale.
     * </p>
     *
     * @return a date/time formatter
     */
    public static FastDateFormat getInstance() {
        return CACHE.getInstance();
    }

    /**
     * <p>
     * Gets a formatter instance using the specified pattern in the default locale.
     * </p>
     *
     * @param pattern {@link java.text.SimpleDateFormat} compatible pattern
     * @return a pattern based date/time formatter
     * @throws IllegalArgumentException if pattern is invalid
     */
    public static FastDateFormat getInstance(final String pattern) {
        return CACHE.getInstance(pattern, null, null);
    }

    /**
     * <p>
     * Gets a formatter instance using the specified pattern and time zone.
     * </p>
     *
     * @param pattern {@link java.text.SimpleDateFormat} compatible pattern
     * @param timeZone optional time zone, overrides time zone of formatted date
     * @return a pattern based date/time formatter
     * @throws IllegalArgumentException if pattern is invalid
     */
    public static FastDateFormat getInstance(final String pattern, final TimeZone timeZone) {
        return CACHE.getInstance(pattern, timeZone, null);
    }

    /**
     * <p>
     * Gets a formatter instance using the specified pattern and locale.
     * </p>
     *
     * @param pattern {@link java.text.SimpleDateFormat} compatible pattern
     * @param locale optional locale, overrides system locale
     * @return a pattern based date/time formatter
     * @throws IllegalArgumentException if pattern is invalid
     */
    public static FastDateFormat getInstance(final String pattern, final Locale locale) {
        return CACHE.getInstance(pattern, null, locale);
    }

    /**
     * <p>
     * Gets a formatter instance using the specified pattern, time zone and locale.
     * </p>
     *
     * @param pattern {@link java.text.SimpleDateFormat} compatible pattern
     * @param timeZone optional time zone, overrides time zone of formatted date
     * @param locale optional locale, overrides system locale
     * @return a pattern based date/time formatter
     * @throws IllegalArgumentException if pattern is invalid or {@code null}
     */
    public static FastDateFormat getInstance(final String pattern, final TimeZone timeZone, final Locale locale) {
        return CACHE.getInstance(pattern, timeZone, locale);
    }

    // -----------------------------------------------------------------------
    /**
     * <p>
     * Gets a date formatter instance using the specified style in the default time zone and locale.
     * </p>
     *
     * @param style date style: FULL, LONG, MEDIUM, or SHORT
     * @return a localized standard date formatter
     * @throws IllegalArgumentException if the Locale has no date pattern defined
     * @since 2.1
     */
    public static FastDateFormat getDateInstance(final int style) {
        return CACHE.getDateInstance(style, null, null);
    }

    /**
     * <p>
     * Gets a date formatter instance using the specified style and locale in the default time zone.
     * </p>
     *
     * @param style date style: FULL, LONG, MEDIUM, or SHORT
     * @param locale optional locale, overrides system locale
     * @return a localized standard date formatter
     * @throws IllegalArgumentException if the Locale has no date pattern defined
     * @since 2.1
     */
    public static FastDateFormat getDateInstance(final int style, final Locale locale) {
        return CACHE.getDateInstance(style, null, locale);
    }

    /**
     * <p>
     * Gets a date formatter instance using the specified style and time zone in the default locale.
     * </p>
     *
     * @param style date style: FULL, LONG, MEDIUM, or SHORT
     * @param timeZone optional time zone, overrides time zone of formatted date
     * @return a localized standard date formatter
     * @throws IllegalArgumentException if the Locale has no date pattern defined
     * @since 2.1
     */
    public static FastDateFormat getDateInstance(final int style, final TimeZone timeZone) {
        return CACHE.getDateInstance(style, timeZone, null);
    }

    /**
     * <p>
     * Gets a date formatter instance using the specified style, time zone and locale.
     * </p>
     *
     * @param style date style: FULL, LONG, MEDIUM, or SHORT
     * @param timeZone optional time zone, overrides time zone of formatted date
     * @param locale optional locale, overrides system locale
     * @return a localized standard date formatter
     * @throws IllegalArgumentException if the Locale has no date pattern defined
     */
    public static FastDateFormat getDateInstance(final int style, final TimeZone timeZone, final Locale locale) {
        return CACHE.getDateInstance(style, timeZone, locale);
    }

    // -----------------------------------------------------------------------
    /**
     * <p>
     * Gets a time formatter instance using the specified style in the default time zone and locale.
     * </p>
     *
     * @param style time style: FULL, LONG, MEDIUM, or SHORT
     * @return a localized standard time formatter
     * @throws IllegalArgumentException if the Locale has no time pattern defined
     * @since 2.1
     */
    public static FastDateFormat getTimeInstance(final int style) {
        return CACHE.getTimeInstance(style, null, null);
    }

    /**
     * <p>
     * Gets a time formatter instance using the specified style and locale in the default time zone.
     * </p>
     *
     * @param style time style: FULL, LONG, MEDIUM, or SHORT
     * @param locale optional locale, overrides system locale
     * @return a localized standard time formatter
     * @throws IllegalArgumentException if the Locale has no time pattern defined
     * @since 2.1
     */
    public static FastDateFormat getTimeInstance(final int style, final Locale locale) {
        return CACHE.getTimeInstance(style, null, locale);
    }

    /**
     * <p>
     * Gets a time formatter instance using the specified style and time zone in the default locale.
     * </p>
     *
     * @param style time style: FULL, LONG, MEDIUM, or SHORT
     * @param timeZone optional time zone, overrides time zone of formatted time
     * @return a localized standard time formatter
     * @throws IllegalArgumentException if the Locale has no time pattern defined
     * @since 2.1
     */
    public static FastDateFormat getTimeInstance(final int style, final TimeZone timeZone) {
        return CACHE.getTimeInstance(style, timeZone, null);
    }

    /**
     * <p>
     * Gets a time formatter instance using the specified style, time zone and locale.
     * </p>
     *
     * @param style time style: FULL, LONG, MEDIUM, or SHORT
     * @param timeZone optional time zone, overrides time zone of formatted time
     * @param locale optional locale, overrides system locale
     * @return a localized standard time formatter
     * @throws IllegalArgumentException if the Locale has no time pattern defined
     */
    public static FastDateFormat getTimeInstance(final int style, final TimeZone timeZone, final Locale locale) {
        return CACHE.getTimeInstance(style, timeZone, locale);
    }

    // -----------------------------------------------------------------------
    /**
     * <p>
     * Gets a date/time formatter instance using the specified style in the default time zone and locale.
     * </p>
     *
     * @param dateStyle date style: FULL, LONG, MEDIUM, or SHORT
     * @param timeStyle time style: FULL, LONG, MEDIUM, or SHORT
     * @return a localized standard date/time formatter
     * @throws IllegalArgumentException if the Locale has no date/time pattern defined
     * @since 2.1
     */
    public static FastDateFormat getDateTimeInstance(final int dateStyle, final int timeStyle) {
        return CACHE.getDateTimeInstance(dateStyle, timeStyle, null, null);
    }

    /**
     * <p>
     * Gets a date/time formatter instance using the specified style and locale in the default time zone.
     * </p>
     *
     * @param dateStyle date style: FULL, LONG, MEDIUM, or SHORT
     * @param timeStyle time style: FULL, LONG, MEDIUM, or SHORT
     * @param locale optional locale, overrides system locale
     * @return a localized standard date/time formatter
     * @throws IllegalArgumentException if the Locale has no date/time pattern defined
     * @since 2.1
     */
    public static FastDateFormat getDateTimeInstance(final int dateStyle, final int timeStyle, final Locale locale) {
        return CACHE.getDateTimeInstance(dateStyle, timeStyle, null, locale);
    }

    /**
     * <p>
     * Gets a date/time formatter instance using the specified style and time zone in the default locale.
     * </p>
     *
     * @param dateStyle date style: FULL, LONG, MEDIUM, or SHORT
     * @param timeStyle time style: FULL, LONG, MEDIUM, or SHORT
     * @param timeZone optional time zone, overrides time zone of formatted date
     * @return a localized standard date/time formatter
     * @throws IllegalArgumentException if the Locale has no date/time pattern defined
     * @since 2.1
     */
    public static FastDateFormat getDateTimeInstance(final int dateStyle, final int timeStyle,
            final TimeZone timeZone) {
        return getDateTimeInstance(dateStyle, timeStyle, timeZone, null);
    }

    /**
     * <p>
     * Gets a date/time formatter instance using the specified style, time zone and locale.
     * </p>
     *
     * @param dateStyle date style: FULL, LONG, MEDIUM, or SHORT
     * @param timeStyle time style: FULL, LONG, MEDIUM, or SHORT
     * @param timeZone optional time zone, overrides time zone of formatted date
     * @param locale optional locale, overrides system locale
     * @return a localized standard date/time formatter
     * @throws IllegalArgumentException if the Locale has no date/time pattern defined
     */
    public static FastDateFormat getDateTimeInstance(final int dateStyle, final int timeStyle,
            final TimeZone timeZone, final Locale locale) {
        return CACHE.getDateTimeInstance(dateStyle, timeStyle, timeZone, locale);
    }

    // Format methods
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Formats a {@code Date}, {@code Calendar} or {@code Long} (milliseconds) object.
     * </p>
     *
     * @param obj the object to format
     * @param toAppendTo the buffer to append to
     * @param pos the position - ignored
     * @return the buffer passed in
     */
    @Override
    public StringBuilder format(final Object obj, final StringBuilder toAppendTo, final FieldPosition pos) {
        return printer.format(obj, toAppendTo, pos);
    }

    /**
     * <p>
     * Formats a millisecond {@code long} value.
     * </p>
     *
     * @param millis the millisecond value to format
     * @return the formatted string
     * @since 2.1
     */
    @Override
    public String format(final long millis) {
        return printer.format(millis);
    }

    /**
     * <p>
     * Formats a {@code Date} object using a {@code GregorianCalendar}.
     * </p>
     *
     * @param date the date to format
     * @return the formatted string
     */
    @Override
    public String format(final Date date) {
        return printer.format(date);
    }

    /**
     * <p>
     * Formats a {@code Calendar} object.
     * </p>
     *
     * @param calendar the calendar to format
     * @return the formatted string
     */
    @Override
    public String format(final Calendar calendar) {
        return printer.format(calendar);
    }

    /**
     * <p>
     * Formats a millisecond {@code long} value into the supplied {@code StringBuilder}.
     * </p>
     *
     * @param millis the millisecond value to format
     * @param buf the buffer to format into
     * @return the specified string buffer
     * @since 2.1
     */
    @Override
    public StringBuilder format(final long millis, final StringBuilder buf) {
        return printer.format(millis, buf);
    }

    /**
     * <p>
     * Formats a {@code Date} object into the supplied {@code StringBuilder} using a {@code GregorianCalendar}.
     * </p>
     *
     * @param date the date to format
     * @param buf the buffer to format into
     * @return the specified string buffer
     */
    @Override
    public StringBuilder format(final Date date, final StringBuilder buf) {
        return printer.format(date, buf);
    }

    /**
     * <p>
     * Formats a {@code Calendar} object into the supplied {@code StringBuilder}.
     * </p>
     *
     * @param calendar the calendar to format
     * @param buf the buffer to format into
     * @return the specified string buffer
     */
    @Override
    public StringBuilder format(final Calendar calendar, final StringBuilder buf) {
        return printer.format(calendar, buf);
    }

    // Parsing
    // -----------------------------------------------------------------------

    /*
     * (non-Javadoc)
     * 
     * @see DateParser#parse(java.lang.String)
     */
    @Override
    public Date parse(final String source) throws ParseException {
        return parser.parse(source);
    }

    /*
     * (non-Javadoc)
     * 
     * @see DateParser#parse(java.lang.String, java.text.ParsePosition)
     */
    @Override
    public Date parse(final String source, final ParsePosition pos) {
        return parser.parse(source, pos);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.text.Format#parseObject(java.lang.String, java.text.ParsePosition)
     */
    @Override
    public Object parseObject(final String source, final ParsePosition pos) {
        return parser.parseObject(source, pos);
    }

    // Accessors
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Gets the pattern used by this formatter.
     * </p>
     *
     * @return the pattern, {@link java.text.SimpleDateFormat} compatible
     */
    @Override
    public String getPattern() {
        return printer.getPattern();
    }

    /**
     * <p>
     * Gets the time zone used by this formatter.
     * </p>
     *
     * <p>
     * This zone is always used for {@code Date} formatting.
     * </p>
     *
     * @return the time zone
     */
    @Override
    public TimeZone getTimeZone() {
        return printer.getTimeZone();
    }

    /**
     * <p>
     * Gets the locale used by this formatter.
     * </p>
     *
     * @return the locale
     */
    @Override
    public Locale getLocale() {
        return printer.getLocale();
    }

    /**
     * <p>
     * Gets an estimate for the maximum string length that the formatter will produce.
     * </p>
     *
     * <p>
     * The actual formatted length will almost always be less than or equal to this amount.
     * </p>
     *
     * @return the maximum formatted length
     */
    public int getMaxLengthEstimate() {
        return printer.getMaxLengthEstimate();
    }

    public String toPattern() {
        return printer.getPattern();
    }

    // Basics
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Compares two objects for equality.
     * </p>
     *
     * @param obj the object to compare to
     * @return {@code true} if equal
     */
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof FastDateFormat)) {
            return false;
        }
        final FastDateFormat other = (FastDateFormat) obj;
        // no need to check parser, as it has same invariants as printer
        return printer.equals(other.printer);
    }

    /**
     * <p>
     * Returns a hashcode compatible with equals.
     * </p>
     *
     * @return a hashcode compatible with equals
     */
    @Override
    public int hashCode() {
        return printer.hashCode();
    }

    /**
     * <p>
     * Gets a debugging string version of this formatter.
     * </p>
     *
     * @return a debugging string
     */
    @Override
    public String toString() {
        return "FastDateFormat[" + printer.getPattern() + "," + printer.getLocale() + ","
                + printer.getTimeZone().getID() + "]";
    }

    /**
     * <p>
     * Performs the formatting by applying the rules to the specified calendar.
     * </p>
     *
     * @param calendar the calendar to format
     * @param buf the buffer to format into
     * @return the specified string buffer
     */
    protected StringBuilder applyRules(final Calendar calendar, final StringBuilder buf) {
        return printer.applyRules(calendar, buf);
    }

}
