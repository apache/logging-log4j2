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

import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.logging.log4j.core.time.internal.format.*;

/**
 *
 * @deprecated Use {@link org.apache.logging.log4j.core.time.internal.format.FastDateFormat}
 */
public class FastDateFormat extends Format implements DateParser, DatePrinter {

    private org.apache.logging.log4j.core.time.internal.format.FastDateFormat formatter = null;

    public static FastDateFormat getInstance() {
        return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getInstance());
    }

    public static FastDateFormat getInstance(final String pattern) {
        return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getInstance(pattern));
    }

    public static FastDateFormat getInstance(final String pattern, final TimeZone timeZone) {
        return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getInstance(pattern, timeZone));
    }

    public static FastDateFormat getInstance(final String pattern, final Locale locale) {
        return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getInstance(pattern, locale));
    }

    public static FastDateFormat getInstance(final String pattern, final TimeZone timeZone, final Locale locale) {
        return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getInstance(pattern, timeZone, locale));
    }

    public static FastDateFormat getDateInstance(final int style) {
        return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getDateInstance(style));
    }

    public static FastDateFormat getDateInstance(final int style, final Locale locale) {
        return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getDateInstance(style, locale));
    }

    public static FastDateFormat getDateInstance(final int style, final TimeZone timeZone) {
        return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getDateInstance(style, timeZone));
    }

    public static FastDateFormat getDateInstance(final int style, final TimeZone timeZone, final Locale locale) {
        return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getDateInstance(style, timeZone, locale));
    }

    public static FastDateFormat getTimeInstance(final int style) {
        return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getTimeInstance(style));
    }

    public static FastDateFormat getTimeInstance(final int style, final Locale locale) {
        return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getTimeInstance(style, locale));
    }

    public static FastDateFormat getTimeInstance(final int style, final TimeZone timeZone) {
        return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getTimeInstance(style, timeZone));
    }

    public static FastDateFormat getTimeInstance(final int style, final TimeZone timeZone, final Locale locale) {
        return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getTimeInstance(style, timeZone, locale));
    }

    public static FastDateFormat getDateTimeInstance(final int dateStyle, final int timeStyle) {
        return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getDateTimeInstance(dateStyle, timeStyle));
    }

    public static FastDateFormat getDateTimeInstance(final int dateStyle, final int timeStyle, final Locale locale) {
        return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getDateTimeInstance(dateStyle, timeStyle, locale));
    }

    public static FastDateFormat getDateTimeInstance(final int dateStyle, final int timeStyle, final TimeZone timeZone) {
        return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getDateTimeInstance(dateStyle, timeStyle, timeZone));
    }

    public static FastDateFormat getDateTimeInstance(
            final int dateStyle, final int timeStyle, final TimeZone timeZone, final Locale locale) {
        return new FastDateFormat(org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getDateTimeInstance(dateStyle, timeStyle, timeZone, locale));
    }

    private FastDateFormat(final org.apache.logging.log4j.core.time.internal.format.FastDateFormat formatter) {
        this.formatter = formatter;
    }

    protected FastDateFormat(final String pattern, final TimeZone timeZone, final Locale locale) {
        formatter = org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getInstance(pattern,
                timeZone, locale);
    }

    protected FastDateFormat(final String pattern, final TimeZone timeZone, final Locale locale, final Date centuryStart) {
        formatter = org.apache.logging.log4j.core.time.internal.format.FastDateFormat.getDateTimeInstance(pattern,
                timeZone, locale, centuryStart);
    }

    @Override
    public StringBuilder format(final Object obj, final StringBuilder toAppendTo, final FieldPosition pos) {
        return formatter.format(obj, toAppendTo, pos);
    }

    @Override
    public String format(final long millis) {
        return formatter.format(millis);
    }

    @Override
    public String format(final Date date) {
        return formatter.format(date);
    }

    @Override
    public String format(final Calendar calendar) {
        return formatter.format(calendar);
    }

    @Override
    public <B extends Appendable> B format(final long millis, final B buf) {
        return formatter.format(millis, buf);
    }

    @Override
    public <B extends Appendable> B format(final Date date, final B buf) {
        return formatter.format(date, buf);
    }

    @Override
    public <B extends Appendable> B format(final Calendar calendar, final B buf) {
        return formatter.format(calendar, buf);
    }

    @Override
    public Date parse(final String source) throws ParseException {
        return formatter.parse(source);
    }

    @Override
    public Date parse(final String source, final ParsePosition pos) {
        return formatter.parse(source, pos);
    }

    @Override
    public boolean parse(final String source, final ParsePosition pos, final Calendar calendar) {
        return formatter.parse(source, pos, calendar);
    }

    @Override
    public Object parseObject(final String source, final ParsePosition pos) {
        return formatter.parseObject(source, pos);
    }

    @Override
    public String getPattern() {
        return formatter.getPattern();
    }

    @Override
    public TimeZone getTimeZone() {
        return formatter.getTimeZone();
    }

    @Override
    public Locale getLocale() {
        return formatter.getLocale();
    }

    public int getMaxLengthEstimate() {
        return formatter.getMaxLengthEstimate();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof FastDateFormat == false) {
            return false;
        }
        final FastDateFormat other = (FastDateFormat) obj;
        // no need to check parser, as it has same invariants as printer
        return formatter.equals(other.formatter);
    }

    @Override
    public int hashCode() {
        return formatter.hashCode();
    }

    @Override
    public String toString() {
        return formatter.toString();
    }
}
