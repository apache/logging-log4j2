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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.logging.log4j.core.time.internal.FixedPreciseClock;
import org.junit.jupiter.api.Test;

public class MutableInstantTest {

    @Test
    public void testGetEpochSecond() {
        MutableInstant instant = new MutableInstant();
        assertThat(instant.getEpochSecond()).describedAs("initial").isEqualTo(0);

        instant.initFromEpochSecond(123, 456);
        assertThat(instant.getEpochSecond()).describedAs("returns directly set value").isEqualTo(123);

        instant.initFromEpochMilli(123456, 789012);
        assertThat(instant.getEpochSecond()).describedAs("returns converted value when initialized from milllis").isEqualTo(123);

        MutableInstant other = new MutableInstant();
        other.initFromEpochSecond(788, 456);
        instant.initFrom(other);

        assertThat(instant.getEpochSecond()).describedAs("returns ref value when initialized from instant").isEqualTo(788);
    }

    @Test
    public void testGetNanoOfSecond() {
        MutableInstant instant = new MutableInstant();
        assertThat(instant.getNanoOfSecond()).describedAs("initial").isEqualTo(0);

        instant.initFromEpochSecond(123, 456);
        assertThat(instant.getNanoOfSecond()).describedAs("returns directly set value").isEqualTo(456);

        instant.initFromEpochMilli(123456, 789012);
        assertThat(instant.getNanoOfSecond()).describedAs("returns converted value when initialized from milllis").isEqualTo(456789012);

        MutableInstant other = new MutableInstant();
        other.initFromEpochSecond(788, 456);
        instant.initFrom(other);

        assertThat(instant.getNanoOfSecond()).describedAs("returns ref value when initialized from instant").isEqualTo(456);
    }

    @Test
    public void testGetEpochMillisecond() {
        MutableInstant instant = new MutableInstant();
        assertThat(instant.getEpochMillisecond()).describedAs("initial").isEqualTo(0);

        instant.initFromEpochMilli(123, 456);
        assertThat(instant.getEpochMillisecond()).describedAs("returns directly set value").isEqualTo(123);

        instant.initFromEpochSecond(123, 456789012);
        assertThat(instant.getEpochMillisecond()).describedAs("returns converted value when initialized from seconds").isEqualTo(123456);

        MutableInstant other = new MutableInstant();
        other.initFromEpochMilli(788, 456);
        instant.initFrom(other);

        assertThat(instant.getEpochMillisecond()).describedAs("returns ref value when initialized from instant").isEqualTo(788);
    }

    @Test
    public void getGetNanoOfMillisecond() {
        MutableInstant instant = new MutableInstant();
        assertThat(instant.getNanoOfMillisecond()).describedAs("initial").isEqualTo(0);

        instant.initFromEpochMilli(123, 456);
        assertThat(instant.getNanoOfMillisecond()).describedAs("returns directly set value").isEqualTo(456);

        instant.initFromEpochSecond(123, 456789012);
        assertThat(instant.getNanoOfMillisecond()).describedAs("returns converted value when initialized from milllis").isEqualTo(789012);

        MutableInstant other = new MutableInstant();
        other.initFromEpochMilli(788, 456);
        instant.initFrom(other);

        assertThat(instant.getNanoOfMillisecond()).describedAs("returns ref value when initialized from instant").isEqualTo(456);
    }

    @Test
    public void testInitFromInstantRejectsNull() {
        assertThatThrownBy(() -> new MutableInstant().initFrom((Instant) null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testInitFromInstantCopiesValues() {
        MutableInstant other = new MutableInstant();
        other.initFromEpochSecond(788, 456);
        assertThat(other.getEpochSecond()).describedAs("epochSec").isEqualTo(788);
        assertThat(other.getNanoOfSecond()).describedAs("NanosOfSec").isEqualTo(456);

        MutableInstant instant = new MutableInstant();
        instant.initFrom(other);

        assertThat(instant.getEpochSecond()).describedAs("epochSec").isEqualTo(788);
        assertThat(instant.getNanoOfSecond()).describedAs("NanoOfSec").isEqualTo(456);
    }

    @Test
    public void testInitFromEpochMillis() {
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochMilli(123456, 789012);
        assertThat(instant.getEpochSecond()).describedAs("epochSec").isEqualTo(123);
        assertThat(instant.getNanoOfSecond()).describedAs("NanoOfSec").isEqualTo(456789012);
        assertThat(instant.getEpochMillisecond()).describedAs("epochMilli").isEqualTo(123456);
        assertThat(instant.getNanoOfMillisecond()).describedAs("NanoOfMilli").isEqualTo(789012);
    }

    @Test
    public void testInitFromEpochMillisRejectsNegativeNanoOfMilli() {
        MutableInstant instant = new MutableInstant();
        assertThatThrownBy(() -> instant.initFromEpochMilli(123456, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testInitFromEpochMillisRejectsTooLargeNanoOfMilli() {
        MutableInstant instant = new MutableInstant();
        assertThatThrownBy(() -> instant.initFromEpochMilli(123456, 1000_000)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testInitFromEpochMillisAcceptsTooMaxNanoOfMilli() {
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochMilli(123456, 999_999);
        assertThat(instant.getNanoOfMillisecond()).describedAs("NanoOfMilli").isEqualTo(999_999);
    }

    @Test
    public void testInitFromEpochSecond() {
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochSecond(123, 456789012);
        assertThat(instant.getEpochSecond()).describedAs("epochSec").isEqualTo(123);
        assertThat(instant.getNanoOfSecond()).describedAs("NanoOfSec").isEqualTo(456789012);
        assertThat(instant.getEpochMillisecond()).describedAs("epochMilli").isEqualTo(123456);
        assertThat(instant.getNanoOfMillisecond()).describedAs("NanoOfMilli").isEqualTo(789012);
    }

    @Test
    public void testInitFromEpochSecondRejectsNegativeNanoOfMilli() {
        MutableInstant instant = new MutableInstant();
        assertThatThrownBy(() -> instant.initFromEpochSecond(123456, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testInitFromEpochSecondRejectsTooLargeNanoOfMilli() {
        MutableInstant instant = new MutableInstant();
        assertThatThrownBy(() -> instant.initFromEpochSecond(123456, 1000_000_000)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testInitFromEpochSecondAcceptsTooMaxNanoOfMilli() {
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochSecond(123456, 999_999_999);
        assertThat(instant.getNanoOfSecond()).describedAs("NanoOfSec").isEqualTo(999_999_999);
    }

    @Test
    public void testInstantToMillisAndNanos() {
        long[] values = new long[2];
        MutableInstant.instantToMillisAndNanos(123456, 999_999_999, values);
        assertThat(values[0]).isEqualTo(123456_999);
        assertThat(values[1]).isEqualTo(999_999);
    }

    @Test
    public void testInitFromClock() {
        MutableInstant instant = new MutableInstant();

        PreciseClock clock = new FixedPreciseClock(123456, 789012);
        instant.initFrom(clock);

        assertThat(instant.getEpochMillisecond()).isEqualTo(123456);
        assertThat(instant.getNanoOfMillisecond()).isEqualTo(789012);
        assertThat(instant.getEpochSecond()).isEqualTo(123);
        assertThat(instant.getNanoOfSecond()).isEqualTo(456789012);
    }

    @Test
    public void testEquals() {
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochSecond(123, 456789012);

        MutableInstant instant2 = new MutableInstant();
        instant2.initFromEpochMilli(123456, 789012);

        assertThat(instant2).isEqualTo(instant);
    }

    @Test
    public void testHashCode() {
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochSecond(123, 456789012);

        MutableInstant instant2 = new MutableInstant();
        instant2.initFromEpochMilli(123456, 789012);

        assertThat(instant2.hashCode()).isEqualTo(instant.hashCode());


        instant2.initFromEpochMilli(123456, 789013);
        assertThat(instant2.hashCode()).isNotEqualTo(instant.hashCode());
    }

    @Test
    public void testToString() {
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochSecond(123, 456789012);
        assertThat(instant.toString()).isEqualTo("MutableInstant[epochSecond=123, nano=456789012]");

        instant.initFromEpochMilli(123456, 789012);
        assertThat(instant.toString()).isEqualTo("MutableInstant[epochSecond=123, nano=456789012]");
    }
}
