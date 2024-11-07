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

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.core.util.internal.instant.InstantFormatter;
import org.apache.logging.log4j.core.util.internal.instant.InstantNumberFormatter;
import org.apache.logging.log4j.core.util.internal.instant.InstantPatternFormatter;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.util.PerformanceSensitive;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Converts and formats the event's date in a StringBuilder.
 */
@Namespace(PatternConverter.CATEGORY)
@Plugin("DatePatternConverter")
@ConverterKeys({"d", "date"})
@PerformanceSensitive("allocation")
@NullMarked
public final class DatePatternConverter extends LogEventPatternConverter implements ArrayPatternConverter {

    private static final String CLASS_NAME = DatePatternConverter.class.getSimpleName();

    private static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";

    private final InstantFormatter formatter;

    private DatePatternConverter(@Nullable final String[] options) {
        super("Date", "date");
        this.formatter = createFormatter(options);
    }

    private static InstantFormatter createFormatter(@Nullable final String[] options) {
        try {
            return createFormatterUnsafely(options);
        } catch (final Exception error) {
            logOptionReadFailure(options, error, "failed for options: {}, falling back to the default instance");
        }
        return InstantPatternFormatter.newBuilder().setPattern(DEFAULT_PATTERN).build();
    }

    private static InstantFormatter createFormatterUnsafely(@Nullable final String[] options) {

        // Read options
        final String pattern = readPattern(options);
        final TimeZone timeZone = readTimeZone(options);
        final Locale locale = readLocale(options);

        // Is it epoch seconds?
        if ("UNIX".equals(pattern)) {
            return InstantNumberFormatter.EPOCH_SECONDS_ROUNDED;
        }

        // Is it epoch milliseconds?
        if ("UNIX_MILLIS".equals(pattern)) {
            return InstantNumberFormatter.EPOCH_MILLIS_ROUNDED;
        }

        return InstantPatternFormatter.newBuilder()
                .setPattern(pattern)
                .setTimeZone(timeZone)
                .setLocale(locale)
                .build();
    }

    private static String readPattern(@Nullable final String[] options) {
        return options != null && options.length > 0 && options[0] != null ? options[0] : DEFAULT_PATTERN;
    }

    private static TimeZone readTimeZone(@Nullable final String[] options) {
        try {
            if (options != null && options.length > 1 && options[1] != null) {
                return TimeZone.getTimeZone(options[1]);
            }
        } catch (final Exception error) {
            logOptionReadFailure(
                    options,
                    error,
                    "failed to read the time zone at index 1 of options: {}, falling back to the default time zone");
        }
        return TimeZone.getDefault();
    }

    private static Locale readLocale(@Nullable final String[] options) {
        try {
            if (options != null && options.length > 2 && options[2] != null) {
                return Locale.forLanguageTag(options[2]);
            }
        } catch (final Exception error) {
            logOptionReadFailure(
                    options,
                    error,
                    "failed to read the locale at index 2 of options: {}, falling back to the default locale");
        }
        return Locale.getDefault();
    }

    private static void logOptionReadFailure(final String[] options, final Exception error, final String message) {
        if (LOGGER.isWarnEnabled()) {
            final String quotedOptions =
                    Arrays.stream(options).map(option -> '`' + option + '`').collect(Collectors.joining(", "));
            LOGGER.warn("[{}] " + message, CLASS_NAME, quotedOptions, error);
        }
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

    @Override
    public void format(final LogEvent event, final StringBuilder buffer) {
        formatter.formatTo(buffer, event.getInstant());
    }

    @Override
    public void format(@Nullable final Object object, final StringBuilder buffer) {
        requireNonNull(buffer, "buffer");
        if (object == null) {
            return;
        }
        if (object instanceof LogEvent logEvent) {
            format(logEvent, buffer);
        } else if (object instanceof Date date) {
            final MutableInstant instant = new MutableInstant();
            instant.initFromEpochMilli(date.getTime(), 0);
            formatter.formatTo(buffer, instant);
        } else if (object instanceof Instant instant) {
            formatter.formatTo(buffer, instant);
        } else if (object instanceof Long epochMillis) {
            final MutableInstant instant = new MutableInstant();
            instant.initFromEpochMilli(epochMillis, 0);
            formatter.formatTo(buffer, instant);
        }
        LOGGER.warn(
                "[{}]: unsupported object type `{}`",
                CLASS_NAME,
                object.getClass().getCanonicalName());
    }

    @Override
    public void format(final StringBuilder buffer, @Nullable final Object... objects) {
        requireNonNull(buffer, "buffer");
        if (objects != null) {
            for (final Object object : objects) {
                if (object instanceof Date date) {
                    format(date, buffer);
                    break;
                }
            }
        }
    }

    /**
     * @return the pattern string describing this date format or {@code null} if the format does not have a pattern.
     */
    @Nullable
    public String getPattern() {
        return (formatter instanceof InstantPatternFormatter)
                ? ((InstantPatternFormatter) formatter).getPattern()
                : null;
    }

    /**
     * @return the time zone used by this date format
     */
    @Nullable
    public TimeZone getTimeZone() {
        return (formatter instanceof InstantPatternFormatter)
                ? ((InstantPatternFormatter) formatter).getTimeZone()
                : null;
    }
}
