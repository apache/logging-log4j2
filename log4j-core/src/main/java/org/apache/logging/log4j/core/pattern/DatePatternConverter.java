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

import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;
import org.apache.logging.log4j.core.util.datetime.FixedDateFormat;
import org.apache.logging.log4j.core.util.datetime.FixedDateFormat.FixedFormat;
import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * Converts and formats the event's date in a StringBuilder.
 */
@Plugin(name = "DatePatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"d", "date"})
@PerformanceSensitive("allocation")
public final class DatePatternConverter extends LogEventPatternConverter implements ArrayPatternConverter {

    private abstract static class Formatter {
        long previousTime; // for ThreadLocal caching mode
        int nanos;

        abstract String format(final Instant instant);

        abstract void formatToBuffer(final Instant instant, StringBuilder destination);

        public String toPattern() {
            return null;
        }

        public TimeZone getTimeZone() {
            return TimeZone.getDefault();
        }
    }

    private static final class PatternFormatter extends Formatter {
        private final FastDateFormat fastDateFormat;

        // this field is only used in ThreadLocal caching mode
        private final StringBuilder cachedBuffer = new StringBuilder(64);

        PatternFormatter(final FastDateFormat fastDateFormat) {
            this.fastDateFormat = fastDateFormat;
        }

        @Override
        String format(final Instant instant) {
            return fastDateFormat.format(instant.getEpochMillisecond());
        }

        @Override
        void formatToBuffer(final Instant instant, final StringBuilder destination) {
            final long timeMillis = instant.getEpochMillisecond();
            if (previousTime != timeMillis) {
                cachedBuffer.setLength(0);
                fastDateFormat.format(timeMillis, cachedBuffer);
            }
            destination.append(cachedBuffer);
        }

        @Override
        public String toPattern() {
            return fastDateFormat.getPattern();
        }

        @Override
        public TimeZone getTimeZone() {
            return fastDateFormat.getTimeZone();
        }
    }

    private static final class FixedFormatter extends Formatter {
        private final FixedDateFormat fixedDateFormat;

        // below fields are only used in ThreadLocal caching mode
        private final char[] cachedBuffer = new char[70]; // max length of formatted date-time in any format < 70
        private int length = 0;

        FixedFormatter(final FixedDateFormat fixedDateFormat) {
            this.fixedDateFormat = fixedDateFormat;
        }

        @Override
        String format(final Instant instant) {
            return fixedDateFormat.formatInstant(instant);
        }

        @Override
        void formatToBuffer(final Instant instant, final StringBuilder destination) {
            final long epochSecond = instant.getEpochSecond();
            final int nanoOfSecond = instant.getNanoOfSecond();
            if (!fixedDateFormat.isEquivalent(previousTime, nanos, epochSecond, nanoOfSecond)) {
                length = fixedDateFormat.formatInstant(instant, cachedBuffer, 0);
                previousTime = epochSecond;
                nanos = nanoOfSecond;
            }
            destination.append(cachedBuffer, 0, length);
        }

        @Override
        public String toPattern() {
            return fixedDateFormat.getFormat();
        }

        @Override
        public TimeZone getTimeZone() {
            return fixedDateFormat.getTimeZone();
        }
    }

    private static final class UnixFormatter extends Formatter {

        @Override
        String format(final Instant instant) {
            return Long.toString(instant.getEpochSecond());
        }

        @Override
        void formatToBuffer(final Instant instant, final StringBuilder destination) {
            destination.append(instant.getEpochSecond()); // no need for caching
        }
    }

    private static final class UnixMillisFormatter extends Formatter {

        @Override
        String format(final Instant instant) {
            return Long.toString(instant.getEpochMillisecond());
        }

        @Override
        void formatToBuffer(final Instant instant, final StringBuilder destination) {
            destination.append(instant.getEpochMillisecond()); // no need for caching
        }
    }

    private final class CachedTime {
        public long epochSecond;
        public int nanoOfSecond;
        public String formatted;

        public CachedTime(final Instant instant) {
            this.epochSecond = instant.getEpochSecond();
            this.nanoOfSecond = instant.getNanoOfSecond();
            this.formatted = formatter.format(instant);
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

    private final String[] options;
    private final ThreadLocal<MutableInstant> threadLocalMutableInstant = new ThreadLocal<>();
    private final ThreadLocal<Formatter> threadLocalFormatter = new ThreadLocal<>();
    private final AtomicReference<CachedTime> cachedTime;
    private final Formatter formatter;

    /**
     * Private constructor.
     *
     * @param options options, may be null.
     */
    private DatePatternConverter(final String[] options) {
        super("Date", "date");
        this.options = options == null ? null : Arrays.copyOf(options, options.length);
        this.formatter = createFormatter(options);
        cachedTime = new AtomicReference<>(fromEpochMillis(System.currentTimeMillis()));
    }

    private CachedTime fromEpochMillis(final long epochMillis) {
        final MutableInstant temp = new MutableInstant();
        temp.initFromEpochMilli(epochMillis, 0);
        return new CachedTime(temp);
    }

    private Formatter createFormatter(final String[] options) {
        final FixedDateFormat fixedDateFormat = FixedDateFormat.createIfSupported(options);
        if (fixedDateFormat != null) {
            return createFixedFormatter(fixedDateFormat);
        }
        return createNonFixedFormatter(options);
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

    private static Formatter createFixedFormatter(final FixedDateFormat fixedDateFormat) {
        return new FixedFormatter(fixedDateFormat);
    }

    private static Formatter createNonFixedFormatter(final String[] options) {
        // if we get here, options is a non-null array with at least one element (first of which non-null)
        Objects.requireNonNull(options);
        if (options.length == 0) {
            throw new IllegalArgumentException("Options array must have at least one element");
        }
        Objects.requireNonNull(options[0]);
        final String patternOption = options[0];
        if (UNIX_FORMAT.equals(patternOption)) {
            return new UnixFormatter();
        }
        if (UNIX_MILLIS_FORMAT.equals(patternOption)) {
            return new UnixMillisFormatter();
        }
        // LOG4J2-1149: patternOption may be a name (if a time zone was specified)
        final FixedDateFormat.FixedFormat fixedFormat = FixedDateFormat.FixedFormat.lookup(patternOption);
        final String pattern = fixedFormat == null ? patternOption : fixedFormat.getPattern();

        // if the option list contains a TZ option, then set it.
        TimeZone tz = null;
        if (options.length > 1 && options[1] != null) {
            tz = TimeZone.getTimeZone(options[1]);
        }

        Locale locale = null;
        if (options.length > 2 && options[2] != null) {
            locale = Locale.forLanguageTag(options[2]);
        }

        try {
            final FastDateFormat tempFormat = FastDateFormat.getInstance(pattern, tz, locale);
            return new PatternFormatter(tempFormat);
        } catch (final IllegalArgumentException e) {
            LOGGER.warn("Could not instantiate FastDateFormat with pattern " + pattern, e);

            // default to the DEFAULT format
            return createFixedFormatter(FixedDateFormat.create(FixedFormat.DEFAULT, tz));
        }
    }

    /**
     * Appends formatted date to string buffer.
     *
     * @param date date
     * @param toAppendTo buffer to which formatted date is appended.
     */
    public void format(final Date date, final StringBuilder toAppendTo) {
        format(date.getTime(), toAppendTo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder output) {
        format(event.getInstant(), output);
    }

    public void format(final long epochMilli, final StringBuilder output) {
        final MutableInstant instant = getMutableInstant();
        instant.initFromEpochMilli(epochMilli, 0);
        format(instant, output);
    }

    private MutableInstant getMutableInstant() {
        if (Constants.ENABLE_THREADLOCALS) {
            MutableInstant result = threadLocalMutableInstant.get();
            if (result == null) {
                result = new MutableInstant();
                threadLocalMutableInstant.set(result);
            }
            return result;
        }
        return new MutableInstant();
    }

    public void format(final Instant instant, final StringBuilder output) {
        if (Constants.ENABLE_THREADLOCALS) {
            formatWithoutAllocation(instant, output);
        } else {
            formatWithoutThreadLocals(instant, output);
        }
    }

    private void formatWithoutAllocation(final Instant instant, final StringBuilder output) {
        getThreadLocalFormatter().formatToBuffer(instant, output);
    }

    private Formatter getThreadLocalFormatter() {
        Formatter result = threadLocalFormatter.get();
        if (result == null) {
            result = createFormatter(options);
            threadLocalFormatter.set(result);
        }
        return result;
    }

    private void formatWithoutThreadLocals(final Instant instant, final StringBuilder output) {
        final CachedTime effective;
        final CachedTime cached = cachedTime.get();
        if (instant.getEpochSecond() != cached.epochSecond || instant.getNanoOfSecond() != cached.nanoOfSecond) {
            effective = new CachedTime(instant);
            cachedTime.compareAndSet(cached, effective);
        } else {
            effective = cached;
        }
        output.append(effective.formatted);
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
     * @return the pattern string describing this date format or {@code  null} if the format does not have a pattern.
     */
    public String getPattern() {
        return formatter.toPattern();
    }

    /**
     * Gets the timezone used by this date format.
     *
     * @return the timezone used by this date format.
     */
    public TimeZone getTimeZone() {
        return formatter.getTimeZone();
    }
}
