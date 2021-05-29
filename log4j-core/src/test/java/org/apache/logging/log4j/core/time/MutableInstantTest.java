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
package org.apache.logging.log4j.core.time;

import org.apache.logging.log4j.core.time.internal.FixedPreciseClock;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class MutableInstantTest {

    @Test
    void testGetEpochSecond() {
        MutableInstant instant = new MutableInstant();
        assertEquals(0, instant.getEpochSecond(), "initial");

        instant.initFromEpochSecond(123, 456);
        assertEquals(123, instant.getEpochSecond(), "returns directly set value");

        instant.initFromEpochMilli(123456, 789012);
        assertEquals(123, instant.getEpochSecond(), "returns converted value when initialized from milllis");

        MutableInstant other = new MutableInstant();
        other.initFromEpochSecond(788, 456);
        instant.initFrom(other);

        assertEquals(788, instant.getEpochSecond(), "returns ref value when initialized from instant");
    }

    @Test
    void testGetNanoOfSecond() {
        MutableInstant instant = new MutableInstant();
        assertEquals(0, instant.getNanoOfSecond(), "initial");

        instant.initFromEpochSecond(123, 456);
        assertEquals(456, instant.getNanoOfSecond(), "returns directly set value");

        instant.initFromEpochMilli(123456, 789012);
        assertEquals(456789012, instant.getNanoOfSecond(), "returns converted value when initialized from milllis");

        MutableInstant other = new MutableInstant();
        other.initFromEpochSecond(788, 456);
        instant.initFrom(other);

        assertEquals(456, instant.getNanoOfSecond(), "returns ref value when initialized from instant");
    }

    @Test
    void testGetEpochMillisecond() {
        MutableInstant instant = new MutableInstant();
        assertEquals(0, instant.getEpochMillisecond(), "initial");

        instant.initFromEpochMilli(123, 456);
        assertEquals(123, instant.getEpochMillisecond(), "returns directly set value");

        instant.initFromEpochSecond(123, 456789012);
        assertEquals(123456, instant.getEpochMillisecond(), "returns converted value when initialized from seconds");

        MutableInstant other = new MutableInstant();
        other.initFromEpochMilli(788, 456);
        instant.initFrom(other);

        assertEquals(788, instant.getEpochMillisecond(), "returns ref value when initialized from instant");
    }

    @Test
    void getGetNanoOfMillisecond() {
        MutableInstant instant = new MutableInstant();
        assertEquals(0, instant.getNanoOfMillisecond(), "initial");

        instant.initFromEpochMilli(123, 456);
        assertEquals(456, instant.getNanoOfMillisecond(), "returns directly set value");

        instant.initFromEpochSecond(123, 456789012);
        assertEquals(789012, instant.getNanoOfMillisecond(), "returns converted value when initialized from milllis");

        MutableInstant other = new MutableInstant();
        other.initFromEpochMilli(788, 456);
        instant.initFrom(other);

        assertEquals(456, instant.getNanoOfMillisecond(), "returns ref value when initialized from instant");
    }

    @Test
    void testInitFromInstantRejectsNull() {
        assertThrows(NullPointerException.class, () -> new MutableInstant().initFrom((Instant) null));
    }

    @Test
    void testInitFromInstantCopiesValues() {
        MutableInstant other = new MutableInstant();
        other.initFromEpochSecond(788, 456);
        assertEquals(788, other.getEpochSecond(), "epochSec");
        assertEquals(456, other.getNanoOfSecond(), "NanosOfSec");

        MutableInstant instant = new MutableInstant();
        instant.initFrom(other);

        assertEquals(788, instant.getEpochSecond(), "epochSec");
        assertEquals(456, instant.getNanoOfSecond(), "NanoOfSec");
    }

    @Test
    void testInitFromEpochMillis() {
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochMilli(123456, 789012);
        assertEquals(123, instant.getEpochSecond(), "epochSec");
        assertEquals(456789012, instant.getNanoOfSecond(), "NanoOfSec");
        assertEquals(123456, instant.getEpochMillisecond(), "epochMilli");
        assertEquals(789012, instant.getNanoOfMillisecond(), "NanoOfMilli");
    }

    @Test
    void testInitFromEpochMillisRejectsNegativeNanoOfMilli() {
        MutableInstant instant = new MutableInstant();
        assertThrows(IllegalArgumentException.class, () -> instant.initFromEpochMilli(123456, -1));
    }

    @Test
    void testInitFromEpochMillisRejectsTooLargeNanoOfMilli() {
        MutableInstant instant = new MutableInstant();
        assertThrows(IllegalArgumentException.class, () -> instant.initFromEpochMilli(123456, 1000_000));
    }

    @Test
    void testInitFromEpochMillisAcceptsTooMaxNanoOfMilli() {
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochMilli(123456, 999_999);
        assertEquals(999_999, instant.getNanoOfMillisecond(), "NanoOfMilli");
    }

    @Test
    void testInitFromEpochSecond() {
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochSecond(123, 456789012);
        assertEquals(123, instant.getEpochSecond(), "epochSec");
        assertEquals(456789012, instant.getNanoOfSecond(), "NanoOfSec");
        assertEquals(123456, instant.getEpochMillisecond(), "epochMilli");
        assertEquals(789012, instant.getNanoOfMillisecond(), "NanoOfMilli");
    }

    @Test
    void testInitFromEpochSecondRejectsNegativeNanoOfMilli() {
        MutableInstant instant = new MutableInstant();
        assertThrows(IllegalArgumentException.class, () -> instant.initFromEpochSecond(123456, -1));
    }

    @Test
    void testInitFromEpochSecondRejectsTooLargeNanoOfMilli() {
        MutableInstant instant = new MutableInstant();
        assertThrows(IllegalArgumentException.class, () -> instant.initFromEpochSecond(123456, 1000_000_000));
    }

    @Test
    void testInitFromEpochSecondAcceptsTooMaxNanoOfMilli() {
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochSecond(123456, 999_999_999);
        assertEquals(999_999_999, instant.getNanoOfSecond(), "NanoOfSec");
    }

    @Test
    void testInstantToMillisAndNanos() {
        long[] values = new long[2];
        MutableInstant.instantToMillisAndNanos(123456, 999_999_999, values);
        assertEquals(123456_999, values[0]);
        assertEquals(999_999, values[1]);
    }

    @Test
    void testInitFromClock() {
        MutableInstant instant = new MutableInstant();

        PreciseClock clock = new FixedPreciseClock(123456, 789012);
        instant.initFrom(clock);

        assertEquals(123456, instant.getEpochMillisecond());
        assertEquals(789012, instant.getNanoOfMillisecond());
        assertEquals(123, instant.getEpochSecond());
        assertEquals(456789012, instant.getNanoOfSecond());
    }

    @Test
    void testEquals() {
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochSecond(123, 456789012);

        MutableInstant instant2 = new MutableInstant();
        instant2.initFromEpochMilli(123456, 789012);

        assertEquals(instant, instant2);
    }

    @Test
    void testHashCode() {
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochSecond(123, 456789012);

        MutableInstant instant2 = new MutableInstant();
        instant2.initFromEpochMilli(123456, 789012);

        assertEquals(instant.hashCode(), instant2.hashCode());


        instant2.initFromEpochMilli(123456, 789013);
        assertNotEquals(instant.hashCode(), instant2.hashCode());
    }

    @Test
    void testToString() {
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochSecond(123, 456789012);
        assertEquals("MutableInstant[epochSecond=123, nano=456789012]", instant.toString());

        instant.initFromEpochMilli(123456, 789012);
        assertEquals("MutableInstant[epochSecond=123, nano=456789012]", instant.toString());
    }

    @Test
    void testTemporalAccessor() {
        java.time.Instant javaInstant = java.time.Instant.parse("2020-05-10T22:09:04.123456789Z");
        MutableInstant log4jInstant = new MutableInstant();
        log4jInstant.initFromEpochSecond(javaInstant.getEpochSecond(), javaInstant.getNano());
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'")
                .withZone(ZoneId.of("UTC"));
        assertEquals(formatter.format(javaInstant), formatter.format(log4jInstant));
    }

}
