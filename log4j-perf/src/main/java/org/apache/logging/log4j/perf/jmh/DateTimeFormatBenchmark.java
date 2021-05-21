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
package org.apache.logging.log4j.perf.jmh;

import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.core.util.datetime.FastDatePrinter;
import org.apache.logging.log4j.core.util.datetime.FixedDateFormat;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.IntStream;

/**
 * Compares {@link MutableInstant} formatting efficiency of
 * {@link FastDatePrinter}, {@link FixedDateFormat}, and {@link DateTimeFormatter}.
 * <p>
 * The major formatting efficiency is mostly provided by caching, i.e.,
 * reusing the earlier formatter output if timestamps match. We deliberately
 * exclude this optimization, since it is applicable to all formatters. This
 * benchmark rather focuses on only and only the formatting efficiency.
 */
@State(Scope.Thread)
public class DateTimeFormatBenchmark {

    /**
     * The pattern to be tested.
     * <p>
     * Note that neither {@link FastDatePrinter}, nor {@link FixedDateFormat}
     * supports nanosecond precision.
     */
    private static final String PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    private static final Locale LOCALE = Locale.US;

    private static final TimeZone TIME_ZONE = TimeZone.getTimeZone("UTC");

    private static final Instant INIT_INSTANT = Instant.parse("2020-05-14T10:44:23.901Z");

    private static final MutableInstant[] INSTANTS = IntStream
            .range(0, 1_000)
            .mapToObj((final int index) -> {
                final MutableInstant instant = new MutableInstant();
                instant.initFromEpochSecond(
                        Math.addExact(INIT_INSTANT.getEpochSecond(), index),
                        Math.addExact(INIT_INSTANT.getNano(), index));
                return instant;
            })
            .toArray(MutableInstant[]::new);

    private static final Calendar[] CALENDARS = Arrays
            .stream(INSTANTS)
            .map((final MutableInstant instant) -> {
                final Calendar calendar = Calendar.getInstance(TIME_ZONE, LOCALE);
                calendar.setTimeInMillis(instant.getEpochMillisecond());
                return calendar;
            })
            .toArray(Calendar[]::new);

    private static final FastDatePrinter FAST_DATE_PRINTER =
            new FastDatePrinter(PATTERN, TIME_ZONE, LOCALE) {};

    private static final FixedDateFormat FIXED_DATE_FORMAT =
            Objects.requireNonNull(
                    FixedDateFormat.createIfSupported(PATTERN, TIME_ZONE.getID()),
                    "couldn't create FixedDateTime for pattern " + PATTERN + " and time zone " + TIME_ZONE.getID());

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter
                    .ofPattern(PATTERN)
                    .withZone(TIME_ZONE.toZoneId())
                    .withLocale(LOCALE);

    private final StringBuilder stringBuilder = new StringBuilder(PATTERN.length() * 2);

    private final char[] charBuffer = new char[stringBuilder.capacity()];

    @Benchmark
    public void fastDatePrinter(final Blackhole blackhole) {
        for (final Calendar calendar : CALENDARS) {
            stringBuilder.setLength(0);
            FAST_DATE_PRINTER.format(calendar, stringBuilder);
            blackhole.consume(stringBuilder.length());
        }
    }

    @Benchmark
    public void fixedDateFormat(final Blackhole blackhole) {
        for (final MutableInstant instant : INSTANTS) {
            final int length = FIXED_DATE_FORMAT.formatInstant(instant, charBuffer, 0);
            blackhole.consume(length);
        }
    }

    @Benchmark
    public void dateTimeFormatter(final Blackhole blackhole) {
        for (final MutableInstant instant : INSTANTS) {
            stringBuilder.setLength(0);
            DATE_TIME_FORMATTER.formatTo(instant, stringBuilder);
            blackhole.consume(stringBuilder.length());
        }
    }

}
