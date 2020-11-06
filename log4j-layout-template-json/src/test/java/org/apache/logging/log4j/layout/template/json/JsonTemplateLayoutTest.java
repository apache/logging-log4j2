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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.SocketAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.lookup.MainMapLookup;
import org.apache.logging.log4j.core.net.Severity;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout.EventTemplateAdditionalField;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout.EventTemplateAdditionalFields;
import org.apache.logging.log4j.layout.template.json.util.JsonReader;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;
import org.apache.logging.log4j.layout.template.json.util.MapAccessor;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.logging.log4j.message.ReusableMessageFactory;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.message.StringMapMessage;
import org.apache.logging.log4j.test.AvailablePortFinder;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.util.Strings;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("DoubleBraceInitialization")
public class JsonTemplateLayoutTest {

    private static final Configuration CONFIGURATION = new DefaultConfiguration();

    private static final List<LogEvent> LOG_EVENTS = LogEventFixture.createFullLogEvents(5);

    private static final JsonWriter JSON_WRITER = JsonWriter
            .newBuilder()
            .setMaxStringLength(10_000)
            .setTruncatedStringSuffix("…")
            .build();

    private static final ObjectMapper OBJECT_MAPPER = JacksonFixture.getObjectMapper();

    private static final String LOGGER_NAME = JsonTemplateLayoutTest.class.getSimpleName();

    @Test
    public void test_serialized_event() throws IOException {
        final String lookupTestKey = "lookup_test_key";
        final String lookupTestVal =
                String.format("lookup_test_value_%d", (int) (1000 * Math.random()));
        System.setProperty(lookupTestKey, lookupTestVal);
        for (final LogEvent logEvent : LOG_EVENTS) {
            checkLogEvent(logEvent, lookupTestKey, lookupTestVal);
        }
    }

    private void checkLogEvent(
            final LogEvent logEvent,
            @SuppressWarnings("SameParameterValue")
            final String lookupTestKey,
            final String lookupTestVal) throws IOException {
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplateUri("classpath:testJsonTemplateLayout.json")
                .setStackTraceEnabled(true)
                .setLocationInfoEnabled(true)
                .build();
        final String serializedLogEvent = layout.toSerializable(logEvent);
        final JsonNode rootNode = OBJECT_MAPPER.readValue(serializedLogEvent, JsonNode.class);
        checkConstants(rootNode);
        checkBasicFields(logEvent, rootNode);
        checkSource(logEvent, rootNode);
        checkException(layout.getCharset(), logEvent, rootNode);
        checkLookupTest(lookupTestKey, lookupTestVal, rootNode);
    }

    private static void checkConstants(final JsonNode rootNode) {
        assertThat(point(rootNode, "@version").asInt()).isEqualTo(1);
    }

    private static void checkBasicFields(final LogEvent logEvent, final JsonNode rootNode) {
        assertThat(point(rootNode, "message").asText())
                .isEqualTo(logEvent.getMessage().getFormattedMessage());
        assertThat(point(rootNode, "level").asText())
                .isEqualTo(logEvent.getLevel().name());
        assertThat(point(rootNode, "logger_fqcn").asText())
                .isEqualTo(logEvent.getLoggerFqcn());
        assertThat(point(rootNode, "logger_name").asText())
                .isEqualTo(logEvent.getLoggerName());
        assertThat(point(rootNode, "thread_id").asLong())
                .isEqualTo(logEvent.getThreadId());
        assertThat(point(rootNode, "thread_name").asText())
                .isEqualTo(logEvent.getThreadName());
        assertThat(point(rootNode, "thread_priority").asInt())
                .isEqualTo(logEvent.getThreadPriority());
        assertThat(point(rootNode, "end_of_batch").asBoolean())
                .isEqualTo(logEvent.isEndOfBatch());
    }

    private static void checkSource(final LogEvent logEvent, final JsonNode rootNode) {
        assertThat(point(rootNode, "class").asText()).isEqualTo(logEvent.getSource().getClassName());
        assertThat(point(rootNode, "file").asText()).isEqualTo(logEvent.getSource().getFileName());
        assertThat(point(rootNode, "line_number").asInt()).isEqualTo(logEvent.getSource().getLineNumber());
    }

    private static void checkException(
            final Charset charset,
            final LogEvent logEvent,
            final JsonNode rootNode) {
        final Throwable thrown = logEvent.getThrown();
        if (thrown != null) {
            assertThat(point(rootNode, "exception_class").asText()).isEqualTo(thrown.getClass().getCanonicalName());
            assertThat(point(rootNode, "exception_message").asText()).isEqualTo(thrown.getMessage());
            final String stackTrace = serializeStackTrace(charset, thrown);
            assertThat(point(rootNode, "stacktrace").asText()).isEqualTo(stackTrace);
        }
    }

    private static String serializeStackTrace(
            final Charset charset,
            final Throwable exception) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final String charsetName = charset.name();
        try (final PrintStream printStream =
                     new PrintStream(outputStream, false, charsetName)) {
            exception.printStackTrace(printStream);
            return outputStream.toString(charsetName);
        }  catch (final UnsupportedEncodingException error) {
            throw new RuntimeException("failed converting the stack trace to string", error);
        }
    }

    private static void checkLookupTest(
            final String lookupTestKey,
            final String lookupTestVal,
            final JsonNode rootNode) {
        assertThat(point(rootNode, lookupTestKey).asText()).isEqualTo(lookupTestVal);
    }

    private static JsonNode point(final JsonNode node, final Object... fields) {
        final String pointer = createJsonPointer(fields);
        return node.at(pointer);
    }

    private static String createJsonPointer(final Object... fields) {
        final StringBuilder jsonPathBuilder = new StringBuilder();
        for (final Object field : fields) {
            jsonPathBuilder.append("/").append(field);
        }
        return jsonPathBuilder.toString();
    }

    @Test
    public void test_inline_template() throws Exception {

        // Create the log event.
        final SimpleMessage message = new SimpleMessage("Hello, World");
        final String timestamp = "2017-09-28T17:13:29.098+02:00";
        final long timeMillis = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                .parse(timestamp)
                .getTime();
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setLevel(Level.INFO)
                .setMessage(message)
                .setTimeMillis(timeMillis)
                .build();

        // Create the event template.
        final String timestampFieldName = "@timestamp";
        final String staticFieldName = "staticFieldName";
        final String staticFieldValue = "staticFieldValue";
        final String eventTemplate = writeJson(Map(
                timestampFieldName, Map(
                        "$resolver", "timestamp",
                        "pattern", Map("timeZone", "Europe/Amsterdam")),
                staticFieldName, staticFieldValue));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            assertThat(accessor.getString(timestampFieldName)).isEqualTo(timestamp);
            assertThat(accessor.getString(staticFieldName)).isEqualTo(staticFieldValue);
        });

    }

    @Test
    public void test_log4j_deferred_runtime_resolver_for_MapMessage() {

        // Create the event template.
        final String eventTemplate = writeJson(Map(
                "mapValue3", Map("$resolver", "message"),
                "mapValue1", "${map:key1}",
                "mapValue2", "${map:key2}",
                "nestedLookupEmptyValue", "${map:noExist:-${map:noExist2:-${map:noExist3:-}}}",
                "nestedLookupStaticValue", "${map:noExist:-${map:noExist2:-${map:noExist3:-Static Value}}}"));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate(eventTemplate)
                .build();

        // Create the log event with a MapMessage.
        final StringMapMessage mapMessage = new StringMapMessage()
                .with("key1", "val1")
                .with("key2", "val2")
                .with("key3", Collections.singletonMap("foo", "bar"));
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setLevel(Level.INFO)
                .setMessage(mapMessage)
                .setTimeMillis(System.currentTimeMillis())
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            assertThat(accessor.getString("mapValue1")).isEqualTo("val1");
            assertThat(accessor.getString("mapValue2")).isEqualTo("val2");
            assertThat(accessor.getString("nestedLookupEmptyValue")).isEmpty();
            assertThat(accessor.getString("nestedLookupStaticValue")).isEqualTo("Static Value");
        });

    }

    @Test
    public void test_MapMessage_serialization() {

        // Create the event template.
        final String eventTemplate = writeJson(Map(
                "message", Map("$resolver", "message")));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate(eventTemplate)
                .build();

        // Create the log event with a MapMessage.
        final StringMapMessage mapMessage = new StringMapMessage()
                .with("key1", "val1")
                .with("key2", 0xDEADBEEF)
                .with("key3", Collections.singletonMap("key3.1", "val3.1"));
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setLevel(Level.INFO)
                .setMessage(mapMessage)
                .setTimeMillis(System.currentTimeMillis())
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            assertThat(accessor.getString(new String[]{"message", "key1"})).isEqualTo("val1");
            assertThat(accessor.getInteger(new String[]{"message", "key2"})).isEqualTo(0xDEADBEEF);
            assertThat(accessor.getString(new String[]{"message", "key3", "key3.1"})).isEqualTo("val3.1");
        });

    }

    @Test
    public void test_MapMessage_keyed_access() {

        // Create the event template.
        final String key = "list";
        final String eventTemplate = writeJson(Map(
                "typedValue", Map(
                        "$resolver", "map",
                        "key", key),
                "stringifiedValue", Map(
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
                .setLoggerName(LOGGER_NAME)
                .setLevel(Level.INFO)
                .setMessage(mapMessage)
                .setTimeMillis(System.currentTimeMillis())
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            assertThat(accessor.getObject("typedValue")).isEqualTo(value);
            assertThat(accessor.getString("stringifiedValue")).isEqualTo(String.valueOf(value));
        });

    }

    @Test
    public void test_message_fallbackKey() {

        // Create the event template.
        final String eventTemplate = writeJson(Map(
                "message", Map(
                        "$resolver", "message",
                        "fallbackKey", "formattedMessage")));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate(eventTemplate)
                .build();

        // Create a log event with a MapMessage.
        final Message mapMessage = new StringMapMessage()
                .with("key1", "val1");
        final LogEvent mapMessageLogEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setLevel(Level.INFO)
                .setMessage(mapMessage)
                .setTimeMillis(System.currentTimeMillis())
                .build();

        // Check the serialized MapMessage.
        usingSerializedLogEventAccessor(layout, mapMessageLogEvent, accessor ->
                assertThat(accessor.getString(new String[]{"message", "key1"}))
                        .isEqualTo("val1"));

        // Create a log event with a SimpleMessage.
        final Message simpleMessage = new SimpleMessage("simple");
        final LogEvent simpleMessageLogEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setLevel(Level.INFO)
                .setMessage(simpleMessage)
                .setTimeMillis(System.currentTimeMillis())
                .build();

        // Check the serialized MapMessage.
        usingSerializedLogEventAccessor(layout, simpleMessageLogEvent, accessor ->
                assertThat(accessor.getString(new String[]{"message", "formattedMessage"}))
                        .isEqualTo("simple"));

    }

    @Test
    public void test_property_injection() {

        // Create the log event.
        final SimpleMessage message = new SimpleMessage("Hello, World");
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setLevel(Level.INFO)
                .setMessage(message)
                .build();

        // Create the event template with property.
        final String propertyName = "propertyName";
        final String eventTemplate = writeJson(Map(
                propertyName, "${" + propertyName + "}"));

        // Create the layout with property.
        final String propertyValue = "propertyValue";
        final Configuration config = ConfigurationBuilderFactory
                .newConfigurationBuilder()
                .addProperty(propertyName, propertyValue)
                .build();
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(config)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor ->
                assertThat(accessor.getString(propertyName)).isEqualTo(propertyValue));

    }

    @Test
    public void test_empty_root_cause() {

        // Create the log event.
        final SimpleMessage message = new SimpleMessage("Hello, World!");
        final RuntimeException exception = new RuntimeException("failure for test purposes");
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setLevel(Level.ERROR)
                .setMessage(message)
                .setThrown(exception)
                .build();

        // Create the event template.
        final String eventTemplate = writeJson(Map(
                "ex_class", Map(
                        "$resolver", "exception",
                        "field", "className"),
                "ex_message", Map(
                        "$resolver", "exception",
                        "field", "message"),
                "ex_stacktrace", Map(
                        "$resolver", "exception",
                        "field", "stackTrace",
                        "stringified", true),
                "root_ex_class", Map(
                        "$resolver", "exceptionRootCause",
                        "field", "className"),
                "root_ex_message", Map(
                        "$resolver", "exceptionRootCause",
                        "field", "message"),
                "root_ex_stacktrace", Map(
                        "$resolver", "exceptionRootCause",
                        "field", "stackTrace",
                        "stringified", true)));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setStackTraceEnabled(true)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            assertThat(accessor.getString("ex_class"))
                    .isEqualTo(exception.getClass().getCanonicalName());
            assertThat(accessor.getString("ex_message"))
                    .isEqualTo(exception.getMessage());
            assertThat(accessor.getString("ex_stacktrace"))
                    .startsWith(exception.getClass().getCanonicalName() + ": " + exception.getMessage());
            assertThat(accessor.getString("root_ex_class"))
                    .isEqualTo(accessor.getString("ex_class"));
            assertThat(accessor.getString("root_ex_message"))
                    .isEqualTo(accessor.getString("ex_message"));
            assertThat(accessor.getString("root_ex_stacktrace"))
                    .isEqualTo(accessor.getString("ex_stacktrace"));
        });

    }

    @Test
    public void test_root_cause() {

        // Create the log event.
        final SimpleMessage message = new SimpleMessage("Hello, World!");
        final RuntimeException exceptionCause = new RuntimeException("failure cause for test purposes");
        final RuntimeException exception = new RuntimeException("failure for test purposes", exceptionCause);
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setLevel(Level.ERROR)
                .setMessage(message)
                .setThrown(exception)
                .build();

        // Create the event template.
        final String eventTemplate = writeJson(Map(
                "ex_class", Map(
                        "$resolver", "exception",
                        "field", "className"),
                "ex_message", Map(
                        "$resolver", "exception",
                        "field", "message"),
                "ex_stacktrace", Map(
                        "$resolver", "exception",
                        "field", "stackTrace",
                        "stringified", true),
                "root_ex_class", Map(
                        "$resolver", "exceptionRootCause",
                        "field", "className"),
                "root_ex_message", Map(
                        "$resolver", "exceptionRootCause",
                        "field", "message"),
                "root_ex_stacktrace", Map(
                        "$resolver", "exceptionRootCause",
                        "field", "stackTrace",
                        "stringified", true)));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setStackTraceEnabled(true)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            assertThat(accessor.getString("ex_class"))
                    .isEqualTo(exception.getClass().getCanonicalName());
            assertThat(accessor.getString("ex_message"))
                    .isEqualTo(exception.getMessage());
            assertThat(accessor.getString("ex_stacktrace"))
                    .startsWith(exception.getClass().getCanonicalName() + ": " + exception.getMessage());
            assertThat(accessor.getString("root_ex_class"))
                    .isEqualTo(exceptionCause.getClass().getCanonicalName());
            assertThat(accessor.getString("root_ex_message"))
                    .isEqualTo(exceptionCause.getMessage());
            assertThat(accessor.getString("root_ex_stacktrace"))
                    .startsWith(exceptionCause.getClass().getCanonicalName() + ": " + exceptionCause.getMessage());
        });

    }

    @Test
    public void test_marker_name() {

        // Create the log event.
        final SimpleMessage message = new SimpleMessage("Hello, World!");
        final String markerName = "test";
        final Marker marker = MarkerManager.getMarker(markerName);
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setLevel(Level.ERROR)
                .setMessage(message)
                .setMarker(marker)
                .build();

        // Create the event template.
        final String messageKey = "message";
        final String markerNameKey = "marker";
        final String eventTemplate = writeJson(Map(
                "message", Map("$resolver", "message"),
                "marker", Map(
                        "$resolver", "marker",
                        "field", "name")));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            assertThat(accessor.getString(messageKey)).isEqualTo(message.getFormattedMessage());
            assertThat(accessor.getString(markerNameKey)).isEqualTo(markerName);
        });

    }

    @Test
    public void test_lineSeparator_suffix() {

        // Create the log event.
        final SimpleMessage message = new SimpleMessage("Hello, World!");
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setLevel(Level.INFO)
                .setMessage(message)
                .build();

        // Check line separators.
        test_lineSeparator_suffix(logEvent, true);
        test_lineSeparator_suffix(logEvent, false);

    }

    private void test_lineSeparator_suffix(
            final LogEvent logEvent,
            final boolean prettyPrintEnabled) {

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplateUri("classpath:LogstashJsonEventLayoutV1.json")
                .build();

        // Check the serialized event.
        final String serializedLogEvent = layout.toSerializable(logEvent);
        final String assertionCaption = String.format("testing lineSeperator (prettyPrintEnabled=%s)", prettyPrintEnabled);
        assertThat(serializedLogEvent).as(assertionCaption).endsWith("}" + System.lineSeparator());

    }

    @Test
    public void test_main_key_access() {

        // Set main() arguments.
        final String kwKey = "--name";
        final String kwVal = "aNameValue";
        final String positionArg = "position2Value";
        final String missingKwKey = "--missing";
        final String[] mainArgs = {kwKey, kwVal, positionArg};
        MainMapLookup.setMainArguments(mainArgs);

        // Create the log event.
        final SimpleMessage message = new SimpleMessage("Hello, World!");
        final LogEvent logEvent = Log4jLogEvent
            .newBuilder()
            .setLoggerName(LOGGER_NAME)
            .setLevel(Level.INFO)
            .setMessage(message)
            .build();

        // Create the template.
        final String template = writeJson(Map(
                "name", Map(
                        "$resolver", "main",
                        "key", kwKey),
                "positionArg", Map(
                        "$resolver", "main",
                        "index", 2),
                "notFoundArg", Map(
                        "$resolver", "main",
                        "key", missingKwKey)));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate(template)
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            assertThat(accessor.getString("name")).isEqualTo(kwVal);
            assertThat(accessor.getString("positionArg")).isEqualTo(positionArg);
            assertThat(accessor.exists("notFoundArg")).isFalse();
        });

    }

    @Test
    public void test_mdc_key_access() {

        // Create the log event.
        final SimpleMessage message = new SimpleMessage("Hello, World!");
        final StringMap contextData = new SortedArrayStringMap();
        final String mdcDirectlyAccessedKey = "mdcKey1";
        final String mdcDirectlyAccessedValue = "mdcValue1";
        contextData.putValue(mdcDirectlyAccessedKey, mdcDirectlyAccessedValue);
        final String mdcDirectlyAccessedNullPropertyKey = "mdcKey2";
        final String mdcDirectlyAccessedNullPropertyValue = null;
        // noinspection ConstantConditions
        contextData.putValue(mdcDirectlyAccessedNullPropertyKey, mdcDirectlyAccessedNullPropertyValue);
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setLevel(Level.INFO)
                .setMessage(message)
                .setContextData(contextData)
                .build();

        // Create the event template.
        String eventTemplate = writeJson(Map(
                mdcDirectlyAccessedKey, Map(
                        "$resolver", "mdc",
                        "key", mdcDirectlyAccessedKey),
                mdcDirectlyAccessedNullPropertyKey, Map(
                        "$resolver", "mdc",
                        "key", mdcDirectlyAccessedNullPropertyKey)));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setStackTraceEnabled(true)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            assertThat(accessor.getString(mdcDirectlyAccessedKey)).isEqualTo(mdcDirectlyAccessedValue);
            assertThat(accessor.getString(mdcDirectlyAccessedNullPropertyKey)).isNull();
        });

    }

    @Test
    public void test_mdc_pattern() {

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
                .setLoggerName(LOGGER_NAME)
                .setLevel(Level.INFO)
                .setMessage(message)
                .setContextData(contextData)
                .build();

        // Create the event template.
        final String mdcFieldName = "mdc";
        final String eventTemplate = writeJson(Map(
                mdcFieldName, Map(
                        "$resolver", "mdc",
                        "pattern", mdcPatternMatchedKey)));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setStackTraceEnabled(true)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            assertThat(accessor.getString(new String[]{mdcFieldName, mdcPatternMatchedKey})).isEqualTo(mdcPatternMatchedValue);
            assertThat(accessor.exists(new String[]{mdcFieldName, mdcPatternMismatchedKey})).isFalse();
        });

    }

    @Test
    public void test_mdc_flatten() {

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
                .setLoggerName(LOGGER_NAME)
                .setLevel(Level.INFO)
                .setMessage(message)
                .setContextData(contextData)
                .build();

        // Create the event template.
        final String mdcPrefix = "_mdc.";
        final String eventTemplate = writeJson(Map(
                "ignoredFieldName", Map(
                        "$resolver", "mdc",
                        "pattern", mdcPatternMatchedKey,
                        "flatten", Map("prefix", mdcPrefix))));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setStackTraceEnabled(true)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            assertThat(accessor.getString(mdcPrefix + mdcPatternMatchedKey)).isEqualTo(mdcPatternMatchedValue);
            assertThat(accessor.exists(mdcPrefix + mdcPatternMismatchedKey)).isFalse();
        });

    }

    @Test
    public void test_MapResolver() {

        // Create the log event.
        final StringMapMessage message = new StringMapMessage().with("key1", "val1");
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setLevel(Level.INFO)
                .setMessage(message)
                .build();

        // Create the event template node with map values.
        final String eventTemplate = writeJson(Map(
                "mapValue1", Map(
                        "$resolver", "map",
                        "key", "key1"),
                "mapValue2", Map(
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
    public void test_StringMapMessage() {

        // Create the log event.
        final StringMapMessage message = new StringMapMessage();
        message.put("message", "Hello, World!");
        message.put("bottle", "Kickapoo Joy Juice");
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setLevel(Level.INFO)
                .setMessage(message)
                .build();

        // Create the event template.
        final String eventTemplate = writeJson(Map(
                "message", Map("$resolver", "message")));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setStackTraceEnabled(true)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            assertThat(accessor.getString(new String[]{"message", "message"})).isEqualTo("Hello, World!");
            assertThat(accessor.getString(new String[]{"message", "bottle"})).isEqualTo("Kickapoo Joy Juice");
        });

    }

    @Test
    public void test_ObjectMessage() {

        // Create the log event.
        final int id = 0xDEADBEEF;
        final String name = "name-" + id;
        final Object attachment = new LinkedHashMap<String, Object>() {{
            put("id", id);
            put("name", name);
        }};
        final ObjectMessage message = new ObjectMessage(attachment);
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setLevel(Level.INFO)
                .setMessage(message)
                .build();

        // Create the event template.
        final String eventTemplate = writeJson(Map(
                "message", Map("$resolver", "message")));

        // Create the layout.
        JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setStackTraceEnabled(true)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            assertThat(accessor.getInteger(new String[]{"message", "id"})).isEqualTo(id);
            assertThat(accessor.getString(new String[]{"message", "name"})).isEqualTo(name);
        });

    }

    @Test
    public void test_StackTraceElement_template() {

        // Create the stack trace element template.
        final String classNameFieldName = "className";
        final String methodNameFieldName = "methodName";
        final String fileNameFieldName = "fileName";
        final String lineNumberFieldName = "lineNumber";
        final String stackTraceElementTemplate = writeJson(Map(
                classNameFieldName, Map(
                        "$resolver", "stackTraceElement",
                        "field", "className"),
                methodNameFieldName, Map(
                        "$resolver", "stackTraceElement",
                        "field", "methodName"),
                fileNameFieldName, Map(
                        "$resolver", "stackTraceElement",
                        "field", "fileName"),
                lineNumberFieldName, Map(
                        "$resolver", "stackTraceElement",
                        "field", "lineNumber")));

        // Create the event template.
        final String stackTraceFieldName = "stackTrace";
        final String eventTemplate = writeJson(Map(
                stackTraceFieldName, Map(
                        "$resolver", "exception",
                        "field", "stackTrace")));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setStackTraceEnabled(true)
                .setStackTraceElementTemplate(stackTraceElementTemplate)
                .setEventTemplate(eventTemplate)
                .build();

        // Create the log event.
        final SimpleMessage message = new SimpleMessage("Hello, World!");
        final RuntimeException exceptionCause = new RuntimeException("failure cause for test purposes");
        final RuntimeException exception = new RuntimeException("failure for test purposes", exceptionCause);
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setLevel(Level.ERROR)
                .setMessage(message)
                .setThrown(exception)
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            assertThat(accessor.exists(stackTraceFieldName)).isTrue();
            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> deserializedStackTraceElements =
                    accessor.getObject(stackTraceFieldName, List.class);
            final StackTraceElement[] stackTraceElements = exception.getStackTrace();
            assertThat(deserializedStackTraceElements.size()).isEqualTo(stackTraceElements.length);
            for (int stackTraceElementIndex = 0;
                 stackTraceElementIndex < stackTraceElements.length;
                 stackTraceElementIndex++) {
                final StackTraceElement stackTraceElement = stackTraceElements[stackTraceElementIndex];
                final Map<String, Object> deserializedStackTraceElement = deserializedStackTraceElements.get(stackTraceElementIndex);
                assertThat(deserializedStackTraceElement.size()).isEqualTo(4);
                assertThat(deserializedStackTraceElement.get(classNameFieldName))
                        .isEqualTo(stackTraceElement.getClassName());
                assertThat(deserializedStackTraceElement.get(methodNameFieldName))
                        .isEqualTo(stackTraceElement.getMethodName());
                assertThat(deserializedStackTraceElement.get(fileNameFieldName))
                        .isEqualTo(stackTraceElement.getFileName());
                assertThat(deserializedStackTraceElement.get(lineNumberFieldName))
                        .isEqualTo(stackTraceElement.getLineNumber());
            }
        });

    }

    @Test
    public void test_toSerializable_toByteArray_encode_outputs() {

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplateUri("classpath:LogstashJsonEventLayoutV1.json")
                .setStackTraceEnabled(true)
                .build();

        // Create the log event.
        final LogEvent logEvent = LogEventFixture.createFullLogEvents(1).get(0);

        // Get toSerializable() output.
        final String toSerializableOutput = layout.toSerializable(logEvent);

        // Get toByteArrayOutput().
        final byte[] toByteArrayOutputBytes = layout.toByteArray(logEvent);
        final String toByteArrayOutput = new String(
                toByteArrayOutputBytes,
                0,
                toByteArrayOutputBytes.length,
                layout.getCharset());

        // Get encode() output.
        final ByteBuffer byteBuffer = ByteBuffer.allocate(512 * 1024);
        final ByteBufferDestination byteBufferDestination = new ByteBufferDestination() {

            @Override
            public ByteBuffer getByteBuffer() {
                return byteBuffer;
            }

            @Override
            public ByteBuffer drain(final ByteBuffer ignored) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void writeBytes(final ByteBuffer data) {
                byteBuffer.put(data);
            }

            @Override
            public void writeBytes(final byte[] buffer, final int offset, final int length) {
                byteBuffer.put(buffer, offset, length);
            }

        };
        layout.encode(logEvent, byteBufferDestination);
        String encodeOutput = new String(
                byteBuffer.array(),
                0,
                byteBuffer.position(),
                layout.getCharset());

        // Compare outputs.
        assertThat(toSerializableOutput).isEqualTo(toByteArrayOutput);
        assertThat(toByteArrayOutput).isEqualTo(encodeOutput);

    }

    @Test
    public void test_maxStringLength() {

        // Create the log event.
        final int maxStringLength = 30;
        final String excessiveMessageString = Strings.repeat("m", maxStringLength) + 'M';
        final SimpleMessage message = new SimpleMessage(excessiveMessageString);
        final Throwable thrown = new RuntimeException();
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setLevel(Level.INFO)
                .setMessage(message)
                .setThrown(thrown)
                .build();

        // Create the event template node with map values.
        final String messageKey = "message";
        final String excessiveKey = Strings.repeat("k", maxStringLength) + 'K';
        final String excessiveValue = Strings.repeat("v", maxStringLength) + 'V';
        final String nullValueKey = "nullValueKey";
        final String eventTemplate = writeJson(Map(
                messageKey, Map("$resolver", "message"),
                excessiveKey, excessiveValue,
                nullValueKey, Map(
                        "$resolver", "exception",
                        "field", "message")));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate(eventTemplate)
                .setMaxStringLength(maxStringLength)
                .build();

        // Check serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            final String truncatedStringSuffix =
                    JsonTemplateLayoutDefaults.getTruncatedStringSuffix();
            final String truncatedMessageString =
                    excessiveMessageString.substring(0, maxStringLength) +
                            truncatedStringSuffix;
            assertThat(accessor.getString(messageKey)).isEqualTo(truncatedMessageString);
            final String truncatedKey =
                    excessiveKey.substring(0, maxStringLength) +
                            truncatedStringSuffix;
            final String truncatedValue =
                    excessiveValue.substring(0, maxStringLength) +
                            truncatedStringSuffix;
            assertThat(accessor.getString(truncatedKey)).isEqualTo(truncatedValue);
            assertThat(accessor.getString(nullValueKey)).isNull();
        });

    }

    private static final class NonAsciiUtf8MethodNameContainingException extends RuntimeException {;

        public static final long serialVersionUID = 0;

        private static final String NON_ASCII_UTF8_TEXT = "அஆஇฬ๘";

        private static final NonAsciiUtf8MethodNameContainingException INSTANCE =
                createInstance();

        private static NonAsciiUtf8MethodNameContainingException createInstance() {
            try {
                throwException_அஆஇฬ๘();
                throw new IllegalStateException("should not have reached here");
            } catch (final NonAsciiUtf8MethodNameContainingException exception) {
                return exception;
            }
        }

        @SuppressWarnings("NonAsciiCharacters")
        private static void throwException_அஆஇฬ๘() {
            throw new NonAsciiUtf8MethodNameContainingException(
                    "exception with non-ASCII UTF-8 method name");
        }

        private NonAsciiUtf8MethodNameContainingException(final String message) {
            super(message);
        }

    }

    @Test
    public void test_exception_with_nonAscii_utf8_method_name() {

        // Create the log event.
        final SimpleMessage message = new SimpleMessage("Hello, World!");
        final RuntimeException exception = NonAsciiUtf8MethodNameContainingException.INSTANCE;
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setLevel(Level.ERROR)
                .setMessage(message)
                .setThrown(exception)
                .build();

        // Create the event template.
        final String eventTemplate = writeJson(Map(
                "ex_stacktrace", Map(
                        "$resolver", "exception",
                        "field", "stackTrace",
                        "stringified", true)));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setStackTraceEnabled(true)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor ->
                assertThat(accessor.getString("ex_stacktrace"))
                        .contains(NonAsciiUtf8MethodNameContainingException.NON_ASCII_UTF8_TEXT));

    }

    @Test
    public void test_event_template_additional_fields() {

        // Create the log event.
        final SimpleMessage message = new SimpleMessage("Hello, World!");
        final RuntimeException exception = NonAsciiUtf8MethodNameContainingException.INSTANCE;
        final Level level = Level.ERROR;
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setLevel(level)
                .setMessage(message)
                .setThrown(exception)
                .build();

        // Create the event template.
        final String eventTemplate = "{}";

        // Create the layout.
        final EventTemplateAdditionalField[] additionalFieldPairs = {
                EventTemplateAdditionalField
                        .newBuilder()
                        .setKey("number")
                        .setValue("1")
                        .setType(EventTemplateAdditionalField.Type.JSON)
                        .build(),
                EventTemplateAdditionalField
                        .newBuilder()
                        .setKey("string")
                        .setValue("foo")
                        .build(),
                EventTemplateAdditionalField
                        .newBuilder()
                        .setKey("level")
                        .setValue("{\"$resolver\": \"level\", \"field\": \"name\"}")
                        .setType(EventTemplateAdditionalField.Type.JSON)
                        .build()
        };
        final EventTemplateAdditionalFields additionalFields = EventTemplateAdditionalFields
                .newBuilder()
                .setAdditionalFields(additionalFieldPairs)
                .build();
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setStackTraceEnabled(true)
                .setEventTemplate(eventTemplate)
                .setEventTemplateAdditionalFields(additionalFields)
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            assertThat(accessor.getInteger("number")).isEqualTo(1);
            assertThat(accessor.getString("string")).isEqualTo("foo");
            assertThat(accessor.getString("level")).isEqualTo(level.name());
        });

    }

    @Test
    @SuppressWarnings("FloatingPointLiteralPrecision")
    public void test_timestamp_epoch_resolvers() {

        final List<Map<String, Object>> testCases = Arrays.asList(
                Map(
                        "epochSecs", new BigDecimal("1581082727.982123456"),
                        "epochSecsRounded", 1581082727,
                        "epochSecsNanos", 982123456,
                        "epochMillis", new BigDecimal("1581082727982.123456"),
                        "epochMillisRounded", 1581082727982L,
                        "epochMillisNanos", 123456,
                        "epochNanos", 1581082727982123456L),
                Map(
                        "epochSecs", new BigDecimal("1591177590.005000001"),
                        "epochSecsRounded", 1591177590,
                        "epochSecsNanos", 5000001,
                        "epochMillis", new BigDecimal("1591177590005.000001"),
                        "epochMillisRounded", 1591177590005L,
                        "epochMillisNanos", 1,
                        "epochNanos", 1591177590005000001L));

        // Create the event template.
        final String eventTemplate = writeJson(Map(
                "epochSecs", Map(
                        "$resolver", "timestamp",
                        "epoch", Map("unit", "secs")),
                "epochSecsRounded", Map(
                        "$resolver", "timestamp",
                        "epoch", Map(
                                "unit", "secs",
                                "rounded", true)),
                "epochSecsNanos", Map(
                        "$resolver", "timestamp",
                        "epoch", Map("unit", "secs.nanos")),
                "epochMillis", Map(
                        "$resolver", "timestamp",
                        "epoch", Map("unit", "millis")),
                "epochMillisRounded", Map(
                        "$resolver", "timestamp",
                        "epoch", Map(
                                "unit", "millis",
                                "rounded", true)),
                "epochMillisNanos", Map(
                        "$resolver", "timestamp",
                        "epoch", Map("unit", "millis.nanos")),
                "epochNanos", Map(
                        "$resolver", "timestamp",
                        "epoch", Map("unit", "nanos"))));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate(eventTemplate)
                .build();

        testCases.forEach(testCase -> {

            // Create the log event.
            final SimpleMessage message = new SimpleMessage("Hello, World!");
            final Level level = Level.ERROR;
            final MutableInstant instant = new MutableInstant();
            final Object instantSecsObject = testCase.get("epochSecsRounded");
            final long instantSecs = instantSecsObject instanceof Long
                    ? (long) instantSecsObject
                    : (int) instantSecsObject;
            final int instantSecsNanos = (int) testCase.get("epochSecsNanos");
            instant.initFromEpochSecond(instantSecs, instantSecsNanos);
            final LogEvent logEvent = Log4jLogEvent
                    .newBuilder()
                    .setLoggerName(LOGGER_NAME)
                    .setLevel(level)
                    .setMessage(message)
                    .setInstant(instant)
                    .build();

            // Verify the test case.
            usingSerializedLogEventAccessor(layout, logEvent, accessor ->
                    testCase.forEach((key, expectedValue) ->
                            Assertions
                                    .assertThat(accessor.getObject(key))
                                    .describedAs("key=%s", key)
                                    .isEqualTo(expectedValue)));

        });

    }

    @Test
    public void test_timestamp_pattern_resolver() {

        // Create log events.
        final String logEvent1FormattedInstant = "2019-01-02T09:34:11Z";
        final LogEvent logEvent1 = createLogEventAtInstant(logEvent1FormattedInstant);
        final String logEvent2FormattedInstant = "2019-01-02T09:34:12Z";
        final LogEvent logEvent2 = createLogEventAtInstant(logEvent2FormattedInstant);
        @SuppressWarnings("UnnecessaryLocalVariable")
        final String logEvent3FormattedInstant = logEvent2FormattedInstant;
        final LogEvent logEvent3 = createLogEventAtInstant(logEvent3FormattedInstant);
        final String logEvent4FormattedInstant = "2019-01-02T09:34:13Z";
        final LogEvent logEvent4 = createLogEventAtInstant(logEvent4FormattedInstant);

        // Create the event template.
        final String eventTemplate = writeJson(Map(
                "timestamp", Map(
                        "$resolver", "timestamp",
                        "pattern", Map(
                                "format", "yyyy-MM-dd'T'HH:mm:ss'Z'",
                                "timeZone", "UTC"))));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized 1st event.
        usingSerializedLogEventAccessor(layout, logEvent1, accessor ->
                assertThat(accessor.getString("timestamp"))
                        .isEqualTo(logEvent1FormattedInstant));

        // Check the serialized 2nd event.
        usingSerializedLogEventAccessor(layout, logEvent2, accessor ->
                assertThat(accessor.getString("timestamp"))
                        .isEqualTo(logEvent2FormattedInstant));

        // Check the serialized 3rd event.
        usingSerializedLogEventAccessor(layout, logEvent3, accessor ->
                assertThat(accessor.getString("timestamp"))
                        .isEqualTo(logEvent3FormattedInstant));

        // Check the serialized 4th event.
        usingSerializedLogEventAccessor(layout, logEvent4, accessor ->
                assertThat(accessor.getString("timestamp"))
                        .isEqualTo(logEvent4FormattedInstant));

    }

    private static LogEvent createLogEventAtInstant(final String formattedInstant) {
        final SimpleMessage message = new SimpleMessage("LogEvent at instant " + formattedInstant);
        final long instantEpochMillis = Instant.parse(formattedInstant).toEpochMilli();
        final MutableInstant instant = new MutableInstant();
        instant.initFromEpochMilli(instantEpochMillis, 0);
        return Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setMessage(message)
                .setInstant(instant)
                .build();
    }

    @Test
    public void test_level_severity() {

        // Create the event template.
        final String eventTemplate = writeJson(Map(
                "severityKeyword", Map(
                        "$resolver", "level",
                        "field", "severity",
                        "severity", Map("field", "keyword")),
                "severityCode", Map(
                        "$resolver", "level",
                        "field", "severity",
                        "severity", Map("field", "code"))));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate(eventTemplate)
                .build();

        for (final Level level : Level.values()) {

            // Create the log event.
            final SimpleMessage message = new SimpleMessage("Hello, World!");
            final LogEvent logEvent = Log4jLogEvent
                    .newBuilder()
                    .setLoggerName(LOGGER_NAME)
                    .setLevel(level)
                    .setMessage(message)
                    .build();

            // Check the serialized event.
            usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
                final Severity expectedSeverity = Severity.getSeverity(level);
                final String expectedSeverityKeyword = expectedSeverity.name();
                final int expectedSeverityCode = expectedSeverity.getCode();
                assertThat(accessor.getString("severityKeyword")).isEqualTo(expectedSeverityKeyword);
                assertThat(accessor.getInteger("severityCode")).isEqualTo(expectedSeverityCode);
            });

        }

    }

    @Test
    public void test_exception_resolvers_against_no_exceptions() {

        // Create the log event.
        final SimpleMessage message = new SimpleMessage("Hello, World!");
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setMessage(message)
                .build();

        // Create the event template.
        final String eventTemplate = writeJson(Map(
                "exStackTrace", Map(
                        "$resolver", "exception",
                        "field", "stackTrace"),
                "exStackTraceString", Map(
                        "$resolver", "exception",
                        "field", "stackTrace",
                        "stringified", true),
                "exRootCauseStackTrace", Map(
                        "$resolver", "exceptionRootCause",
                        "field", "stackTrace"),
                "exRootCauseStackTraceString", Map(
                        "$resolver", "exceptionRootCause",
                        "field", "stackTrace",
                        "stringified", true),
                "requiredFieldTriggeringError", true));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate(eventTemplate)
                .setStackTraceEnabled(true)
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            assertThat(accessor.getObject("exStackTrace")).isNull();
            assertThat(accessor.getObject("exStackTraceString")).isNull();
            assertThat(accessor.getObject("exRootCauseStackTrace")).isNull();
            assertThat(accessor.getObject("exRootCauseStackTraceString")).isNull();
            assertThat(accessor.getBoolean("requiredFieldTriggeringError")).isTrue();
        });

    }

    @Test
    public void test_StackTraceTextResolver_with_maxStringLength() {

        // Create the event template.
        final String eventTemplate = writeJson(Map(
                "stackTrace", Map(
                        "$resolver", "exception",
                        "field", "stackTrace",
                        "stringified", true)));

        // Create the layout.
        final int maxStringLength = eventTemplate.length();
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate(eventTemplate)
                .setMaxStringLength(maxStringLength)
                .setStackTraceEnabled(true)
                .build();

        // Create the log event.
        final SimpleMessage message = new SimpleMessage("foo");
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setMessage(message)
                .setThrown(NonAsciiUtf8MethodNameContainingException.INSTANCE)
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            final int expectedLength = maxStringLength +
                    JsonTemplateLayoutDefaults.getTruncatedStringSuffix().length();
            assertThat(accessor.getString("stackTrace").length()).isEqualTo(expectedLength);
        });

    }

    @Test
    public void test_null_eventDelimiter() {

        // Create the event template.
        final String eventTemplate = writeJson(Map("key", "val"));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate(eventTemplate)
                .setEventDelimiter("\0")
                .build();

        // Create the log event.
        final SimpleMessage message = new SimpleMessage("foo");
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setMessage(message)
                .setThrown(NonAsciiUtf8MethodNameContainingException.INSTANCE)
                .build();

        // Check the serialized event.
        final String serializedLogEvent = layout.toSerializable(logEvent);
        assertThat(serializedLogEvent).isEqualTo(eventTemplate + '\0');

    }

    @Test
    public void test_against_SocketAppender() throws Exception {

        // Craft nasty events.
        final List<LogEvent> logEvents = createNastyLogEvents();

        // Create the event template.
        final String eventTemplate = writeJson(Map(
                "message", Map("$resolver", "message")));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate(eventTemplate)
                .build();

        // Create the server.
        final int port = AvailablePortFinder.getNextAvailable();
        try (final JsonAcceptingTcpServer server = new JsonAcceptingTcpServer(port, 1)) {

            // Create the appender.
            final SocketAppender appender = SocketAppender
                    .newBuilder()
                    .withHost("localhost")
                    .withBufferedIo(false)
                    .withPort(port)
                    .withReconnectDelayMillis(100)
                    .setName("test")
                    .withImmediateFail(false)
                    .setIgnoreExceptions(false)
                    .setLayout(layout)
                    .build();

            // Start the appender.
            appender.start();

            // Transfer and verify the log events.
            for (int logEventIndex = 0; logEventIndex < logEvents.size(); logEventIndex++) {

                // Send the log event.
                final LogEvent logEvent = logEvents.get(logEventIndex);
                appender.append(logEvent);
                appender.getManager().flush();

                // Pull the parsed log event.
                final JsonNode node = server.receivedNodes.poll(3, TimeUnit.SECONDS);
                assertThat(node)
                        .as("logEventIndex=%d", logEventIndex)
                        .isNotNull();

                // Verify the received content.
                final String expectedMessage = logEvent.getMessage().getFormattedMessage();
                final String expectedMessageChars = explainChars(expectedMessage);
                final String actualMessage = point(node, "message").asText();
                final String actualMessageChars = explainChars(actualMessage);
                assertThat(actualMessageChars)
                        .as("logEventIndex=%d", logEventIndex)
                        .isEqualTo(expectedMessageChars);

            }

            // Verify that there were no overflows.
            assertThat(server.droppedNodeCount).isZero();

        }

    }

    private static List<LogEvent> createNastyLogEvents() {
        return createNastyMessages()
                .stream()
                .map(message -> Log4jLogEvent
                        .newBuilder()
                        .setLoggerName(LOGGER_NAME)
                        .setMessage(message)
                        .build())
                .collect(Collectors.toList());
    }

    private static List<SimpleMessage> createNastyMessages() {

        // Determine the message count and character offset.
        final int messageCount = 1024;
        final int minChar = Character.MIN_VALUE;
        final int maxChar = Character.MIN_HIGH_SURROGATE - 1;
        final int totalCharCount = maxChar - minChar + 1;
        final int charOffset = totalCharCount / messageCount;

        // Populate messages.
        List<SimpleMessage> messages = new ArrayList<>(messageCount);
        for (int messageIndex = 0; messageIndex < messageCount; messageIndex++) {
            final StringBuilder stringBuilder = new StringBuilder(messageIndex + "@");
            for (int charIndex = 0; charIndex < charOffset; charIndex++) {
                final char c = (char) (minChar + messageIndex * charOffset + charIndex);
                stringBuilder.append(c);
            }
            final String messageString = stringBuilder.toString();
            final SimpleMessage message = new SimpleMessage(messageString);
            messages.add(message);
        }
        return messages;

    }

    private static final class JsonAcceptingTcpServer extends Thread implements AutoCloseable {

        private final ServerSocket serverSocket;

        private final BlockingQueue<JsonNode> receivedNodes;

        private volatile int droppedNodeCount = 0;

        private volatile boolean closed = false;

        private JsonAcceptingTcpServer(
                final int port,
                final int capacity) throws IOException {
            this.serverSocket = new ServerSocket(port);
            this.receivedNodes = new ArrayBlockingQueue<>(capacity);
            serverSocket.setReuseAddress(true);
            serverSocket.setSoTimeout(5_000);
            setDaemon(true);
            start();
        }

        @Override
        public void run() {
            try {
                try (final Socket socket = serverSocket.accept()) {
                    final InputStream inputStream = socket.getInputStream();
                    while (!closed) {
                        final MappingIterator<JsonNode> iterator = JacksonFixture
                                .getObjectMapper()
                                .readerFor(JsonNode.class)
                                .readValues(inputStream);
                        while (iterator.hasNextValue()) {
                            final JsonNode value = iterator.nextValue();
                            synchronized (this) {
                                final boolean added = receivedNodes.offer(value);
                                if (!added) {
                                    droppedNodeCount++;
                                }
                            }
                        }
                    }
                }
            } catch (final EOFException ignored) {
                // Socket is closed.
            } catch (final Exception error) {
                if (!closed) {
                    throw new RuntimeException(error);
                }
            }
        }

        @Override
        public synchronized void close() throws InterruptedException {
            if (closed) {
                throw new IllegalStateException("shutdown has already been invoked");
            }
            closed = true;
            interrupt();
            join(3_000L);
        }

    }

    private static String explainChars(final String input) {
        return IntStream
                .range(0, input.length())
                .mapToObj(i -> {
                    final char c = input.charAt(i);
                    return String.format("'%c' (%04X)", c, (int) c);
                })
                .collect(Collectors.joining(", "));
    }

    @Test
    public void test_PatternResolver() {

        // Create the event template.
        final String eventTemplate = writeJson(Map(
                "message", Map(
                        "$resolver", "pattern",
                        "pattern", "%p:%m")));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate(eventTemplate)
                .build();

        // Create the log event.
        final SimpleMessage message = new SimpleMessage("foo");
        final Level level = Level.FATAL;
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setMessage(message)
                .setLevel(level)
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            final String expectedMessage = String.format(
                    "%s:%s",
                    level, message.getFormattedMessage());
            assertThat(accessor.getString("message")).isEqualTo(expectedMessage);
        });

    }

    @Test
    public void test_MessageParameterResolver_with_ParameterizedMessageFactory() {
        testMessageParameterResolver(ParameterizedMessageFactory.INSTANCE);
    }

    @Test
    public void test_MessageParameterResolver_with_ReusableMessageFactory() {
        testMessageParameterResolver(ReusableMessageFactory.INSTANCE);
    }

    private void testMessageParameterResolver(MessageFactory messageFactory) {

        // Create the event template.
        final String eventTemplate = writeJson(Map(
                "po*", Map(
                        "$resolver", "messageParameter"),
                "ps*", Map(
                        "$resolver", "messageParameter",
                        "stringified", true),
                "po2", Map(
                        "$resolver", "messageParameter",
                        "index", 2),
                "ps2", Map(
                        "$resolver", "messageParameter",
                        "index", 2,
                        "stringified", true)));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate(eventTemplate)
                .build();

        // Create the log event.
        final Object[] parameters = {1L + (long) Integer.MAX_VALUE, "foo", 56};
        final Message message = messageFactory.newMessage("foo", parameters);
        final Level level = Level.FATAL;
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setMessage(message)
                .setLevel(level)
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            assertThat(accessor.getObject("po*")).isEqualTo(Arrays.asList(parameters));
            List<String> stringifiedParameters = Arrays
                    .stream(parameters)
                    .map(String::valueOf)
                    .collect(Collectors.toList());
            assertThat(accessor.getObject("ps*")).isEqualTo(stringifiedParameters);
            assertThat(accessor.getObject("po2")).isEqualTo(parameters[2]);
            assertThat(accessor.getString("ps2")).isEqualTo(stringifiedParameters.get(2));
        });

    }

    @Test
    public void test_unresolvable_nested_fields_are_skipped() {

        // Create the event template.
        final String eventTemplate = writeJson(Map(
                "exception", Map(
                        "message", Map(
                                "$resolver", "exception",
                                "field", "message"),
                        "className", Map(
                                "$resolver", "exception",
                                "field", "className")),
                "exceptionRootCause", Map(
                        "message", Map(
                                "$resolver", "exceptionRootCause",
                                "field", "message"),
                        "className", Map(
                                "$resolver", "exceptionRootCause",
                                "field", "className")),
                "source", Map(
                        "lineNumber", Map(
                                "$resolver", "source",
                                "field", "lineNumber"),
                        "fileName", Map(
                                "$resolver", "source",
                                "field", "fileName")),
                "emptyMap", Collections.emptyMap(),
                "emptyList", Collections.emptyList(),
                "null", null));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate(eventTemplate)
                .setStackTraceEnabled(false)        // Disable "exception" and "exceptionRootCause" resolvers.
                .setLocationInfoEnabled(false)      // Disable the "source" resolver.
                .build();

        // Create the log event.
        final SimpleMessage message = new SimpleMessage("foo");
        final Level level = Level.FATAL;
        final Exception thrown = new RuntimeException("bar");
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setMessage(message)
                .setLevel(level)
                .setThrown(thrown)
                .build();

        // Check the serialized event.
        final String expectedSerializedLogEventJson =
                "{}" + JsonTemplateLayoutDefaults.getEventDelimiter();
        final String actualSerializedLogEventJson = layout.toSerializable(logEvent);
        Assertions
                .assertThat(actualSerializedLogEventJson)
                .isEqualTo(expectedSerializedLogEventJson);

    }

    private static String writeJson(final Object value) {
        final StringBuilder stringBuilder = JSON_WRITER.getStringBuilder();
        stringBuilder.setLength(0);
        try {
            JSON_WRITER.writeValue(value);
            return stringBuilder.toString();
        } finally {
            stringBuilder.setLength(0);
        }
    }

    private static void usingSerializedLogEventAccessor(
            final Layout<String> layout,
            final LogEvent logEvent,
            final Consumer<MapAccessor> accessorConsumer) {
        final String serializedLogEventJson = layout.toSerializable(logEvent);
        @SuppressWarnings("unchecked")
        final Map<String, Object> serializedLogEvent =
                (Map<String, Object>) readJson(serializedLogEventJson);
        final MapAccessor serializedLogEventAccessor = new MapAccessor(serializedLogEvent);
        accessorConsumer.accept(serializedLogEventAccessor);
    }

    private static Object readJson(final String json) {
        return JsonReader.read(json);
    }

    private static Map<String, Object> Map(final Object... pairs) {
        final Map<String, Object> map = new LinkedHashMap<>();
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("odd number of arguments");
        }
        for (int i = 0; i < pairs.length; i += 2) {
            final String key = (String) pairs[i];
            final Object value = pairs[i + 1];
            map.put(key, value);
        }
        return map;
    }

}
