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
package org.apache.logging.log4j.perf.jmh.instant;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.core.util.datetime.FastDatePrinter;
import org.apache.logging.log4j.core.util.datetime.FixedDateFormat;
import org.apache.logging.log4j.core.util.internal.instant.InstantPatternFormatter;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Compares {@link MutableInstant} formatting efficiency of {@link InstantPatternFormatter}, {@link FastDatePrinter}, {@link FixedDateFormat}, and {@link DateTimeFormatter}.
 * <p>
 * The major formatting efficiency is mostly provided by caching, i.e., reusing the earlier formatter output if timestamps match.
 * We deliberately exclude this optimization (by means of always distinct instants), since it is applicable to all formatters.
 * This benchmark rather focuses on only and only the formatting efficiency.
 * </p>
 *
 * @see InstantPatternFormatterImpactBenchmark for the performance impact of different date & time formatters on a typical layout
 */
@State(Scope.Thread)
@SuppressWarnings("deprecation")
public class InstantPatternFormatterBenchmark {

    static final Locale LOCALE = Locale.US;

    static final TimeZone TIME_ZONE = TimeZone.getTimeZone("UTC");

    private static final MutableInstant[] INSTANTS = createInstants();

    private static MutableInstant[] createInstants() {
        final Instant initInstant = Instant.parse("2020-05-14T10:44:23.901Z");
        MutableInstant[] instants = IntStream.range(0, 1_000)
                .mapToObj((final int index) -> {
                    final Instant instant = initInstant.plusMillis(index).plusNanos(1);
                    final MutableInstant mutableInstant = new MutableInstant();
                    mutableInstant.initFromEpochSecond(instant.getEpochSecond(), instant.getNano());
                    return mutableInstant;
                })
                .toArray(MutableInstant[]::new);
        validateInstants(instants);
        return instants;
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    static <I extends org.apache.logging.log4j.core.time.Instant> void validateInstants(final I[] instants) {

        // Find the instant offset
        final Supplier<LongStream> millisStreamSupplier = () ->
                Arrays.stream(instants).mapToLong(org.apache.logging.log4j.core.time.Instant::getEpochMillisecond);
        final long minMillis = millisStreamSupplier.get().min().getAsLong();
        final long maxMillis = millisStreamSupplier.get().max().getAsLong();
        final long offMillis = maxMillis - minMillis;

        // Validate for `FixedDateFormat`
        if (TimeUnit.DAYS.toMillis(1) <= offMillis) {
            final String message = String.format(
                    "instant samples must be of the same day to exploit the `%s` caching",
                    FixedDateFormat.class.getSimpleName());
            throw new IllegalStateException(message);
        }

        // Validate for `InstantPatternDynamicFormatter`
        if (TimeUnit.MINUTES.toMillis(1) <= offMillis) {
            final String message = String.format(
                    "instant samples must be of the same week to exploit the `%s` caching",
                    InstantPatternFormatter.class.getSimpleName());
            throw new IllegalStateException(message);
        }
    }

    private static final Formatters DATE_TIME_FORMATTERS = new Formatters("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private static final Formatters TIME_FORMATTERS = new Formatters("HH:mm:ss.SSS");

    static final class Formatters {

        private final String pattern;

        final FastDatePrinter fastFormatter;

        final FixedDateFormat fixedFormatter;

        final InstantPatternFormatter instantFormatter;

        final DateTimeFormatter javaFormatter;

        Formatters(final String pattern) {
            this.pattern = pattern;
            this.fastFormatter = new FastDatePrinter(pattern, TIME_ZONE, LOCALE) {};
            this.fixedFormatter = FixedDateFormat.createIfSupported(pattern, TIME_ZONE.getID());
            if (fixedFormatter == null) {
                final String message = String.format(
                        "couldn't create `%s` for pattern `%s` and time zone `%s`",
                        FixedDateFormat.class.getSimpleName(), pattern, TIME_ZONE.getID());
                throw new IllegalStateException(message);
            }
            this.instantFormatter = InstantPatternFormatter.newBuilder()
                    .setPattern(pattern)
                    .setLocale(LOCALE)
                    .setTimeZone(TIME_ZONE)
                    .setCachingEnabled(false)
                    .build();
            this.javaFormatter = DateTimeFormatter.ofPattern(pattern)
                    .withZone(TIME_ZONE.toZoneId())
                    .withLocale(LOCALE);
        }
    }

    private final StringBuilder stringBuilder =
            new StringBuilder(Math.max(DATE_TIME_FORMATTERS.pattern.length(), TIME_FORMATTERS.pattern.length()) * 2);

    private final char[] charBuffer = new char[stringBuilder.capacity()];

    private final Calendar calendar = Calendar.getInstance(TIME_ZONE, LOCALE);

    @Benchmark
    public void instantFormatter_dateTime(final Blackhole blackhole) {
        instantFormatter(blackhole, DATE_TIME_FORMATTERS.instantFormatter);
    }

    @Benchmark
    public void instantFormatter_time(final Blackhole blackhole) {
        instantFormatter(blackhole, TIME_FORMATTERS.instantFormatter);
    }

    private void instantFormatter(final Blackhole blackhole, final InstantPatternFormatter formatter) {
        for (final MutableInstant instant : INSTANTS) {
            stringBuilder.setLength(0);
            formatter.formatTo(stringBuilder, instant);
            blackhole.consume(stringBuilder.length());
        }
    }

    @Benchmark
    public void fastFormatter_dateTime(final Blackhole blackhole) {
        fastFormatter(blackhole, DATE_TIME_FORMATTERS.fastFormatter);
    }

    @Benchmark
    public void fastFormatter_time(final Blackhole blackhole) {
        fastFormatter(blackhole, TIME_FORMATTERS.fastFormatter);
    }

    private void fastFormatter(final Blackhole blackhole, final FastDatePrinter formatter) {
        for (final MutableInstant instant : INSTANTS) {
            stringBuilder.setLength(0);
            calendar.setTimeInMillis(instant.getEpochMillisecond());
            formatter.format(calendar, stringBuilder);
            blackhole.consume(stringBuilder.length());
        }
    }

    @Benchmark
    public void fixedFormatter_dateTime(final Blackhole blackhole) {
        fixedFormatter(blackhole, DATE_TIME_FORMATTERS.fixedFormatter);
    }

    @Benchmark
    public void fixedFormatter_time(final Blackhole blackhole) {
        fixedFormatter(blackhole, DATE_TIME_FORMATTERS.fixedFormatter);
    }

    private void fixedFormatter(final Blackhole blackhole, final FixedDateFormat formatter) {
        for (final MutableInstant instant : INSTANTS) {
            final int length = formatter.formatInstant(instant, charBuffer, 0);
            blackhole.consume(length);
        }
    }

    @Benchmark
    public void javaFormatter_dateTime(final Blackhole blackhole) {
        javaFormatter(blackhole, DATE_TIME_FORMATTERS.javaFormatter);
    }

    @Benchmark
    public void javaFormatter_time(final Blackhole blackhole) {
        javaFormatter(blackhole, TIME_FORMATTERS.javaFormatter);
    }

    private void javaFormatter(final Blackhole blackhole, final DateTimeFormatter formatter) {
        for (final MutableInstant instant : INSTANTS) {
            stringBuilder.setLength(0);
            formatter.formatTo(instant, stringBuilder);
            blackhole.consume(stringBuilder.length());
        }
    }
}
