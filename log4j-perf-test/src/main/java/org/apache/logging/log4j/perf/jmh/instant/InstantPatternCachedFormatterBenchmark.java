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
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.IntStream;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.core.util.internal.instant.InstantPatternFormatter;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Measures the caching {@link InstantPatternFormatter}, i.e. the one a garbage-free configuration gets for {@code %d}.
 * <p>
 *     {@link InstantPatternFormatterBenchmark} deliberately defeats the cache to isolate raw formatting cost. This
 *     benchmark does the opposite: it measures the wrapper itself, on the hit-dominated traffic a high throughput
 *     application actually produces, where many events share a millisecond.
 * </p>
 */
@State(Scope.Thread)
public class InstantPatternCachedFormatterBenchmark {

    private static final Locale LOCALE = Locale.US;

    private static final TimeZone TIME_ZONE = TimeZone.getTimeZone("UTC");

    /**
     * The default {@code %d} pattern, which is of millisecond precision and hence gets a millisecond-precision cache.
     */
    private static final String PATTERN = "yyyy-MM-dd HH:mm:ss,SSS";

    private static final InstantPatternFormatter FORMATTER = InstantPatternFormatter.newBuilder()
            .setPattern(PATTERN)
            .setLocale(LOCALE)
            .setTimeZone(TIME_ZONE)
            .setCachingEnabled(true)
            .build();

    /**
     * Distinct instants that all fall in the same millisecond: every formatting is a cache hit.
     */
    private static final MutableInstant[] CACHE_HITTING_INSTANTS = createInstants(0);

    /**
     * Instants a millisecond apart: every formatting is a cache miss.
     */
    private static final MutableInstant[] CACHE_MISSING_INSTANTS = createInstants(1);

    private static MutableInstant[] createInstants(final int millisStep) {
        final Instant initInstant = Instant.parse("2020-05-14T10:44:23.901Z");
        return IntStream.range(0, 1_000)
                .mapToObj(index -> {
                    // Nanos differ even when millis do not, so the instants are never equal.
                    final Instant instant =
                            initInstant.plusMillis((long) index * millisStep).plusNanos(index);
                    final MutableInstant mutableInstant = new MutableInstant();
                    mutableInstant.initFromEpochSecond(instant.getEpochSecond(), instant.getNano());
                    return mutableInstant;
                })
                .toArray(MutableInstant[]::new);
    }

    private final StringBuilder buffer = new StringBuilder();

    @Benchmark
    public void cacheHit(final Blackhole blackhole) {
        format(CACHE_HITTING_INSTANTS, blackhole);
    }

    @Benchmark
    public void cacheMiss(final Blackhole blackhole) {
        format(CACHE_MISSING_INSTANTS, blackhole);
    }

    private void format(final MutableInstant[] instants, final Blackhole blackhole) {
        for (final MutableInstant instant : instants) {
            buffer.setLength(0);
            FORMATTER.formatTo(buffer, instant);
            blackhole.consume(buffer);
        }
    }
}
