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
package org.apache.logging.log4j.layout.template.json;

import static org.apache.logging.log4j.layout.template.json.TestHelpers.serializeUsingLayout;

import co.elastic.logging.log4j2.EcsLayout;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout.EventTemplateAdditionalField;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class EcsLayoutTest {

    private static final Configuration CONFIGURATION = new DefaultConfiguration();

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private static final String SERVICE_NAME = "test";

    private static final String EVENT_DATASET = SERVICE_NAME + ".log";

    private static final JsonTemplateLayout JSON_TEMPLATE_LAYOUT = JsonTemplateLayout.newBuilder()
            .setConfiguration(CONFIGURATION)
            .setCharset(CHARSET)
            .setEventTemplateUri("classpath:EcsLayout.json")
            .setEventTemplateAdditionalFields(new EventTemplateAdditionalField[] {
                EventTemplateAdditionalField.newBuilder()
                        .setKey("service.name")
                        .setValue(SERVICE_NAME)
                        .build(),
                EventTemplateAdditionalField.newBuilder()
                        .setKey("event.dataset")
                        .setValue(EVENT_DATASET)
                        .build()
            })
            .build();

    private static final EcsLayout ECS_LAYOUT = EcsLayout.newBuilder()
            .setConfiguration(CONFIGURATION)
            .setServiceName(SERVICE_NAME)
            .setEventDataset(EVENT_DATASET)
            .build();

    @Test
    void test_EcsLayout_charset() {
        Assertions.assertThat(ECS_LAYOUT.getCharset()).isEqualTo(CHARSET);
    }

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
        final Map<String, Object> ecsLayoutMap = renderUsingEcsLayout(logEvent);
        Assertions.assertThat(jsonTemplateLayoutMap).isEqualTo(ecsLayoutMap);
    }

    private static Map<String, Object> renderUsingJsonTemplateLayout(final LogEvent logEvent) {
        return serializeUsingLayout(logEvent, JSON_TEMPLATE_LAYOUT);
    }

    private static Map<String, Object> renderUsingEcsLayout(final LogEvent logEvent) {
        return serializeUsingLayout(logEvent, ECS_LAYOUT);
    }
}
