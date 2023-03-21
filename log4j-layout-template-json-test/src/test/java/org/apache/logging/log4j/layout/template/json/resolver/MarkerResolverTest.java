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

import java.util.Arrays;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout;
import org.junit.jupiter.api.Test;

import static org.apache.logging.log4j.layout.template.json.TestHelpers.*;
import static org.assertj.core.api.Assertions.assertThat;

class MarkerResolverTest {

  @Test
  void should_have_a_marker_name() {
    final String eventTemplate = writeJson(asMap(
      "marker",
      asMap(
        "$resolver", "marker",
        "field", "name"
      )
    ));

    // Create the layout.
    final JsonTemplateLayout layout = JsonTemplateLayout
            .newBuilder()
            .setConfiguration(CONFIGURATION)
            .setEventTemplate(eventTemplate)
            .build();

    // Create the log event.
    final Marker marker = MarkerManager.getMarker("MARKER");
    final LogEvent logEvent = Log4jLogEvent
            .newBuilder()
            .setMarker(marker)
            .build();

    // Check the serialized event.
    usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
      assertThat(accessor.getString("marker")).isEqualTo("MARKER");
    });
  }

  @Test
  void should_list_parents_as_array() {
    final String eventTemplate = writeJson(asMap(
      "parents",
      asMap(
        "$resolver", "marker",
        "field", "parents"
      )
    ));

    // Create the layout.
    final JsonTemplateLayout layout = JsonTemplateLayout
            .newBuilder()
            .setConfiguration(CONFIGURATION)
            .setEventTemplate(eventTemplate)
            .build();

      // Create the log event.
    final Marker PARENT_MARKER_1 = MarkerManager.getMarker("PARENT_MARKER_NAME_1");
    final Marker PARENT_MARKER_2 = MarkerManager.getMarker("PARENT_MARKER_NAME_2");
    final Marker CHILD_MARKER = MarkerManager.getMarker("CHILD_MARKER_NAME");
    CHILD_MARKER.setParents(PARENT_MARKER_1, PARENT_MARKER_2);

    final LogEvent logEvent = Log4jLogEvent
            .newBuilder()
            .setMarker(CHILD_MARKER)
            .build();

    // Check the serialized event.
    usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
      assertThat(accessor.getList("parents", String.class)).hasSize(2);
      assertThat(accessor.getList("parents", String.class)).containsAll(
        Arrays.asList(
          "PARENT_MARKER_NAME_1",
          "PARENT_MARKER_NAME_2"
        )
      );
    });
  }

}
