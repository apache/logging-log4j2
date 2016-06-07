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

package org.apache.logging.log4j.core.appender.rolling.action;

import org.apache.logging.log4j.core.appender.rolling.action.Duration;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the Duration class.
 */
public class DurationTest {

    @Test(expected = NullPointerException.class)
    public void testParseFailsIfNullText() {
        Duration.parse(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseFailsIfInvalidPattern() {
        Duration.parse("abc");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseFailsIfSectionsOutOfOrder() {
        Duration.parse("P4DT2M1S3H");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseFailsIfTButMissingTime() {
        Duration.parse("P1dT");
    }

    @Test
    public void testParseIsCaseInsensitive() {
        assertEquals("P4DT3H2M1S", Duration.parse("p4dt3h2m1s").toString());
    }

    @Test
    public void testParseAllowsOverflows() {
        assertEquals(1000 * 70, Duration.parse("PT70S").toMillis());
        assertEquals(1000 * 70 * 60, Duration.parse("PT70M").toMillis());
        assertEquals(1000 * 25 * 60 * 60, Duration.parse("PT25H").toMillis());
    }

    @Test
    public void testToMillis() {
        assertEquals(0, Duration.ZERO.toMillis());
        assertEquals(1000, Duration.parse("PT1S").toMillis());
        assertEquals(1000 * 2 * 60, Duration.parse("PT2M").toMillis());
        assertEquals(1000 * 3 * 60 * 60, Duration.parse("PT3H").toMillis());
        assertEquals(1000 * 4 * 24 * 60 * 60, Duration.parse("P4D").toMillis());
        final long expected = (1000 * 4 * 24 * 60 * 60) + (1000 * 3 * 60 * 60) + (1000 * 2 * 60) + 1000;
        assertEquals(expected, Duration.parse("P4DT3H2M1S").toMillis());
    }

    @Test
    public void testToString() {
        assertEquals("PT0S", Duration.ZERO.toString());
        assertEquals("PT1S", Duration.parse("PT1S").toString());
        assertEquals("PT2M1S", Duration.parse("PT2M1S").toString());
        assertEquals("PT3H2M1S", Duration.parse("PT3H2M1S").toString());
        assertEquals("P4DT3H2M1S", Duration.parse("P4DT3H2M1S").toString());
    }

    @Test
    public void testPrefixPNotRequired() {
        assertEquals("PT1S", Duration.parse("T1S").toString());
        assertEquals("PT2M1S", Duration.parse("T2M1S").toString());
        assertEquals("PT3H2M1S", Duration.parse("T3H2M1S").toString());
        assertEquals("P4DT3H2M1S", Duration.parse("4DT3H2M1S").toString());
    }

    @Test
    public void testInfixTNotRequired() {
        assertEquals("PT1S", Duration.parse("P1S").toString());
        assertEquals("PT2M1S", Duration.parse("P2M1S").toString());
        assertEquals("PT3H2M1S", Duration.parse("P3H2M1S").toString());
        assertEquals("P4DT3H2M1S", Duration.parse("P4D3H2M1S").toString());
    }

    @Test
    public void testPrefixPAndInfixTNotRequired() {
        assertEquals("PT1S", Duration.parse("1S").toString());
        assertEquals("PT2M1S", Duration.parse("2M1S").toString());
        assertEquals("PT3H2M1S", Duration.parse("3H2M1S").toString());
        assertEquals("P4DT3H2M1S", Duration.parse("4D3H2M1S").toString());
    }

    @Test
    public void testCompareTo() {
        assertEquals(-1, Duration.parse("PT1S").compareTo(Duration.parse("PT2S")));
        assertEquals(-1, Duration.parse("PT1M").compareTo(Duration.parse("PT2M")));
        assertEquals(-1, Duration.parse("PT1H").compareTo(Duration.parse("PT2H")));
        assertEquals(-1, Duration.parse("P1D").compareTo(Duration.parse("P2D")));

        assertEquals(0, Duration.parse("PT1S").compareTo(Duration.parse("PT1S")));
        assertEquals(0, Duration.parse("PT1M").compareTo(Duration.parse("PT1M")));
        assertEquals(0, Duration.parse("PT1H").compareTo(Duration.parse("PT1H")));
        assertEquals(0, Duration.parse("P1D").compareTo(Duration.parse("P1D")));

        assertEquals(1, Duration.parse("PT2S").compareTo(Duration.parse("PT1S")));
        assertEquals(1, Duration.parse("PT2M").compareTo(Duration.parse("PT1M")));
        assertEquals(1, Duration.parse("PT2H").compareTo(Duration.parse("PT1H")));
        assertEquals(1, Duration.parse("P2D").compareTo(Duration.parse("P1D")));

        assertEquals(0, Duration.parse("PT1M").compareTo(Duration.parse("PT60S")));
        assertEquals(0, Duration.parse("PT1H").compareTo(Duration.parse("PT60M")));
        assertEquals(0, Duration.parse("PT1H").compareTo(Duration.parse("PT3600S")));
        assertEquals(0, Duration.parse("P1D").compareTo(Duration.parse("PT24H")));
        assertEquals(0, Duration.parse("P1D").compareTo(Duration.parse("PT1440M")));
    }

    @Test
    public void testEquals() {
        assertNotEquals(Duration.parse("PT1S"),(Duration.parse("PT2S")));
        assertNotEquals(Duration.parse("PT1M"),(Duration.parse("PT2M")));
        assertNotEquals(Duration.parse("PT1H"),(Duration.parse("PT2H")));
        assertNotEquals(Duration.parse("P1D"),(Duration.parse("P2D")));

        assertEquals( Duration.parse("PT1S"),(Duration.parse("PT1S")));
        assertEquals( Duration.parse("PT1M"),(Duration.parse("PT1M")));
        assertEquals( Duration.parse("PT1H"),(Duration.parse("PT1H")));
        assertEquals( Duration.parse("P1D"),(Duration.parse("P1D")));

        assertEquals( Duration.parse("PT1M"),(Duration.parse("PT60S")));
        assertEquals( Duration.parse("PT1H"),(Duration.parse("PT60M")));
        assertEquals( Duration.parse("PT1H"),(Duration.parse("PT3600S")));
        assertEquals( Duration.parse("P1D"),(Duration.parse("PT24H")));
        assertEquals(Duration.parse("P1D"), (Duration.parse("PT1440M")));
    }

}
