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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.apache.logging.log4j.junit.Named;
import org.apache.logging.log4j.layout.template.json.util.JsonReader;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Execution(ExecutionMode.SAME_THREAD)
class JsonTemplateLayoutAdditionalFieldTest {

    @Test
    @LoggerContextSource("additionalFieldEnrichedJsonTemplateLayoutLogging.json")
    void test_JSON_config_additional_fields(
            final LoggerContext loggerContext,
            final @Named(value = "List") ListAppender appender) {
        assertAdditionalFields(loggerContext, appender);
    }

    @Test
    @LoggerContextSource("additionalFieldEnrichedJsonTemplateLayoutLogging.properties")
    void test_Properties_config_additional_fields(
            final LoggerContext loggerContext,
            final @Named(value = "List") ListAppender appender) {
        assertAdditionalFields(loggerContext, appender);
    }

    @Test
    @LoggerContextSource("additionalFieldEnrichedJsonTemplateLayoutLogging.xml")
    void test_XML_config_additional_fields(
            final LoggerContext loggerContext,
            final @Named(value = "List") ListAppender appender) {
        assertAdditionalFields(loggerContext, appender);
    }

    @Test
    @LoggerContextSource("additionalFieldEnrichedJsonTemplateLayoutLogging.yaml")
    void test_YAML_config_additional_fields(
            final LoggerContext loggerContext,
            final @Named(value = "List") ListAppender appender) {
        assertAdditionalFields(loggerContext, appender);
    }

    private static void assertAdditionalFields(
            final LoggerContext loggerContext,
            final ListAppender appender) {

        // Log an event.
        final Logger logger =
                loggerContext.getLogger(JsonTemplateLayoutAdditionalFieldTest.class);
        logger.info("trigger");

        // Verify that the appender has logged the event.
        final List<byte[]> serializedEvents = appender.getData();
        Assertions.assertThat(serializedEvents).hasSize(1);

        // Deserialize the serialized event.
        final byte[] serializedEvent = serializedEvents.get(0);
        final String serializedEventJson =
                new String(
                        serializedEvent,
                        JsonTemplateLayoutDefaults.getCharset());
        final Object serializedEventObject = JsonReader.read(serializedEventJson);
        Assertions.assertThat(serializedEventObject).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked") final Map<String, Object> serializedEventMap =
                (Map<String, Object>) serializedEventObject;

        // Verify the serialized additional fields.
        Assertions
                .assertThat(serializedEventMap)
                .containsEntry("stringField", "string")
                .containsEntry("numberField", 1)
                .containsEntry("objectField", Collections.singletonMap("numberField", 1))
                .containsEntry("listField", Arrays.asList(1, "two"));

    }

}
