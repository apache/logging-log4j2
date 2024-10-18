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
package org.apache.logging.log4j.core.util.internal;

import static java.util.Objects.requireNonNull;

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.function.Supplier;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;
import org.apache.logging.log4j.core.util.datetime.FixedDateFormat;
import org.apache.logging.log4j.status.StatusLogger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Formats an {@link Instant} using the specified date & time formatting pattern.
 * This is a composite formatter trying to employ either {@link FixedDateFormat}, {@link FastDateFormat} or {@link DateTimeFormatter} in the given order due to performance reasons.
 * <p>
 * If the given pattern happens to be supported by either {@link FixedDateFormat} or {@link FastDateFormat}, yet they produce a different result compared to {@link DateTimeFormatter}, {@link DateTimeFormatter} will be employed instead.
 * Note that {@link FastDateFormat} supports at most millisecond precision.
 * </p>
 *
 * @since 2.25.0
 */
@NullMarked
final class InstantPatternDynamicFormatter implements InstantPatternFormatter {

    private static final StatusLogger LOGGER = StatusLogger.getLogger();

    /**
     * The list of formatter factories in decreasing efficiency order.
     */
    private static final FormatterFactory[] FORMATTER_FACTORIES = {
        new Log4jFixedFormatterFactory(), new Log4jFastFormatterFactory(), new JavaDateTimeFormatterFactory()
    };

    private final String pattern;

    private final Locale locale;

    private final TimeZone timeZone;

    private final Formatter formatter;

    InstantPatternDynamicFormatter(final String pattern, final Locale locale, final TimeZone timeZone) {
        this.pattern = pattern;
        this.locale = locale;
        this.timeZone = timeZone;
        this.formatter = Arrays.stream(FORMATTER_FACTORIES)
                .map(formatterFactory -> {
                    try {
                        return formatterFactory.createIfSupported(pattern, locale, timeZone);
                    } catch (final Exception error) {
                        LOGGER.warn("skipping the failed formatter factory `{}`", formatterFactory, error);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new AssertionError("could not find a matching formatter"));
    }

    @Override
    public void formatTo(final StringBuilder buffer, final Instant instant) {
        requireNonNull(buffer, "buffer");
        requireNonNull(instant, "instant");
        formatter.formatTo(buffer, instant);
    }

    @Override
    public ChronoUnit getPrecision() {
        return formatter.precision;
    }

    Class<?> getInternalImplementationClass() {
        return formatter.getInternalImplementationClass();
    }

    @Override
    public String getPattern() {
        return pattern;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    public TimeZone getTimeZone() {
        return timeZone;
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        buffer.append(InstantPatternDynamicFormatter.class.getSimpleName()).append('{');
        buffer.append("pattern=`").append(pattern).append('`');
        if (!locale.equals(Locale.getDefault())) {
            buffer.append(",locale=`").append(locale).append('`');
        }
        if (!timeZone.equals(TimeZone.getDefault())) {
            buffer.append(",timeZone=`").append(timeZone.getID()).append('`');
        }
        buffer.append('}');
        return buffer.toString();
    }

    private interface FormatterFactory {

        @Nullable
        Formatter createIfSupported(String pattern, Locale locale, TimeZone timeZone);
    }

    private abstract static class Formatter {

        private final ChronoUnit precision;

        Formatter(final ChronoUnit precision) {
            this.precision = precision;
        }

        abstract Class<?> getInternalImplementationClass();

        abstract void formatTo(StringBuilder buffer, Instant instant);
    }

    private static final class JavaDateTimeFormatterFactory implements FormatterFactory {

        @Override
        public Formatter createIfSupported(final String pattern, final Locale locale, final TimeZone timeZone) {
            return new JavaDateTimeFormatter(pattern, locale, timeZone);
        }
    }

    private static final class JavaDateTimeFormatter extends Formatter {

        private final DateTimeFormatter formatter;

        private final MutableInstant mutableInstant;

        private JavaDateTimeFormatter(final String pattern, final Locale locale, final TimeZone timeZone) {
            super(patternPrecision(pattern));
            this.formatter =
                    DateTimeFormatter.ofPattern(pattern).withLocale(locale).withZone(timeZone.toZoneId());
            this.mutableInstant = new MutableInstant();
        }

        @Override
        public Class<?> getInternalImplementationClass() {
            return DateTimeFormatter.class;
        }

        @Override
        public void formatTo(final StringBuilder buffer, final Instant instant) {
            if (instant instanceof MutableInstant) {
                formatMutableInstant((MutableInstant) instant, buffer);
            } else {
                formatInstant(instant, buffer);
            }
        }

        private void formatMutableInstant(final MutableInstant instant, final StringBuilder buffer) {
            formatter.formatTo(instant, buffer);
        }

        private void formatInstant(final Instant instant, final StringBuilder buffer) {
            mutableInstant.initFrom(instant);
            formatMutableInstant(mutableInstant, buffer);
        }
    }

    private static final class Log4jFastFormatterFactory implements FormatterFactory {

        @Override
        public Formatter createIfSupported(final String pattern, final Locale locale, final TimeZone timeZone) {
            final Log4jFastFormatter formatter = new Log4jFastFormatter(pattern, locale, timeZone);
            final boolean patternSupported = patternSupported(pattern, locale, timeZone, formatter);
            return patternSupported ? formatter : null;
        }
    }

    private static final class Log4jFastFormatter extends Formatter {

        private final FastDateFormat formatter;

        private final Supplier<Calendar> calendarSupplier;

        private Log4jFastFormatter(final String pattern, final Locale locale, final TimeZone timeZone) {
            super(effectivePatternPrecision(pattern));
            this.formatter = FastDateFormat.getInstance(pattern, timeZone, locale);
            this.calendarSupplier = memoryEfficientInstanceSupplier(() -> Calendar.getInstance(timeZone, locale));
        }

        @Override
        public Class<?> getInternalImplementationClass() {
            return FastDateFormat.class;
        }

        private static ChronoUnit effectivePatternPrecision(final String pattern) {
            final ChronoUnit patternPrecision = patternPrecision(pattern);
            // `FastDateFormat` doesn't support precision higher than millisecond
            return ChronoUnit.MILLIS.compareTo(patternPrecision) > 0 ? ChronoUnit.MILLIS : patternPrecision;
        }

        @Override
        public void formatTo(final StringBuilder stringBuilder, final Instant instant) {
            final Calendar calendar = calendarSupplier.get();
            calendar.setTimeInMillis(instant.getEpochMillisecond());
            formatter.format(calendar, stringBuilder);
        }
    }

    private static final class Log4jFixedFormatterFactory implements FormatterFactory {

        @Override
        @Nullable
        public Formatter createIfSupported(final String pattern, final Locale locale, final TimeZone timeZone) {
            final FixedDateFormat internalFormatter = FixedDateFormat.createIfSupported(pattern, timeZone.getID());
            if (internalFormatter == null) {
                return null;
            }
            final Log4jFixedFormatter formatter = new Log4jFixedFormatter(internalFormatter);
            final boolean patternSupported = patternSupported(pattern, locale, timeZone, formatter);
            return patternSupported ? formatter : null;
        }
    }

    private static final class Log4jFixedFormatter extends Formatter {

        private final FixedDateFormat formatter;

        private final Supplier<char[]> bufferSupplier;

        private Log4jFixedFormatter(final FixedDateFormat formatter) {
            super(patternPrecision(formatter.getFormat()));
            this.formatter = formatter;
            this.bufferSupplier = memoryEfficientInstanceSupplier(() -> {
                // Double size for locales with lengthy `DateFormatSymbols`
                return new char[formatter.getLength() << 1];
            });
        }

        @Override
        public Class<?> getInternalImplementationClass() {
            return FixedDateFormat.class;
        }

        @Override
        public void formatTo(final StringBuilder buffer, final Instant instant) {
            final char[] charBuffer = bufferSupplier.get();
            final int length = formatter.formatInstant(instant, charBuffer, 0);
            buffer.append(charBuffer, 0, length);
        }
    }

    private static <V> Supplier<V> memoryEfficientInstanceSupplier(final Supplier<V> supplier) {
        return Constants.ENABLE_THREADLOCALS ? ThreadLocal.withInitial(supplier)::get : supplier;
    }

    /**
     * Checks if the provided formatter output matches with the one generated by {@link DateTimeFormatter}.
     */
    private static boolean patternSupported(
            final String pattern, final Locale locale, final TimeZone timeZone, final Formatter formatter) {
        final DateTimeFormatter javaFormatter =
                DateTimeFormatter.ofPattern(pattern).withLocale(locale).withZone(timeZone.toZoneId());
        final MutableInstant instant = new MutableInstant();
        instant.initFromEpochSecond(
                // 2021-05-17 21:41:10
                1_621_280_470,
                // Using the highest nanosecond precision possible to
                // differentiate formatters only supporting millisecond
                // precision.
                123_456_789);
        final String expectedFormat = javaFormatter.format(instant);
        final StringBuilder buffer = new StringBuilder();
        formatter.formatTo(buffer, instant);
        final String actualFormat = buffer.toString();
        return expectedFormat.equals(actualFormat);
    }

    /**
     * @param pattern a date & time formatting pattern
     * @return the time precision of the output when formatted using the specified {@code pattern}
     */
    static ChronoUnit patternPrecision(final String pattern) {
        // Remove text blocks
        final String trimmedPattern = pattern.replaceAll("'[^']*'", "");
        // A single `S` (fraction-of-second) outputs nanosecond precision
        if (trimmedPattern.matches(".*(?<!S)S(?!S).*")
                // 3 consequent `S` characters output millisecond precision.
                // We will treat 4 or more consequent `S` characters as they output nanosecond precision too.
                || trimmedPattern.matches(".*(?<!S)S{4,}.*")
                // `n` (nano-of-second) and `N` (nano-of-day) always output nanosecond precision.
                // This is independent of how many times they occur sequentially.
                || trimmedPattern.contains("n")
                || trimmedPattern.contains("N")) {
            return ChronoUnit.NANOS;
        }
        // A single `S` (fraction-of-second) outputs nanosecond precision.
        // 4 consequent `S` outputs microsecond precision.
        // We will treat 2 to 3 consequent `S` characters as they output millisecond precision.
        else if (trimmedPattern.matches(".*(?<!S)S{2,3}(?!S).*")
                // `A` (milli-of-day) outputs millisecond precision.
                || trimmedPattern.contains("A")) {
            return ChronoUnit.MILLIS;
        }
        // We will treat the rest as they output second precision
        return ChronoUnit.SECONDS;
    }
}
