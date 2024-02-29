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
package org.apache.logging.log4j.status;

import static org.assertj.core.api.Assertions.assertThat;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.org.webcompere.systemstubs.SystemStubs;

class StatusLoggerDateTest {

    @ParameterizedTest
    @CsvSource({"yyyy-MM-dd", "HH:mm:ss", "HH:mm:ss.SSS"})
    void common_date_patterns_should_work(final String instantPattern) {

        // Create a `StatusLogger` configuration
        final Properties statusLoggerConfigProperties = new Properties();
        statusLoggerConfigProperties.put(StatusLogger.STATUS_DATE_FORMAT, instantPattern);
        final ZoneId zoneId = ZoneId.of("UTC");
        statusLoggerConfigProperties.put(StatusLogger.STATUS_DATE_FORMAT_ZONE, zoneId.toString());
        final StatusLogger.Config statusLoggerConfig = new StatusLogger.Config(statusLoggerConfigProperties);

        // Verify the formatter
        final DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(instantPattern).withZone(zoneId);
        verifyFormatter(statusLoggerConfig.instantFormatter, formatter);
    }

    @Test
    void invalid_date_format_should_cause_fallback_to_defaults() throws Exception {
        final String invalidFormat = "l";
        verifyInvalidDateFormatAndZone(invalidFormat, "UTC", "failed reading the instant format", null);
    }

    @Test
    void invalid_date_format_zone_should_cause_fallback_to_defaults() throws Exception {
        final String invalidZone = "XXX";
        final String format = "yyyy";
        verifyInvalidDateFormatAndZone(
                format,
                invalidZone,
                "Failed reading the instant formatting zone ID",
                DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault()));
    }

    private static void verifyInvalidDateFormatAndZone(
            final String format,
            final String zone,
            final String stderrMessage,
            @Nullable final DateTimeFormatter formatter)
            throws Exception {

        // Create a `StatusLogger` configuration using invalid input
        final Properties statusLoggerConfigProperties = new Properties();
        statusLoggerConfigProperties.put(StatusLogger.STATUS_DATE_FORMAT, format);
        statusLoggerConfigProperties.put(StatusLogger.STATUS_DATE_FORMAT_ZONE, zone);
        final StatusLogger.Config[] statusLoggerConfigRef = {null};
        final String stderr = SystemStubs.tapSystemErr(
                () -> statusLoggerConfigRef[0] = new StatusLogger.Config(statusLoggerConfigProperties));
        final StatusLogger.Config statusLoggerConfig = statusLoggerConfigRef[0];

        // Verify the stderr dump
        assertThat(stderr).contains(stderrMessage);

        // Verify the formatter
        verifyFormatter(statusLoggerConfig.instantFormatter, formatter);
    }

    /**
     * {@link DateTimeFormatter} doesn't have an {@link Object#equals(Object)} implementation, hence <a href="https://stackoverflow.com/a/63887712/1278899">this manual <em>behavioral</em> comparison</a>.
     *
     * @param actual the actual formatter
     * @param expected the expected formatter
     */
    private static void verifyFormatter(@Nullable DateTimeFormatter actual, @Nullable DateTimeFormatter expected) {
        if (expected == null) {
            assertThat(actual).isNull();
        } else {
            assertThat(actual).isNotNull();
            final Instant instant = Instant.now();
            assertThat(actual.format(instant)).isEqualTo(expected.format(instant));
        }
    }
}
