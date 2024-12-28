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

import static java.util.Arrays.asList;
import static org.apache.logging.log4j.core.util.internal.instant.InstantPatternDynamicFormatter.sequencePattern;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.core.util.internal.instant.InstantPatternDynamicFormatter.DateTimeFormatterPatternFormatterFactory;
import org.apache.logging.log4j.core.util.internal.instant.InstantPatternDynamicFormatter.PatternFormatterFactory;
import org.apache.logging.log4j.core.util.internal.instant.InstantPatternDynamicFormatter.SecondPatternFormatterFactory;
import org.apache.logging.log4j.util.Constants;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class InstantPatternDynamicFormatterTest {

    @ParameterizedTest
    @MethodSource("sequencingTestCases")
    void sequencing_should_work(
            final String pattern,
            final ChronoUnit thresholdPrecision,
            final List<PatternFormatterFactory> expectedSequences) {
        final List<PatternFormatterFactory> actualSequences = sequencePattern(pattern, thresholdPrecision);
        assertThat(actualSequences).isEqualTo(expectedSequences);
    }

    static List<Arguments> sequencingTestCases() {
        final List<Arguments> testCases = new ArrayList<>();

        // `SSSX` should be treated constant for daily updates
        testCases.add(Arguments.of("SSSX", ChronoUnit.DAYS, asList(pMilliSec(), pDyn("X"))));

        // `yyyyMMddHHmmssSSSX` instant cache updated hourly
        testCases.add(Arguments.of(
                "yyyyMMddHHmmssSSSX",
                ChronoUnit.HOURS,
                asList(pDyn("yyyyMMddHH", ChronoUnit.HOURS), pDyn("mm"), pSec("", 3), pDyn("X"))));

        // `yyyyMMddHHmmssSSSX` instant cache updated per minute
        testCases.add(Arguments.of(
                "yyyyMMddHHmmssSSSX",
                ChronoUnit.MINUTES,
                asList(pDyn("yyyyMMddHHmm", ChronoUnit.MINUTES), pSec("", 3), pDyn("X"))));

        // ISO9601 instant cache updated daily
        final String iso8601InstantPattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
        testCases.add(Arguments.of(
                iso8601InstantPattern,
                ChronoUnit.DAYS,
                asList(
                        pDyn("yyyy'-'MM'-'dd'T'", ChronoUnit.DAYS),
                        pDyn("HH':'mm':'", ChronoUnit.MINUTES),
                        pSec(".", 3),
                        pDyn("X"))));

        // ISO9601 instant cache updated per minute
        testCases.add(Arguments.of(
                iso8601InstantPattern,
                ChronoUnit.MINUTES,
                asList(pDyn("yyyy'-'MM'-'dd'T'HH':'mm':'", ChronoUnit.MINUTES), pSec(".", 3), pDyn("X"))));

        // ISO9601 instant cache updated per second
        testCases.add(Arguments.of(
                iso8601InstantPattern,
                ChronoUnit.SECONDS,
                asList(pDyn("yyyy'-'MM'-'dd'T'HH':'mm':'", ChronoUnit.MINUTES), pSec(".", 3), pDyn("X"))));

        // Seconds and micros
        testCases.add(Arguments.of(
                "HH:mm:ss.SSSSSS", ChronoUnit.MINUTES, asList(pDyn("HH':'mm':'", ChronoUnit.MINUTES), pSec(".", 6))));

        return testCases;
    }

    private static DateTimeFormatterPatternFormatterFactory pDyn(final String pattern) {
        return new DateTimeFormatterPatternFormatterFactory(pattern);
    }

    private static DateTimeFormatterPatternFormatterFactory pDyn(final String pattern, final ChronoUnit precision) {
        return new DateTimeFormatterPatternFormatterFactory(pattern, precision);
    }

    private static SecondPatternFormatterFactory pSec(String separator, int fractionalDigits) {
        return new SecondPatternFormatterFactory(true, separator, fractionalDigits);
    }

    private static SecondPatternFormatterFactory pMilliSec() {
        return new SecondPatternFormatterFactory(false, "", 3);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                // Basics
                "SSSSSSS",
                "SSSSSSSSS",
                "n",
                "nn",
                "N",
                "NN",
                // Mixed with other stuff
                "yyyy-MM-dd HH:mm:ss,SSSSSSS",
                "yyyy-MM-dd HH:mm:ss,SSSSSSSS",
                "yyyy-MM-dd HH:mm:ss,SSSSSSSSS",
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS"
            })
    void should_recognize_patterns_of_nano_precision(final String pattern) {
        assertPatternPrecision(pattern, ChronoUnit.NANOS);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                // Basics
                "SSSS",
                "SSSSS",
                "SSSSSS",
                // Mixed with other stuff
                "yyyy-MM-dd HH:mm:ss,SSSS",
                "yyyy-MM-dd HH:mm:ss,SSSSS",
                "yyyy-MM-dd HH:mm:ss,SSSSSS",
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
                // Single-quoted text containing nanosecond directives
                "yyyy-MM-dd'S'HH:mm:ss.SSSSSSXXX",
                "yyyy-MM-dd'n'HH:mm:ss.SSSSSSXXX",
                "yyyy-MM-dd'N'HH:mm:ss.SSSSSSXXX",
            })
    void should_recognize_patterns_of_micro_precision(final String pattern) {
        assertPatternPrecision(pattern, ChronoUnit.MICROS);
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
                "yyyy-MM-dd HH:mm:s",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "HH:mm:s",
                // Single-quoted text containing nanosecond and millisecond directives
                "yyyy-MM-dd'S'HH:mm:ss",
                "yyyy-MM-dd'n'HH:mm:ss",
                "yyyy-MM-dd'N'HH:mm:ss",
                "yyyy-MM-dd'A'HH:mm:ss"
            })
    void should_recognize_patterns_of_second_precision(final String pattern) {
        assertPatternPrecision(pattern, ChronoUnit.SECONDS);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                // Basics
                "m",
                "mm",
                // Mixed with other stuff
                "yyyy-MM-dd HH:mm",
                "yyyy-MM-dd'T'HH:mm",
                "HH:mm",
                // Single-quoted text containing nanosecond and millisecond directives
                "yyyy-MM-dd'S'HH:mm",
                "yyyy-MM-dd'n'HH:mm"
            })
    void should_recognize_patterns_of_minute_precision(final String pattern) {
        assertPatternPrecision(pattern, ChronoUnit.MINUTES);
    }

    @ParameterizedTest
    @MethodSource("hourPrecisionPatterns")
    void should_recognize_patterns_of_hour_precision(final String pattern) {
        assertPatternPrecision(pattern, ChronoUnit.HOURS);
    }

    static List<String> hourPrecisionPatterns() {
        final List<String> java8Patterns = new ArrayList<>(asList(
                // Basics
                "H",
                "HH",
                "a",
                "h",
                "K",
                "k",
                "H",
                "Z",
                "x",
                "X",
                "O",
                "z",
                "VV",
                // Mixed with other stuff
                "yyyy-MM-dd HH",
                "yyyy-MM-dd'T'HH",
                "yyyy-MM-dd HH x",
                "yyyy-MM-dd'T'HH XX",
                "ddHH",
                // Single-quoted text containing nanosecond and millisecond directives
                "yyyy-MM-dd'S'HH",
                "yyyy-MM-dd'n'HH"));
        if (Constants.JAVA_MAJOR_VERSION > 8) {
            java8Patterns.add("B");
            java8Patterns.add("v");
        }
        return java8Patterns;
    }

    private static void assertPatternPrecision(final String pattern, final ChronoUnit expectedPrecision) {
        final InstantPatternFormatter formatter =
                new InstantPatternDynamicFormatter(pattern, Locale.getDefault(), TimeZone.getDefault());
        assertThat(formatter.getPrecision()).as("pattern=`%s`", pattern).isEqualTo(expectedPrecision);
    }

    @ParameterizedTest
    @MethodSource("formatterInputs")
    void output_should_match_DateTimeFormatter(
            final String pattern, final Locale locale, final TimeZone timeZone, final MutableInstant instant) {
        final String log4jOutput = formatInstant(pattern, locale, timeZone, instant);
        final String javaOutput = DateTimeFormatter.ofPattern(pattern, locale)
                .withZone(timeZone.toZoneId())
                .format(instant);
        assertThat(log4jOutput).isEqualTo(javaOutput);
    }

    static Stream<Arguments> formatterInputs() {
        return Stream.of(
                        // Complete list of `FixedDateFormat`-supported patterns in version `2.24.1`
                        "HH:mm:ss,SSS",
                        "HH:mm:ss,SSSSSS",
                        "HH:mm:ss,SSSSSSSSS",
                        "HH:mm:ss.SSS",
                        "yyyyMMddHHmmssSSS",
                        "dd MMM yyyy HH:mm:ss,SSS",
                        "dd MMM yyyy HH:mm:ss.SSS",
                        "yyyy-MM-dd HH:mm:ss,SSS",
                        "yyyy-MM-dd HH:mm:ss,SSSSSS",
                        "yyyy-MM-dd HH:mm:ss,SSSSSSSSS",
                        "yyyy-MM-dd HH:mm:ss.SSS",
                        "yyyyMMdd'T'HHmmss,SSS",
                        "yyyyMMdd'T'HHmmss.SSS",
                        "yyyy-MM-dd'T'HH:mm:ss,SSS",
                        "yyyy-MM-dd'T'HH:mm:ss,SSSx",
                        "yyyy-MM-dd'T'HH:mm:ss,SSSxx",
                        "yyyy-MM-dd'T'HH:mm:ss,SSSxxx",
                        "yyyy-MM-dd'T'HH:mm:ss.SSS",
                        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
                        "dd/MM/yy HH:mm:ss.SSS",
                        "dd/MM/yyyy HH:mm:ss.SSS")
                .flatMap(InstantPatternDynamicFormatterTest::formatterInputs);
    }

    private static final Random RANDOM = new Random(0);

    private static final Locale[] LOCALES = Locale.getAvailableLocales();

    private static final TimeZone[] TIME_ZONES =
            Arrays.stream(TimeZone.getAvailableIDs()).map(TimeZone::getTimeZone).toArray(TimeZone[]::new);

    static Stream<Arguments> formatterInputs(final String pattern) {
        return IntStream.range(0, 500).mapToObj(ignoredIndex -> {
            final Locale locale = LOCALES[RANDOM.nextInt(LOCALES.length)];
            final TimeZone timeZone = TIME_ZONES[RANDOM.nextInt(TIME_ZONES.length)];
            final MutableInstant instant = randomInstant();
            return Arguments.of(pattern, locale, timeZone, instant);
        });
    }

    private static MutableInstant randomInstant() {
        final MutableInstant instant = new MutableInstant();
        // In the 1970's some time zones had sub-minute offsets to UTC, e.g., Africa/Monrovia.
        // We will exclude them for tests:
        final long minEpochSecond = 315_532_800; // 1980-01-01 01:00:00
        final long maxEpochSecond = 1_621_280_470; // 2021-05-17 21:41:10
        final long epochSecond = RANDOM.nextLong(minEpochSecond, maxEpochSecond);
        final int epochSecondNano = randomNanos();
        instant.initFromEpochSecond(epochSecond, epochSecondNano);
        return instant;
    }

    private static int randomNanos() {
        int total = 0;
        for (int digitIndex = 0; digitIndex < 9; digitIndex++) {
            int number;
            do {
                number = RANDOM.nextInt(10);
            } while (digitIndex == 0 && number == 0);
            total = total * 10 + number;
        }
        return total;
    }

    private static String formatInstant(
            final String pattern, final Locale locale, final TimeZone timeZone, final MutableInstant instant) {
        final InstantPatternFormatter formatter = new InstantPatternDynamicFormatter(pattern, locale, timeZone);
        final StringBuilder buffer = new StringBuilder();
        formatter.formatTo(buffer, instant);
        return buffer.toString();
    }

    @ParameterizedTest
    @MethodSource("formatterInputs")
    void verify_manually_computed_sub_minute_precision_values(
            final String ignoredPattern,
            final Locale ignoredLocale,
            final TimeZone timeZone,
            final MutableInstant instant) {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                        "HH:mm:ss.S-SS-SSS-SSSS-SSSSS-SSSSSS-SSSSSSS-SSSSSSSS-SSSSSSSSS|n")
                .withZone(timeZone.toZoneId());
        final String formatterOutput = formatter.format(instant);
        final int offsetMillis = timeZone.getOffset(instant.getEpochMillisecond());
        final long adjustedEpochSeconds = (instant.getEpochMillisecond() + offsetMillis) / 1000;
        // 86400 seconds per day, 3600 seconds per hour
        final int local_H = (int) ((adjustedEpochSeconds % 86400L) / 3600L);
        final int local_m = (int) ((adjustedEpochSeconds / 60) % 60);
        final int local_s = (int) (adjustedEpochSeconds % 60);
        final int local_S = instant.getNanoOfSecond() / 100000000;
        final int local_SS = instant.getNanoOfSecond() / 10000000;
        final int local_SSS = instant.getNanoOfSecond() / 1000000;
        final int local_SSSS = instant.getNanoOfSecond() / 100000;
        final int local_SSSSS = instant.getNanoOfSecond() / 10000;
        final int local_SSSSSS = instant.getNanoOfSecond() / 1000;
        final int local_SSSSSSS = instant.getNanoOfSecond() / 100;
        final int local_SSSSSSSS = instant.getNanoOfSecond() / 10;
        final int local_SSSSSSSSS = instant.getNanoOfSecond();
        final int local_n = instant.getNanoOfSecond();
        final String output = String.format(
                "%02d:%02d:%02d.%d-%d-%d-%d-%d-%d-%d-%d-%d|%d",
                local_H,
                local_m,
                local_s,
                local_S,
                local_SS,
                local_SSS,
                local_SSSS,
                local_SSSSS,
                local_SSSSSS,
                local_SSSSSSS,
                local_SSSSSSSS,
                local_SSSSSSSSS,
                local_n);
        assertThat(output).isEqualTo(formatterOutput);
    }
}
