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
package org.apache.logging.log4j.core.pattern;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;


/**
 * Convert and format the event's date in a StringBuilder.
 */
@Plugin(name = "DatePatternConverter", category = "Converter")
@ConverterKeys({"d", "date" })
public final class DatePatternConverter extends LogEventPatternConverter implements ArrayPatternConverter {

    /**
     * ABSOLUTE string literal.
     */
    private static final String ABSOLUTE_FORMAT = "ABSOLUTE";

    /**
     * COMPACT string literal.
     */
    private static final String COMPACT_FORMAT = "COMPACT";

    /**
     * SimpleTimePattern for ABSOLUTE.
     */
    private static final String ABSOLUTE_TIME_PATTERN = "HH:mm:ss,SSS";

    /**
     * DATE string literal.
     */
    private static final String DATE_AND_TIME_FORMAT = "DATE";

    /**
     * SimpleTimePattern for DATE.
     */
    private static final String DATE_AND_TIME_PATTERN = "dd MMM yyyy HH:mm:ss,SSS";

    /**
     * ISO8601 string literal.
     */
    private static final String ISO8601_FORMAT = "ISO8601";

    /**
     * ISO8601_BASIC string literal.
     */
    private static final String ISO8601_BASIC_FORMAT = "ISO8601_BASIC";

    /**
     * SimpleTimePattern for ISO8601.
     */
    private static final String ISO8601_PATTERN = "yyyy-MM-dd HH:mm:ss,SSS";

    /**
     * SimpleTimePattern for ISO8601_BASIC.
     */
    private static final String ISO8601_BASIC_PATTERN = "yyyyMMdd HHmmss,SSS";

    /**
     * SimpleTimePattern for COMPACT.
     */
    private static final String COMPACT_PATTERN = "yyyyMMddHHmmssSSS";

    /**
     * Date format.
     */
    private String cachedDate;

    private long lastTimestamp;

    private final SimpleDateFormat simpleFormat;

    /**
     * Private constructor.
     *
     * @param options options, may be null.
     */
    private DatePatternConverter(final String[] options) {
        super("Date", "date");

        String patternOption;

        if (options == null || options.length == 0) {
            // the branch could be optimized, but here we are making explicit
            // that null values for patternOption are allowed.
            patternOption = null;
        } else {
            patternOption = options[0];
        }

        String pattern;

        if (patternOption == null || patternOption.equalsIgnoreCase(ISO8601_FORMAT)) {
            pattern = ISO8601_PATTERN;
        } else if (patternOption.equalsIgnoreCase(ISO8601_BASIC_FORMAT)) {
            pattern = ISO8601_BASIC_PATTERN;
        } else if (patternOption.equalsIgnoreCase(ABSOLUTE_FORMAT)) {
            pattern = ABSOLUTE_TIME_PATTERN;
        } else if (patternOption.equalsIgnoreCase(DATE_AND_TIME_FORMAT)) {
            pattern = DATE_AND_TIME_PATTERN;
        } else if (patternOption.equalsIgnoreCase(COMPACT_FORMAT)) {
            pattern = COMPACT_PATTERN;
        } else {
            pattern = patternOption;
        }

        SimpleDateFormat tempFormat;

        try {
            tempFormat = new SimpleDateFormat(pattern);
        } catch (final IllegalArgumentException e) {
            LOGGER.warn("Could not instantiate SimpleDateFormat with pattern " + patternOption, e);

            // default to the ISO8601 format
            tempFormat = new SimpleDateFormat(ISO8601_PATTERN);
        }

        // if the option list contains a TZ option, then set it.
        if (options != null && options.length > 1) {
            final TimeZone tz = TimeZone.getTimeZone(options[1]);
            tempFormat.setTimeZone(tz);
        }
        simpleFormat = tempFormat;
    }

    /**
     * Obtains an instance of pattern converter.
     *
     * @param options options, may be null.
     * @return instance of pattern converter.
     */
    public static DatePatternConverter newInstance(final String[] options) {
        return new DatePatternConverter(options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder output) {
        final long timestamp = event.getMillis();

        synchronized (this) {
            if (timestamp != lastTimestamp) {
                lastTimestamp = timestamp;
                cachedDate = simpleFormat.format(timestamp);
            }
        }
        output.append(cachedDate);
    }

    @Override
    public void format(final StringBuilder toAppendTo, final Object... objects) {
        for (final Object obj : objects) {
            if (obj instanceof Date) {
                format(obj, toAppendTo);
                break;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final Object obj, final StringBuilder output) {
        if (obj instanceof Date) {
            format((Date) obj, output);
        }

        super.format(obj, output);
    }

    /**
     * Append formatted date to string buffer.
     *
     * @param date       date
     * @param toAppendTo buffer to which formatted date is appended.
     */
    public void format(final Date date, final StringBuilder toAppendTo) {
        synchronized (this) {
            toAppendTo.append(simpleFormat.format(date.getTime()));
        }
    }

    public String getPattern() {
        return simpleFormat.toPattern();
    }

}
