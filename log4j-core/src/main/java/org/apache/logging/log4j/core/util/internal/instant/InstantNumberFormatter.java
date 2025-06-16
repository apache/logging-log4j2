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
import java.util.function.BiConsumer;
import org.apache.logging.log4j.core.time.Instant;

/**
 * Formats an {@link Instant} numerically; e.g., formats its epoch<sup>1</sup> seconds.
 * <p>
 * <sup>1</sup> Epoch is a fixed instant on {@code 1970-01-01Z}.
 * </p>
 * <h2>Internal usage only!</h2>
 * <p>
 * This class is intended only for internal Log4j usage.
 * <b>Log4j users should not use this class!</b>
 * This class is not subject to any backward compatibility concerns.
 * </p>
 *
 * @since 2.25.0
 */
public enum InstantNumberFormatter implements InstantFormatter {

    /**
     * Formats nanoseconds since epoch; e.g., {@code 1581082727982123456}.
     */
    EPOCH_NANOS(ChronoUnit.NANOS, (instant, buffer) -> {
        final long nanos = epochNanos(instant);
        buffer.append(nanos);
    }),

    /**
     * Formats milliseconds since epoch, including the nanosecond fraction; e.g., {@code 1581082727982.123456}.
     * The nanosecond fraction will be skipped if it is zero.
     */
    EPOCH_MILLIS(ChronoUnit.NANOS, (instant, buffer) -> {
        final long nanos = epochNanos(instant);
        buffer.append(nanos);
        buffer.insert(buffer.length() - 6, '.');
    }),

    /**
     * Formats milliseconds since epoch, excluding the nanosecond fraction; e.g., {@code 1581082727982}.
     */
    EPOCH_MILLIS_ROUNDED(ChronoUnit.MILLIS, (instant, buffer) -> {
        final long millis = instant.getEpochMillisecond();
        buffer.append(millis);
    }),

    /**
     * Formats the nanosecond fraction of milliseconds since epoch; e.g., {@code 123456}.
     */
    EPOCH_MILLIS_NANOS(ChronoUnit.NANOS, (instant, buffer) -> {
        final long nanos = epochNanos(instant);
        final long fraction = nanos % 1_000_000L;
        buffer.append(fraction);
    }),

    /**
     * Formats seconds since epoch, including the nanosecond fraction; e.g., {@code 1581082727.982123456}.
     * The nanosecond fraction will be skipped if it is zero.
     */
    EPOCH_SECONDS(ChronoUnit.NANOS, (instant, buffer) -> {
        final long nanos = epochNanos(instant);
        buffer.append(nanos);
        buffer.insert(buffer.length() - 9, '.');
    }),

    /**
     * Formats seconds since epoch, excluding the nanosecond fraction; e.g., {@code 1581082727}.
     * The nanosecond fraction will be skipped if it is zero.
     */
    EPOCH_SECONDS_ROUNDED(ChronoUnit.SECONDS, (instant, buffer) -> {
        final long seconds = instant.getEpochSecond();
        buffer.append(seconds);
    }),

    /**
     * Formats the nanosecond fraction of seconds since epoch; e.g., {@code 982123456}.
     */
    EPOCH_SECONDS_NANOS(ChronoUnit.NANOS, (instant, buffer) -> {
        final long secondsNanos = instant.getNanoOfSecond();
        buffer.append(secondsNanos);
    });

    private static long epochNanos(final Instant instant) {
        final long nanos = Math.multiplyExact(1_000_000_000L, instant.getEpochSecond());
        return Math.addExact(nanos, instant.getNanoOfSecond());
    }

    private final ChronoUnit precision;

    private final BiConsumer<Instant, StringBuilder> formatter;

    InstantNumberFormatter(final ChronoUnit precision, final BiConsumer<Instant, StringBuilder> formatter) {
        this.precision = precision;
        this.formatter = formatter;
    }

    @Override
    public ChronoUnit getPrecision() {
        return precision;
    }

    @Override
    public void formatTo(final StringBuilder buffer, final Instant instant) {
        requireNonNull(buffer, "buffer");
        requireNonNull(instant, "instant");
        formatter.accept(instant, buffer);
    }
}
