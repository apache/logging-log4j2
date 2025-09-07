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
package org.apache.logging.log4j.core.pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.core.util.internal.instant.InstantPatternFormatter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class NamedInstantPatternTest {

    @ParameterizedTest
    @EnumSource(NamedInstantPattern.class)
    void compatibilityOfLegacyPattern(NamedInstantPattern namedPattern) {
        if (namedPattern == NamedInstantPattern.ISO8601_OFFSET_DATE_TIME_HH) {
            ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(Instant.now());
            assumeThat(offset.getTotalSeconds() % 3600 == 0)
                    .withFailMessage(
                            "Skipping test: ISO8601_OFFSET_DATE_TIME_HH requires a whole-hour offset, but system offset is %s",
                            offset)
                    .isTrue();
        }

        InstantPatternFormatter legacyFormatter = InstantPatternFormatter.newBuilder()
                .setPattern(namedPattern.getLegacyPattern())
                .setLegacyFormattersEnabled(true)
                .build();
        InstantPatternFormatter formatter = InstantPatternFormatter.newBuilder()
                .setPattern(namedPattern.getPattern())
                .setLegacyFormattersEnabled(false)
                .build();
        Instant javaTimeInstant = Instant.now();
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochSecond(javaTimeInstant.getEpochSecond(), javaTimeInstant.getNano());
        String legacy = legacyFormatter.format(instant);
        String modern = formatter.format(instant);
        assertThat(legacy).isEqualTo(modern);
    }
}
