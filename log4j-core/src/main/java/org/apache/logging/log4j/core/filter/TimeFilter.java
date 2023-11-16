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
package org.apache.logging.log4j.core.filter;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.util.Clock;
import org.apache.logging.log4j.core.util.ClockFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * Filters events that fall within a specified time period in each day.
 */
@Plugin(name = "TimeFilter", category = Node.CATEGORY, elementType = Filter.ELEMENT_TYPE, printObject = true)
@PerformanceSensitive("allocation")
public final class TimeFilter extends AbstractFilter {
    private static final Clock CLOCK = ClockFactory.getClock();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Length of hour in milliseconds.
     */
    private static final long HOUR_MS = 3600000;

    private static final long DAY_MS = HOUR_MS * 24;

    /**
     * Starting offset from midnight in milliseconds.
     */
    private volatile long start;

    private final LocalTime startTime;

    /**
     * Ending offset from midnight in milliseconds.
     */
    private volatile long end;

    private final LocalTime endTime;

    private final long duration;

    /**
     * Timezone.
     */
    private final ZoneId timeZone;

    /*
     * Expose for unit testing.
     */
    TimeFilter(
            final LocalTime start,
            final LocalTime end,
            final ZoneId timeZone,
            final Result onMatch,
            final Result onMismatch,
            final LocalDate now) {
        super(onMatch, onMismatch);
        this.startTime = start;
        this.endTime = end;
        this.timeZone = timeZone;
        this.start = ZonedDateTime.of(now, startTime, timeZone)
                .withEarlierOffsetAtOverlap()
                .toInstant()
                .toEpochMilli();
        long endMillis = ZonedDateTime.of(now, endTime, timeZone)
                .withEarlierOffsetAtOverlap()
                .toInstant()
                .toEpochMilli();
        if (end.isBefore(start)) {
            // End time must be tomorrow.
            endMillis += DAY_MS;
        }
        duration = startTime.isBefore(endTime)
                ? Duration.between(startTime, endTime).toMillis()
                : Duration.between(startTime, endTime).plusHours(24).toMillis();
        final long difference = (endMillis - this.start) - duration;
        if (difference != 0) {
            // Handle switch from standard time to daylight time and daylight time to standard time.
            endMillis -= difference;
        }
        this.end = endMillis;
    }

    private TimeFilter(
            final LocalTime start,
            final LocalTime end,
            final ZoneId timeZone,
            final Result onMatch,
            final Result onMismatch) {
        this(start, end, timeZone, onMatch, onMismatch, LocalDate.now(timeZone));
    }

    private synchronized void adjustTimes(final long currentTimeMillis) {
        if (currentTimeMillis <= end) {
            return;
        }
        final LocalDate date =
                Instant.ofEpochMilli(currentTimeMillis).atZone(timeZone).toLocalDate();
        this.start = ZonedDateTime.of(date, startTime, timeZone)
                .withEarlierOffsetAtOverlap()
                .toInstant()
                .toEpochMilli();
        long endMillis = ZonedDateTime.of(date, endTime, timeZone)
                .withEarlierOffsetAtOverlap()
                .toInstant()
                .toEpochMilli();
        if (endTime.isBefore(startTime)) {
            // End time must be tomorrow.
            endMillis += DAY_MS;
        }
        final long difference = (endMillis - this.start) - duration;
        if (difference != 0) {
            // Handle switch from standard time to daylight time and daylight time to standard time.
            endMillis -= difference;
        }
        this.end = endMillis;
    }

    /**
     * Package-protected for tests.
     *
     * @param currentTimeMillis the time to compare with the boundaries. May re-initialize the cached midnight
     *          boundary values.
     * @return the action to perform
     */
    Result filter(final long currentTimeMillis) {
        if (currentTimeMillis > end) {
            adjustTimes(currentTimeMillis);
        }
        return currentTimeMillis >= start && currentTimeMillis <= end ? onMatch : onMismatch;
    }

    @Override
    public Result filter(final LogEvent event) {
        return filter(event.getTimeMillis());
    }

    private Result filter() {
        return filter(CLOCK.currentTimeMillis());
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final Message msg, final Throwable t) {
        return filter();
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final Object msg, final Throwable t) {
        return filter();
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final String msg, final Object... params) {
        return filter();
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final String msg, final Object p0) {
        return filter();
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1) {
        return filter();
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2) {
        return filter();
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3) {
        return filter();
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4) {
        return filter();
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        return filter();
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        return filter();
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        return filter();
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        return filter();
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9) {
        return filter();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("start=").append(start);
        sb.append(", end=").append(end);
        sb.append(", timezone=").append(timeZone.toString());
        return sb.toString();
    }

    /**
     * Creates a TimeFilter.
     * @param start The start time.
     * @param end The end time.
     * @param tz timezone.
     * @param match Action to perform if the time matches.
     * @param mismatch Action to perform if the action does not match.
     * @return A TimeFilter.
     */
    // TODO Consider refactoring to use AbstractFilter.AbstractFilterBuilder
    @PluginFactory
    public static TimeFilter createFilter(
            @PluginAttribute("start") final String start,
            @PluginAttribute("end") final String end,
            @PluginAttribute("timezone") final String tz,
            @PluginAttribute("onMatch") final Result match,
            @PluginAttribute("onMismatch") final Result mismatch) {
        final LocalTime startTime = parseTimestamp(start, LocalTime.MIN);
        final LocalTime endTime = parseTimestamp(end, LocalTime.MAX);
        final ZoneId timeZone = tz == null ? ZoneId.systemDefault() : ZoneId.of(tz);
        final Result onMatch = match == null ? Result.NEUTRAL : match;
        final Result onMismatch = mismatch == null ? Result.DENY : mismatch;
        return new TimeFilter(startTime, endTime, timeZone, onMatch, onMismatch);
    }

    private static LocalTime parseTimestamp(final String timestamp, final LocalTime defaultValue) {
        if (timestamp == null) {
            return defaultValue;
        }

        try {
            return LocalTime.parse(timestamp, FORMATTER);
        } catch (final Exception e) {
            LOGGER.warn("Error parsing TimeFilter timestamp value {}", timestamp, e);
            return defaultValue;
        }
    }
}
