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
package org.apache.logging.log4j.core.util.internal.instant;

import static java.util.Objects.requireNonNull;

import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Supplier;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;
import org.apache.logging.log4j.core.util.datetime.FixedDateFormat;
import org.apache.logging.log4j.util.BiConsumer;

/**
 * A {@link InstantPatternFormatter} implementation using {@link FixedDateFormat} and {@link FastDateFormat} under the hood.
 */
@SuppressWarnings("deprecation")
final class InstantPatternLegacyFormatter implements InstantPatternFormatter {

    private final ChronoUnit precision;

    private final String pattern;

    private final Locale locale;

    private final TimeZone timeZone;

    private final BiConsumer<StringBuilder, Instant> formatter;

    InstantPatternLegacyFormatter(final String pattern, final Locale locale, final TimeZone timeZone) {
        this.precision = new InstantPatternDynamicFormatter(pattern, locale, timeZone).getPrecision();
        this.pattern = pattern;
        this.locale = locale;
        this.timeZone = timeZone;
        this.formatter = createFormatter(pattern, locale, timeZone);
    }

    private static BiConsumer<StringBuilder, Instant> createFormatter(
            final String pattern, final Locale locale, final TimeZone timeZone) {
        final FixedDateFormat fixedFormatter = FixedDateFormat.createIfSupported(pattern, timeZone.getID());
        return fixedFormatter != null
                ? adaptFixedFormatter(fixedFormatter)
                : createFastFormatter(pattern, locale, timeZone);
    }

    private static BiConsumer<StringBuilder, Instant> adaptFixedFormatter(final FixedDateFormat formatter) {
        final Supplier<char[]> charBufferSupplier = memoryEfficientInstanceSupplier(() -> {
            // Double size for locales with lengthy `DateFormatSymbols`
            return new char[formatter.getLength() << 1];
        });
        return (buffer, instant) -> {
            final char[] charBuffer = charBufferSupplier.get();
            final int length = formatter.formatInstant(instant, charBuffer, 0);
            buffer.append(charBuffer, 0, length);
        };
    }

    private static BiConsumer<StringBuilder, Instant> createFastFormatter(
            final String pattern, final Locale locale, final TimeZone timeZone) {
        final FastDateFormat formatter = FastDateFormat.getInstance(pattern, timeZone, locale);
        final Supplier<Calendar> calendarSupplier =
                memoryEfficientInstanceSupplier(() -> Calendar.getInstance(timeZone, locale));
        return (buffer, instant) -> {
            final Calendar calendar = calendarSupplier.get();
            calendar.setTimeInMillis(instant.getEpochMillisecond());
            formatter.format(calendar, buffer);
        };
    }

    private static <V> Supplier<V> memoryEfficientInstanceSupplier(final Supplier<V> supplier) {
        return Constants.ENABLE_THREADLOCALS ? ThreadLocal.withInitial(supplier)::get : supplier;
    }

    @Override
    public ChronoUnit getPrecision() {
        return precision;
    }

    @Override
    public void formatTo(final StringBuilder buffer, final Instant instant) {
        requireNonNull(buffer, "buffer");
        requireNonNull(instant, "instant");
        formatter.accept(buffer, instant);
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
}
