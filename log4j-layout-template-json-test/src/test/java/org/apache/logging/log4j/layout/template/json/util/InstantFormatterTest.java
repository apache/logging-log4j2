/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.layout.template.json.util;

import org.apache.logging.log4j.core.time.MutableInstant;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.TimeZone;

class InstantFormatterTest {

    @ParameterizedTest
    @CsvSource({
            "yyyy-MM-dd'T'HH:mm:ss.SSS"             + ",FixedDateFormat",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"          + ",FastDateFormat",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'"    + ",DateTimeFormatter"
    })
    void all_internal_implementations_should_be_used(
            final String pattern,
            final String className) {
        final InstantFormatter formatter = InstantFormatter
                .newBuilder()
                .setPattern(pattern)
                .build();
        Assertions
                .assertThat(formatter.getInternalImplementationClass())
                .asString()
                .describedAs("pattern=%s", pattern)
                .endsWith("." + className);
    }

    @Test
    void nanoseconds_should_be_formatted() {
        final InstantFormatter formatter = InstantFormatter
                .newBuilder()
                .setPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'")
                .setTimeZone(TimeZone.getTimeZone("UTC"))
                .build();
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochSecond(0, 123_456_789);
        Assertions
                .assertThat(formatter.format(instant))
                .isEqualTo("1970-01-01T00:00:00.123456789Z");
    }

}
