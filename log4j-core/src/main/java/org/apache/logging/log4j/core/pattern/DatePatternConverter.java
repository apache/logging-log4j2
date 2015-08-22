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

import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;
import org.apache.logging.log4j.core.util.datetime.FixedDateFormat;
import org.apache.logging.log4j.core.util.datetime.FixedDateFormat.FixedFormat;

/**
 * Converts and formats the event's date in a StringBuilder.
 */
@Plugin(name = "DatePatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({ "d", "date" })
public final class DatePatternConverter extends LogEventPatternConverter implements ArrayPatternConverter {

    private final class CachedTime {
        public long timestampMillis;
        public String formatted;

        public CachedTime(final long timestampMillis) {
            this.timestampMillis = timestampMillis;
            this.formatted = formatter.format(this.timestampMillis);
        }
    }

    private final AtomicReference<CachedTime> cachedTime;

    private abstract static class Formatter {
        abstract String format(long timeMillis);

        public String toPattern() {
            return null;
        }
    }

    private static final class PatternFormatter extends Formatter {
        private final FastDateFormat fastDateFormat;

        PatternFormatter(final FastDateFormat fastDateFormat) {
            this.fastDateFormat = fastDateFormat;
        }

        @Override
        String format(final long timeMillis) {
            return fastDateFormat.format(timeMillis);
        }

        @Override
        public String toPattern() {
            return fastDateFormat.toPattern();
        }
    }

    private static final class FixedFormatter extends Formatter {
        private final FixedDateFormat fixedDateFormat;

        FixedFormatter(final FixedDateFormat fixedDateFormat) {
            this.fixedDateFormat = fixedDateFormat;
        }

        @Override
        String format(final long timeMillis) {
            return fixedDateFormat.format(timeMillis);
        }

        @Override
        public String toPattern() {
            return fixedDateFormat.getFormat();
        }
    }

    private static final class UnixFormatter extends Formatter {

        @Override
        String format(final long timeMillis) {
            return Long.toString(timeMillis / 1000);
        }
    }

    private static final class UnixMillisFormatter extends Formatter {

        @Override
        String format(final long timeMillis) {
            return Long.toString(timeMillis);
        }
    }

    /**
     * UNIX formatter in seconds (standard).
     */
    private static final String UNIX_FORMAT = "UNIX";

    /**
     * UNIX formatter in milliseconds
     */
    private static final String UNIX_MILLIS_FORMAT = "UNIX_MILLIS";

    /**
     * Obtains an instance of pattern converter.
     *
     * @param options
     *            options, may be null.
     * @return instance of pattern converter.
     */
    public static DatePatternConverter newInstance(final String[] options) {
        return new DatePatternConverter(options);
    }

    private final Formatter formatter;

    /**
     * Private constructor.
     *
     * @param options
     *            options, may be null.
     */
    private DatePatternConverter(final String[] options) {
        super("Date", "date");

        final FixedDateFormat fixedDateFormat = FixedDateFormat.createIfSupported(options);
        if (fixedDateFormat != null) {
            formatter = createFormatter(fixedDateFormat);
        } else {
            formatter = createFormatter(options);
        }
        cachedTime = new AtomicReference<>(new CachedTime(System.currentTimeMillis()));
    }

    private static Formatter createFormatter(final FixedDateFormat fixedDateFormat) {
        return new FixedFormatter(fixedDateFormat);
    }

    private static Formatter createFormatter(final String[] options) {
        // if we get here, options is a non-null array with at least one element (first of which non-null)
        Objects.requireNonNull(options);
        if (options.length == 0) {
            throw new IllegalArgumentException("options array must have at least one element");
        }
        Objects.requireNonNull(options[0]);
        final String patternOption = options[0];
        if (UNIX_FORMAT.equals(patternOption)) {
            return new UnixFormatter();
        }
        if (UNIX_MILLIS_FORMAT.equals(patternOption)) {
            return new UnixMillisFormatter();
        }

        // if the option list contains a TZ option, then set it.
        TimeZone tz = null;
        if (options != null && options.length > 1) {
            tz = TimeZone.getTimeZone(options[1]);
        }

        try {
            final FastDateFormat tempFormat = FastDateFormat.getInstance(patternOption, tz);
            return new PatternFormatter(tempFormat);
        } catch (final IllegalArgumentException e) {
            LOGGER.warn("Could not instantiate FastDateFormat with pattern " + patternOption, e);

            // default to the DEFAULT format
            return createFormatter(FixedDateFormat.create(FixedFormat.DEFAULT));
        }
    }

    /**
     * Appends formatted date to string buffer.
     *
     * @param date
     *            date
     * @param toAppendTo
     *            buffer to which formatted date is appended.
     */
    public void format(final Date date, final StringBuilder toAppendTo) {
        format(date.getTime(), toAppendTo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder output) {
        format(event.getTimeMillis(), output);
    }
    
    public void format(final long timestampMillis, final StringBuilder output) {
        CachedTime cached = cachedTime.get();
        if (timestampMillis != cached.timestampMillis) {
            final CachedTime newTime = new CachedTime(timestampMillis);
            if (cachedTime.compareAndSet(cached, newTime)) {
                cached = newTime;
            } else {
                cached = cachedTime.get();
            }
        }
        output.append(cached.formatted);
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
     * Gets the pattern string describing this date format.
     *
     * @return the pattern string describing this date format.
     */
    public String getPattern() {
        return formatter.toPattern();
    }

}
