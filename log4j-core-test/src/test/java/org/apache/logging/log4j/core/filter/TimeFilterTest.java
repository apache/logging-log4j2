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

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.time.Clock;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TimeFilterTest {
    private static long CLOCKTIME = System.currentTimeMillis();

    /** Helper class */
    public static class FixedTimeClock implements Clock {
        @Override
        public long currentTimeMillis() {
            return CLOCKTIME;
        }
    }

    @Test
    public void springForward() {
        final TimeFilter filter = new TimeFilter(LocalTime.of(2,0), LocalTime.of(3,0),
                ZoneId.of("America/Los_Angeles"), null, null, LocalDate.of(2020, 3, 8), new FixedTimeClock());
        filter.start();
        assertTrue(filter.isStarted());
        ZonedDateTime date = ZonedDateTime.of(2020, 3, 8, 2, 6, 30, 0, ZoneId.of("America/Los_Angeles"));
        CLOCKTIME = date.toInstant().toEpochMilli();
        LogEvent event = Log4jLogEvent.newBuilder().setTimeMillis(CLOCKTIME).build();
        assertSame(Filter.Result.NEUTRAL, filter.filter(event),
                "Time " + CLOCKTIME + " is not within range: " + filter.toString());
        date = date.plusDays(1).withHour(2);
        CLOCKTIME = date.toInstant().toEpochMilli();
        event = Log4jLogEvent.newBuilder().setTimeMillis(CLOCKTIME).build();
        assertSame(Filter.Result.NEUTRAL, filter.filter(event),
                "Time " + CLOCKTIME + " is not within range: " + filter.toString());
        date = date.withHour(4);
        CLOCKTIME = date.toInstant().toEpochMilli();
        event = Log4jLogEvent.newBuilder().setTimeMillis(CLOCKTIME).build();
        assertSame(Filter.Result.DENY, filter.filter(event),
                "Time " + CLOCKTIME + " is within range: " + filter.toString());
    }


    @Test
    public void fallBack() {
        final TimeFilter filter = new TimeFilter(LocalTime.of(1,0), LocalTime.of(2,0),
                ZoneId.of("America/Los_Angeles"), null, null, LocalDate.of(2020, 11, 1), new FixedTimeClock());
        filter.start();
        assertTrue(filter.isStarted());
        ZonedDateTime date = ZonedDateTime.of(2020, 11, 1, 1, 6, 30, 0, ZoneId.of("America/Los_Angeles")).withEarlierOffsetAtOverlap();
        CLOCKTIME = date.toInstant().toEpochMilli();
        LogEvent event = Log4jLogEvent.newBuilder().setTimeMillis(CLOCKTIME).build();
        assertSame(Filter.Result.NEUTRAL, filter.filter(event),
                "Time " + CLOCKTIME + " is not within range: " + filter.toString());
        date = ZonedDateTime.of(2020, 11, 1, 1, 6, 30, 0, ZoneId.of("America/Los_Angeles")).withLaterOffsetAtOverlap();
        CLOCKTIME = date.toInstant().toEpochMilli();
        event = Log4jLogEvent.newBuilder().setTimeMillis(CLOCKTIME).build();
        assertSame(Filter.Result.DENY, filter.filter(event),
                "Time " + CLOCKTIME + " is within range: " + filter.toString());
        date = date.plusDays(1).withHour(1).withMinute(30);
        CLOCKTIME = date.toInstant().toEpochMilli();
        event = Log4jLogEvent.newBuilder().setTimeMillis(CLOCKTIME).build();
        assertSame(Filter.Result.NEUTRAL, filter.filter(event),
                "Time " + CLOCKTIME + " is not within range: " + filter.toString());
        date = date.withHour(4);
        CLOCKTIME = date.toInstant().toEpochMilli();
        event = Log4jLogEvent.newBuilder().setTimeMillis(CLOCKTIME).build();
        assertSame(Filter.Result.DENY, filter.filter(event),
                "Time " + CLOCKTIME + " is within range: " + filter.toString());
    }


    @Test
    public void overnight() {
        final TimeFilter filter = new TimeFilter(LocalTime.of(23,0), LocalTime.of(1,0),
                ZoneId.of("America/Los_Angeles"), null, null, LocalDate.of(2020, 3, 10), new FixedTimeClock());
        filter.start();
        assertTrue(filter.isStarted());
        ZonedDateTime date = ZonedDateTime.of(2020, 3, 10, 23, 30, 30, 0, ZoneId.of("America/Los_Angeles")).withEarlierOffsetAtOverlap();
        CLOCKTIME = date.toInstant().toEpochMilli();
        LogEvent event = Log4jLogEvent.newBuilder().setTimeMillis(CLOCKTIME).build();
        assertSame(Filter.Result.NEUTRAL, filter.filter(event),
                "Time " + CLOCKTIME + " is not within range: " + filter.toString());
        date = date.plusHours(1);
        CLOCKTIME = date.toInstant().toEpochMilli();
        event = Log4jLogEvent.newBuilder().setTimeMillis(CLOCKTIME).build();
        assertSame(Filter.Result.NEUTRAL, filter.filter(event),
                "Time " + CLOCKTIME + " is not within range: " + filter.toString());
        date = date.plusHours(1);
        CLOCKTIME = date.toInstant().toEpochMilli();
        event = Log4jLogEvent.newBuilder().setTimeMillis(CLOCKTIME).build();
        assertSame(Filter.Result.DENY, filter.filter(event),
                "Time " + CLOCKTIME + " is within range: " + filter.toString());
        date = date.plusDays(1).withHour(0);
        CLOCKTIME = date.toInstant().toEpochMilli();
        event = Log4jLogEvent.newBuilder().setTimeMillis(CLOCKTIME).build();
        assertSame(Filter.Result.NEUTRAL, filter.filter(event),
                "Time " + CLOCKTIME + " is not within range: " + filter.toString());
    }

    @Test
    public void overnightForward() {
        final TimeFilter filter = new TimeFilter(LocalTime.of(23,0), LocalTime.of(2,0),
                ZoneId.of("America/Los_Angeles"), null, null, LocalDate.of(2020, 3, 7), new FixedTimeClock());
        filter.start();
        assertTrue(filter.isStarted());
        ZonedDateTime date = ZonedDateTime.of(2020, 3, 7, 23, 30, 30, 0, ZoneId.of("America/Los_Angeles")).withEarlierOffsetAtOverlap();
        CLOCKTIME = date.toInstant().toEpochMilli();
        LogEvent event = Log4jLogEvent.newBuilder().setTimeMillis(CLOCKTIME).build();
        assertSame(Filter.Result.NEUTRAL, filter.filter(event),
                "Time " + CLOCKTIME + " is not within range: " + filter.toString());
        date = date.plusHours(1);
        CLOCKTIME = date.toInstant().toEpochMilli();
        event = Log4jLogEvent.newBuilder().setTimeMillis(CLOCKTIME).build();
        assertSame(Filter.Result.NEUTRAL, filter.filter(event),
                "Time " + CLOCKTIME + " is not within range: " + filter.toString());
        date = date.plusHours(2);
        CLOCKTIME = date.toInstant().toEpochMilli();
        event = Log4jLogEvent.newBuilder().setTimeMillis(CLOCKTIME).build();
        assertSame(Filter.Result.DENY, filter.filter(event),
                "Time " + CLOCKTIME + " is within range: " + filter.toString());
        date = date.plusDays(1).withHour(0);
        CLOCKTIME = date.toInstant().toEpochMilli();
        event = Log4jLogEvent.newBuilder().setTimeMillis(CLOCKTIME).build();
        assertSame(Filter.Result.NEUTRAL, filter.filter(event),
                "Time " + CLOCKTIME + " is not within range: " + filter.toString());
    }


    @Test
    public void overnightFallback() {
        final TimeFilter filter = new TimeFilter(LocalTime.of(23,0), LocalTime.of(2,0),
                ZoneId.of("America/Los_Angeles"), null, null, LocalDate.of(2020, 10, 31), new FixedTimeClock());
        filter.start();
        assertTrue(filter.isStarted());
        ZonedDateTime date = ZonedDateTime.of(2020, 10, 31, 23, 30, 30, 0, ZoneId.of("America/Los_Angeles")).withEarlierOffsetAtOverlap();
        CLOCKTIME = date.toInstant().toEpochMilli();
        LogEvent event = Log4jLogEvent.newBuilder().setTimeMillis(CLOCKTIME).build();
        assertSame(Filter.Result.NEUTRAL, filter.filter(event),
                "Time " + CLOCKTIME + " is not within range: " + filter.toString());
        date = date.plusHours(1);
        CLOCKTIME = date.toInstant().toEpochMilli();
        event = Log4jLogEvent.newBuilder().setTimeMillis(CLOCKTIME).build();
        assertSame(Filter.Result.NEUTRAL, filter.filter(event),
                "Time " + CLOCKTIME + " is not within range: " + filter.toString());
        date = date.plusHours(2);
        CLOCKTIME = date.toInstant().toEpochMilli();
        event = Log4jLogEvent.newBuilder().setTimeMillis(CLOCKTIME).build();
        assertSame(Filter.Result.DENY, filter.filter(event),
                "Time " + CLOCKTIME + " is within range: " + filter.toString());
        date = date.plusDays(1).withHour(0);
        CLOCKTIME = date.toInstant().toEpochMilli();
        event = Log4jLogEvent.newBuilder().setTimeMillis(CLOCKTIME).build();
        assertSame(Filter.Result.NEUTRAL, filter.filter(event),
                "Time " + CLOCKTIME + " is not within range: " + filter.toString());
    }

    @Test
    public void testTime() {
        // https://garygregory.wordpress.com/2013/06/18/what-are-the-java-timezone-ids/
        final TimeFilter filter = TimeFilter.newBuilder()
                .setStart("02:00:00")
                .setEnd("03:00:00")
                .setTimezone(ZoneId.of("America/Los_Angeles"))
                .setClock(new FixedTimeClock())
                .get();
        filter.start();
        assertTrue(filter.isStarted());
        final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
        cal.set(Calendar.HOUR_OF_DAY, 2);
        CLOCKTIME = cal.getTimeInMillis();
        LogEvent event = Log4jLogEvent.newBuilder().setTimeMillis(CLOCKTIME).build();
        //assertSame(Filter.Result.NEUTRAL, filter.filter(null, Level.ERROR, null, (Object) null, (Throwable) null));
        assertSame(Filter.Result.NEUTRAL, filter.filter(event),
                "Time " + CLOCKTIME + " is not within range: " + filter.toString());

        cal.add(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 2);
        CLOCKTIME = cal.getTimeInMillis();
        event = Log4jLogEvent.newBuilder().setTimeMillis(CLOCKTIME).build();
        assertSame(Filter.Result.NEUTRAL, filter.filter(event),
                "Time " + CLOCKTIME + " is not within range: " + filter.toString());
        //assertSame(Filter.Result.NEUTRAL, filter.filter(null, Level.ERROR, null, (Object) null, (Throwable) null));

        cal.set(Calendar.HOUR_OF_DAY, 4);
        CLOCKTIME = cal.getTimeInMillis();
        event = Log4jLogEvent.newBuilder().setTimeMillis(CLOCKTIME).build();
        //assertSame(Filter.Result.DENY, filter.filter(null, Level.ERROR, null, (Object) null, (Throwable) null));
        assertSame(Filter.Result.DENY, filter.filter(event),
                "Time " + CLOCKTIME + " is within range: " + filter.toString());
    }
}
