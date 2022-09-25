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
package org.apache.logging.log4j.layout.template.json.resolver;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout;
import org.apache.logging.log4j.layout.template.json.util.JsonReader;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.apache.logging.log4j.layout.template.json.TestHelpers.*;
import static org.assertj.core.api.Assertions.assertThat;

class CounterResolverTest {

    @Test
    void no_arg_setup_should_start_from_zero() {
        final String eventTemplate = writeJson(asMap("$resolver", "counter"));
        verify(eventTemplate, 0, 1);
    }

    @Test
    void positive_start_should_work() {
        final String eventTemplate = writeJson(asMap(
                "$resolver", "counter",
                "start", 3));
        verify(eventTemplate, 3, 4);
    }

    @Test
    void positive_start_should_work_when_stringified() {
        final String eventTemplate = writeJson(asMap(
                "$resolver", "counter",
                "start", 3,
                "stringified", true));
        verify(eventTemplate, "3", "4");
    }

    @Test
    void negative_start_should_work() {
        final String eventTemplate = writeJson(asMap(
                "$resolver", "counter",
                "start", -3));
        verify(eventTemplate, -3, -2);
    }

    @Test
    void negative_start_should_work_when_stringified() {
        final String eventTemplate = writeJson(asMap(
                "$resolver", "counter",
                "start", -3,
                "stringified", true));
        verify(eventTemplate, "-3", "-2");
    }

    @Test
    void min_long_should_work_when_overflow_enabled() {
        final String eventTemplate = writeJson(asMap(
                "$resolver", "counter",
                "start", Long.MIN_VALUE));
        verify(eventTemplate, Long.MIN_VALUE, Long.MIN_VALUE + 1L);
    }

    @Test
    void min_long_should_work_when_overflow_enabled_and_stringified() {
        final String eventTemplate = writeJson(asMap(
                "$resolver", "counter",
                "start", Long.MIN_VALUE,
                "stringified", true));
        verify(eventTemplate, "" + Long.MIN_VALUE, "" + (Long.MIN_VALUE + 1L));
    }

    @Test
    void max_long_should_work_when_overflowing() {
        final String eventTemplate = writeJson(asMap(
                "$resolver", "counter",
                "start", Long.MAX_VALUE));
        verify(eventTemplate, Long.MAX_VALUE, Long.MIN_VALUE);
    }

    @Test
    void max_long_should_work_when_overflowing_and_stringified() {
        final String eventTemplate = writeJson(asMap(
                "$resolver", "counter",
                "start", Long.MAX_VALUE,
                "stringified", true));
        verify(eventTemplate, "" + Long.MAX_VALUE, "" + Long.MIN_VALUE);
    }

    @Test
    void max_long_should_work_when_not_overflowing() {
        final String eventTemplate = writeJson(asMap(
                "$resolver", "counter",
                "start", Long.MAX_VALUE,
                "overflowing", false));
        verify(
                eventTemplate,
                Long.MAX_VALUE,
                BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE));
    }

    @Test
    void max_long_should_work_when_not_overflowing_and_stringified() {
        final String eventTemplate = writeJson(asMap(
                "$resolver", "counter",
                "start", Long.MAX_VALUE,
                "overflowing", false,
                "stringified", true));
        verify(
                eventTemplate,
                "" + Long.MAX_VALUE,
                "" + BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE));
    }

    private static void verify(
            final String eventTemplate,
            final Object expectedNumber1,
            final Object expectedNumber2) {

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate(eventTemplate)
                .build();

        // Create the log event.
        final LogEvent logEvent = Log4jLogEvent.newBuilder().build();

        // Check the 1st serialized event.
        final String serializedLogEvent1 = layout.toSerializable(logEvent);
        final Object deserializedLogEvent1 = JsonReader.read(serializedLogEvent1);
        assertThat(deserializedLogEvent1).isEqualTo(expectedNumber1);

        // Check the 2nd serialized event.
        final String serializedLogEvent2 = layout.toSerializable(logEvent);
        final Object deserializedLogEvent2 = JsonReader.read(serializedLogEvent2);
        assertThat(deserializedLogEvent2).isEqualTo(expectedNumber2);

    }

}
