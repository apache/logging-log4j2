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

import static org.apache.logging.log4j.layout.template.json.TestHelpers.*;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout;
import org.junit.jupiter.api.Test;

class MarkerResolverTest {

    @Test
    void should_have_a_marker_name() {

        // Create the event template
        final String eventTemplate = writeJson(asMap(
                "marker",
                asMap(
                        "$resolver", "marker",
                        "field", "name")));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout.newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate(eventTemplate)
                .build();

        // Create the log event.
        final Marker marker = MarkerManager.getMarker("MARKER");
        final LogEvent logEvent = Log4jLogEvent.newBuilder().setMarker(marker).build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> assertThat(accessor.getString("marker"))
                .isEqualTo("MARKER"));
    }

    @Test
    void should_list_parents_as_array() {

        // Create the event template
        final String eventTemplate = writeJson(asMap(
                "parents",
                asMap(
                        "$resolver", "marker",
                        "field", "parents")));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout.newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate(eventTemplate)
                .build();

        // Create the log event.
        final Marker parentMarker1 = MarkerManager.getMarker("PARENT_MARKER_NAME_1");
        final Marker parentMarker2 = MarkerManager.getMarker("PARENT_MARKER_NAME_2");
        final Marker childMarker = MarkerManager.getMarker("CHILD_MARKER_NAME");
        childMarker.setParents(parentMarker1, parentMarker2);

        final LogEvent logEvent =
                Log4jLogEvent.newBuilder().setMarker(childMarker).build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(
                layout, logEvent, accessor -> assertThat(accessor.getList("parents", String.class))
                        .containsOnly(parentMarker1.getName(), parentMarker2.getName()));
    }
}
