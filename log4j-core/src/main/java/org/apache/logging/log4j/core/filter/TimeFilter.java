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
package org.apache.logging.log4j.core.filter;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.core.time.ClockFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * Filters events that fall within a specified time period in each day.
 */
@Configurable(elementType = Filter.ELEMENT_TYPE, printObject = true)
@Plugin
@PerformanceSensitive("allocation")
public final class TimeFilter extends AbstractFilter {
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

    private final Clock clock;

    /*
     * Expose for unit testing.
     */
    TimeFilter(
            final LocalTime start, final LocalTime end, final ZoneId timeZone, final Result onMatch,
            final Result onMismatch, final LocalDate now, final Clock clock) {
        super(onMatch, onMismatch);
        this.startTime = start;
        this.endTime = end;
        this.timeZone = timeZone;
        this.start = ZonedDateTime.of(now, startTime, timeZone).withEarlierOffsetAtOverlap().toInstant().toEpochMilli();
        long endMillis = ZonedDateTime.of(now, endTime, timeZone).withEarlierOffsetAtOverlap().toInstant().toEpochMilli();
        if (end.isBefore(start)) {
            // End time must be tomorrow.
            endMillis += DAY_MS;
        }
        duration = startTime.isBefore(endTime) ? Duration.between(startTime, endTime).toMillis() :
            Duration.between(startTime, endTime).plusHours(24).toMillis();
        final long difference = (endMillis - this.start) - duration;
        if (difference != 0) {
            // Handle switch from standard time to daylight time and daylight time to standard time.
            endMillis -= difference;
        }
        this.end = endMillis;
        this.clock = clock;
    }

    private TimeFilter(
            final LocalTime start, final LocalTime end, final ZoneId timeZone, final Result onMatch,
            final Result onMismatch, final Clock clock) {
        this(start, end, timeZone, onMatch, onMismatch, LocalDate.now(timeZone), clock);
    }

    private synchronized void adjustTimes(final long currentTimeMillis) {
        if (currentTimeMillis <= end) {
            return;
        }
        final LocalDate date = Instant.ofEpochMilli(currentTimeMillis).atZone(timeZone).toLocalDate();
        this.start = ZonedDateTime.of(date, startTime, timeZone).withEarlierOffsetAtOverlap().toInstant().toEpochMilli();
        long endMillis = ZonedDateTime.of(date, endTime, timeZone).withEarlierOffsetAtOverlap().toInstant().toEpochMilli();
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
        return filter(clock.currentTimeMillis());
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final Message msg,
            final Throwable t) {
        return filter();
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final Object msg,
            final Throwable t) {
        return filter();
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
            final Object... params) {
        return filter();
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
            final Object p0) {
        return filter();
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
            final Object p0, final Object p1) {
        return filter();
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
            final Object p0, final Object p1, final Object p2) {
        return filter();
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
            final Object p0, final Object p1, final Object p2, final Object p3) {
        return filter();
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
            final Object p0, final Object p1, final Object p2, final Object p3, final Object p4) {
        return filter();
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
            final Object p0, final Object p1, final Object p2, final Object p3, final Object p4, final Object p5) {
        return filter();
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
            final Object p0, final Object p1, final Object p2, final Object p3, final Object p4, final Object p5,
            final Object p6) {
        return filter();
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
            final Object p0, final Object p1, final Object p2, final Object p3, final Object p4, final Object p5,
            final Object p6, final Object p7) {
        return filter();
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
            final Object p0, final Object p1, final Object p2, final Object p3, final Object p4, final Object p5,
            final Object p6, final Object p7, final Object p8) {
        return filter();
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
            final Object p0, final Object p1, final Object p2, final Object p3, final Object p4, final Object p5,
            final Object p6, final Object p7, final Object p8, final Object p9) {
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
    @Deprecated(since = "3.0.0", forRemoval = true)
    public static TimeFilter createFilter(
            final String start, final String end, final String tz, final Result match, final Result mismatch) {
        final Builder builder = newBuilder()
                .setStart(start)
                .setEnd(end);
        if (tz != null) {
            builder.setTimezone(ZoneId.of(tz));
        }
        if (match != null) {
            builder.setOnMatch(match);
        }
        if (mismatch != null) {
            builder.setOnMismatch(mismatch);
        }
        return builder.get();
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

    @PluginFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends AbstractFilterBuilder<Builder> implements Supplier<TimeFilter> {
        private String start;
        private String end;
        @PluginAttribute
        private ZoneId timezone = ZoneId.systemDefault();
        private Clock clock;

        public Builder setStart(@PluginAttribute final String start) {
            this.start = start;
            return this;
        }

        public Builder setEnd(@PluginAttribute final String end) {
            this.end = end;
            return this;
        }

        public Builder setTimezone(final ZoneId timezone) {
            this.timezone = timezone;
            return this;
        }

        @Inject
        public Builder setClock(final Clock clock) {
            this.clock = clock;
            return this;
        }

        @Override
        public TimeFilter get() {
            final LocalTime startTime = parseTimestamp(start, LocalTime.MIN);
            final LocalTime endTime = parseTimestamp(end, LocalTime.MAX);
            if (clock == null) {
                clock = ClockFactory.getClock();
            }
            return new TimeFilter(startTime, endTime, timezone, getOnMatch(), getOnMismatch(), clock);
        }
    }

}
