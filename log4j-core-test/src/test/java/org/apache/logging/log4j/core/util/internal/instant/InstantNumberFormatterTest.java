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

import java.util.Arrays;
import java.util.stream.Stream;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class InstantNumberFormatterTest {

    @ParameterizedTest
    @MethodSource("testCases")
    void should_produce_expected_output(
            final InstantFormatter formatter, final Instant instant, final String expectedOutput) {
        final String actualOutput = formatter.format(instant);
        assertThat(actualOutput).isEqualTo(expectedOutput);
    }

    static Stream<Object[]> testCases() {
        return Stream.concat(
                testCases(1581082727, 982123456, new Object[][] {
                    {InstantNumberFormatter.EPOCH_SECONDS, "1581082727.982123456"},
                    {InstantNumberFormatter.EPOCH_SECONDS_ROUNDED, "1581082727"},
                    {InstantNumberFormatter.EPOCH_SECONDS_NANOS, "982123456"},
                    {InstantNumberFormatter.EPOCH_MILLIS, "1581082727982.123456"},
                    {InstantNumberFormatter.EPOCH_MILLIS_ROUNDED, "1581082727982"},
                    {InstantNumberFormatter.EPOCH_MILLIS_NANOS, "123456"},
                    {InstantNumberFormatter.EPOCH_NANOS, "1581082727982123456"}
                }),
                testCases(1591177590, 5000001, new Object[][] {
                    {InstantNumberFormatter.EPOCH_SECONDS, "1591177590.005000001"},
                    {InstantNumberFormatter.EPOCH_SECONDS_ROUNDED, "1591177590"},
                    {InstantNumberFormatter.EPOCH_SECONDS_NANOS, "5000001"},
                    {InstantNumberFormatter.EPOCH_MILLIS, "1591177590005.000001"},
                    {InstantNumberFormatter.EPOCH_MILLIS_ROUNDED, "1591177590005"},
                    {InstantNumberFormatter.EPOCH_MILLIS_NANOS, "1"},
                    {InstantNumberFormatter.EPOCH_NANOS, "1591177590005000001"}
                }));
    }

    private static Stream<Object[]> testCases(
            long epochSeconds, int epochSecondsNanos, Object[][] formatterAndOutputPairs) {
        return Arrays.stream(formatterAndOutputPairs).map(formatterAndOutputPair -> {
            final InstantFormatter formatter = (InstantFormatter) formatterAndOutputPair[0];
            final String expectedOutput = (String) formatterAndOutputPair[1];
            final MutableInstant instant = new MutableInstant();
            instant.initFromEpochSecond(epochSeconds, epochSecondsNanos);
            return new Object[] {formatter, instant, expectedOutput};
        });
    }
}
