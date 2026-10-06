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
package org.apache.logging.log4j.layout.template.json.resolver;

import static org.apache.logging.log4j.layout.template.json.TestHelpers.CONFIGURATION;
import static org.apache.logging.log4j.layout.template.json.TestHelpers.asMap;
import static org.apache.logging.log4j.layout.template.json.TestHelpers.usingSerializedLogEventAccessor;
import static org.apache.logging.log4j.layout.template.json.TestHelpers.writeJson;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout;
import org.junit.jupiter.api.Test;

class TraceResolverTest {

    @Test
    void should_resolve_trace_metadata() {

        // Create the event template using JTL TestHelpers
        final String eventTemplate = writeJson(asMap(
                "traceId", asMap("$resolver", "traceId"),
                "spanId", asMap("$resolver", "spanId"),
                "traceFlags", asMap("$resolver", "traceFlags")));

        // Create the layout
        final JsonTemplateLayout layout = JsonTemplateLayout.newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate(eventTemplate)
                .build();

        // Create the log event with active tracing data
        final LogEvent logEvent = Log4jLogEvent.newBuilder()
                .setTraceId("4bf92f3577b34da6a3ce929d0e0e4736")
                .setSpanId("00f067aa0ba902b7")
                .setTraceFlags("01")
                .build();

        // Check the serialized event parses the JSON correctly
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            assertThat(accessor.getString("traceId")).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
            assertThat(accessor.getString("spanId")).isEqualTo("00f067aa0ba902b7");
            assertThat(accessor.getString("traceFlags")).isEqualTo("01");
        });
    }

    @Test
    void should_resolve_empty_metadata_safely() {

        // Create the event template
        final String eventTemplate = writeJson(asMap(
                "traceId", asMap("$resolver", "traceId"),
                "spanId", asMap("$resolver", "spanId"),
                "traceFlags", asMap("$resolver", "traceFlags")));

        // Create the layout
        final JsonTemplateLayout layout = JsonTemplateLayout.newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate(eventTemplate)
                .build();

        // Create the log event with NO tracing data (simulating default behavior)
        final LogEvent logEvent = Log4jLogEvent.newBuilder().build();

        // Check the serialized event does not throw exceptions and handles missing fields safely
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            assertThat(accessor.getString("traceId")).isNullOrEmpty();
            assertThat(accessor.getString("spanId")).isNullOrEmpty();
            assertThat(accessor.getString("traceFlags")).isNullOrEmpty();
        });
    }
}
