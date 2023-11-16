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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.layout.JsonLayout;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class JsonLayoutTest {

    private static final Configuration CONFIGURATION = new DefaultConfiguration();

    private static final JsonTemplateLayout JSON_TEMPLATE_LAYOUT = JsonTemplateLayout.newBuilder()
            .setConfiguration(CONFIGURATION)
            .setEventTemplateUri("classpath:JsonLayout.json")
            .build();

    private static final JsonLayout JSON_LAYOUT = JsonLayout.newBuilder()
            .setConfiguration(CONFIGURATION)
            .setProperties(true)
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
        final Map<String, Object> jsonLayoutMap = renderUsingJsonLayout(logEvent);
        // `JsonLayout` blindly serializes the `Throwable` as a POJO, this is, to say the least, quite wrong, and I
        // ain't going to try to emulate this behaviour in `JsonTemplateLayout`.
        // Hence, discarding the "thrown" field.
        jsonTemplateLayoutMap.remove("thrown");
        jsonLayoutMap.remove("thrown");
        // When the log event doesn't have any MDC, `JsonLayout` still emits an empty `contextMap` field, whereas
        // `JsonTemplateLayout` totally skips it.
        // Removing `contextMap` field to avoid discrepancies when there is no MDC to render.
        if (logEvent.getContextData().isEmpty()) {
            jsonLayoutMap.remove("contextMap");
        }
        Assertions.assertThat(jsonTemplateLayoutMap).isEqualTo(jsonLayoutMap);
    }

    private static Map<String, Object> renderUsingJsonTemplateLayout(final LogEvent logEvent) {
        return serializeUsingLayout(logEvent, JSON_TEMPLATE_LAYOUT);
    }

    private static Map<String, Object> renderUsingJsonLayout(final LogEvent logEvent) {
        return serializeUsingLayout(logEvent, JSON_LAYOUT);
    }
}
