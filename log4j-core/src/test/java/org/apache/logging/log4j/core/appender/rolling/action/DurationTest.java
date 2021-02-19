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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests the Duration class.
 */
public class DurationTest {

    @Test
    public void testParseFailsIfNullText() {
        assertThatThrownBy(() -> Duration.parse(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testParseFailsIfInvalidPattern() {
        assertThatThrownBy(() -> Duration.parse("abc")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testParseFailsIfSectionsOutOfOrder() {
        assertThatThrownBy(() -> Duration.parse("P4DT2M1S3H")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testParseFailsIfTButMissingTime() {
        assertThatThrownBy(() -> Duration.parse("P1dT")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testParseIsCaseInsensitive() {
        assertThat(Duration.parse("p4dt3h2m1s").toString()).isEqualTo("P4DT3H2M1S");
    }

    @Test
    public void testParseAllowsOverflows() {
        assertThat(Duration.parse("PT70S").toMillis()).isEqualTo(1000 * 70);
        assertThat(Duration.parse("PT70M").toMillis()).isEqualTo(1000 * 70 * 60);
        assertThat(Duration.parse("PT25H").toMillis()).isEqualTo(1000 * 25 * 60 * 60);
    }

    @Test
    public void testToMillis() {
        assertThat(Duration.ZERO.toMillis()).isEqualTo(0);
        assertThat(Duration.parse("PT1S").toMillis()).isEqualTo(1000);
        assertThat(Duration.parse("PT2M").toMillis()).isEqualTo(1000 * 2 * 60);
        assertThat(Duration.parse("PT3H").toMillis()).isEqualTo(1000 * 3 * 60 * 60);
        assertThat(Duration.parse("P4D").toMillis()).isEqualTo(1000 * 4 * 24 * 60 * 60);
        final long expected = (1000 * 4 * 24 * 60 * 60) + (1000 * 3 * 60 * 60) + (1000 * 2 * 60) + 1000;
        assertThat(Duration.parse("P4DT3H2M1S").toMillis()).isEqualTo(expected);
    }

    @Test
    public void testToString() {
        assertThat(Duration.ZERO.toString()).isEqualTo("PT0S");
        assertThat(Duration.parse("PT1S").toString()).isEqualTo("PT1S");
        assertThat(Duration.parse("PT2M1S").toString()).isEqualTo("PT2M1S");
        assertThat(Duration.parse("PT3H2M1S").toString()).isEqualTo("PT3H2M1S");
        assertThat(Duration.parse("P4DT3H2M1S").toString()).isEqualTo("P4DT3H2M1S");
    }

    @Test
    public void testPrefixPNotRequired() {
        assertThat(Duration.parse("T1S").toString()).isEqualTo("PT1S");
        assertThat(Duration.parse("T2M1S").toString()).isEqualTo("PT2M1S");
        assertThat(Duration.parse("T3H2M1S").toString()).isEqualTo("PT3H2M1S");
        assertThat(Duration.parse("4DT3H2M1S").toString()).isEqualTo("P4DT3H2M1S");
    }

    @Test
    public void testInfixTNotRequired() {
        assertThat(Duration.parse("P1S").toString()).isEqualTo("PT1S");
        assertThat(Duration.parse("P2M1S").toString()).isEqualTo("PT2M1S");
        assertThat(Duration.parse("P3H2M1S").toString()).isEqualTo("PT3H2M1S");
        assertThat(Duration.parse("P4D3H2M1S").toString()).isEqualTo("P4DT3H2M1S");
    }

    @Test
    public void testPrefixPAndInfixTNotRequired() {
        assertThat(Duration.parse("1S").toString()).isEqualTo("PT1S");
        assertThat(Duration.parse("2M1S").toString()).isEqualTo("PT2M1S");
        assertThat(Duration.parse("3H2M1S").toString()).isEqualTo("PT3H2M1S");
        assertThat(Duration.parse("4D3H2M1S").toString()).isEqualTo("P4DT3H2M1S");
    }

    @Test
    public void testCompareTo() {
        assertThat(Duration.parse("PT1S").compareTo(Duration.parse("PT2S"))).isEqualTo(-1);
        assertThat(Duration.parse("PT1M").compareTo(Duration.parse("PT2M"))).isEqualTo(-1);
        assertThat(Duration.parse("PT1H").compareTo(Duration.parse("PT2H"))).isEqualTo(-1);
        assertThat(Duration.parse("P1D").compareTo(Duration.parse("P2D"))).isEqualTo(-1);

        assertThat(Duration.parse("PT1S").compareTo(Duration.parse("PT1S"))).isEqualTo(0);
        assertThat(Duration.parse("PT1M").compareTo(Duration.parse("PT1M"))).isEqualTo(0);
        assertThat(Duration.parse("PT1H").compareTo(Duration.parse("PT1H"))).isEqualTo(0);
        assertThat(Duration.parse("P1D").compareTo(Duration.parse("P1D"))).isEqualTo(0);

        assertThat(Duration.parse("PT2S").compareTo(Duration.parse("PT1S"))).isEqualTo(1);
        assertThat(Duration.parse("PT2M").compareTo(Duration.parse("PT1M"))).isEqualTo(1);
        assertThat(Duration.parse("PT2H").compareTo(Duration.parse("PT1H"))).isEqualTo(1);
        assertThat(Duration.parse("P2D").compareTo(Duration.parse("P1D"))).isEqualTo(1);

        assertThat(Duration.parse("PT1M").compareTo(Duration.parse("PT60S"))).isEqualTo(0);
        assertThat(Duration.parse("PT1H").compareTo(Duration.parse("PT60M"))).isEqualTo(0);
        assertThat(Duration.parse("PT1H").compareTo(Duration.parse("PT3600S"))).isEqualTo(0);
        assertThat(Duration.parse("P1D").compareTo(Duration.parse("PT24H"))).isEqualTo(0);
        assertThat(Duration.parse("P1D").compareTo(Duration.parse("PT1440M"))).isEqualTo(0);
    }

    @Test
    public void testEquals() {
        assertThat((Duration.parse("PT2S"))).isNotEqualTo(Duration.parse("PT1S"));
        assertThat((Duration.parse("PT2M"))).isNotEqualTo(Duration.parse("PT1M"));
        assertThat((Duration.parse("PT2H"))).isNotEqualTo(Duration.parse("PT1H"));
        assertThat((Duration.parse("P2D"))).isNotEqualTo(Duration.parse("P1D"));

        assertThat((Duration.parse("PT1S"))).isEqualTo(Duration.parse("PT1S"));
        assertThat((Duration.parse("PT1M"))).isEqualTo(Duration.parse("PT1M"));
        assertThat((Duration.parse("PT1H"))).isEqualTo(Duration.parse("PT1H"));
        assertThat((Duration.parse("P1D"))).isEqualTo(Duration.parse("P1D"));

        assertThat((Duration.parse("PT60S"))).isEqualTo(Duration.parse("PT1M"));
        assertThat((Duration.parse("PT60M"))).isEqualTo(Duration.parse("PT1H"));
        assertThat((Duration.parse("PT3600S"))).isEqualTo(Duration.parse("PT1H"));
        assertThat((Duration.parse("PT24H"))).isEqualTo(Duration.parse("P1D"));
        assertThat((Duration.parse("PT1440M"))).isEqualTo(Duration.parse("P1D"));
    }

}
