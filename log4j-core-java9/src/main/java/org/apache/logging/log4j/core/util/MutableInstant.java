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
package org.apache.logging.log4j.core.util;

import org.apache.logging.log4j.util.PerformanceSensitive;

import java.io.Serializable;

// This class is here to allow {@link SystemClock}, {@link SystemMillisClock}
// to compile. It will not be copied into the log4j-core module.

/**
 * An instantaneous point on the time line, used for high-precision log event timestamps.
 * Modelled on <a href="https://docs.oracle.com/javase/9/docs/api/index.html?java/time/class-use/Instant.html">java.time.Instant</a>,
 * except that this version is mutable to prevent allocating temporary objects that need to be garbage-collected later.
 * <p>
 * Instances of this class are <em>not</em> thread-safe and should not be shared between threads.
 * </p>
 *
 * @since 2.11.0
 */
@PerformanceSensitive("allocation")
public class MutableInstant implements Instant, Serializable {

    private static final int MILLIS_PER_SECOND = 1000;
    private static final int NANOS_PER_MILLI = 1000_000;
    static final int NANOS_PER_SECOND = MILLIS_PER_SECOND * NANOS_PER_MILLI;

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
        long epochMillisecond = epochSecond * MILLIS_PER_SECOND + millis;
        return epochMillisecond;
    }

    @Override
    public int getNanoOfMillisecond() {
        final int millis = nanoOfSecond / NANOS_PER_MILLI;
        int nanoOfMillisecond = nanoOfSecond - (millis * NANOS_PER_MILLI); // cheaper than nanoOfSecond % NANOS_PER_MILLI
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
        this.nanoOfSecond = (int) (epochMilli - (epochSecond * MILLIS_PER_SECOND)) * NANOS_PER_MILLI + nanoOfMillisecond;
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
        int millis = nano / NANOS_PER_MILLI;
        result[0] = epochSecond * MILLIS_PER_SECOND + millis;
        result[1] = nano - (millis * NANOS_PER_MILLI); // cheaper than nanoOfSecond % NANOS_PER_MILLI
    }

    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof MutableInstant)) {
            return false;
        }
        MutableInstant other = (MutableInstant) object;
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
        return "MutableInstant[epochSecond=" + epochSecond + ", nano=" + nanoOfSecond + "]";
    }
}
