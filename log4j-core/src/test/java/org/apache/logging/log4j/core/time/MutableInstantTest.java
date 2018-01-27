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
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class MutableInstantTest {

    @Test
    public void testGetEpochSecond() {
        MutableInstant instant = new MutableInstant();
        assertEquals("initial", 0, instant.getEpochSecond());

        instant.initFromEpochSecond(123, 456);
        assertEquals("returns directly set value", 123, instant.getEpochSecond());

        instant.initFromEpochMilli(123456, 789012);
        assertEquals("returns converted value when initialized from milllis", 123, instant.getEpochSecond());

        MutableInstant other = new MutableInstant();
        other.initFromEpochSecond(788, 456);
        instant.initFrom(other);

        assertEquals("returns ref value when initialized from instant", 788, instant.getEpochSecond());
    }

    @Test
    public void testGetNanoOfSecond() {
        MutableInstant instant = new MutableInstant();
        assertEquals("initial", 0, instant.getNanoOfSecond());

        instant.initFromEpochSecond(123, 456);
        assertEquals("returns directly set value", 456, instant.getNanoOfSecond());

        instant.initFromEpochMilli(123456, 789012);
        assertEquals("returns converted value when initialized from milllis", 456789012, instant.getNanoOfSecond());

        MutableInstant other = new MutableInstant();
        other.initFromEpochSecond(788, 456);
        instant.initFrom(other);

        assertEquals("returns ref value when initialized from instant", 456, instant.getNanoOfSecond());
    }

    @Test
    public void testGetEpochMillisecond() {
        MutableInstant instant = new MutableInstant();
        assertEquals("initial", 0, instant.getEpochMillisecond());

        instant.initFromEpochMilli(123, 456);
        assertEquals("returns directly set value", 123, instant.getEpochMillisecond());

        instant.initFromEpochSecond(123, 456789012);
        assertEquals("returns converted value when initialized from seconds", 123456, instant.getEpochMillisecond());

        MutableInstant other = new MutableInstant();
        other.initFromEpochMilli(788, 456);
        instant.initFrom(other);

        assertEquals("returns ref value when initialized from instant", 788, instant.getEpochMillisecond());
    }

    @Test
    public void getGetNanoOfMillisecond() {
        MutableInstant instant = new MutableInstant();
        assertEquals("initial", 0, instant.getNanoOfMillisecond());

        instant.initFromEpochMilli(123, 456);
        assertEquals("returns directly set value", 456, instant.getNanoOfMillisecond());

        instant.initFromEpochSecond(123, 456789012);
        assertEquals("returns converted value when initialized from milllis", 789012, instant.getNanoOfMillisecond());

        MutableInstant other = new MutableInstant();
        other.initFromEpochMilli(788, 456);
        instant.initFrom(other);

        assertEquals("returns ref value when initialized from instant", 456, instant.getNanoOfMillisecond());
    }

    @Test(expected = NullPointerException.class)
    public void testInitFromInstantRejectsNull() {
        new MutableInstant().initFrom((Instant) null);
    }

    @Test
    public void testInitFromInstantCopiesValues() {
        MutableInstant other = new MutableInstant();
        other.initFromEpochSecond(788, 456);
        assertEquals("epochSec", 788, other.getEpochSecond());
        assertEquals("NanosOfSec", 456, other.getNanoOfSecond());

        MutableInstant instant = new MutableInstant();
        instant.initFrom(other);

        assertEquals("epochSec", 788, instant.getEpochSecond());
        assertEquals("NanoOfSec", 456, instant.getNanoOfSecond());
    }

    @Test
    public void testInitFromEpochMillis() {
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochMilli(123456, 789012);
        assertEquals("epochSec", 123, instant.getEpochSecond());
        assertEquals("NanoOfSec", 456789012, instant.getNanoOfSecond());
        assertEquals("epochMilli", 123456, instant.getEpochMillisecond());
        assertEquals("NanoOfMilli", 789012, instant.getNanoOfMillisecond());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInitFromEpochMillisRejectsNegativeNanoOfMilli() {
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochMilli(123456, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInitFromEpochMillisRejectsTooLargeNanoOfMilli() {
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochMilli(123456, 1000_000);
    }

    @Test
    public void testInitFromEpochMillisAcceptsTooMaxNanoOfMilli() {
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochMilli(123456, 999_999);
        assertEquals("NanoOfMilli", 999_999, instant.getNanoOfMillisecond());
    }

    @Test
    public void testInitFromEpochSecond() {
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochSecond(123, 456789012);
        assertEquals("epochSec", 123, instant.getEpochSecond());
        assertEquals("NanoOfSec", 456789012, instant.getNanoOfSecond());
        assertEquals("epochMilli", 123456, instant.getEpochMillisecond());
        assertEquals("NanoOfMilli", 789012, instant.getNanoOfMillisecond());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInitFromEpochSecondRejectsNegativeNanoOfMilli() {
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochSecond(123456, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInitFromEpochSecondRejectsTooLargeNanoOfMilli() {
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochSecond(123456, 1000_000_000);
    }

    @Test
    public void testInitFromEpochSecondAcceptsTooMaxNanoOfMilli() {
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochSecond(123456, 999_999_999);
        assertEquals("NanoOfSec", 999_999_999, instant.getNanoOfSecond());
    }

    @Test
    public void testInstantToMillisAndNanos() {
        long[] values = new long[2];
        MutableInstant.instantToMillisAndNanos(123456, 999_999_999, values);
        assertEquals(123456_999, values[0]);
        assertEquals(999_999, values[1]);
    }

    @Test
    public void testInitFromClock() {
        MutableInstant instant = new MutableInstant();

        PreciseClock clock = new FixedPreciseClock(123456, 789012);
        instant.initFrom(clock);

        assertEquals(123456, instant.getEpochMillisecond());
        assertEquals(789012, instant.getNanoOfMillisecond());
        assertEquals(123, instant.getEpochSecond());
        assertEquals(456789012, instant.getNanoOfSecond());
    }

    @Test
    public void testEquals() {
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochSecond(123, 456789012);

        MutableInstant instant2 = new MutableInstant();
        instant2.initFromEpochMilli(123456, 789012);

        assertEquals(instant, instant2);
    }

    @Test
    public void testHashCode() {
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochSecond(123, 456789012);

        MutableInstant instant2 = new MutableInstant();
        instant2.initFromEpochMilli(123456, 789012);

        assertEquals(instant.hashCode(), instant2.hashCode());


        instant2.initFromEpochMilli(123456, 789013);
        assertNotEquals(instant.hashCode(), instant2.hashCode());
    }

    @Test
    public void testToString() {
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochSecond(123, 456789012);
        assertEquals("MutableInstant[epochSecond=123, nano=456789012]", instant.toString());

        instant.initFromEpochMilli(123456, 789012);
        assertEquals("MutableInstant[epochSecond=123, nano=456789012]", instant.toString());
    }
}