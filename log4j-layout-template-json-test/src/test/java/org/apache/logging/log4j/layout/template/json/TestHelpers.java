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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.layout.template.json.util.JsonReader;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;
import org.apache.logging.log4j.layout.template.json.util.MapAccessor;
import org.apache.logging.log4j.util.Constants;

public final class TestHelpers {

    public static final Configuration CONFIGURATION = new DefaultConfiguration();

    public static final String JAVA_BASE_PREFIX = Constants.JAVA_MAJOR_VERSION > 8 ? "java.base/" : "";

    private static final JsonWriter JSON_WRITER = JsonWriter.newBuilder()
            .setMaxStringLength(10_000)
            .setTruncatedStringSuffix("…")
            .build();

    private TestHelpers() {}

    @SuppressWarnings("unchecked")
    static Map<String, Object> serializeUsingLayout(final LogEvent logEvent, final Layout<String> layout) {
        final String json = layout.toSerializable(logEvent);
        return (Map<String, Object>) JsonReader.read(json);
    }

    public static String writeJson(final Object value) {
        synchronized (JSON_WRITER) {
            final StringBuilder stringBuilder = JSON_WRITER.getStringBuilder();
            stringBuilder.setLength(0);
            try {
                JSON_WRITER.writeValue(value);
                return stringBuilder.toString();
            } finally {
                stringBuilder.setLength(0);
            }
        }
    }

    public static void usingSerializedLogEventAccessor(
            final Layout<String> layout, final LogEvent logEvent, final Consumer<MapAccessor> accessorConsumer) {
        final String serializedLogEventJson = layout.toSerializable(logEvent);
        @SuppressWarnings("unchecked")
        final Map<String, Object> deserializedLogEvent = (Map<String, Object>) readJson(serializedLogEventJson);
        final MapAccessor serializedLogEventAccessor = new MapAccessor(deserializedLogEvent);
        accessorConsumer.accept(serializedLogEventAccessor);
    }

    public static Object readJson(final String json) {
        try {
            return JsonReader.read(json);
        } catch (final Exception error) {
            throw new RuntimeException("failed to deserialize the JSON: " + json, error);
        }
    }

    public static Map<String, Object> asMap(final Object... pairs) {
        final Map<String, Object> map = new LinkedHashMap<>();
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("odd number of arguments: " + pairs.length);
        }
        for (int i = 0; i < pairs.length; i += 2) {
            final String key = (String) pairs[i];
            final Object value = pairs[i + 1];
            map.put(key, value);
        }
        return map;
    }

    /**
     * Provides a configuration using the given JSON event template to the given consumer.
     * <p>
     * A {@link ListAppender} (named {@code List}) wrapping the layout is used to store the log events.
     * The root logger level is set to {@link Level#ALL}.
     * </p>
     * @param configName a configuration name
     * @param eventTemplate a JSON template
     * @param consumer a consumer accepting the created logger context and the list appender
     */
    public static void withContextFromTemplate(
            final String configName,
            final Object eventTemplate,
            final BiConsumer<LoggerContext, ListAppender> consumer) {

        // Create the configuration builder.
        final ConfigurationBuilder<BuiltConfiguration> configBuilder =
                ConfigurationBuilderFactory.newConfigurationBuilder()
                        .setStatusLevel(Level.ERROR)
                        .setConfigurationName(configName);

        // Create the configuration.
        final String eventTemplateJson = writeJson(eventTemplate);
        final String appenderName = "List";
        final Configuration config = configBuilder
                .add(configBuilder
                        .newAppender(appenderName, "List")
                        .addAttribute("raw", true)
                        .add(configBuilder
                                .newLayout("JsonTemplateLayout")
                                .addAttribute("eventTemplate", eventTemplateJson)))
                .add(configBuilder.newRootLogger(Level.ALL).add(configBuilder.newAppenderRef(appenderName)))
                .build(false);

        // Initialize the configuration and pass it to the consumer.
        try (final LoggerContext loggerContext = Configurator.initialize(config)) {
            final ListAppender appender = loggerContext.getConfiguration().getAppender("List");
            consumer.accept(loggerContext, appender);
        }
    }

    public static void uncheckedSleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted");
        }
    }
}
