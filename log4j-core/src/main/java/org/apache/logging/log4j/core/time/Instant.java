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

import org.apache.logging.log4j.core.util.Clock;
import org.apache.logging.log4j.util.StringBuilderFormattable;

/**
 * Models a point in time, suitable for event timestamps.
 * <p>
 * Provides methods for obtaining high precision time information similar to the
 * <a href="https://docs.oracle.com/javase/9/docs/api/java/time/Instant.html">Instant</a> class introduced in Java 8,
 * while also supporting the legacy millisecond precision API.
 * </p><p>
 * Depending on the platform, time sources ({@link Clock} implementations) may produce high precision or millisecond
 * precision time values. At the same time, some time value consumers (for example timestamp formatters) may only be
 * able to consume time values of millisecond precision, while some others may require a high precision time value.
 * </p><p>
 * This class bridges these two time APIs.
 * </p>
 * @since 2.11
 */
public interface Instant extends StringBuilderFormattable {
    /**
     * Gets the number of seconds from the Java epoch of 1970-01-01T00:00:00Z.
     * <p>
     * The epoch second count is a simple incrementing count of seconds where second 0 is 1970-01-01T00:00:00Z.
     * The nanosecond part of the day is returned by {@link #getNanoOfSecond()}.
     * </p>
     * @return the seconds from the epoch of 1970-01-01T00:00:00Z
     */
    long getEpochSecond();

    /**
     * Gets the number of nanoseconds, later along the time-line, from the start of the second.
     * <p>
     * The nanosecond-of-second value measures the total number of nanoseconds from the second returned by {@link #getEpochSecond()}.
     * </p>
     * @return the nanoseconds within the second, always positive, never exceeds {@code 999,999,999}
     */
    int getNanoOfSecond();

    /**
     * Gets the number of milliseconds from the Java epoch of 1970-01-01T00:00:00Z.
     * <p>
     * The epoch millisecond count is a simple incrementing count of milliseconds where millisecond 0 is 1970-01-01T00:00:00Z.
     * The nanosecond part of the day is returned by {@link #getNanoOfMillisecond()}.
     * </p>
     * @return the milliseconds from the epoch of 1970-01-01T00:00:00Z
     */
    long getEpochMillisecond();

    /**
     * Gets the number of nanoseconds, later along the time-line, from the start of the millisecond.
     * <p>
     * The nanosecond-of-millisecond value measures the total number of nanoseconds from the millisecond returned by {@link #getEpochMillisecond()}.
     * </p>
     * @return the nanoseconds within the millisecond, always positive, never exceeds {@code 999,999}
     */
    int getNanoOfMillisecond();
}
