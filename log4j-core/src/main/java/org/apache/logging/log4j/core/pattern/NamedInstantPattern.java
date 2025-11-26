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

import org.jspecify.annotations.NullMarked;

/**
 * Represents named date &amp; time patterns for formatting log timestamps.
 *
 * @see DatePatternConverter
 * @since 2.26.0
 */
@NullMarked
public enum NamedInstantPattern {
    ABSOLUTE("HH:mm:ss,SSS"),

    ABSOLUTE_MICROS("HH:mm:ss,SSSSSS", "HH:mm:ss,nnnnnn"),

    ABSOLUTE_NANOS("HH:mm:ss,SSSSSSSSS", "HH:mm:ss,nnnnnnnnn"),

    ABSOLUTE_PERIOD("HH:mm:ss.SSS"),

    COMPACT("yyyyMMddHHmmssSSS"),

    DATE("dd MMM yyyy HH:mm:ss,SSS"),

    DATE_PERIOD("dd MMM yyyy HH:mm:ss.SSS"),

    DEFAULT("yyyy-MM-dd HH:mm:ss,SSS"),

    DEFAULT_MICROS("yyyy-MM-dd HH:mm:ss,SSSSSS", "yyyy-MM-dd HH:mm:ss,nnnnnn"),

    DEFAULT_NANOS("yyyy-MM-dd HH:mm:ss,SSSSSSSSS", "yyyy-MM-dd HH:mm:ss,nnnnnnnnn"),

    DEFAULT_PERIOD("yyyy-MM-dd HH:mm:ss.SSS"),

    ISO8601_BASIC("yyyyMMdd'T'HHmmss,SSS"),

    ISO8601_BASIC_PERIOD("yyyyMMdd'T'HHmmss.SSS"),

    ISO8601("yyyy-MM-dd'T'HH:mm:ss,SSS"),

    ISO8601_OFFSET_DATE_TIME_HH("yyyy-MM-dd'T'HH:mm:ss,SSSx", "yyyy-MM-dd'T'HH:mm:ss,SSSX"),

    ISO8601_OFFSET_DATE_TIME_HHMM("yyyy-MM-dd'T'HH:mm:ss,SSSxx", "yyyy-MM-dd'T'HH:mm:ss,SSSXX"),

    ISO8601_OFFSET_DATE_TIME_HHCMM("yyyy-MM-dd'T'HH:mm:ss,SSSxxx", "yyyy-MM-dd'T'HH:mm:ss,SSSXXX"),

    ISO8601_PERIOD("yyyy-MM-dd'T'HH:mm:ss.SSS"),

    ISO8601_PERIOD_MICROS("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", "yyyy-MM-dd'T'HH:mm:ss.nnnnnn"),

    US_MONTH_DAY_YEAR2_TIME("dd/MM/yy HH:mm:ss.SSS"),

    US_MONTH_DAY_YEAR4_TIME("dd/MM/yyyy HH:mm:ss.SSS");

    private final String pattern;
    private final String legacyPattern;

    NamedInstantPattern(String pattern) {
        this(pattern, pattern);
    }

    NamedInstantPattern(String pattern, String legacyPattern) {
        this.pattern = pattern;
        this.legacyPattern = legacyPattern;
    }

    /**
     * Returns the date-time pattern string compatible with {@link java.time.format.DateTimeFormatter}
     * that is associated with this named pattern.
     *
     * @return the date-time pattern string for use with {@code DateTimeFormatter}
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * Returns the legacy {@link org.apache.logging.log4j.core.util.datetime.FixedDateFormat} pattern
     * associated with this named pattern.
     * <p>
     * If legacy formatters are enabled, output is produced for
     * {@code FixedDateFormat} and {@code FastDateFormat}. To convert the {@code DateTimeFormatter}
     * to its legacy counterpart, the following transformations need to be applied:
     * </p>
     * <table>
     *   <caption>Pattern Differences</caption>
     *   <thead>
     *     <tr>
     *       <th></th>
     *       <th>Microseconds</th>
     *       <th>Nanoseconds</th>
     *       <th>Time-zone</th>
     *     </tr>
     *   </thead>
     *   <tbody>
     *     <tr>
     *       <td>Legacy formatter directive</td>
     *       <td><code>nnnnnn</code></td>
     *       <td><code>nnnnnnnnn</code></td>
     *       <td><code>X</code>, <code>XX</code>, <code>XXX</code></td>
     *     </tr>
     *     <tr>
     *       <td>{@code DateTimeFormatter} directive</td>
     *       <td><code>SSSSSS</code></td>
     *       <td><code>SSSSSSSSS</code></td>
     *       <td><code>x</code>, <code>xx</code>, <code>xxx</code></td>
     *     </tr>
     *   </tbody>
     * </table>
     * <h4>Rationale</h4>
     * <ul>
     *   <li>
     *     <p>
     *       Legacy formatters are largely compatible with the {@code SimpleDateFormat} specification,
     *       but introduce a custom {@code n} pattern letter, unique to Log4j, to represent sub-millisecond precision.
     *       This {@code n} is not part of the standard {@code SimpleDateFormat}.
     *     </p>
     *     <p>
     *       In legacy formatters, repeating {@code n} increases the precision, similar to how repeated {@code S}
     *       is used for fractional seconds in {@code DateTimeFormatter}.
     *     </p>
     *     <p>
     *       In contrast, {@code DateTimeFormatter} interprets {@code n} as nano-of-second.
     *       In Java 17, both {@code n} and {@code N} always output nanosecond precision,
     *       regardless of the number of pattern letters.
     *     </p>
     *   </li>
     *   <li>
     *     <p>
     *       Legacy formatters use <code>X</code>, <code>XX</code>, and <code>XXX</code> to format time zones as
     *       <code>+00</code>, <code>+0000</code>, or <code>+00:00</code>, following {@code SimpleDateFormat} conventions.
     *       In contrast, {@code DateTimeFormatter} outputs <code>Z</code> for zero-offset when using <code>X</code>.
     *       To ensure numeric output for zero-offset (e.g., <code>+00</code>),
     *       we use <code>x</code>, <code>xx</code>, or <code>xxx</code> instead.
     *     </p>
     *   </li>
     * </ul>
     *
     * @return the legacy pattern string as used in
     * {@link org.apache.logging.log4j.core.util.datetime.FixedDateFormat.FixedFormat}
     */
    String getLegacyPattern() {
        return legacyPattern;
    }
}
