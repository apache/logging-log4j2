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
package org.apache.logging.log4j.core.time;

import static java.time.temporal.ChronoField.INSTANT_SECONDS;
import static java.time.temporal.ChronoField.MICRO_OF_SECOND;
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoUnit.NANOS;

import java.io.Serializable;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
import org.apache.logging.log4j.core.util.Clock;
import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * An instantaneous point on the time line, used for high-precision log event timestamps.
 * Modeled on <a href="https://docs.oracle.com/javase/9/docs/api/index.html?java/time/class-use/Instant.html">java.time.Instant</a>,
 * except that this version is mutable to prevent allocating temporary objects that need to be garbage-collected later.
 * <p>
 * Instances of this class are <em>not</em> thread-safe and should not be shared between threads.
 * </p>
 *
 * @since 2.11
 */
@PerformanceSensitive("allocation")
public class MutableInstant implements Instant, Serializable, TemporalAccessor {

    private static final int MILLIS_PER_SECOND = 1000;
    private static final int NANOS_PER_MILLI = 1000_000;
    private static final int NANOS_PER_SECOND = MILLIS_PER_SECOND * NANOS_PER_MILLI;

    private long epochSecond;
    private int nanoOfSecond;

    @Override
    public long getEpochSecond() {
        return epochSecond;
    }

    @Override
    public int getNanoOfSecond() {
        return nanoOfSecond;
    }

    @Override
    public long getEpochMillisecond() {
        final int millis = nanoOfSecond / NANOS_PER_MILLI;
        final long epochMillisecond = epochSecond * MILLIS_PER_SECOND + millis;
        return epochMillisecond;
    }

    @Override
    public int getNanoOfMillisecond() {
        final int millis = nanoOfSecond / NANOS_PER_MILLI;
        final int nanoOfMillisecond =
                nanoOfSecond - (millis * NANOS_PER_MILLI); // cheaper than nanoOfSecond % NANOS_PER_MILLI
        return nanoOfMillisecond;
    }

    public void initFrom(final Instant other) {
        this.epochSecond = other.getEpochSecond();
        this.nanoOfSecond = other.getNanoOfSecond();
    }

    /**
     * Updates the fields of this {@code MutableInstant} from the specified epoch millis.
     * @param epochMilli the number of milliseconds from the Java epoch of 1970-01-01T00:00:00Z
     * @param nanoOfMillisecond the number of nanoseconds, later along the time-line, from the start of the millisecond
     */
    public void initFromEpochMilli(final long epochMilli, final int nanoOfMillisecond) {
        validateNanoOfMillisecond(nanoOfMillisecond);
        this.epochSecond = epochMilli / MILLIS_PER_SECOND;
        this.nanoOfSecond =
                (int) (epochMilli - (epochSecond * MILLIS_PER_SECOND)) * NANOS_PER_MILLI + nanoOfMillisecond;
    }

    private void validateNanoOfMillisecond(final int nanoOfMillisecond) {
        if (nanoOfMillisecond < 0 || nanoOfMillisecond >= NANOS_PER_MILLI) {
            throw new IllegalArgumentException("Invalid nanoOfMillisecond " + nanoOfMillisecond);
        }
    }

    public void initFrom(final Clock clock) {
        if (clock instanceof PreciseClock) {
            ((PreciseClock) clock).init(this);
        } else {
            initFromEpochMilli(clock.currentTimeMillis(), 0);
        }
    }

    /**
     * Updates the fields of this {@code MutableInstant} from the specified instant components.
     * @param epochSecond the number of seconds from the Java epoch of 1970-01-01T00:00:00Z
     * @param nano the number of nanoseconds, later along the time-line, from the start of the second
     */
    public void initFromEpochSecond(final long epochSecond, final int nano) {
        validateNanoOfSecond(nano);
        this.epochSecond = epochSecond;
        this.nanoOfSecond = nano;
    }

    private void validateNanoOfSecond(final int nano) {
        if (nano < 0 || nano >= NANOS_PER_SECOND) {
            throw new IllegalArgumentException("Invalid nanoOfSecond " + nano);
        }
    }

    /**
     * Updates the elements of the specified {@code long[]} result array from the specified instant components.
     * @param epochSecond (input) the number of seconds from the Java epoch of 1970-01-01T00:00:00Z
     * @param nano (input) the number of nanoseconds, later along the time-line, from the start of the second
     * @param result (output) a two-element array to store the result: the first element is the number of milliseconds
     *               from the Java epoch of 1970-01-01T00:00:00Z,
     *               the second element is the number of nanoseconds, later along the time-line, from the start of the millisecond
     */
    public static void instantToMillisAndNanos(final long epochSecond, final int nano, final long[] result) {
        final int millis = nano / NANOS_PER_MILLI;
        result[0] = epochSecond * MILLIS_PER_SECOND + millis;
        result[1] = nano - (millis * NANOS_PER_MILLI); // cheaper than nanoOfSecond % NANOS_PER_MILLI
    }

    @Override
    public boolean isSupported(final TemporalField field) {
        if (field instanceof ChronoField) {
            return field == INSTANT_SECONDS
                    || field == NANO_OF_SECOND
                    || field == MICRO_OF_SECOND
                    || field == MILLI_OF_SECOND;
        }
        return field != null && field.isSupportedBy(this);
    }

    @Override
    public long getLong(final TemporalField field) {
        if (field instanceof ChronoField) {
            switch ((ChronoField) field) {
                case NANO_OF_SECOND:
                    return nanoOfSecond;
                case MICRO_OF_SECOND:
                    return nanoOfSecond / 1000;
                case MILLI_OF_SECOND:
                    return nanoOfSecond / 1000_000;
                case INSTANT_SECONDS:
                    return epochSecond;
            }
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.getFrom(this);
    }

    @Override
    public ValueRange range(final TemporalField field) {
        return TemporalAccessor.super.range(field);
    }

    @Override
    public int get(final TemporalField field) {
        if (field instanceof ChronoField) {
            switch ((ChronoField) field) {
                case NANO_OF_SECOND:
                    return nanoOfSecond;
                case MICRO_OF_SECOND:
                    return nanoOfSecond / 1000;
                case MILLI_OF_SECOND:
                    return nanoOfSecond / 1000_000;
                case INSTANT_SECONDS:
                    INSTANT_SECONDS.checkValidIntValue(epochSecond);
            }
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return range(field).checkValidIntValue(field.getFrom(this), field);
    }

    @Override
    public <R> R query(final TemporalQuery<R> query) {
        if (query == TemporalQueries.precision()) {
            return (R) NANOS;
        }
        // inline TemporalAccessor.super.query(query) as an optimization
        if (query == TemporalQueries.chronology()
                || query == TemporalQueries.zoneId()
                || query == TemporalQueries.zone()
                || query == TemporalQueries.offset()
                || query == TemporalQueries.localDate()
                || query == TemporalQueries.localTime()) {
            return null;
        }
        return query.queryFrom(this);
    }

    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof MutableInstant)) {
            return false;
        }
        final MutableInstant other = (MutableInstant) object;
        return epochSecond == other.epochSecond && nanoOfSecond == other.nanoOfSecond;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (int) (epochSecond ^ (epochSecond >>> 32));
        result = 31 * result + nanoOfSecond;
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(64);
        formatTo(sb);
        return sb.toString();
    }

    @Override
    public void formatTo(final StringBuilder buffer) {
        buffer.append("MutableInstant[epochSecond=")
                .append(epochSecond)
                .append(", nano=")
                .append(nanoOfSecond)
                .append("]");
    }
}
