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
import java.util.function.ToLongFunction;
import org.apache.logging.log4j.core.time.Instant;

/**
 * An {@link InstantFormatter} wrapper caching the last formatted output in a {@link ThreadLocal} and trying to reuse it.
 *
 * @since 2.25.0
 */
final class InstantPatternThreadLocalCachedFormatter implements InstantPatternFormatter {

    /**
     * The epoch instant of an empty cache. Colliding with it is harmless: it only costs one redundant formatting.
     */
    private static final long UNSET_EPOCH_INSTANT = -1L;

    private final InstantPatternFormatter formatter;

    /**
     * Extracts the epoch instant the cache is keyed on.
     * <p>
     *     This is a {@link ToLongFunction}, and not a {@code Function<Instant, Long>}, on purpose: the latter would
     *     box the extracted epoch on every single formatted instant, that is, on every single log event.
     * </p>
     */
    private final ToLongFunction<Instant> epochInstantExtractor;

    private final ThreadLocal<CachedInstant> cachedInstantRef = ThreadLocal.withInitial(CachedInstant::new);

    /**
     * The last instant a thread formatted, together with the output it produced for it.
     * <p>
     *     {@code StringBuilderEncoder} documents a preference for keeping only JDK types in {@link ThreadLocal}s, so
     *     that a Log4j class loaded by an undeployed web application's class loader cannot be kept alive by a pooled
     *     thread. That concern does not reach this class: the caching wrapper is only ever installed when
     *     {@code Constants.ENABLE_THREADLOCALS} is set, and that flag is off for web applications precisely to
     *     disable the thread locals which could leak a class loader.
     * </p>
     * <p>
     *     Holding the epoch instant in a {@code long} field rather than in a boxed {@code Long} is what keeps this
     *     cache allocation-free.
     * </p>
     */
    private static final class CachedInstant {

        private long epochInstant = UNSET_EPOCH_INSTANT;

        private final StringBuilder buffer = new StringBuilder();
    }

    private final ChronoUnit precision;

    private InstantPatternThreadLocalCachedFormatter(
            final InstantPatternFormatter formatter,
            final ToLongFunction<Instant> epochInstantExtractor,
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
        final CachedInstant cachedInstant = cachedInstantRef.get();
        final StringBuilder localBuffer = cachedInstant.buffer;
        final long nextEpochInstant = epochInstantExtractor.applyAsLong(instant);
        if (cachedInstant.epochInstant != nextEpochInstant) {
            // We could have used `StringBuilders.trimToMaxSize()` on `localBuffer`.
            // That is, we wouldn't want exploded `StringBuilder`s in hundreds of `ThreadLocal`s.
            // Though we are formatting instants and always expect to produce strings of more or less the same length.
            // Hence, no need for truncation.

            // Invalidate the cache for as long as the buffer does not hold a complete output. Were `formatTo()` below
            // to fail, leaving the previous epoch instant in place would make a later call for *that* instant hit the
            // cache and read the buffer we are about to clobber here.
            cachedInstant.epochInstant = UNSET_EPOCH_INSTANT;
            localBuffer.setLength(0);
            formatter.formatTo(localBuffer, instant);
            cachedInstant.epochInstant = nextEpochInstant;
        }
        buffer.append(localBuffer);
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
