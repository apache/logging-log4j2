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
package org.apache.logging.log4j.core.util.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.TimeZone;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;
import org.apache.logging.log4j.core.util.datetime.FixedDateFormat;
import org.apache.logging.log4j.test.ListStatusListener;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class InstantPatternDynamicFormatterTest {

    @ParameterizedTest
    @CsvSource({
        "yyyy-MM-dd'T'HH:mm:ss.SSS" + ",FixedDateFormat",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" + ",FastDateFormat",
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'" + ",DateTimeFormatter"
    })
    void all_internal_implementations_should_be_used(final String pattern, final String className) {
        final InstantPatternDynamicFormatter formatter =
                new InstantPatternDynamicFormatter(pattern, Locale.getDefault(), TimeZone.getDefault());
        assertThat(formatter.getInternalImplementationClass())
                .asString()
                .describedAs("pattern=`%s`", pattern)
                .endsWith("." + className);
    }

    /**
     * Reproduces <a href="https://issues.apache.org/jira/browse/LOG4J2-3075">LOG4J2-3075</a>.
     */
    @Test
    void nanoseconds_should_be_formatted() {
        final InstantFormatter formatter = new InstantPatternDynamicFormatter(
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'", Locale.getDefault(), TimeZone.getTimeZone("UTC"));
        final MutableInstant instant = new MutableInstant();
        instant.initFromEpochSecond(0, 123_456_789);
        assertThat(formatter.format(instant)).isEqualTo("1970-01-01T00:00:00.123456789Z");
    }

    /**
     * Reproduces <a href="https://issues.apache.org/jira/browse/LOG4J2-3614">LOG4J2-3614</a>.
     */
    @Test
    void FastDateFormat_failures_should_be_handled() {

        // Define a pattern causing `FastDateFormat` to fail.
        final String pattern = "ss.nnnnnnnnn";
        final TimeZone timeZone = TimeZone.getTimeZone("UTC");
        final Locale locale = Locale.US;

        // Assert that the pattern is not supported by `FixedDateFormat`.
        final FixedDateFormat fixedDateFormat = FixedDateFormat.createIfSupported(pattern, timeZone.getID());
        assertThat(fixedDateFormat).isNull();

        // Assert that the pattern indeed causes a `FastDateFormat` failure.
        assertThatThrownBy(() -> FastDateFormat.getInstance(pattern, timeZone, locale))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Illegal pattern component: nnnnnnnnn");

        // Assert that `InstantFormatter` falls back to `DateTimeFormatter`.
        final InstantPatternDynamicFormatter formatter =
                new InstantPatternDynamicFormatter(pattern, Locale.getDefault(), timeZone);
        assertThat(formatter.getInternalImplementationClass()).asString().endsWith(".DateTimeFormatter");

        // Assert that formatting works.
        final MutableInstant instant = new MutableInstant();
        instant.initFromEpochSecond(0, 123_456_789);
        assertThat(formatter.format(instant)).isEqualTo("00.123456789");
    }

    /**
     * Reproduces <a href="https://github.com/apache/logging-log4j2/issues/1418">#1418</a>.
     */
    @Test
    @UsingStatusListener
    void FixedFormatter_should_allocate_large_enough_buffer(final ListStatusListener listener) {
        final String pattern = "yyyy-MM-dd'T'HH:mm:ss,SSSXXX";
        final TimeZone timeZone = TimeZone.getTimeZone("America/Chicago");
        final Locale locale = Locale.ENGLISH;
        final InstantPatternDynamicFormatter formatter = new InstantPatternDynamicFormatter(pattern, locale, timeZone);

        // On this pattern the `FixedFormatter` used a buffer shorter than necessary,
        // which caused exceptions and warnings.
        assertThat(listener.findStatusData(Level.WARN)).hasSize(0);
        assertThat(formatter.getInternalImplementationClass()).asString().endsWith(".FixedDateFormat");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                // Basics
                "S",
                "SSSS",
                "SSSSS",
                "SSSSSS",
                "SSSSSSS",
                "SSSSSSSSS",
                "n",
                "nn",
                "N",
                "NN",
                // Mixed with other stuff
                "yyyy-MM-dd HH:mm:ss,SSSSSS",
                "yyyy-MM-dd HH:mm:ss,SSSSSS",
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
                "yyyy-MM-dd'T'HH:mm:ss.SXXX"
            })
    void should_recognize_patterns_of_nano_precision(final String pattern) {
        assertPatternPrecision(pattern, ChronoUnit.NANOS);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                // Basics
                "SS",
                "SSS",
                "A",
                "AA",
                // Mixed with other stuff
                "yyyy-MM-dd HH:mm:ss,SS",
                "yyyy-MM-dd HH:mm:ss,SSS",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                // Single-quoted text containing nanosecond directives
                "yyyy-MM-dd'S'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'n'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'N'HH:mm:ss.SSSXXX",
            })
    void should_recognize_patterns_of_milli_precision(final String pattern) {
        assertPatternPrecision(pattern, ChronoUnit.MILLIS);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                // Basics
                "s",
                "ss",
                // Mixed with other stuff
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "HH:mm",
                "yyyy-MM-dd'T'",
                // Single-quoted text containing nanosecond and millisecond directives
                "yyyy-MM-dd'S'HH:mm:ss",
                "yyyy-MM-dd'n'HH:mm:ss",
                "yyyy-MM-dd'N'HH:mm:ss",
                "yyyy-MM-dd'A'HH:mm:ss"
            })
    void should_recognize_patterns_of_second_precision(final String pattern) {
        assertPatternPrecision(pattern, ChronoUnit.SECONDS);
    }

    private static void assertPatternPrecision(final String pattern, final ChronoUnit expectedPrecision) {
        final ChronoUnit actualPrecision = InstantPatternDynamicFormatter.patternPrecision(pattern);
        assertThat(actualPrecision).as("pattern=`%s`", pattern).isEqualTo(expectedPrecision);
    }
}
