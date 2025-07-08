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

import static org.apache.logging.log4j.core.util.internal.instant.InstantPatternFormatter.LEGACY_FORMATTERS_ENABLED;

import org.apache.logging.log4j.core.util.internal.instant.InstantPatternFormatter;

/**
 * Represents named date & time patterns for formatting log timestamps.
 *
 * @see InstantPatternFormatter#LEGACY_FORMATTERS_ENABLED
 * @see DatePatternConverter
 * @since 2.26.0
 */
public enum NamedDatePattern {

    // If legacy formatters are enabled, we need to produce output aimed for `FixedDateFormat` and `FastDateFormat`.
    // Otherwise, we need to produce output aimed for `DateTimeFormatter`.
    // In conclusion, we need to check if legacy formatters enabled and apply following transformations.
    //
    //                               | Microseconds | Nanoseconds | Time-zone
    // ------------------------------+--------------+-------------+-----------
    // Legacy formatter directive    | nnnnnn       | nnnnnnnnn   | X, XX, XXX
    // `DateTimeFormatter` directive | SSSSSS       | SSSSSSSSS   | x, xx, xxx
    //
    // Enabling legacy formatters mean that user requests the pattern to be formatted using deprecated
    // `FixedDateFormat` and `FastDateFormat`.
    // These two have, let's not say _bogus_, but an _interesting_ way of handling certain pattern directives:
    //
    // - They say they adhere to `SimpleDateFormat` specification, but use `n` directive.
    //   `n` is neither defined by `SimpleDateFormat`, nor `SimpleDateFormat` supports sub-millisecond precisions.
    //   `n` is probably manually introduced by Log4j to support sub-millisecond precisions.
    //
    // - `n` denotes nano-of-second for `DateTimeFormatter`.
    //   In Java 17, `n` and `N` (nano-of-day) always output nanosecond precision.
    //   This is independent of how many times they occur consequently.
    //   Yet legacy formatters use repeated `n` to denote sub-milliseconds precision of certain length.
    //   This doesn't work for `DateTimeFormatter`, which needs
    //
    //   - `SSSSSS` for 6-digit microsecond precision
    //   - `SSSSSSSSS` for 9-digit nanosecond precision
    //
    // - Legacy formatters use `X`, `XX,` and `XXX` to choose between `+00`, `+0000`, or `+00:00`.
    //   This is the correct behaviour for `SimpleDateFormat`.
    //   Though `X` in `DateTimeFormatter` produces `Z` for zero-offset.
    //   To avoid the `Z` output, one needs to use `x` with `DateTimeFormatter`.

    ABSOLUTE("HH:mm:ss,SSS"),

    ABSOLUTE_MICROS("HH:mm:ss," + (LEGACY_FORMATTERS_ENABLED ? "nnnnnn" : "SSSSSS")),

    ABSOLUTE_NANOS("HH:mm:ss," + (LEGACY_FORMATTERS_ENABLED ? "nnnnnnnnn" : "SSSSSSSSS")),

    ABSOLUTE_PERIOD("HH:mm:ss.SSS"),

    COMPACT("yyyyMMddHHmmssSSS"),

    DATE("dd MMM yyyy HH:mm:ss,SSS"),

    DATE_PERIOD("dd MMM yyyy HH:mm:ss.SSS"),

    DEFAULT("yyyy-MM-dd HH:mm:ss,SSS"),

    DEFAULT_MICROS("yyyy-MM-dd HH:mm:ss," + (LEGACY_FORMATTERS_ENABLED ? "nnnnnn" : "SSSSSS")),

    DEFAULT_NANOS("yyyy-MM-dd HH:mm:ss," + (LEGACY_FORMATTERS_ENABLED ? "nnnnnnnnn" : "SSSSSSSSS")),

    DEFAULT_PERIOD("yyyy-MM-dd HH:mm:ss.SSS"),

    ISO8601_BASIC("yyyyMMdd'T'HHmmss,SSS"),

    ISO8601_BASIC_PERIOD("yyyyMMdd'T'HHmmss.SSS"),

    ISO8601("yyyy-MM-dd'T'HH:mm:ss,SSS"),

    ISO8601_OFFSET_DATE_TIME_HH("yyyy-MM-dd'T'HH:mm:ss,SSS" + (LEGACY_FORMATTERS_ENABLED ? "X" : "x")),

    ISO8601_OFFSET_DATE_TIME_HHMM("yyyy-MM-dd'T'HH:mm:ss,SSS" + (LEGACY_FORMATTERS_ENABLED ? "XX" : "xx")),

    ISO8601_OFFSET_DATE_TIME_HHCMM("yyyy-MM-dd'T'HH:mm:ss,SSS" + (LEGACY_FORMATTERS_ENABLED ? "XXX" : "xxx")),

    ISO8601_PERIOD("yyyy-MM-dd'T'HH:mm:ss.SSS"),

    ISO8601_PERIOD_MICROS("yyyy-MM-dd'T'HH:mm:ss." + (LEGACY_FORMATTERS_ENABLED ? "nnnnnn" : "SSSSSS")),

    US_MONTH_DAY_YEAR2_TIME("dd/MM/yy HH:mm:ss.SSS"),

    US_MONTH_DAY_YEAR4_TIME("dd/MM/yyyy HH:mm:ss.SSS");

    private final String pattern;

    NamedDatePattern(String pattern) {
        this.pattern = pattern;
    }

    public String getPattern() {
        return pattern;
    }
}
