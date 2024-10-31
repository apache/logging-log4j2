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
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Function;
import org.apache.logging.log4j.core.time.Instant;

/**
 * An {@link InstantFormatter} wrapper caching the last formatted output in a {@link ThreadLocal} and trying to reuse it.
 *
 * @since 2.25.0
 */
final class InstantPatternThreadLocalCachedFormatter implements InstantPatternFormatter {

    private final InstantPatternFormatter formatter;

    private final Function<Instant, Long> epochInstantExtractor;

    private final ThreadLocal<Object[]> epochInstantAndBufferRef =
            ThreadLocal.withInitial(InstantPatternThreadLocalCachedFormatter::createEpochInstantAndBuffer);

    private Object[] lastEpochInstantAndBuffer = createEpochInstantAndBuffer();

    private static Object[] createEpochInstantAndBuffer() {
        return new Object[] {-1L, new StringBuilder()};
    }

    private final ChronoUnit precision;

    private InstantPatternThreadLocalCachedFormatter(
            final InstantPatternFormatter formatter,
            final Function<Instant, Long> epochInstantExtractor,
            final ChronoUnit precision) {
        this.formatter = formatter;
        this.epochInstantExtractor = epochInstantExtractor;
        this.precision = precision;
    }

    static InstantPatternThreadLocalCachedFormatter ofMilliPrecision(final InstantPatternFormatter formatter) {
        final ChronoUnit precision = effectivePrecision(formatter, ChronoUnit.MILLIS);
        return new InstantPatternThreadLocalCachedFormatter(formatter, Instant::getEpochMillisecond, precision);
    }

    static InstantPatternThreadLocalCachedFormatter ofSecondPrecision(final InstantPatternFormatter formatter) {
        final ChronoUnit precision = effectivePrecision(formatter, ChronoUnit.SECONDS);
        return new InstantPatternThreadLocalCachedFormatter(formatter, Instant::getEpochSecond, precision);
    }

    private static ChronoUnit effectivePrecision(final InstantFormatter formatter, final ChronoUnit cachePrecision) {
        final ChronoUnit formatterPrecision = formatter.getPrecision();
        final int comparison = cachePrecision.compareTo(formatterPrecision);
        if (comparison == 0) {
            return formatterPrecision;
        } else if (comparison > 0) {
            final String message = String.format(
                    "instant formatter `%s` is of `%s` precision, whereas the requested cache precision is `%s`",
                    formatter, formatterPrecision, cachePrecision);
            throw new IllegalArgumentException(message);
        } else {
            return cachePrecision;
        }
    }

    @Override
    public ChronoUnit getPrecision() {
        return precision;
    }

    @Override
    public void formatTo(final StringBuilder buffer, final Instant instant) {
        requireNonNull(buffer, "buffer");
        requireNonNull(instant, "instant");
        final Object[] prevEpochInstantAndBuffer = lastEpochInstantAndBuffer;
        final long prevEpochInstant = (long) prevEpochInstantAndBuffer[0];
        final StringBuilder prevBuffer = (StringBuilder) prevEpochInstantAndBuffer[1];
        final long nextEpochInstant = epochInstantExtractor.apply(instant);
        if (prevEpochInstant == nextEpochInstant) {
            buffer.append(prevBuffer);
        } else {

            // We could have used `StringBuilders.trimToMaxSize()` on `prevBuffer`.
            // That is, we wouldn't want exploded `StringBuilder`s in hundreds of `ThreadLocal`s.
            // Though we are formatting instants and always expect to produce strings of more or less the same length.
            // Hence, no need for truncation.

            // Populate a new cache entry
            final Object[] nextEpochInstantAndBuffer = epochInstantAndBufferRef.get();
            nextEpochInstantAndBuffer[0] = nextEpochInstant;
            final StringBuilder nextBuffer = (StringBuilder) nextEpochInstantAndBuffer[1];
            nextBuffer.setLength(0);
            formatter.formatTo(nextBuffer, instant);

            // Update the effective cache entry
            lastEpochInstantAndBuffer = nextEpochInstantAndBuffer;

            // Help out the request
            buffer.append(nextBuffer);
        }
    }

    @Override
    public String getPattern() {
        return formatter.getPattern();
    }

    @Override
    public Locale getLocale() {
        return formatter.getLocale();
    }

    @Override
    public TimeZone getTimeZone() {
        return formatter.getTimeZone();
    }
}
