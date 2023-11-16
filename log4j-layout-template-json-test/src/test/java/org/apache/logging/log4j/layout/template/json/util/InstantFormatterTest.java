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
package org.apache.logging.log4j.layout.template.json.util;

import java.util.Locale;
import java.util.TimeZone;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;
import org.apache.logging.log4j.core.util.datetime.FixedDateFormat;
import org.apache.logging.log4j.test.ListStatusListener;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class InstantFormatterTest {

    @ParameterizedTest
    @CsvSource({
        "yyyy-MM-dd'T'HH:mm:ss.SSS" + ",FixedDateFormat",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" + ",FastDateFormat",
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'" + ",DateTimeFormatter"
    })
    void all_internal_implementations_should_be_used(final String pattern, final String className) {
        final InstantFormatter formatter =
                InstantFormatter.newBuilder().setPattern(pattern).build();
        Assertions.assertThat(formatter.getInternalImplementationClass())
                .asString()
                .describedAs("pattern=%s", pattern)
                .endsWith("." + className);
    }

    @Test
    void nanoseconds_should_be_formatted() {
        final InstantFormatter formatter = InstantFormatter.newBuilder()
                .setPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'")
                .setTimeZone(TimeZone.getTimeZone("UTC"))
                .build();
        final MutableInstant instant = new MutableInstant();
        instant.initFromEpochSecond(0, 123_456_789);
        Assertions.assertThat(formatter.format(instant)).isEqualTo("1970-01-01T00:00:00.123456789Z");
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/LOG4J2-3614">LOG4J2-3614</a>
     */
    @Test
    void FastDateFormat_failures_should_be_handled() {

        // Define a pattern causing `FastDateFormat` to fail.
        final String pattern = "ss.nnnnnnnnn";
        final TimeZone timeZone = TimeZone.getTimeZone("UTC");
        final Locale locale = Locale.US;

        // Assert that the pattern is not supported by `FixedDateFormat`.
        final FixedDateFormat fixedDateFormat = FixedDateFormat.createIfSupported(pattern, timeZone.getID());
        Assertions.assertThat(fixedDateFormat).isNull();

        // Assert that the pattern indeed causes a `FastDateFormat` failure.
        Assertions.assertThatThrownBy(() -> FastDateFormat.getInstance(pattern, timeZone, locale))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Illegal pattern component: nnnnnnnnn");

        // Assert that `InstantFormatter` falls back to `DateTimeFormatter`.
        final InstantFormatter formatter = InstantFormatter.newBuilder()
                .setPattern(pattern)
                .setTimeZone(timeZone)
                .build();
        Assertions.assertThat(formatter.getInternalImplementationClass())
                .asString()
                .endsWith(".DateTimeFormatter");

        // Assert that formatting works.
        final MutableInstant instant = new MutableInstant();
        instant.initFromEpochSecond(0, 123_456_789);
        Assertions.assertThat(formatter.format(instant)).isEqualTo("00.123456789");
    }

    @Test
    @UsingStatusListener
    void FixedFormatter_large_enough_buffer(ListStatusListener listener) {
        final String pattern = "yyyy-MM-dd'T'HH:mm:ss,SSSXXX";
        final TimeZone timeZone = TimeZone.getTimeZone("America/Chicago");
        final Locale locale = Locale.ENGLISH;
        final InstantFormatter formatter = InstantFormatter.newBuilder()
                .setPattern(pattern)
                .setTimeZone(timeZone)
                .setLocale(locale)
                .build();

        // On this pattern the FixedFormatter used a buffer shorter than necessary,
        // which caused exceptions and warnings.
        Assertions.assertThat(listener.findStatusData(Level.WARN)).hasSize(0);
        Assertions.assertThat(formatter.getInternalImplementationClass())
                .asString()
                .endsWith(".FixedDateFormat");
    }
}
