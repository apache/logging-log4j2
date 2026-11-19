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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TimeFilterOvernightTest {

    @ParameterizedTest
    @CsvSource({
        "2026-11-14, UTC",
        "2026-11-14, Asia/Kolkata",
        "2020-03-08, America/Los_Angeles",
        "2020-11-01, America/Los_Angeles"
    })
    void matchesAfterMidnightOnStartup(final LocalDate date, final ZoneId zone) {
        final TimeFilter filter = overnightFilter(date, zone);

        assertResult(filter, date, LocalTime.of(1, 0), zone, Result.ACCEPT);
        assertResult(filter, date, LocalTime.NOON, zone, Result.DENY);
        assertResult(filter, date, LocalTime.of(22, 0), zone, Result.ACCEPT);
        assertResult(filter, date.plusDays(1), LocalTime.of(1, 0), zone, Result.ACCEPT);
    }

    @ParameterizedTest
    @CsvSource({
        "2026-11-14, UTC",
        "2026-11-14, Asia/Kolkata",
        "2020-03-08, America/Los_Angeles",
        "2020-11-01, America/Los_Angeles"
    })
    void matchesAfterMidnightFollowingIdleDays(final LocalDate date, final ZoneId zone) {
        final TimeFilter filter = overnightFilter(date.minusDays(2), zone);

        assertResult(filter, date.minusDays(2), LocalTime.of(23, 0), zone, Result.ACCEPT);
        assertResult(filter, date, LocalTime.of(1, 0), zone, Result.ACCEPT);
        assertResult(filter, date.plusDays(3), LocalTime.of(1, 0), zone, Result.ACCEPT);
    }

    @ParameterizedTest
    @CsvSource({
        "00:00:00, ACCEPT",
        "01:00:00, ACCEPT",
        "02:00:00, ACCEPT",
        "02:00:00.001, DENY",
        "12:00:00, DENY",
        "21:59:59.999, DENY",
        "22:00:00, ACCEPT",
        "23:59:59.999, ACCEPT"
    })
    void preservesInclusiveBoundaries(final LocalTime time, final Result expected) {
        final LocalDate date = LocalDate.of(2026, 11, 14);
        final ZoneId zone = ZoneId.of("UTC");
        assertResult(overnightFilter(date, zone), date, time, zone, expected);
    }

    private static TimeFilter overnightFilter(final LocalDate date, final ZoneId zone) {
        return new TimeFilter(LocalTime.of(22, 0), LocalTime.of(2, 0), zone, Result.ACCEPT, Result.DENY, date);
    }

    private static void assertResult(
            final TimeFilter filter,
            final LocalDate date,
            final LocalTime time,
            final ZoneId zone,
            final Result expected) {
        final long timestamp = date.atTime(time).atZone(zone).toInstant().toEpochMilli();
        assertEquals(
                expected,
                filter.filter(
                        Log4jLogEvent.newBuilder().setTimeMillis(timestamp).build()),
                () -> date + " " + time + " " + zone + " with " + filter);
    }
}
