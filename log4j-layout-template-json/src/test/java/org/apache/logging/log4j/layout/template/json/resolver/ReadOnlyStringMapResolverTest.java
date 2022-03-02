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
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.message.StringMapMessage;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import static org.apache.logging.log4j.layout.template.json.TestHelpers.*;
import static org.assertj.core.api.Assertions.assertThat;

class ReadOnlyStringMapResolverTest {

    @Test
    void key_should_not_be_allowed_with_flatten() {
        verifyConfigFailure(
                writeJson(asMap(
                        "$resolver", "mdc",
                        "key", "foo",
                        "flatten", true)),
                IllegalArgumentException.class,
                "key and flatten options cannot be combined");
    }

    @Test
    void invalid_pattern_should_fail() {
        verifyConfigFailure(
                writeJson(asMap(
                        "$resolver", "mdc",
                        "pattern", "[1")),
                PatternSyntaxException.class,
                "Unclosed character");
    }

    @Test
    void pattern_should_not_be_allowed_with_key() {
        verifyConfigFailure(
                writeJson(asMap(
                        "$resolver", "mdc",
                        "key", "foo",
                        "pattern", "bar")),
                IllegalArgumentException.class,
                "pattern and key options cannot be combined");
    }

    @Test
    void replacement_should_not_be_allowed_without_pattern() {
        verifyConfigFailure(
                writeJson(asMap(
                        "$resolver", "mdc",
                        "replacement", "$1")),
                IllegalArgumentException.class,
                "replacement cannot be provided without a pattern");
    }

    private static void verifyConfigFailure(
            final String eventTemplate,
            final Class<? extends Throwable> failureClass,
            final String failureMessage) {
        Assertions
                .assertThatThrownBy(() -> JsonTemplateLayout
                        .newBuilder()
                        .setConfiguration(CONFIGURATION)
                        .setEventTemplate(eventTemplate)
                        .build())
                .isInstanceOf(failureClass)
                .hasMessageContaining(failureMessage);
    }

    @Test
    void pattern_replacement_should_work() {

        // Create the event template.
        final String eventTemplate = writeJson(asMap(
                "$resolver", "mdc",
                "pattern", "user:(role|rank)",
                "replacement", "$1"));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate(eventTemplate)
                .build();

        // Create the log event.
        final StringMap contextData = new SortedArrayStringMap();
        contextData.putValue("user:role", "engineer");
        contextData.putValue("user:rank", "senior");
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setContextData(contextData)
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            assertThat(accessor.getString("role")).isEqualTo("engineer");
            assertThat(accessor.getString("rank")).isEqualTo("senior");
        });

    }

    @Test
    void test_mdc_key_access() {

        // Create the log event.
        final SimpleMessage message = new SimpleMessage("Hello, World!");
        final StringMap contextData = new SortedArrayStringMap();
        final String mdcDirectlyAccessedKey = "mdcKey1";
        final String mdcDirectlyAccessedValue = "mdcValue1";
        contextData.putValue(mdcDirectlyAccessedKey, mdcDirectlyAccessedValue);
        final String mdcDirectlyAccessedNullPropertyKey = "mdcKey2";
        contextData.putValue(mdcDirectlyAccessedNullPropertyKey, null);
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setMessage(message)
                .setContextData(contextData)
                .build();

        // Check the serialized event.
        testReadOnlyStringMapKeyAccess(
                mdcDirectlyAccessedKey,
                mdcDirectlyAccessedValue,
                mdcDirectlyAccessedNullPropertyKey,
                logEvent,
                "mdc");

    }

    @Test
    public void test_map_key_access() {

        // Create the log event.
        final String directlyAccessedKey = "mapKey1";
        final String directlyAccessedValue = "mapValue1";
        final String directlyAccessedNullPropertyKey = "mapKey2";
        final Message message = new StringMapMessage()
                .with(directlyAccessedKey, directlyAccessedValue);
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setMessage(message)
                .build();

        // Check the serialized event.
        testReadOnlyStringMapKeyAccess(
                directlyAccessedKey,
                directlyAccessedValue,
                directlyAccessedNullPropertyKey,
                logEvent,
                "map");

    }

    private static void testReadOnlyStringMapKeyAccess(
            final String directlyAccessedKey,
            final String directlyAccessedValue,
            final String directlyAccessedNullPropertyKey,
            final LogEvent logEvent,
            final String resolverName) {

        // Create the event template.
        String eventTemplate = writeJson(asMap(
                directlyAccessedKey, asMap(
                        "$resolver", resolverName,
                        "key", directlyAccessedKey),
                directlyAccessedNullPropertyKey, asMap(
                        "$resolver", resolverName,
                        "key", directlyAccessedNullPropertyKey)));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setStackTraceEnabled(true)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            assertThat(accessor.getString(directlyAccessedKey)).isEqualTo(directlyAccessedValue);
            assertThat(accessor.getString(directlyAccessedNullPropertyKey)).isNull();
        });

    }

    @Test
    void test_mdc_pattern() {

        // Create the log event.
        final SimpleMessage message = new SimpleMessage("Hello, World!");
        final StringMap contextData = new SortedArrayStringMap();
        final String mdcPatternMatchedKey = "mdcKey1";
        final String mdcPatternMatchedValue = "mdcValue1";
        contextData.putValue(mdcPatternMatchedKey, mdcPatternMatchedValue);
        final String mdcPatternMismatchedKey = "mdcKey2";
        final String mdcPatternMismatchedValue = "mdcValue2";
        contextData.putValue(mdcPatternMismatchedKey, mdcPatternMismatchedValue);
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setMessage(message)
                .setContextData(contextData)
                .build();

        // Check the serialized event.
        testReadOnlyStringMapPattern(
                mdcPatternMatchedKey,
                mdcPatternMatchedValue,
                mdcPatternMismatchedKey,
                logEvent,
                "mdc");

    }

    @Test
    public void test_map_pattern() {

        // Create the log event.
        final String patternMatchedKey = "mapKey1";
        final String patternMatchedValue = "mapValue1";
        final String patternMismatchedKey = "mapKey2";
        final String patternMismatchedValue = "mapValue2";
        final Message message = new StringMapMessage()
                .with(patternMatchedKey, patternMatchedValue)
                .with(patternMismatchedKey, patternMismatchedValue);
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setMessage(message)
                .build();

        // Check the serialized event.
        testReadOnlyStringMapPattern(
                patternMatchedKey,
                patternMatchedValue,
                patternMismatchedKey,
                logEvent,
                "map");

    }

    private static void testReadOnlyStringMapPattern(
            final String patternMatchedKey,
            final String patternMatchedValue,
            final String patternMismatchedKey,
            final LogEvent logEvent,
            final String resolverName) {

        // Create the event template.
        final String mapFieldName = "map";
        final String eventTemplate = writeJson(asMap(
                mapFieldName, asMap(
                        "$resolver", resolverName,
                        "pattern", patternMatchedKey)));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setStackTraceEnabled(true)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            assertThat(accessor.getString(new String[]{mapFieldName, patternMatchedKey})).isEqualTo(patternMatchedValue);
            assertThat(accessor.exists(new String[]{mapFieldName, patternMismatchedKey})).isFalse();
        });

    }

    @Test
    void test_mdc_flatten() {

        // Create the log event.
        final SimpleMessage message = new SimpleMessage("Hello, World!");
        final StringMap contextData = new SortedArrayStringMap();
        final String mdcPatternMatchedKey = "mdcKey1";
        final String mdcPatternMatchedValue = "mdcValue1";
        contextData.putValue(mdcPatternMatchedKey, mdcPatternMatchedValue);
        final String mdcPatternMismatchedKey = "mdcKey2";
        final String mdcPatternMismatchedValue = "mdcValue2";
        contextData.putValue(mdcPatternMismatchedKey, mdcPatternMismatchedValue);
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setMessage(message)
                .setContextData(contextData)
                .build();

        // Check the serialized event.
        testReadOnlyStringMapFlatten(
                mdcPatternMatchedKey,
                mdcPatternMatchedValue,
                mdcPatternMismatchedKey,
                logEvent,
                "mdc");

    }

    @Test
    public void test_map_flatten() {

        // Create the log event.
        final String patternMatchedKey = "mapKey1";
        final String patternMatchedValue = "mapValue1";
        final String patternMismatchedKey = "mapKey2";
        final String patternMismatchedValue = "mapValue2";
        final Message message = new StringMapMessage()
                .with(patternMatchedKey, patternMatchedValue)
                .with(patternMismatchedKey, patternMismatchedValue);
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setMessage(message)
                .build();

        // Check the serialized event.
        testReadOnlyStringMapFlatten(
                patternMatchedKey,
                patternMatchedValue,
                patternMismatchedKey,
                logEvent,
                "map");

    }

    private static void testReadOnlyStringMapFlatten(
            final String patternMatchedKey,
            final String patternMatchedValue,
            final String patternMismatchedKey,
            final LogEvent logEvent,
            final String resolverName) {

        // Create the event template.
        final String prefix = "_map.";
        final String eventTemplate = writeJson(asMap(
                "ignoredFieldName", asMap(
                        "$resolver", resolverName,
                        "pattern", patternMatchedKey,
                        "flatten", asMap("prefix", prefix))));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setStackTraceEnabled(true)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            assertThat(accessor.getString(prefix + patternMatchedKey)).isEqualTo(patternMatchedValue);
            assertThat(accessor.exists(prefix + patternMismatchedKey)).isFalse();
        });

    }

    @Test
    void test_MapResolver() {

        // Create the log event.
        final StringMapMessage message = new StringMapMessage().with("key1", "val1");
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setMessage(message)
                .build();

        // Create the event template node with map values.
        final String eventTemplate = writeJson(asMap(
                "mapValue1", asMap(
                        "$resolver", "map",
                        "key", "key1"),
                "mapValue2", asMap(
                        "$resolver", "map",
                        "key", "key?")));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate(eventTemplate)
                .build();

        // Check serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            assertThat(accessor.getString("mapValue1")).isEqualTo("val1");
            assertThat(accessor.getString("mapValue2")).isNull();
        });

    }

    @Test
    void test_MapMessage_keyed_access() {

        // Create the event template.
        final String key = "list";
        final String eventTemplate = writeJson(asMap(
                "typedValue", asMap(
                        "$resolver", "map",
                        "key", key),
                "stringifiedValue", asMap(
                        "$resolver", "map",
                        "key", key,
                        "stringified", true)));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate(eventTemplate)
                .build();

        // Create the log event with a MapMessage.
        final List<Integer> value = Arrays.asList(1, 2);
        final StringMapMessage mapMessage = new StringMapMessage()
                .with(key, value);
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setMessage(mapMessage)
                .setTimeMillis(System.currentTimeMillis())
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            assertThat(accessor.getObject("typedValue")).isEqualTo(value);
            assertThat(accessor.getString("stringifiedValue")).isEqualTo(String.valueOf(value));
        });

    }

}
