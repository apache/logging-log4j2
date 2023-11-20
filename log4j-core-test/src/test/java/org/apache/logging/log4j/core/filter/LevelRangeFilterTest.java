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

import static org.apache.logging.log4j.core.filter.LevelRangeFilter.createFilter;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class LevelRangeFilterTest {

    @Test
    void verify_constants() {
        assertThat(LevelRangeFilter.DEFAULT_MIN_LEVEL).isEqualTo(Level.OFF);
        assertThat(LevelRangeFilter.DEFAULT_MAX_LEVEL).isEqualTo(Level.ALL);
        assertThat(LevelRangeFilter.DEFAULT_ON_MATCH).isEqualTo(Result.NEUTRAL);
        assertThat(LevelRangeFilter.DEFAULT_ON_MISMATCH).isEqualTo(Result.DENY);
    }

    @Test
    void verify_defaults() {
        final LevelRangeFilter filter = createFilter(null, null, null, null);
        assertThat(filter.getMinLevel()).isEqualTo(Level.OFF);
        assertThat(filter.getMaxLevel()).isEqualTo(Level.ALL);
        assertThat(filter.getOnMatch()).isEqualTo(Result.NEUTRAL);
        assertThat(filter.getOnMismatch()).isEqualTo(Result.DENY);
    }

    @ParameterizedTest
    @MethodSource("org.apache.logging.log4j.Level#values")
    void default_should_match_all_levels(final Level level) {
        final LevelRangeFilter filter = createFilter(null, null, null, null);
        assertThat(filter.filter(createEvent(level))).isEqualTo(LevelRangeFilter.DEFAULT_ON_MATCH);
    }

    @Test
    void overriding_defaults_should_be_effective() {

        // Choose a configuration
        final Level minLevel = Level.ERROR;
        final Level maxLevel = Level.WARN;
        final Result onMatch = Result.ACCEPT;
        final Result onMismatch = Result.NEUTRAL;

        // Verify we deviate from the defaults
        assertThat(minLevel).isNotEqualTo(LevelRangeFilter.DEFAULT_MIN_LEVEL);
        assertThat(maxLevel).isNotEqualTo(LevelRangeFilter.DEFAULT_MAX_LEVEL);
        assertThat(onMatch).isNotEqualTo(LevelRangeFilter.DEFAULT_ON_MATCH);
        assertThat(onMismatch).isNotEqualTo(LevelRangeFilter.DEFAULT_ON_MISMATCH);

        // Verify the filtering
        final LevelRangeFilter filter = createFilter(minLevel, maxLevel, onMatch, onMismatch);
        final SoftAssertions assertions = new SoftAssertions();
        for (final Level level : Level.values()) {
            final Result expectedResult = level.isInRange(minLevel, maxLevel) ? onMatch : onMismatch;
            assertions.assertThat(filter.filter(createEvent(level))).isEqualTo(expectedResult);
        }
        assertions.assertAll();
    }

    private static LogEvent createEvent(final Level level) {
        final SimpleMessage message = new SimpleMessage("test message at level " + level);
        return Log4jLogEvent.newBuilder().setLevel(level).setMessage(message).build();
    }
}
