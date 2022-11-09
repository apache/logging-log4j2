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
package org.apache.logging.log4j.util;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

enum TimeUnit {
    NANOS("ns,nano,nanos,nanosecond,nanoseconds", ChronoUnit.NANOS),
    MICROS("us,micro,micros,microsecond,microseconds", ChronoUnit.MICROS),
    MILLIS("ms,milli,millis,millsecond,milliseconds", ChronoUnit.MILLIS),
    SECONDS("s,second,seconds", ChronoUnit.SECONDS),
    MINUTES("m,minute,minutes", ChronoUnit.MINUTES),
    HOURS("h,hour,hours", ChronoUnit.HOURS),
    DAYS("d,day,days", ChronoUnit.DAYS);

    private final String[] descriptions;
    private final ChronoUnit timeUnit;

    TimeUnit(final String descriptions, final ChronoUnit timeUnit) {
        this.descriptions = descriptions.split(",");
        this.timeUnit = timeUnit;
    }

    ChronoUnit getTimeUnit() {
        return this.timeUnit;
    }

    static Duration getDuration(final String time) {
        final String value = time.trim();
        TemporalUnit temporalUnit = ChronoUnit.MILLIS;
        long timeVal = 0;
        for (final TimeUnit timeUnit : values()) {
            for (final String suffix : timeUnit.descriptions) {
                if (value.endsWith(suffix)) {
                    temporalUnit = timeUnit.timeUnit;
                    timeVal = Long.parseLong(value.substring(0, value.length() - suffix.length()));
                }
            }
        }
        return Duration.of(timeVal, temporalUnit);
    }
}
