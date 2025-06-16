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
import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.IntStream;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Compares {@link DateTimeFormatter} efficiency for formatting the {@code ss.SSS} singleton versus formatting the {@code ss}, {@code .}, and {@code SSS} sequence.
 * This comparison is influential on the sequence merging strategies of {@code InstantPatternDynamicFormatter}.
 */
@State(Scope.Thread)
public class InstantPatternDynamicFormatterSequencingBenchmark {

    static final Locale LOCALE = Locale.US;

    static final TimeZone TIME_ZONE = TimeZone.getTimeZone("UTC");

    private static final Instant[] INSTANTS = createInstants();

    private static Instant[] createInstants() {
        final Instant initInstant = Instant.parse("2020-05-14T10:44:23.901Z");
        return IntStream.range(0, 1_000)
                .mapToObj((final int index) -> Instant.ofEpochSecond(
                        Math.addExact(initInstant.getEpochSecond(), index),
                        Math.addExact(initInstant.getNano(), index)))
                .toArray(Instant[]::new);
    }

    @FunctionalInterface
    private interface Formatter {

        void formatTo(TemporalAccessor instantAccessor, StringBuilder buffer);
    }

    private static final Formatter SINGLETON_FORMATTER =
            DateTimeFormatter.ofPattern("ss.SSS").withLocale(LOCALE).withZone(TIME_ZONE.toZoneId())::formatTo;

    private static final Formatter SEQUENCED_FORMATTER = new Formatter() {

        private final Formatter[] formatters = {
            DateTimeFormatter.ofPattern("ss").withLocale(LOCALE).withZone(TIME_ZONE.toZoneId())::formatTo,
            (temporal, appendable) -> appendable.append("."),
            DateTimeFormatter.ofPattern("SSS").withLocale(LOCALE).withZone(TIME_ZONE.toZoneId())::formatTo
        };

        @Override
        public void formatTo(final TemporalAccessor instantAccessor, final StringBuilder buffer) {
            for (Formatter formatter : formatters) {
                formatter.formatTo(instantAccessor, buffer);
            }
        }
    };

    private final StringBuilder buffer = new StringBuilder();

    @Benchmark
    public void singleton(final Blackhole blackhole) {
        benchmark(blackhole, SINGLETON_FORMATTER);
    }

    @Benchmark
    public void sequenced(final Blackhole blackhole) {
        benchmark(blackhole, SEQUENCED_FORMATTER);
    }

    private void benchmark(final Blackhole blackhole, final Formatter formatter) {
        for (final Instant instant : INSTANTS) {
            formatter.formatTo(instant, buffer);
            blackhole.consume(buffer);
            buffer.setLength(0);
        }
    }
}
