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

import static org.apache.logging.log4j.perf.jmh.instant.InstantPatternFormatterBenchmark.validateInstants;

import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;
import java.util.function.BiFunction;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.core.util.datetime.FastDatePrinter;
import org.apache.logging.log4j.core.util.datetime.FixedDateFormat;
import org.apache.logging.log4j.core.util.internal.instant.InstantPatternFormatter;
import org.apache.logging.log4j.layout.template.json.LogEventFixture;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmarks the impact of different date & time formatters on a typical layout.
 *
 * @see InstantPatternFormatterBenchmark for isolated benchmarks of date & time formatters
 */
@State(Scope.Thread)
@SuppressWarnings("deprecation")
public class InstantPatternFormatterImpactBenchmark {

    private static final List<LogEvent> LITE_LOG_EVENTS = createLogEvents(LogEventFixture::createLiteLogEvents);

    private static final List<LogEvent> FULL_LOG_EVENTS = createLogEvents(LogEventFixture::createFullLogEvents);

    private static List<LogEvent> createLogEvents(final BiFunction<Integer, Integer, List<LogEvent>> supplier) {
        final int logEventCount = 1_000;
        final List<LogEvent> logEvents = supplier.apply(
                logEventCount,
                // Avoid overlapping instants to ensure the impact of date & time formatting at event encoding:
                1);
        final Instant[] instants = logEvents.stream().map(LogEvent::getInstant).toArray(Instant[]::new);
        validateInstants(instants);
        return logEvents;
    }

    private static final PatternLayout LAYOUT = PatternLayout.newBuilder()
            .withConfiguration(new NullConfiguration())
            // Use a typical pattern *without* a date & time converter!
            .withPattern("[%t] %p %-40.40c{1.} %notEmpty{%x }- %m%n")
            .withAlwaysWriteExceptions(true)
            .build();

    private static final InstantPatternFormatterBenchmark.Formatters FORMATTERS =
            new InstantPatternFormatterBenchmark.Formatters("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private final StringBuilder stringBuilder = new StringBuilder(1_1024 * 16);

    private final char[] charBuffer = new char[stringBuilder.capacity()];

    private final Calendar calendar =
            Calendar.getInstance(InstantPatternFormatterBenchmark.TIME_ZONE, InstantPatternFormatterBenchmark.LOCALE);

    @Benchmark
    public void fastFormatter_lite(final Blackhole blackhole) {
        fastFormatter(blackhole, LITE_LOG_EVENTS, FORMATTERS.fastFormatter);
    }

    @Benchmark
    public void fastFormatter_full(final Blackhole blackhole) {
        fastFormatter(blackhole, FULL_LOG_EVENTS, FORMATTERS.fastFormatter);
    }

    private void fastFormatter(
            final Blackhole blackhole, final List<LogEvent> logEvents, final FastDatePrinter formatter) {
        // noinspection ForLoopReplaceableByForEach (avoid iterator allocation)
        for (int logEventIndex = 0; logEventIndex < logEvents.size(); logEventIndex++) {

            // 1. Encode event
            final LogEvent logEvent = logEvents.get(logEventIndex);
            stringBuilder.setLength(0);
            LAYOUT.serialize(logEvent, stringBuilder);

            // 2. Encode date & time
            calendar.setTimeInMillis(logEvent.getInstant().getEpochMillisecond());
            formatter.format(calendar, stringBuilder);
            blackhole.consume(stringBuilder.length());
        }
    }

    @Benchmark
    public void fixedFormatter_lite(final Blackhole blackhole) {
        fixedFormatter(blackhole, LITE_LOG_EVENTS, FORMATTERS.fixedFormatter);
    }

    @Benchmark
    public void fixedFormatter_full(final Blackhole blackhole) {
        fixedFormatter(blackhole, FULL_LOG_EVENTS, FORMATTERS.fixedFormatter);
    }

    private void fixedFormatter(
            final Blackhole blackhole, final List<LogEvent> logEvents, final FixedDateFormat formatter) {
        // noinspection ForLoopReplaceableByForEach (avoid iterator allocation)
        for (int logEventIndex = 0; logEventIndex < logEvents.size(); logEventIndex++) {

            // 1. Encode event
            final LogEvent logEvent = logEvents.get(logEventIndex);
            stringBuilder.setLength(0);
            LAYOUT.serialize(logEvent, stringBuilder);

            // 2. Encode date & time
            final MutableInstant instant = (MutableInstant) logEvent.getInstant();
            final int length = formatter.formatInstant(instant, charBuffer, 0);
            blackhole.consume(length);
        }
    }

    @Benchmark
    public void instantFormatter_lite(final Blackhole blackhole) {
        instantFormatter(blackhole, LITE_LOG_EVENTS, FORMATTERS.instantFormatter);
    }

    @Benchmark
    public void instantFormatter_full(final Blackhole blackhole) {
        instantFormatter(blackhole, FULL_LOG_EVENTS, FORMATTERS.instantFormatter);
    }

    private void instantFormatter(
            final Blackhole blackhole, final List<LogEvent> logEvents, final InstantPatternFormatter formatter) {
        // noinspection ForLoopReplaceableByForEach (avoid iterator allocation)
        for (int logEventIndex = 0; logEventIndex < logEvents.size(); logEventIndex++) {

            // 1. Encode event
            final LogEvent logEvent = logEvents.get(logEventIndex);
            stringBuilder.setLength(0);
            LAYOUT.serialize(logEvent, stringBuilder);

            // 2. Encode date & time
            final MutableInstant instant = (MutableInstant) logEvent.getInstant();
            formatter.formatTo(stringBuilder, instant);
            blackhole.consume(stringBuilder.length());
        }
    }

    @Benchmark
    public void javaFormatter_lite(final Blackhole blackhole) {
        javaFormatter(blackhole, LITE_LOG_EVENTS, FORMATTERS.javaFormatter);
    }

    @Benchmark
    public void javaFormatter_full(final Blackhole blackhole) {
        javaFormatter(blackhole, FULL_LOG_EVENTS, FORMATTERS.javaFormatter);
    }

    private void javaFormatter(
            final Blackhole blackhole, final List<LogEvent> logEvents, final DateTimeFormatter formatter) {
        // noinspection ForLoopReplaceableByForEach (avoid iterator allocation)
        for (int logEventIndex = 0; logEventIndex < logEvents.size(); logEventIndex++) {

            // 1. Encode event
            final LogEvent logEvent = logEvents.get(logEventIndex);
            stringBuilder.setLength(0);
            LAYOUT.serialize(logEvent, stringBuilder);

            // 2. Encode date & time
            final MutableInstant instant = (MutableInstant) logEvent.getInstant();
            formatter.formatTo(instant, stringBuilder);
            blackhole.consume(stringBuilder.length());
        }
    }
}
