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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.function.Function;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class InstantPatternThreadLocalCachedFormatterTest {

    private static final Locale LOCALE = Locale.getDefault();

    private static final TimeZone TIME_ZONE = TimeZone.getDefault();

    @ParameterizedTest
    @MethodSource("getterTestCases")
    void getters_should_work(
            final Function<InstantPatternFormatter, InstantPatternThreadLocalCachedFormatter> cachedFormatterSupplier,
            final String pattern,
            final Locale locale,
            final TimeZone timeZone) {
        final InstantPatternDynamicFormatter dynamicFormatter =
                new InstantPatternDynamicFormatter(pattern, locale, timeZone);
        final InstantPatternThreadLocalCachedFormatter cachedFormatter =
                cachedFormatterSupplier.apply(dynamicFormatter);
        assertThat(cachedFormatter.getPattern()).isEqualTo(pattern);
        assertThat(cachedFormatter.getLocale()).isEqualTo(locale);
        assertThat(cachedFormatter.getTimeZone()).isEqualTo(timeZone);
    }

    static Object[][] getterTestCases() {

        // Choosing two different locale & time zone pairs to ensure having one that doesn't match the system default
        final Locale locale1 = Locale.forLanguageTag("nl_NL");
        final Locale locale2 = Locale.forLanguageTag("tr_TR");
        final String[] timeZoneIds = TimeZone.getAvailableIDs();
        final int timeZone1IdIndex = new Random(0).nextInt(timeZoneIds.length);
        final int timeZone2IdIndex = (timeZone1IdIndex + 1) % timeZoneIds.length;
        final TimeZone timeZone1 = TimeZone.getTimeZone(timeZoneIds[timeZone1IdIndex]);
        final TimeZone timeZone2 = TimeZone.getTimeZone(timeZoneIds[timeZone2IdIndex]);

        // Create test cases
        return new Object[][] {
            // For `ofMilliPrecision()`
            {
                (Function<InstantPatternFormatter, InstantPatternThreadLocalCachedFormatter>)
                        InstantPatternThreadLocalCachedFormatter::ofMilliPrecision,
                "HH:mm.SSS",
                locale1,
                timeZone1
            },
            {
                (Function<InstantPatternFormatter, InstantPatternThreadLocalCachedFormatter>)
                        InstantPatternThreadLocalCachedFormatter::ofMilliPrecision,
                "HH:mm.SSS",
                locale2,
                timeZone2
            },
            // For `ofSecondPrecision()`
            {
                (Function<InstantPatternFormatter, InstantPatternThreadLocalCachedFormatter>)
                        InstantPatternThreadLocalCachedFormatter::ofSecondPrecision,
                "yyyy",
                locale1,
                timeZone1
            },
            {
                (Function<InstantPatternFormatter, InstantPatternThreadLocalCachedFormatter>)
                        InstantPatternThreadLocalCachedFormatter::ofSecondPrecision,
                "yyyy",
                locale2,
                timeZone2
            }
        };
    }

    @ParameterizedTest
    @ValueSource(strings = {"SSSS", "SSSSS", "SSSSSS", "SSSSSSS", "SSSSSSSS", "SSSSSSSSS", "n", "N"})
    void ofMilliPrecision_should_fail_on_inconsistent_precision(final String subMilliPattern) {
        final InstantPatternDynamicFormatter dynamicFormatter =
                new InstantPatternDynamicFormatter(subMilliPattern, LOCALE, TIME_ZONE);
        assertThatThrownBy(() -> InstantPatternThreadLocalCachedFormatter.ofMilliPrecision(dynamicFormatter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "instant formatter `%s` is of `%s` precision, whereas the requested cache precision is `%s`",
                        dynamicFormatter, dynamicFormatter.getPrecision(), ChronoUnit.MILLIS);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SSS", "s", "ss", "m", "mm", "H", "HH"})
    void ofMilliPrecision_should_truncate_precision_to_milli(final String superMilliPattern) {
        final InstantPatternDynamicFormatter dynamicFormatter =
                new InstantPatternDynamicFormatter(superMilliPattern, LOCALE, TIME_ZONE);
        final InstantPatternThreadLocalCachedFormatter cachedFormatter =
                InstantPatternThreadLocalCachedFormatter.ofMilliPrecision(dynamicFormatter);
        assertThat(cachedFormatter.getPrecision()).isEqualTo(ChronoUnit.MILLIS);
        assertThat(cachedFormatter.getPrecision().compareTo(dynamicFormatter.getPrecision()))
                .isLessThanOrEqualTo(0);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"S", "SS", "SSS", "SSSS", "SSSSS", "SSSSSS", "SSSSSSS", "SSSSSSSS", "SSSSSSSSS", "n", "N", "A"})
    void ofSecondPrecision_should_fail_on_inconsistent_precision(final String subSecondPattern) {
        final InstantPatternDynamicFormatter dynamicFormatter =
                new InstantPatternDynamicFormatter(subSecondPattern, LOCALE, TIME_ZONE);
        assertThatThrownBy(() -> InstantPatternThreadLocalCachedFormatter.ofSecondPrecision(dynamicFormatter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "instant formatter `%s` is of `%s` precision, whereas the requested cache precision is `%s`",
                        dynamicFormatter, dynamicFormatter.getPrecision(), ChronoUnit.SECONDS);
    }

    @ParameterizedTest
    @ValueSource(strings = {"s", "ss", "m", "mm", "H", "HH"})
    void ofSecondPrecision_should_truncate_precision_to_second(final String superSecondPattern) {
        final InstantPatternDynamicFormatter dynamicFormatter =
                new InstantPatternDynamicFormatter(superSecondPattern, LOCALE, TIME_ZONE);
        final InstantPatternThreadLocalCachedFormatter cachedFormatter =
                InstantPatternThreadLocalCachedFormatter.ofSecondPrecision(dynamicFormatter);
        assertThat(cachedFormatter.getPrecision()).isEqualTo(ChronoUnit.SECONDS);
        assertThat(cachedFormatter.getPrecision().compareTo(dynamicFormatter.getPrecision()))
                .isLessThanOrEqualTo(0);
    }

    private static final MutableInstant INSTANT0 = createInstant(0, 0);

    @Test
    void ofMilliPrecision_should_cache() {

        // Mock a pattern formatter
        final InstantPatternFormatter patternFormatter = mock(InstantPatternFormatter.class);
        when(patternFormatter.getPrecision()).thenReturn(ChronoUnit.MILLIS);

        // Configure the pattern formatter for the 1st instant
        final Instant instant1 = INSTANT0;
        final String output1 = "instant1";
        doAnswer(invocation -> {
                    final StringBuilder buffer = invocation.getArgument(0);
                    buffer.append(output1);
                    return null;
                })
                .when(patternFormatter)
                .formatTo(any(StringBuilder.class), eq(instant1));

        // Create a 2nd distinct instant that shares the same milliseconds with the 1st instant.
        // That is, the 2nd instant should trigger a cache hit.
        final MutableInstant instant2 = offsetInstant(instant1, 0, 1);
        assertThat(instant1.getEpochMillisecond()).isEqualTo(instant2.getEpochMillisecond());
        assertThat(instant1).isNotEqualTo(instant2);

        // Configure the pattern for a 3rd distinct instant.
        // The 3rd instant should be of different milliseconds with the 1st (and 2nd) instants to trigger a cache miss.
        final MutableInstant instant3 = offsetInstant(instant2, 1, 0);
        assertThat(instant2.getEpochMillisecond()).isNotEqualTo(instant3.getEpochMillisecond());
        final String output3 = "instant3";
        doAnswer(invocation -> {
                    final StringBuilder buffer = invocation.getArgument(0);
                    buffer.append(output3);
                    return null;
                })
                .when(patternFormatter)
                .formatTo(any(StringBuilder.class), eq(instant3));

        // Create a 4th distinct instant that shares the same milliseconds with the 3rd instant.
        // That is, the 4th instant should trigger a cache hit.
        final MutableInstant instant4 = offsetInstant(instant3, 0, 1);
        assertThat(instant3.getEpochMillisecond()).isEqualTo(instant4.getEpochMillisecond());
        assertThat(instant3).isNotEqualTo(instant4);

        // Create the cached formatter and verify its output
        final InstantFormatter cachedFormatter =
                InstantPatternThreadLocalCachedFormatter.ofMilliPrecision(patternFormatter);
        assertThat(cachedFormatter.format(instant1)).isEqualTo(output1); // Cache miss
        assertThat(cachedFormatter.format(instant2)).isEqualTo(output1); // Cache hit
        assertThat(cachedFormatter.format(instant2)).isEqualTo(output1); // Repeated cache hit
        assertThat(cachedFormatter.format(instant3)).isEqualTo(output3); // Cache miss
        assertThat(cachedFormatter.format(instant4)).isEqualTo(output3); // Cache hit
        assertThat(cachedFormatter.format(instant4)).isEqualTo(output3); // Repeated cache hit

        // Verify the pattern formatter interaction
        verify(patternFormatter).getPrecision();
        verify(patternFormatter).formatTo(any(StringBuilder.class), eq(instant1));
        verify(patternFormatter).formatTo(any(StringBuilder.class), eq(instant3));
        verifyNoMoreInteractions(patternFormatter);
    }

    @Test
    void ofSecondPrecision_should_cache() {

        // Mock a pattern formatter
        final InstantPatternFormatter patternFormatter = mock(InstantPatternFormatter.class);
        when(patternFormatter.getPrecision()).thenReturn(ChronoUnit.SECONDS);

        // Configure the pattern formatter for the 1st instant
        final Instant instant1 = INSTANT0;
        final String output1 = "instant1";
        doAnswer(invocation -> {
                    final StringBuilder buffer = invocation.getArgument(0);
                    buffer.append(output1);
                    return null;
                })
                .when(patternFormatter)
                .formatTo(any(StringBuilder.class), eq(instant1));

        // Create a 2nd distinct instant that shares the same seconds with the 1st instant.
        // That is, the 2nd instant should trigger a cache hit.
        final MutableInstant instant2 = offsetInstant(instant1, 1, 0);
        assertThat(instant1.getEpochSecond()).isEqualTo(instant2.getEpochSecond());
        assertThat(instant1).isNotEqualTo(instant2);

        // Configure the pattern for a 3rd distinct instant.
        // The 3rd instant should be of different seconds with the 1st (and 2nd) instants to trigger a cache miss.
        final MutableInstant instant3 = offsetInstant(instant2, 1_000, 0);
        assertThat(instant2.getEpochSecond()).isNotEqualTo(instant3.getEpochSecond());
        final String output3 = "instant3";
        doAnswer(invocation -> {
                    final StringBuilder buffer = invocation.getArgument(0);
                    buffer.append(output3);
                    return null;
                })
                .when(patternFormatter)
                .formatTo(any(StringBuilder.class), eq(instant3));

        // Create a 4th distinct instant that shares the same seconds with the 3rd instant.
        // That is, the 4th instant should trigger a cache hit.
        final MutableInstant instant4 = offsetInstant(instant3, 1, 0);
        assertThat(instant3.getEpochSecond()).isEqualTo(instant4.getEpochSecond());
        assertThat(instant3).isNotEqualTo(instant4);

        // Create the cached formatter and verify its output
        final InstantFormatter cachedFormatter =
                InstantPatternThreadLocalCachedFormatter.ofSecondPrecision(patternFormatter);
        assertThat(cachedFormatter.format(instant1)).isEqualTo(output1); // Cache miss
        assertThat(cachedFormatter.format(instant2)).isEqualTo(output1); // Cache hit
        assertThat(cachedFormatter.format(instant2)).isEqualTo(output1); // Repeated cache hit
        assertThat(cachedFormatter.format(instant3)).isEqualTo(output3); // Cache miss
        assertThat(cachedFormatter.format(instant4)).isEqualTo(output3); // Cache hit
        assertThat(cachedFormatter.format(instant4)).isEqualTo(output3); // Repeated cache hit

        // Verify the pattern formatter interaction
        verify(patternFormatter).getPrecision();
        verify(patternFormatter).formatTo(any(StringBuilder.class), eq(instant1));
        verify(patternFormatter).formatTo(any(StringBuilder.class), eq(instant3));
        verifyNoMoreInteractions(patternFormatter);
    }

    private static MutableInstant offsetInstant(
            final Instant instant, final long epochMillisOffset, final int epochMillisNanosOffset) {
        final long epochMillis = Math.addExact(instant.getEpochMillisecond(), epochMillisOffset);
        final int epochMillisNanos = Math.addExact(instant.getNanoOfMillisecond(), epochMillisNanosOffset);
        return createInstant(epochMillis, epochMillisNanos);
    }

    private static MutableInstant createInstant(final long epochMillis, final int epochMillisNanos) {
        final MutableInstant instant = new MutableInstant();
        instant.initFromEpochMilli(epochMillis, epochMillisNanos);
        return instant;
    }
}
