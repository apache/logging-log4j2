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

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.core.util.datetime.FixedDateFormat;
import org.apache.logging.log4j.core.util.internal.instant.InstantFormatter;
import org.apache.logging.log4j.core.util.internal.instant.InstantNumberFormatter;
import org.apache.logging.log4j.core.util.internal.instant.InstantPatternFormatter;
import org.apache.logging.log4j.util.PerformanceSensitive;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Converts and formats the event's date in a StringBuilder.
 */
@SuppressWarnings("deprecation")
@Plugin(name = "DatePatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"d", "date"})
@PerformanceSensitive("allocation")
@NullMarked
public final class DatePatternConverter extends LogEventPatternConverter implements ArrayPatternConverter {

    private static final String CLASS_NAME = DatePatternConverter.class.getSimpleName();

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
        return InstantPatternFormatter.newBuilder()
                .setPattern(NamedInstantPattern.DEFAULT.getPattern())
                .build();
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
        return options != null && options.length > 0 && options[0] != null
                ? decodeNamedPattern(options[0])
                : NamedInstantPattern.DEFAULT.getPattern();
    }

    /**
     * Decodes {@link FixedDateFormat} named patterns into their corresponding {@link DateTimeFormatter} representations.
     * <p>
     * In version {@code 2.25.0}, {@link FixedDateFormat} and {@link FastDateFormat} are deprecated in favor of {@link InstantPatternFormatter}.
     * We introduced this method to keep backward compatibility with the named patterns provided by {@link FixedDateFormat}.
     * </p>
     *
     * @param pattern a user provided date & time formatting pattern
     * @return the transformed formatting pattern where {@link FixedDateFormat} named patterns are replaced with their corresponding {@link DateTimeFormatter} representations
     * @since 2.25.0
     */
    static String decodeNamedPattern(final String pattern) {
        // See `NamedInstantPattern.getLegacyPattern()`
        // for the difference between legacy and `DateTimeFormatter` patterns.
        try {
            NamedInstantPattern namedInstantPattern = NamedInstantPattern.valueOf(pattern);
            return InstantPatternFormatter.LEGACY_FORMATTERS_ENABLED
                    ? namedInstantPattern.getLegacyPattern()
                    : namedInstantPattern.getPattern();
        } catch (IllegalArgumentException ignored) {
            return pattern;
        }
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

    /**
     * Formats the given date to the provided buffer.
     *
     * @param date a date
     * @param buffer a buffer to append to
     * @deprecated Starting with version {@code 2.25.0}, this method is deprecated and planned to be removed in the next major release.
     */
    @Deprecated
    public void format(final Date date, final StringBuilder buffer) {
        format(date.getTime(), buffer);
    }

    @Override
    public void format(final LogEvent event, final StringBuilder output) {
        format(event.getInstant(), output);
    }

    /**
     * Formats the given epoch milliseconds to the provided buffer.
     *
     * @param epochMillis epoch milliseconds
     * @param buffer a buffer to append to
     * @deprecated Starting with version {@code 2.25.0}, this method is deprecated and planned to be removed in the next major release.
     */
    @Deprecated
    public void format(final long epochMillis, final StringBuilder buffer) {
        final MutableInstant instant = new MutableInstant();
        instant.initFromEpochMilli(epochMillis, 0);
        format(instant, buffer);
    }

    /**
     * Formats the given instant to the provided buffer.
     *
     * @param instant an instant
     * @param buffer a buffer to append to
     * @deprecated Starting with version {@code 2.25.0}, this method is deprecated and planned to be removed in the next major release.
     */
    @Deprecated
    public void format(final Instant instant, final StringBuilder buffer) {
        formatter.formatTo(buffer, instant);
    }

    @Override
    public void format(@Nullable final Object object, final StringBuilder buffer) {
        requireNonNull(buffer, "buffer");
        if (object == null) {
            return;
        }
        if (object instanceof LogEvent) {
            format((LogEvent) object, buffer);
        } else if (object instanceof Date) {
            format((Date) object, buffer);
        } else if (object instanceof Instant) {
            format((Instant) object, buffer);
        } else if (object instanceof Long) {
            format((long) object, buffer);
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
                if (object instanceof Date) {
                    format((Date) object, buffer);
                    break;
                }
            }
        }
    }

    /**
     * @return the pattern string describing this date format or {@code null} if the format does not have a pattern.
     */
    public String getPattern() {
        return (formatter instanceof InstantPatternFormatter)
                ? ((InstantPatternFormatter) formatter).getPattern()
                : null;
    }

    /**
     * @return the time zone used by this date format
     */
    public TimeZone getTimeZone() {
        return (formatter instanceof InstantPatternFormatter)
                ? ((InstantPatternFormatter) formatter).getTimeZone()
                : null;
    }
}
