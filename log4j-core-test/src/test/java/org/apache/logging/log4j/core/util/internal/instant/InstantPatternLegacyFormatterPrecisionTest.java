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
package org.apache.logging.log4j.core.util.internal.instant;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;

/**
 * Regression for issue #3816: legacy fractional-second letter {@code n} must not be
 * treated as always-nanosecond when computing cache precision.
 */
class InstantPatternLegacyFormatterPrecisionTest {

    @Test
    void microsPatternUsingLegacyNUsesMicroPrecision() {
        final InstantPatternFormatter legacy = InstantPatternFormatter.newBuilder()
                .setLegacyFormattersEnabled(true)
                .setPattern("yyyy-MM-dd HH:mm:ss,nnnnnn")
                .setLocale(Locale.US)
                .setTimeZone(TimeZone.getTimeZone("UTC"))
                .build();
        final InstantPatternFormatter modern = InstantPatternFormatter.newBuilder()
                .setLegacyFormattersEnabled(false)
                .setPattern("yyyy-MM-dd HH:mm:ss,SSSSSS")
                .setLocale(Locale.US)
                .setTimeZone(TimeZone.getTimeZone("UTC"))
                .build();
        assertThat(legacy.getPrecision()).isEqualTo(ChronoUnit.MICROS);
        assertThat(legacy.getPrecision()).isEqualTo(modern.getPrecision());
    }

    @Test
    void millisPatternUsingLegacySssUsesMilliPrecision() {
        final InstantPatternFormatter legacy = InstantPatternFormatter.newBuilder()
                .setLegacyFormattersEnabled(true)
                .setPattern("yyyy-MM-dd HH:mm:ss,SSS")
                .setLocale(Locale.US)
                .setTimeZone(TimeZone.getTimeZone("UTC"))
                .build();
        assertThat(legacy.getPrecision()).isEqualTo(ChronoUnit.MILLIS);
    }
}
