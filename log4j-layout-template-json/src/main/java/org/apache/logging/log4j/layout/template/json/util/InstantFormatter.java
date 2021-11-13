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
package org.apache.logging.log4j.layout.template.json.util;

import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;
import org.apache.logging.log4j.core.util.datetime.FixedDateFormat;
import org.apache.logging.log4j.util.Strings;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

/**
 * A composite {@link Instant} formatter trying to employ either
 * {@link FixedDateFormat}, {@link FastDateFormat}, or {@link DateTimeFormatter}
 * in the given order due to performance reasons.
 * <p>
 * Note that {@link FixedDateFormat} and {@link FastDateFormat} only support
 * millisecond precision. If the pattern asks for a higher precision,
 * {@link DateTimeFormatter} will be employed, which is significantly slower.
 */
public final class InstantFormatter {

    /**
     * The list of formatter factories in decreasing efficiency order.
     */
    private static final FormatterFactory[] FORMATTER_FACTORIES = {
            new Log4jFixedFormatterFactory(),
            new Log4jFastFormatterFactory(),
            new JavaDateTimeFormatterFactory()
    };

    private final Formatter formatter;

    private InstantFormatter(final Builder builder) {
        this.formatter = Arrays
                .stream(FORMATTER_FACTORIES)
                .map(formatterFactory -> formatterFactory.createIfSupported(
                        builder.getPattern(),
                        builder.getLocale(),
                        builder.getTimeZone()))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new AssertionError("could not find a matching formatter"));
    }

    public String format(final Instant instant) {
        Objects.requireNonNull(instant, "instant");
        final StringBuilder stringBuilder = new StringBuilder();
        formatter.format(instant, stringBuilder);
        return stringBuilder.toString();
    }

    public void format(final Instant instant, final StringBuilder stringBuilder) {
        Objects.requireNonNull(instant, "instant");
        Objects.requireNonNull(stringBuilder, "stringBuilder");
        formatter.format(instant, stringBuilder);
    }

    /**
     * Checks if the given {@link Instant}s are equal from the point of view of
     * the employed formatter.
     * <p>
     * This method should be preferred over {@link Instant#equals(Object)}. For
     * instance, {@link FixedDateFormat} and {@link FastDateFormat} discard
     * nanoseconds, hence, from their point of view, two different
     * {@link Instant}s are equal if they match up to millisecond precision.
     */
    public boolean isInstantMatching(final Instant instant1, final Instant instant2) {
        return formatter.isInstantMatching(instant1, instant2);
    }

    public Class<?> getInternalImplementationClass() {
        return formatter.getInternalImplementationClass();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {

        private String pattern;

        private Locale locale = Locale.getDefault();

        private TimeZone timeZone = TimeZone.getDefault();

        private Builder() {}

        public String getPattern() {
            return pattern;
        }

        public Builder setPattern(final String pattern) {
            this.pattern = pattern;
            return this;
        }

        public Locale getLocale() {
            return locale;
        }

        public Builder setLocale(final Locale locale) {
            this.locale = locale;
            return this;
        }

        public TimeZone getTimeZone() {
            return timeZone;
        }

        public Builder setTimeZone(final TimeZone timeZone) {
            this.timeZone = timeZone;
            return this;
        }

        public InstantFormatter build() {
            validate();
            return new InstantFormatter(this);
        }

        private void validate() {
            if (Strings.isBlank(pattern)) {
                throw new IllegalArgumentException("blank pattern");
            }
            Objects.requireNonNull(locale, "locale");
            Objects.requireNonNull(timeZone, "timeZone");
        }

    }

    private interface FormatterFactory {

        Formatter createIfSupported(
                String pattern,
                Locale locale,
                TimeZone timeZone);

    }

    private interface Formatter {

        Class<?> getInternalImplementationClass();

        void format(Instant instant, StringBuilder stringBuilder);

        boolean isInstantMatching(Instant instant1, Instant instant2);

    }

    private static final class JavaDateTimeFormatterFactory implements FormatterFactory {

        @Override
        public Formatter createIfSupported(
                final String pattern,
                final Locale locale,
                final TimeZone timeZone) {
            return new JavaDateTimeFormatter(pattern, locale, timeZone);
        }

    }

    private static final class JavaDateTimeFormatter implements Formatter {

        private final DateTimeFormatter formatter;

        private final MutableInstant mutableInstant;

        private JavaDateTimeFormatter(
                final String pattern,
                final Locale locale,
                final TimeZone timeZone) {
            this.formatter = DateTimeFormatter
                    .ofPattern(pattern)
                    .withLocale(locale)
                    .withZone(timeZone.toZoneId());
            this.mutableInstant = new MutableInstant();
        }

        @Override
        public Class<?> getInternalImplementationClass() {
            return DateTimeFormatter.class;
        }

        @Override
        public void format(
                final Instant instant,
                final StringBuilder stringBuilder) {
            if (instant instanceof MutableInstant) {
                formatMutableInstant((MutableInstant) instant, stringBuilder);
            } else {
                formatInstant(instant, stringBuilder);
            }
        }

        private void formatMutableInstant(
                final MutableInstant instant,
                final StringBuilder stringBuilder) {
            formatter.formatTo(instant, stringBuilder);
        }

        private void formatInstant(
                final Instant instant,
                final StringBuilder stringBuilder) {
            mutableInstant.initFrom(instant);
            formatMutableInstant(mutableInstant, stringBuilder);
        }

        @Override
        public boolean isInstantMatching(final Instant instant1, final Instant instant2) {
            return instant1.getEpochSecond() == instant2.getEpochSecond() &&
                    instant1.getNanoOfSecond() == instant2.getNanoOfSecond();
        }

    }

    private static final class Log4jFastFormatterFactory implements FormatterFactory {

        @Override
        public Formatter createIfSupported(
                final String pattern,
                final Locale locale,
                final TimeZone timeZone) {
            final Log4jFastFormatter formatter =
                    new Log4jFastFormatter(pattern, locale, timeZone);
            final boolean patternSupported =
                    patternSupported(pattern, locale, timeZone, formatter);
            return patternSupported ? formatter : null;
        }

    }

    private static final class Log4jFastFormatter implements Formatter {

        private final FastDateFormat formatter;

        private final Calendar calendar;

        private Log4jFastFormatter(
                final String pattern,
                final Locale locale,
                final TimeZone timeZone) {
            this.formatter = FastDateFormat.getInstance(pattern, timeZone, locale);
            this.calendar = Calendar.getInstance(timeZone, locale);
        }

        @Override
        public Class<?> getInternalImplementationClass() {
            return FastDateFormat.class;
        }

        @Override
        public void format(
                final Instant instant,
                final StringBuilder stringBuilder) {
            calendar.setTimeInMillis(instant.getEpochMillisecond());
            formatter.format(calendar, stringBuilder);
        }

        @Override
        public boolean isInstantMatching(final Instant instant1, final Instant instant2) {
            return instant1.getEpochMillisecond() == instant2.getEpochMillisecond();
        }

    }

    private static final class Log4jFixedFormatterFactory implements FormatterFactory {

        @Override
        public Formatter createIfSupported(
                final String pattern,
                final Locale locale,
                final TimeZone timeZone) {
            final FixedDateFormat internalFormatter =
                    FixedDateFormat.createIfSupported(pattern, timeZone.getID());
            if (internalFormatter == null) {
                return null;
            }
            final Log4jFixedFormatter formatter =
                    new Log4jFixedFormatter(internalFormatter);
            final boolean patternSupported =
                    patternSupported(pattern, locale, timeZone, formatter);
            return patternSupported ? formatter : null;
        }

    }

    private static final class Log4jFixedFormatter implements Formatter {

        private final FixedDateFormat formatter;

        private final char[] buffer;

        private Log4jFixedFormatter(final FixedDateFormat formatter) {
            this.formatter = formatter;
            this.buffer = new char[formatter.getFormat().length()];
        }

        @Override
        public Class<?> getInternalImplementationClass() {
            return FixedDateFormat.class;
        }

        @Override
        public void format(
                final Instant instant,
                final StringBuilder stringBuilder) {
            final int length = formatter.formatInstant(instant, buffer, 0);
            stringBuilder.append(buffer, 0, length);
        }

        @Override
        public boolean isInstantMatching(final Instant instant1, final Instant instant2) {
            return formatter.isEquivalent(
                    instant1.getEpochSecond(),
                    instant1.getNanoOfSecond(),
                    instant2.getEpochSecond(),
                    instant2.getNanoOfSecond());
        }

    }

    /**
     * Checks if the provided formatter output matches with the one generated by
     * {@link DateTimeFormatter}.
     */
    private static boolean patternSupported(
            final String pattern,
            final Locale locale,
            final TimeZone timeZone,
            final Formatter formatter) {
        final DateTimeFormatter javaFormatter = DateTimeFormatter
                .ofPattern(pattern)
                .withLocale(locale)
                .withZone(timeZone.toZoneId());
        final MutableInstant instant = new MutableInstant();
        instant.initFromEpochSecond(
                // 2021-05-17 21:41:10
                1621280470,
                // Using the highest nanosecond precision possible to
                // differentiate formatters only supporting millisecond
                // precision.
                123_456_789);
        final String expectedFormat = javaFormatter.format(instant);
        final StringBuilder stringBuilder = new StringBuilder();
        formatter.format(instant, stringBuilder);
        final String actualFormat = stringBuilder.toString();
        return expectedFormat.equals(actualFormat);
    }

}
