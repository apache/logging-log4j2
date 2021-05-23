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
package org.apache.logging.log4j.layout.template.json;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.layout.GelfLayout;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout.EventTemplateAdditionalField;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.apache.logging.log4j.layout.template.json.TestHelpers.serializeUsingLayout;

class GelfLayoutTest {

    private static final Configuration CONFIGURATION = new DefaultConfiguration();

    private static final String HOST_NAME = "localhost";

    private static final JsonTemplateLayout JSON_TEMPLATE_LAYOUT = JsonTemplateLayout
            .newBuilder()
            .setConfiguration(CONFIGURATION)
            .setEventTemplateUri("classpath:GelfLayout.json")
            .setEventTemplateAdditionalFields(
                    new EventTemplateAdditionalField[]{
                            EventTemplateAdditionalField
                                    .newBuilder()
                                    .setKey("host")
                                    .setValue(HOST_NAME)
                                    .build()
                    })
            .build();

    private static final GelfLayout GELF_LAYOUT = GelfLayout
            .newBuilder()
            .setConfiguration(CONFIGURATION)
            .setHost(HOST_NAME)
            .setCompressionType(GelfLayout.CompressionType.OFF)
            .build();

    @Test
    void test_lite_log_events() {
        final List<LogEvent> logEvents = LogEventFixture.createLiteLogEvents(1_000);
        test(logEvents);
    }

    @Test
    void test_full_log_events() {
        final List<LogEvent> logEvents = LogEventFixture.createFullLogEvents(1_000);
        test(logEvents);
    }

    private static void test(final Collection<LogEvent> logEvents) {
        for (final LogEvent logEvent : logEvents) {
            test(logEvent);
        }
    }

    private static void test(final LogEvent logEvent) {
        final Map<String, Object> jsonTemplateLayoutMap = renderUsingJsonTemplateLayout(logEvent);
        final Map<String, Object> gelfLayoutMap = renderUsingGelfLayout(logEvent);
        verifyTimestamp(logEvent.getInstant(), jsonTemplateLayoutMap, gelfLayoutMap);
        Assertions.assertThat(jsonTemplateLayoutMap).isEqualTo(gelfLayoutMap);
    }

    private static Map<String, Object> renderUsingJsonTemplateLayout(
            final LogEvent logEvent) {
        return serializeUsingLayout(logEvent, JSON_TEMPLATE_LAYOUT);
    }

    private static Map<String, Object> renderUsingGelfLayout(
            final LogEvent logEvent) {
        return serializeUsingLayout(logEvent, GELF_LAYOUT);
    }

    /**
     * Handle timestamps individually to avoid floating-point comparison hiccups.
     */
    private static void verifyTimestamp(
            final Instant logEventInstant,
            final Map<String, Object> jsonTemplateLayoutMap,
            final Map<String, Object> gelfLayoutMap) {
        final BigDecimal jsonTemplateLayoutTimestamp =
                (BigDecimal) jsonTemplateLayoutMap.remove("timestamp");
        final BigDecimal gelfLayoutTimestamp =
                (BigDecimal) gelfLayoutMap.remove("timestamp");
        final String description = String.format(
                "instantEpochSecs=%d.%d, jsonTemplateLayoutTimestamp=%s, gelfLayoutTimestamp=%s",
                logEventInstant.getEpochSecond(),
                logEventInstant.getNanoOfSecond(),
                jsonTemplateLayoutTimestamp,
                gelfLayoutTimestamp);
        Assertions
                .assertThat(jsonTemplateLayoutTimestamp.compareTo(gelfLayoutTimestamp))
                .as(description)
                .isEqualTo(0);
    }

}
