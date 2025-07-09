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

/**
 * Represents named date &amp; time patterns for formatting log timestamps.
 *
 * @see DatePatternConverter
 * @since 2.26.0
 */
public enum NamedInstantPattern {
    ABSOLUTE("HH:mm:ss,SSS"),

    ABSOLUTE_MICROS("HH:mm:ss,SSSSSS"),

    ABSOLUTE_NANOS("HH:mm:ss,SSSSSSSSS"),

    ABSOLUTE_PERIOD("HH:mm:ss.SSS"),

    COMPACT("yyyyMMddHHmmssSSS"),

    DATE("dd MMM yyyy HH:mm:ss,SSS"),

    DATE_PERIOD("dd MMM yyyy HH:mm:ss.SSS"),

    DEFAULT("yyyy-MM-dd HH:mm:ss,SSS"),

    DEFAULT_MICROS("yyyy-MM-dd HH:mm:ss,SSSSSS"),

    DEFAULT_NANOS("yyyy-MM-dd HH:mm:ss,SSSSSSSSS"),

    DEFAULT_PERIOD("yyyy-MM-dd HH:mm:ss.SSS"),

    ISO8601_BASIC("yyyyMMdd'T'HHmmss,SSS"),

    ISO8601_BASIC_PERIOD("yyyyMMdd'T'HHmmss.SSS"),

    ISO8601("yyyy-MM-dd'T'HH:mm:ss,SSS"),

    ISO8601_OFFSET_DATE_TIME_HH("yyyy-MM-dd'T'HH:mm:ss,SSSx"),

    ISO8601_OFFSET_DATE_TIME_HHMM("yyyy-MM-dd'T'HH:mm:ss,SSSxx"),

    ISO8601_OFFSET_DATE_TIME_HHCMM("yyyy-MM-dd'T'HH:mm:ss,SSSxxx"),

    ISO8601_PERIOD("yyyy-MM-dd'T'HH:mm:ss.SSS"),

    ISO8601_PERIOD_MICROS("yyyy-MM-dd'T'HH:mm:ss.SSSSSS"),

    US_MONTH_DAY_YEAR2_TIME("dd/MM/yy HH:mm:ss.SSS"),

    US_MONTH_DAY_YEAR4_TIME("dd/MM/yyyy HH:mm:ss.SSS");

    private final String pattern;

    NamedInstantPattern(String pattern) {
        this.pattern = pattern;
    }

    /**
     * @return pattern that is compatible with {@link java.time.format.DateTimeFormatter}
     */
    public String getPattern() {
        return pattern;
    }
}
