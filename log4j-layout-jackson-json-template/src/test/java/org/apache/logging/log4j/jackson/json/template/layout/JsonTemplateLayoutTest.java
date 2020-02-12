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
package org.apache.logging.log4j.jackson.json.template.layout;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.lookup.MainMapLookup;
import org.apache.logging.log4j.core.net.Severity;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.message.StringMapMessage;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.util.Strings;
import org.assertj.core.data.Percentage;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonTemplateLayoutTest {

    private static final Configuration CONFIGURATION = new DefaultConfiguration();

    private static final List<LogEvent> LOG_EVENTS = LogEventFixture.createFullLogEvents(5);

    private static final JsonNodeFactory JSON_NODE_FACTORY = JsonNodeFactory.instance;

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFixture.getObjectMapper();

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
        final Set<String> mdcKeys = logEvent.getContextData().toMap().keySet();
        final String firstMdcKey = mdcKeys.iterator().next();
        final String firstMdcKeyExcludingRegex = !mdcKeys.isEmpty()
                ? String.format("^(?!%s).*$", Pattern.quote(firstMdcKey))
                : null;
        final List<String> ndcItems = logEvent.getContextStack().asList();
        final String firstNdcItem = ndcItems.get(0);
        @SuppressWarnings("ConstantConditions")
        final String firstNdcItemExcludingRegex = !ndcItems.isEmpty()
                ? String.format("^(?!%s).*$", Pattern.quote(firstNdcItem))
                : null;
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplateUri("classpath:LogstashTestLayout.json")
                .setStackTraceEnabled(true)
                .setLocationInfoEnabled(true)
                .setMdcKeyPattern(firstMdcKeyExcludingRegex)
                .setNdcPattern(firstNdcItemExcludingRegex)
                .build();
        final String serializedLogEvent = layout.toSerializable(logEvent);
        final JsonNode rootNode = OBJECT_MAPPER.readValue(serializedLogEvent, JsonNode.class);
        checkConstants(rootNode);
        checkBasicFields(logEvent, rootNode);
        checkSource(logEvent, rootNode);
        checkException(layout.getCharset(), logEvent, rootNode);
        checkContextData(logEvent, firstMdcKeyExcludingRegex, rootNode);
        checkContextStack(logEvent, firstNdcItemExcludingRegex, rootNode);
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

    private static void checkContextData(
            final LogEvent logEvent,
            final String mdcKeyRegex,
            final JsonNode rootNode) {
        final Pattern mdcKeyPattern = mdcKeyRegex != null ? Pattern.compile(mdcKeyRegex) : null;
        logEvent.getContextData().forEach((final String key, final Object value) -> {
            final JsonNode node = point(rootNode, "mdc", key);
            final boolean matches = mdcKeyPattern == null || mdcKeyPattern.matcher(key).matches();
            if (matches) {
                final JsonNode valueNode = OBJECT_MAPPER.convertValue(value, JsonNode.class);
                if (valueNode.isNumber()) {
                    final double valueNodeDouble = valueNode.asDouble();
                    final double nodeDouble = node.asDouble();
                    assertThat(nodeDouble).isEqualTo(valueNodeDouble);
                } else {
                    assertThat(node).isEqualTo(valueNode);
                }
            } else {
                assertThat(node).isEqualTo(MissingNode.getInstance());
            }
        });
    }

    private static void checkContextStack(
            final LogEvent logEvent,
            final String ndcRegex,
            final JsonNode rootNode) {

        // Determine the expected context stack.
        final Pattern ndcPattern = ndcRegex == null ? null : Pattern.compile(ndcRegex);
        final List<String> initialContextStack = logEvent.getContextStack().asList();
        final List<String> expectedContextStack = new ArrayList<>();
        for (final String contextStackItem : initialContextStack) {
            final boolean matches = ndcPattern == null || ndcPattern.matcher(contextStackItem).matches();
            if (matches) {
                expectedContextStack.add(contextStackItem);
            }
        }

        // Determine the actual context stack.
        final ArrayNode contextStack = (ArrayNode) point(rootNode, "ndc");
        final List<String> actualContextStack = new ArrayList<>();
        for (final JsonNode contextStackItem : contextStack) {
            actualContextStack.add(contextStackItem.asText());
        }

        // Compare expected and actual context stacks.
        assertThat(actualContextStack).isEqualTo(expectedContextStack);

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
        final ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.put("@timestamp", "${json:timestamp:timeZone=Europe/Amsterdam}");
        final String staticFieldName = "staticFieldName";
        final String staticFieldValue = "staticFieldValue";
        eventTemplateRootNode.put(staticFieldName, staticFieldValue);
        final String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        final BuiltConfiguration configuration =
                ConfigurationBuilderFactory.newConfigurationBuilder().build();
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        final String serializedLogEvent = layout.toSerializable(logEvent);
        final JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, "@timestamp").asText()).isEqualTo(timestamp);
        assertThat(point(rootNode, staticFieldName).asText()).isEqualTo(staticFieldValue);

    }

    @Test
    public void test_log4j_deferred_runtime_resolver_for_MapMessage() throws Exception {

        // Create the event template.
        final ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.put("mapValue3", "${json:message:json}");
        eventTemplateRootNode.put("mapValue1", "${map:key1}");
        eventTemplateRootNode.put("mapValue2", "${map:key2}");
        eventTemplateRootNode.put(
                "nestedLookupEmptyValue",
                "${map:noExist:-${map:noExist2:-${map:noExist3:-}}}");
        eventTemplateRootNode.put(
                "nestedLookupStaticValue",
                "${map:noExist:-${map:noExist2:-${map:noExist3:-Static Value}}}");
        final String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        final BuiltConfiguration configuration =
                ConfigurationBuilderFactory.newConfigurationBuilder().build();
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setEventTemplate(eventTemplate)
                .setBlankFieldExclusionEnabled(true)
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
        final String serializedLogEvent = layout.toSerializable(logEvent);
        final JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, "mapValue1").asText()).isEqualTo("val1");
        assertThat(point(rootNode, "mapValue2").asText()).isEqualTo("val2");
        assertThat(point(rootNode, "nestedLookupEmptyValue")).isInstanceOf(MissingNode.class);
        assertThat(point(rootNode, "nestedLookupStaticValue").asText()).isEqualTo("Static Value");

    }

    @Test
    public void test_MapMessage_serialization() throws Exception {

        // Create the event template.
        final ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.put("message", "${json:message:json}");
        final String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        final BuiltConfiguration configuration =
                ConfigurationBuilderFactory.newConfigurationBuilder().build();
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(configuration)
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
        final String serializedLogEvent = layout.toSerializable(logEvent);
        final JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, "message", "key1").asText()).isEqualTo("val1");
        assertThat(point(rootNode, "message", "key2").asLong()).isEqualTo(0xDEADBEEF);
        assertThat(point(rootNode, "message", "key3", "key3.1").asText()).isEqualTo("val3.1");

    }

    @Test
    public void test_property_injection() throws Exception {

        // Create the log event.
        final SimpleMessage message = new SimpleMessage("Hello, World");
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setLevel(Level.INFO)
                .setMessage(message)
                .build();

        // Create the event template with property.
        final ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        final String propertyName = "propertyName";
        eventTemplateRootNode.put(propertyName, "${" + propertyName + "}");
        final String eventTemplate = eventTemplateRootNode.toString();

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
        final String serializedLogEvent = layout.toSerializable(logEvent);
        final JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, propertyName).asText()).isEqualTo(propertyValue);

    }

    @Test
    public void test_empty_root_cause() throws Exception {

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
        final ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.put("ex_class", "${json:exception:className}");
        eventTemplateRootNode.put("ex_message", "${json:exception:message}");
        eventTemplateRootNode.put("ex_stacktrace", "${json:exception:stackTrace:text}");
        eventTemplateRootNode.put("root_ex_class", "${json:exceptionRootCause:className}");
        eventTemplateRootNode.put("root_ex_message", "${json:exceptionRootCause:message}");
        eventTemplateRootNode.put("root_ex_stacktrace", "${json:exceptionRootCause:stackTrace:text}");
        final String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        final BuiltConfiguration configuration =
                ConfigurationBuilderFactory.newConfigurationBuilder().build();
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setStackTraceEnabled(true)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        final String serializedLogEvent = layout.toSerializable(logEvent);
        final JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, "ex_class").asText())
                .isEqualTo(exception.getClass().getCanonicalName());
        assertThat(point(rootNode, "ex_message").asText())
                .isEqualTo(exception.getMessage());
        assertThat(point(rootNode, "ex_stacktrace").asText())
                .startsWith(exception.getClass().getCanonicalName() + ": " + exception.getMessage());
        assertThat(point(rootNode, "root_ex_class").asText())
                .isEqualTo(point(rootNode, "ex_class").asText());
        assertThat(point(rootNode, "root_ex_message").asText())
                .isEqualTo(point(rootNode, "ex_message").asText());
        assertThat(point(rootNode, "root_ex_stacktrace").asText())
                .isEqualTo(point(rootNode, "ex_stacktrace").asText());

    }

    @Test
    public void test_root_cause() throws Exception {

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
        final ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.put("ex_class", "${json:exception:className}");
        eventTemplateRootNode.put("ex_message", "${json:exception:message}");
        eventTemplateRootNode.put("ex_stacktrace", "${json:exception:stackTrace:text}");
        eventTemplateRootNode.put("root_ex_class", "${json:exceptionRootCause:className}");
        eventTemplateRootNode.put("root_ex_message", "${json:exceptionRootCause:message}");
        eventTemplateRootNode.put("root_ex_stacktrace", "${json:exceptionRootCause:stackTrace:text}");
        final String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        final BuiltConfiguration configuration =
                ConfigurationBuilderFactory.newConfigurationBuilder().build();
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setStackTraceEnabled(true)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        final String serializedLogEvent = layout.toSerializable(logEvent);
        final JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, "ex_class").asText())
                .isEqualTo(exception.getClass().getCanonicalName());
        assertThat(point(rootNode, "ex_message").asText())
                .isEqualTo(exception.getMessage());
        assertThat(point(rootNode, "ex_stacktrace").asText())
                .startsWith(exception.getClass().getCanonicalName() + ": " + exception.getMessage());
        assertThat(point(rootNode, "root_ex_class").asText())
                .isEqualTo(exceptionCause.getClass().getCanonicalName());
        assertThat(point(rootNode, "root_ex_message").asText())
                .isEqualTo(exceptionCause.getMessage());
        assertThat(point(rootNode, "root_ex_stacktrace").asText())
                .startsWith(exceptionCause.getClass().getCanonicalName() + ": " + exceptionCause.getMessage());

    }

    @Test
    public void test_marker_name() throws IOException {

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
        final ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        final String messageKey = "message";
        eventTemplateRootNode.put(messageKey, "${json:message}");
        final String markerNameKey = "marker";
        eventTemplateRootNode.put(markerNameKey, "${json:marker:name}");
        final String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        final BuiltConfiguration configuration =
                ConfigurationBuilderFactory.newConfigurationBuilder().build();
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        final String serializedLogEvent = layout.toSerializable(logEvent);
        final JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, messageKey).asText()).isEqualTo(message.getFormattedMessage());
        assertThat(point(rootNode, markerNameKey).asText()).isEqualTo(markerName);

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
        final BuiltConfiguration config =
                ConfigurationBuilderFactory.newConfigurationBuilder().build();
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(config)
                .setEventTemplateUri("classpath:LogstashJsonEventLayoutV1.json")
                .setPrettyPrintEnabled(prettyPrintEnabled)
                .build();

        // Check the serialized event.
        final String serializedLogEvent = layout.toSerializable(logEvent);
        final String assertionCaption = String.format("testing lineSeperator (prettyPrintEnabled=%s)", prettyPrintEnabled);
        assertThat(serializedLogEvent).as(assertionCaption).endsWith("}" + System.lineSeparator());

    }

    @Test
    public void test_main_key_access() throws IOException {

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
        final ObjectNode templateRootNode = JSON_NODE_FACTORY.objectNode();
        templateRootNode.put("name", String.format("${json:main:%s}", kwKey));
        templateRootNode.put("positionArg", "${json:main:2}");
        templateRootNode.put("notFoundArg", String.format("${json:main:%s}", missingKwKey));
        final String template = templateRootNode.toString();

        // Create the layout.
        final BuiltConfiguration configuration =
                ConfigurationBuilderFactory.newConfigurationBuilder().build();
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setEventTemplate(template)
                .build();

        // Check the serialized event.
        final String serializedLogEvent = layout.toSerializable(logEvent);
        final JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, "name").asText()).isEqualTo(kwVal);
        assertThat(point(rootNode, "positionArg").asText()).isEqualTo(positionArg);
        assertThat(point(rootNode, "notFoundArg")).isInstanceOf(NullNode.class);

    }

    @Test
    public void test_mdc_key_access() throws IOException {

        // Create the log event.
        final SimpleMessage message = new SimpleMessage("Hello, World!");
        final StringMap contextData = new SortedArrayStringMap();
        final String mdcDirectlyAccessedKey = "mdcKey1";
        final String mdcDirectlyAccessedValue = "mdcValue1";
        contextData.putValue(mdcDirectlyAccessedKey, mdcDirectlyAccessedValue);
        final String mdcPatternMatchedKey = "mdcKey2";
        final String mdcPatternMatchedValue = "mdcValue2";
        contextData.putValue(mdcPatternMatchedKey, mdcPatternMatchedValue);
        final String mdcPatternMismatchedKey = "mdcKey3";
        final String mdcPatternMismatchedValue = "mdcValue3";
        contextData.putValue(mdcPatternMismatchedKey, mdcPatternMismatchedValue);
        final String mdcDirectlyAccessedNullPropertyKey = "mdcKey4";
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
        final ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        final String mdcFieldName = "mdc";
        eventTemplateRootNode.put(mdcFieldName, "${json:mdc}");
        eventTemplateRootNode.put(
                mdcDirectlyAccessedKey,
                String.format("${json:mdc:%s}", mdcDirectlyAccessedKey));
        eventTemplateRootNode.put(
                mdcDirectlyAccessedNullPropertyKey,
                String.format("${json:mdc:%s}", mdcDirectlyAccessedNullPropertyKey));
        String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        final BuiltConfiguration configuration =
                ConfigurationBuilderFactory.newConfigurationBuilder().build();
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setStackTraceEnabled(true)
                .setEventTemplate(eventTemplate)
                .setMdcKeyPattern(mdcPatternMatchedKey)
                .build();

        // Check the serialized event.
        final String serializedLogEvent = layout.toSerializable(logEvent);
        final JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, mdcDirectlyAccessedKey).asText()).isEqualTo(mdcDirectlyAccessedValue);
        assertThat(point(rootNode, mdcFieldName, mdcPatternMatchedKey).asText()).isEqualTo(mdcPatternMatchedValue);
        assertThat(point(rootNode, mdcFieldName, mdcPatternMismatchedKey)).isInstanceOf(MissingNode.class);
        assertThat(point(rootNode, mdcDirectlyAccessedNullPropertyKey)).isInstanceOf(NullNode.class);

    }

    @Test
    public void test_MapResolver() throws IOException {

        // Create the log event.
        final StringMapMessage message = new StringMapMessage().with("key1", "val1");
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setLevel(Level.INFO)
                .setMessage(message)
                .build();

        // Create the event template node with map values.
        final ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.put("mapValue1", "${json:map:key1}");
        eventTemplateRootNode.put("mapValue2", "${json:map:noExist}");
        final String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        final BuiltConfiguration configuration =
                ConfigurationBuilderFactory.newConfigurationBuilder().build();
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setEventTemplate(eventTemplate)
                .setBlankFieldExclusionEnabled(true)
                .build();

        // Check serialized event.
        final String serializedLogEvent = layout.toSerializable(logEvent);
        final JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, "mapValue1").asText()).isEqualTo("val1");
        assertThat(point(rootNode, "mapValue2")).isInstanceOf(MissingNode.class);

    }

    @Test
    public void test_blankFieldExclusionEnabled() throws IOException {

        // Create the log event.
        final SimpleMessage message = new SimpleMessage("Hello, World!");
        final StringMap contextData = new SortedArrayStringMap();
        final String mdcEmptyKey1 = "mdcKey1";
        final String mdcEmptyKey2 = "mdcKey2";
        contextData.putValue(mdcEmptyKey1, "");
        contextData.putValue(mdcEmptyKey2, null);
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setLevel(Level.INFO)
                .setMessage(message)
                .setContextData(contextData)
                .build();

        // Create event template node with empty property and MDC fields.
        final ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        final String mdcFieldName = "mdc";
        final String blankField1Name = "property1Name";
        eventTemplateRootNode.put(blankField1Name, "${" + blankField1Name + "}");
        eventTemplateRootNode.put(mdcFieldName, "${json:mdc}");
        eventTemplateRootNode.put(mdcEmptyKey1, String.format("${json:mdc:%s}", mdcEmptyKey1));
        eventTemplateRootNode.put(mdcEmptyKey2, String.format("${json:mdc:%s}", mdcEmptyKey2));

        // Put a "blankObject": {"emptyArray": []} field into event template.
        final String blankObjectFieldName = "blankObject";
        final ObjectNode blankObjectNode = JSON_NODE_FACTORY.objectNode();
        final String emptyArrayFieldName = "emptyArray";
        final ArrayNode emptyArrayNode = JSON_NODE_FACTORY.arrayNode();
        blankObjectNode.set(emptyArrayFieldName, emptyArrayNode);
        eventTemplateRootNode.set(blankObjectFieldName, blankObjectNode);

        // Put an "emptyObject": {} field into the event template.
        final String emptyObjectFieldName = "emptyObject";
        final ObjectNode emptyObjectNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.set(emptyObjectFieldName, emptyObjectNode);

        // Render the event template.
        final String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout configuration.
        final Configuration config = ConfigurationBuilderFactory
                .newConfigurationBuilder()
                .addProperty(blankField1Name, "")
                .build();

        for (final boolean blankFieldExclusionEnabled : new boolean[]{true, false}) {

            // Create the layout.
            final JsonTemplateLayout layout = JsonTemplateLayout
                    .newBuilder()
                    .setConfiguration(config)
                    .setEventTemplate(eventTemplate)
                    .setBlankFieldExclusionEnabled(blankFieldExclusionEnabled)
                    .build();

            // Check serialized event.
            final String serializedLogEvent = layout.toSerializable(logEvent);
            if (blankFieldExclusionEnabled) {
                assertThat(serializedLogEvent).isEqualTo("{}" + System.lineSeparator());
            } else {

                // Check property and MDC fields.
                final JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
                assertThat(point(rootNode, mdcEmptyKey1).asText()).isEmpty();
                assertThat(point(rootNode, mdcEmptyKey2)).isInstanceOf(NullNode.class);
                assertThat(point(rootNode, mdcFieldName)).isInstanceOf(ObjectNode.class);
                assertThat(point(rootNode, mdcFieldName, mdcEmptyKey1).asText()).isEmpty();
                assertThat(point(rootNode, mdcFieldName, mdcEmptyKey2)).isInstanceOf(NullNode.class);
                assertThat(point(rootNode, blankField1Name).asText()).isEmpty();

                // Check "blankObject": {"emptyArray": []} field.
                assertThat(point(rootNode, blankObjectFieldName, emptyArrayFieldName).isArray()).isTrue();
                assertThat(point(rootNode, blankObjectFieldName, emptyArrayFieldName).size()).isZero();

                // Check "emptyObject": {} field.
                assertThat(point(rootNode, emptyObjectFieldName).isObject()).isTrue();
                assertThat(point(rootNode, emptyObjectFieldName).size()).isZero();

            }

        }

    }

    @Test
    public void test_message_json() throws IOException {

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
        final ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.put("message", "${json:message:json}");
        final String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        final BuiltConfiguration configuration =
                ConfigurationBuilderFactory.newConfigurationBuilder().build();
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setStackTraceEnabled(true)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        final String serializedLogEvent = layout.toSerializable(logEvent);
        final JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, "message", "message").asText()).isEqualTo("Hello, World!");
        assertThat(point(rootNode, "message", "bottle").asText()).isEqualTo("Kickapoo Joy Juice");

    }

    @Test
    public void test_message_json_fallback() throws IOException {

        // Create the log event.
        final SimpleMessage message = new SimpleMessage("Hello, World!");
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setLevel(Level.INFO)
                .setMessage(message)
                .build();

        // Create the event template.
        final ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.put("message", "${json:message:json}");
        final String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        final BuiltConfiguration configuration =
                ConfigurationBuilderFactory.newConfigurationBuilder().build();
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setStackTraceEnabled(true)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        final String serializedLogEvent = layout.toSerializable(logEvent);
        final JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, "message").asText()).isEqualTo("Hello, World!");

    }

    private static final class ObjectMessageAttachment {

        @JsonProperty
        private final int id;

        @JsonProperty
        private final String name;

        private ObjectMessageAttachment(final int id, final String name) {
            this.id = id;
            this.name = name;
        }

    }

    @Test
    public void test_message_object() throws IOException {

        // Create the log event.
        final int id = Math.abs((int) (Math.random() * Integer.MAX_VALUE));
        final String name = "name-" + id;
        final ObjectMessageAttachment attachment = new ObjectMessageAttachment(id, name);
        final ObjectMessage message = new ObjectMessage(attachment);
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setLevel(Level.INFO)
                .setMessage(message)
                .build();

        // Create the event template.
        final ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.put("message", "${json:message:json}");
        final String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        BuiltConfiguration configuration = ConfigurationBuilderFactory.newConfigurationBuilder().build();
        JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setStackTraceEnabled(true)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        final String serializedLogEvent = layout.toSerializable(logEvent);
        final JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, "message", "id").asInt()).isEqualTo(attachment.id);
        assertThat(point(rootNode, "message", "name").asText()).isEqualTo(attachment.name);

    }

    @Test
    public void test_StackTraceElement_template() throws IOException {

        // Create the stack trace element template.
        final ObjectNode stackTraceElementTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        final String classNameFieldName = "className";
        stackTraceElementTemplateRootNode.put(
                classNameFieldName,
                "${json:stackTraceElement:className}");
        final String methodNameFieldName = "methodName";
        stackTraceElementTemplateRootNode.put(
                methodNameFieldName,
                "${json:stackTraceElement:methodName}");
        final String fileNameFieldName = "fileName";
        stackTraceElementTemplateRootNode.put(
                fileNameFieldName,
                "${json:stackTraceElement:fileName}");
        final String lineNumberFieldName = "lineNumber";
        stackTraceElementTemplateRootNode.put(
                lineNumberFieldName,
                "${json:stackTraceElement:lineNumber}");
        final String stackTraceElementTemplate = stackTraceElementTemplateRootNode.toString();

        // Create the event template.
        final ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        final String stackTraceFieldName = "stackTrace";
        eventTemplateRootNode.put(stackTraceFieldName, "${json:exception:stackTrace}");
        final String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        final BuiltConfiguration configuration =
                ConfigurationBuilderFactory.newConfigurationBuilder().build();
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(configuration)
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
        final String serializedLogEvent = layout.toSerializable(logEvent);
        final JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        final JsonNode stackTraceNode = point(rootNode, stackTraceFieldName);
        assertThat(stackTraceNode.isArray()).isTrue();
        final StackTraceElement[] stackTraceElements = exception.getStackTrace();
        assertThat(stackTraceNode.size()).isEqualTo(stackTraceElements.length);
        for (int stackTraceElementIndex = 0;
             stackTraceElementIndex < stackTraceElements.length;
             stackTraceElementIndex++) {
            final StackTraceElement stackTraceElement = stackTraceElements[stackTraceElementIndex];
            final JsonNode stackTraceElementNode = stackTraceNode.get(stackTraceElementIndex);
            assertThat(stackTraceElementNode.size()).isEqualTo(4);
            assertThat(point(stackTraceElementNode, classNameFieldName).asText())
                    .isEqualTo(stackTraceElement.getClassName());
            assertThat(point(stackTraceElementNode, methodNameFieldName).asText())
                    .isEqualTo(stackTraceElement.getMethodName());
            assertThat(point(stackTraceElementNode, fileNameFieldName).asText())
                    .isEqualTo(stackTraceElement.getFileName());
            assertThat(point(stackTraceElementNode, lineNumberFieldName).asInt())
                    .isEqualTo(stackTraceElement.getLineNumber());
        }

    }

    @Test
    public void test_toSerializable_toByteArray_encode_outputs() {

        // Create the layout.
        final BuiltConfiguration configuration =
                ConfigurationBuilderFactory.newConfigurationBuilder().build();
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setEventTemplateUri("classpath:LogstashJsonEventLayoutV1.json")
                .setConfiguration(configuration)
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
    public void test_maxStringLength() throws IOException {

        // Create the log event.
        final int maxStringLength = 30;
        final String truncatedMessage = Strings.repeat("m", maxStringLength);
        final SimpleMessage message = new SimpleMessage(truncatedMessage + 'M');
        final Throwable thrown = new RuntimeException();
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setLevel(Level.INFO)
                .setMessage(message)
                .setThrown(thrown)
                .build();

        // Create the event template node with map values.
        final ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        final String messageKey = "message";
        eventTemplateRootNode.put(messageKey, "${json:message}");
        final String truncatedKey = Strings.repeat("k", maxStringLength);
        final String truncatedValue = Strings.repeat("v", maxStringLength);
        eventTemplateRootNode.put(truncatedKey + "K", truncatedValue + "V");
        final String nullValueKey = "nullValueKey";
        eventTemplateRootNode.put(nullValueKey, "${json:exception:message}");
        final String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        final BuiltConfiguration configuration =
                ConfigurationBuilderFactory.newConfigurationBuilder().build();
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setEventTemplate(eventTemplate)
                .setMaxStringLength(maxStringLength)
                .build();

        // Check serialized event.
        final String serializedLogEvent = layout.toSerializable(logEvent);
        final JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, messageKey).asText()).isEqualTo(truncatedMessage);
        assertThat(point(rootNode, truncatedKey).asText()).isEqualTo(truncatedValue);
        assertThat(point(rootNode, nullValueKey).isNull()).isTrue();

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
    public void test_exception_with_nonAscii_utf8_method_name() throws IOException {

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
        final ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.put("ex_stacktrace", "${json:exception:stackTrace:text}");
        final String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        final BuiltConfiguration configuration =
                ConfigurationBuilderFactory.newConfigurationBuilder().build();
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setStackTraceEnabled(true)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        final String serializedLogEvent = layout.toSerializable(logEvent);
        final JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, "ex_stacktrace").asText())
                .contains(NonAsciiUtf8MethodNameContainingException.NON_ASCII_UTF8_TEXT);

    }

    @Test
    public void test_custom_ObjectMapper_factory_method() throws IOException {

        // Create the log event.
        final Date logEventDate = new Date();
        final ObjectMessage message = new ObjectMessage(logEventDate);
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setLevel(Level.INFO)
                .setMessage(message)
                .setTimeMillis(logEventDate.getTime())
                .build();

        // Create the event template.
        final ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.put("message", "${json:message:json}");
        final String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        final BuiltConfiguration configuration =
                ConfigurationBuilderFactory.newConfigurationBuilder().build();
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setObjectMapperFactoryMethod(
                        "org.apache.logging.log4j.jackson.json.template.layout.JsonTemplateLayoutTest.getCustomObjectMapper")
                .setConfiguration(configuration)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        final String serializedLogEvent = layout.toSerializable(logEvent);
        final String expectedTimestamp = getCustomObjectMapperDateFormat().format(logEventDate);
        final JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, "message").asText()).isEqualTo(expectedTimestamp);

    }

    @SuppressWarnings("unused")
    public static ObjectMapper getCustomObjectMapper() {
        SimpleDateFormat dateFormat = getCustomObjectMapperDateFormat();
        return new ObjectMapper().setDateFormat(dateFormat);
    }

    private static SimpleDateFormat getCustomObjectMapperDateFormat() {
        return new SimpleDateFormat("'year='yyyy', month='MM', day='dd");
    }

    @Test
    public void test_event_template_additional_fields() throws IOException {

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
        final ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.put("level", "${json:level}");
        final String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        final BuiltConfiguration configuration =
                ConfigurationBuilderFactory.newConfigurationBuilder().build();
        final KeyValuePair additionalField1 = new KeyValuePair("message", "${json:message}");
        final KeyValuePair additionalField2 = new KeyValuePair("@version", "1");
        final KeyValuePair[] additionalFieldPairs = {additionalField1, additionalField2};
        final JsonTemplateLayout.EventTemplateAdditionalFields additionalFields = JsonTemplateLayout
                .EventTemplateAdditionalFields
                .newBuilder()
                .setAdditionalFields(additionalFieldPairs)
                .build();
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setStackTraceEnabled(true)
                .setEventTemplate(eventTemplate)
                .setEventTemplateAdditionalFields(additionalFields)
                .build();

        // Check the serialized event.
        final String serializedLogEvent = layout.toSerializable(logEvent);
        final JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, "level").asText()).isEqualTo(level.name());
        assertThat(point(rootNode, additionalField1.getKey()).asText()).isEqualTo(message.getFormattedMessage());
        assertThat(point(rootNode, additionalField2.getKey()).asText()).isEqualTo(additionalField2.getValue());

    }

    @Test
    @SuppressWarnings("FloatingPointLiteralPrecision")
    public void test_timestamp_epoch_accessor() throws IOException {

        // Create the log event.
        final SimpleMessage message = new SimpleMessage("Hello, World!");
        final Level level = Level.ERROR;
        final MutableInstant instant = new MutableInstant();
        final long instantEpochSecond = 1581082727L;
        final int instantEpochSecondNano = 982123456;
        instant.initFromEpochSecond(instantEpochSecond, instantEpochSecondNano);
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setLevel(level)
                .setMessage(message)
                .setInstant(instant)
                .build();

        // Create the event template.
        final ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        final ObjectNode epochSecsNode = eventTemplateRootNode.putObject("epochSecs");
        epochSecsNode.put("double", "${json:timestamp:epoch:secs}");
        epochSecsNode.put("long", "${json:timestamp:epoch:secs,integral}");
        epochSecsNode.put("nanos", "${json:timestamp:epoch:secs.nanos}");
        epochSecsNode.put("micros", "${json:timestamp:epoch:secs.micros}");
        epochSecsNode.put("millis", "${json:timestamp:epoch:secs.millis}");
        final ObjectNode epochMillisNode = eventTemplateRootNode.putObject("epochMillis");
        epochMillisNode.put("double", "${json:timestamp:epoch:millis}");
        epochMillisNode.put("long", "${json:timestamp:epoch:millis,integral}");
        epochMillisNode.put("nanos", "${json:timestamp:epoch:millis.nanos}");
        epochMillisNode.put("micros", "${json:timestamp:epoch:millis.micros}");
        final ObjectNode epochMicrosNode = eventTemplateRootNode.putObject("epochMicros");
        epochMicrosNode.put("double", "${json:timestamp:epoch:micros}");
        epochMicrosNode.put("long", "${json:timestamp:epoch:micros,integral}");
        epochMicrosNode.put("nanos", "${json:timestamp:epoch:micros.nanos}");
        final ObjectNode epochNanosNode = eventTemplateRootNode.putObject("epochNanos");
        epochNanosNode.put("long", "${json:timestamp:epoch:nanos}");
        final String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        final BuiltConfiguration configuration =
                ConfigurationBuilderFactory.newConfigurationBuilder().build();
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        final String serializedLogEvent = layout.toSerializable(logEvent);
        final JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        final Percentage errorMargin = Percentage.withPercentage(0.001D);
        assertThat(point(rootNode, "epochSecs", "double").asDouble())
                .isCloseTo(1581082727.982123456D, errorMargin);
        assertThat(point(rootNode, "epochSecs", "long").asLong())
                .isEqualTo(1581082727L);
        assertThat(point(rootNode, "epochSecs", "nanos").asInt())
                .isEqualTo(982123456L);
        assertThat(point(rootNode, "epochSecs", "micros").asInt())
                .isEqualTo(982123L);
        assertThat(point(rootNode, "epochSecs", "millis").asInt())
                .isEqualTo(982L);
        assertThat(point(rootNode, "epochMillis", "double").asDouble())
                .isCloseTo(1581082727982.123456D, errorMargin);
        assertThat(point(rootNode, "epochMillis", "long").asLong())
                .isEqualTo(1581082727982L);
        assertThat(point(rootNode, "epochMillis", "nanos").asInt())
                .isEqualTo(123456);
        assertThat(point(rootNode, "epochMillis", "micros").asInt())
                .isEqualTo(123);
        assertThat(point(rootNode, "epochMicros", "double").asDouble())
                .isCloseTo(1581082727982123.456D, errorMargin);
        assertThat(point(rootNode, "epochMicros", "long").asLong())
                .isEqualTo(1581082727982123L);
        assertThat(point(rootNode, "epochMicros", "nanos").asInt())
                .isEqualTo(456);
        assertThat(point(rootNode, "epochNanos", "long").asLong())
                .isEqualTo(1581082727982123456L);

    }

    @Test
    public void test_level_severity() throws IOException {

        // Create the event template.
        final ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.put("severity", "${json:level:severity}");
        eventTemplateRootNode.put("severityCode", "${json:level:severity:code}");
        final String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        final BuiltConfiguration configuration =
                ConfigurationBuilderFactory.newConfigurationBuilder().build();
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(configuration)
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
            final String serializedLogEvent = layout.toSerializable(logEvent);
            final JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
            final Severity expectedSeverity = Severity.getSeverity(level);
            final String expectedSeverityName = expectedSeverity.name();
            final int expectedSeverityCode = expectedSeverity.getCode();
            assertThat(point(rootNode, "severity").asText()).isEqualTo(expectedSeverityName);
            assertThat(point(rootNode, "severityCode").asInt()).isEqualTo(expectedSeverityCode);

        }

    }

    @Test
    public void test_exception_resolvers_against_no_exceptions() throws IOException {

        // Create the log event.
        final SimpleMessage message = new SimpleMessage("Hello, World!");
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setMessage(message)
                .build();

        // Create the event template.
        final ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.put("exceptionStackTrace", "${json:exception:stackTrace}");
        eventTemplateRootNode.put("exceptionStackTraceText", "${json:exception:stackTrace:text}");
        eventTemplateRootNode.put("exceptionRootCauseStackTrace", "${json:exceptionRootCause:stackTrace}");
        eventTemplateRootNode.put("exceptionRootCauseStackTraceText", "${json:exceptionRootCause:stackTrace:text}");
        eventTemplateRootNode.put("requiredFieldTriggeringError", true);
        final String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        final BuiltConfiguration configuration =
                ConfigurationBuilderFactory.newConfigurationBuilder().build();
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setEventTemplate(eventTemplate)
                .setStackTraceEnabled(true)
                .build();

        // Check the serialized event.
        final String serializedLogEvent = layout.toSerializable(logEvent);
        final JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, "exceptionStackTrace")).isInstanceOf(NullNode.class);
        assertThat(point(rootNode, "exceptionStackTraceText")).isInstanceOf(NullNode.class);
        assertThat(point(rootNode, "exceptionRootCauseStackTrace")).isInstanceOf(NullNode.class);
        assertThat(point(rootNode, "exceptionRootCauseStackTraceText")).isInstanceOf(NullNode.class);
        assertThat(point(rootNode, "requiredFieldTriggeringError").asBoolean()).isTrue();

    }

    @Test
    public void test_timestamp_resolver() throws IOException {

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
        final ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.put(
                "timestamp",
                "${json:timestamp:" +
                        "pattern=yyyy-MM-dd'T'HH:mm:ss'Z'," +
                        "timeZone=UTC" +
                        "}");
        final String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        final BuiltConfiguration configuration =
                ConfigurationBuilderFactory.newConfigurationBuilder().build();
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized 1st event.
        final String serializedLogEvent1 = layout.toSerializable(logEvent1);
        final JsonNode rootNode1 = OBJECT_MAPPER.readTree(serializedLogEvent1);
        assertThat(point(rootNode1, "timestamp").asText()).isEqualTo(logEvent1FormattedInstant);

        // Check the serialized 2nd event.
        final String serializedLogEvent2 = layout.toSerializable(logEvent2);
        final JsonNode rootNode2 = OBJECT_MAPPER.readTree(serializedLogEvent2);
        assertThat(point(rootNode2, "timestamp").asText()).isEqualTo(logEvent2FormattedInstant);

        // Check the serialized 3rd event.
        final String serializedLogEvent3 = layout.toSerializable(logEvent3);
        final JsonNode rootNode3 = OBJECT_MAPPER.readTree(serializedLogEvent3);
        assertThat(point(rootNode3, "timestamp").asText()).isEqualTo(logEvent3FormattedInstant);

        // Check the serialized 4th event.
        final String serializedLogEvent4 = layout.toSerializable(logEvent4);
        final JsonNode rootNode4 = OBJECT_MAPPER.readTree(serializedLogEvent4);
        assertThat(point(rootNode4, "timestamp").asText()).isEqualTo(logEvent4FormattedInstant);

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

}
